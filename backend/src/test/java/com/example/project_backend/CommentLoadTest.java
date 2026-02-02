package com.example.project_backend;

import model.Comment;
import model.Post;
import model.User;
import repository.CommentRepository;
import repository.PostRepository;
import repository.UserRepository;
import service.CommentRateLimitService;
import service.CommentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import dto.CommentDTO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentLoadTest - Test skripta za demonstraciju rate limiting-a (3.6 zahtev)
 * 
 * TESTIRA:
 * 1. Kreiranje velikog broja komentara
 * 2. Rate limiting (60 komentara po satu)
 * 3. Paginaciju komentara
 * 4. Keširanje
 */
@SpringBootTest
@Transactional
public class CommentLoadTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRateLimitService rateLimitService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // Kreiraj test korisnika
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address 123");  // ← DODATO!
        testUser.setActivated(true);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Kreiraj test post
        testPost = new Post();
        testPost.setTitle("Test Video");
        testPost.setDescription("Test Description");
        testPost.setVideoUrl("http://example.com/video.mp4");
        testPost.setThumbnailUrl("http://example.com/thumb.jpg");
        testPost.setUser(testUser);
        testPost = postRepository.save(testPost);

        System.out.println("=".repeat(80));
        System.out.println("🧪 COMMENT LOAD TEST - Setup");
        System.out.println("   Test User ID: " + testUser.getId());
        System.out.println("   Test Post ID: " + testPost.getId());
        System.out.println("=".repeat(80));
    }

    /**
     * TEST 1: Kreiranje 60 komentara (maksimalni limit)
     * 
     * OČEKIVANO:
     * - Svih 60 komentara treba da prođe
     * - Komentari se čuvaju u bazi
     */
    @Test
    void testCreate60Comments_ShouldSucceed() {
        System.out.println("\n📝 TEST 1: Kreiranje 60 komentara (maksimalni limit)");
        System.out.println("-".repeat(80));

        int successCount = 0;

        for (int i = 1; i <= 60; i++) {
            try {
                String text = "Test komentar broj " + i;
                CommentDTO comment = commentService.createComment(
                    testPost.getId(),
                    testUser.getEmail(),
                    text
                );

                assertNotNull(comment);
                assertEquals(text, comment.getText());
                successCount++;

                if (i % 10 == 0) {
                    System.out.println("   ✅ Kreirana " + i + " komentara");
                }
            } catch (Exception e) {
                fail("Komentar " + i + " nije trebao biti odbijen: " + e.getMessage());
            }
        }

        System.out.println("\n✅ REZULTAT: Sve 60 komentara uspešno kreirano!");
        assertEquals(60, successCount);
    }

    /**
     * TEST 2: Pokušaj kreiranja 61. komentara (preko limita)
     * 
     * OČEKIVANO:
     * - 60 komentara treba da prođe
     * - 61. komentar treba da bude odbijen sa "Rate limit dostignut"
     */
    @Test
    void testCreate61Comments_ShouldFailOnLast() {
        System.out.println("\n📝 TEST 2: Kreiranje 61 komentara (preko limita)");
        System.out.println("-".repeat(80));

        int successCount = 0;
        int failedCount = 0;

        for (int i = 1; i <= 61; i++) {
            try {
                String text = "Test komentar broj " + i;
                CommentDTO comment = commentService.createComment(
                    testPost.getId(),
                    testUser.getEmail(),
                    text
                );

                successCount++;

                if (i % 10 == 0) {
                    System.out.println("   ✅ Kreirana " + i + " komentara");
                }
            } catch (RuntimeException e) {
                System.out.println("   ❌ Komentar " + i + " odbijen: " + e.getMessage());
                failedCount++;
                
                // 61. komentar treba da bude odbijen
                if (i == 61) {
                    assertTrue(e.getMessage().contains("Rate limit"));
                    System.out.println("\n✅ RATE LIMIT RADI! 61. komentar je odbijen.");
                }
            }
        }

        System.out.println("\n✅ REZULTAT:");
        System.out.println("   Uspešno: " + successCount + " komentara");
        System.out.println("   Odbijeno: " + failedCount + " komentara");

        assertEquals(60, successCount);
        assertEquals(1, failedCount);
    }

    /**
     * TEST 3: Paginacija komentara (3.6 zahtev)
     * 
     * TESTIRA:
     * - Kreiranje 50 komentara
     * - Učitavanje prve stranice (20 komentara)
     * - Učitavanje druge stranice (20 komentara)
     * - Provera ukupnog broja stranica
     */
    @Test
    void testCommentPagination() {
        System.out.println("\n📝 TEST 3: Paginacija komentara");
        System.out.println("-".repeat(80));

        // Kreiraj 50 komentara
        System.out.println("   Kreiranje 50 komentara...");
        for (int i = 1; i <= 50; i++) {
            commentService.createComment(
                testPost.getId(),
                testUser.getEmail(),
                "Komentar broj " + i
            );
        }
        System.out.println("   ✅ 50 komentara kreirano");

        // Učitaj prvu stranicu (page 0)
        System.out.println("\n   Učitavanje stranice 0...");
        Page<CommentDTO> page0 = commentService.getCommentsByPost(testPost.getId(), 0);
        
        System.out.println("   📄 Stranica 0:");
        System.out.println("      - Komentara na stranici: " + page0.getNumberOfElements());
        System.out.println("      - Ukupno komentara: " + page0.getTotalElements());
        System.out.println("      - Ukupno stranica: " + page0.getTotalPages());
        System.out.println("      - Ima još: " + page0.hasNext());

        assertEquals(20, page0.getNumberOfElements());
        assertEquals(50, page0.getTotalElements());
        assertEquals(3, page0.getTotalPages());
        assertTrue(page0.hasNext());

        // Učitaj drugu stranicu (page 1)
        System.out.println("\n   Učitavanje stranice 1...");
        Page<CommentDTO> page1 = commentService.getCommentsByPost(testPost.getId(), 1);
        
        System.out.println("   📄 Stranica 1:");
        System.out.println("      - Komentara na stranici: " + page1.getNumberOfElements());
        System.out.println("      - Ima još: " + page1.hasNext());

        assertEquals(20, page1.getNumberOfElements());
        assertTrue(page1.hasNext());

        // Učitaj treću stranicu (page 2) - poslednja
        System.out.println("\n   Učitavanje stranice 2...");
        Page<CommentDTO> page2 = commentService.getCommentsByPost(testPost.getId(), 2);
        
        System.out.println("   📄 Stranica 2 (poslednja):");
        System.out.println("      - Komentara na stranici: " + page2.getNumberOfElements());
        System.out.println("      - Ima još: " + page2.hasNext());

        assertEquals(10, page2.getNumberOfElements());
        assertFalse(page2.hasNext());

        System.out.println("\n✅ PAGINACIJA RADI ISPRAVNO!");
    }

    /**
     * TEST 4: Rate limiting provera
     * 
     * TESTIRA:
     * - canComment() metodu
     * - getCommentCountInLastHour() metodu
     * - getRemainingComments() metodu
     */
    @Test
    void testRateLimitingMethods() {
        System.out.println("\n📝 TEST 4: Rate limiting metode");
        System.out.println("-".repeat(80));

        // Na početku - korisnik može komentarisati
        assertTrue(rateLimitService.canComment(testUser.getId()));
        System.out.println("   ✅ Korisnik može komentarisati (0 komentara)");

        // Kreiraj 30 komentara
        for (int i = 1; i <= 30; i++) {
            commentService.createComment(
                testPost.getId(),
                testUser.getEmail(),
                "Komentar " + i
            );
        }

        int count = rateLimitService.getCommentCountInLastHour(testUser.getId());
        int remaining = rateLimitService.getRemainingComments(testUser.getId());
        
        System.out.println("\n   Nakon 30 komentara:");
        System.out.println("      - Komentara u zadnjih sat: " + count);
        System.out.println("      - Preostalo: " + remaining);
        
        assertEquals(30, count);
        assertEquals(30, remaining);
        assertTrue(rateLimitService.canComment(testUser.getId()));

        // Kreiraj još 30 komentara (ukupno 60)
        for (int i = 31; i <= 60; i++) {
            commentService.createComment(
                testPost.getId(),
                testUser.getEmail(),
                "Komentar " + i
            );
        }

        count = rateLimitService.getCommentCountInLastHour(testUser.getId());
        remaining = rateLimitService.getRemainingComments(testUser.getId());
        
        System.out.println("\n   Nakon 60 komentara:");
        System.out.println("      - Komentara u zadnjih sat: " + count);
        System.out.println("      - Preostalo: " + remaining);
        System.out.println("      - Može komentarisati: " + rateLimitService.canComment(testUser.getId()));
        
        assertEquals(60, count);
        assertEquals(0, remaining);
        assertFalse(rateLimitService.canComment(testUser.getId()));

        System.out.println("\n✅ RATE LIMITING METODE RADE ISPRAVNO!");
    }

    /**
     * TEST 5: Sortiranje komentara (najnoviji prvo - 3.6 zahtev)
     */
    @Test
    void testCommentSorting_NewestFirst() {
        System.out.println("\n📝 TEST 5: Sortiranje komentara (najnoviji prvo)");
        System.out.println("-".repeat(80));

        // Kreiraj 5 komentara
        for (int i = 1; i <= 5; i++) {
            commentService.createComment(
                testPost.getId(),
                testUser.getEmail(),
                "Komentar broj " + i
            );
            
            // Mali delay između komentara
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // Učitaj komentare
        Page<CommentDTO> page = commentService.getCommentsByPost(testPost.getId(), 0);
        
        System.out.println("\n   Redosled komentara:");
        for (int i = 0; i < page.getContent().size(); i++) {
            CommentDTO comment = page.getContent().get(i);
            System.out.println("      " + (i + 1) + ". " + comment.getText());
        }

        // Proveri da je najnoviji prvi
        assertEquals("Komentar broj 5", page.getContent().get(0).getText());
        assertEquals("Komentar broj 4", page.getContent().get(1).getText());
        assertEquals("Komentar broj 1", page.getContent().get(4).getText());

        System.out.println("\n✅ SORTIRANJE RADI ISPRAVNO (najnoviji prvo)!");
    }
}