package service;

import model.Post;
import model.User;
import model.WatchParty;
import repository.PostRepository;
import repository.UserRepository;
import repository.WatchPartyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class WatchPartyService {

    @Autowired
    private WatchPartyRepository watchPartyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    // ============================================
    // KREIRANJE SOBE
    // ============================================

   
    @Transactional
    public WatchParty createRoom(String username, String roomName) {
        System.out.println("🎬 Kreiranje Watch Party sobe...");
        System.out.println("   Kreator: " + username);
        System.out.println("   Naziv: " + roomName);

        // VALIDACIJA 1: Prazan naziv
        if (roomName == null || roomName.trim().isEmpty()) {
            throw new RuntimeException("Naziv sobe ne sme biti prazan!");
        }

        // VALIDACIJA 2: Dužina naziva
        if (roomName.length() > 200) {
            throw new RuntimeException("Naziv sobe može imati maksimum 200 karaktera!");
        }

        // VALIDACIJA 3: Korisnik postoji?
     // Pronađi korisnika po email-u ili username-u
        Optional<User> userOpt = userRepository.findByUsername(username);

        // Ako nije pronađen po username-u, pokušaj po email-u
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(username);
        }

        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen: " + username);
        }
        User creator = userOpt.get();

        // OPCIONO: Proveri da li korisnik već ima aktivnu sobu
        // boolean hasActive = watchPartyRepository.existsByCreatorUsernameAndActive(username, true);
        // if (hasActive) {
        //     throw new RuntimeException("Već imaš aktivnu sobu! Zatvori staru pre nego što kreiraš novu.");
        // }

        // Kreiraj sobu
        WatchParty party = new WatchParty(roomName, creator);

        // Sačuvaj u bazi
        WatchParty savedParty = watchPartyRepository.save(party);

        System.out.println("✅ Soba kreirana! ID: " + savedParty.getId());
        System.out.println("   Kreator: " + savedParty.getCreator().getUsername());
        System.out.println("   Članovi: " + savedParty.getMemberCount());

        return savedParty;
    }

    // ============================================
    // DOBIJANJE SOBA
    // ============================================

    
    public List<WatchParty> getActiveRooms() {
        System.out.println("📋 Učitavanje aktivnih soba...");
        
        List<WatchParty> rooms = watchPartyRepository.findByActiveOrderByCreatedAtDesc(true);
        
        System.out.println("✅ Učitano " + rooms.size() + " aktivnih soba");
        
        return rooms;
    }

   
    public List<WatchParty> getRoomsByCreator(String usernameOrEmail) {
        System.out.println("📋 Učitavanje soba korisnika: " + usernameOrEmail);
        
        // Pokušaj prvo po username-u
        List<WatchParty> rooms = watchPartyRepository.findByCreatorUsernameOrderByCreatedAtDesc(usernameOrEmail);
        
        // Ako nije pronađeno, pokušaj po email-u
        if (rooms.isEmpty()) {
            System.out.println("   Username nije pronađen, probavam email...");
            rooms = watchPartyRepository.findByCreatorEmailOrderByCreatedAtDesc(usernameOrEmail);
        }
        
        System.out.println("✅ Korisnik ima " + rooms.size() + " soba");
        
        return rooms;
    }
    
    public List<WatchParty> getRoomsWhereUserIsMember(String username) {
        System.out.println("📋 Učitavanje soba gde je " + username + " član...");
        
        List<WatchParty> rooms = watchPartyRepository.findPartiesByMember(username);
        
        System.out.println("✅ Korisnik je član u " + rooms.size() + " soba");
        
        return rooms;
    }

   
    public WatchParty getRoomById(Long roomId) {
        System.out.println("🔍 Učitavanje sobe ID: " + roomId);
        
        Optional<WatchParty> partyOpt = watchPartyRepository.findById(roomId);
        
        if (!partyOpt.isPresent()) {
            throw new RuntimeException("Soba nije pronađena! ID: " + roomId);
        }
        
        WatchParty party = partyOpt.get();
        
        System.out.println("✅ Soba učitana: " + party.getName());
        System.out.println("   Članovi: " + party.getMemberCount());
        
        return party;
    }

    // ============================================
    // PRIDRUŽIVANJE SOBI
    // ============================================

   
    @Transactional
    public WatchParty joinRoom(Long roomId, String usernameOrEmail) {
        System.out.println("➕ Pridruživanje sobi...");
        System.out.println("   Soba ID: " + roomId);
        System.out.println("   Korisnik: " + usernameOrEmail);

        // Pronađi sobu
        WatchParty party = getRoomById(roomId);

        // Proveri da li je soba aktivna
        if (!party.getActive()) {
            throw new RuntimeException("Soba nije aktivna!");
        }

        // PRONAĐI KORISNIKA da bi dobio pravi username
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen!");
        }
        
        User user = userOpt.get();
        String actualUsername = user.getUsername();  // ✅ PRAVI USERNAME!

        // Proveri da li korisnik već nije član
        if (party.isMember(actualUsername)) {
            System.out.println("⚠️ Korisnik je već član sobe!");
            return party;
        }
        
        
        if (party.isCreator(actualUsername)) {
            System.out.println("⚠️ Korisnik je kreator, već je član!");
            return party;
        }

        // Dodaj korisnika sa pravim username-om
        party.addMember(actualUsername);  // ✅ Dodaje USERNAME!

        // Sačuvaj
        WatchParty savedParty = watchPartyRepository.save(party);

        System.out.println("✅ Korisnik pridružen sobi!");
        System.out.println("   Ukupno članova: " + savedParty.getMemberCount());

        return savedParty;
    }

    // ============================================
    // NAPUŠTANJE SOBE
    // ============================================

   
    @Transactional
    public WatchParty leaveRoom(Long roomId, String usernameOrEmail) {
        System.out.println("➖ Napuštanje sobe...");
        System.out.println("   Soba ID: " + roomId);
        System.out.println("   Korisnik: " + usernameOrEmail);

        // Pronađi sobu
        WatchParty party = getRoomById(roomId);

        // PRONAĐI KORISNIKA da bi dobio pravi username
        Optional<User> userOpt = userRepository.findByUsername(usernameOrEmail);
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByEmail(usernameOrEmail);
        }
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Korisnik nije pronađen!");
        }
        
        User user = userOpt.get();
        String actualUsername = user.getUsername();

        // Ukloni korisnika
        party.removeMember(actualUsername);

        // Ako je kreator napustio → zatvori sobu
        if (party.isCreator(actualUsername)) {
            System.out.println("⚠️ Kreator je napustio sobu → zatvaranje sobe!");
            party.setActive(false);
        }

        // Sačuvaj
        WatchParty savedParty = watchPartyRepository.save(party);

        System.out.println("✅ Korisnik napustio sobu!");
        System.out.println("   Preostalo članova: " + savedParty.getMemberCount());

        return savedParty;
    }

    // ============================================
    // POKRETANJE VIDEA
    // ============================================

   
    @Transactional
    public WatchParty startVideo(Long roomId, Long postId, String username) {
        System.out.println("▶️ Pokretanje videa u sobi...");
        System.out.println("   Soba ID: " + roomId);
        System.out.println("   Video ID: " + postId);
        System.out.println("   Korisnik: " + username);

        // Pronađi sobu
        WatchParty party = getRoomById(roomId);

        // VALIDACIJA: Samo kreator može pokrenuti video
        if (!party.isCreator(username)) {
            throw new RuntimeException("Samo kreator sobe može pokrenuti video!");
        }

        // Pronađi video
        Optional<Post> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            throw new RuntimeException("Video nije pronađen! ID: " + postId);
        }
        Post post = postOpt.get();

        // Postavi trenutni video
        party.setCurrentPost(post);

        // Sačuvaj
        WatchParty savedParty = watchPartyRepository.save(party);

        System.out.println("✅ Video pokrenut!");
        System.out.println("   Video: " + post.getTitle());
        System.out.println("   Broadcast će se poslati svim članovima...");

        return savedParty;
    }

    // ============================================
    // ZATVARANJE SOBE
    // ============================================

    
    @Transactional
    public WatchParty closeRoom(Long roomId, String username) {
        System.out.println("🚫 Zatvaranje sobe...");
        System.out.println("   Soba ID: " + roomId);
        System.out.println("   Korisnik: " + username);

        // Pronađi sobu
        WatchParty party = getRoomById(roomId);

        // VALIDACIJA: Samo kreator može zatvoriti sobu
        if (!party.isCreator(username)) {
            throw new RuntimeException("Samo kreator sobe može je zatvoriti!");
        }

        // Zatvori sobu
        party.setActive(false);

        // Sačuvaj
        WatchParty savedParty = watchPartyRepository.save(party);

        System.out.println("✅ Soba zatvorena!");

        return savedParty;
    }
}
