import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DeckModel, CreateDeckModel, DeckValidationResultModel } from '../../../shared/models/deck.model';
import { API_URL } from '../../../core/tokens/api-url.token';

@Injectable({ providedIn: 'root' })
export class DeckService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_URL)}/api/decks`;

  private playerHeaders(playerId: string): HttpHeaders {
    return new HttpHeaders({ 'X-Player-Id': playerId });
  }

  getDecks(playerId: string): Observable<DeckModel[]> {
    return this.http.get<DeckModel[]>(this.apiUrl, { headers: this.playerHeaders(playerId) });
  }

  getDeckById(id: string): Observable<DeckModel> {
    return this.http.get<DeckModel>(`${this.apiUrl}/${id}`);
  }

  createDeck(deck: CreateDeckModel, playerId: string): Observable<DeckModel> {
    return this.http.post<DeckModel>(this.apiUrl, deck, { headers: this.playerHeaders(playerId) });
  }

  updateDeck(id: string, deck: CreateDeckModel, playerId: string): Observable<DeckModel> {
    return this.http.put<DeckModel>(`${this.apiUrl}/${id}`, deck, { headers: this.playerHeaders(playerId) });
  }

  deleteDeck(id: string, playerId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.playerHeaders(playerId) });
  }

  validateDeck(id: string): Observable<DeckValidationResultModel> {
    return this.http.post<DeckValidationResultModel>(`${this.apiUrl}/${id}/validate`, null);
  }
}
