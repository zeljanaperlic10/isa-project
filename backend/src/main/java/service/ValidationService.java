package service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    // Regex za validaciju email-a
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // Validacija email formata
    public void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email je obavezan!");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new RuntimeException("Neispravan format email adrese!");
        }
    }

    // Validacija lozinke
    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Lozinka je obavezna!");
        }
        
        if (password.length() < 8) {
            throw new RuntimeException("Lozinka mora imati minimum 8 karaktera!");
        }
        
        // Provera da li ima bar jedno slovo
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new RuntimeException("Lozinka mora sadržati bar jedno slovo!");
        }
        
        // Provera da li ima bar jedan broj
        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Lozinka mora sadržati bar jedan broj!");
        }
    }

    // Validacija username-a
    public void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username je obavezan!");
        }
        
        if (username.length() < 3) {
            throw new RuntimeException("Username mora imati minimum 3 karaktera!");
        }
        
        if (username.length() > 20) {
            throw new RuntimeException("Username može imati maksimum 20 karaktera!");
        }
        
        if (username.contains(" ")) {
            throw new RuntimeException("Username ne može sadržati razmake!");
        }
        
        // Samo slova, brojevi i _ (underscore)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("Username može sadržati samo slova, brojeve i underscore (_)!");
        }
    }

    // Validacija imena i prezimena
    public void validateName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " je obavezno!");
        }
        
        if (name.length() < 2) {
            throw new RuntimeException(fieldName + " mora imati minimum 2 karaktera!");
        }
        
        if (name.length() > 50) {
            throw new RuntimeException(fieldName + " može imati maksimum 50 karaktera!");
        }
    }

    // Validacija adrese
    public void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new RuntimeException("Adresa je obavezna!");
        }
        
        if (address.length() < 5) {
            throw new RuntimeException("Adresa mora imati minimum 5 karaktera!");
        }
        
        if (address.length() > 200) {
            throw new RuntimeException("Adresa može imati maksimum 200 karaktera!");
        }
    }
}
