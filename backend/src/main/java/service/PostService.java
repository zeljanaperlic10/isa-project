package service;

import dto.PostDTO;
import model.Post;
import model.Tag;
import model.UploadEvent;
import model.User;
// import model.UploadEvent;  // PRIVREMENO ZAKOMENTIRISANO
import repository.PostRepository;
import repository.TagRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FileStorageService fileStorageService;

    // PRIVREMENO ZAKOMENTIRISANO - dok ne kopiraš JsonMessageProducer.java
     @Autowired
     private JsonMessageProducer jsonMessageProducer;

    // ============================================
    // KREIRANJE POSTA - @TRANSACTIONAL (3.3 zahtev)
    // ============================================
    
    @Transactional(timeout = 120)
    public PostDTO createPost(
            String username, 
            String title, 
            String description,
            MultipartFile videoFile, 
            MultipartFile thumbnailFile,
            Set<String> tagNames,
            Double latitude,
            Double longitude,
            String locationName) {
        
        System.out.println("🎬 Kreiranje posta - START");
        
        try {
            // KORAK 1: Pronalaženje korisnika
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Korisnik nije pronađen: " + username);
            }
            User user = userOpt.get();
            System.out.println("✅ Korisnik pronađen: " + user.getUsername() + " (email: " + username + ")");

            // KORAK 2: Validacija naslova
            if (title == null || title.trim().isEmpty()) {
                throw new RuntimeException("Naslov je obavezan!");
            }
            if (title.length() > 200) {
                throw new RuntimeException("Naslov može imati maksimum 200 karaktera!");
            }
            System.out.println("✅ Naslov validan: " + title);

            // KORAK 3: UPLOAD VIDEO FAJLA
            System.out.println("📤 Upload videa u toku...");
            String videoFileName = fileStorageService.storeVideoFile(videoFile);
            String videoUrl = "/api/videos/" + videoFileName;
            System.out.println("✅ Video uploadovan: " + videoFileName);

            // KORAK 4: UPLOAD THUMBNAIL SLIKE
            System.out.println("📤 Upload thumbnail-a u toku...");
            String thumbnailFileName = fileStorageService.storeThumbnailFile(thumbnailFile);
            String thumbnailUrl = "/api/thumbnails/" + thumbnailFileName;
            System.out.println("✅ Thumbnail uploadovan: " + thumbnailFileName);

            // KORAK 5: Kreiranje Post objekta
            Post post = new Post();
            post.setUser(user);
            post.setTitle(title.trim());
            post.setDescription(description != null ? description.trim() : null);
            post.setVideoUrl(videoUrl);
            post.setThumbnailUrl(thumbnailUrl);
            post.setVideoFileName(videoFileName);
            post.setFileSize(videoFile.getSize());
            post.setDuration(null);
            
            // KORAK 6: TAGOVI
            if (tagNames != null && !tagNames.isEmpty()) {
                System.out.println("🏷️ Procesiranje tagova: " + tagNames);
                Set<Tag> tags = processTagsString(tagNames);
                post.setTags(tags);
                System.out.println("✅ Tagovi dodati: " + tags.size() + " tagova");
            }

            // KORAK 7: GEOGRAFSKA LOKACIJA
            if (latitude != null && longitude != null) {
                if (latitude < -90 || latitude > 90) {
                    throw new RuntimeException("Nevažeća geografska širina (latitude): " + latitude);
                }
                if (longitude < -180 || longitude > 180) {
                    throw new RuntimeException("Nevažeća geografska dužina (longitude): " + longitude);
                }
                
                post.setLatitude(latitude);
                post.setLongitude(longitude);
                post.setLocationName(locationName);
                System.out.println("✅ Geolokacija: " + locationName + " (" + latitude + ", " + longitude + ")");
            }

            // KORAK 8: Čuvanje u bazi
            Post savedPost = postRepository.save(post);
            System.out.println("✅ Post sačuvan u bazi - ID: " + savedPost.getId());

            // KORAK 9: Ažuriranje brojača tagova
            updateTagCounts(savedPost.getTags());

            // ============================================
            // KORAK 10: RabbitMQ - PRIVREMENO ZAKOMENTIRISANO
            // ============================================
            
            try {
                System.out.println("📤 Slanje UploadEvent poruke u RabbitMQ...");
                
                UploadEvent uploadEvent = new UploadEvent(
                    savedPost.getId(),
                    savedPost.getTitle(),
                    savedPost.getDescription(),
                    savedPost.getUser().getUsername(),
                    savedPost.getUser().getEmail(),
                    savedPost.getVideoUrl(),
                    savedPost.getThumbnailUrl(),
                    savedPost.getFileSize(),
                    savedPost.getDuration()
                );
                
                jsonMessageProducer.sendMessage(uploadEvent);
                
                System.out.println("✅ UploadEvent poruka poslata u RabbitMQ!");
                
            } catch (Exception e) {
                System.err.println("⚠️ Greška pri slanju poruke u RabbitMQ: " + e.getMessage());
            }
            

            System.out.println("🎉 Post uspešno kreiran! ID: " + savedPost.getId());
            
            return convertToDTO(savedPost);
            
        } catch (Exception e) {
            System.err.println("❌ Greška pri kreiranju posta: " + e.getMessage());
            cleanupFailedUpload(null, null);
            throw new RuntimeException("Upload video objave nije uspeo: " + e.getMessage(), e);
        }
    }

    // ============================================
    // DOBIJANJE SVIH POSTOVA
    // ============================================
    
    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // DOBIJANJE JEDNOG POSTA (3.7 - sa atomic increment)
    // ============================================
    
    public PostDTO getPostById(Long postId) {
        System.out.println("🔍 [getPostById] START - ID: " + postId);
        
        Optional<Post> postOpt = postRepository.findById(postId);
        
        if (!postOpt.isPresent()) {
            System.err.println("❌ Post nije pronađen!");
            throw new RuntimeException("Post nije pronađen! ID: " + postId);
        }
        
        Post post = postOpt.get();
        System.out.println("✅ Post pronađen: " + post.getTitle());
        
        // JEDNOSTAVNO: increment view count OVDE, u istoj transakciji
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
        
        System.out.println("✅ View count: " + post.getViewsCount());
        
        return convertToDTO(post);
    }

    // ============================================
    // DOBIJANJE POSTOVA KORISNIKA
    // ============================================
    
    public List<PostDTO> getUserPosts(String username) {
        List<Post> posts = postRepository.findByUserUsernameOrderByCreatedAtDesc(username);
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // BRISANJE POSTA
    // ============================================
    
    @Transactional
    public void deletePost(Long postId, String email) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen! ID: " + postId);
        }

        Post post = postOpt.get();

        if (!post.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Nemate pravo da obrišete ovaj post!");
        }

        String videoFileName = extractFileName(post.getVideoUrl());
        String thumbnailFileName = extractFileName(post.getThumbnailUrl());
        
        fileStorageService.deleteVideoFile(videoFileName);
        fileStorageService.deleteThumbnailFile(thumbnailFileName);

        postRepository.deleteById(postId);
        
        System.out.println("🗑️ Post obrisan: ID=" + postId);
    }

    // ============================================
    // PRETRAGA PO TAGOVIMA
    // ============================================
    
    public List<PostDTO> searchByTag(String tagName) {
        List<Post> posts = postRepository.findByTagName(tagName.toLowerCase());
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // BROJAČI - LAJKOVI
    // ============================================
    
    public void incrementLikesCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
        }
    }

    public void decrementLikesCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            postRepository.save(post);
        }
    }

    // ============================================
    // BROJAČ KOMENTARA (3.6 zahtev)
    // ============================================
    
    @Transactional
    public void incrementCommentsCount(Long postId) {
        System.out.println("➕ Increment comments count za post " + postId);
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(post.getCommentsCount() + 1);
            postRepository.save(post);
            
            System.out.println("✅ Comments count ažuriran: " + post.getCommentsCount());
        }
    }
    
    @Transactional
    public void decrementCommentsCount(Long postId) {
        System.out.println("➖ Decrement comments count za post " + postId);
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
            
            System.out.println("✅ Comments count ažuriran: " + post.getCommentsCount());
        }
    }

    // ============================================
    // BROJAČ PREGLEDA (3.7 zahtev)
    // ============================================
    
    @Transactional
    public void incrementViewCount(Long postId) {
        System.out.println("👁️ Increment view count za post " + postId);
        
        int updated = postRepository.incrementViewCount(postId);
        
        if (updated > 0) {
            System.out.println("✅ View count inkrementiran (atomic operation)");
        } else {
            System.err.println("❌ Post nije pronađen: " + postId);
        }
    }

    // ============================================
    // POMOĆNE METODE - TAGOVI
    // ============================================
    
    private Set<Tag> processTagsString(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) {
                continue;
            }
            
            String normalizedTagName = tagName.toLowerCase().trim();
            
            if (normalizedTagName.length() > 50) {
                throw new RuntimeException("Tag je predugačak (max 50 karaktera): " + normalizedTagName);
            }
            
            Optional<Tag> existingTag = tagRepository.findByName(normalizedTagName);
            
            Tag tag;
            if (existingTag.isPresent()) {
                tag = existingTag.get();
                System.out.println("   ✓ Tag pronađen: " + normalizedTagName);
            } else {
                tag = new Tag(normalizedTagName);
                tag = tagRepository.save(tag);
                System.out.println("   ✓ Tag kreiran: " + normalizedTagName);
            }
            
            tags.add(tag);
        }
        
        return tags;
    }

    private void updateTagCounts(Set<Tag> tags) {
        for (Tag tag : tags) {
            long count = postRepository.findByTagName(tag.getName()).size();
            tag.setPostCount((int) count);
            tagRepository.save(tag);
        }
    }

    // ============================================
    // POMOĆNE METODE - CLEANUP
    // ============================================
    
    private void cleanupFailedUpload(String videoFileName, String thumbnailFileName) {
        if (videoFileName != null) {
            try {
                fileStorageService.deleteVideoFile(videoFileName);
                System.out.println("🗑️ Rollback: Video fajl obrisan");
            } catch (Exception e) {
                System.err.println("⚠️ Ne mogu obrisati video fajl: " + videoFileName);
            }
        }
        
        if (thumbnailFileName != null) {
            try {
                fileStorageService.deleteThumbnailFile(thumbnailFileName);
                System.out.println("🗑️ Rollback: Thumbnail obrisan");
            } catch (Exception e) {
                System.err.println("⚠️ Ne mogu obrisati thumbnail: " + thumbnailFileName);
            }
        }
    }

    // ============================================
    // KONVERZIJA - Post -> PostDTO
    // ============================================
    
    private PostDTO convertToDTO(Post post) {
        return new PostDTO(post);
    }

    private String extractFileName(String url) {
        int lastSlashIndex = url.lastIndexOf('/');
        return url.substring(lastSlashIndex + 1);
    }
}