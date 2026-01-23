package controller;

import dto.RegisterRequest;
import dto.LoginRequest;
import dto.LoginResponse;
import dto.UserDTO;
import service.UserService;
import service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    // POST /auth/register - Registracija novog korisnika
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserDTO user = userService.registerUser(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(user);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    // POST /auth/login - Prijava korisnika (sa Rate Limiting)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // Dobijanje IP adrese korisnika
            String ipAddress = getClientIP(httpRequest);
            
            // Pozivamo AuthService koji proverava rate limiting i validira korisnika
            LoginResponse response = authService.login(request, ipAddress);
            
            // Vraćamo uspešan odgovor sa statusom 200 (OK)
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            // Ako ima greška (npr. pogrešna lozinka, previše pokušaja), vraćamo 401 (Unauthorized)
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        }
    }

    // GET /auth/activate?token=abc-123-xyz - Aktivacija naloga
    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(@RequestParam String token) {
        String message = userService.activateAccount(token);

        if (message.contains("uspešno")) {
            return ResponseEntity.ok(message);
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(message);
        }
    }

    // GET /auth/test - Test endpoint
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend radi! 🚀");
    }

    // Pomoćna metoda za dobijanje IP adrese korisnika
    private String getClientIP(HttpServletRequest request) {
        // Prvo proveravamo X-Forwarded-For header (za proxy/load balancer)
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            // Ako nema header, koristimo direktnu IP adresu
            return request.getRemoteAddr();
        }
        // X-Forwarded-For može imati više IP adresa, uzimamo prvu (originalna)
        return xfHeader.split(",")[0];
    }
}