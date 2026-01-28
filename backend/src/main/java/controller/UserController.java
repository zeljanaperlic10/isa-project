package controller;

import dto.UserDTO;
import model.User;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/users/{username} - Dobija osnovne podatke o korisniku
     * Javno dostupno (3.1 zahtev - za profil stranicu)
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        System.out.println("👤 GET /api/users/" + username);

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (!userOpt.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Korisnik nije pronađen: " + username);
        }

        User user = userOpt.get();

        // Konvertuj u DTO (bez lozinke i ostalih osetljivih podataka)
        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAddress(),
                user.getActivated(),
                user.getCreatedAt()
        );

        return ResponseEntity.ok(userDTO);
    }

    /**
     * GET /api/users/test - Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("👤 User API radi!");
    }
}
