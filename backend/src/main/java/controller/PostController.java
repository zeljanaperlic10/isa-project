package controller;

import dto.PostDTO;
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
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private FileStorageService fileStorageService;

    // ============================================
    // POST /api/posts - UPLOAD VIDEO OBJAVE (3.3)
    // ============================================
    
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("thumbnail") MultipartFile thumbnailFile,
            @RequestParam(value = "tags", required = false) String tagsString, // "programiranje,python,tutorial"
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "locationName", required = false) String locationName,
            Authentication authentication) {
        
        System.out.println("📥 POST /api/posts - Upload video objave");
        
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
            
            System.out.println("📹 Video: " + videoFile.getOriginalFilename() + 
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
        
        // NAPOMENA: Javno dostupno - i neautentifikovani mogu videti (3.1 zahtev)
        List<PostDTO> posts = postService.getAllPosts();
        
        System.out.println("✅ Vraćeno " + posts.size() + " postova");
        return ResponseEntity.ok(posts);
    }

    // ============================================
    // GET /api/posts/{id} - JEDAN POST (3.1)
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
            // Provera autentifikacije
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
    // GET /api/videos/{filename} - STREAMING VIDEA (3.1)
    // ============================================
    
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> streamVideo(@PathVariable String filename) {
        System.out.println("▶️ GET /api/videos/" + filename);
        
        try {
            // Učitavanje video fajla
            Resource resource = fileStorageService.loadVideoAsResource(filename);

            // Određivanje content type-a
            String contentType = "video/mp4";  // Default (3.3 zahtev - samo MP4)
            
            if (filename.endsWith(".webm")) {
                contentType = "video/webm";
            } else if (filename.endsWith(".avi")) {
                contentType = "video/x-msvideo";
            }

            // Vraćanje videa sa headerima za streaming
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
        // NAPOMENA: Thumbnail će biti keširan u CacheConfig.java (3.3 zahtev)
        
        try {
            Resource resource = fileStorageService.loadThumbnailAsResource(filename);

            // Određivanje content type-a
            String contentType = "image/jpeg";  // Default
            
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