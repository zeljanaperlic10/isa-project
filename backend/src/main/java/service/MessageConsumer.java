package service;

import config.RabbitMQConfig;
import model.UploadEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * MessageConsumer - Prima poruke iz RabbitMQ queue-a (3.14 zahtev)
 * 
 * KORISTI:
 * - @RabbitListener anotaciju (Spring automatski konektuje)
 * - Osluškuje UPLOAD_QUEUE
 * 
 * PROCES:
 * 1. Producer šalje poruku → RabbitMQ queue
 * 2. Spring detektuje novu poruku
 * 3. Poziva handleMessage() metodu
 * 4. Poruka se procesira i loguje
 */
@Service
public class MessageConsumer {

    // ============================================
    // STATISTIKA
    // ============================================
    
    private int jsonMessagesReceived = 0;
    private int protobufMessagesReceived = 0;
    private int totalMessagesReceived = 0;

    // ============================================
    // MESSAGE LISTENER - JSON FORMAT
    // ============================================

    /**
     * Osluškuje queue i prima JSON poruke.
     * 
     * @RabbitListener - Spring automatski:
     * - Konektuje na RabbitMQ
     * - Osluškuje queue
     * - Deserijalizuje JSON → UploadEvent
     * - Poziva ovu metodu
     * 
     * @param event - Automatski deserijalizovan UploadEvent
     */
    @RabbitListener(queues = RabbitMQConfig.UPLOAD_QUEUE)
    public void handleMessage(UploadEvent event) {
        try {
            System.out.println("=".repeat(80));
            System.out.println("📨 MESSAGE CONSUMER - Nova poruka primljena!");
            System.out.println("=".repeat(80));
            
            System.out.println("📄 Post ID: " + event.getPostId());
            System.out.println("📄 Title: " + event.getTitle());
            System.out.println("📄 Author: " + event.getAuthor());
            System.out.println("📄 Email: " + event.getAuthorEmail());
            System.out.println("📄 File Size: " + event.getReadableFileSize());
            System.out.println("📄 Duration: " + event.getReadableDuration());
            System.out.println("📄 Video URL: " + event.getVideoUrl());
            System.out.println("📄 Thumbnail URL: " + event.getThumbnailUrl());
            System.out.println("📄 Timestamp: " + event.getTimestamp());
            System.out.println("📄 Event Type: " + event.getEventType());
            
            System.out.println("=".repeat(80));

            // Ažuriraj statistiku
            jsonMessagesReceived++;
            totalMessagesReceived++;

            // OVDE MOŽE DA IDE BIZNIS LOGIKA:
            // - Slanje email notifikacije
            // - Procesiranje videa (thumbnails, transcoding)
            // - Ažuriranje analytics
            // - Slanje push notifikacija
            // - Ažuriranje cache-a
            // - itd.

            System.out.println("✅ Poruka uspešno procesirana!");
            System.out.println("📊 Ukupno primljenih poruka: " + totalMessagesReceived);
            System.out.println("📊 JSON poruke: " + jsonMessagesReceived);
            System.out.println("📊 Protobuf poruke: " + protobufMessagesReceived);
            System.out.println();

        } catch (Exception e) {
            System.err.println("❌ Greška pri procesiranju poruke: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================
    // DODATNE METODE ZA TESTIRANJE
    // ============================================

    /**
     * Prima poruku i meri vreme deserijalizacije.
     * 
     * NAPOMENA: Ova metoda se NE koristi u produkciji.
     * Služi samo za testiranje i poređenje performansi.
     * 
     * @param event - UploadEvent objekat
     * @return long - Vreme deserijalizacije (nanosekunde)
     */
    public long handleMessageWithTiming(UploadEvent event) {
        long startTime = System.nanoTime();
        
        // Simulacija procesiranja
        String title = event.getTitle();
        String author = event.getAuthor();
        Long fileSize = event.getFileSize();
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        System.out.println("⏱️ Deserijalizacija: " + duration + " ns (" + (duration / 1_000_000.0) + " ms)");
        
        return duration;
    }

    /**
     * Inkrementira brojač Protobuf poruka (za testiranje).
     */
    public void incrementProtobufCount() {
        protobufMessagesReceived++;
        totalMessagesReceived++;
    }

    // ============================================
    // GETTERS - STATISTIKA
    // ============================================

    public int getJsonMessagesReceived() {
        return jsonMessagesReceived;
    }

    public int getProtobufMessagesReceived() {
        return protobufMessagesReceived;
    }

    public int getTotalMessagesReceived() {
        return totalMessagesReceived;
    }

    /**
     * Resetuje statistiku (za testiranje).
     */
    public void resetStatistics() {
        jsonMessagesReceived = 0;
        protobufMessagesReceived = 0;
        totalMessagesReceived = 0;
        System.out.println("📊 Statistika resetovana!");
    }

    /**
     * Ispisuje trenutnu statistiku.
     */
    public void printStatistics() {
        System.out.println("=".repeat(80));
        System.out.println("📊 MESSAGE CONSUMER - STATISTIKA");
        System.out.println("=".repeat(80));
        System.out.println("Ukupno poruka: " + totalMessagesReceived);
        System.out.println("JSON poruke: " + jsonMessagesReceived);
        System.out.println("Protobuf poruke: " + protobufMessagesReceived);
        System.out.println("=".repeat(80));
    }

    // ============================================
    // LIFECYCLE
    // ============================================

    public MessageConsumer() {
        System.out.println("=".repeat(80));
        System.out.println("📨 MessageConsumer - Inicijalizacija (3.14 zahtev)");
        System.out.println("   Osluškuje queue: " + RabbitMQConfig.UPLOAD_QUEUE);
        System.out.println("=".repeat(80));
    }
}