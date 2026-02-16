import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { WatchPartyService } from '../services/watch-party.service';
import { WatchParty, WatchPartyEvent } from '../models/watch-party.model';
import { Subscription } from 'rxjs';
import { PostService } from '../services/post.service';

/**
 * WatchPartyRoomComponent - Unutar Watch Party sobe (3.15 zahtev)
 * 
 * FUNKCIONALNOST:
 * - WebSocket konekcija
 * - Subscribe na sobu (prima event-e)
 * - Pokretanje videa (kreator)
 * - Automatsko otvaranje videa kod svih (VIDEO_STARTED event)
 * - Lista članova
 * - Real-time notifikacije
 * 
 * LIFECYCLE:
 * 1. ngOnInit() - Konektuj WebSocket, učitaj sobu, subscribe
 * 2. Korisnik u sobi - Prima event-e
 * 3. ngOnDestroy() - Disconnect WebSocket
 */
@Component({
  selector: 'app-watch-party-room',
  templateUrl: './watch-party-room.component.html',
  styleUrls: ['./watch-party-room.component.css']
})
export class WatchPartyRoomComponent implements OnInit, OnDestroy {

  // ============================================
  // STATE
  // ============================================

  /**
   * ID sobe (iz URL-a)
   */
  roomId: number = 0;

  /**
   * WatchParty objekat (učitan sa Backend-a)
   */
  room: WatchParty | null = null;

  /**
   * Loading state
   */
  loading: boolean = true;

  /**
   * Error poruka
   */
  errorMessage: string = '';

  /**
   * Username trenutno ulogovanog korisnika
   */
  currentUsername: string = '';

  /**
   * Lista videa (mock podaci - trebalo bi učitati sa Backend-a)
   * 
   * U realnoj aplikaciji:
   * - Učitaj sve postove sa Backend-a
   * - Prikaži ih u dropdown-u
   * - Kreator bira video koji želi da pokrene
   */
  availableVideos: any[] = [];

  /**
   * Izabrani video (iz dropdown-a)
   */
  selectedVideoId: number | null = null;

  /**
   * Da li je WebSocket konektovan
   */
  websocketConnected: boolean = false;

  /**
   * RXJS Subscription za event-e
   * 
   * Koristimo da bi mogli unsubscribe-ovati u ngOnDestroy
   */
  private eventsSubscription: Subscription | null = null;

  // ============================================
  // CONSTRUCTOR
  // ============================================

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private watchPartyService: WatchPartyService,
    private postService: PostService
  ) {
    console.log('🎬 WatchPartyRoomComponent - Constructor');
  }

  // ============================================
  // LIFECYCLE HOOKS
  // ============================================

  
  ngOnInit(): void {
    console.log('🎬 WatchPartyRoomComponent - ngOnInit');

    // 1. Učitaj roomId iz URL-a
    // URL: /watch-party/123 → roomId = 123
    this.route.params.subscribe(params => {
      this.roomId = +params['id'];  // + konvertuje string → number
      console.log('   Room ID:', this.roomId);

      // 2. Učitaj username
      this.currentUsername = localStorage.getItem('username') || '';

      // 3. Konektuj WebSocket
      this.connectWebSocket();

      // 4. Učitaj sobu
      this.loadRoom();

      // 5. Učitaj dostupne videe (MOCK - u realnosti sa Backend-a)
      this.loadAvailableVideos();
    });
  }

  
  ngOnDestroy(): void {
    console.log('🎬 WatchPartyRoomComponent - ngOnDestroy');

    // 1. Notifikuj ostale
    if (this.websocketConnected) {
      this.watchPartyService.notifyLeft(this.roomId);
    }

    // 2. Unsubscribe
    if (this.eventsSubscription) {
      this.eventsSubscription.unsubscribe();
    }

    // 3. Disconnect WebSocket
    this.watchPartyService.unsubscribeFromRoom();
    this.watchPartyService.disconnect();
  }

  
  connectWebSocket(): void {
    console.log('🔌 Konektovanje WebSocket...');

    // 1. Konektuj
    this.watchPartyService.connect(() => {
      console.log('✅ WebSocket konektovan!');
      this.websocketConnected = true;

      // 2. Subscribe na sobu
      this.watchPartyService.subscribeToRoom(this.roomId);

      // 3. Notifikuj ostale da si se pridružio
      this.watchPartyService.notifyJoined(this.roomId);

      // 4. Slušaj event-e
      this.subscribeToEvents();
    });
  }

  
  subscribeToEvents(): void {
    console.log('📡 Subscribe na event-e...');

    this.eventsSubscription = this.watchPartyService.events$.subscribe(
      (event: WatchPartyEvent) => {
        console.log('📨 Primljen event:', event);

        // Proveri da li je event za ovu sobu
        if (event.roomId !== this.roomId) {
          console.log('⚠️ Event nije za ovu sobu, ignoriši');
          return;
        }

        // Reaguj na tip event-a
        switch (event.type) {
          case 'VIDEO_STARTED':
            this.onVideoStarted(event);
            break;

          case 'USER_JOINED':
            this.onUserJoined(event);
            break;

          case 'USER_LEFT':
            this.onUserLeft(event);
            break;

          case 'ROOM_CLOSED':
            this.onRoomClosed(event);
            break;

          case 'ERROR':
            this.onError(event);
            break;

          default:
            console.log('⚠️ Nepoznat tip event-a:', event.type);
        }
      },
      (error) => {
        console.error('❌ Greška pri primanju event-a:', error);
      }
    );
  }

 
  onVideoStarted(event: WatchPartyEvent): void {
    console.log('🎬 VIDEO STARTED:', event);

    // Prikaži notifikaciju
    this.showNotification(`${event['startedBy']} je pokrenuo video: ${event['postTitle']}`);

    // AUTOMATSKI OTVORI VIDEO!
    console.log('▶️ Automatski otvaram video:', event['postId']);
    
    // Navigiraj na video stranicu
    this.router.navigate(['/video', event['postId']]);
  }

  /**
   * USER_JOINED event - Novi član se pridružio.
   */
  onUserJoined(event: WatchPartyEvent): void {
    console.log('➕ USER JOINED:', event);

    this.showNotification(`${event['username']} se pridružio sobi!`);

    // Ažuriraj sobu (refresh podatke)
    this.loadRoom();
  }

  /**
   * USER_LEFT event - Član je napustio sobu.
   */
  onUserLeft(event: WatchPartyEvent): void {
    console.log('➖ USER LEFT:', event);

    this.showNotification(`${event['username']} je napustio sobu.`);

    // Ažuriraj sobu
    this.loadRoom();
  }

  /**
   * ROOM_CLOSED event - Soba je zatvorena.
   */
  onRoomClosed(event: WatchPartyEvent): void {
    console.log('🚫 ROOM CLOSED:', event);

    alert(`Soba je zatvorena od strane ${event['closedBy']}.`);

    // Redirect na homepage
    this.router.navigate(['/']);
  }

  /**
   * ERROR event - Greška sa Backend-a.
   */
  onError(event: WatchPartyEvent): void {
    console.error('❌ ERROR EVENT:', event);

    alert('Greška: ' + event['message']);
  }

  // ============================================
  // UČITAVANJE PODATAKA
  // ============================================

  /**
   * Učitaj sobu sa Backend-a.
   * 
   * HTTP REQUEST:
   * GET http://localhost:9090/api/watch-party/123
   */
  loadRoom(): void {
    console.log('📋 Učitavanje sobe...');

    this.loading = true;

    this.watchPartyService.getRoomById(this.roomId).subscribe(
      (room: WatchParty) => {
        console.log('✅ Soba učitana:', room);

        this.room = room;
        this.loading = false;
      },
      (error) => {
        console.error('❌ Greška:', error);

        this.errorMessage = 'Soba nije pronađena.';
        this.loading = false;
      }
    );
  }

 
  loadAvailableVideos(): void {
    console.log('📋 Učitavanje dostupnih videa...');

    // Učitaj SVE postove sa Backend-a
    this.postService.getAllPosts().subscribe(
      (posts) => {
        console.log('✅ Videi učitani:', posts);
        
        // Mapuj Post objekte u format za dropdown
        this.availableVideos = posts.map(post => ({
          id: post.id,
          title: post.title
        }));
        
        console.log('📹 Broj dostupnih videa:', this.availableVideos.length);
      },
      (error) => {
        console.error('❌ Greška pri učitavanju videa:', error);
        
        // Fallback - prazna lista
        this.availableVideos = [];
        alert('Greška pri učitavanju videa. Pokušaj ponovo.');
      }
    );
  }

  // ============================================
  // AKCIJE
  // ============================================

  /**
   * Kreator pokreće video.
   * 
   * POZIV:
   * <button (click)="startVideo()">Pokreni video</button>
   * 
   * PROCES:
   * 1. Validacija (da li je video izabran)
   * 2. Pošalji WebSocket poruku
   * 3. Backend broadcast-uje VIDEO_STARTED event
   * 4. Svi članovi primaju event i automatski otvaraju video!
   */
  startVideo(): void {
    if (!this.selectedVideoId) {
      alert('Izaberi video prvo!');
      return;
    }

    console.log('▶️ Pokretanje videa:', this.selectedVideoId);

    // Pošalji WebSocket poruku
    this.watchPartyService.startVideo(this.roomId, this.selectedVideoId);

    console.log('✅ Start video poruka poslata!');
  }

 
  leaveRoom(): void {
    console.log('➖ Napuštanje sobe...');

    // 1. Notifikuj ostale
    if (this.websocketConnected) {
      this.watchPartyService.notifyLeft(this.roomId);
    }

    // 2. REST API
    this.watchPartyService.leaveRoom(this.roomId).subscribe(
      () => {
        console.log('✅ Napuštena soba!');

        // 3. Redirect
        this.router.navigate(['/watch-party-list']);
      },
      (error) => {
        console.error('❌ Greška:', error);
        // Ipak redirect
        this.router.navigate(['/watch-party-list']);
      }
    );
  }

  /**
   * Zatvori sobu (samo kreator).
   */
  closeRoom(): void {
    if (!confirm('Da li si siguran da želiš da zatvoriš sobu?')) {
      return;
    }

    console.log('🚫 Zatvaranje sobe...');

    this.watchPartyService.closeRoom(this.roomId).subscribe(
      () => {
        console.log('✅ Soba zatvorena!');

        // Redirect
        this.router.navigate(['/watch-party-list']);
      },
      (error) => {
        console.error('❌ Greška:', error);
        alert('Greška pri zatvaranju sobe.');
      }
    );
  }

 
  isCreator(): boolean {
    return this.room?.creator.username === this.currentUsername;
  }

  
  showNotification(message: string): void {
    console.log('🔔 Notifikacija:', message);
    
    // Jednostavna implementacija:
    // alert(message);
    
    // Ili bolje - koristi toast library:
    // this.toastr.info(message);
  }
}