package controller;

import dto.PostDTO;
import model.Post;
import repository.PostRepository;
import service.FileStorageService;
import service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PostRepository postRepository;

    // ============================================
    // POST /api/posts - UPLOAD VIDEO OBJAVE (3.3)
    // ============================================
    
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("thumbnail") MultipartFile thumbnailFile,
            @RequestParam(value = "tags", required = false) String tagsString,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "locationName", required = false) String locationName,
            Authentication authentication) {
        
        System.out.println("🔥 POST /api/posts - Upload video objave");
        
        try {
            // ============================================
            // KORAK 1: Provera autentifikacije (3.3 - samo registrovani)
            // ============================================
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Morate biti prijavljeni da biste postavili video! (3.3 zahtev)");
            }

            String username = authentication.getName();
            System.out.println("👤 Korisnik: " + username);

            // ============================================
            // KORAK 2: Parsiranje tagova iz string-a
            // ============================================
            
            Set<String> tagNames = new HashSet<>();
            if (tagsString != null && !tagsString.trim().isEmpty()) {
                String[] tagsArray = tagsString.split(",");
                for (String tag : tagsArray) {
                    String trimmed = tag.trim();
                    if (!trimmed.isEmpty()) {
                        tagNames.add(trimmed);
                    }
                }
                System.out.println("🏷️ Tagovi: " + tagNames);
            }

            // ============================================
            // KORAK 3: Logovanje upload informacija
            // ============================================
            
            System.out.println("🎹 Video: " + videoFile.getOriginalFilename() + 
                             " (" + formatFileSize(videoFile.getSize()) + ")");
            System.out.println("🖼️ Thumbnail: " + thumbnailFile.getOriginalFilename() + 
                             " (" + formatFileSize(thumbnailFile.getSize()) + ")");
            
            if (latitude != null && longitude != null) {
                System.out.println("📍 Lokacija: " + locationName + " (" + latitude + ", " + longitude + ")");
            }

            // ============================================
            // KORAK 4: Poziv PostService - @Transactional upload
            // ============================================
            
            PostDTO post = postService.createPost(
                username, 
                title, 
                description,
                videoFile, 
                thumbnailFile,
                tagNames,
                latitude,
                longitude,
                locationName
            );

            // ============================================
            // KORAK 5: Vraćanje odgovora (201 CREATED)
            // ============================================
            
            System.out.println("✅ Post kreiran! ID: " + post.getId());
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(post);

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Upload nije uspeo: " + e.getMessage());
        }
    }

    // ============================================
    // GET /api/posts - SVI POSTOVI (3.1 - za HOME feed)
    // ============================================
    
    @GetMapping("/posts")
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        System.out.println("📋 GET /api/posts - Svi postovi");
        
        List<PostDTO> posts = postService.getAllPosts();
        
        System.out.println("✅ Vraćeno " + posts.size() + " postova");
        return ResponseEntity.ok(posts);
    }

    // ============================================
    // GET /api/posts/{id} - JEDAN POST (3.1) - SA VIEW INCREMENT
    // ============================================
    
    @GetMapping("/posts/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        System.out.println("🎬 GET /api/posts/" + id);
        
        try {
            PostDTO post = postService.getPostById(id);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Post nije pronađen: " + e.getMessage());
        }
    }

    // ============================================
    // GET /api/posts/{id}/refresh - REFRESH POST (BEZ VIEW INCREMENT) - NOVO! 🔄
    // ============================================
    
    /**
     * Dobija post BEZ incrementa view count-a.
     * Koristi se za refresh nakon komentara/lajkova.
     */
    @GetMapping("/posts/{id}/refresh")
    public ResponseEntity<?> refreshPost(@PathVariable Long id) {
        System.out.println("🔄 GET /api/posts/" + id + "/refresh (bez view increment)");
        
        try {
            // Pozovi repository direktno (bez incrementa)
            Post post = postRepository.findByIdWithAssociations(id)
                    .orElseThrow(() -> new RuntimeException("Post nije pronađen: " + id));
            
            PostDTO dto = new PostDTO(post);
            return ResponseEntity.ok(dto);
            
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Post nije pronađen: " + e.getMessage());
        }
    }

    // ============================================
    // GET /api/posts/user/{username} - POSTOVI KORISNIKA
    // ============================================
    
    @GetMapping("/posts/user/{username}")
    public ResponseEntity<List<PostDTO>> getUserPosts(@PathVariable String username) {
        System.out.println("👤 GET /api/posts/user/" + username);
        
        List<PostDTO> posts = postService.getUserPosts(username);
        System.out.println("✅ Korisnik " + username + " ima " + posts.size() + " postova");
        
        return ResponseEntity.ok(posts);
    }

    // ============================================
    // GET /api/posts/tag/{tagName} - POSTOVI PO TAGU
    // ============================================
    
    @GetMapping("/posts/tag/{tagName}")
    public ResponseEntity<List<PostDTO>> getPostsByTag(@PathVariable String tagName) {
        System.out.println("🏷️ GET /api/posts/tag/" + tagName);
        
        List<PostDTO> posts = postService.searchByTag(tagName);
        System.out.println("✅ Tag '" + tagName + "' ima " + posts.size() + " postova");
        
        return ResponseEntity.ok(posts);
    }

    // ============================================
    // DELETE /api/posts/{id} - BRISANJE POSTA
    // ============================================
    
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id, Authentication authentication) {
        System.out.println("🗑️ DELETE /api/posts/" + id);
        
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Morate biti prijavljeni!");
            }

            String username = authentication.getName();
            postService.deletePost(id, username);

            System.out.println("✅ Post obrisan");
            return ResponseEntity.ok("Post uspešno obrisan!");

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    // ============================================
    // POST /api/posts/{id}/like - LAJKUJ POST ❤️
    // ============================================
    
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<?> likePost(@PathVariable Long id, Authentication authentication) {
        System.out.println("❤️ POST /api/posts/" + id + "/like");
        
        try {
            // Provera autentifikacije - SAMO REGISTROVANI!
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "error", "Morate biti prijavljeni da biste lajkovali post!",
                            "liked", false
                        ));
            }

            String email = authentication.getName();
            boolean success = postService.likePost(id, email);

            if (success) {
                System.out.println("✅ Post lajkovan");
                return ResponseEntity.ok().body(Map.of(
                    "message", "Post uspešno lajkovan!",
                    "liked", true
                ));
            } else {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(Map.of(
                            "message", "Već ste lajkovali ovaj post!",
                            "liked", true
                        ));
            }

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // DELETE /api/posts/{id}/like - UKLONI LAJK 💔
    // ============================================
    
    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<?> unlikePost(@PathVariable Long id, Authentication authentication) {
        System.out.println("💔 DELETE /api/posts/" + id + "/like");
        
        try {
            // Provera autentifikacije - SAMO REGISTROVANI!
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                            "error", "Morate biti prijavljeni!",
                            "liked", false
                        ));
            }

            String email = authentication.getName();
            boolean success = postService.unlikePost(id, email);

            if (success) {
                System.out.println("✅ Like uklonjen");
                return ResponseEntity.ok().body(Map.of(
                    "message", "Like uspešno uklonjen!",
                    "liked", false
                ));
            } else {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "message", "Niste lajkovali ovaj post!",
                            "liked", false
                        ));
            }

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // GET /api/posts/{id}/like/status - PROVERA DA LI JE LAJKOVAN
    // ============================================
    
    @GetMapping("/posts/{id}/like/status")
    public ResponseEntity<?> getLikeStatus(@PathVariable Long id, Authentication authentication) {
        System.out.println("🔍 GET /api/posts/" + id + "/like/status");
        
        try {
            String email = (authentication != null && authentication.isAuthenticated()) 
                ? authentication.getName() 
                : null;

            boolean isLiked = postService.isPostLikedByUser(id, email);

            return ResponseEntity.ok().body(Map.of(
                "postId", id,
                "isLiked", isLiked
            ));

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================
    // GET /api/videos/{filename} - STREAMING VIDEA (3.1)
    // ============================================
    
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
        System.out.println("▶️ GET /api/videos/" + filename);
        
        try {
            Resource resource = fileStorageService.loadVideoAsResource(filename);

            String contentType = "video/mp4";
            
            if (filename.endsWith(".webm")) {
                contentType = "video/webm";
            } else if (filename.endsWith(".avi")) {
                contentType = "video/x-msvideo";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (RuntimeException e) {
            System.err.println("❌ Video nije pronađen: " + filename);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    // ============================================
    // GET /api/thumbnails/{filename} - THUMBNAIL SLIKA (3.1)
    // ============================================
    
    @GetMapping("/thumbnails/{filename:.+}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String filename) {
        try {
            Resource resource = fileStorageService.loadThumbnailAsResource(filename);

            String contentType = "image/jpeg";
            
            if (filename.endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (filename.endsWith(".webp")) {
                contentType = "image/webp";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    // ============================================
    // GET /api/posts/test - TEST ENDPOINT
    // ============================================
    
    @GetMapping("/posts/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("🎬 Post API radi! Backend je spreman za 3.1 i 3.3!");
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}