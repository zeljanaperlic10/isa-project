package security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public SecurityConfig() {
        System.out.println("🔥🔥🔥 SECURITY CONFIG SE UCITAVA! 🔥🔥🔥");
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
            
            // Autorizacija zahteva
            .authorizeHttpRequests(auth -> {
                System.out.println("📋 Konfigurisem authorization rules...");
                System.out.println("✅ Javni endpoint-i: /auth/register, /auth/login, /auth/activate, /auth/test");
                auth
                    .requestMatchers("/auth/register", "/auth/login", "/auth/activate", "/auth/test").permitAll()
                    .anyRequest().authenticated();
            })
            
            // Stateless sesija (koristimo JWT)
            .sessionManagement(session -> {
                System.out.println("🔓 Stateless session policy!");
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            });
        
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
