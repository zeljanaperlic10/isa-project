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

/**
 * CommentRepository - JPA Repository za Comment entitet (3.6 zahtev)
 * 
 * FUNKCIONALNOSTI:
 * - CRUD operacije (Create, Read, Update, Delete)
 * - Paginacija komentara (3.6 zahtev - velika količina)
 * - Sortiranje najnoviji -> najstariji (3.6 zahtev)
 * - Brojanje komentara po korisniku (rate limiting - 3.6 zahtev)
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ============================================
    // OSNOVNE METODE (automatske od JpaRepository)
    // ============================================
    
    // findById(Long id) - pronalazi komentar po ID-u
    // save(Comment comment) - čuva novi ili ažurira postojeći
    // deleteById(Long id) - briše komentar
    // findAll() - vraća sve komentare

    // ============================================
    // PAGINACIJA (3.6 zahtev - velika količina)
    // ============================================
    
    /**
     * Vraća komentare za određeni post sa paginacijom.
     * 
     * PAGINACIJA (3.6 zahtev):
     * - Omogućava učitavanje dela komentara (npr. 20 po stranici)
     * - Smanjuje opterećenje baze i mreže
     * - Frontend može učitavati više dok korisnik skroluje
     * 
     * SORTIRANJE (3.6 zahtev):
     * - OrderByCreatedAtDesc → najnoviji prvi
     * 
     * @param postId - ID posta
     * @param pageable - Pageable objekat (page number, size, sort)
     * @return Page<Comment> - stranica komentara + metadata (total, pages...)
     * 
     * PRIMER KORIŠĆENJA:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
     * Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);
     */
    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    /**
     * Vraća sve komentare za post (bez paginacije - za male količine)
     * 
     * @param postId - ID posta
     * @return List<Comment> - svi komentari za post
     */
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    // ============================================
    // RATE LIMITING (3.6 zahtev - 60 komentara/sat)
    // ============================================
    
    /**
     * Broji koliko je korisnik ostavio komentara u zadnjih 60 minuta.
     * 
     * RATE LIMITING (3.6 zahtev):
     * - Jedan korisnik može maksimum 60 komentara po satu
     * - Broji se na osnovu userId i vremena kreiranja
     * - Ograničenje važi za SVE postove zajedno (ne po postu)
     * 
     * SQL UPIT:
     * SELECT COUNT(*) FROM comments 
     * WHERE user_id = ? AND created_at > ?
     * 
     * @param userId - ID korisnika
     * @param since - Vreme od kada se broji (npr. sada - 1 sat)
     * @return int - broj komentara u tom periodu
     * 
     * PRIMER:
     * LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
     * int count = commentRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
     * if (count >= 60) {
     *     throw new RuntimeException("Rate limit dostignut!");
     * }
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId AND c.createdAt > :since")
    int countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // ============================================
    // KORISNIČKE METODE
    // ============================================
    
    /**
     * Vraća sve komentare korisnika (za profil)
     * 
     * @param userId - ID korisnika
     * @return List<Comment> - svi komentari korisnika
     */
    List<Comment> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Broji ukupan broj komentara na postu
     * 
     * @param postId - ID posta
     * @return int - broj komentara
     */
    int countByPostId(Long postId);

    /**
     * Briše sve komentare sa određenog posta
     * Koristi se kada se post briše
     * 
     * @param postId - ID posta
     */
    void deleteByPostId(Long postId);
}