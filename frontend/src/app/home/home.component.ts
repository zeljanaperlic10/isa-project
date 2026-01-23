import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { User } from '../models/user.model';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {

  // Trenutno prijavljeni korisnik (null ako nije prijavljen)
  currentUser: User | null = null;

  // Da li je korisnik prijavljen
  isLoggedIn = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // KORAK 1: Provera da li je korisnik prijavljen
    this.isLoggedIn = this.authService.isLoggedIn();
    
    // KORAK 2: Dobijanje trenutnog korisnika (ako je prijavljen)
    if (this.isLoggedIn) {
      this.currentUser = this.authService.currentUserValue;
      console.log('✅ Prijavljen korisnik:', this.currentUser?.username);
    } else {
      console.log('👤 Korisnik nije prijavljen');
    }
  }

  // Navigacija ka Login stranici
  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  // Navigacija ka Register stranici
  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  // Logout korisnika
  logout(): void {
    // KORAK 1: Pozovi AuthService.logout() koji briše token i korisnika
    this.authService.logout();
    
    // KORAK 2: Postavi lokalne promenljive na null/false
    this.currentUser = null;
    this.isLoggedIn = false;
    
    // KORAK 3: Redirect na login stranicu
    this.router.navigate(['/login']);
    
    console.log('🚪 Korisnik se odjavio');
  }
}
