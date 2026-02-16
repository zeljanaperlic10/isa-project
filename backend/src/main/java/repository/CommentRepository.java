package repository;

import model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

   
    
    
    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    // ============================================
    // RATE LIMITING (3.6 zahtev - 60 komentara/sat)
    // ============================================
    
  
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.createdAt > :since")
    int countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // ============================================
    // KORISNIČKE METODE
    // ============================================
    
    
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

   
    int countByPostId(Long postId);

   
    void deleteByPostId(Long postId);
}