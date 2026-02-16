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


@Controller
public class WatchPartyWebSocketController {

    @Autowired
    private WatchPartyService watchPartyService;

    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ============================================
    // POKRETANJE VIDEA (GLAVNI FEATURE!)
    // ============================================

   
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
