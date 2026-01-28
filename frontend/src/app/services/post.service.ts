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
   * Dobija jedan post po ID-u
   */
  getPostById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.apiUrl}/posts/${id}`);
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
    // Napomena: Token se automatski dodaje preko HTTP Interceptor-a (kada ga napravimo)
    // Za sada, AuthGuard sprečava pristup upload stranici ako nisi prijavljen
    return this.http.post<Post>(`${this.apiUrl}/posts`, formData);
  }

  /**
   * Briše post (samo vlasnik)
   */
  deletePost(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/posts/${id}`);
  }
}