import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { GameStateService } from '../../services/game-state.service';
import { ButtonComponent } from '../../../../shared/ui/button/button';

@Component({
  selector: 'app-game-over',
  imports: [RouterLink, ButtonComponent],
  templateUrl: './game-over.html',
  styleUrl: './game-over.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class GameOverComponent {
  readonly gameState = inject(GameStateService);

  readonly isDraw = computed(() => this.gameState.winnerId() === null && this.gameState.myPlayerId() !== null);
  readonly isWinner = computed(() => this.gameState.winnerId() === this.gameState.myPlayerId());
  readonly isAbandoned = computed(() => this.gameState.isAbandoned());
  readonly prizeCardsTaken = computed(() => this.gameState.myBoard()?.board?.prizeCardsTaken ?? 0);
}
