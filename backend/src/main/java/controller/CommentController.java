package controller;

import dto.CommentDTO;
import service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CommentController - REST API za komentare (3.6 zahtev)
 * 
 * ENDPOINT-I:
 * POST   /api/posts/{postId}/comments       - Kreiraj komentar (samo registrovani)
 * GET    /api/posts/{postId}/comments       - Dobij komentare (javno, paginacija)
 * DELETE /api/comments/{commentId}          - Obriši komentar (samo vlasnik)
 * GET    /api/comments/test                 - Test endpoint
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // ============================================
    // POST /api/posts/{postId}/comments
    // KREIRANJE KOMENTARA (3.6 - samo registrovani)
    // ============================================
    
    /**
     * Kreira novi komentar na postu.
     * 
     * ZAHTEVI (3.6):
     * - Samo registrovani korisnici (JWT autentifikacija)
     * - Rate limiting: 60 komentara po satu
     * - Tekst obavezan
     * 
     * REQUEST BODY:
     * {
     *   "text": "Odličan video!"
     * }
     * 
     * RESPONSE (201 CREATED):
     * {
     *   "id": 123,
     *   "text": "Odličan video!",
     *   "username": "petar123",
     *   "createdAt": "2026-01-28T20:30:00"
     * }
     * 
     * @param postId - ID posta
     * @param requestBody - Map sa "text" poljem
     * @param authentication - Automatski injektovan (iz JWT tokena)
     * @return ResponseEntity<CommentDTO>
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        
        System.out.println("📥 POST /api/posts/" + postId + "/comments - Kreiranje komentara");

        try {
            // ============================================
            // KORAK 1: Provera autentifikacije
            // ============================================
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Morate biti prijavljeni da biste komentarisali! (3.6 zahtev)");
            }

            String email = authentication.getName();
            System.out.println("👤 Korisnik: " + email);

            // ============================================
            // KORAK 2: Izvlačenje teksta iz request body-a
            // ============================================
            
            String text = requestBody.get("text");
            
            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("Tekst komentara je obavezan!");
            }

            System.out.println("💬 Tekst: " + (text.length() > 50 ? text.substring(0, 50) + "..." : text));

            // ============================================
            // KORAK 3: Poziv servisa
            // ============================================
            
            CommentDTO comment = commentService.createComment(postId, email, text);

            System.out.println("✅ Komentar kreiran - ID: " + comment.getId());

            // ============================================
            // KORAK 4: Vraćanje odgovora (201 CREATED)
            // ============================================
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(comment);

        } catch (RuntimeException e) {
            // Rate limit ili druga greška
            System.err.println("❌ Greška: " + e.getMessage());
            
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ============================================
    // GET /api/posts/{postId}/comments
    // DOBIJANJE KOMENTARA (3.6 - javno dostupno, paginacija)
    // ============================================
    
    /**
     * Vraća komentare za post sa paginacijom.
     * 
     * JAVNO DOSTUPNO (3.6):
     * - I neautentifikovani korisnici mogu videti komentare
     * 
     * PAGINACIJA (3.6):
     * - Query parametar: ?page=0 (default)
     * - Vraća 20 komentara po stranici
     * - Ukupno stranica i komentara u response-u
     * 
     * SORTIRANJE (3.6):
     * - Najnoviji → najstariji
     * 
     * KEŠIRANJE (3.6):
     * - @Cacheable u servisu
     * 
     * RESPONSE (200 OK):
     * {
     *   "content": [
     *     { "id": 1, "text": "...", "username": "...", "createdAt": "..." },
     *     { "id": 2, "text": "...", "username": "...", "createdAt": "..." }
     *   ],
     *   "totalElements": 287,
     *   "totalPages": 15,
     *   "number": 0,
     *   "size": 20,
     *   "first": true,
     *   "last": false,
     *   "numberOfElements": 20
     * }
     * 
     * @param postId - ID posta
     * @param page - Broj stranice (default 0)
     * @return ResponseEntity<Page<CommentDTO>>
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page) {
        
        System.out.println("📖 GET /api/posts/" + postId + "/comments?page=" + page);

        try {
            // Poziv servisa (javno dostupno - 3.6 zahtev)
            Page<CommentDTO> comments = commentService.getCommentsByPost(postId, page);

            System.out.println("✅ Vraćeno " + comments.getNumberOfElements() + " komentara");
            System.out.println("   Ukupno: " + comments.getTotalElements() + " komentara");

            // Vraćanje Page objekta (Spring automatski konvertuje u JSON)
            return ResponseEntity.ok(comments);

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ============================================
    // DELETE /api/comments/{commentId}
    // BRISANJE KOMENTARA (3.6 - samo vlasnik)
    // ============================================
    
    /**
     * Briše komentar.
     * 
     * ZAHTEVI (3.6):
     * - Samo vlasnik može obrisati svoj komentar
     * - JWT autentifikacija
     * 
     * @param commentId - ID komentara
     * @param authentication - Automatski injektovan
     * @return ResponseEntity<String>
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        
        System.out.println("🗑️ DELETE /api/comments/" + commentId);

        try {
            // Provera autentifikacije
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body("Morate biti prijavljeni!");
            }

            String email = authentication.getName();
            System.out.println("👤 Korisnik: " + email);

            // Poziv servisa (proverava vlasništvo)
            commentService.deleteComment(commentId, email);

            System.out.println("✅ Komentar obrisan");

            return ResponseEntity.ok("Komentar uspešno obrisan!");

        } catch (RuntimeException e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    // ============================================
    // GET /api/comments/test
    // TEST ENDPOINT
    // ============================================
    
    @GetMapping("/comments/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("💬 Comment API radi! Spremni za 3.6!");
    }

    // ============================================
    // HELPER METODE
    // ============================================
    
    /**
     * Kreira error response kao JSON objekat.
     * 
     * @param message - Error poruka
     * @return Map<String, String>
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}