export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  address: string;
  activated: boolean;
  createdAt: Date;
}