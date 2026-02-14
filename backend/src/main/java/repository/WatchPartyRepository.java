package repository;

import model.WatchParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WatchPartyRepository extends JpaRepository<WatchParty, Long> {

    List<WatchParty> findByActiveOrderByCreatedAtDesc(Boolean active);
    
    List<WatchParty> findByCreatorUsernameOrderByCreatedAtDesc(String username);
    
    // DODAJ OVO - traži po email-u:
    @Query("SELECT wp FROM WatchParty wp WHERE wp.creator.email = :email ORDER BY wp.createdAt DESC")
    List<WatchParty> findByCreatorEmailOrderByCreatedAtDesc(@Param("email") String email);
    
    List<WatchParty> findByCreatorUsernameAndActive(String username, Boolean active);
    
    Boolean existsByCreatorUsernameAndActive(String username, Boolean active);
    
    Long countByActive(Boolean active);
    
    @Query("SELECT wp FROM WatchParty wp WHERE :usernameOrEmail MEMBER OF wp.members AND wp.active = true")
    List<WatchParty> findPartiesByMember(@Param("usernameOrEmail") String usernameOrEmail);
}
