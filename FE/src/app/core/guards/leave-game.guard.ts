import { CanDeactivateFn } from '@angular/router';
import type { GamePage } from '../../features/game/pages/game-page/game-page';

export const leaveGameGuard: CanDeactivateFn<GamePage> = (component) => {
  const gameState = component.gameState;
  const status = gameState.status();

  if (status === 'WAITING' || status === 'SETUP' || status === 'ACTIVE') {
    const confirmed = window.confirm('Si te retirás, perderás la partida. ¿Estás seguro?');
    if (confirmed) {
      gameState.surrender();
    }
    return confirmed;
  }
  return true;
};
