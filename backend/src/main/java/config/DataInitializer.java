package config;

import model.Post;
import model.Tag;
import model.User;
import repository.PostRepository;
import repository.TagRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Automatsko kreiranje test podataka pri pokretanju aplikacije.
 * Kreira test korisnike i postove sa YouTube video linkovima.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        
        // Provera da li već postoje podaci
        if (userRepository.count() > 0) {
            System.out.println("ℹ️  Podaci već postoje u bazi, preskačem inicijalizaciju.");
            return;
        }

        System.out.println("🔄 Kreiranje test podataka...");
        System.out.println("================================================");

        // ============================================
        // KREIRANJE TEST KORISNIKA
        // ============================================

        User petar = createTestUser(
            "petar123", 
            "petar@example.com", 
            "password123", 
            "Petar", 
            "Petrović", 
            "Beograd, Srbija"
        );

        User ana = createTestUser(
            "ana_m", 
            "ana@example.com", 
            "password123", 
            "Ana", 
            "Marković", 
            "Novi Sad, Srbija"
        );

        User stefan = createTestUser(
            "stefan", 
            "stefan@example.com", 
            "password123", 
            "Stefan", 
            "Jovanović", 
            "Niš, Srbija"
        );

        User marko = createTestUser(
            "marko_dev", 
            "marko@example.com", 
            "password123", 
            "Marko", 
            "Đorđević", 
            "Kragujevac, Srbija"
        );

        System.out.println("✅ Kreirano 4 test korisnika");

        // ============================================
        // KREIRANJE TAGOVA
        // ============================================

        Tag tagProgramiranje = createTag("programiranje");
        Tag tagPython = createTag("python");
        Tag tagJava = createTag("java");
        Tag tagTutorial = createTag("tutorial");
        Tag tagPriroda = createTag("priroda");
        Tag tagPutovanja = createTag("putovanja");
        Tag tagMuzika = createTag("muzika");
        Tag tagSpring = createTag("spring");
        Tag tagAngular = createTag("angular");
        Tag tagKuhinja = createTag("kuhinja");

        System.out.println("✅ Kreirano 10 tagova");

        // ============================================
        // KREIRANJE TEST POSTOVA (sa YouTube linkovima)
        // ============================================

        // Post 1 - Python Tutorial
        createTestPost(
            petar,
            "Uvod u programiranje - Python za početnike",
            "Naučite osnovne koncepte programiranja kroz Python. Idealno za početnike koji žele da savladaju osnove!",
            "https://www.youtube.com/watch?v=rfscVS0vtbw",
            "https://img.youtube.com/vi/rfscVS0vtbw/maxresdefault.jpg",
            Set.of(tagProgramiranje, tagPython, tagTutorial),
            44.8176, 20.4569, "Beograd, Srbija"
        );

        // Post 2 - Priroda Srbije
        createTestPost(
            ana,
            "Najlepši pejzaži Srbije - Priroda i planine",
            "Uživajte u najlepšim delovima srpske prirode. Putovanje kroz planine, reke i nacionalne parkove.",
            "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
            "https://img.youtube.com/vi/kJQP7kiw5Fk/maxresdefault.jpg",
            Set.of(tagPriroda, tagPutovanja),
            43.3209, 21.8958, "Niš, Srbija"
        );

        // Post 3 - Spring Boot Tutorial
        createTestPost(
            stefan,
            "Spring Boot Tutorial - Full Course",
            "Kompletna obuka za Spring Boot framework. Od nule do profesionalnog projekta sa bazom podataka.",
            "https://www.youtube.com/watch?v=9SGDpanrc8U",
            "https://img.youtube.com/vi/9SGDpanrc8U/maxresdefault.jpg",
            Set.of(tagProgramiranje, tagJava, tagSpring, tagTutorial),
            null, null, null // Bez geolokacije
        );

        // Post 4 - Angular Tutorial
        createTestPost(
            petar,
            "Angular za početnike - Kreiranje prve aplikacije",
            "Naučite kako da kreirate modernu web aplikaciju koristeći Angular framework. Step-by-step vodič.",
            "https://www.youtube.com/watch?v=3qBXWUpoPHo",
            "https://img.youtube.com/vi/3qBXWUpoPHo/maxresdefault.jpg",
            Set.of(tagProgramiranje, tagAngular, tagTutorial),
            44.8176, 20.4569, "Beograd, Srbija"
        );

        // Post 5 - Recepti
        createTestPost(
            ana,
            "Recepti za zdrave obroke - Brza kuhinja",
            "Ukusni i zdravi recepti koje možete pripremiti za manje od 30 minuta. Idealno za užurbane dane!",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
            Set.of(tagKuhinja),
            45.2671, 19.8335, "Novi Sad, Srbija"
        );

        // Post 6 - JavaScript ES6
        createTestPost(
            marko,
            "JavaScript ES6+ - Moderne features",
            "Savladajte nove mogućnosti JavaScripta - arrow functions, promises, async/await i još mnogo toga.",
            "https://www.youtube.com/watch?v=NCwa_xi0Uuc",
            "https://img.youtube.com/vi/NCwa_xi0Uuc/maxresdefault.jpg",
            Set.of(tagProgramiranje, tagTutorial),
            44.0165, 21.0059, "Kragujevac, Srbija"
        );

        // Post 7 - Java Backend
        createTestPost(
            stefan,
            "Java Backend Development - REST API",
            "Kompletan vodič za kreiranje REST API-ja u Javi. Spring Boot, Hibernate, PostgreSQL.",
            "https://www.youtube.com/watch?v=vtPkZShrvXQ",
            "https://img.youtube.com/vi/vtPkZShrvXQ/maxresdefault.jpg",
            Set.of(tagProgramiranje, tagJava, tagSpring),
            null, null, null
        );

        // Post 8 - Muzika
        createTestPost(
            ana,
            "Opuštajuća muzika za rad i učenje",
            "2 sata relaksirajuće muzike. Idealno za fokus, koncentraciju i produktivnost.",
            "https://www.youtube.com/watch?v=jfKfPfyJRdk",
            "https://img.youtube.com/vi/jfKfPfyJRdk/maxresdefault.jpg",
            Set.of(tagMuzika),
            45.2671, 19.8335, "Novi Sad, Srbija"
        );

        System.out.println("✅ Kreirano 8 test postova");
        System.out.println("================================================");
        System.out.println("🎉 Test podaci uspešno kreirani!");
        System.out.println("");
        System.out.println("📝 TEST CREDENTIALS:");
        System.out.println("   Email: petar@example.com  | Password: password123");
        System.out.println("   Email: ana@example.com    | Password: password123");
        System.out.println("   Email: stefan@example.com | Password: password123");
        System.out.println("   Email: marko@example.com  | Password: password123");
        System.out.println("");
        System.out.println("🎬 Svi postovi koriste YouTube video linkove!");
        System.out.println("================================================");
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    private User createTestUser(String username, String email, String password, 
                                String firstName, String lastName, String address) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAddress(address);
        user.setActivated(true);  // Odmah aktiviran
        user.setEnabled(true);
        
        return userRepository.save(user);
    }

    private Tag createTag(String name) {
        Tag tag = new Tag(name);
        return tagRepository.save(tag);
    }

    private Post createTestPost(User user, String title, String description, 
                                String videoUrl, String thumbnailUrl,
                                Set<Tag> tags,
                                Double latitude, Double longitude, String locationName) {
        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setDescription(description);
        post.setVideoUrl(videoUrl);
        post.setThumbnailUrl(thumbnailUrl);
        post.setVideoFileName("youtube-video"); // Dummy - nije lokalni fajl
        post.setFileSize(0L);
        post.setDuration(null);
        
        // Tagovi
        post.setTags(tags);
        
        // Geolokacija (opciono)
        post.setLatitude(latitude);
        post.setLongitude(longitude);
        post.setLocationName(locationName);
        
        // Random statistika
        post.setLikesCount((int)(Math.random() * 150));
        post.setCommentsCount((int)(Math.random() * 80));
        post.setViewsCount((int)(Math.random() * 1000));
        
        Post savedPost = postRepository.save(post);
        
        // Ažuriraj brojače tagova
        for (Tag tag : tags) {
            tag.setPostCount(tag.getPostCount() + 1);
            tagRepository.save(tag);
        }
        
        return savedPost;
    }
}
