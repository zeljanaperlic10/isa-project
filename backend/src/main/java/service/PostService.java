package service;

import dto.PostDTO;
import model.Post;
import model.PostLike;
import model.Tag;
import model.UploadEvent;
import model.User;
import repository.PostLikeRepository;
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

    @Autowired
    private JsonMessageProducer jsonMessageProducer;

    @Autowired
    private PostLikeRepository postLikeRepository; // ← DODATO!

    // ============================================
    // KREIRANJE POSTA - @TRANSACTIONAL (3.3 zahtev)
    // POPRAVLJENO: Rollback sada pravilno briše fajlove!
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
        
        // VAŽNO: Pamtimo imena fajlova za rollback! (3.3 zahtev)
        String videoFileName = null;
        String thumbnailFileName = null;
        
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

            // KORAK 3: UPLOAD VIDEO FAJLA (3.3 zahtev - max 200MB, mp4)
            System.out.println("📤 Upload videa u toku...");
            videoFileName = fileStorageService.storeVideoFile(videoFile);
            String videoUrl = "/api/videos/" + videoFileName;
            System.out.println("✅ Video uploadovan: " + videoFileName);

            // KORAK 4: UPLOAD THUMBNAIL SLIKE (3.3 zahtev)
            System.out.println("📤 Upload thumbnail-a u toku...");
            thumbnailFileName = fileStorageService.storeThumbnailFile(thumbnailFile);
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
            
            // KORAK 6: TAGOVI (3.3 zahtev)
            if (tagNames != null && !tagNames.isEmpty()) {
                System.out.println("🏷️ Procesiranje tagova: " + tagNames);
                Set<Tag> tags = processTagsString(tagNames);
                post.setTags(tags);
                System.out.println("✅ Tagovi dodati: " + tags.size() + " tagova");
            }

            // KORAK 7: GEOGRAFSKA LOKACIJA (3.3 zahtev - opciono)
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

            // KORAK 8: Čuvanje u bazi (3.3 zahtev - transakciono)
            Post savedPost = postRepository.save(post);
            System.out.println("✅ Post sačuvan u bazi - ID: " + savedPost.getId());

            // KORAK 9: Ažuriranje brojača tagova
            updateTagCounts(savedPost.getTags());

            // KORAK 10: RabbitMQ poruka (3.14 zahtev - JSON format)
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
                // Ne baca exception - ako RabbitMQ ne radi, upload nastavlja
            }

            System.out.println("🎉 Post uspešno kreiran! ID: " + savedPost.getId());
            
            return convertToDTO(savedPost);
            
        } catch (Exception e) {
            System.err.println("❌ Greška pri kreiranju posta: " + e.getMessage());
            
            // ROLLBACK: Obrišimo fajlove sa file sistema! (3.3 zahtev)
            cleanupFailedUpload(videoFileName, thumbnailFileName);
            
            throw new RuntimeException("Upload video objave nije uspeo: " + e.getMessage(), e);
        }
    }

    // ============================================
    // DOBIJANJE SVIH POSTOVA - SA EAGER FETCH
    // ============================================
    
    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDescWithAssociations();
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // DOBIJANJE JEDNOG POSTA (3.7 - sa atomic increment)
    // POPRAVLJENO: Eager fetch + pravilna atomic operacija!
    // ============================================
    
    @Transactional
    public PostDTO getPostById(Long postId) {
        System.out.println("🔍 [getPostById] START - ID: " + postId);
        
        // EAGER FETCH - učitava sve asocijacije (tags, user)
        Post post = postRepository.findByIdWithAssociations(postId)
                .orElseThrow(() -> {
                    System.err.println("❌ Post nije pronađen: " + postId);
                    return new RuntimeException("Post nije pronađen! ID: " + postId);
                });
        
        System.out.println("✅ Post pronađen: " + post.getTitle());
        
        // ATOMIC INCREMENT (3.7 zahtev - thread-safe!)
        incrementViewCount(postId);
        
        // Refresh post da dobijemo novi viewsCount
        post = postRepository.findByIdWithAssociations(postId).get();
        
        System.out.println("✅ View count: " + post.getViewsCount());
        
        return convertToDTO(post);
    }

    // ============================================
    // DOBIJANJE POSTOVA KORISNIKA - SA EAGER FETCH
    // ============================================
    
    public List<PostDTO> getUserPosts(String username) {
        List<Post> posts = postRepository.findByUserUsernameOrderByCreatedAtDescWithAssociations(username);
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // BRISANJE POSTA (3.3 - sa autorizacijom)
    // ============================================
    
    @Transactional
    public void deletePost(Long postId, String email) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen! ID: " + postId);
        }

        Post post = postOpt.get();

        // AUTORIZACIJA: Samo vlasnik može da obriše svoj post!
        if (!post.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Nemate pravo da obrišete ovaj post!");
        }

        String videoFileName = extractFileName(post.getVideoUrl());
        String thumbnailFileName = extractFileName(post.getThumbnailUrl());
        
        // Brisanje fajlova sa file sistema
        fileStorageService.deleteVideoFile(videoFileName);
        fileStorageService.deleteThumbnailFile(thumbnailFileName);

        // Brisanje iz baze
        postRepository.deleteById(postId);
        
        System.out.println("🗑️ Post obrisan: ID=" + postId);
    }

    // ============================================
    // PRETRAGA PO TAGOVIMA - SA EAGER FETCH
    // ============================================
    
    public List<PostDTO> searchByTag(String tagName) {
        List<Post> posts = postRepository.findByTagNameWithAssociations(tagName.toLowerCase());
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // LAJKOVANJE (LIKE/UNLIKE) - NOVO! ❤️
    // ============================================

    /**
     * Lajkuje post (3.3 zahtev - lajkovanje objave)
     * 
     * @param postId - ID posta
     * @param email - Email korisnika koji lajkuje
     * @return true ako je uspešno lajkovano, false ako je već lajkovano
     */
    @Transactional
    public boolean likePost(Long postId, String email) {
        System.out.println("❤️ Like post - postId: " + postId + ", user: " + email);
        
        // Pronađi korisnika
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen: " + email);
        }
        User user = userOpt.get();
        
        // Pronađi post
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen: " + postId);
        }
        Post post = postOpt.get();
        
        // Proveri da li je već lajkovao
        if (postLikeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            System.out.println("⚠️ Korisnik je već lajkovao ovaj post!");
            return false;
        }
        
        // Kreiraj like
        PostLike like = new PostLike(user, post);
        postLikeRepository.save(like);
        
        // Inkrementiraj likesCount na postu
        incrementLikesCount(postId);
        
        System.out.println("✅ Post lajkovan!");
        return true;
    }

    /**
     * Uklanja lajk sa posta (unlike)
     * 
     * @param postId - ID posta
     * @param email - Email korisnika koji uklanja lajk
     * @return true ako je uspešno uklonjeno, false ako lajk nije postojao
     */
    @Transactional
    public boolean unlikePost(Long postId, String email) {
        System.out.println("💔 Unlike post - postId: " + postId + ", user: " + email);
        
        // Pronađi korisnika
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen: " + email);
        }
        User user = userOpt.get();
        
        // Pronađi like
        Optional<PostLike> likeOpt = postLikeRepository.findByUserIdAndPostId(user.getId(), postId);
        
        if (!likeOpt.isPresent()) {
            System.out.println("⚠️ Like nije pronađen!");
            return false;
        }
        
        // Obriši like
        postLikeRepository.delete(likeOpt.get());
        
        // Dekrementiraj likesCount na postu
        decrementLikesCount(postId);
        
        System.out.println("✅ Like uklonjen!");
        return true;
    }

    /**
     * Proverava da li je korisnik lajkovao post
     * 
     * @param postId - ID posta
     * @param email - Email korisnika (može biti null za neautentifikovane)
     * @return true ako je lajkovao, false ako nije ili nije prijavljen
     */
    public boolean isPostLikedByUser(Long postId, String email) {
        if (email == null) {
            return false;
        }
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return false;
        }
        
        return postLikeRepository.existsByUserIdAndPostId(userOpt.get().getId(), postId);
    }

    // ============================================
    // BROJAČI - LAJKOVI
    // ============================================
    
    @Transactional
    public void incrementLikesCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setLikesCount(post.getLikesCount() + 1);
            postRepository.save(post);
        }
    }

    @Transactional
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
    // BROJAČ PREGLEDA (3.7 zahtev - atomic increment)
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
    // POMOĆNE METODE - CLEANUP (3.3 zahtev - rollback)
    // ============================================
    
    private void cleanupFailedUpload(String videoFileName, String thumbnailFileName) {
        if (videoFileName != null) {
            try {
                fileStorageService.deleteVideoFile(videoFileName);
                System.out.println("🗑️ Rollback: Video fajl obrisan - " + videoFileName);
            } catch (Exception e) {
                System.err.println("⚠️ Ne mogu obrisati video fajl: " + videoFileName);
            }
        }
        
        if (thumbnailFileName != null) {
            try {
                fileStorageService.deleteThumbnailFile(thumbnailFileName);
                System.out.println("🗑️ Rollback: Thumbnail obrisan - " + thumbnailFileName);
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