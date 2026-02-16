package service;

import repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class CommentRateLimitService {

    // ============================================
    // KONSTANTE
    // ============================================
    
    
    private static final int MAX_COMMENTS_PER_HOUR = 60;

   
    private static final int TIME_WINDOW_HOURS = 1;

    // ============================================
    // ZAVISNOSTI
    // ============================================
    
    @Autowired
    private CommentRepository commentRepository;

    // ============================================
    // PUBLIC METODE
    // ============================================
    
    
    public boolean canComment(Long userId) {
        // Provera null parametra
        if (userId == null) {
            return false;
        }

        // Vreme pre jednog sata
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);

        // Broji komentare u zadnjih sat vremena
        int commentsInLastHour = commentRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);

        System.out.println("🕐 Rate Limit Check:");
        System.out.println("   User ID: " + userId);
        System.out.println("   Komentara u zadnjih " + TIME_WINDOW_HOURS + "h: " + commentsInLastHour);
        System.out.println("   Limit: " + MAX_COMMENTS_PER_HOUR);

        // Provera limita
        boolean canComment = commentsInLastHour < MAX_COMMENTS_PER_HOUR;

        if (!canComment) {
            System.out.println("   ❌ LIMIT DOSTIGNUT! Korisnik ne može komentarisati.");
        } else {
            System.out.println("   ✅ OK - Korisnik može komentarisati.");
        }

        return canComment;
    }

   
    public int getCommentCountInLastHour(Long userId) {
        if (userId == null) {
            return 0;
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        return commentRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
    }

  
    public int getRemainingComments(Long userId) {
        int usedComments = getCommentCountInLastHour(userId);
        int remaining = MAX_COMMENTS_PER_HOUR - usedComments;
        return Math.max(0, remaining);  // Ne vraća negativan broj
    }

   
    public void checkRateLimitOrThrow(Long userId) {
        if (!canComment(userId)) {
            int remaining = getRemainingComments(userId);
            throw new RuntimeException(
                "Rate limit dostignut! Možete ostaviti maksimum " + MAX_COMMENTS_PER_HOUR + 
                " komentara po satu. Preostalo: " + remaining + 
                ". Pokušajte ponovo za nekoliko minuta."
            );
        }
    }

   
    
    
    public int getMaxCommentsPerHour() {
        return MAX_COMMENTS_PER_HOUR;
    }

    
    public int getTimeWindowHours() {
        return TIME_WINDOW_HOURS;
    }
}

