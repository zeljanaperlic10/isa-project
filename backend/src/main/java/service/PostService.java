package service;

import dto.PostDTO;
import model.Post;
import model.Tag;
import model.User;
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

    // ============================================
    // KREIRANJE POSTA - @TRANSACTIONAL (3.3 zahtev)
    // ============================================
    
    /**
     * Kreira novi post sa video upload-om.
     * @Transactional osigurava da:
     * - Ako upload ne uspe → ROLLBACK (brisanje iz baze)
     * - Ako ne završi u predviđenom vremenu → ROLLBACK
     * - Sve ili ništa (atomicity)
     */
    @Transactional(timeout = 120) // 120 sekundi timeout (3.3 zahtev)
    public PostDTO createPost(
            String username, 
            String title, 
            String description,
            MultipartFile videoFile, 
            MultipartFile thumbnailFile,
            Set<String> tagNames,  // Tagovi kao Set<String>
            Double latitude,       // Geografska lokacija (opciono)
            Double longitude,
            String locationName) {
        
        System.out.println("🎬 Kreiranje posta - START");
        
        try {
            // ============================================
            // KORAK 1: Pronalaženje korisnika
            // ============================================
            
            // NAPOMENA: username parametar zapravo sadrži EMAIL (zbog JWT tokena)
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Korisnik nije pronađen: " + username);
            }
            User user = userOpt.get();
            System.out.println("✅ Korisnik pronađen: " + user.getUsername() + " (email: " + username + ")");

            // ============================================
            // KORAK 2: Validacija naslova (3.3 zahtev)
            // ============================================
            
            if (title == null || title.trim().isEmpty()) {
                throw new RuntimeException("Naslov je obavezan! (3.3 zahtev)");
            }
            if (title.length() > 200) {
                throw new RuntimeException("Naslov može imati maksimum 200 karaktera!");
            }
            System.out.println("✅ Naslov validan: " + title);

            // ============================================
            // KORAK 3: UPLOAD VIDEO FAJLA (3.3 zahtev)
            // ============================================
            
            System.out.println("📤 Upload videa u toku...");
            String videoFileName = fileStorageService.storeVideoFile(videoFile);
            String videoUrl = "/api/videos/" + videoFileName;
            System.out.println("✅ Video uploadovan: " + videoFileName);

            // ============================================
            // KORAK 4: UPLOAD THUMBNAIL SLIKE (3.3 zahtev)
            // ============================================
            
            System.out.println("📤 Upload thumbnail-a u toku...");
            String thumbnailFileName = fileStorageService.storeThumbnailFile(thumbnailFile);
            String thumbnailUrl = "/api/thumbnails/" + thumbnailFileName;
            System.out.println("✅ Thumbnail uploadovan: " + thumbnailFileName);

            // ============================================
            // KORAK 5: Kreiranje Post objekta
            // ============================================
            
            Post post = new Post();
            post.setUser(user);
            post.setTitle(title.trim());
            post.setDescription(description != null ? description.trim() : null);
            post.setVideoUrl(videoUrl);
            post.setThumbnailUrl(thumbnailUrl);
            post.setVideoFileName(videoFileName);
            post.setFileSize(videoFile.getSize());
            post.setDuration(null); // TODO: Možemo dodati detekciju trajanja videa
            
            // ============================================
            // KORAK 6: TAGOVI (3.3 zahtev)
            // ============================================
            
            if (tagNames != null && !tagNames.isEmpty()) {
                System.out.println("🏷️ Procesiranje tagova: " + tagNames);
                Set<Tag> tags = processTagsString(tagNames);
                post.setTags(tags);
                System.out.println("✅ Tagovi dodati: " + tags.size() + " tagova");
            }

            // ============================================
            // KORAK 7: GEOGRAFSKA LOKACIJA (opciono - 3.3 zahtev)
            // ============================================
            
            if (latitude != null && longitude != null) {
                // Validacija koordinata
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

            // ============================================
            // KORAK 8: Čuvanje u bazi (3.3 zahtev - sistemsko vreme)
            // ============================================
            
            Post savedPost = postRepository.save(post);
            System.out.println("✅ Post sačuvan u bazi - ID: " + savedPost.getId());

            // ============================================
            // KORAK 9: Ažuriranje brojača tagova
            // ============================================
            
            updateTagCounts(savedPost.getTags());

            System.out.println("🎉 Post uspešno kreiran! ID: " + savedPost.getId());
            
            // ============================================
            // KORAK 10: Konverzija u DTO i vraćanje
            // ============================================
            
            return convertToDTO(savedPost);
            
        } catch (Exception e) {
            // AKO SE DESI GREŠKA → @Transactional ROLLBACK!
            System.err.println("❌ Greška pri kreiranju posta: " + e.getMessage());
            
            // Pokušaj brisanja uploadovanih fajlova (cleanup)
            // Ovo je sigurnosna mera - @Transactional već radi rollback u bazi
            cleanupFailedUpload(null, null);
            
            throw new RuntimeException("Upload video objave nije uspeo: " + e.getMessage(), e);
        }
    }

    // ============================================
    // DOBIJANJE SVIH POSTOVA (za HOME feed)
    // ============================================
    
    public List<PostDTO> getAllPosts() {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        return posts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ============================================
    // DOBIJANJE JEDNOG POSTA
    // ============================================
    
    public PostDTO getPostById(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen! ID: " + postId);
        }
        
        // Povećaj broj pregleda
        Post post = postOpt.get();
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
        
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

        // Provera vlasništva - poredimo EMAIL!
        if (!post.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Nemate pravo da obrišete ovaj post!");
        }

        // Brisanje fajlova
        String videoFileName = extractFileName(post.getVideoUrl());
        String thumbnailFileName = extractFileName(post.getThumbnailUrl());
        
        fileStorageService.deleteVideoFile(videoFileName);
        fileStorageService.deleteThumbnailFile(thumbnailFileName);

        // Brisanje iz baze
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
    // BROJAČI (lajkovi, komentari)
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

    public void incrementCommentsCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(post.getCommentsCount() + 1);
            postRepository.save(post);
        }
    }

    public void decrementCommentsCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        }
    }

    // ============================================
    // POMOĆNE METODE - TAGOVI
    // ============================================
    
    /**
     * Parsira string tagove i kreira/pronalazi Tag entitete.
     * Ako tag ne postoji → kreira ga
     * Ako postoji → koristi postojeći
     */
    private Set<Tag> processTagsString(Set<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        
        for (String tagName : tagNames) {
            if (tagName == null || tagName.trim().isEmpty()) {
                continue;
            }
            
            // Normalizacija: mala slova, trim
            String normalizedTagName = tagName.toLowerCase().trim();
            
            // Validacija dužine
            if (normalizedTagName.length() > 50) {
                throw new RuntimeException("Tag je predugačak (max 50 karaktera): " + normalizedTagName);
            }
            
            // Pronađi ili kreiraj tag
            Optional<Tag> existingTag = tagRepository.findByName(normalizedTagName);
            
            Tag tag;
            if (existingTag.isPresent()) {
                // Tag već postoji
                tag = existingTag.get();
                System.out.println("   ✓ Tag pronađen: " + normalizedTagName);
            } else {
                // Kreiraj novi tag
                tag = new Tag(normalizedTagName);
                tag = tagRepository.save(tag);
                System.out.println("   ✓ Tag kreiran: " + normalizedTagName);
            }
            
            tags.add(tag);
        }
        
        return tags;
    }

    /**
     * Ažurira brojače postova za svaki tag
     */
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
    
    /**
     * Čisti fajlove ako upload ne uspe (3.3 zahtev - rollback)
     */
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

    // Izvlačenje imena fajla iz URL-a
    private String extractFileName(String url) {
        int lastSlashIndex = url.lastIndexOf('/');
        return url.substring(lastSlashIndex + 1);
    }
}