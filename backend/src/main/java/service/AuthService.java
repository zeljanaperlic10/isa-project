package service;

import dto.LoginRequest;
import dto.LoginResponse;
import dto.UserDTO;
import model.User;
import repository.UserRepository;
import security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginAttemptService loginAttemptService;  // ← DODATO za Rate Limiting

    // Login metoda (sa Rate Limiting)
    public LoginResponse login(LoginRequest request, String ipAddress) {
        
        // KORAK 1: Provera da li je IP adresa blokirana
        if (loginAttemptService.isBlocked(ipAddress)) {
            throw new RuntimeException("Previše pokušaja prijave! Pokušajte ponovo za 1 minut.");
        }
        
        // KORAK 2: Pronađi korisnika po email-u
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        
        if (!userOpt.isPresent()) {
            loginAttemptService.loginFailed(ipAddress); // Beleži neuspešan pokušaj
            throw new RuntimeException("Pogrešan email ili lozinka!");
        }
        
        User user = userOpt.get();
        
        // KORAK 3: Proveri lozinku (BCrypt hash provera)
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(ipAddress); // Beleži neuspešan pokušaj
            throw new RuntimeException("Pogrešan email ili lozinka!");
        }
        
        // KORAK 4: Proveri da li je nalog aktiviran
        if (!user.getActivated()) {
            throw new RuntimeException("Nalog nije aktiviran! Proverite email.");
        }
        
        // KORAK 5: Uspešna prijava - generiši JWT token
        loginAttemptService.loginSucceeded(ipAddress); // Resetuj brojač pokušaja
        String token = jwtUtil.generateToken(user.getUsername());
        
        // KORAK 6: Konvertuj User u UserDTO (bez lozinke)
        UserDTO userDTO = convertToDTO(user);
        
        // KORAK 7: Vrati LoginResponse sa tokenom i user podacima
        return new LoginResponse(token, userDTO);
    }

    // Konverzija User -> UserDTO
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getAddress(),
            user.getActivated(),
            user.getCreatedAt()
        );
    }
}
