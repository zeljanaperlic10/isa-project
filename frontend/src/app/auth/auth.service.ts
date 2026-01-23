import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { RegisterRequest } from '../models/register-request.model';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { User } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private API_URL = 'http://localhost:9090/auth';
  
  // BehaviorSubject čuva trenutno prijavljenog korisnika
  private currentUserSubject: BehaviorSubject<User | null>;
  public currentUser: Observable<User | null>;

  constructor(private http: HttpClient) {
    // Učitaj korisnika iz localStorage (ako postoji)
    const storedUser = localStorage.getItem('currentUser');
    this.currentUserSubject = new BehaviorSubject<User | null>(
      storedUser ? JSON.parse(storedUser) : null
    );
    this.currentUser = this.currentUserSubject.asObservable();
  }

  // Getter za trenutnog korisnika
  public get currentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  // Getter za JWT token
  public get token(): string | null {
    return localStorage.getItem('jwt_token');
  }

  // Provera da li je korisnik prijavljen
  public isLoggedIn(): boolean {
    return !!this.token && !!this.currentUserValue;
  }

  // REGISTRACIJA
  register(request: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.API_URL}/register`, request);
  }

  // LOGIN
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, request)
      .pipe(
        tap(response => {
          // Čuvanje tokena i korisnika u localStorage
          localStorage.setItem('jwt_token', response.token);
          localStorage.setItem('currentUser', JSON.stringify(response.user));
          this.currentUserSubject.next(response.user);
          console.log('✅ Korisnik prijavljen:', response.user.username);
        })
      );
  }

  // LOGOUT
  logout(): void {
    // Brisanje tokena i korisnika iz localStorage
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    console.log('🚪 Korisnik odjavljen');
  }

  // AKTIVACIJA NALOGA
  activateAccount(token: string): Observable<string> {
    return this.http.get(`${this.API_URL}/activate?token=${token}`, { 
      responseType: 'text' 
    });
  }
}
