import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Post } from '../models/post.model';

@Injectable({
  providedIn: 'root'
})
export class PostService {

  private apiUrl = 'http://localhost:9090/api';

  constructor(private http: HttpClient) { }

  // ============================================
  // GET ENDPOINTS (3.1 - javno dostupno)
  // ============================================

  /**
   * Dobija sve postove (za HOME feed)
   */
  getAllPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.apiUrl}/posts`);
  }

  /**
   * Dobija jedan post po ID-u (sa view increment)
   */
  getPostById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.apiUrl}/posts/${id}`);
  }

  /**
   * Dobija post BEZ incrementa view count-a (za refresh) - NOVO! 🔄
   */
  refreshPost(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.apiUrl}/posts/${id}/refresh`);
  }

  /**
   * Dobija postove korisnika
   */
  getUserPosts(username: string): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.apiUrl}/posts/user/${username}`);
  }

  /**
   * Dobija postove sa određenim tagom
   */
  getPostsByTag(tagName: string): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.apiUrl}/posts/tag/${tagName}`);
  }

  /**
   * Dobija korisnika po username-u (za profil stranicu - 3.1 zahtev)
   */
  getUserByUsername(username: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/users/${username}`);
  }

  // ============================================
  // LAJKOVANJE (LIKE/UNLIKE) ❤️
  // ============================================

  /**
   * Lajkuje post
   * @param postId - ID posta
   * @returns Observable sa odgovorom servera
   */
  likePost(postId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/posts/${postId}/like`, {});
  }

  /**
   * Uklanja lajk sa posta
   * @param postId - ID posta
   * @returns Observable sa odgovorom servera
   */
  unlikePost(postId: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/posts/${postId}/like`);
  }

  /**
   * Proverava da li je korisnik lajkovao post
   * @param postId - ID posta
   * @returns Observable sa statusom { postId, isLiked }
   */
  getLikeStatus(postId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/posts/${postId}/like/status`);
  }

  // ============================================
  // BROJAČ KOMENTARA (3.6 zahtev)
  // ============================================

  /**
   * Inkrementira brojač komentara na postu.
   * Poziva se kada se kreira novi komentar.
   * 
   * @param postId - ID posta
   */
  incrementCommentsCount(postId: number): void {
    console.log(`➕ Increment comments count za post ${postId}`);
  }

  /**
   * Dekrementira brojač komentara na postu.
   * Poziva se kada se briše komentar.
   * 
   * @param postId - ID posta
   */
  decrementCommentsCount(postId: number): void {
    console.log(`➖ Decrement comments count za post ${postId}`);
  }

  // ============================================
  // URL HELPERS (za video i thumbnail)
  // ============================================

  /**
   * Vraća pun URL za video streaming
   */
  getVideoUrl(filename: string): string {
    return `${this.apiUrl}/videos/${filename}`;
  }

  /**
   * Vraća pun URL za thumbnail sliku
   */
  getThumbnailUrl(filename: string): string {
    return `${this.apiUrl}/thumbnails/${filename}`;
  }

  // ============================================
  // POST/DELETE ENDPOINTS (3.3)
  // ============================================

  /**
   * Kreira novi post (upload videa - 3.3 zahtev)
   * @param formData - FormData sa video, thumbnail, title, description, tags, geolokacija
   */
  createPost(formData: FormData): Observable<Post> {
    return this.http.post<Post>(`${this.apiUrl}/posts`, formData);
  }

  /**
   * Briše post (samo vlasnik)
   */
  deletePost(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/posts/${id}`);
  }
}