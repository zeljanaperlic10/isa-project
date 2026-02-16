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

  
  refreshPost(): void {
    if (!this.post) {
      return;
    }

    console.log('🔄 Osvježavam post...');

    // NOVO - koristi refresh endpoint (bez view increment)
    this.postService.refreshPost(this.post.id).subscribe({
      next: (post) => {
        console.log('✅ Post osvježen - commentsCount:', post.commentsCount);
        
        // Ažuriraj samo brojače (ne ceo post da ne bi resetovao video)
        if (this.post) {
          this.post.commentsCount = post.commentsCount;
          this.post.likesCount = post.likesCount;
          this.post.viewsCount = post.viewsCount;
        }
      },
      error: (err) => {
        console.error('❌ Greška pri osvježavanju:', err);
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
  // LAJKOVANJE ❤️
  // ============================================

  /**
   * Toggle like/unlike za video (optimistički update)
   */
  toggleLike(): void {
    // Provera da li je korisnik prijavljen
    if (!this.isLoggedIn) {
      alert('Morate biti prijavljeni da biste lajkovali video!');
      this.router.navigate(['/login']);
      return;
    }

    if (!this.post) {
      return;
    }

    // Optimistički UI update (odmah prikaži promenu)
    const wasLiked = this.post.isLikedByCurrentUser;
    this.post.isLikedByCurrentUser = !wasLiked;
    this.post.likesCount += wasLiked ? -1 : 1;

    // Poziv backend-a
    const request$ = wasLiked 
      ? this.postService.unlikePost(this.post.id)
      : this.postService.likePost(this.post.id);

    request$.subscribe({
      next: (response) => {
        console.log('✅ Like/Unlike uspešan:', response);
        // Backend je potvrdio - sve OK!
      },
      error: (err) => {
        console.error('❌ Greška pri like/unlike:', err);
        
        // Rollback optimističkog update-a
        if (this.post) {
          this.post.isLikedByCurrentUser = wasLiked;
          this.post.likesCount += wasLiked ? 1 : -1;
        }

        // Provera da li je greška 401 (neautentifikovan)
        if (err.status === 401) {
          alert('Sesija je istekla. Molimo prijavite se ponovo.');
          this.authService.logout();
          this.router.navigate(['/login']);
        } else {
          alert('Greška pri lajkovanju. Pokušajte ponovo.');
        }
      }
    });
  }

  // ============================================
  // USER ACTIONS
  // ============================================

  onComment(): void {
    if (!this.isLoggedIn) {
      alert('Morate se prijaviti da biste komentarisali! 💬');
      return;
    }

    // Scroll do komentara
    const commentsSection = document.querySelector('app-comments');
    if (commentsSection) {
      commentsSection.scrollIntoView({ behavior: 'smooth' });
    }

    console.log('💬 Scroll do komentara');
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
