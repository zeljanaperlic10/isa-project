import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { HomeComponent } from './home/home.component';
import { VideoPlayerComponent } from './video-player/video-player.component';
import { UploadComponent } from './upload/upload.component';
import { ProfileComponent } from './profile/profile.component';
import { WatchPartyListComponent } from './watch-party-list/watch-party-list.component';
import { WatchPartyRoomComponent } from './watch-party-room/watch-party-room.component';

const routes: Routes = [
  // Default route
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  
  // Auth routes
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  
  // Home route (javno dostupno)
  { path: 'home', component: HomeComponent },
  
  // Video player route (javno dostupno - 3.1 zahtev)
  { path: 'video/:id', component: VideoPlayerComponent },
  
  // Upload route (samo za autentifikovane - 3.3 zahtev)
  { path: 'upload', component: UploadComponent },
  
  // Profile route (javno dostupno - 3.1 zahtev)
  { path: 'profile/:username', component: ProfileComponent },
  
  // Watch Party routes (3.15 zahtev) - MORA biti PRE wildcard-a!
  { path: 'watch-party-list', component: WatchPartyListComponent },
  { path: 'watch-party/:id', component: WatchPartyRoomComponent },
  
  // Wildcard route (404) - MORA biti POSLEDNJI!
  { path: '**', redirectTo: '/home' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }