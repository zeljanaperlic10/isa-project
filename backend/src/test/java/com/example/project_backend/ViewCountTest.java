package com.example.project_backend;

import model.Post;
import model.User;
import repository.PostRepository;
import repository.UserRepository;
import service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ViewCountTest - Test skripta za demonstraciju atomic view count (3.7 zahtev)
 * 
 * TESTIRA:
 * 1. Istovremeni pristup istom videu od strane više korisnika
 * 2. Thread-safety atomic update operacije
 * 3. Konzistentnost brojača pregleda
 * 
 * ZAHTEV (3.7):
 * "Za potrebe demonstriranja mehanizma, napisati skriptu ili jedinični test 
 * koji simulira istovremenu posetu istom videu od strane više korisnika 
 * i pravilan inkrement broja pregleda."
 */
@SpringBootTest
@Transactional
public class ViewCountTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

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
        testPost.setViewsCount(0);  // Počinje sa 0
        testPost = postRepository.save(testPost);

        System.out.println("=".repeat(80));
        System.out.println("🧪 VIEW COUNT TEST - Setup");
        System.out.println("   Test User ID: " + testUser.getId());
        System.out.println("   Test Post ID: " + testPost.getId());
        System.out.println("   Initial Views: " + testPost.getViewsCount());
        System.out.println("=".repeat(80));
    }

    /**
     * TEST 1: Sekvencijalni pristup (baseline)
     * 
     * TESTIRA:
     * - 10 korisnika otvara video jedan po jedan
     * - Očekujemo 10 pregleda
     */
    @Test
    void testSequentialViewCount() throws Exception {
        System.out.println("\n📝 TEST 1: Sekvencijalni pristup (baseline)");
        System.out.println("-".repeat(80));

        int numberOfViews = 10;

        for (int i = 1; i <= numberOfViews; i++) {
            postService.incrementViewCount(testPost.getId());
            System.out.println("   ✅ Pregled " + i + " zabeležen");
        }

        // Proveri rezultat
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        int finalCount = updatedPost.getViewsCount();

        System.out.println("\n✅ REZULTAT:");
        System.out.println("   Očekivano: " + numberOfViews);
        System.out.println("   Dobijeno: " + finalCount);

        assertEquals(numberOfViews, finalCount);
    }

    /**
     * TEST 2: Istovremeni pristup (3.7 zahtev)
     * 
     * SIMULACIJA:
     * - 10 korisnika ISTOVREMENO otvara video
     * - Svaki poziva incrementViewCount()
     * 
     * OČEKIVANO:
     * - viewsCount = 10 (bez race condition-a!)
     * 
     * BEZ ATOMIC UPDATE:
     * - Mogući rezultat: 7, 8, 9... (race condition!)
     * 
     * SA ATOMIC UPDATE:
     * - Garantovano: 10 ✅
     */
    @Test
    void testConcurrentViewCount_Atomic() throws Exception {
        System.out.println("\n📝 TEST 2: Istovremeni pristup - ATOMIC UPDATE (3.7 zahtev)");
        System.out.println("-".repeat(80));

        int numberOfThreads = 10;
        
        System.out.println("   Simulacija: " + numberOfThreads + " korisnika ISTOVREMENO otvara video");
        System.out.println("   Implementacija: ATOMIC UPDATE (thread-safe)");
        System.out.println();

        // Kreiraj thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // Lista za praćenje rezultata
        List<Future<Boolean>> futures = new ArrayList<>();

        // Pokreni sve thread-ove ISTOVREMENO
        for (int i = 1; i <= numberOfThreads; i++) {
            final int threadNumber = i;
            
            Future<Boolean> future = executor.submit(() -> {
                try {
                    // Sačekaj da svi thread-ovi budu spremni
                    latch.countDown();
                    latch.await();

                    System.out.println("      👤 Korisnik " + threadNumber + " otvara video...");

                    // ATOMIC INCREMENT
                    postService.incrementViewCount(testPost.getId());

                    System.out.println("      ✅ Korisnik " + threadNumber + " - pregled zabeležen");
                    return true;

                } catch (Exception e) {
                    System.err.println("      ❌ Korisnik " + threadNumber + " - greška: " + e.getMessage());
                    return false;
                }
            });

            futures.add(future);
        }

        // Sačekaj da svi thread-ovi završe
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Proveri da su svi thread-ovi uspeli
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        System.out.println("\n   Uspešno završenih thread-ova: " + successCount + "/" + numberOfThreads);

        // Proveri finalni viewsCount
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        int finalCount = updatedPost.getViewsCount();

        System.out.println("\n✅ REZULTAT:");
        System.out.println("   Očekivano: " + numberOfThreads);
        System.out.println("   Dobijeno: " + finalCount);
        System.out.println("   Thread-safe: " + (finalCount == numberOfThreads ? "✅ DA" : "❌ NE"));

        assertEquals(numberOfThreads, successCount);
        assertEquals(numberOfThreads, finalCount);

        System.out.println("\n🎉 ATOMIC UPDATE RADI! Nema race condition-a!");
    }

    /**
     * TEST 3: Veliki broj istovremenih pristupa (stress test)
     * 
     * SIMULACIJA:
     * - 100 korisnika istovremeno
     * 
     * OČEKIVANO:
     * - viewsCount = 100
     */
    @Test
    void testConcurrentViewCount_StressTest() throws Exception {
        System.out.println("\n📝 TEST 3: Stress test - 100 istovremenih korisnika");
        System.out.println("-".repeat(80));

        int numberOfThreads = 100;
        
        System.out.println("   Simulacija: " + numberOfThreads + " korisnika ISTOVREMENO");
        System.out.println();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        List<Future<Boolean>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Pokreni sve thread-ove
        for (int i = 1; i <= numberOfThreads; i++) {
            final int threadNumber = i;
            
            Future<Boolean> future = executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    postService.incrementViewCount(testPost.getId());

                    if (threadNumber % 20 == 0) {
                        System.out.println("      ✅ " + threadNumber + " korisnika...");
                    }

                    return true;

                } catch (Exception e) {
                    return false;
                }
            });

            futures.add(future);
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Proveri rezultate
        int successCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        Post updatedPost = postRepository.findById(testPost.getId()).get();
        int finalCount = updatedPost.getViewsCount();

        System.out.println("\n✅ REZULTAT:");
        System.out.println("   Uspešno: " + successCount + "/" + numberOfThreads);
        System.out.println("   Očekivano: " + numberOfThreads);
        System.out.println("   Dobijeno: " + finalCount);
        System.out.println("   Trajanje: " + duration + "ms");
        System.out.println("   Thread-safe: " + (finalCount == numberOfThreads ? "✅ DA" : "❌ NE"));

        assertEquals(numberOfThreads, successCount);
        assertEquals(numberOfThreads, finalCount);

        System.out.println("\n🎉 STRESS TEST USPEŠAN! Atomic update podnosi veliki load!");
    }

    /**
     * TEST 4: Provera postojanja posta
     * 
     * TESTIRA:
     * - Šta se desi ako post ne postoji
     */
    @Test
    void testIncrementViewCount_PostNotFound() {
        System.out.println("\n📝 TEST 4: Increment za nepostojeći post");
        System.out.println("-".repeat(80));

        Long fakePostId = 99999L;

        System.out.println("   Pozivam incrementViewCount(" + fakePostId + ")...");
        
        // Ne treba da baci exception, samo log
        postService.incrementViewCount(fakePostId);

        System.out.println("\n✅ Metoda se izvršila bez exception-a (vraća 0 affected rows)");
    }

    /**
     * TEST 5: getPostById() automatski inkrementuje
     * 
     * TESTIRA:
     * - Da li getPostById() poziva incrementViewCount()
     */
    @Test
    void testGetPostById_AutomaticallyIncrementsViews() {
        System.out.println("\n📝 TEST 5: getPostById() automatski inkrementuje viewsCount");
        System.out.println("-".repeat(80));

        int initialViews = testPost.getViewsCount();
        System.out.println("   Početni broj pregleda: " + initialViews);

        // Pozovi getPostById() 3 puta
        for (int i = 1; i <= 3; i++) {
            postService.getPostById(testPost.getId());
            System.out.println("   ✅ getPostById() poziv " + i);
        }

        // Proveri da je viewsCount uvećan za 3
        Post updatedPost = postRepository.findById(testPost.getId()).get();
        int finalViews = updatedPost.getViewsCount();

        System.out.println("\n✅ REZULTAT:");
        System.out.println("   Početno: " + initialViews);
        System.out.println("   Finalno: " + finalViews);
        System.out.println("   Razlika: " + (finalViews - initialViews));

        assertEquals(initialViews + 3, finalViews);

        System.out.println("\n🎉 getPostById() AUTOMATSKI inkrementuje preglede!");
    }
}