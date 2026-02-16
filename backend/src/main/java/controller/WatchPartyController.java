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


@RestController
@RequestMapping("/api/watch-party")
@CrossOrigin(origins = "http://localhost:4200")
public class WatchPartyController {

    @Autowired
    private WatchPartyService watchPartyService;

    // ============================================
    // KREIRANJE SOBE
    // ============================================

    
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
