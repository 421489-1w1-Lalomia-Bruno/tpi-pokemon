import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      console.error('HTTP Error:', error.status, error.message);

      // Redirigir al login en 401, excepto si estamos en una partida activa.
      // El lobby page puede tener otra pestaña; no debe interrumpir el juego.
      if (error.status === 401 && !req.url.includes('/api/auth/')
          && !router.url.startsWith('/game/')) {
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
