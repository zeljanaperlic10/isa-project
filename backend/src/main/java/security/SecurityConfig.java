package security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        System.out.println("🔥🔥🔥 SECURITY CONFIG SE UČITAVA! 🔥🔥🔥");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        System.out.println("✅ PasswordEncoder Bean kreiran!");
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("🔐🔐🔐 SECURITY FILTER CHAIN SE KREIRA! 🔐🔐🔐");
        
        http
            // Isključivanje CSRF (ne treba za REST API)
            .csrf(csrf -> {
                System.out.println("🚫 CSRF isključen!");
                csrf.disable();
            })
            
            // CORS konfiguracija
            .cors(cors -> {
                System.out.println("🌐 CORS konfigurisan!");
                cors.configurationSource(corsConfigurationSource());
            })
            
            // Autorizacija zahteva - AŽURIRANO za 3.1 i 3.3
            .authorizeHttpRequests(auth -> {
                System.out.println("📋 Konfigurisem authorization rules...");
                
                // ============================================
                // JAVNO DOSTUPNO (3.1 zahtev)
                // ============================================
                System.out.println("✅ Javni endpoint-i:");
                System.out.println("   - /auth/** (registracija, login, aktivacija)");
                System.out.println("   - GET /api/posts/** (prikaz postova - 3.1)");
                System.out.println("   - GET /api/videos/** (streaming videa - 3.1)");
                System.out.println("   - GET /api/thumbnails/** (thumbnail slike - 3.1)");
                System.out.println("   - GET /api/users/** (profil korisnika - 3.1)");
                System.out.println("   - GET /api/posts/{id}/comments (čitanje komentara - 3.6)");
                
                auth
                    // Auth endpoint-i (registracija, login, aktivacija)
                    .requestMatchers("/auth/**").permitAll()
                    
                    // Postovi - GET je javno (3.1 zahtev - neautentifikovani mogu videti)
                    .requestMatchers("GET", "/api/posts/**").permitAll()
                    
                    // Video streaming - javno dostupno (3.1)
                    .requestMatchers("GET", "/api/videos/**").permitAll()
                    
                    // Thumbnail slike - javno dostupno (3.1)
                    .requestMatchers("GET", "/api/thumbnails/**").permitAll()
                    
                    // Korisnici - GET je javno (3.1 zahtev - profil stranica)
                    .requestMatchers("GET", "/api/users/**").permitAll()
                    
                    // ============================================
                    // ZAHTEVA AUTENTIFIKACIJU (3.3 i 3.6 zahtevi)
                    // ============================================
                    // POST /api/posts - kreiranje posta (samo registrovani - 3.3)
                    // DELETE /api/posts/** - brisanje posta (samo registrovani)
                    // POST /api/posts/{id}/comments - kreiranje komentara (samo registrovani - 3.6)
                    // DELETE /api/comments/** - brisanje komentara (samo registrovani - 3.6)
                    // Sve ostalo
                    .anyRequest().authenticated();
            })
            
            // Stateless sesija (koristimo JWT)
            .sessionManagement(session -> {
                System.out.println("🔓 Stateless session policy!");
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            })
            
            // Dodavanje JWT Authentication Filter-a
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        System.out.println("✅✅✅ Security filter chain BUILD završen! ✅✅✅");
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("🌍 CORS Configuration Source kreiran!");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
