package service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    // Čuva broj pokušaja i vreme poslednjeg pokušaja za svaku IP adresu
    private final Map<String, LoginAttemptInfo> attemptCache = new ConcurrentHashMap<>();
    
    private static final int MAX_ATTEMPTS = 5; // Maksimalno 5 pokušaja
    private static final int ATTEMPT_WINDOW_MINUTES = 1; // U roku od 1 minuta

    // Metoda koja beleži neuspešan pokušaj prijave
    public void loginFailed(String ipAddress) {
        LoginAttemptInfo info = attemptCache.get(ipAddress);
        
        if (info == null) {
            // Prva prijava sa ove IP adrese
            info = new LoginAttemptInfo();
            info.attempts = 1;
            info.firstAttemptTime = LocalDateTime.now();
            attemptCache.put(ipAddress, info);
        } else {
            // Provera da li je prošlo više od 1 minuta od prvog pokušaja
            if (info.firstAttemptTime.plusMinutes(ATTEMPT_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
                // Prošlo je više od 1 minut - resetuj brojač
                info.attempts = 1;
                info.firstAttemptTime = LocalDateTime.now();
            } else {
                // Još uvek u okviru 1 minuta - povećaj brojač
                info.attempts++;
            }
        }
        
        System.out.println("⚠️ Neuspešna prijava sa IP: " + ipAddress + " (Pokušaj " + info.attempts + "/" + MAX_ATTEMPTS + ")");
    }

    // Metoda koja beleži uspešan pokušaj prijave
    public void loginSucceeded(String ipAddress) {
        // Resetuj brojač pokušaja za ovu IP adresu
        attemptCache.remove(ipAddress);
        System.out.println("✅ Uspešna prijava sa IP: " + ipAddress);
    }

    // Provera da li je IP adresa blokirana
    public boolean isBlocked(String ipAddress) {
        LoginAttemptInfo info = attemptCache.get(ipAddress);
        
        if (info == null) {
            return false; // Nema pokušaja - nije blokiran
        }
        
        // Provera da li je prošao vremenski prozor
        if (info.firstAttemptTime.plusMinutes(ATTEMPT_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            // Prošao je vremenski prozor - resetuj i nije blokiran
            attemptCache.remove(ipAddress);
            return false;
        }
        
        // Provera da li je prekoračen broj pokušaja
        if (info.attempts >= MAX_ATTEMPTS) {
            System.out.println("🚫 IP adresa blokirana: " + ipAddress + " (Previše pokušaja!)");
            return true;
        }
        
        return false;
    }

    // Pomoćna klasa koja čuva informacije o pokušajima prijave
    private static class LoginAttemptInfo {
        int attempts;
        LocalDateTime firstAttemptTime;
    }
}
