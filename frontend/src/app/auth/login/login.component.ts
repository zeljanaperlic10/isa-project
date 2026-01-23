import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  loginForm!: FormGroup;
  loading = false;
  errorMessage = '';
  returnUrl = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit(): void {
    // Kreiranje forme sa validacijama
    this.loginForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });

    // Dobijanje returnUrl iz parametara (za redirect nakon login-a)
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/home';

    // Ako je korisnik već prijavljen, redirect na home
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/home']);
    }
  }

  // Pomoćna metoda za lak pristup form kontrolama
  get f() { 
    return this.loginForm.controls; 
  }

  onSubmit(): void {
    this.errorMessage = '';

    // Provera validnosti forme
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;

    // Slanje zahteva za login
    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        console.log('✅ Login uspešan:', response.user.username);
        this.loading = false;
        
        // Redirect na returnUrl ili home
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        console.error('❌ Greška pri login-u:', error);
        
        // Prikazivanje odgovarajuće poruke greške
        if (error.status === 401) {
          this.errorMessage = error.error || 'Pogrešan email ili lozinka!';
        } else if (error.status === 429) {
          this.errorMessage = 'Previše pokušaja! Pokušajte ponovo za 1 minut.';
        } else {
          this.errorMessage = 'Greška pri prijavi. Pokušajte ponovo.';
        }
        
        this.loading = false;
      }
    });
  }

  // Navigacija ka registraciji
  goToRegister(): void {
    this.router.navigate(['/register']);
  }
}
