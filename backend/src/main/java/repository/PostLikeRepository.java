package repository;

import model.PostLike;
import model.User;
import model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // ============================================
    // PROVERA DA LI JE KORISNIK LAJKOVAO POST
    // ============================================

    /**
     * Proverava da li je korisnik već lajkovao post
     * @param userId - ID korisnika
     * @param postId - ID posta
     * @return true ako jeste, false ako nije
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    /**
     * Pronalazi like za kombinaciju korisnik + post
     * @param userId - ID korisnika
     * @param postId - ID posta
     * @return Optional<PostLike>
     */
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    /**
     * Pronalazi like za kombinaciju User objekat + Post objekat
     * @param user - User objekat
     * @param post - Post objekat
     * @return Optional<PostLike>
     */
    Optional<PostLike> findByUserAndPost(User user, Post post);

    // ============================================
    // BROJANJE LAJKOVA
    // ============================================

    /**
     * Broji ukupan broj lajkova na postu
     * @param postId - ID posta
     * @return broj lajkova
     */
    long countByPostId(Long postId);

    /**
     * Broji ukupan broj lajkova koje je korisnik dao
     * @param userId - ID korisnika
     * @return broj lajkova
     */
    long countByUserId(Long userId);

    // ============================================
    // BRISANJE LAJKA
    // ============================================

    /**
     * Briše like za kombinaciju korisnik + post
     * @param userId - ID korisnika
     * @param postId - ID posta
     */
    void deleteByUserIdAndPostId(Long userId, Long postId);
}
