package service;

import config.RabbitMQConfig;
import model.UploadEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class JsonMessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // ============================================
    // SLANJE PORUKA - JSON FORMAT
    // ============================================

  
    public void sendMessage(UploadEvent event) {
        try {
            System.out.println("📤 JSON Producer - Slanje poruke...");
            System.out.println("   Post ID: " + event.getPostId());
            System.out.println("   Title: " + event.getTitle());
            System.out.println("   Author: " + event.getAuthor());
            System.out.println("   File Size: " + event.getReadableFileSize());

            // Slanje poruke (automatska JSON konverzija)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.UPLOAD_EXCHANGE,
                RabbitMQConfig.UPLOAD_ROUTING_KEY,
                event
            );

            System.out.println("✅ JSON poruka poslata!");

        } catch (Exception e) {
            System.err.println("❌ Greška pri slanju JSON poruke: " + e.getMessage());
            throw new RuntimeException("Failed to send JSON message", e);
        }
    }

    
    public long sendMessageWithTiming(UploadEvent event) {
        try {
            // Start timer
            long startTime = System.nanoTime();

            // Slanje poruke
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.UPLOAD_EXCHANGE,
                RabbitMQConfig.UPLOAD_ROUTING_KEY,
                event
            );

            // End timer
            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            System.out.println("⏱️ JSON serijalizacija: " + duration + " ns (" + (duration / 1_000_000.0) + " ms)");

            return duration;

        } catch (Exception e) {
            System.err.println("❌ Greška: " + e.getMessage());
            throw new RuntimeException("Failed to send message", e);
        }
    }

   
    public int estimateMessageSize(UploadEvent event) {
        try {
            // Konvertuj u JSON string
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            
            String json = mapper.writeValueAsString(event);
            
            int size = json.getBytes("UTF-8").length;
            
            System.out.println("📊 JSON veličina: " + size + " bytes");
            
            return size;

        } catch (Exception e) {
            System.err.println("❌ Greška pri proceni veličine: " + e.getMessage());
            return -1;
        }
    }

    // ============================================
    // STATISTIKA
    // ============================================

    /**
     * Vraća informacije o JSON producer-u.
     */
    public String getInfo() {
        return "JsonMessageProducer{" +
                "exchange='" + RabbitMQConfig.UPLOAD_EXCHANGE + '\'' +
                ", routingKey='" + RabbitMQConfig.UPLOAD_ROUTING_KEY + '\'' +
                ", format='JSON'" +
                '}';
    }
}
