import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PostService } from '../services/post.service';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-upload',
  templateUrl: './upload.component.html',
  styleUrls: ['./upload.component.css']
})
export class UploadComponent implements OnInit {

  // ============================================
  // FORM DATA
  // ============================================
  title: string = '';
  description: string = '';
  tagsInput: string = ''; // "programiranje, python, tutorial"
  
  // Geolokacija (opciono)
  locationName: string = '';
  latitude: number | null = null;
  longitude: number | null = null;
  
  // Files
  videoFile: File | null = null;
  thumbnailFile: File | null = null;

  // ============================================
  // PREVIEW
  // ============================================
  videoPreviewUrl: string | null = null;
  thumbnailPreviewUrl: string | null = null;

  // ============================================
  // UI STATE
  // ============================================
  uploading: boolean = false;
  uploadProgress: number = 0;
  error: string = '';
  success: boolean = false;

  // ============================================
  // VALIDATION
  // ============================================
  videoError: string = '';
  thumbnailError: string = '';

  constructor(
    private postService: PostService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Provera autentifikacije
    if (!this.authService.isLoggedIn()) {
      alert('Morate biti prijavljeni da biste postavili video!');
      this.router.navigate(['/login']);
    }
  }

  // ============================================
  // FILE SELECTION
  // ============================================

  /**
   * Odabir video fajla (3.3 zahtev: max 200MB, MP4)
   */
  onVideoSelect(event: any): void {
    const file = event.target.files[0];
    this.videoError = '';

    if (!file) {
      this.videoFile = null;
      this.videoPreviewUrl = null;
      return;
    }

    // Validacija tipa (samo MP4 - 3.3 zahtev)
    if (!file.type.startsWith('video/mp4')) {
      this.videoError = 'Samo MP4 format je dozvoljen! (3.3 zahtev)';
      this.videoFile = null;
      return;
    }

    // Validacija veličine (max 200MB - 3.3 zahtev)
    const maxSize = 200 * 1024 * 1024; // 200MB
    if (file.size > maxSize) {
      this.videoError = `Video je prevelik! Maksimum: 200MB. Vaš fajl: ${this.formatFileSize(file.size)}`;
      this.videoFile = null;
      return;
    }

    // Validacija OK
    this.videoFile = file;
    
    // Preview
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.videoPreviewUrl = e.target.result;
    };
    reader.readAsDataURL(file);

    console.log('✅ Video odabran:', file.name, this.formatFileSize(file.size));
  }

  /**
   * Odabir thumbnail slike (3.3 zahtev)
   */
  onThumbnailSelect(event: any): void {
    const file = event.target.files[0];
    this.thumbnailError = '';

    if (!file) {
      this.thumbnailFile = null;
      this.thumbnailPreviewUrl = null;
      return;
    }

    // Validacija tipa (samo slike)
    if (!file.type.startsWith('image/')) {
      this.thumbnailError = 'Samo slike su dozvoljene! (jpg, png, webp)';
      this.thumbnailFile = null;
      return;
    }

    // Validacija veličine (max 5MB)
    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
      this.thumbnailError = `Slika je prevelika! Maksimum: 5MB. Vaš fajl: ${this.formatFileSize(file.size)}`;
      this.thumbnailFile = null;
      return;
    }

    // Validacija OK
    this.thumbnailFile = file;
    
    // Preview
    const reader = new FileReader();
    reader.onload = (e: any) => {
      this.thumbnailPreviewUrl = e.target.result;
    };
    reader.readAsDataURL(file);

    console.log('✅ Thumbnail odabran:', file.name);
  }

  // ============================================
  // GEOLOKACIJA
  // ============================================

  /**
   * Dobija trenutnu lokaciju korisnika (opciono - 3.3)
   */
  getCurrentLocation(): void {
    if (!navigator.geolocation) {
      alert('Vaš browser ne podržava geolokaciju');
      return;
    }

    this.uploading = true; // Pokazuj loading dok dobijamo lokaciju

    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.latitude = position.coords.latitude;
        this.longitude = position.coords.longitude;
        this.locationName = `Lokacija: ${this.latitude.toFixed(4)}, ${this.longitude.toFixed(4)}`;
        this.uploading = false;
        console.log('✅ Lokacija dobijena:', this.latitude, this.longitude);
      },
      (error) => {
        console.error('❌ Greška pri dobijanju lokacije:', error);
        alert('Ne mogu dobiti vašu lokaciju. Možete je uneti ručno.');
        this.uploading = false;
      }
    );
  }

  // ============================================
  // UPLOAD
  // ============================================

  /**
   * Upload video objave (3.3 zahtev - transakciono)
   */
  onSubmit(): void {
    // Reset
    this.error = '';
    this.success = false;

    // Validacija
    if (!this.validateForm()) {
      return;
    }

    // Kreiranje FormData
    const formData = new FormData();
    formData.append('title', this.title.trim());
    
    if (this.description.trim()) {
      formData.append('description', this.description.trim());
    }
    
    if (this.videoFile) {
      formData.append('video', this.videoFile);
    }
    
    if (this.thumbnailFile) {
      formData.append('thumbnail', this.thumbnailFile);
    }

    // Tagovi (3.3 zahtev)
    if (this.tagsInput.trim()) {
      // "programiranje, python, tutorial" → "programiranje,python,tutorial"
      const tags = this.tagsInput.split(',').map(t => t.trim()).filter(t => t).join(',');
      formData.append('tags', tags);
    }

    // Geolokacija (opciono - 3.3)
    if (this.latitude !== null && this.longitude !== null) {
      formData.append('latitude', this.latitude.toString());
      formData.append('longitude', this.longitude.toString());
      
      if (this.locationName.trim()) {
        formData.append('locationName', this.locationName.trim());
      }
    }

    // Upload
    this.uploading = true;
    this.uploadProgress = 0;

    console.log('📤 Započinjem upload...');

    this.postService.createPost(formData).subscribe({
      next: (post) => {
        console.log('✅ Post uspešno kreiran!', post);
        this.uploading = false;
        this.success = true;
        
        // Redirect na video player nakon 2 sekunde
        setTimeout(() => {
          this.router.navigate(['/video', post.id]);
        }, 2000);
      },
      error: (err) => {
        console.error('❌ Greška pri upload-u:', err);
        this.uploading = false;
        this.error = err.error?.message || err.error || 'Upload nije uspeo. Pokušajte ponovo.';
      }
    });
  }

  // ============================================
  // VALIDATION
  // ============================================

  validateForm(): boolean {
    // Naslov (obavezno - 3.3)
    if (!this.title.trim()) {
      this.error = 'Naslov je obavezan!';
      return false;
    }

    if (this.title.length > 200) {
      this.error = 'Naslov može imati maksimum 200 karaktera!';
      return false;
    }

    // Video (obavezno - 3.3)
    if (!this.videoFile) {
      this.error = 'Video fajl je obavezan!';
      return false;
    }

    // Thumbnail (obavezno - 3.3)
    if (!this.thumbnailFile) {
      this.error = 'Thumbnail slika je obavezna!';
      return false;
    }

    return true;
  }

  // ============================================
  // HELPERS
  // ============================================

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  cancel(): void {
    if (confirm('Da li ste sigurni da želite da odustanete?')) {
      this.router.navigate(['/home']);
    }
  }
}
