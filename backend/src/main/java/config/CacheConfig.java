package config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;


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
           
            new ConcurrentMapCache("thumbnails"),
            
           
            new ConcurrentMapCache("comments")
        ));
        
        System.out.println("✅ Cache Manager konfigurisan sa 2 keš regiona:");
        System.out.println("   1. thumbnails (3.3 zahtev - thumbnail slike)");
        System.out.println("   2. comments (3.6 zahtev - komentari sa paginacijom)");
        
        return cacheManager;
    }
}