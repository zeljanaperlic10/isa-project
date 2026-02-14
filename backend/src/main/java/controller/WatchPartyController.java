package controller;

import model.WatchParty;
import service.WatchPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WatchPartyController - REST API za Watch Party (3.15 zahtev)
 * 
 * ENDPOINT-i:
 * - POST   /api/watch-party/create       → Kreira sobu
 * - GET    /api/watch-party/active       → Lista aktivnih soba
 * - GET    /api/watch-party/my-rooms     → Sobe koje sam kreirao
 * - GET    /api/watch-party/joined       → Sobe gde sam član
 * - GET    /api/watch-party/{id}         → Jedna soba
 * - POST   /api/watch-party/{id}/join    → Pridruži se sobi
 * - POST   /api/watch-party/{id}/leave   → Napusti sobu
 * - DELETE /api/watch-party/{id}/close   → Zatvori sobu
 * 
 * NAPOMENA:
 * - Pokretanje videa se NE radi ovde!
 * - Pokretanje videa je preko WebSocket-a (real-time)
 * - Ovde su samo CRUD operacije
 */
@RestController
@RequestMapping("/api/watch-party")
@CrossOrigin(origins = "http://localhost:4200")
public class WatchPartyController {

    @Autowired
    private WatchPartyService watchPartyService;

    // ============================================
    // KREIRANJE SOBE
    // ============================================

    /**
     * Kreira novu Watch Party sobu.
     * 
     * HTTP REQUEST:
     * POST /api/watch-party/create
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * Body: { "name": "Movie Night" }
     * 
     * HTTP RESPONSE (201 Created):
     * {
     *   "id": 123,
     *   "name": "Movie Night",
     *   "creator": { "id": 5, "username": "petar", ... },
     *   "currentPost": null,
     *   "active": true,
     *   "createdAt": "2026-02-02T12:00:00",
     *   "members": ["petar"],
     *   "memberCount": 1
     * }
     * 
     * AUTENTIFIKACIJA:
     * - Authentication objekat sadrži podatke o ulogovanom korisniku
     * - Spring Security automatski kreira ovaj objekat iz JWT tokena
     * - authentication.getName() → username korisnika
     * 
     * @param request - { "name": "..." }
     * @param authentication - Ulogovani korisnik (Spring Security)
     * @return 201 Created sa WatchParty objektom
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        try {
            System.out.println("📥 POST /api/watch-party/create");
            
            // Uzmi username iz JWT tokena
            String username = authentication.getName();
            String roomName = request.get("name");
            
            System.out.println("   Korisnik: " + username);
            System.out.println("   Naziv: " + roomName);
            
            // Pozovi Service
            WatchParty party = watchPartyService.createRoom(username, roomName);
            
            System.out.println("✅ Soba kreirana! ID: " + party.getId());
            
            // Vrati 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body(party);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============================================
    // LISTA SOBA
    // ============================================

    /**
     * Sve aktivne sobe (za homepage).
     * 
     * HTTP REQUEST:
     * GET /api/watch-party/active
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * [
     *   { "id": 123, "name": "Movie Night", "creator": {...}, "memberCount": 3 },
     *   { "id": 124, "name": "Study Session", "creator": {...}, "memberCount": 5 }
     * ]
     * 
     * @return 200 OK sa listom soba
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveRooms() {
        try {
            System.out.println("📥 GET /api/watch-party/active");
            
            List<WatchParty> rooms = watchPartyService.getActiveRooms();
            
            System.out.println("✅ Vraćeno " + rooms.size() + " soba");
            
            return ResponseEntity.ok(rooms);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Sobe koje je korisnik kreirao.
     * 
     * HTTP REQUEST:
     * GET /api/watch-party/my-rooms
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * [
     *   { "id": 123, "name": "My Room", "active": true, ... }
     * ]
     * 
     * @param authentication - Ulogovani korisnik
     * @return 200 OK sa listom soba
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<?> getMyRooms(Authentication authentication) {
        try {
            System.out.println("📥 GET /api/watch-party/my-rooms");
            
            String username = authentication.getName();
            
            System.out.println("   Korisnik: " + username);
            
            List<WatchParty> rooms = watchPartyService.getRoomsByCreator(username);
            
            System.out.println("✅ Vraćeno " + rooms.size() + " soba");
            
            return ResponseEntity.ok(rooms);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Sobe gde je korisnik član.
     * 
     * HTTP REQUEST:
     * GET /api/watch-party/joined
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * [
     *   { "id": 125, "name": "Friend's Room", "creator": {...}, ... }
     * ]
     * 
     * @param authentication - Ulogovani korisnik
     * @return 200 OK sa listom soba
     */
    @GetMapping("/joined")
    public ResponseEntity<?> getJoinedRooms(Authentication authentication) {
        try {
            System.out.println("📥 GET /api/watch-party/joined");
            
            String username = authentication.getName();
            
            System.out.println("   Korisnik: " + username);
            
            List<WatchParty> rooms = watchPartyService.getRoomsWhereUserIsMember(username);
            
            System.out.println("✅ Vraćeno " + rooms.size() + " soba");
            
            return ResponseEntity.ok(rooms);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Jedna soba po ID-ju.
     * 
     * HTTP REQUEST:
     * GET /api/watch-party/123
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * {
     *   "id": 123,
     *   "name": "Movie Night",
     *   "creator": {...},
     *   "currentPost": { "id": 10, "title": "My Video", ... },
     *   "members": ["petar", "stefan", "ana"],
     *   "memberCount": 3
     * }
     * 
     * @param roomId - ID sobe (iz URL-a)
     * @return 200 OK sa WatchParty objektom
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomById(@PathVariable Long roomId) {
        try {
            System.out.println("📥 GET /api/watch-party/" + roomId);
            
            WatchParty party = watchPartyService.getRoomById(roomId);
            
            System.out.println("✅ Soba učitana: " + party.getName());
            
            return ResponseEntity.ok(party);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    // ============================================
    // PRIDRUŽIVANJE/NAPUŠTANJE SOBE
    // ============================================

    /**
     * Pridruži se sobi.
     * 
     * HTTP REQUEST:
     * POST /api/watch-party/123/join
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * {
     *   "id": 123,
     *   "name": "Movie Night",
     *   "members": ["petar", "stefan"],  ← Stefan je dodat!
     *   "memberCount": 2
     * }
     * 
     * @param roomId - ID sobe
     * @param authentication - Ulogovani korisnik
     * @return 200 OK sa ažuriranom WatchParty
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        try {
            System.out.println("📥 POST /api/watch-party/" + roomId + "/join");
            
            String username = authentication.getName();
            
            System.out.println("   Korisnik: " + username);
            
            WatchParty party = watchPartyService.joinRoom(roomId, username);
            
            System.out.println("✅ Korisnik pridružen sobi!");
            
            return ResponseEntity.ok(party);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Napusti sobu.
     * 
     * HTTP REQUEST:
     * POST /api/watch-party/123/leave
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * {
     *   "id": 123,
     *   "name": "Movie Night",
     *   "members": ["petar"],  ← Stefan je uklonjen!
     *   "memberCount": 1
     * }
     * 
     * @param roomId - ID sobe
     * @param authentication - Ulogovani korisnik
     * @return 200 OK sa ažuriranom WatchParty
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        try {
            System.out.println("📥 POST /api/watch-party/" + roomId + "/leave");
            
            String username = authentication.getName();
            
            System.out.println("   Korisnik: " + username);
            
            WatchParty party = watchPartyService.leaveRoom(roomId, username);
            
            System.out.println("✅ Korisnik napustio sobu!");
            
            return ResponseEntity.ok(party);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============================================
    // ZATVARANJE SOBE
    // ============================================

    /**
     * Zatvori sobu (samo kreator).
     * 
     * HTTP REQUEST:
     * DELETE /api/watch-party/123/close
     * Headers: Authorization: Bearer <JWT_TOKEN>
     * 
     * HTTP RESPONSE (200 OK):
     * {
     *   "id": 123,
     *   "name": "Movie Night",
     *   "active": false  ← Soba zatvorena!
     * }
     * 
     * @param roomId - ID sobe
     * @param authentication - Ulogovani korisnik
     * @return 200 OK sa zatvorenom WatchParty
     */
    @DeleteMapping("/{roomId}/close")
    public ResponseEntity<?> closeRoom(
            @PathVariable Long roomId,
            Authentication authentication) {
        
        try {
            System.out.println("📥 DELETE /api/watch-party/" + roomId + "/close");
            
            String username = authentication.getName();
            
            System.out.println("   Korisnik: " + username);
            
            WatchParty party = watchPartyService.closeRoom(roomId, username);
            
            System.out.println("✅ Soba zatvorena!");
            
            return ResponseEntity.ok(party);
            
        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }
}
