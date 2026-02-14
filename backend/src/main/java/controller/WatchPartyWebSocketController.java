package controller;

import model.Post;
import model.WatchParty;
import service.WatchPartyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * WatchPartyWebSocketController - Real-time komunikacija (3.15 zahtev)
 * 
 * RAZLIKA: REST Controller vs WebSocket Controller
 * 
 * REST Controller:
 * - @RestController
 * - @PostMapping, @GetMapping
 * - Jedan zahtev → jedan odgovor
 * - Client mora slati zahtev da dobije update
 * 
 * WebSocket Controller:
 * - @Controller (ne @RestController!)
 * - @MessageMapping (za STOMP poruke)
 * - Konekcija ostaje otvorena
 * - Server PUSH-uje poruke klijentima
 * 
 * ENDPOINT-i:
 * - /app/watch-party/{roomId}/start-video  → Kreator pokreće video
 * - /app/watch-party/{roomId}/join         → Korisnik se pridružuje (WebSocket)
 * - /app/watch-party/{roomId}/leave        → Korisnik napušta (WebSocket)
 * 
 * BROADCAST DESTINACIJE:
 * - /topic/watch-party/{roomId}  → Svi članovi primaju poruku
 */
@Controller
public class WatchPartyWebSocketController {

    @Autowired
    private WatchPartyService watchPartyService;

    /**
     * SimpMessagingTemplate - Tool za slanje WebSocket poruka.
     * 
     * METODE:
     * - convertAndSend(destination, payload) → Broadcast svima
     * - convertAndSendToUser(user, dest, payload) → Šalji jednom korisniku
     * 
     * PRIMER:
     * messagingTemplate.convertAndSend("/topic/watch-party/123", event);
     * → Svi koji su subscribe-ovani na /topic/watch-party/123 primaju event!
     */
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ============================================
    // POKRETANJE VIDEA (GLAVNI FEATURE!)
    // ============================================

    /**
     * Kreator pokreće video u sobi.
     * 
     * WEBSOCKET TOK:
     * 
     * 1. Frontend (Kreator):
     *    stompClient.send('/app/watch-party/123/start-video', {}, JSON.stringify({
     *        postId: 10
     *    }));
     * 
     * 2. Backend (ova metoda):
     *    - Prima poruku
     *    - Poziva Service da postavi currentPost
     *    - BROADCAST event svim članovima sobe!
     * 
     * 3. Frontend (Svi članovi):
     *    stompClient.subscribe('/topic/watch-party/123', (message) => {
     *        const event = JSON.parse(message.body);
     *        if (event.type === 'VIDEO_STARTED') {
     *            router.navigate(['/video', event.postId]);  // Otvori video!
     *        }
     *    });
     * 
     * ANOTACIJE:
     * - @MessageMapping - mapira STOMP poruku (kao @PostMapping za WebSocket)
     * - @DestinationVariable - izvlači {roomId} iz URL-a
     * - Principal - ulogovani korisnik (iz WebSocket sesije)
     * 
     * REQUEST PAYLOAD:
     * {
     *   "postId": 10
     * }
     * 
     * BROADCAST PAYLOAD:
     * {
     *   "type": "VIDEO_STARTED",
     *   "roomId": 123,
     *   "postId": 10,
     *   "postTitle": "My Video",
     *   "videoUrl": "/api/videos/abc123.mp4",
     *   "startedBy": "petar",
     *   "timestamp": "2026-02-02T12:00:00"
     * }
     * 
     * @param roomId - ID sobe (iz URL-a)
     * @param payload - { "postId": 10 }
     * @param principal - Ulogovani korisnik
     */
    @MessageMapping("/watch-party/{roomId}/start-video")
    public void startVideo(
            @DestinationVariable Long roomId,
            Map<String, Object> payload,
            Principal principal) {
        
        try {
            System.out.println("=".repeat(80));
            System.out.println("🎬 WebSocket: START VIDEO");
            System.out.println("   Soba ID: " + roomId);
            System.out.println("   Korisnik: " + principal.getName());
            System.out.println("   Payload: " + payload);
            System.out.println("=".repeat(80));

            String username = principal.getName();
            Long postId = Long.valueOf(payload.get("postId").toString());

            // Pozovi Service - postavi trenutni video
            WatchParty party = watchPartyService.startVideo(roomId, postId, username);

            // Pripremi broadcast event
            Map<String, Object> event = new HashMap<>();
            event.put("type", "VIDEO_STARTED");
            event.put("roomId", party.getId());
            event.put("postId", party.getCurrentPost().getId());
            event.put("postTitle", party.getCurrentPost().getTitle());
            event.put("videoUrl", party.getCurrentPost().getVideoUrl());
            event.put("startedBy", username);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            // BROADCAST svim članovima sobe!
            String destination = "/topic/watch-party/" + roomId;
            messagingTemplate.convertAndSend(destination, event);

            System.out.println("✅ VIDEO_STARTED event broadcast-ovan!");
            System.out.println("   Destinacija: " + destination);
            System.out.println("   Video: " + party.getCurrentPost().getTitle());
            System.out.println("   Broj članova: " + party.getMemberCount());
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("❌ Greška pri pokretanju videa: " + e.getMessage());
            e.printStackTrace();

            // Pošalji error poruku korisniku
            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("type", "ERROR");
            errorEvent.put("message", e.getMessage());

            messagingTemplate.convertAndSend("/topic/watch-party/" + roomId, errorEvent);
        }
    }

    // ============================================
    // PRIDRUŽIVANJE SOBI (WEBSOCKET NOTIFIKACIJA)
    // ============================================

    /**
     * Korisnik se pridružuje sobi - notifikacija ostalim članovima.
     * 
     * NAPOMENA:
     * - Korisnik se PRVO pridružuje preko REST API-ja (POST /api/watch-party/{id}/join)
     * - Zatim šalje WebSocket poruku da obavesti ostale
     * 
     * WEBSOCKET TOK:
     * 
     * 1. Frontend:
     *    await http.post('/api/watch-party/123/join');  // REST - dodaj u bazu
     *    stompClient.send('/app/watch-party/123/join', {});  // WebSocket - notifikuj ostale
     * 
     * 2. Backend (ova metoda):
     *    - Prima poruku
     *    - BROADCAST "USER_JOINED" event svim članovima
     * 
     * 3. Frontend (Ostali članovi):
     *    stompClient.subscribe('/topic/watch-party/123', (message) => {
     *        const event = JSON.parse(message.body);
     *        if (event.type === 'USER_JOINED') {
     *            console.log(event.username + ' se pridružio sobi!');
     *            // Ažuriraj listu članova u UI
     *        }
     *    });
     * 
     * BROADCAST PAYLOAD:
     * {
     *   "type": "USER_JOINED",
     *   "roomId": 123,
     *   "username": "stefan",
     *   "memberCount": 3,
     *   "timestamp": "2026-02-02T12:00:00"
     * }
     * 
     * @param roomId - ID sobe
     * @param principal - Ulogovani korisnik
     */
    @MessageMapping("/watch-party/{roomId}/join")
    public void notifyUserJoined(
            @DestinationVariable Long roomId,
            Principal principal) {
        
        try {
            System.out.println("➕ WebSocket: USER JOINED");
            System.out.println("   Soba ID: " + roomId);
            System.out.println("   Korisnik: " + principal.getName());

            String username = principal.getName();

            // Učitaj sobu (da dobijemo broj članova)
            WatchParty party = watchPartyService.getRoomById(roomId);

            // Pripremi broadcast event
            Map<String, Object> event = new HashMap<>();
            event.put("type", "USER_JOINED");
            event.put("roomId", roomId);
            event.put("username", username);
            event.put("memberCount", party.getMemberCount());
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            // BROADCAST
            messagingTemplate.convertAndSend("/topic/watch-party/" + roomId, event);

            System.out.println("✅ USER_JOINED event broadcast-ovan!");
            System.out.println("   Ukupno članova: " + party.getMemberCount());

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
        }
    }

    // ============================================
    // NAPUŠTANJE SOBE (WEBSOCKET NOTIFIKACIJA)
    // ============================================

    /**
     * Korisnik napušta sobu - notifikacija ostalim članovima.
     * 
     * TOK:
     * 1. Frontend šalje WebSocket poruku
     * 2. Backend broadcast-uje "USER_LEFT" event
     * 3. Ostali članovi vide notifikaciju
     * 4. Frontend zatim poziva REST API da ukloni iz baze
     * 
     * BROADCAST PAYLOAD:
     * {
     *   "type": "USER_LEFT",
     *   "roomId": 123,
     *   "username": "stefan",
     *   "memberCount": 2,
     *   "timestamp": "2026-02-02T12:00:00"
     * }
     * 
     * @param roomId - ID sobe
     * @param principal - Ulogovani korisnik
     */
    @MessageMapping("/watch-party/{roomId}/leave")
    public void notifyUserLeft(
            @DestinationVariable Long roomId,
            Principal principal) {
        
        try {
            System.out.println("➖ WebSocket: USER LEFT");
            System.out.println("   Soba ID: " + roomId);
            System.out.println("   Korisnik: " + principal.getName());

            String username = principal.getName();

            // Pripremi broadcast event
            Map<String, Object> event = new HashMap<>();
            event.put("type", "USER_LEFT");
            event.put("roomId", roomId);
            event.put("username", username);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            // BROADCAST
            messagingTemplate.convertAndSend("/topic/watch-party/" + roomId, event);

            System.out.println("✅ USER_LEFT event broadcast-ovan!");

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
        }
    }

    // ============================================
    // ZATVARANJE SOBE (WEBSOCKET NOTIFIKACIJA)
    // ============================================

    /**
     * Kreator zatvara sobu - notifikacija svim članovima.
     * 
     * BROADCAST PAYLOAD:
     * {
     *   "type": "ROOM_CLOSED",
     *   "roomId": 123,
     *   "closedBy": "petar",
     *   "timestamp": "2026-02-02T12:00:00"
     * }
     * 
     * Frontend reakcija:
     * - Prikaži poruku: "Soba je zatvorena od strane kreatora"
     * - Redirect na homepage ili listu soba
     * 
     * @param roomId - ID sobe
     * @param principal - Kreator (ulogovani korisnik)
     */
    @MessageMapping("/watch-party/{roomId}/close")
    public void notifyRoomClosed(
            @DestinationVariable Long roomId,
            Principal principal) {
        
        try {
            System.out.println("🚫 WebSocket: ROOM CLOSED");
            System.out.println("   Soba ID: " + roomId);
            System.out.println("   Kreator: " + principal.getName());

            String username = principal.getName();

            // Pripremi broadcast event
            Map<String, Object> event = new HashMap<>();
            event.put("type", "ROOM_CLOSED");
            event.put("roomId", roomId);
            event.put("closedBy", username);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            // BROADCAST
            messagingTemplate.convertAndSend("/topic/watch-party/" + roomId, event);

            System.out.println("✅ ROOM_CLOSED event broadcast-ovan!");

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
        }
    }
}
