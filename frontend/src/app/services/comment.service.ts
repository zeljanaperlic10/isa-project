import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Comment, CommentPage, CreateCommentRequest } from '../models/comment.model';

/**
 * CommentService - Servis za HTTP pozive ka Comment API-ju (3.6 zahtev)
 * 
 * FUNKCIONALNOSTI:
 * - Kreiranje komentara (POST)
 * - Dobijanje komentara sa paginacijom (GET)
 * - Brisanje komentara (DELETE)
 */
@Injectable({
  providedIn: 'root'
})
export class CommentService {

  // ============================================
  // API URL
  // ============================================
  
  private apiUrl = 'http://localhost:9090/api';

  // ============================================
  // CONSTRUCTOR
  // ============================================
  
  constructor(private http: HttpClient) { }

  // ============================================
  // KREIRANJE KOMENTARA (3.6 - samo registrovani)
  // ============================================
  
  /**
   * Kreira novi komentar na postu.
   * 
   * ZAHTEVI (3.6):
   * - Samo registrovani korisnici (JWT token automatski iz interceptor-a)
   * - Rate limiting: 60 komentara po satu
   * 
   * ENDPOINT: POST /api/posts/{postId}/comments
   * 
   * REQUEST BODY:
   * {
   *   "text": "Odličan video!"
   * }
   * 
   * RESPONSE (201 CREATED):
   * {
   *   "id": 123,
   *   "text": "Odličan video!",
   *   "username": "petar123",
   *   "createdAt": "2026-01-28T20:30:00"
   * }
   * 
   * @param postId - ID posta
   * @param text - Tekst komentara
   * @returns Observable<Comment>
   */
  createComment(postId: number, text: string): Observable<Comment> {
    console.log(`💬 POST /api/posts/${postId}/comments - Kreiranje komentara`);
    
    const request: CreateCommentRequest = { text };
    
    return this.http.post<Comment>(
      `${this.apiUrl}/posts/${postId}/comments`,
      request
    );
  }

  // ============================================
  // DOBIJANJE KOMENTARA (3.6 - javno, paginacija)
  // ============================================
  
  /**
   * Dobija komentare za post sa paginacijom.
   * 
   * JAVNO DOSTUPNO (3.6):
   * - I neautentifikovani korisnici mogu videti komentare
   * 
   * PAGINACIJA (3.6):
   * - Vraća 20 komentara po stranici
   * - Metadata: totalPages, totalElements, itd.
   * 
   * SORTIRANJE (3.6):
   * - Najnoviji → najstariji (backend automatski)
   * 
   * KEŠIRANJE (3.6):
   * - Backend kešira rezultate
   * 
   * ENDPOINT: GET /api/posts/{postId}/comments?page=0
   * 
   * RESPONSE (200 OK):
   * {
   *   "content": [Comment[]],
   *   "totalElements": 287,
   *   "totalPages": 15,
   *   "number": 0,
   *   "size": 20,
   *   "first": true,
   *   "last": false
   * }
   * 
   * @param postId - ID posta
   * @param page - Broj stranice (default 0)
   * @returns Observable<CommentPage>
   */
  getComments(postId: number, page: number = 0): Observable<CommentPage> {
    console.log(`📖 GET /api/posts/${postId}/comments?page=${page}`);
    
    const params = new HttpParams().set('page', page.toString());
    
    return this.http.get<CommentPage>(
      `${this.apiUrl}/posts/${postId}/comments`,
      { params }
    );
  }

  // ============================================
  // BRISANJE KOMENTARA (3.6 - samo vlasnik)
  // ============================================
  
  /**
   * Briše komentar.
   * 
   * ZAHTEVI (3.6):
   * - Samo vlasnik može obrisati svoj komentar
   * - JWT token automatski iz interceptor-a
   * 
   * ENDPOINT: DELETE /api/comments/{commentId}
   * 
   * RESPONSE (200 OK):
   * "Komentar uspešno obrisan!"
   * 
   * @param commentId - ID komentara
   * @returns Observable<string>
   */
  deleteComment(commentId: number): Observable<string> {
    console.log(`🗑️ DELETE /api/comments/${commentId}`);
    
    return this.http.delete(
      `${this.apiUrl}/comments/${commentId}`,
      { responseType: 'text' }  // Očekujemo string response
    );
  }

  // ============================================
  // HELPER METODE
  // ============================================
  
  /**
   * Test API poziv.
   * 
   * @returns Observable<string>
   */
  testApi(): Observable<string> {
    return this.http.get(`${this.apiUrl}/comments/test`, { responseType: 'text' });
  }
}