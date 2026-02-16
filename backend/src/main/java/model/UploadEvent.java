package model;

import java.io.Serializable;
import java.time.LocalDateTime;


public class UploadEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // ============================================
    // POLJA
    // ============================================
    
   
    private Long postId;
    
   
    private String title;
    
    
    private String description;
    
    
    private String author;
    
    
    private String authorEmail;
    
    
    private String videoUrl;
    
    
    private String thumbnailUrl;
    
    
    private Long fileSize;
    
    
    private Integer duration;
    
    
    private LocalDateTime timestamp;
    
    
    private String eventType;

    // ============================================
    // KONSTRUKTORI
    // ============================================
    
    
    public UploadEvent() {
        this.timestamp = LocalDateTime.now();
        this.eventType = "VIDEO_UPLOADED";
    }

    
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