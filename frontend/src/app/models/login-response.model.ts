import { User } from './user.model';

export interface LoginResponse {
  token: string;
  type: string;
  user: User;
}