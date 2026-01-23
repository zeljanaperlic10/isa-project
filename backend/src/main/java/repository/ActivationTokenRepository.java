package repository;

import model.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    
    // Pronalaženje tokena po string vrednosti tokena
    Optional<ActivationToken> findByToken(String token);
    
    // Pronalaženje tokena po User ID-u
    Optional<ActivationToken> findByUserId(Long userId);
}
