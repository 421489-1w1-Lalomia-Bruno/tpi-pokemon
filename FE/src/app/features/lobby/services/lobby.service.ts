import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MatchModel, CreateMatchModel } from '../../../shared/models/match.model';
import { API_URL } from '../../../core/tokens/api-url.token';

@Injectable({ providedIn: 'root' })
export class LobbyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_URL)}/api/matches`;

  private readonly _matches = signal<MatchModel[]>([]);
  private readonly _myMatches = signal<MatchModel[]>([]);
  private readonly _loading = signal(false);
  private readonly _myLoading = signal(false);

  readonly matches = this._matches.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly myMatches = this._myMatches.asReadonly();
  readonly myLoading = this._myLoading.asReadonly();

  readonly availableMatches = computed(() => {
    const playerId = this.getPlayerId();
    return this._matches().filter(m => m.status === 'WAITING' && m.player1Id !== playerId);
  });

  private getPlayerId(): string {
    return localStorage.getItem('playerId') ?? 'anonymous';
  }

  private playerHeaders(): HttpHeaders {
    return new HttpHeaders({ 'X-Player-Id': this.getPlayerId() });
  }

  getMatches(): void {
    this._loading.set(true);
    this.http.get<MatchModel[]>(this.apiUrl, { headers: this.playerHeaders() }).subscribe({
      next: (matches: MatchModel[]) => {
        this._matches.set(matches);
        this._loading.set(false);
      },
      error: () => {
        this._loading.set(false);
      }
    });
  }

  getMyMatches(): void {
    this._myLoading.set(true);
    this.http.get<MatchModel[]>(`${this.apiUrl}/me`, { headers: this.playerHeaders() }).subscribe({
      next: (matches: MatchModel[]) => {
        this._myMatches.set(matches);
        this._myLoading.set(false);
      },
      error: () => {
        this._myLoading.set(false);
      }
    });
  }

  joinMatch(matchId: string, deckId: string): Observable<MatchModel> {
    return this.http.post<MatchModel>(
      `${this.apiUrl}/${matchId}/join`,
      { deckId },
      { headers: this.playerHeaders() }
    );
  }

  createMatch(deckId: string): Observable<MatchModel> {
    const body: CreateMatchModel = { player1DeckId: deckId };
    return this.http.post<MatchModel>(this.apiUrl, body, { headers: this.playerHeaders() });
  }

  cancelMatch(matchId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${matchId}`, { headers: this.playerHeaders() });
  }

  leaveMatch(matchId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${matchId}/leave`, null, { headers: this.playerHeaders() });
  }

  getMatch(matchId: string): Observable<MatchModel> {
    return this.http.get<MatchModel>(`${this.apiUrl}/${matchId}`, { headers: this.playerHeaders() });
  }
}