package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PostLike - Entitet za lajkove na postovima
 * 
 * FUNKCIONLANOST:
 * - Jedan korisnik može lajkovati jedan post samo jednom
 * - Composite unique constraint (user_id + post_id)
 * - Automatsko kreiranje timestampa
 */
@Entity
@Table(
    name = "post_likes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
    }
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================
    // RELACIJE
    // ============================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // ============================================
    // TIMESTAMP
    // ============================================

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ============================================
    // KONSTRUKTORI
    // ============================================

    public PostLike() {}

    public PostLike(User user, Post post) {
        this.user = user;
        this.post = post;
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

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}