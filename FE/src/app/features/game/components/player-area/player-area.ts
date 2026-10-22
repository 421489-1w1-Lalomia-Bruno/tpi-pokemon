import { Component, ChangeDetectionStrategy, computed, inject, input } from '@angular/core';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { GameStateService } from '../../services/game-state.service';
import { BenchAreaComponent } from '../bench-area/bench-area';
import { ActivePokemonComponent } from '../active-pokemon/active-pokemon';
import { DeckPileComponent } from '../deck-pile/deck-pile';
import { DiscardPileComponent } from '../discard-pile/discard-pile';
import { PrizeCardsComponent } from '../prize-cards/prize-cards';
import { GameLogComponent } from '../../../../shared/components/game-log/game-log';
import {
  GameCardModel,
  GameActionType,
  GameActionPayload,
  PokemonInPlayModel,
} from '../../../../shared/models/game.model';

@Component({
  selector: 'app-player-area',
  imports: [
    BenchAreaComponent,
    ActivePokemonComponent,
    DeckPileComponent,
    DiscardPileComponent,
    PrizeCardsComponent,
    GameLogComponent,
  ],
  templateUrl: './player-area.html',
  styleUrl: './player-area.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerAreaComponent {
  readonly gameState = inject(GameStateService);

  readonly evolvablePokemonIds = input<Set<string>>(new Set());

  readonly myBoard = this.gameState.myBoard;
  readonly isMyTurn = this.gameState.isMyTurn;
  readonly turnContext = this.gameState.turnContext;
  readonly phase = this.gameState.phase;

  readonly myBench = computed<PokemonInPlayModel[]>(() => this.myBoard()?.board?.bench ?? []);
  readonly myActive = computed<PokemonInPlayModel | null>(() => this.myBoard()?.board?.activePokemon ?? null);
  readonly myDeck = computed<GameCardModel[]>(() => this.myBoard()?.board?.deck ?? []);
  readonly myDiscard = computed<GameCardModel[]>(() => this.myBoard()?.board?.discardPile ?? []);
  readonly myPrizes = computed<GameCardModel[]>(() => this.myBoard()?.board?.prizeCards ?? []);
  readonly events = this.gameState.events;
  readonly myPlayerId = this.gameState.myPlayerId;

  onCardDropped(event: CdkDragDrop<PokemonInPlayModel | null> | CdkDragDrop<GameCardModel[]>): void {
    const card = event.item.data as GameCardModel;
    const targetZone = event.container.id;

    if (targetZone.startsWith('benchSlot')) {
      const targetPokemon = event.container.data as PokemonInPlayModel | null;

      if (card.type === 'POKEMON' && !targetPokemon) {
        this.dispatch('PLACE_POKEMON', { cardId: card.id, zone: 'BENCH' });
      } else if (card.type === 'ENERGY' && targetPokemon) {
        this.dispatch('ATTACH_ENERGY', { cardId: card.id, targetId: targetPokemon.card.id });
      } else if (card.type === 'TRAINER') {
        const payload: GameActionPayload = { cardId: card.id };
        if (targetPokemon) {
          payload['targetId'] = targetPokemon.card.id;
        }
        this.dispatch('PLAY_TRAINER', payload);
      }
    } else if (targetZone === 'activeZone') {
      const activePokemon = this.myActive();

      if (card.type === 'ENERGY' && activePokemon) {
        this.dispatch('ATTACH_ENERGY', { cardId: card.id, targetId: activePokemon.card.id });
      } else if (card.type === 'TRAINER') {
        const payload: GameActionPayload = { cardId: card.id };
        if (activePokemon) {
          payload['targetId'] = activePokemon.card.id;
        }
        this.dispatch('PLAY_TRAINER', payload);
      }
    }

    this.focusGameBoard();
  }

  private focusGameBoard(): void {
    const el = document.querySelector<HTMLElement>('.game-board');
    el?.focus({ preventScroll: true });
  }

  private dispatch(type: GameActionType, payload: GameActionPayload): void {
    this.gameState.dispatchAction(type, payload);
  }
}
