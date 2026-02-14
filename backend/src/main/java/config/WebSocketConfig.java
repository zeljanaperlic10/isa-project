package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocketConfig - Konfiguracija WebSocket-a (3.15 zahtev)
 * 
 * WEBSOCKET = Real-time dvosmerana komunikacija
 * 
 * RAZLIKA: HTTP vs WebSocket
 * 
 * HTTP (klasičan):
 * 1. Client → šalje zahtev → Server
 * 2. Server → odgovara → Client
 * 3. Konekcija se zatvara
 * 4. Za novu informaciju → ponovo ceo proces
 * 
 * WebSocket:
 * 1. Client ← → Server (otvorena konekcija)
 * 2. Obe strane mogu slati poruke bilo kada
 * 3. Konekcija ostaje otvorena
 * 4. REAL-TIME komunikacija! ⚡
 * 
 * USE CASE (3.15 zahtev):
 * - Kreator pokrene video → Server šalje poruku SVIM članovima sobe
 * - Članovi ODMAH dobijaju event (bez refresh-a!)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // ============================================
    // STOMP ENDPOINT - Gde se klijenti konektuju
    // ============================================
    
    /**
     * Registruje endpoint za WebSocket konekciju.
     * 
     * ENDPOINT: /ws
     * 
     * PROCES KONEKTOVANJA:
     * 1. Frontend: const socket = new SockJS('http://localhost:9090/ws')
     * 2. Kreira WebSocket konekcija
     * 3. Koristi SockJS (fallback ako WebSocket nije podržan)
     * 4. STOMP protokol preko WebSocket-a
     * 
     * STOMP = Simple Text Oriented Messaging Protocol
     * - Protokol za razmenu poruka
     * - Slično HTTP, ali za messaging
     * - Subscribe/Publish model
     * 
     * CORS:
     * - setAllowedOriginPatterns("*") → Dozvoljava sve origine
     * - U produkciji: setAllowedOrigins("http://localhost:4200")
     * 
     * @param registry - STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("🔌 Registrujem WebSocket endpoint: /ws");
        
        registry
            .addEndpoint("/ws")                    // URL: ws://localhost:9090/ws
            .setAllowedOriginPatterns("*")         // CORS: dozvoli sve
            .withSockJS();                         // Fallback za stare browsere
        
        System.out.println("✅ WebSocket endpoint registrovan!");
    }

    // ============================================
    // MESSAGE BROKER - Rutiranje poruka
    // ============================================
    
    /**
     * Konfiguracija message broker-a.
     * 
     * MESSAGE BROKER = Posrednik za razmenu poruka
     * 
     * DVA TIPA DESTINACIJA:
     * 
     * 1. APPLICATION DESTINATION PREFIX: /app
     *    - Poruke od CLIENT → SERVER
     *    - Client šalje na /app/watch-party/join
     *    - Server prima i procesira
     * 
     * 2. BROKER PREFIX: /topic, /queue
     *    - Poruke od SERVER → CLIENT(S)
     *    - /topic/* → broadcast (svi koji su pretplaćeni)
     *    - /queue/* → point-to-point (jedan korisnik)
     * 
     * TOK PORUKE:
     * 
     * [Client 1] --send--> /app/watch-party/start
     *                           ↓
     *                      [Controller]
     *                           ↓ (procesira)
     *                [Message Template]
     *                           ↓ send-to
     *               /topic/watch-party/123/video-started
     *                     ↙          ↓          ↘
     *            [Client 1]    [Client 2]    [Client 3]
     *         (subscribe)    (subscribe)    (subscribe)
     * 
     * PRIMER:
     * 
     * // Client šalje:
     * stompClient.send('/app/watch-party/join', {}, JSON.stringify({roomId: 123}))
     * 
     * // Server prima na:
     * @MessageMapping("/watch-party/join")  // /app se automatski dodaje!
     * 
     * // Server broadcast-uje:
     * messagingTemplate.convertAndSend('/topic/watch-party/123', event)
     * 
     * // Svi klijenti koji su subscribe-ovani na:
     * stompClient.subscribe('/topic/watch-party/123', (message) => {...})
     * 
     * @param registry - Message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        System.out.println("🔧 Konfigurisanje Message Broker-a...");
        
        // Poruke OD klijenata → server
        registry.setApplicationDestinationPrefixes("/app");
        System.out.println("   ✅ Application prefix: /app");
        System.out.println("      Client → Server: /app/watch-party/*");
        
        // Poruke OD servera → klijenti (broadcast)
        registry.enableSimpleBroker("/topic", "/queue");
        System.out.println("   ✅ Broker prefixes: /topic, /queue");
        System.out.println("      Server → Clients: /topic/watch-party/*");
        System.out.println("      Server → Client: /queue/user/*");
        
        System.out.println("✅ Message Broker konfigurisan!");
    }

    // ============================================
    // LIFECYCLE
    // ============================================
    
    public WebSocketConfig() {
        System.out.println("=".repeat(80));
        System.out.println("🔌 WebSocketConfig - Inicijalizacija (3.15 zahtev)");
        System.out.println("   Real-time komunikacija za Watch Party");
        System.out.println("=".repeat(80));
    }
}
