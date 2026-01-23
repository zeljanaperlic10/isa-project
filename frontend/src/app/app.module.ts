import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';        // ← DODATO
import { ReactiveFormsModule } from '@angular/forms';           // ← DODATO

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { RegisterComponent } from './auth/register/register.component';
import { LoginComponent } from './auth/login/login.component';
import { HomeComponent } from './home/home.component';

@NgModule({
  declarations: [
    AppComponent,
    RegisterComponent,
    LoginComponent,
    HomeComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,      // ← DODATO - omogućava HTTP zahteve
    ReactiveFormsModule    // ← DODATO - omogućava reactive forme
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
