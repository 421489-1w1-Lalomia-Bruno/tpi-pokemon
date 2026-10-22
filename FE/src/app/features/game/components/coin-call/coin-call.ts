import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

/**
 * Sorteo inicial: el backend elige al azar a un jugador para llamar cara o cruz.
 * Ese jugador ve los botones; el rival ve un mensaje de espera. Tras la elección
 * el backend tira la moneda y la animación corre en `CoinFlipComponent`.
 */
@Component({
  selector: 'app-coin-call',
  imports: [ButtonComponent, LoadingSpinnerComponent],
  templateUrl: './coin-call.html',
  styleUrl: './coin-call.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoinCallComponent {
  private readonly gameState = inject(GameStateService);

  readonly isCaller = this.gameState.isCoinCaller;

  /** Bloquea los botones una vez enviada la elección, esperando la respuesta. */
  readonly submitted = signal(false);

  onCall(call: 'HEADS' | 'TAILS'): void {
    if (this.submitted()) return;
    this.submitted.set(true);
    this.gameState.callCoin(call);
  }
}
