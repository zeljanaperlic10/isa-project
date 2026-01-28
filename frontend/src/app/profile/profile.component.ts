import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PostService } from '../services/post.service';
import { Post } from '../models/post.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {

  // ============================================
  // USER DATA
  // ============================================
  user: any = null;
  username: string = '';

  // ============================================
  // POSTS DATA
  // ============================================
  posts: Post[] = [];
  
  // ============================================
  // UI STATE
  // ============================================
  loading: boolean = true;
  error: string = '';
  
  // ============================================
  // STATISTICS
  // ============================================
  totalPosts: number = 0;
  totalViews: number = 0;
  totalLikes: number = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private postService: PostService
  ) { }

  ngOnInit(): void {
    // Dobijanje username-a iz URL-a
    this.username = this.route.snapshot.paramMap.get('username') || '';
    
    if (this.username) {
      this.loadUserProfile();
      this.loadUserPosts();
    } else {
      this.error = 'Nevažeće korisničko ime';
      this.loading = false;
    }
  }

  // ============================================
  // LOAD USER DATA
  // ============================================

  loadUserProfile(): void {
    this.postService.getUserByUsername(this.username).subscribe({
      next: (user) => {
        console.log('✅ Korisnik učitan:', user);
        this.user = user;
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju korisnika:', err);
        this.error = 'Korisnik nije pronađen';
        this.loading = false;
      }
    });
  }

  loadUserPosts(): void {
    this.postService.getUserPosts(this.username).subscribe({
      next: (posts) => {
        console.log('✅ Postovi učitani:', posts.length);
        this.posts = posts;
        this.calculateStatistics();
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju postova:', err);
        this.error = 'Greška pri učitavanju postova';
        this.loading = false;
      }
    });
  }

  // ============================================
  // STATISTICS
  // ============================================

  calculateStatistics(): void {
    this.totalPosts = this.posts.length;
    
    this.totalViews = this.posts.reduce((sum, post) => sum + post.viewsCount, 0);
    this.totalLikes = this.posts.reduce((sum, post) => sum + post.likesCount, 0);
  }

  // ============================================
  // NAVIGATION
  // ============================================

  openVideo(postId: number): void {
    this.router.navigate(['/video', postId]);
  }

  goBack(): void {
    this.router.navigate(['/home']);
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