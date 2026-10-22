import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { WebsocketService } from '../../../core/services/websocket.service';
import {
  GameActionPayload,
  GameActionType,
  GameCardModel,
  GameEvent,
  GameEventType,
  GameStateUpdateMessage,
  MatchSnapshotModel,
  PlayerBoardModel,
  PlayerPerspective,
  PokemonInPlayModel,
  TurnContextModel,
  TurnPhase,
} from '../../../shared/models/game.model';
import { MatchStatus } from '../../../shared/models/match.model';

/**
 * Centraliza el estado de la partida en tiempo real.
 *
 * El backend es la única fuente de verdad: cada `MatchSnapshot` recibido por el
 * socket pisa el signal `matchState`. Los componentes del tablero NO guardan
 * estado propio; consumen las propiedades derivadas (`computed`) de este servicio.
 */
@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly websocket = inject(WebsocketService);
  private readonly destroyRef = inject(DestroyRef);

  /** ID del jugador local. Necesario para calcular las perspectivas del tablero. */
  private readonly _myPlayerId = signal<string | null>(null);

  /** Signal principal: el snapshot completo de la partida (fuente de verdad). */
  private readonly _matchState = signal<MatchSnapshotModel | null>(null);

  /** Tick forzado cada 1s para refrescar los computed de temporizador. */
  private readonly _timerTick = signal(0);

  /** Último error emitido por el servidor a través del socket. */
  private readonly _error = signal<string | null>(null);

  /** Última notificación textual recibida del servidor. */
  private readonly _notification = signal<string | null>(null);

  /** Historial acumulado de eventos del engine (máx 100 entries). */
  private readonly _events = signal<GameEvent[]>([]);

  /** Carta revelada por un ataque/efecto del oponente (ej: Astonish). */
  private readonly _revealedCard = signal<GameCardModel | null>(null);

  readonly myPlayerId = this._myPlayerId.asReadonly();
  readonly matchState = this._matchState.asReadonly();
  readonly error = this._error.asReadonly();
  readonly notification = this._notification.asReadonly();
  readonly events = this._events.asReadonly();
  readonly revealedCard = this._revealedCard.asReadonly();

  /** Derivados de nivel partida. */
  readonly matchId = computed<string | null>(() => this._matchState()?.matchId ?? null);
  readonly status = computed<MatchStatus | null>(() => this._matchState()?.status ?? null);
  readonly turnNumber = computed<number>(() => this._matchState()?.turnNumber ?? 0);
  readonly currentPlayerId = computed<string | null>(() => this._matchState()?.currentPlayerId ?? null);
  readonly winnerId = computed<string | null>(() => this._matchState()?.winnerId ?? null);
  readonly firstPlayerId = computed<string | null>(() => this._matchState()?.firstPlayerId ?? null);

  /** Sorteo inicial cara/cruz: estado y resultado. */
  readonly awaitingCoinCall = computed<boolean>(() => this._matchState()?.awaitingCoinCall ?? false);
  readonly coinCallerId = computed<string | null>(() => this._matchState()?.coinCallerId ?? null);
  readonly coinCall = computed<'HEADS' | 'TAILS' | null>(() => this._matchState()?.coinCall ?? null);
  readonly coinFlipResult = computed<'HEADS' | 'TAILS' | null>(() => this._matchState()?.coinFlipResult ?? null);

  /** Verdadero cuando el jugador local es quien debe elegir cara o cruz. */
  readonly isCoinCaller = computed<boolean>(() => {
    const me = this._myPlayerId();
    return me !== null && this.coinCallerId() === me;
  });

  /** Cartas reveladas pendientes de elección (p. ej. Great Ball). */
  readonly revealedCards = computed<GameCardModel[]>(
    () => this.myBoard()?.board?.revealedCards ?? [],
  );

  /** Verdadero cuando el jugador local debe resolver un "reveal". */
  readonly mustResolveReveal = computed<boolean>(() => {
    const board = this.myBoard()?.board;
    return !!board?.hasPendingReveal && this.revealedCards().length > 0;
  });

  /** Verdadero cuando es el turno del jugador local. */
  readonly isMyTurn = computed<boolean>(() => {
    const me = this._myPlayerId();
    return me !== null && this.currentPlayerId() === me;
  });

  /** Verdadero cuando la partida terminó. */
  readonly isFinished = computed<boolean>(() => {
    const s = this.status();
    return s === 'FINISHED' || s === 'ABANDONED';
  });

  /** Verdadero cuando la partida terminó por abandono. */
  readonly isAbandoned = computed<boolean>(() => this.status() === 'ABANDONED');

  readonly abandonmentCountdownMs = computed<number>(() => {
    this._timerTick();
    const snap = this._matchState();
    if (!snap || !snap.abandonmentAtMs) return 0;
    return Math.max(0, snap.abandonmentAtMs - Date.now());
  });

  /**
   * Último evento "importante" que requiere feedback visual inmediato.
   * Incluye: KO, premio tomado, condición aplicada/retirada, trainer jugado.
   */
  readonly latestImportantEvent = computed<GameEvent | null>(() => {
    const important: GameEventType[] = [
      'POKEMON_KNOCKED_OUT',
      'PRIZE_TAKEN',
      'PRIZE_NEEDED',
      'SPECIAL_CONDITION_APPLIED',
      'SPECIAL_CONDITION_REMOVED',
      'TRAINER_PLAYED',
      'COIN_FLIP_RESULT',
      'CARD_DRAWN',
      'MULLIGAN_EXECUTED',
      'ABILITY_USED',
    ];
    const evts = this._events();
    for (let i = evts.length - 1; i >= 0; i--) {
      if (important.includes(evts[i].type as GameEventType)) {
        return evts[i];
      }
    }
    return null;
  });

  /** Último COIN_FLIP_RESULT o MULTI_COIN_FLIP, sin interferir con otros eventos importantes. */
  readonly latestCoinFlipResult = computed<GameEvent | null>(() => {
    const evts = this._events();
    for (let i = evts.length - 1; i >= 0; i--) {
      if (evts[i].type === 'COIN_FLIP_RESULT' || evts[i].type === 'MULTI_COIN_FLIP') {
        return evts[i];
      }
    }
    return null;
  });

  /** Último entrenador jugado por el jugador local. Extraído del historial de eventos. */
  readonly lastTrainerPlayed = computed<string | null>(() => {
    const evts = this._events();
    for (let i = evts.length - 1; i >= 0; i--) {
      if (evts[i].type === 'TRAINER_PLAYED' && evts[i].playerId === this._myPlayerId()) {
        return evts[i].description.replace('Played trainer: ', '');
      }
    }
    return null;
  });

  /** Contexto de turno (fase y flags de acciones ya realizadas). */
  readonly turnContext = computed<TurnContextModel | null>(() => this._matchState()?.turnContext ?? null);

  /** Fase de turno actual (DRAW / ACTIONS / ATTACK / BETWEEN_TURNS). */
  readonly phase = computed<TurnPhase | null>(() => this.turnContext()?.phase ?? null);

  /** ID del oponente, resuelto invirtiendo el slot que ocupa el jugador local. */
  readonly opponentId = computed<string | null>(() => {
    const snap = this._matchState();
    const me = this._myPlayerId();
    if (!snap || !me) return null;
    if (me === snap.player1Id) return snap.player2Id;
    if (me === snap.player2Id) return snap.player1Id;
    return null;
  });

  /**
   * Perspectivas del tablero. Invierten la mesa según el ID del jugador local:
   * `myBoard` siempre es el lado propio y `opponentBoard` el rival, sin importar
   * si el jugador local es player1 o player2.
   */
  readonly myBoard = computed<PlayerPerspective | null>(() => this.buildPerspective(this._myPlayerId()));
  readonly opponentBoard = computed<PlayerPerspective | null>(() => this.buildPerspective(this.opponentId()));

  /** Pokémon activo de cada lado, listos para los componentes del tablero. */
  readonly myActivePokemon = computed<PokemonInPlayModel | null>(() => this.myBoard()?.board?.activePokemon ?? null);
  readonly opponentActivePokemon = computed<PokemonInPlayModel | null>(
    () => this.opponentBoard()?.board?.activePokemon ?? null,
  );

  /**
   * Verdadero cuando el jugador local debe promover un Pokémon de la banca:
   * la partida está en curso, su Pokémon activo fue noqueado (queda null) y
   * todavía tiene Pokémon en banca para subir. Dispara el `PromoteModal` (P6).
   */
  readonly pendingPrizeCount = computed<number>(() => this.myBoard()?.board?.pendingPrizeCount ?? 0);

  readonly pendingMultiCoinTargets = computed<number>(() => this.myBoard()?.board?.pendingMultiCoinTargets ?? 0);

  readonly mustSelectTarget = computed<boolean>(() => this.pendingMultiCoinTargets() > 0);

  readonly mustTakePrize = computed<boolean>(() => this.pendingPrizeCount() > 0);

  readonly mustPromoteActive = computed<boolean>(() => {
    if (this.status() !== 'ACTIVE') return false;
    const board = this.myBoard()?.board;
    if (!board) return false;
    return board.activePokemon === null && board.bench.length > 0;
  });

  /** Tiempo restante del jugador local en ms (actualizado cada segundo). */
  readonly myTimeRemainingMs = computed<number>(() => {
    this._timerTick();
    const snap = this._matchState();
    const me = this._myPlayerId();
    if (!snap || !me) return 0;
    if (snap.turnContext == null) return 0;
    const base = me === snap.player1Id ? snap.player1TimeRemainingMs : snap.player2TimeRemainingMs;
    if (snap.currentPlayerId === me) {
      const elapsed = Date.now() - snap.turnContext.turnStartTimeMs;
      return Math.max(0, base - elapsed);
    }
    return base;
  });

  /** Tiempo restante del oponente en ms. */
  readonly opponentTimeRemainingMs = computed<number>(() => {
    this._timerTick();
    const snap = this._matchState();
    const me = this._myPlayerId();
    if (!snap || !me) return 0;
    if (snap.turnContext == null) return 0;
    const base = me === snap.player1Id ? snap.player2TimeRemainingMs : snap.player1TimeRemainingMs;
    const elapsed = Date.now() - snap.turnContext.turnStartTimeMs;
    if (snap.currentPlayerId === this.opponentId()) {
      return Math.max(0, base - elapsed);
    }
    return base;
  });

  constructor() {
    // Tick forzado cada 1s para refrescar la cuenta regresiva en el UI.
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this._timerTick.update(t => t + 1));

    // Sincroniza el estado local con el stream del socket: cada mensaje del
    // servidor se rutea según su tipo. La suscripción se limpia con el servicio.
    this.websocket.messages$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message: GameStateUpdateMessage) => this.handleMessage(message));
  }

  /** Identifica al jugador local con su ID (lo recibe el lobby al unirse/crear). */
  setMyPlayerId(playerId: string): void {
    this._myPlayerId.set(playerId);
  }

  /** Reemplaza el estado completo con el snapshot recibido del servidor. */
  updateState(snapshot: MatchSnapshotModel): void {
    this._matchState.set(snapshot);
  }

  /** Limpia el estado al salir de la partida. */
  reset(): void {
    this._matchState.set(null);
    this._myPlayerId.set(null);
    this._error.set(null);
    this._notification.set(null);
    this._revealedCard.set(null);
    this._events.set([]);
  }

  // ───────────────────────────────────────────────────────────────────────────
  // Orquestación de acciones: punto único de envío hacia el socket.
  // Los componentes del tablero (P5/P6) invocan estos métodos en vez de armar
  // el `GameActionMessage` a mano, evitando duplicar matchId/timestamp.
  // ───────────────────────────────────────────────────────────────────────────

  /** Envía una acción de juego al servidor completando matchId y timestamp. */
  dispatchAction(type: GameActionType, payload: GameActionPayload = {}): void {
    const id = this.matchId();
    if (!id) {
      console.warn('[GameState] dispatchAction dropped: matchId is null', type, payload);
      return;
    }
    this.websocket.sendAction({
      matchId: id,
      type,
      payload,
      timestamp: new Date().toISOString(),
    });
  }

  /** Marca al jugador local como listo en la fase de setup. */
  setReady(): void {
    this.dispatchAction('READY');
  }

  /** Llama cara o cruz en el sorteo inicial (solo el jugador llamador). */
  callCoin(call: 'HEADS' | 'TAILS'): void {
    this.dispatchAction('CALL_COIN', { call });
  }

  /**
   * Resuelve un "reveal": elige una carta (cardId) para la mano. Sin cardId,
   * descarta la elección y las cartas vuelven al mazo.
   */
  resolveReveal(cardId?: string): void {
    this.dispatchAction('RESOLVE_REVEAL', cardId ? { cardId } : {});
  }

  /** Pasa el turno al oponente. */
  passTurn(): void {
    this.dispatchAction('PASS_TURN');
  }

  /** Abandona la partida. */
  surrender(): void {
    this.dispatchAction('SURRENDER');
  }

  /** Envía la selección de un Pokémon de la banca para adjuntar energía multi-coin. */
  selectTarget(targetId: string): void {
    this.dispatchAction('SELECT_TARGET', { targetId });
  }

  /** Promueve a activo el Pokémon de la banca en `benchIndex` (acción tras KO). */
  promoteActive(benchIndex: number): void {
    const bench = this.myBoard()?.board?.bench;
    if (!bench || benchIndex < 0 || benchIndex >= bench.length) return;
    const cardId = bench[benchIndex].card.id;
    this.dispatchAction('SET_ACTIVE', { cardId });
  }

  /** Descarta el error mostrado (al cerrarlo el usuario). */
  clearError(): void {
    this._error.set(null);
  }

  /** Descarta la notificación mostrada (al cerrarla el usuario). */
  clearNotification(): void {
    this._notification.set(null);
  }

  /**
   * Construye la perspectiva de un jugador a partir del snapshot actual,
   * seleccionando el lado del tablero (`player1Board`/`player2Board`) que le
   * corresponde según su `playerId`.
   */
  private buildPerspective(playerId: string | null): PlayerPerspective | null {
    const snap = this._matchState();
    if (!snap || !playerId) return null;
    return {
      playerId,
      board: this.boardFor(snap, playerId),
      isCurrentTurn: snap.currentPlayerId === playerId,
      isWinner: snap.winnerId === playerId,
    };
  }

  /** Devuelve el lado del tablero correspondiente al jugador, o null. */
  private boardFor(snapshot: MatchSnapshotModel, playerId: string): PlayerBoardModel | null {
    const board = snapshot.board;
    if (!board) return null;
    if (board.player1Board?.playerId === playerId) return board.player1Board;
    if (board.player2Board?.playerId === playerId) return board.player2Board;
    return null;
  }

  /** Reemplaza UUID por "vos"/"tu rival" en descripciones de coin flip. */
  private formatCoinFlipDescription(description: string): string {
    const uuidMatch = description.match(/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\s+goes first/i);
    if (uuidMatch) {
      const uuid = uuidMatch[1];
      const playerName = uuid === this._myPlayerId() ? 'vos' : 'tu rival';
      return description.replace(uuid, playerName);
    }
    return description;
  }

  /** Rutea cada mensaje del socket hacia el signal correspondiente. */
  private handleMessage(message: GameStateUpdateMessage): void {
    console.log('[GameState] message received', message.type, message.state?.status ?? 'no state');
    switch (message.type) {
      case 'STATE_UPDATE':
        if (message.state) {
          console.log('[GameState] STATE_UPDATE status:', message.state.status, 'board:', !!message.state.board);
          this.updateState(message.state);
        } else {
          console.warn('[GameState] STATE_UPDATE without state object');
        }
        break;
      case 'ERROR':
        console.warn('[GameState] ERROR:', message.error);
        this._error.set(message.error ?? 'Error desconocido del servidor.');
        break;
      case 'NOTIFICATION':
        console.log('[GameState] NOTIFICATION:', message.notification);
        this._notification.set(message.notification ?? null);
        break;
      case 'EVENT':
        if (message.event) {
          if (message.event.type === 'COIN_FLIP_RESULT' && message.event.description) {
            message.event.description = this.formatCoinFlipDescription(message.event.description);
          }
          console.log('[GameState] EVENT:', message.event.type, message.event.description);
          this._events.update(evts => [...evts, message.event!].slice(-100));

          if (message.event.type === 'REVEAL_CARDS' && message.event.card) {
            console.log('[GameState] REVEAL_CARDS with card:', message.event.card.name, message.event.card.id);
            this._revealedCard.set(message.event.card);
          }
        }
        if (message.event?.type === 'TRAINER_PLAYED') {
          this._notification.set(message.event.description);
        }
        break;
    }
  }
}
