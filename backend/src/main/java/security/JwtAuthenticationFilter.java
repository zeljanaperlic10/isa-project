package security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Izvuci JWT token iz Authorization header-a
            String jwt = getJwtFromRequest(request);

            if (jwt != null && validateToken(jwt)) {
                // 2. Izvuci email (subject) iz tokena
                String email = getEmailFromJwt(jwt);

                // 3. Učitaj korisnika iz baze
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 4. Kreiraj Authentication objekat
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 5. Postavi Authentication u SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                System.out.println("✅ JWT Token validiran za korisnika: " + email);
            }

        } catch (Exception e) {
            System.err.println("❌ Greška pri validaciji JWT tokena: " + e.getMessage());
            e.printStackTrace();
        }

        // Nastavi sa filter chain-om
        filterChain.doFilter(request, response);
    }

    /**
     * Izvlači JWT token iz Authorization header-a
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Ukloni "Bearer " prefiks
        }

        return null;
    }

   
    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            // JJWT 0.12.3 API:
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.err.println("❌ JWT token nije validan: " + e.getMessage());
            return false;
        }
    }

    
    private String getEmailFromJwt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        // JJWT 0.12.3 API:
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }
}