import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommentService } from '../services/comment.service';
import { AuthService } from '../auth/auth.service';
import { Comment, CommentPage } from '../models/comment.model';


@Component({
  selector: 'app-comments',
  templateUrl: './comments.component.html',
  styleUrls: ['./comments.component.css']
})
export class CommentsComponent implements OnInit {

  // ============================================
  // INPUT - ID posta (prima od parent komponente)
  // ============================================
  
  @Input() postId!: number;


  
  @Output() commentAdded = new EventEmitter<void>();
  @Output() commentDeleted = new EventEmitter<void>();

 
  
  comments: Comment[] = [];
  
  // ============================================
  // PAGINACIJA (3.6 zahtev)
  // ============================================
  
  currentPage: number = 0;
  totalPages: number = 0;
  totalComments: number = 0;
  hasMore: boolean = false;
  loadingMore: boolean = false;

  // ============================================
  // FORMA ZA NOVI KOMENTAR
  // ============================================
  
  newCommentText: string = '';
  submitting: boolean = false;

  // ============================================
  // UI STATE
  // ============================================
  
  loading: boolean = true;
  error: string = '';

  // ============================================
  // AUTH
  // ============================================
  
  isLoggedIn: boolean = false;
  currentUsername: string = '';

  // ============================================
  // CONSTRUCTOR
  // ============================================
  
  constructor(
    private commentService: CommentService,
    private authService: AuthService
  ) { }

  // ============================================
  // LIFECYCLE
  // ============================================
  
  ngOnInit(): void {
    console.log('💬 CommentsComponent init - Post ID:', this.postId);

    // Provera autentifikacije
    this.isLoggedIn = this.authService.isLoggedIn();
    if (this.isLoggedIn && this.authService.currentUserValue) {
      this.currentUsername = this.authService.currentUserValue.username;
      console.log('👤 Prijavljen kao:', this.currentUsername);
    }

    // Učitaj komentare
    this.loadComments();
  }

  // ============================================
  // UČITAVANJE KOMENTARA (3.6 - paginacija)
  // ============================================
  
  
  loadComments(): void {
    this.loading = true;
    this.error = '';

    console.log(`📖 Učitavanje komentara za post ${this.postId}, stranica 0`);

    this.commentService.getComments(this.postId, 0).subscribe({
      next: (page: CommentPage) => {
        console.log('✅ Komentari učitani:', page.numberOfElements);
        
        this.comments = page.content;
        this.currentPage = page.number;
        this.totalPages = page.totalPages;
        this.totalComments = page.totalElements;
        this.hasMore = !page.last;
        
        this.loading = false;

        console.log(`   Ukupno komentara: ${this.totalComments}`);
        console.log(`   Ukupno stranica: ${this.totalPages}`);
        console.log(`   Ima još: ${this.hasMore}`);
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju komentara:', err);
        this.error = 'Greška pri učitavanju komentara';
        this.loading = false;
      }
    });
  }

  
  loadMoreComments(): void {
    if (!this.hasMore || this.loadingMore) {
      return;
    }

    this.loadingMore = true;
    const nextPage = this.currentPage + 1;

    console.log(`📖 Učitavanje još komentara - stranica ${nextPage}`);

    this.commentService.getComments(this.postId, nextPage).subscribe({
      next: (page: CommentPage) => {
        console.log('✅ Još komentara učitano:', page.numberOfElements);
        
        // Dodaj nove komentare na postojeću listu
        this.comments = [...this.comments, ...page.content];
        
        this.currentPage = page.number;
        this.hasMore = !page.last;
        this.loadingMore = false;

        console.log(`   Trenutno učitano: ${this.comments.length} od ${this.totalComments}`);
      },
      error: (err) => {
        console.error('❌ Greška pri učitavanju još komentara:', err);
        this.loadingMore = false;
      }
    });
  }


  submitComment(): void {
    // Validacija
    if (!this.newCommentText || this.newCommentText.trim().length === 0) {
      alert('Unesite tekst komentara!');
      return;
    }

    if (!this.isLoggedIn) {
      alert('Morate biti prijavljeni da biste komentarisali!');
      return;
    }

    this.submitting = true;

    console.log('💬 Kreiranje komentara...');
    console.log('   Tekst:', this.newCommentText);

    this.commentService.createComment(this.postId, this.newCommentText).subscribe({
      next: (comment: Comment) => {
        console.log('✅ Komentar kreiran:', comment);

        // Dodaj novi komentar NA POČETAK liste (najnoviji prvi - 3.6 zahtev)
        this.comments.unshift(comment);
        this.totalComments++;

        // NOVO - Obavesti parent komponentu da osvježi brojač! ❤️
        this.commentAdded.emit();
        console.log('📤 Event commentAdded emitovan!');

        // Resetuj formu
        this.newCommentText = '';
        this.submitting = false;

        alert('Komentar uspešno dodat! 💬');
      },
      error: (err) => {
        console.error('❌ Greška pri kreiranju komentara:', err);
        
        // Provera rate limit greške
        const errorMessage = err.error?.error || err.error || 'Greška pri kreiranju komentara';
        
        alert(errorMessage);
        this.submitting = false;
      }
    });
  }

  // ============================================
  // BRISANJE KOMENTARA (3.6 - samo vlasnik)
  // ============================================
  
  /**
   * Briše komentar.
   * Samo vlasnik može obrisati svoj komentar.
   * 
   * @param comment - Komentar za brisanje
   */
  deleteComment(comment: Comment): void {
    // Provera vlasništva
    if (comment.username !== this.currentUsername) {
      alert('Možete obrisati samo svoje komentare!');
      return;
    }

    if (!confirm('Da li ste sigurni da želite da obrišete ovaj komentar?')) {
      return;
    }

    console.log('🗑️ Brisanje komentara:', comment.id);

    this.commentService.deleteComment(comment.id).subscribe({
      next: (message) => {
        console.log('✅', message);

        // Ukloni iz liste
        this.comments = this.comments.filter(c => c.id !== comment.id);
        this.totalComments--;

        // NOVO - Obavesti parent komponentu da osvježi brojač! ❤️
        this.commentDeleted.emit();
        console.log('📤 Event commentDeleted emitovan!');

        alert('Komentar obrisan! 🗑️');
      },
      error: (err) => {
        console.error('❌ Greška pri brisanju komentara:', err);
        alert('Greška pri brisanju komentara!');
      }
    });
  }

  // ============================================
  // HELPER METODE
  // ============================================
  
  /**
   * Proverava da li je trenutni korisnik vlasnik komentara.
   * 
   * @param comment - Komentar
   * @returns true ako je vlasnik
   */
  isOwner(comment: Comment): boolean {
    return this.isLoggedIn && comment.username === this.currentUsername;
  }

  /**
   * Formatira datum za prikaz.
   * Angular Date pipe će to uraditi u HTML-u.
   * 
   * @param dateString - ISO 8601 string
   * @returns Date objekat
   */
  parseDate(dateString: string): Date {
    return new Date(dateString);
  }
}
