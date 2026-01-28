import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';  // ← HTTP_INTERCEPTORS dodato
import { ReactiveFormsModule, FormsModule } from '@angular/forms';           // ← FormsModule dodato (za [(ngModel)])

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Components
import { RegisterComponent } from './auth/register/register.component';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { VideoPlayerComponent } from './video-player/video-player.component';
import { UploadComponent } from './upload/upload.component';

// Services
import { AuthService } from './auth/auth.service';
import { PostService } from './services/post.service';

// Interceptors
import { JwtInterceptor } from './auth/jwt.interceptor';
import { ProfileComponent } from './profile/profile.component';

@NgModule({
  declarations: [
    AppComponent,
    RegisterComponent,
    LoginComponent,
    HomeComponent,
    VideoPlayerComponent,
    UploadComponent,
    ProfileComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,      // HTTP zahtevi
    ReactiveFormsModule,   // Reactive forme (login, register)
    FormsModule            // Template-driven forme ([(ngModel)] u upload)
  ],
  providers: [
    AuthService,
    PostService,
    // HTTP Interceptor - automatski dodaje JWT token u sve HTTP zahteve
    {
      provide: HTTP_INTERCEPTORS,
      useClass: JwtInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }