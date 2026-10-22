import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  AuthResponseModel,
  LoginRequestModel,
  RegisterRequestModel,
  UserModel,
  UserProfileModel,
} from '../../shared/models/user.model';
import { API_URL } from '../tokens/api-url.token';

const TOKEN_KEY = 'auth-token';
const USER_KEY = 'auth-user';
/** Clave legacy usada por deck/lobby/websocket para identificar al jugador. */
const PLAYER_ID_KEY = 'playerId';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  private readonly currentUserSignal = signal<UserModel | null>(this.readStoredUser());

  /** Usuario autenticado (o null si es anónimo). Reactivo para navbar/perfil. */
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  login(credentials: LoginRequestModel): Observable<AuthResponseModel> {
    return this.http
      .post<AuthResponseModel>(`${this.apiUrl}/api/auth/login`, credentials)
      .pipe(tap((res: AuthResponseModel) => this.persistSession(res)));
  }

  register(data: RegisterRequestModel): Observable<AuthResponseModel> {
    return this.http
      .post<AuthResponseModel>(`${this.apiUrl}/api/auth/register`, data)
      .pipe(tap((res: AuthResponseModel) => this.persistSession(res)));
  }

  /** Perfil del usuario autenticado junto con su lista de mazos. */
  getProfile(): Observable<UserProfileModel> {
    return this.http.get<UserProfileModel>(`${this.apiUrl}/api/users/me`);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(PLAYER_ID_KEY);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  private persistSession(res: AuthResponseModel): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(USER_KEY, JSON.stringify(res.user));
    // Mantener compatibilidad con el código existente (deck/lobby/websocket)
    // que identifica al jugador mediante la clave 'playerId'.
    localStorage.setItem(PLAYER_ID_KEY, res.user.id);
    this.currentUserSignal.set(res.user);
  }

  private readStoredUser(): UserModel | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserModel;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }
}
