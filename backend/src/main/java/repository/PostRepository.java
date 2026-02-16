package repository;

import model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ============================================
    // PRONALAŽENJE POJEDINAČNOG POSTA - SA EAGER FETCH
    // ============================================
    
    /**
     * Pronalazi post sa svim asocijacijama (tags, user) eager loaded.
     * Koristi se umesto findById() da bi se izbegao LazyInitializationException.
     */
    @Query("SELECT p FROM Post p " +
           "LEFT JOIN FETCH p.tags " +
           "LEFT JOIN FETCH p.user " +
           "WHERE p.id = :id")
    Optional<Post> findByIdWithAssociations(@Param("id") Long id);

    // ============================================
    // PRONALAŽENJE SVIH POSTOVA - SA EAGER FETCH
    // ============================================

    /**
     * Svi postovi sa eager loaded asocijacijama (tags, user).
     * Koristi se umesto findAllByOrderByCreatedAtDesc().
     */
    @Query("SELECT DISTINCT p FROM Post p " +
           "LEFT JOIN FETCH p.tags " +
           "LEFT JOIN FETCH p.user " +
           "ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDescWithAssociations();

    // Originalni metod (deprecated - koristi gornji)
    List<Post> findAllByOrderByCreatedAtDesc();

    // ============================================
    // PRONALAŽENJE POSTOVA PO KORISNIKU - SA EAGER FETCH
    // ============================================

    /**
     * Svi postovi jednog korisnika sa eager loaded tagovima.
     */
    @Query("SELECT DISTINCT p FROM Post p " +
           "LEFT JOIN FETCH p.tags " +
           "LEFT JOIN FETCH p.user u " +
           "WHERE u.username = :username " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByUserUsernameOrderByCreatedAtDescWithAssociations(@Param("username") String username);

    // Originalni metod (deprecated)
    List<Post> findByUserUsernameOrderByCreatedAtDesc(String username);

    // Svi postovi jednog korisnika (po user ID-u)
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);

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
    // PRETRAGA PO TAGOVIMA - SA EAGER FETCH
    // ============================================

    /**
     * Pronalaženje postova koji imaju određeni tag sa eager loaded user-om.
     */
    @Query("SELECT DISTINCT p FROM Post p " +
           "LEFT JOIN FETCH p.tags t " +
           "LEFT JOIN FETCH p.user " +
           "WHERE t.name = :tagName " +
           "ORDER BY p.createdAt DESC")
    List<Post> findByTagNameWithAssociations(@Param("tagName") String tagName);

    // Originalni metod (za interno korišćenje - brojanje)
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.name = :tagName ORDER BY p.createdAt DESC")
    List<Post> findByTagName(@Param("tagName") String tagName);

    // Pronalaženje postova koji imaju bar jedan od navedenih tagova
    @Query("SELECT DISTINCT p FROM Post p JOIN p.tags t WHERE t.name IN :tagNames ORDER BY p.createdAt DESC")
    List<Post> findByTagNames(@Param("tagNames") List<String> tagNames);

    // Pronalaženje postova koji imaju definisanu lokaciju
    @Query("SELECT p FROM Post p WHERE p.latitude IS NOT NULL AND p.longitude IS NOT NULL ORDER BY p.createdAt DESC")
    List<Post> findAllWithLocation();

    // ============================================
    // BROJAČ PREGLEDA - ATOMIC UPDATE (3.7 zahtev)
    // ============================================

    /**
     * Atomski inkrementuje broj pregleda bez potrebe za read-modify-write ciklus.
     * Thread-safe operacija koja sprečava race conditions.
     * 
     * @param postId ID posta čiji broj pregleda treba inkrementirati
     * @return Broj ažuriranih redova (1 ako je uspešno, 0 ako post ne postoji)
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);
}