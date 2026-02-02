package service;

import dto.CommentDTO;
import model.Comment;
import model.Post;
import model.User;
import repository.CommentRepository;
import repository.PostRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CommentService - Business logika za komentare (3.6 zahtev)
 * 
 * FUNKCIONALNOSTI:
 * - Kreiranje komentara sa rate limiting (60/sat)
 * - Paginacija komentara (velika količina)
 * - Keširanje (3.6 zahtev)
 * - Sortiranje najnoviji → najstariji (3.6 zahtev)
 * - Brisanje komentara
 */
@Service
public class CommentService {

    // ============================================
    // KONSTANTE
    // ============================================
    
    /**
     * Broj komentara po stranici (3.6 - paginacija)
     */
    private static final int COMMENTS_PER_PAGE = 20;

    // ============================================
    // ZAVISNOSTI
    // ============================================
    
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRateLimitService rateLimitService;

    @Autowired
    private PostService postService;  // Za inkrementiranje commentsCount

    // ============================================
    // KREIRANJE KOMENTARA (3.6 zahtev)
    // ============================================
    
    /**
     * Kreira novi komentar na postu.
     * 
     * ZAHTEVI (3.6):
     * - Samo registrovani korisnici (JWT proverava Controller)
     * - Rate limiting: maksimum 60 komentara po satu
     * - Komentar sadrži samo tekst
     * - Automatski postavlja vreme kreiranja
     * 
     * @param postId - ID posta
     * @param email - Email korisnika (iz JWT tokena)
     * @param text - Tekst komentara
     * @return CommentDTO
     * @throws RuntimeException ako post ne postoji, user ne postoji, ili rate limit
     */
    @Transactional
    @CacheEvict(value = "comments", key = "#postId")  // Obriši keš za ovaj post
    public CommentDTO createComment(Long postId, String email, String text) {
        System.out.println("💬 Kreiranje komentara - START");
        System.out.println("   Post ID: " + postId);
        System.out.println("   Email: " + email);

        // ============================================
        // KORAK 1: Validacija teksta
        // ============================================
        
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Tekst komentara je obavezan!");
        }

        if (text.length() > 1000) {
            throw new RuntimeException("Komentar može imati maksimum 1000 karaktera!");
        }

        System.out.println("✅ Tekst validan: " + text.length() + " karaktera");

        // ============================================
        // KORAK 2: Pronalaženje posta
        // ============================================
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen: " + postId);
        }
        Post post = postOpt.get();
        System.out.println("✅ Post pronađen: " + post.getTitle());

        // ============================================
        // KORAK 3: Pronalaženje korisnika
        // ============================================
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen: " + email);
        }
        User user = userOpt.get();
        System.out.println("✅ Korisnik pronađen: " + user.getUsername());

        // ============================================
        // KORAK 4: RATE LIMITING PROVERA (3.6 zahtev - 60/sat)
        // ============================================
        
        rateLimitService.checkRateLimitOrThrow(user.getId());
        // Ako je dostigao limit → baca exception!
        // Ako nije → nastavlja dalje
        
        int remaining = rateLimitService.getRemainingComments(user.getId());
        System.out.println("✅ Rate limit OK - Preostalo: " + remaining + " komentara");

        // ============================================
        // KORAK 5: Kreiranje komentara
        // ============================================
        
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setText(text.trim());
        // createdAt se automatski postavlja u @PrePersist

        Comment savedComment = commentRepository.save(comment);
        System.out.println("✅ Komentar sačuvan - ID: " + savedComment.getId());

        // ============================================
        // KORAK 6: Inkrementiranje brojača komentara na postu
        // ============================================
        
        postService.incrementCommentsCount(postId);
        System.out.println("✅ Brojač komentara na postu ažuriran");

        System.out.println("🎉 Komentar uspešno kreiran!");

        // ============================================
        // KORAK 7: Konverzija u DTO i vraćanje
        // ============================================
        
        return new CommentDTO(savedComment);
    }

    // ============================================
    // DOBIJANJE KOMENTARA SA PAGINACIJOM (3.6)
    // ============================================
    
    /**
     * Vraća komentare za post sa paginacijom.
     * 
     * KEŠIRANJE (3.6 zahtev):
     * - @Cacheable - rezultat se kešira
     * - Key: postId + page
     * - Smanjuje opterećenje baze
     * 
     * PAGINACIJA (3.6 zahtev):
     * - Vraća samo 20 komentara po stranici
     * - Frontend može učitavati više (infinite scroll)
     * 
     * SORTIRANJE (3.6 zahtev):
     * - Najnoviji prvo (ORDER BY createdAt DESC)
     * 
     * @param postId - ID posta
     * @param page - Broj stranice (0 = prva)
     * @return Page<CommentDTO> - stranica komentara + metadata
     */
    @Cacheable(value = "comments", key = "#postId + '-' + #page")
    public Page<CommentDTO> getCommentsByPost(Long postId, int page) {
        System.out.println("📖 Učitavanje komentara:");
        System.out.println("   Post ID: " + postId);
        System.out.println("   Stranica: " + page);
        System.out.println("   Komentara po stranici: " + COMMENTS_PER_PAGE);

        // Kreiranje Pageable objekta
        Pageable pageable = PageRequest.of(
            page, 
            COMMENTS_PER_PAGE, 
            Sort.by("createdAt").descending()  // Najnoviji prvo (3.6 zahtev)
        );

        // Dobijanje komentara iz baze
        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);

        System.out.println("✅ Učitano " + commentPage.getNumberOfElements() + " komentara");
        System.out.println("   Ukupno stranica: " + commentPage.getTotalPages());
        System.out.println("   Ukupno komentara: " + commentPage.getTotalElements());

        // Konverzija u DTO
        Page<CommentDTO> dtoPage = commentPage.map(CommentDTO::new);

        return dtoPage;
    }

    /**
     * Vraća SVE komentare za post (bez paginacije).
     * Koristi se samo za male količine.
     * 
     * @param postId - ID posta
     * @return List<CommentDTO>
     */
    @Cacheable(value = "comments", key = "#postId + '-all'")
    public List<CommentDTO> getAllCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        
        return comments.stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }

    // ============================================
    // BRISANJE KOMENTARA
    // ============================================
    
    /**
     * Briše komentar.
     * Samo vlasnik komentara može da ga obriše.
     * 
     * @param commentId - ID komentara
     * @param email - Email korisnika (iz JWT tokena)
     * @throws RuntimeException ako komentar ne postoji ili korisnik nije vlasnik
     */
    @Transactional
    @CacheEvict(value = "comments", allEntries = true)  // Obriši sav keš (ne znamo koji post)
    public void deleteComment(Long commentId, String email) {
        System.out.println("🗑️ Brisanje komentara - ID: " + commentId);

        // Pronalaženje komentara
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            throw new RuntimeException("Komentar nije pronađen: " + commentId);
        }
        Comment comment = commentOpt.get();

        // Provera vlasništva
        if (!comment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Nemate pravo da obrišete ovaj komentar!");
        }

        Long postId = comment.getPost().getId();

        // Brisanje
        commentRepository.deleteById(commentId);
        System.out.println("✅ Komentar obrisan");

        // Dekrementiranje brojača
        postService.decrementCommentsCount(postId);
        System.out.println("✅ Brojač komentara na postu ažuriran");
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================
    
    /**
     * Vraća ukupan broj komentara na postu.
     * 
     * @param postId - ID posta
     * @return broj komentara
     */
    public int getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    /**
     * Vraća komentare korisnika (za profil).
     * 
     * @param userId - ID korisnika
     * @return List<CommentDTO>
     */
    public List<CommentDTO> getUserComments(Long userId) {
        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return comments.stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }
}