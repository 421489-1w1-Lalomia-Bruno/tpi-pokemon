import { DeckModel } from './deck.model';

export const GENDER = {
  MALE: 'MALE',
  FEMALE: 'FEMALE',
  OTHER: 'OTHER',
} as const;

export type Gender = typeof GENDER[keyof typeof GENDER];

export interface UserModel {
  id: string;
  username: string;
  gender: Gender | null;
  createdAt: string;
  wins: number;
  losses: number;
}

/** Perfil completo devuelto por GET /api/users/me (usuario + sus mazos). */
export interface UserProfileModel extends UserModel {
  decks: DeckModel[];
}

export interface LoginRequestModel {
  username: string;
  password: string;
}

export interface RegisterRequestModel {
  username: string;
  password: string;
  gender: Gender | null;
}

/** Respuesta de POST /api/auth/login y /api/auth/register. */
export interface AuthResponseModel {
  token: string;
  user: UserModel;
}
