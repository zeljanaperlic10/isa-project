/**
 * Comment model - reprezentuje komentar (3.6 zahtev)
 * 
 * Mapira se na CommentDTO sa backend-a
 */
export interface Comment {
  id: number;
  text: string;
  username: string;
  createdAt: string;  // ISO 8601 format (npr. "2026-01-28T20:30:00")
}

/**
 * Page response za paginaciju komentara (3.6 zahtev)
 * 
 * Mapira se na Spring Page objekat
 */
export interface CommentPage {
  content: Comment[];           // Komentari na trenutnoj stranici
  totalElements: number;        // Ukupno komentara
  totalPages: number;           // Ukupno stranica
  number: number;               // Trenutna stranica (0-based)
  size: number;                 // Komentara po stranici
  first: boolean;               // Da li je prva stranica
  last: boolean;                // Da li je poslednja stranica
  numberOfElements: number;     // Broj komentara na ovoj stranici
}

/**
 * Request za kreiranje komentara
 */
export interface CreateCommentRequest {
  text: string;
}