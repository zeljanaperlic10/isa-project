package repository;

import model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    // ============================================
    // PRONALAŽENJE SVIH POSTOVA
    // ============================================
    
    // Svi postovi sortirani po vremenu (najnoviji prvi)
    List<Post> findAllByOrderByCreatedAtDesc();
    
    // ============================================
    // PRONALAŽENJE POSTOVA PO KORISNIKU
    // ============================================
    
    // Svi postovi jednog korisnika (po user ID-u)
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Svi postovi jednog korisnika (po username-u)
    List<Post> findByUserUsernameOrderByCreatedAtDesc(String username);
    
    // ============================================
    // BROJANJE POSTOVA
    // ============================================
    
    // Broj postova jednog korisnika
    Long countByUserId(Long userId);
    
    // ============================================
    // PRETRAGA PO NASLOVU
    // ============================================
    
    // Pronalaženje postova koji sadrže ključnu reč u naslovu
    List<Post> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
    
    // ============================================
    // PRETRAGA PO TAGOVIMA
    // ============================================
    
    // Pronalaženje postova koji imaju određeni tag
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName ORDER BY p.createdAt DESC")
    List<Post> findByTagName(@Param("tagName") String tagName);
    
    // Pronalaženje postova koji imaju bar jedan od navedenih tagova
    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE t.name IN :tagNames ORDER BY p.createdAt DESC")
    List<Post> findByTagNames(@Param("tagNames") List<String> tagNames);
    
    // ============================================
    // PRETRAGA PO LOKACIJI (za kasnije - može biti korisno)
    // ============================================
    
    // Pronalaženje postova koji imaju definisanu lokaciju
    @Query("SELECT p FROM Post p WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL ORDER BY p.createdAt DESC")
    List<Post> findAllWithLocation();
}
