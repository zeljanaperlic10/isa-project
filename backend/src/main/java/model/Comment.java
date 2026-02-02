package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Comment entitet - reprezentuje komentar na video objavi (3.6 zahtev)
 * 
 * ZAHTEVI:
 * - Tekst komentara
 * - Vreme kreiranja
 * - Nalog koji ga je kreirao
 * - Veza sa Post entitetom
 * - Potrebno keširanje
 */
@Entity
@Table(name = "comments")
public class Comment {

    // ============================================
    // PRIMARY KEY
    // ============================================
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================
    // RELACIJE
    // ============================================
    
    /**
     * Korisnik koji je napisao komentar (3.6 zahtev)
     * Many-to-One: Jedan korisnik može imati mnogo komentara
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Post na koji se odnosi komentar (3.6 zahtev)
     * Many-to-One: Jedan post može imati mnogo komentara
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // ============================================
    // SADRŽAJ KOMENTARA
    // ============================================
    
    /**
     * Tekst komentara (3.6 zahtev - samo tekst)
     * Max dužina: 1000 karaktera
     */
    @Column(nullable = false, length = 1000)
    private String text;

    // ============================================
    // VREME KREIRANJA (3.6 zahtev)
    // ============================================
    
    /**
     * Vreme kreiranja komentara
     * Automatski se postavlja prilikom kreiranja
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    public Comment() {
    }

    public Comment(User user, Post post, String text) {
        this.user = user;
        this.post = post;
        this.text = text;
    }

    // ============================================
    // LIFECYCLE CALLBACKS
    // ============================================
    
    /**
     * Automatski postavlja vreme kreiranja pre čuvanja u bazu
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        System.out.println("💬 Novi komentar kreiran u " + this.createdAt);
    }

    // ============================================
    // GETTERS AND SETTERS
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ============================================
    // HELPER METODE
    // ============================================
    
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", text='" + (text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text) + "' " +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return id != null && id.equals(comment.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
