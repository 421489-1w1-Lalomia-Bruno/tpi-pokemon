import { InjectionToken, inject } from '@angular/core';
import { API_URL } from './api-url.token';

export const WS_URL = new InjectionToken<string>('WS_URL', {
  factory: () => {
    const apiUrl = inject(API_URL);
    return `${apiUrl.replace(/\/$/, '')}/ws/game`;
  },
  providedIn: 'root',
});
