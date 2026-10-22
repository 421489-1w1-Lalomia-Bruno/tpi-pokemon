import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { leaveGameGuard } from './core/guards/leave-game.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login-page/login-page')
      .then(m => m.LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/pages/register-page/register-page')
      .then(m => m.RegisterPage)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/auth/pages/profile-page/profile-page')
      .then(m => m.ProfilePage)
  },
  {
    path: 'pokedex',
    canActivate: [authGuard],
    loadComponent: () => import('./features/pokedex/pages/pokedex-page/pokedex-page')
      .then(m => m.PokedexPage)
  },
  {
    path: 'deck-builder',
    canActivate: [authGuard],
    loadComponent: () => import('./features/deck-builder/pages/deck-builder-page/deck-builder-page')
      .then(m => m.DeckBuilderPage)
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lobby/pages/lobby-page/lobby-page')
      .then(m => m.LobbyPage)
  },
  {
    path: 'game/:matchId',
    canActivate: [authGuard],
    canDeactivate: [leaveGameGuard],
    loadComponent: () => import('./features/game/pages/game-page/game-page')
      .then(m => m.GamePage)
  },
  // Ruta por defecto: si hay sesión, el authGuard deja pasar a /pokedex;
  // si no, el propio guard redirige a /login.
  { path: '', redirectTo: 'pokedex', pathMatch: 'full' },
  { path: '**', redirectTo: 'pokedex' }
];
