package dto;

import model.Post;
import model.Tag;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class PostDTO {

    private Long id;
    private UserBasicDTO user;
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnailUrl;
    private String videoFileName;
    private Long fileSize;
    private Integer duration;
    
    // Tagovi (samo imena tagova, ne celi Tag objekat!)
    private Set<String> tags;
    
    // Geografska lokacija (opciono)
    private Double latitude;
    private Double longitude;
    private String locationName;
    
    // Statistika
    private Integer likesCount;
    private Integer commentsCount;
    private Integer viewsCount;
    
    // Vreme
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============================================
    // KONSTRUKTORI
    // ============================================

    public PostDTO() {}

    // Konstruktor koji prima Post entitet i pretvara ga u DTO
    public PostDTO(Post post) {
        this.id = post.getId();
        
        // Korisnik (samo osnovno - id i username)
        this.user = new UserBasicDTO(
            post.getUser().getId(),
            post.getUser().getUsername()
        );
        
        // Osnovni podaci
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.videoUrl = post.getVideoUrl();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.videoFileName = post.getVideoFileName();
        this.fileSize = post.getFileSize();
        this.duration = post.getDuration();
        
        // Tagovi - konvertuj Set<Tag> u Set<String> (samo imena)
        this.tags = post.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        
        // Geografska lokacija
        this.latitude = post.getLatitude();
        this.longitude = post.getLongitude();
        this.locationName = post.getLocationName();
        
        // Statistika
        this.likesCount = post.getLikesCount();
        this.commentsCount = post.getCommentsCount();
        this.viewsCount = post.getViewsCount();
        
        // Vreme
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
    }

    // ============================================
    // UGNEŽDENA KLASA - UserBasicDTO
    // ============================================
    
    public static class UserBasicDTO {
        private Long id;
        private String username;

        public UserBasicDTO() {}

        public UserBasicDTO(Long id, String username) {
            this.id = id;
            this.username = username;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    // ============================================
    // GETTERI I SETTERI
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserBasicDTO getUser() {
        return user;
    }

    public void setUser(UserBasicDTO user) {
        this.user = user;
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

    public String getVideoFileName() {
        return videoFileName;
    }

    public void setVideoFileName(String videoFileName) {
        this.videoFileName = videoFileName;
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

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Integer getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Integer viewsCount) {
        this.viewsCount = viewsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}