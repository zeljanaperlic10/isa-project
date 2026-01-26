package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Servis za čuvanje i učitavanje video i thumbnail fajlova.
 * 
 * Thumbnail slike se keširaju u memoriji (3.3 zahtev):
 * - Prvi put: čita sa file sistema
 * - Sledeći put: vraća iz cache-a (brže!)
 * - @Cacheable("thumbnails") anotacija
 */
@Service
public class FileStorageService {

    // Lokacija za čuvanje video fajlova
    private final Path videoStorageLocation;
    
    // Lokacija za čuvanje thumbnail slika
    private final Path thumbnailStorageLocation;

    // ============================================
    // KONSTRUKTOR - Kreira foldere ako ne postoje
    // ============================================
    
    public FileStorageService(
            @Value("${file.upload-dir:uploads/videos}") String videoUploadDir,
            @Value("${file.thumbnail-dir:uploads/thumbnails}") String thumbnailUploadDir) {
        
        // KORAK 1: Postavljanje putanja
        this.videoStorageLocation = Paths.get(videoUploadDir).toAbsolutePath().normalize();
        this.thumbnailStorageLocation = Paths.get(thumbnailUploadDir).toAbsolutePath().normalize();

        // KORAK 2: Kreiranje foldera
        try {
            Files.createDirectories(this.videoStorageLocation);
            Files.createDirectories(this.thumbnailStorageLocation);
            System.out.println("✅ Video folder: " + this.videoStorageLocation);
            System.out.println("✅ Thumbnail folder: " + this.thumbnailStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Greška pri kreiranju upload foldera!", ex);
        }
    }

    // ============================================
    // ČUVANJE VIDEO FAJLA (max 200MB - 3.3 zahtev)
    // ============================================
    
    public String storeVideoFile(MultipartFile file) {
        // KORAK 1: Validacija
        validateVideoFile(file);

        // KORAK 2: Generisanje jedinstvenog imena
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // KORAK 3: Security provera
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Neispravno ime fajla: " + originalFileName);
            }

            // KORAK 4: Čuvanje fajla
            Path targetLocation = this.videoStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ Video sačuvan: " + newFileName + " (" + formatFileSize(file.getSize()) + ")");
            
            return newFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Greška pri čuvanju video fajla: " + originalFileName, ex);
        }
    }

    // ============================================
    // ČUVANJE THUMBNAIL SLIKE (3.3 zahtev)
    // ============================================
    
    public String storeThumbnailFile(MultipartFile file) {
        // KORAK 1: Validacija
        validateImageFile(file);

        // KORAK 2: Generisanje imena
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // KORAK 3: Security provera
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Neispravno ime fajla: " + originalFileName);
            }

            // KORAK 4: Čuvanje thumbnail-a
            Path targetLocation = this.thumbnailStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ Thumbnail sačuvan: " + newFileName);
            
            return newFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Greška pri čuvanju thumbnail-a: " + originalFileName, ex);
        }
    }

    // ============================================
    // UČITAVANJE VIDEO FAJLA (za streaming)
    // ============================================
    
    public Resource loadVideoAsResource(String fileName) {
        try {
            Path filePath = this.videoStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Video fajl nije pronađen: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Video fajl nije pronađen: " + fileName, ex);
        }
    }

    // ============================================
    // UČITAVANJE THUMBNAIL SLIKE (sa kešom - 3.3 zahtev)
    // ============================================
    
    /**
     * Učitava thumbnail sliku sa keširanjem.
     * 
     * KAKO RADI CACHING:
     * 1. Prvi zahtev: GET /api/thumbnails/abc-123.jpg
     *    → Čita fajl sa diska
     *    → Vraća Resource
     *    → Čuva u cache pod ključem "abc-123.jpg"
     * 
     * 2. Drugi zahtev: GET /api/thumbnails/abc-123.jpg
     *    → NE čita sa diska
     *    → Vraća iz cache-a (BRŽE!)
     * 
     * 3. Cache se resetuje kada se aplikacija restartuje
     *    (za trajni cache: koristiti Redis/Ehcache)
     */
    @org.springframework.cache.annotation.Cacheable(value = "thumbnails", key = "#fileName")
    public Resource loadThumbnailAsResource(String fileName) {
        try {
            Path filePath = this.thumbnailStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Thumbnail nije pronađen: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Thumbnail nije pronađen: " + fileName, ex);
        }
    }

    // ============================================
    // BRISANJE FAJLOVA
    // ============================================
    
    public void deleteVideoFile(String fileName) {
        try {
            Path filePath = this.videoStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("🗑️ Video obrisan: " + fileName);
        } catch (IOException ex) {
            System.err.println("⚠️ Greška pri brisanju videa: " + fileName);
        }
    }

    // ============================================
    // BRISANJE THUMBNAIL FAJLA (sa čišćenjem cache-a)
    // ============================================
    
    @org.springframework.cache.annotation.CacheEvict(value = "thumbnails", key = "#fileName")
    public void deleteThumbnailFile(String fileName) {
        try {
            Path filePath = this.thumbnailStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("🗑️ Thumbnail obrisan: " + fileName);
        } catch (IOException ex) {
            System.err.println("⚠️ Greška pri brisanju thumbnail-a: " + fileName);
        }
    }

    // ============================================
    // VALIDACIJE (3.3 zahtevi)
    // ============================================

    private void validateVideoFile(MultipartFile file) {
        // KORAK 1: Provera da li je fajl prazan
        if (file.isEmpty()) {
            throw new RuntimeException("Video fajl je prazan!");
        }

        // KORAK 2: Provera tipa fajla (samo video formati)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new RuntimeException("Samo video fajlovi su dozvoljeni! (mp4, webm, avi) - Tip: " + contentType);
        }

        // KORAK 3: Provera ekstenzije (mora biti .mp4 - 3.3 zahtev)
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".mp4")) {
            throw new RuntimeException("Video mora biti u MP4 formatu! (3.3 zahtev)");
        }

        // KORAK 4: Provera veličine (MAX 200MB - 3.3 zahtev)
        long maxSize = 200 * 1024 * 1024; // 200 MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("Video je prevelik! Maksimum: 200MB (3.3 zahtev) - Vaš fajl: " + formatFileSize(file.getSize()));
        }

        System.out.println("✅ Video validiran: " + file.getOriginalFilename() + " (" + formatFileSize(file.getSize()) + ")");
    }

    private void validateImageFile(MultipartFile file) {
        // Provera da li je fajl prazan
        if (file.isEmpty()) {
            throw new RuntimeException("Thumbnail slika je prazna!");
        }

        // Provera tipa (samo slike)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Samo slike su dozvoljene za thumbnail! (jpg, png, webp) - Tip: " + contentType);
        }

        // Provera veličine (max 5MB za thumbnail)
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new RuntimeException("Thumbnail je prevelik! Maksimum: 5MB - Vaš fajl: " + formatFileSize(file.getSize()));
        }

        System.out.println("✅ Thumbnail validiran: " + file.getOriginalFilename() + " (" + formatFileSize(file.getSize()) + ")");
    }

    // ============================================
    // POMOĆNE METODE
    // ============================================

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}