package dto;

import model.Comment;
import java.time.LocalDateTime;


public class CommentDTO {

    // ============================================
    // POLJA
    // ============================================
    
    private Long id;
    private String text;
    private String username;  // Samo username, ne ceo User objekat
    private LocalDateTime createdAt;

    // ============================================
    // CONSTRUCTORS
    // ============================================
    
    /**
     * Prazan konstruktor
     */
    public CommentDTO() {
    }

    /**
     * Konstruktor sa svim poljima
     */
    public CommentDTO(Long id, String text, String username, LocalDateTime createdAt) {
        this.id = id;
        this.text = text;
        this.username = username;
        this.createdAt = createdAt;
    }

   
    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.text = comment.getText();
        this.username = comment.getUser().getUsername();
        this.createdAt = comment.getCreatedAt();
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
        return "CommentDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", text='" + (text != null && text.length() > 50 ? text.substring(0, 50) + "..." : text) + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}