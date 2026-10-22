import { Component, ChangeDetectionStrategy, inject, effect, OnDestroy, signal } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';
import { GameEvent, GameEventType } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-notification-toast',
  templateUrl: './notification-toast.html',
  styleUrl: './notification-toast.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationToastComponent implements OnDestroy {
  readonly gameState = inject(GameStateService);
  private timers: ReturnType<typeof setTimeout>[] = [];

  private readonly _eventToast = signal<GameEvent | null>(null);
  readonly eventToast = this._eventToast.asReadonly();

  private _lastProcessedIndex = -1;
  private _processing = false;
  private readonly _retrigger = signal(0);

  /** Auto-dismiss error tras 5 segundos. */
  private readonly errorEffect = effect(() => {
    const err = this.gameState.error();
    if (err) {
      this.timers.push(setTimeout(() => this.gameState.clearError(), 5000));
    }
  });

  /** Auto-dismiss notification tras 5 segundos. */
  private readonly notificationEffect = effect(() => {
    const notif = this.gameState.notification();
    if (notif) {
      this.timers.push(setTimeout(() => this.gameState.clearNotification(), 5000));
    }
  });

  /** Cola secuencial de eventos importantes (se muestran uno a uno cada 4s). */
  private readonly queueEffect = effect(() => {
    this._retrigger();
    if (this._processing) return;

    const evts = this.gameState.events();
    const important: GameEventType[] = [
      'POKEMON_KNOCKED_OUT',
      'PRIZE_TAKEN',
      'SPECIAL_CONDITION_APPLIED',
      'SPECIAL_CONDITION_REMOVED',
      'TRAINER_PLAYED',
      'COIN_FLIP_RESULT',
      'MULTI_COIN_FLIP',
      'CARD_DRAWN',
      'ABILITY_USED',
    ];

    const isImportant = (evt: GameEvent): boolean =>
      important.includes(evt.type as GameEventType) ||
      (evt.type === 'ATTACK_EXECUTED' && evt.value === 0);

    for (let i = this._lastProcessedIndex + 1; i < evts.length; i++) {
      if (isImportant(evts[i])) {
        this._lastProcessedIndex = i;
        this._processing = true;
        this._eventToast.set(evts[i]);
        this.timers.push(setTimeout(() => {
          this._eventToast.set(null);
          this._processing = false;
          this._retrigger.update(v => v + 1);
        }, 4000));
        break;
      }
    }
  });

  clearEventToast(): void {
    this._eventToast.set(null);
  }

  eventIcon(type: string): string {
    switch (type) {
      case 'POKEMON_KNOCKED_OUT': return 'ko';
      case 'PRIZE_TAKEN': return 'prize';
      case 'SPECIAL_CONDITION_APPLIED': return 'condition';
      case 'SPECIAL_CONDITION_REMOVED': return 'condition-rm';
      case 'COIN_FLIP_RESULT': return 'coin';
      case 'MULTI_COIN_FLIP': return 'coin';
      case 'CARD_DRAWN': return 'card';
      case 'ABILITY_USED': return 'ability';
      case 'ATTACK_EXECUTED': return 'attack';
      case 'MULLIGAN_EXECUTED': return 'mulligan';
      default: return 'info';
    }
  }

  actorLabel(event: GameEvent): string {
    return event.playerId === this.gameState.myPlayerId() ? 'Tú' : 'Rival';
  }

  ngOnDestroy(): void {
    this.timers.forEach(clearTimeout);
  }
}
