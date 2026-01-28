package security;

import model.User;
import repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom UserDetailsService - učitava korisnika iz baze za Spring Security
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Pronalazi korisnika po email-u
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik nije pronađen: " + email));

        // Kreiranje liste authorities (roles)
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Vraćanje Spring Security UserDetails objekta
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(user.getEnabled() == null || !user.getEnabled())  // FIX: getEnabled() umesto isEnabled()
                .build();
    }
}
