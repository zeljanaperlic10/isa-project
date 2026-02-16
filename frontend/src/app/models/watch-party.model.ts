/**
 * watch-party.model.ts - TypeScript model za Watch Party (3.15 zahtev)
 */

export interface User {
  id: number;
  username: string;
  email: string;
}

export interface Post {
  id: number;
  title: string;
  description?: string;
  videoUrl: string;
  thumbnailUrl?: string;
}

export interface WatchParty {
  id: number;
  name: string;
  creator: User;
  currentPost: Post | null;
  active: boolean;
  createdAt: string;
  members: string[];
  memberCount?: number;
}

export interface CreateRoomRequest {
  name: string;
}

/**
 * StartVideoRequest - Request za pokretanje videa
 * 
 * AŽURIRANO: Dodato username polje!
 */
export interface StartVideoRequest {
  postId: number;
  username: string;  // ← DODATO!
}

export interface WatchPartyEvent {
  type: 'VIDEO_STARTED' | 'USER_JOINED' | 'USER_LEFT' | 'ROOM_CLOSED' | 'ERROR';
  roomId: number;
  [key: string]: any;
}