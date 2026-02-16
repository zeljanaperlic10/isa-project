package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "watch_parties")
public class WatchParty {

    // ============================================
    // POLJA
    // ============================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;
    
  
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "current_post_id")
    private Post currentPost;
    
    
    @Column(nullable = false)
    private Boolean active = true;
    
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "watch_party_members", joinColumns = @JoinColumn(name = "watch_party_id"))
    @Column(name = "username")
    private Set<String> members = new HashSet<>();

    // ============================================
    // KONSTRUKTORI
    // ============================================
    
    public WatchParty() {
    }

    public WatchParty(String name, User creator) {
        this.name = name;
        this.creator = creator;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.members = new HashSet<>();
        this.members.add(creator.getUsername()); // Kreator je automatski član
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ============================================
    // GETTERS & SETTERS
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Post getCurrentPost() {
        return currentPost;
    }

    public void setCurrentPost(Post currentPost) {
        this.currentPost = currentPost;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    /**
     * Dodaj člana u sobu
     */
    public void addMember(String username) {
        if (this.members == null) {
            this.members = new HashSet<>();
        }
        this.members.add(username);
    }

    /**
     * Ukloni člana iz sobe
     */
    public void removeMember(String username) {
        if (this.members != null) {
            this.members.remove(username);
        }
    }

    /**
     * Proveri da li je korisnik član
     */
    public boolean isMember(String username) {
        return this.members != null && this.members.contains(username);
    }

    /**
     * Proveri da li je korisnik kreator
     */
    public boolean isCreator(String username) {
        return this.creator != null && this.creator.getUsername().equals(username);
    }

    /**
     * Broj članova
     */
    public int getMemberCount() {
        return this.members != null ? this.members.size() : 0;
    }

    // ============================================
    // toString
    // ============================================

    @Override
    public String toString() {
        return "WatchParty{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creator=" + (creator != null ? creator.getUsername() : "null") +
                ", currentPost=" + (currentPost != null ? currentPost.getId() : "null") +
                ", active=" + active +
                ", members=" + getMemberCount() +
                ", createdAt=" + createdAt +
                '}';
    }
}
