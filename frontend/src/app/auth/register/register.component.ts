import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

  registerForm!: FormGroup;
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit(): void {
    // Kreiranje forme sa validacijama
    this.registerForm = this.formBuilder.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      address: ['', [Validators.required, Validators.minLength(5)]]
    });
  }

  // Pomoćna metoda za lak pristup form kontrolama
  get f() { 
    return this.registerForm.controls; 
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    // Provera validnosti forme
    if (this.registerForm.invalid) {
      return;
    }

    // Provera da li se lozinke poklapaju
    if (this.f['password'].value !== this.f['confirmPassword'].value) {
      this.errorMessage = 'Lozinke se ne poklapaju!';
      return;
    }

    this.loading = true;

    // Slanje zahteva za registraciju
    this.authService.register(this.registerForm.value).subscribe({
      next: (response) => {
        console.log('✅ Registracija uspešna:', response);
        this.successMessage = 'Registracija uspešna! Proverite email za aktivaciju naloga.';
        this.loading = false;
        
        // Redirect na login nakon 3 sekunde
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        console.error('❌ Greška pri registraciji:', error);
        this.errorMessage = error.error || 'Greška pri registraciji. Pokušajte ponovo.';
        this.loading = false;
      }
    });
  }
}
