package model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * UploadEvent - Model klasa za video upload event (3.14 zahtev)
 * 
 * SADRŽI:
 * - Osnovne informacije o video-u
 * - Metapodatke (autor, vreme, veličina)
 * 
 * KORISTI SE ZA:
 * - JSON serijalizaciju (Jackson)
 * - Protobuf serijalizaciju (manual conversion)
 * - Slanje kroz RabbitMQ
 */
public class UploadEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // ============================================
    // POLJA
    // ============================================
    
    /**
     * ID videa (Post ID)
     */
    private Long postId;
    
    /**
     * Naslov videa
     */
    private String title;
    
    /**
     * Opis videa (može biti null)
     */
    private String description;
    
    /**
     * Ime autora (username)
     */
    private String author;
    
    /**
     * Email autora
     */
    private String authorEmail;
    
    /**
     * URL videa
     */
    private String videoUrl;
    
    /**
     * URL thumbnail slike
     */
    private String thumbnailUrl;
    
    /**
     * Veličina fajla u bajtovima
     */
    private Long fileSize;
    
    /**
     * Trajanje videa u sekundama (može biti null)
     */
    private Integer duration;
    
    /**
     * Vreme kreiranja event-a
     */
    private LocalDateTime timestamp;
    
    /**
     * Event tip (npr: "VIDEO_UPLOADED")
     */
    private String eventType;

    // ============================================
    // KONSTRUKTORI
    // ============================================
    
    /**
     * Default konstruktor (potreban za Jackson JSON deserijalizaciju)
     */
    public UploadEvent() {
        this.timestamp = LocalDateTime.now();
        this.eventType = "VIDEO_UPLOADED";
    }

    /**
     * Konstruktor sa svim poljima
     */
    public UploadEvent(Long postId, String title, String description, 
                      String author, String authorEmail,
                      String videoUrl, String thumbnailUrl, 
                      Long fileSize, Integer duration) {
        this.postId = postId;
        this.title = title;
        this.description = description;
        this.author = author;
        this.authorEmail = authorEmail;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.fileSize = fileSize;
        this.duration = duration;
        this.timestamp = LocalDateTime.now();
        this.eventType = "VIDEO_UPLOADED";
    }

    // ============================================
    // GETTERS & SETTERS
    // ============================================

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    /**
     * Vraća čitljivu veličinu fajla (npr: "15.3 MB")
     */
    public String getReadableFileSize() {
        if (fileSize == null) {
            return "Unknown";
        }
        
        long bytes = fileSize;
        
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

    /**
     * Vraća čitljivo trajanje (npr: "3:45")
     */
    public String getReadableDuration() {
        if (duration == null) {
            return "Unknown";
        }
        
        int minutes = duration / 60;
        int seconds = duration % 60;
        
        return String.format("%d:%02d", minutes, seconds);
    }

    // ============================================
    // toString, equals, hashCode
    // ============================================

    @Override
    public String toString() {
        return "UploadEvent{" +
                "postId=" + postId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", fileSize=" + getReadableFileSize() +
                ", duration=" + getReadableDuration() +
                ", timestamp=" + timestamp +
                ", eventType='" + eventType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UploadEvent that = (UploadEvent) o;

        return postId != null ? postId.equals(that.postId) : that.postId == null;
    }

    @Override
    public int hashCode() {
        return postId != null ? postId.hashCode() : 0;
    }
}