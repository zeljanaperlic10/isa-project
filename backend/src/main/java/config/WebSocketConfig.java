package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("🔌 Registrujem WebSocket endpoint: /ws");
        
        registry
            .addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
        
        System.out.println("✅ WebSocket endpoint registrovan!");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        System.out.println("🔧 Konfigurisanje Message Broker-a...");
        
        registry.setApplicationDestinationPrefixes("/app");
        System.out.println("   ✅ Application prefix: /app");
        
        registry.enableSimpleBroker("/topic", "/queue");
        System.out.println("   ✅ Broker prefixes: /topic, /queue");
        
        System.out.println("✅ Message Broker konfigurisan!");
    }

    public WebSocketConfig() {
        System.out.println("=".repeat(80));
        System.out.println("🔌 WebSocketConfig - Inicijalizacija (3.15 zahtev)");
        System.out.println("   Real-time komunikacija za Watch Party");
        System.out.println("=".repeat(80));
    }
}
