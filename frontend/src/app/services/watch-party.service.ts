import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { WatchParty, WatchPartyEvent, CreateRoomRequest, StartVideoRequest } from '../models/watch-party.model';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * WatchPartyService - Service za Watch Party funkcionalnost (3.15 zahtev)
 * 
 * KOMBINUJE:
 * 1. REST API pozive (HttpClient) - CRUD operacije
 * 2. WebSocket komunikaciju (SockJS + STOMP) - Real-time events
 * 
 * AŽURIRANO: Koristi @stomp/stompjs@7.0.0 (novi API)
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

  constructor(private http: HttpClient) {
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
  // WEBSOCKET - KONEKCIJA (NOVI API)
  // ============================================

  /**
   * Konektuj se na WebSocket.
   * 
   * NOVI API (@stomp/stompjs@7.0.0):
   * - Koristi Client klasu
   * - webSocketFactory umesto direktnog socket-a
   * - activate() umesto connect()
   */
  connect(callback: () => void): void {
    console.log('🔌 Konektovanje na WebSocket...');
    console.log('   URL:', this.wsUrl);

    // Proveri da li je već konektovan
    if (this.isConnected && this.stompClient !== null) {
      console.log('⚠️ Već konektovan!');
      callback();
      return;
    }

    // JWT token iz localStorage
    const token = localStorage.getItem('token');

    // Kreiraj STOMP client (NOVI API)
    this.stompClient = new Client({
      // WebSocket factory - koristi SockJS
      webSocketFactory: () => new SockJS(this.wsUrl) as any,
      
      // Headers sa JWT tokenom
      connectHeaders: {
        Authorization: 'Bearer ' + token
      },
      
      // Debug (opciono)
      debug: (str) => {
        // console.log('STOMP:', str);  // Zakomentiši za manje logova
      },
      
      // Reconnect opcije
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      // Callback za uspešnu konekciju
      onConnect: (frame) => {
        console.log('✅ WebSocket konektovan!');
        console.log('   Frame:', frame);
        
        this.isConnected = true;
        callback();
      },
      
      // Callback za grešku
      onStompError: (frame) => {
        console.error('❌ WebSocket greška:', frame);
        this.isConnected = false;
      },
      
      // Callback za WebSocket grešku
      onWebSocketError: (event) => {
        console.error('❌ WebSocket konekcija greška:', event);
        this.isConnected = false;
      }
    });

    // Aktiviraj konekciju (NOVO!)
    this.stompClient.activate();
  }

  /**
   * Diskonektuj se sa WebSocket-a.
   * 
   * NOVI API:
   * - deactivate() umesto disconnect()
   */
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
  // WEBSOCKET - SUBSCRIBE NA SOBU (NOVI API)
  // ============================================

  /**
   * Subscribe-uj se na WebSocket event-e za određenu sobu.
   * 
   * NOVI API:
   * - subscribe() vraća Subscription objekat
   * - message.body je automatski string
   */
  subscribeToRoom(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan! Pozovi connect() prvo.');
      return;
    }

    console.log('📡 Subscribe na sobu:', roomId);

    this.currentRoomId = roomId;

    // Subscribe na topic
    const topic = `/topic/watch-party/${roomId}`;

    this.stompClient.subscribe(topic, (message) => {
      // Primljena poruka!
      console.log('📨 WebSocket poruka primljena:', message);

      // Parse JSON
      const event: WatchPartyEvent = JSON.parse(message.body);

      console.log('📨 Event:', event);

      // Broadcast svim subscriber-ima!
      this.eventsSubject.next(event);
    });

    console.log('✅ Subscribe-ovan na:', topic);
  }

  unsubscribeFromRoom(): void {
    console.log('📡 Unsubscribe sa sobe:', this.currentRoomId);
    this.currentRoomId = null;
    // STOMP automatski unsubscribe-uje kada se deactivate() pozove
  }

  // ============================================
  // WEBSOCKET - SLANJE PORUKA (NOVI API)
  // ============================================

  /**
   * Kreator pokreće video.
   * 
   * NOVI API:
   * - publish() umesto send()
   * - Prima objekat sa destination i body
   */
  startVideo(roomId: number, postId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      return;
    }

    console.log('▶️ Pokretanje videa...');
    console.log('   Soba ID:', roomId);
    console.log('   Video ID:', postId);

    const destination = `/app/watch-party/${roomId}/start-video`;
    const body: StartVideoRequest = { postId };

    // Publish (NOVO!)
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify(body)
    });

    console.log('✅ Start video poruka poslata!');
  }

  /**
   * Notifikuj ostale članove da si se pridružio.
   */
  notifyJoined(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      return;
    }

    console.log('➕ Notifikacija: Pridružen sobi', roomId);

    const destination = `/app/watch-party/${roomId}/join`;
    
    // Publish (NOVO!)
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify({})
    });

    console.log('✅ Join notifikacija poslata!');
  }

  /**
   * Notifikuj ostale članove da si napustio sobu.
   */
  notifyLeft(roomId: number): void {
    if (!this.isConnected || this.stompClient === null) {
      console.error('❌ WebSocket nije konektovan!');
      return;
    }

    console.log('➖ Notifikacija: Napuštena soba', roomId);

    const destination = `/app/watch-party/${roomId}/leave`;
    
    // Publish (NOVO!)
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify({})
    });

    console.log('✅ Leave notifikacija poslata!');
  }

  // ============================================
  // HELPER METODE
  // ============================================

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');

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