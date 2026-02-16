package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================
    // KORISNIK (Many-to-One)
    // ============================================
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ============================================
    // OSNOVNI PODACI
    // ============================================
    
    // Naslov videa (obavezno)
    @Column(nullable = false, length = 200)
    private String title;

    // Opis videa (opciono)
    @Column(length = 1000)
    private String description;

    // ============================================
    // VIDEO I THUMBNAIL
    // ============================================
    
    // URL/putanja do video fajla
    @Column(nullable = false, length = 500)
    private String videoUrl;

    // URL/putanja do thumbnail slike
    @Column(nullable = false, length = 500)
    private String thumbnailUrl;

    // Originalno ime video fajla
    @Column(length = 255)
    private String videoFileName;

    // Veličina fajla u bajtovima
    @Column
    private Long fileSize;

    // Trajanje videa u sekundama
    @Column
    private Integer duration;

    // ============================================
    // TAGOVI (Many-to-Many) - 3.3 zahtev
    // ============================================
    
    
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    @JoinTable(
        name = "post_tags",
        joinColumns = @JoinColumn(name = "post_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    // ============================================
    // GEOGRAFSKA LOKACIJA (opciono) - 3.3 zahtev
    // ============================================
    
    // Geografska širina (latitude)
    @Column
    private Double latitude;

    // Geografska dužina (longitude)
    @Column
    private Double longitude;

   
    @Column(length = 200)
    private String locationName;

    // ============================================
    // STATISTIKA
    // ============================================
    
    // Broj lajkova
    @Column(nullable = false)
    private Integer likesCount = 0;

    // Broj komentara
    @Column(nullable = false)
    private Integer commentsCount = 0;

    // Broj pregleda (za statistiku)
    @Column(nullable = false)
    private Integer viewsCount = 0;

    // ============================================
    // VREME KREIRANJA (sistemsko) - 3.3 zahtev
    // ============================================
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============================================
    // KONSTRUKTORI
    // ============================================

    public Post() {}

    public Post(User user, String title, String description, String videoUrl, String thumbnailUrl) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    // ============================================
    // POMOĆNE METODE ZA TAGOVE
    // ============================================
    
    // Dodavanje taga
    public void addTag(Tag tag) {
        this.tags.add(tag);
        tag.getPosts().add(this);
    }

    // Uklanjanje taga
    public void removeTag(Tag tag) {
        this.tags.remove(tag);
        tag.getPosts().remove(this);
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
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

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
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
