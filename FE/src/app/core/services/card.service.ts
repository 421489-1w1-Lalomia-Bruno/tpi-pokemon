import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CardModel, CardFilterModel, PageResponse } from '../../shared/models/card.model';
import { API_URL } from '../tokens/api-url.token';

@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${inject(API_URL)}/api/cards`;

  getCards(filter?: CardFilterModel): Observable<PageResponse<CardModel>> {
    let params = new HttpParams();
    if (filter) {
      Object.entries(filter).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params = params.set(key, String(value));
        }
      });
    }
    return this.http.get<PageResponse<CardModel>>(this.apiUrl, { params });
  }

  getCardById(id: string): Observable<CardModel> {
    return this.http.get<CardModel>(`${this.apiUrl}/${id}`);
  }
}
