export interface Post {
  id: number;                    // ID posta
  user: UserBasic;               // Ko je kreirao (samo id i username)
  title: string;                 // Naslov
  description?: string;          // Opis (opciono - ? znači optional)
  videoUrl: string;              // URL ka videu
  thumbnailUrl: string;          // URL ka thumbnail slici
  tags: string[];                // Niz tagova ["programiranje", "python"]
  
  // Geolokacija (opciono)
  latitude?: number;
  longitude?: number;
  locationName?: string;
  
  // Statistika
  likesCount: number;
  commentsCount: number;
  viewsCount: number;
  
  // NOVO - da li je trenutni korisnik lajkovao ❤️
  isLikedByCurrentUser: boolean;
  
  // Vreme
  createdAt: string;             // ISO string format
}

export interface UserBasic {
  id: number;
  username: string;
}