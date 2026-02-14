/**
 * watch-party.model.ts - TypeScript model za Watch Party (3.15 zahtev)
 * 
 * NAMENA:
 * - Definicija strukture podataka sa Backend-a
 * - Type safety u TypeScript-u
 * - IntelliSense u VS Code-u
 * 
 * RAZLIKA: Interface vs Class
 * 
 * Interface (koristimo ovo):
 * - Samo definicija strukture
 * - Ne postoji u runtime-u (briše se nakon kompilacije)
 * - Koristi se za type checking
 * 
 * Class:
 * - Postoji u runtime-u
 * - Može imati metode
 * - Može se instancirati (new MyClass())
 * 
 * Za podatke sa Backend-a uvek koristimo Interface!
 */

/**
 * User - Korisnik (kreator sobe)
 * 
 * NAPOMENA:
 * - Ovo je skraćena verzija User objekta
 * - Backend vraća samo osnovne podatke
 */
export interface User {
  id: number;
  username: string;
  email: string;
}

/**
 * Post - Video (trenutni video u sobi)
 * 
 * NAPOMENA:
 * - Ovo je skraćena verzija Post objekta
 * - Backend vraća samo osnovne podatke
 */
export interface Post {
  id: number;
  title: string;
  description?: string;
  videoUrl: string;
  thumbnailUrl?: string;
}

/**
 * WatchParty - Watch Party soba
 * 
 * BACKEND JSON (primer):
 * {
 *   "id": 123,
 *   "name": "Movie Night",
 *   "creator": {
 *     "id": 5,
 *     "username": "petar",
 *     "email": "petar@example.com"
 *   },
 *   "currentPost": {
 *     "id": 10,
 *     "title": "My Video",
 *     "videoUrl": "/api/videos/abc.mp4"
 *   },
 *   "active": true,
 *   "createdAt": "2026-02-02T12:00:00",
 *   "members": ["petar", "stefan", "ana"],
 *   "memberCount": 3
 * }
 * 
 * TYPESCRIPT TYPE CHECKING:
 * const party: WatchParty = response;  // TypeScript validira strukturu!
 * party.id → OK (number)
 * party.name → OK (string)
 * party.xyz → GREŠKA! (ne postoji u interface-u)
 */
export interface WatchParty {
  /**
   * ID sobe
   */
  id: number;

  /**
   * Naziv sobe
   */
  name: string;

  /**
   * Kreator sobe (vlasnik)
   */
  creator: User;

  /**
   * Trenutni video koji se gleda
   * null ako nije pokrenut video
   */
  currentPost: Post | null;

  /**
   * Da li je soba aktivna
   * true - soba je otvorena
   * false - soba je zatvorena
   */
  active: boolean;

  /**
   * Vreme kreiranja sobe
   * Format: ISO 8601 string (npr. "2026-02-02T12:00:00")
   */
  createdAt: string;

  /**
   * Lista usernames članova sobe
   * Primer: ["petar", "stefan", "ana"]
   */
  members: string[];

  /**
   * Broj članova u sobi (computed sa Backend-a)
   */
  memberCount?: number;  // Opciono (?)
}

/**
 * CreateRoomRequest - Request za kreiranje sobe
 * 
 * KORISTI SE:
 * http.post('/api/watch-party/create', { name: 'Movie Night' })
 */
export interface CreateRoomRequest {
  name: string;
}

/**
 * WatchPartyEvent - WebSocket event-i
 * 
 * TIPOVI EVENT-A:
 * - VIDEO_STARTED - Kreator je pokrenuo video
 * - USER_JOINED - Novi član se pridružio
 * - USER_LEFT - Član je napustio sobu
 * - ROOM_CLOSED - Soba je zatvorena
 * - ERROR - Greška
 * 
 * PRIMER EVENT-A:
 * {
 *   "type": "VIDEO_STARTED",
 *   "roomId": 123,
 *   "postId": 10,
 *   "postTitle": "My Video",
 *   "videoUrl": "/api/videos/abc.mp4",
 *   "startedBy": "petar",
 *   "timestamp": "2026-02-02T12:00:00"
 * }
 */
export interface WatchPartyEvent {
  /**
   * Tip event-a
   */
  type: 'VIDEO_STARTED' | 'USER_JOINED' | 'USER_LEFT' | 'ROOM_CLOSED' | 'ERROR';

  /**
   * ID sobe (uvek prisutno)
   */
  roomId: number;

  /**
   * Dodatna polja (zavise od tipa event-a)
   * 
   * VIDEO_STARTED:
   * - postId: number
   * - postTitle: string
   * - videoUrl: string
   * - startedBy: string
   * 
   * USER_JOINED / USER_LEFT:
   * - username: string
   * - memberCount: number
   * 
   * ROOM_CLOSED:
   * - closedBy: string
   * 
   * ERROR:
   * - message: string
   */
  [key: string]: any;  // Dinamička polja
}

/**
 * StartVideoRequest - Request za pokretanje videa
 * 
 * KORISTI SE:
 * stompClient.send('/app/watch-party/123/start-video', {}, JSON.stringify({ postId: 10 }))
 */
export interface StartVideoRequest {
  postId: number;
}