package com.example.consumer_app.service;

import com.example.consumer_app.config.RabbitMQConfig;
import com.example.consumer_app.model.UploadEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * MessageConsumerService - Prima poruke iz RabbitMQ (3.14 zahtev)
 * 
 * FUNKCIJA:
 * - Osluškuje video.upload.queue
 * - Prima JSON poruke
 * - Deserijalizuje u UploadEvent
 * - Procesira event (logovanje, email, analytics...)
 */
@Service
public class MessageConsumerService {

    // ============================================
    // STATISTIKA
    // ============================================
    
    private int totalMessagesReceived = 0;
    private long totalFileSize = 0;

    // ============================================
    // MESSAGE LISTENER
    // ============================================

    /**
     * Osluškuje queue i prima poruke.
     * 
     * @RabbitListener automatski:
     * 1. Konektuje se na RabbitMQ
     * 2. Osluškuje queue
     * 3. Deserijalizuje JSON → UploadEvent
     * 4. Poziva ovu metodu
     * 
     * @param event - Automatski deserijalizovan UploadEvent
     */
    @RabbitListener(queues = RabbitMQConfig.UPLOAD_QUEUE)
    public void handleVideoUploadEvent(UploadEvent event) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📨 [CONSUMER] NOVA PORUKA PRIMLJENA!");
            System.out.println("=".repeat(80));
            
            // Logovanje detalja
            System.out.println("📹 Post ID:      " + event.getPostId());
            System.out.println("📄 Title:        " + event.getTitle());
            System.out.println("👤 Author:       " + event.getAuthor());
            System.out.println("📧 Email:        " + event.getAuthorEmail());
            System.out.println("📦 File Size:    " + event.getReadableFileSize());
            System.out.println("⏱️  Duration:     " + event.getReadableDuration());
            System.out.println("🎬 Video URL:    " + event.getVideoUrl());
            System.out.println("🖼️  Thumbnail:    " + event.getThumbnailUrl());
            System.out.println("🕐 Timestamp:    " + event.getTimestamp());
            System.out.println("📌 Event Type:   " + event.getEventType());
            
            // Ažuriraj statistiku
            totalMessagesReceived++;
            if (event.getFileSize() != null) {
                totalFileSize += event.getFileSize();
            }
            
            System.out.println();
            System.out.println("📊 STATISTIKA:");
            System.out.println("   Ukupno primljenih poruka: " + totalMessagesReceived);
            System.out.println("   Ukupna veličina video-a:  " + formatBytes(totalFileSize));
            
            System.out.println("=".repeat(80));
            
            // ============================================
            // OVDE MOŽE IĆI BIZNIS LOGIKA:
            // ============================================
            
            // 1. Slanje email notifikacije
            // emailService.sendUploadNotification(event.getAuthor(), event.getTitle());
            
            // 2. Video procesiranje
            // videoProcessingService.generateThumbnails(event.getPostId());
            // videoProcessingService.transcodeVideo(event.getVideoUrl());
            
            // 3. Analytics tracking
            // analyticsService.trackVideoUpload(event);
            
            // 4. Push notifikacije
            // pushService.notifySubscribers(event.getAuthor(), event.getTitle());
            
            // 5. Cache invalidation
            // cacheService.invalidateUserVideos(event.getAuthor());
            
            // 6. Content moderation
            // moderationService.scanVideo(event.getPostId());
            
            System.out.println("✅ [CONSUMER] Poruka uspešno procesirana!\n");
            
        } catch (Exception e) {
            System.err.println("❌ [CONSUMER] Greška pri procesiranju poruke: " + e.getMessage());
            e.printStackTrace();
            
            // Ovde možeš implementirati retry logiku ili dead letter queue
        }
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    /**
     * Formatuje byte-ove u čitljiv format.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // ============================================
    // GETTERS - Statistika
    // ============================================

    public int getTotalMessagesReceived() {
        return totalMessagesReceived;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    /**
     * Resetuje statistiku.
     */
    public void resetStatistics() {
        totalMessagesReceived = 0;
        totalFileSize = 0;
        System.out.println("📊 [CONSUMER] Statistika resetovana!");
    }

    // ============================================
    // LIFECYCLE
    // ============================================

    public MessageConsumerService() {
        System.out.println("=".repeat(80));
        System.out.println("📨 [CONSUMER] MessageConsumerService - Inicijalizacija");
        System.out.println("   Osluškuje queue: " + RabbitMQConfig.UPLOAD_QUEUE);
        System.out.println("   Čeka poruke...");
        System.out.println("=".repeat(80));
    }
}
