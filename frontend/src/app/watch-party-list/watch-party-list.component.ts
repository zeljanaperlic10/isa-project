import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { WatchPartyService } from '../services/watch-party.service';
import { WatchParty } from '../models/watch-party.model';

/**
 * WatchPartyListComponent - Lista Watch Party soba (3.15 zahtev)
 * 
 * FUNKCIONALNOST:
 * - Prikazuje sve aktivne sobe
 * - Kreiranje nove sobe (modal)
 * - Pridruživanje sobi (klik)
 * - Tab-ovi: Aktivne sobe / Moje sobe / Pridružene sobe
 * 
 * LIFECYCLE:
 * 1. ngOnInit() - Učitaj aktivne sobe
 * 2. User klikne "Kreiraj sobu" → Otvori modal
 * 3. User klikne "Pridruži se" → joinRoom() → Router navigate
 */
@Component({
  selector: 'app-watch-party-list',
  templateUrl: './watch-party-list.component.html',
  styleUrls: ['./watch-party-list.component.css']
})
export class WatchPartyListComponent implements OnInit {

  // ============================================
  // STATE - Stanje komponente
  // ============================================

  /**
   * Lista soba koja se prikazuje.
   * 
   * Zavisi od trenutnog tab-a:
   * - activeTab === 'all' → Sve aktivne sobe
   * - activeTab === 'my' → Sobe koje sam kreirao
   * - activeTab === 'joined' → Sobe gde sam član
   */
  rooms: WatchParty[] = [];

  /**
   * Trenutno aktivan tab.
   * 
   * VREDNOSTI:
   * - 'all' → Sve sobe
   * - 'my' → Moje sobe
   * - 'joined' → Pridružene sobe
   */
  activeTab: string = 'all';

  /**
   * Loading state - prikazuje spinner dok se učitava.
   */
  loading: boolean = false;

  /**
   * Error poruka (ako nešto krene po zlu).
   */
  errorMessage: string = '';

  /**
   * Modal za kreiranje sobe - da li je otvoren.
   * 
   * true → Modal je vidljiv
   * false → Modal je sakriven
   */
  showCreateModal: boolean = false;

  /**
   * Naziv nove sobe (input iz modal-a).
   */
  newRoomName: string = '';

  /**
   * Creating state - prikazuje spinner u modal-u dok se kreira soba.
   */
  creating: boolean = false;

  /**
   * Username trenutno ulogovanog korisnika.
   * 
   * Koristi se da bi znali da li je korisnik kreator sobe.
   */
  currentUsername: string = '';

  // ============================================
  // CONSTRUCTOR
  // ============================================

  constructor(
    private watchPartyService: WatchPartyService,
    private router: Router
  ) {
    console.log('🎬 WatchPartyListComponent - Constructor');
  }

  // ============================================
  // LIFECYCLE HOOKS
  // ============================================

  /**
   * ngOnInit - Angular lifecycle hook.
   * 
   * Poziva se JEDNOM kada se komponenta kreira.
   * 
   * PROCES:
   * 1. Učitaj username iz localStorage
   * 2. Učitaj aktivne sobe
   */
  ngOnInit(): void {
    console.log('🎬 WatchPartyListComponent - ngOnInit');

    // Učitaj username
    this.currentUsername = localStorage.getItem('username') || '';

    // Učitaj aktivne sobe
    this.loadActiveRooms();
  }

  // ============================================
  // UČITAVANJE SOBA
  // ============================================

  /**
   * Učitaj aktivne sobe sa Backend-a.
   * 
   * HTTP REQUEST:
   * GET http://localhost:9090/api/watch-party/active
   * 
   * HTTP RESPONSE:
   * [
   *   { id: 123, name: "Movie Night", creator: {...}, members: [...] },
   *   { id: 124, name: "Study Session", creator: {...}, members: [...] }
   * ]
   * 
   * PROCES:
   * 1. Postavi loading = true (prikaži spinner)
   * 2. Pozovi Service
   * 3. Primi sobe
   * 4. Postavi rooms = primljene sobe
   * 5. Postavi loading = false (sakrij spinner)
   */
  loadActiveRooms(): void {
    console.log('📋 Učitavanje aktivnih soba...');

    this.loading = true;
    this.errorMessage = '';

    this.watchPartyService.getActiveRooms().subscribe(
      (rooms: WatchParty[]) => {
        console.log('✅ Sobe učitane:', rooms);
        
        this.rooms = rooms;
        this.loading = false;
      },
      (error) => {
        console.error('❌ Greška pri učitavanju soba:', error);
        
        this.errorMessage = 'Greška pri učitavanju soba. Pokušaj ponovo.';
        this.loading = false;
      }
    );
  }

  /**
   * Učitaj sobe koje je korisnik kreirao.
   */
  loadMyRooms(): void {
    console.log('📋 Učitavanje mojih soba...');

    this.loading = true;
    this.errorMessage = '';

    this.watchPartyService.getMyRooms().subscribe(
      (rooms: WatchParty[]) => {
        console.log('✅ Moje sobe učitane:', rooms);
        
        this.rooms = rooms;
        this.loading = false;
      },
      (error) => {
        console.error('❌ Greška:', error);
        
        this.errorMessage = 'Greška pri učitavanju soba.';
        this.loading = false;
      }
    );
  }

  /**
   * Učitaj sobe gde je korisnik član.
   */
  loadJoinedRooms(): void {
    console.log('📋 Učitavanje pridruženih soba...');

    this.loading = true;
    this.errorMessage = '';

    this.watchPartyService.getJoinedRooms().subscribe(
      (rooms: WatchParty[]) => {
        console.log('✅ Pridružene sobe učitane:', rooms);
        
        this.rooms = rooms;
        this.loading = false;
      },
      (error) => {
        console.error('❌ Greška:', error);
        
        this.errorMessage = 'Greška pri učitavanju soba.';
        this.loading = false;
      }
    );
  }

  // ============================================
  // TAB NAVIGACIJA
  // ============================================

  /**
   * Promeni aktivan tab.
   * 
   * POZIV:
   * <button (click)="switchTab('all')">Sve sobe</button>
   * 
   * PROCES:
   * 1. Postavi activeTab
   * 2. Učitaj odgovarajuće sobe
   * 
   * @param tab - 'all' | 'my' | 'joined'
   */
  switchTab(tab: string): void {
    console.log('📑 Menjam tab na:', tab);

    this.activeTab = tab;

    // Učitaj odgovarajuće sobe:
    if (tab === 'all') {
      this.loadActiveRooms();
    } else if (tab === 'my') {
      this.loadMyRooms();
    } else if (tab === 'joined') {
      this.loadJoinedRooms();
    }
  }

  // ============================================
  // KREIRANJE SOBE
  // ============================================

  /**
   * Otvori modal za kreiranje sobe.
   * 
   * POZIV:
   * <button (click)="openCreateModal()">+ Kreiraj sobu</button>
   */
  openCreateModal(): void {
    console.log('📝 Otvaranje modal-a za kreiranje sobe...');

    this.showCreateModal = true;
    this.newRoomName = '';  // Resetuj input
    this.errorMessage = '';
  }

  /**
   * Zatvori modal za kreiranje sobe.
   * 
   * POZIV:
   * <button (click)="closeCreateModal()">Otkaži</button>
   */
  closeCreateModal(): void {
    console.log('❌ Zatvaranje modal-a...');

    this.showCreateModal = false;
    this.newRoomName = '';
    this.errorMessage = '';
  }

  /**
   * Kreiraj novu sobu.
   * 
   * POZIV:
   * <button (click)="createRoom()">Kreiraj</button>
   * 
   * PROCES:
   * 1. Validacija naziva
   * 2. Pozovi Service.createRoom()
   * 3. Primi kreiranu sobu
   * 4. Navigiraj u sobu (router.navigate)
   * 
   * HTTP REQUEST:
   * POST http://localhost:9090/api/watch-party/create
   * Body: { "name": "Movie Night" }
   * 
   * HTTP RESPONSE:
   * { "id": 123, "name": "Movie Night", ... }
   */
  createRoom(): void {
    console.log('📝 Kreiranje sobe...');
    console.log('   Naziv:', this.newRoomName);

    // VALIDACIJA 1: Prazan naziv
    if (!this.newRoomName || this.newRoomName.trim() === '') {
      this.errorMessage = 'Naziv sobe ne sme biti prazan!';
      return;
    }

    // VALIDACIJA 2: Dužina naziva
    if (this.newRoomName.length > 200) {
      this.errorMessage = 'Naziv sobe može imati maksimum 200 karaktera!';
      return;
    }

    this.creating = true;
    this.errorMessage = '';

    // Pozovi Service:
    this.watchPartyService.createRoom(this.newRoomName.trim()).subscribe(
      (party: WatchParty) => {
        console.log('✅ Soba kreirana:', party);

        this.creating = false;
        this.closeCreateModal();

        // NAVIGIRAJ U SOBU!
        this.router.navigate(['/watch-party', party.id]);
      },
      (error) => {
        console.error('❌ Greška pri kreiranju sobe:', error);

        this.errorMessage = error.error?.error || 'Greška pri kreiranju sobe. Pokušaj ponovo.';
        this.creating = false;
      }
    );
  }

  // ============================================
  // PRIDRUŽIVANJE SOBI
  // ============================================

  /**
   * Pridruži se sobi.
   * 
   * POZIV:
   * <button (click)="joinRoom(party)">Pridruži se</button>
   * 
   * PROCES:
   * 1. Pozovi Service.joinRoom() (REST API - dodaj u bazu)
   * 2. Navigiraj u sobu
   * 3. Komponenta sobe će pozvati WebSocket connect/subscribe
   * 
   * @param party - WatchParty soba
   */
  joinRoom(party: WatchParty): void {
    console.log('➕ Pridruživanje sobi:', party.id);

    // REST API poziv - dodaj u bazu:
    this.watchPartyService.joinRoom(party.id).subscribe(
      (updatedParty: WatchParty) => {
        console.log('✅ Pridružen sobi!');

        // NAVIGIRAJ U SOBU!
        this.router.navigate(['/watch-party', party.id]);
      },
      (error) => {
        console.error('❌ Greška:', error);
        alert('Greška pri pridruživanju sobi: ' + (error.error?.error || 'Nepoznata greška'));
      }
    );
  }

  // ============================================
  // HELPER METODE
  // ============================================

  /**
   * Da li je korisnik kreator sobe.
   * 
   * PROVERA:
   * - party.creator.username === currentUsername (ako je username)
   * - party.creator.email === currentUsername (ako je email)
   * 
   * KORISTI SE U TEMPLATE-u:
   * <span *ngIf="isCreator(party)">Tvoja soba</span>
   * 
   * @param party - WatchParty soba
   * @returns true ako je korisnik kreator
   */
  isCreator(party: WatchParty): boolean {
    // Proveri username
    if (party.creator.username === this.currentUsername) {
      return true;
    }
    
    // Proveri email (ako currentUsername je email)
    if (party.creator.email === this.currentUsername) {
      return true;
    }
    
    return false;
  }

  /**
   * Da li je korisnik član sobe.
   * 
   * PROBLEM: localStorage može imati EMAIL, ali members može imati USERNAME!
   * 
   * REŠENJE: Proveri sve kombinacije:
   * 1. Da li je kreator (kreator je automatski član)
   * 2. Da li je u members listi (po currentUsername)
   * 
   * @param party - WatchParty soba
   * @returns true ako je korisnik član
   */
  isMember(party: WatchParty): boolean {
    // PRVO: Ako je kreator, automatski je član
    if (this.isCreator(party)) {
      return true;
    }
    
    // DRUGO: Proveri da li je u members listi
    // members može sadržati username ili email, a currentUsername može biti bilo šta
    const isMemberInList = party.members.some(member => 
      member === this.currentUsername || 
      member.toLowerCase() === this.currentUsername.toLowerCase()
    );
    
    return isMemberInList;
  }

  /**
   * Da li ima trenutni video.
   * 
   * @param party - WatchParty soba
   * @returns true ako ima trenutni video
   */
  hasCurrentVideo(party: WatchParty): boolean {
    return party.currentPost !== null;
  }
}