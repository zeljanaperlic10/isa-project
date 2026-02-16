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


@Service
public class CommentService {

   
    private static final int COMMENTS_PER_PAGE = 20;

    
    
    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRateLimitService rateLimitService;

    @Autowired
    private PostService postService;

  
    
   
    @Transactional
    @CacheEvict(value = "comments", allEntries = true)  // ← PROMENJENO! Briše SVE keš unose
    public CommentDTO createComment(Long postId, String email, String text) {
        System.out.println("💬 Kreiranje komentara - START");
        System.out.println("   Post ID: " + postId);
        System.out.println("   Email: " + email);

       
        
        if (text == null || text.trim().isEmpty()) {
            throw new RuntimeException("Tekst komentara je obavezan!");
        }

        if (text.length() > 1000) {
            throw new RuntimeException("Komentar može imati maksimum 1000 karaktera!");
        }

        System.out.println("✅ Tekst validan: " + text.length() + " karaktera");

     
        
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Post nije pronađen: " + postId);
        }
        Post post = postOpt.get();
        System.out.println("✅ Post pronađen: " + post.getTitle());

      
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen: " + email);
        }
        User user = userOpt.get();
        System.out.println("✅ Korisnik pronađen: " + user.getUsername());

        
        
        rateLimitService.checkRateLimitOrThrow(user.getId());
        
        int remaining = rateLimitService.getRemainingComments(user.getId());
        System.out.println("✅ Rate limit OK - Preostalo: " + remaining + " komentara");

        
        
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setText(text.trim());

        Comment savedComment = commentRepository.save(comment);
        System.out.println("✅ Komentar sačuvan - ID: " + savedComment.getId());

     
        
        postService.incrementCommentsCount(postId);
        System.out.println("✅ Brojač komentara na postu ažuriran");

        System.out.println("🎉 Komentar uspešno kreiran!");

        
        
        return new CommentDTO(savedComment);
    }

    // ============================================
    // DOBIJANJE KOMENTARA SA PAGINACIJOM (3.6)
    // ============================================
    
   
    @Cacheable(value = "comments", key = "#postId + '-' + #page")
    public Page<CommentDTO> getCommentsByPost(Long postId, int page) {
        System.out.println("📖 Učitavanje komentara:");
        System.out.println("   Post ID: " + postId);
        System.out.println("   Stranica: " + page);
        System.out.println("   Komentara po stranici: " + COMMENTS_PER_PAGE);

        Pageable pageable = PageRequest.of(
            page, 
            COMMENTS_PER_PAGE, 
            Sort.by("createdAt").descending()
        );

        Page<Comment> commentPage = commentRepository.findByPostIdOrderByCreatedAtDesc(postId, pageable);

        System.out.println("✅ Učitano " + commentPage.getNumberOfElements() + " komentara");
        System.out.println("   Ukupno stranica: " + commentPage.getTotalPages());
        System.out.println("   Ukupno komentara: " + commentPage.getTotalElements());

        Page<CommentDTO> dtoPage = commentPage.map(CommentDTO::new);

        return dtoPage;
    }

   
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
    
    
    @Transactional
    @CacheEvict(value = "comments", allEntries = true)
    public void deleteComment(Long commentId, String email) {
        System.out.println("🗑️ Brisanje komentara - ID: " + commentId);

        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            throw new RuntimeException("Komentar nije pronađen: " + commentId);
        }
        Comment comment = commentOpt.get();

        if (!comment.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Nemate pravo da obrišete ovaj komentar!");
        }

        Long postId = comment.getPost().getId();

        commentRepository.deleteById(commentId);
        System.out.println("✅ Komentar obrisan");

        postService.decrementCommentsCount(postId);
        System.out.println("✅ Brojač komentara na postu ažuriran");
    }

    
    public int getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    
    public List<CommentDTO> getUserComments(Long userId) {
        List<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        return comments.stream()
                .map(CommentDTO::new)
                .collect(Collectors.toList());
    }
}