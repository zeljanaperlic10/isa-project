import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { PostService } from '../services/post.service';
import { Post } from '../models/post.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  // ============================================
  // AUTHENTICATION
  // ============================================
  isLoggedIn: boolean = false;
  currentUser: any = null;

  // ============================================
  // POSTS
  // ============================================
  posts: Post[] = [];
  loading: boolean = true;
  error: string = '';

  constructor(
    private authService: AuthService,
    private postService: PostService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Provera autentifikacije
    this.checkAuthentication();

    // Učitavanje postova
    this.loadPosts();
  }

  // ============================================
  // AUTHENTICATION METHODS
  // ============================================

  checkAuthentication(): void {
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn) {
      this.currentUser = this.authService.currentUserValue;
    }
  }

  logout(): void {
    this.authService.logout();
    this.isLoggedIn = false;
    this.currentUser = null;
    this.router.navigate(['/login']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  goToUpload(): void {
    this.router.navigate(['/upload']);
  }

  /**
   * Navigacija na Watch Party listu soba (3.15 zahtev)
   */
  goToWatchParty(): void {
    this.router.navigate(['/watch-party-list']);
  }

  goToProfile(username: string): void {
    this.router.navigate(['/profile', username]);
  }

  // ============================================
  // POST METHODS
  // ============================================

  loadPosts(): void {
    this.loading = true;
    this.error = '';

    this.postService.getAllPosts().subscribe({
      next: (posts) => {
        console.log('✅ Postovi učitani:', posts);
        this.posts = posts;
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju postova:', err);
        this.error = 'Greška pri učitavanju postova. Pokušajte ponovo.';
        this.loading = false;
      }
    });
  }

  /**
   * Otvara video player stranicu
   */
  openVideo(postId: number): void {
    this.router.navigate(['/video', postId]);
  }

  /**
   * Helper metoda - vraća thumbnail URL
   */
  getThumbnailUrl(post: Post): string {
    // Ako je YouTube link - koristi direktno
    if (post.thumbnailUrl.startsWith('http')) {
      return post.thumbnailUrl;
    }
    // Ako je lokalni fajl - konstruiši URL
    return `http://localhost:9090${post.thumbnailUrl}`;
  }
}