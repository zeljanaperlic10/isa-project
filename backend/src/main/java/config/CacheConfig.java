package config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracija za keširanje thumbnail slika (3.3 zahtev).
 * 
 * Umesto da se svaki put čita sa file sistema,
 * thumbnail slike se keširaju u memoriji.
 * 
 * Koristi se @Cacheable anotacija u FileStorageService-u.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public CacheConfig() {
        System.out.println("📦 CACHE CONFIG SE UČITAVA! (3.3 - thumbnail caching)");
    }

    @Bean
    public CacheManager cacheManager() {
        System.out.println("✅ CacheManager kreiran - thumbnail slike će biti keširane!");
        System.out.println("   Cache type: In-Memory (ConcurrentMapCache)");
        System.out.println("   Cache names: thumbnails");
        
        // ConcurrentMapCacheManager - jednostavan in-memory cache
        // Za produkciju: može se koristiti Redis, Ehcache, itd.
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("thumbnails");
        
        return cacheManager;
    }
}
