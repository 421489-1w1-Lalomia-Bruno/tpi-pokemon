import { Component, ChangeDetectionStrategy, input, output, inject, signal, effect, OnDestroy } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';

@Component({
  selector: 'app-coin-flip',
  templateUrl: './coin-flip.html',
  styleUrl: './coin-flip.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CoinFlipComponent implements OnDestroy {
  readonly winnerId = input<string | null>();
  readonly finished = output<void>();
  readonly flipResult = input<'heads' | 'tails' | null>(null);
  readonly customMessage = input<string | null>(null);
  readonly durationMs = input<number>(4000);
  private readonly gameState = inject(GameStateService);
  private flipTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly result = signal<'heads' | 'tails' | null>(null);
  readonly message = signal('');
  readonly visible = signal(true);
  readonly isQuick = signal(false);

  constructor() {
    effect(() => {
      const directResult = this.flipResult();
      const winner = this.winnerId();
      const customMsg = this.customMessage();
      const duration = this.durationMs();

      this.isQuick.set(duration < 3000);

      if (directResult !== null) {
        this.result.set(directResult);
        this.message.set(customMsg ?? '');
      } else if (winner) {
        const isMe = this.gameState.myPlayerId() === winner;
        this.result.set(isMe ? 'heads' : 'tails');
        this.message.set(isMe ? '¡Vos empezás!' : '¡Tu rival empieza!');
      }

      this.flipTimeout = setTimeout(() => {
        this.visible.set(false);
        this.finished.emit();
      }, duration);
    });
  }

  ngOnDestroy(): void {
    if (this.flipTimeout !== null) {
      clearTimeout(this.flipTimeout);
      this.flipTimeout = null;
    }
  }
}
