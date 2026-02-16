import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { WatchParty, WatchPartyEvent, CreateRoomRequest, StartVideoRequest } from '../models/watch-party.model';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService } from '../auth/auth.service';

/**
 * WatchPartyService - Service za Watch Party funkcionalnost (3.15 zahtev)
 * 
 * KOMBINUJE:
 * 1. REST API pozive (HttpClient) - CRUD operacije
 * 2. WebSocket komunikaciju (SockJS + STOMP) - Real-time events
 * 
 * AŽURIRANO: 
 * - Koristi AuthService za JWT token
 * - Šalje username u WebSocket payload-u
 */
@Injectable({
  providedIn: 'root'
})
export class WatchPartyService {

  // ============================================
  // KONFIGURACIJA
  // ============================================

  private apiUrl = 'http://localhost:9090/api/watch-party';
  private wsUrl = 'http://localhost:9090/ws';

  // ============================================
  // WEBSOCKET - STOMP CLIENT
  // ============================================

  private stompClient: Client | null = null;
  private isConnected: boolean = false;
  private currentRoomId: number | null = null;

  // ============================================
  // RXJS SUBJECTS - Event Stream
  // ============================================

  private eventsSubject = new Subject<WatchPartyEvent>();
  public events$ = this.eventsSubject.asObservable();

  // ============================================
  // CONSTRUCTOR
  // ============================================

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    console.log('🎬 WatchPartyService - Inicijalizacija');
  }

  // ============================================
  // REST API - CRUD OPERACIJE
  // ============================================

  createRoom(roomName: string): Observable<WatchParty> {
    console.log('📤 POST /api/watch-party/create');
    console.log('   Naziv:', roomName);

    const body: CreateRoomRequest = { name: roomName };
    const headers = this.getAuthHeaders();

    return this.http.post<WatchParty>(`${this.apiUrl}/create`, body, { headers });
  }

  getActiveRooms(): Observable<WatchParty[]> {
    console.log('📤 GET /api/watch-party/active');

    const headers = this.getAuthHeaders();
    return this.http.get<WatchParty[]>(`${this.apiUrl}/active`, { headers });
  }

  getMyRooms(): Observable<WatchParty[]> {
    console.log('📤 GET /api/watch-party/my-rooms');

    const headers = this.getAuthHeaders();
    return this.http.get<WatchParty[]>(`${this.apiUrl}/my-rooms`, { headers });
  }

  getJoinedRooms(): Observable<WatchParty[]> {
    console.log('📤 GET /api/watch-party/joined');

    const headers = this.getAuthHeaders();
    return this.http.get<WatchParty[]>(`${this.apiUrl}/joined`, { headers });
  }

  getRoomById(roomId: number): Observable<WatchParty> {
    console.log('📤 GET /api/watch-party/' + roomId);

    const headers = this.getAuthHeaders();
    return this.http.get<WatchParty>(`${this.apiUrl}/${roomId}`, { headers });
  }

  joinRoom(roomId: number): Observable<WatchParty> {
    console.log('📤 POST /api/watch-party/' + roomId + '/join');

    const headers = this.getAuthHeaders();
    return this.http.post<WatchParty>(`${this.apiUrl}/${roomId}/join`, {}, { headers });
  }

  leaveRoom(roomId: number): Observable<WatchParty> {
    console.log('📤 POST /api/watch-party/' + roomId + '/leave');

    const headers = this.getAuthHeaders();
    return this.http.post<WatchParty>(`${this.apiUrl}/${roomId}/leave`, {}, { headers });
  }

  closeRoom(roomId: number): Observable<WatchParty> {
    console.log('📤 DELETE /api/watch-party/' + roomId + '/close');

    const headers = this.getAuthHeaders();
    return this.http.delete<WatchParty>(`${this.apiUrl}/${roomId}/close`, { headers });
  }

  // ============================================
  // WEBSOCKET - KONEKCIJA
  // ============================================

  /**
   * Konektuj se na WebSocket.
   */
  connect(callback: () => void): void {
    console.log('='.repeat(80));
    console.log('🔌 POKRETANJE WEBSOCKET KONEKCIJE...');
    console.log('   URL:', this.wsUrl);
    console.log('='.repeat(80));

    if (this.isConnected && this.stompClient !== null) {
      console.log('⚠️ Već konektovan!');
      callback();
      return;
    }

    // ✅ KORISTI AuthService umesto localStorage!
    const token = this.authService.token;
    
    if (!token) {
      console.error('❌ Token nije pronađen u AuthService!');
      console.error('   Da li ste prijavljeni?');
      alert('Niste prijavljeni! Molimo prijavite se prvo.');
      return;
    }

    console.log('✅ Token pronađen preko AuthService');
    console.log('   Prvih 30 karaktera:', token.substring(0, 30) + '...');

    // Kreiraj STOMP client
    console.log('📦 Kreiram STOMP Client...');
    
    this.stompClient = new Client({
      webSocketFactory: () => {
        console.log('🏭 WebSocketFactory pozvan - kreiram SockJS...');
        const sockjs = new SockJS(this.wsUrl);
        
        sockjs.onopen = () => {
          console.log('✅ SockJS OPENED!');
        };
        
        sockjs.onclose = (event) => {
          console.log('🔌 SockJS CLOSED:', event);
        };
        
        sockjs.onerror = (error) => {
          console.error('❌ SockJS ERROR:', error);
        };
        
        return sockjs as any;
      },
      
      connectHeaders: {
        Authorization: 'Bearer ' + token
      },
      
      debug: (str) => {
        console.log('🔵 STOMP:', str);
      },
      
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: (frame) => {
        console.log('='.repeat(80));
        console.log('✅✅✅ WEBSOCKET USPEŠNO KONEKTOVAN! ✅✅✅');
        console.log('   Frame:', frame);
        console.log('='.repeat(80));
        
        this.isConnected = true;
        callback();
      },
      
      onStompError: (frame) => {
        console.error('='.repeat(80));
        console.error('❌❌❌ STOMP GREŠKA! ❌❌❌');
        console.error('   Command:', frame.command);
        console.error('   Headers:', frame.headers);
        console.error('   Body:', frame.body);
        console.error('='.repeat(80));
        this.isConnected = false;
        
        alert('WebSocket STOMP greška! Proveri konzolu.');
      },
      
      onWebSocketError: (event) => {
        console.error('='.repeat(80));
        console.error('❌❌❌ WEBSOCKET GREŠKA! ❌❌❌');
        console.error('   Event:', event);
        console.error('='.repeat(80));
        this.isConnected = false;
        
        alert('WebSocket ne može da se konektuje! Da li je backend pokrenut?');
      },
      
      onWebSocketClose: (event) => {
        console.log('='.repeat(80));
        console.log('🔌 WEBSOCKET ZATVOREN!');
        console.log('   Code:', event.code);
        console.log('   Reason:', event.reason);
        console.log('='.repeat(80));
        this.isConnected = false;
      }
    });

    console.log('🚀 Aktiviram STOMP Client...');
    this.stompClient.activate();
    console.log('⏳ Čekam na konekciju...');
  }

  disconnect(): void {
    if (this.stompClient !== null && this.isConnected) {
      console.log('🔌 Diskonektovanje sa WebSocket-a...');
      
      this.stompClient.deactivate();
      
      console.log('✅ WebSocket diskonektovan!');
      this.isConnected = false;
      this.currentRoomId = null;
    }
  }

  // ============================================
  // WEBSOCKET - SUBSCRIBE NA SOBU
  // ============================================

  subscribeToRoom(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan! Pozovi connect() prvo.');
      return;
    }

    console.log('='.repeat(80));
    console.log('📡 SUBSCRIBE NA SOBU:', roomId);
    console.log('='.repeat(80));

    this.currentRoomId = roomId;

    const topic = `/topic/watch-party/${roomId}`;

    this.stompClient.subscribe(topic, (message) => {
      console.log('='.repeat(80));
      console.log('📨 WEBSOCKET PORUKA PRIMLJENA!');
      console.log('   Topic:', topic);
      console.log('   Body:', message.body);
      console.log('='.repeat(80));

      try {
        const event: WatchPartyEvent = JSON.parse(message.body);
        
        console.log('✅ Event parsiran:');
        console.log('   Type:', event.type);
        console.log('   Payload:', JSON.stringify(event, null, 2));

        this.eventsSubject.next(event);
        
      } catch (e) {
        console.error('❌ Greška pri parsiranju JSON-a:', e);
      }
    });

    console.log('✅ Subscribe-ovan na:', topic);
  }

  unsubscribeFromRoom(): void {
    console.log('📡 Unsubscribe sa sobe:', this.currentRoomId);
    this.currentRoomId = null;
  }

  // ============================================
  // WEBSOCKET - SLANJE PORUKA (AŽURIRANO!)
  // ============================================

  /**
   * Kreator pokreće video.
   * 
   * AŽURIRANO: Šalje username u payload-u!
   */
  startVideo(roomId: number, postId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      alert('WebSocket nije konektovan! Osvježi stranicu.');
      return;
    }

    // ✅ DOBIJ USERNAME
    const username = localStorage.getItem('username');
    
    if (!username) {
      console.error('❌ Username nije pronađen u localStorage!');
      alert('Niste prijavljeni! Prijavite se ponovo.');
      return;
    }

    console.log('='.repeat(80));
    console.log('▶️ POKRETANJE VIDEA...');
    console.log('   Soba ID:', roomId);
    console.log('   Video ID:', postId);
    console.log('   Username:', username);

    const destination = `/app/watch-party/${roomId}/start-video`;
    
    // ✅ DODAJ USERNAME U PAYLOAD!
    const body: StartVideoRequest = { 
      postId: postId,
      username: username
    };

    console.log('   Destination:', destination);
    console.log('   Body:', JSON.stringify(body));

    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify(body)
    });

    console.log('✅ Start video poruka poslata!');
    console.log('='.repeat(80));
  }

  /**
   * Notifikuj ostale članove da si se pridružio.
   * 
   * AŽURIRANO: Šalje username u payload-u!
   */
  notifyJoined(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      return;
    }

    // ✅ DOBIJ USERNAME
    const username = localStorage.getItem('username');
    
    if (!username) {
      console.error('❌ Username nije pronađen!');
      return;
    }

    console.log('➕ Notifikacija: Pridružen sobi', roomId);
    console.log('   Username:', username);

    const destination = `/app/watch-party/${roomId}/join`;
    
    // ✅ DODAJ USERNAME U PAYLOAD!
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify({ username: username })
    });

    console.log('✅ Join notifikacija poslata!');
  }

  /**
   * Notifikuj ostale članove da si napustio sobu.
   * 
   * AŽURIRANO: Šalje username u payload-u!
   */
  notifyLeft(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      return;
    }

    // ✅ DOBIJ USERNAME
    const username = localStorage.getItem('username');
    
    if (!username) {
      console.error('❌ Username nije pronađen!');
      return;
    }

    console.log('➖ Notifikacija: Napuštena soba', roomId);
    console.log('   Username:', username);

    const destination = `/app/watch-party/${roomId}/leave`;
    
    // ✅ DODAJ USERNAME U PAYLOAD!
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify({ username: username })
    });

    console.log('✅ Leave notifikacija poslata!');
  }

  // ============================================
  // HELPER METODE
  // ============================================

  private getAuthHeaders(): HttpHeaders {
    const token = this.authService.token;

    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + token
    });
  }

  isWebSocketConnected(): boolean {
    return this.isConnected;
  }

  getCurrentRoomId(): number | null {
    return this.currentRoomId;
  }
}