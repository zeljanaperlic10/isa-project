package config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * CacheConfig - Konfiguracija keširanje sistema
 * 
 * KEŠIRANI RESURSI:
 * 1. Thumbnail slike (3.3 zahtev) - Da ne čitamo sa file sistema svaki put
 * 2. Komentari (3.6 zahtev) - Smanjuje opterećenje baze
 * 
 * TIP KEŠA:
 * - ConcurrentMapCache - In-memory keš
 * - Thread-safe
 * - Brz pristup
 * - Gubi se pri restartu aplikacije (to je OK)
 */
@Configuration
@EnableCaching  // Omogućava @Cacheable, @CacheEvict anotacije u servisima
public class CacheConfig {

    public CacheConfig() {
        System.out.println("🔧 CacheConfig se inicijalizuje...");
    }

    @Bean
    public CacheManager cacheManager() {
        System.out.println("💾 Konfigurisanje Cache Manager-a...");
        
        // SimpleCacheManager - jednostavan keš menadžer
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Definišemo keš regione
        cacheManager.setCaches(Arrays.asList(
            // ============================================
            // THUMBNAIL CACHE (3.3 zahtev)
            // ============================================
            // Koristi se u FileStorageService.loadThumbnailAsResource()
            // Key: ime thumbnail fajla (npr. "abc-123.jpg")
            // Value: Resource objekat (slika)
            new ConcurrentMapCache("thumbnails"),
            
            // ============================================
            // COMMENTS CACHE (3.6 zahtev)
            // ============================================
            // Koristi se u CommentService.getCommentsByPost()
            // Key format: 
            //   - "postId-page" za paginaciju (npr. "5-0", "5-1")
            //   - "postId-all" za sve komentare
            // Value: Page<CommentDTO> ili List<CommentDTO>
            //
            // Primer:
            //   Key: "5-0" → Stranica 0 komentara za post 5
            //   Key: "8-2" → Stranica 2 komentara za post 8
            new ConcurrentMapCache("comments")
        ));
        
        System.out.println("✅ Cache Manager konfigurisan sa 2 keš regiona:");
        System.out.println("   1. thumbnails (3.3 zahtev - thumbnail slike)");
        System.out.println("   2. comments (3.6 zahtev - komentari sa paginacijom)");
        
        return cacheManager;
    }
}