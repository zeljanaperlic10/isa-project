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

        
        if (loginAttemptService.isBlocked(ipAddress)) {
            throw new RuntimeException("Previše pokušaja prijave! Pokušajte ponovo za 1 minut.");
        }

        
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (!userOpt.isPresent()) {
            loginAttemptService.loginFailed(ipAddress); // Beleži neuspešan pokušaj
            throw new RuntimeException("Pogrešan email ili lozinka!");
        }

        User user = userOpt.get();

        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.loginFailed(ipAddress); // Beleži neuspešan pokušaj
            throw new RuntimeException("Pogrešan email ili lozinka!");
        }

        
        if (!user.getActivated()) {
            throw new RuntimeException("Nalog nije aktiviran! Proverite email.");
        }

        
        loginAttemptService.loginSucceeded(ipAddress); 
        
        
        String token = jwtUtil.generateToken(user.getEmail());

        
        UserDTO userDTO = convertToDTO(user);

        
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
