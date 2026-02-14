import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';  // Forme

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

// Components
import { RegisterComponent } from './auth/register/register.component';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';
import { VideoPlayerComponent } from './video-player/video-player.component';
import { UploadComponent } from './upload/upload.component';
import { ProfileComponent } from './profile/profile.component';      // Profil (3.1)
import { CommentsComponent } from './comments/comments.component';  // Komentari (3.6)

// Services
import { AuthService } from './auth/auth.service';
import { PostService } from './services/post.service';
import { CommentService } from './services/comment.service';  // Komentari (3.6)

// Interceptors
import { JwtInterceptor } from './auth/jwt.interceptor';
import { WatchPartyListComponent } from './watch-party-list/watch-party-list.component';
import { WatchPartyRoomComponent } from './watch-party-room/watch-party-room.component';

@NgModule({
  declarations: [
    AppComponent,
    RegisterComponent,
    LoginComponent,
    HomeComponent,
    VideoPlayerComponent,
    UploadComponent,
    ProfileComponent,    // Profil stranica (3.1)
    CommentsComponent, WatchPartyListComponent, WatchPartyRoomComponent    // Komentari (3.6)
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,      // HTTP zahtevi
    ReactiveFormsModule,   // Reactive forme (login, register)
    FormsModule            // Template-driven forme: [(ngModel)] u upload i comments (3.6)
  ],
  providers: [
    AuthService,
    PostService,
    CommentService,  // Komentari (3.6)
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