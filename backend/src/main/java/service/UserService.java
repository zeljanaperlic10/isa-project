package service;

import dto.RegisterRequest;
import dto.UserDTO;
import model.ActivationToken;
import model.User;
import repository.ActivationTokenRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ValidationService validationService;  // ← DODATO

    // Registracija novog korisnika
    public UserDTO registerUser(RegisterRequest request) {
        
        // VALIDACIJE PODATAKA (format, dužina, itd.)
        validationService.validateEmail(request.getEmail());
        validationService.validateUsername(request.getUsername());
        validationService.validatePassword(request.getPassword());
        validationService.validateName(request.getFirstName(), "Ime");
        validationService.validateName(request.getLastName(), "Prezime");
        validationService.validateAddress(request.getAddress());
        
        // KORAK 1: Validacija - provera da li email već postoji
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email već postoji!");
        }
        
        // KORAK 2: Validacija - provera da li username već postoji
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username već postoji!");
        }
        
        // KORAK 3: Validacija - provera da li se lozinke poklapaju
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Lozinke se ne poklapaju!");
        }
        
        // KORAK 4: Kreiranje novog korisnika
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setAddress(request.getAddress());
        user.setActivated(false);
        user.setEnabled(true);
        
        // KORAK 5: Čuvanje korisnika u bazi
        User savedUser = userRepository.save(user);
        
        // KORAK 6: Kreiranje activation tokena
        ActivationToken token = new ActivationToken(savedUser);
        tokenRepository.save(token);
        
        // KORAK 7: Slanje email-a za aktivaciju (sa try-catch)
        try {
            emailService.sendActivationEmail(
                savedUser.getEmail(), 
                savedUser.getUsername(), 
                token.getToken()
            );
            System.out.println("✅ Email poslat na: " + savedUser.getEmail());
        } catch (Exception e) {
            System.out.println("⚠️ Email nije mogao biti poslat: " + e.getMessage());
            System.out.println("📧 Token za aktivaciju: " + token.getToken());
            System.out.println("🔗 Link: http://localhost:9090/auth/activate?token=" + token.getToken());
        }
        
        // KORAK 8: Vraćanje DTO objekta (bez lozinke)
        return convertToDTO(savedUser);
    }

    // Aktivacija naloga preko tokena
    public String activateAccount(String tokenString) {
        
        // KORAK 1: Pronalaženje tokena u bazi
        Optional<ActivationToken> tokenOpt = tokenRepository.findByToken(tokenString);
        
        if (!tokenOpt.isPresent()) {
            return "Token nije pronađen!";
        }
        
        ActivationToken token = tokenOpt.get();
        
        // KORAK 2: Provera da li je token već iskorišćen
        if (token.isActivated()) {
            return "Nalog je već aktiviran!";
        }
        
        // KORAK 3: Provera da li je token istekao (24h)
        if (token.isExpired()) {
            return "Token je istekao! Molimo registrujte se ponovo.";
        }
        
        // KORAK 4: Aktivacija korisnika
        User user = token.getUser();
        user.setActivated(true);
        userRepository.save(user);
        
        // KORAK 5: Označavanje tokena kao iskorišćenog
        token.setActivatedAt(LocalDateTime.now());
        tokenRepository.save(token);
        
        return "Nalog je uspešno aktiviran! Možete se prijaviti.";
    }

    // Pronalaženje korisnika po ID-u
    public UserDTO getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return convertToDTO(user.get());
        }
        throw new RuntimeException("Korisnik nije pronađen!");
    }

    // Pronalaženje korisnika po username-u
    public UserDTO getUserByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return convertToDTO(user.get());
        }
        throw new RuntimeException("Korisnik nije pronađen!");
    }

    // Svi korisnici
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Konverzija User -> UserDTO (bez lozinke!)
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
