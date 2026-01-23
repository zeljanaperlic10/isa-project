package repository;


import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Pronalaženje korisnika po email-u
    Optional<User> findByEmail(String email);
    
    // Pronalaženje korisnika po username-u
    Optional<User> findByUsername(String username);
    
    // Provera da li postoji korisnik sa datim email-om
    boolean existsByEmail(String email);
    
    // Provera da li postoji korisnik sa datim username-om
    boolean existsByUsername(String username);
}