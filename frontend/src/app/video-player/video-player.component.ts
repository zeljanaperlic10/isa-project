import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PostService } from '../services/post.service';
import { AuthService } from '../auth/auth.service';
import { Post } from '../models/post.model';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.component.html',
  styleUrls: ['./video-player.component.css']
})
export class VideoPlayerComponent implements OnInit {

  // ============================================
  // POST DATA
  // ============================================
  post: Post | null = null;
  loading: boolean = true;
  error: string = '';

  // ============================================
  // AUTHENTICATION
  // ============================================
  isLoggedIn: boolean = false;

  // ============================================
  // VIDEO
  // ============================================
  isYouTube: boolean = false;
  videoEmbedUrl: SafeResourceUrl | null = null;
  localVideoUrl: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private postService: PostService,
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    // Provera autentifikacije
    this.isLoggedIn = this.authService.isLoggedIn();

    // Dobijanje ID-a iz URL-a
    const postId = this.route.snapshot.paramMap.get('id');
    
    if (postId) {
      this.loadPost(Number(postId));
    } else {
      this.error = 'Neispravan ID posta';
      this.loading = false;
    }
  }

  // ============================================
  // UČITAVANJE POSTA
  // ============================================

  loadPost(id: number): void {
    this.loading = true;
    this.error = '';

    this.postService.getPostById(id).subscribe({
      next: (post) => {
        console.log('✅ Post učitan:', post);
        this.post = post;
        this.processVideoUrl(post.videoUrl);
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju posta:', err);
        this.error = 'Post nije pronađen';
        this.loading = false;
      }
    });
  }

  // ============================================
  // PROCESIRANJE VIDEO URL-a
  // ============================================

  processVideoUrl(url: string): void {
    // Provera da li je YouTube video
    if (url.includes('youtube.com') || url.includes('youtu.be')) {
      this.isYouTube = true;
      this.videoEmbedUrl = this.getYouTubeEmbedUrl(url);
    } else {
      // Lokalni video
      this.isYouTube = false;
      this.localVideoUrl = url.startsWith('http') ? url : `http://localhost:9090${url}`;
    }
  }

  /**
   * Konvertuje YouTube URL u embed format
   * https://www.youtube.com/watch?v=VIDEO_ID → https://www.youtube.com/embed/VIDEO_ID
   */
  getYouTubeEmbedUrl(url: string): SafeResourceUrl {
    let videoId = '';

    // Format 1: https://www.youtube.com/watch?v=VIDEO_ID
    if (url.includes('watch?v=')) {
      videoId = url.split('watch?v=')[1].split('&')[0];
    }
    // Format 2: https://youtu.be/VIDEO_ID
    else if (url.includes('youtu.be/')) {
      videoId = url.split('youtu.be/')[1].split('?')[0];
    }

    const embedUrl = `https://www.youtube.com/embed/${videoId}`;
    
    // Sanitize URL za bezbednost
    return this.sanitizer.bypassSecurityTrustResourceUrl(embedUrl);
  }

  // ============================================
  // USER ACTIONS (lajk, komentar)
  // ============================================

  onLike(): void {
    if (!this.isLoggedIn) {
      alert('Morate se prijaviti da biste lajkovali objavu! 😊');
      return;
    }
    
    // TODO: Implementirati lajk funkcionalnost
    console.log('❤️ Like!');
  }

  onComment(): void {
    if (!this.isLoggedIn) {
      alert('Morate se prijaviti da biste komentarisali! 💬');
      return;
    }

    // TODO: Implementirati komentar funkcionalnost
    console.log('💬 Comment!');
  }

  // ============================================
  // NAVIGACIJA
  // ============================================

  goToUserProfile(username: string): void {
    this.router.navigate(['/profile', username]);
  }

  goBack(): void {
    this.router.navigate(['/home']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}