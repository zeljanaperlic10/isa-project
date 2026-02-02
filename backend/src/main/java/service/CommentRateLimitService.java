package service;

import repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * CommentRateLimitService - Implementira rate limiting za komentare (3.6 zahtev)
 * 
 * ZAHTEV (3.6):
 * "Potrebno je ograničiti broj komentara koje može da postavi jedna osoba 
 * sa svog profila u vremenskom intervalu od sat vremena. 
 * Broj dozvoljenih komentara po nalogu je 60 po satu."
 * 
 * VAŽNO:
 * - Limit važi za CELOG korisnika (SVE postove zajedno)
 * - NE važi po postu (korisnik može komentarisati različite objave)
 * - Ali UKUPAN broj komentara ne sme biti veći od 60 u sat vremena
 */
@Service
public class CommentRateLimitService {

    // ============================================
    // KONSTANTE
    // ============================================
    
    /**
     * Maksimalni broj komentara po korisniku u sat vremena (3.6 zahtev)
     */
    private static final int MAX_COMMENTS_PER_HOUR = 60;

    /**
     * Vremenski interval u satima
     */
    private static final int TIME_WINDOW_HOURS = 1;

    // ============================================
    // ZAVISNOSTI
    // ============================================
    
    @Autowired
    private CommentRepository commentRepository;

    // ============================================
    // PUBLIC METODE
    // ============================================
    
    /**
     * Proverava da li korisnik može da ostavi komentar.
     * 
     * LOGIKA (3.6):
     * 1. Izračunaj vreme pre 1 sat
     * 2. Izbroj koliko je korisnik ostavio komentara u tom periodu
     * 3. Ako je >= 60 → BLOKIRAJ
     * 4. Ako je < 60 → DOZVOLI
     * 
     * @param userId - ID korisnika
     * @return true ako može komentarisati, false ako je dostigao limit
     */
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

    /**
     * Vraća koliko je korisnik ostavio komentara u zadnjih sat vremena.
     * 
     * @param userId - ID korisnika
     * @return broj komentara
     */
    public int getCommentCountInLastHour(Long userId) {
        if (userId == null) {
            return 0;
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(TIME_WINDOW_HOURS);
        return commentRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
    }

    /**
     * Vraća koliko još komentara korisnik može da ostavi.
     * 
     * @param userId - ID korisnika
     * @return broj preostalih komentara (0 ako je dostigao limit)
     */
    public int getRemainingComments(Long userId) {
        int usedComments = getCommentCountInLastHour(userId);
        int remaining = MAX_COMMENTS_PER_HOUR - usedComments;
        return Math.max(0, remaining);  // Ne vraća negativan broj
    }

    /**
     * Proverava limit i baca exception ako je dostignut.
     * Koristi se u Service sloju pre kreiranja komentara.
     * 
     * @param userId - ID korisnika
     * @throws RuntimeException ako je limit dostignut
     */
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

    // ============================================
    // HELPER METODE
    // ============================================
    
    /**
     * Vraća maksimalni broj dozvoljenih komentara po satu.
     * 
     * @return 60 (3.6 zahtev)
     */
    public int getMaxCommentsPerHour() {
        return MAX_COMMENTS_PER_HOUR;
    }

    /**
     * Vraća vremenski prozor u satima.
     * 
     * @return 1 sat
     */
    public int getTimeWindowHours() {
        return TIME_WINDOW_HOURS;
    }
}

