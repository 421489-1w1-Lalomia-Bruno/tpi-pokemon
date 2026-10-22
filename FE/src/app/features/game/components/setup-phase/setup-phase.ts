import { Component, ChangeDetectionStrategy, input, inject, signal, computed, effect } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';
import { AuthService } from '../../../../core/services/auth.service';
import { GameCardModel, PokemonCardModel } from '../../../../shared/models/game.model';
import { PokemonInPlayModel } from '../../../../shared/models/game.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-setup-phase',
  imports: [ButtonComponent, LoadingSpinnerComponent],
  templateUrl: './setup-phase.html',
  styleUrl: './setup-phase.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SetupPhaseComponent {
  readonly matchId = input.required<string>();

  private readonly gameState = inject(GameStateService);
  private readonly auth = inject(AuthService);

  readonly status = this.gameState.status;
  readonly opponentId = this.gameState.opponentId;
  readonly playerId = this.gameState.myPlayerId;
  readonly username = computed(() => this.auth.currentUser()?.username ?? this.playerId());

  private readonly _hasClickedReady = signal(false);
  private readonly _hasConfirmed = signal(false);
  private readonly _isProcessing = signal(false);
  readonly selectedCardId = signal<string | null>(null);
  readonly isProcessing = this._isProcessing.asReadonly();

  private _placingCardId: string | null = null;

  readonly isWaitingPhase = computed(() =>
    this.status() === 'WAITING'
  );

  readonly isSetupPhase = computed(() =>
    this.status() === 'SETUP'
  );

  readonly hand = computed(() =>
    this.gameState.myBoard()?.board?.hand ?? []
  );

  readonly myActive = computed(() =>
    this.gameState.myBoard()?.board?.activePokemon ?? null
  );

  readonly myBench = computed(() =>
    this.gameState.myBoard()?.board?.bench ?? []
  );

  readonly placeableCards = computed((): PokemonCardModel[] =>
    this.hand()
      .filter((c: GameCardModel): c is PokemonCardModel =>
        c.type === 'POKEMON' && (c.stage === 'BASIC' || c.stage === 'EX')
      )
  );

  readonly nonPlaceableCards = computed(() =>
    this.hand().filter(c => !this.placeableCards().includes(c as PokemonCardModel))
  );

  readonly benchSlots = computed((): (PokemonInPlayModel | null)[] => {
    const bench = this.myBench();
    const slots: (PokemonInPlayModel | null)[] = [...bench];
    while (slots.length < 5) slots.push(null);
    return slots;
  });

  onReady(): void {
    this._hasClickedReady.set(true);
    this._isProcessing.set(true);
    this.gameState.setReady();
  }

  hasClickedReady = computed(() => this._hasClickedReady());
  readonly hasConfirmed = computed(() => this._hasConfirmed());

  constructor() {
    effect(() => {
      const hand = this.hand();
      if (this._placingCardId !== null && !hand.some(c => c.id === this._placingCardId)) {
        this._placingCardId = null;
        this._isProcessing.set(false);
      }
    });

    effect(() => {
      const err = this.gameState.error();
      if (err) {
        this._isProcessing.set(false);
        this._placingCardId = null;
      }
    });
  }

  onSelectCard(cardId: string): void {
    this._isProcessing.set(false);
    this.selectedCardId.set(this.selectedCardId() === cardId ? null : cardId);
  }

  onPlacePokemon(zone: 'ACTIVE' | 'BENCH'): void {
    const cardId = this.selectedCardId();
    if (!cardId) return;
    this._isProcessing.set(true);
    this._placingCardId = cardId;
    this.gameState.dispatchAction('PLACE_POKEMON', { cardId, zone });
    this.selectedCardId.set(null);
  }

  onConfirmSetup(): void {
    this._hasConfirmed.set(true);
    this._isProcessing.set(true);
    this.gameState.dispatchAction('CONFIRM_SETUP', {});
  }
}