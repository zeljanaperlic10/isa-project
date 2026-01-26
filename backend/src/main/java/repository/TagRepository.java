package repository;

import model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    // ============================================
    // PRONALAŽENJE TAGA PO IMENU
    // ============================================
    
    // Pronalaženje taga po tačnom imenu (case-insensitive jer normalizujemo)
    Optional<Tag> findByName(String name);
    
    // Provera da li tag postoji
    boolean existsByName(String name);
    
    // ============================================
    // PRETRAGA TAGOVA
    // ============================================
    
    // Pronalaženje tagova koji sadrže određeni string (za autocomplete)
    List<Tag> findByNameContainingIgnoreCase(String keyword);
    
    // ============================================
    // POPULARNI TAGOVI
    // ============================================
    
    // Top N tagova po broju postova
    List<Tag> findTop10ByOrderByPostCountDesc();
    
    // Svi tagovi sortirani po popularnosti
    List<Tag> findAllByOrderByPostCountDesc();
}
