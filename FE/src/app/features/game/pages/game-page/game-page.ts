import { Component, ChangeDetectionStrategy, input, inject, OnInit, OnDestroy, DestroyRef, signal, effect, computed, untracked } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';
import { WebsocketService } from '../../../../core/services/websocket.service';
import { AuthService } from '../../../../core/services/auth.service';
import { SetupPhaseComponent } from '../../components/setup-phase/setup-phase';
import { GameBoardComponent } from '../../components/game-board/game-board';
import { GameOverComponent } from '../../components/game-over/game-over';
import { NotificationToastComponent } from '../../components/notification-toast/notification-toast';
import { CoinFlipComponent } from '../../components/coin-flip/coin-flip';
import { CoinCallComponent } from '../../components/coin-call/coin-call';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-game-page',
  imports: [SetupPhaseComponent, GameBoardComponent, GameOverComponent, NotificationToastComponent, CoinFlipComponent, CoinCallComponent, LoadingSpinnerComponent],
  templateUrl: './game-page.html',
  styleUrl: './game-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(window:beforeunload)': 'onBeforeUnload($event)',
  },
})
export class GamePage implements OnInit, OnDestroy {
  readonly matchId = input.required<string>();
  readonly gameState = inject(GameStateService);
  private readonly ws = inject(WebsocketService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly showingCoinFlip = signal(false);
  private _coinFlipShown = false;
  private _connectInterval?: ReturnType<typeof setInterval>;

  /** Animación de moneda en partida (Roller Skates, ataques con moneda, etc.). */
  readonly inGameCoin = signal<{ result: 'heads' | 'tails'; message: string } | null>(null);
  private readonly _seenCoinTs = new Set<string>();
  private readonly _coinFlipQueue: { result: 'heads' | 'tails'; message: string }[] = [];

  /** Resultado real de la moneda en minúsculas para la animación de `CoinFlip`. */
  readonly coinFlipResultLower = computed<'heads' | 'tails' | null>(() => {
    const r = this.gameState.coinFlipResult();
    return r === 'HEADS' ? 'heads' : r === 'TAILS' ? 'tails' : null;
  });

  /** Mensaje del resultado del sorteo, mostrado durante la animación. */
  readonly coinMessage = computed<string | null>(() => {
    const result = this.gameState.coinFlipResult();
    if (!result) return null;
    const cara = result === 'HEADS' ? 'Cara' : 'Cruz';
    const meFirst = this.gameState.firstPlayerId() === this.gameState.myPlayerId();
    return `Salió ${cara} · ${meFirst ? '¡Empezás vos!' : 'Empieza tu rival'}`;
  });

  constructor() {
    effect(() => {
      if (this._coinFlipShown) return;
      if (this.gameState.status() === 'ACTIVE' && this.gameState.firstPlayerId() !== null) {
        this.showingCoinFlip.set(true);
        this._coinFlipShown = true;
        // Pre-mark any coin flip events already in the queue so System 2 doesn't replay them
        const evts = untracked(() => this.gameState.events());
        for (const ev of evts) {
          if (ev.timestamp) this._seenCoinTs.add(ev.timestamp);
        }
      }
    });

    // Monedas en partida: eventos COIN_FLIP_RESULT / MULTI_COIN_FLIP se
    // encolan y muestran secuencialmente (soporta ataques multi-moneda).
    effect(() => {
      const evts = this.gameState.events();
      if (!this._coinFlipShown || this.showingCoinFlip()) return;
      for (const ev of evts) {
        if (!ev.timestamp) continue;
        if (this._seenCoinTs.has(ev.timestamp)) continue;
        if (ev.type !== 'COIN_FLIP_RESULT' && ev.type !== 'MULTI_COIN_FLIP') continue;
        this._seenCoinTs.add(ev.timestamp);
        const heads = (ev.value ?? 0) > 0;
        this._coinFlipQueue.push({ result: heads ? 'heads' : 'tails', message: ev.description });
      }
      if (this.inGameCoin() === null) {
        this._processNextCoinFlip();
      }
    });
  }

  private _processNextCoinFlip(): void {
    if (this._coinFlipQueue.length === 0) return;
    const item = this._coinFlipQueue.shift()!;
    this.inGameCoin.set(item);
  }

  onInGameCoinFinished(): void {
    this.inGameCoin.set(null);
    this._processNextCoinFlip();
  }

  ngOnInit(): void {
    const playerId = this.auth.currentUser()?.id ?? localStorage.getItem('playerId') ?? 'anonymous';
    const token = this.auth.getToken() ?? '';

    this.gameState.setMyPlayerId(playerId);
    this.ws.connect(token);

    let attempts = 0;
    const maxAttempts = 30;
    this._connectInterval = setInterval(() => {
      if (this.ws.connected()) {
        if (this.ws.clientActive()) {
          clearInterval(this._connectInterval!);
          this._connectInterval = undefined;
          this.ws.subscribeToMatch(this.matchId());
        } else if (++attempts >= maxAttempts) {
          clearInterval(this._connectInterval!);
          this._connectInterval = undefined;
          console.error('[GamePage] WebSocket connected but STOMP client not active after', maxAttempts, 'attempts');
        }
      }
    }, 100);
  }

  ngOnDestroy(): void {
    if (this._connectInterval) {
      clearInterval(this._connectInterval);
      this._connectInterval = undefined;
    }

    const status = this.gameState.status();
    const shouldSurrender = status === 'WAITING' || status === 'SETUP' || status === 'ACTIVE';
    if (shouldSurrender) {
      this.gameState.surrender();
    }

    // Defer disconnect to let the surrender STOMP message be delivered first
    setTimeout(() => {
      this.ws.unsubscribeFromMatch(this.matchId());
      this.ws.disconnect();
      this.gameState.reset();
    }, 200);
  }

  onBeforeUnload(event: BeforeUnloadEvent): void {
    const status = this.gameState.status();
    if (status === 'WAITING' || status === 'SETUP' || status === 'ACTIVE') {
      event.preventDefault();
      event.returnValue = '';
    }
  }

  onCoinFlipFinished(): void {
    this.showingCoinFlip.set(false);
  }
}
