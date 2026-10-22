import { Component, ChangeDetectionStrategy, computed, inject, signal } from '@angular/core';
import { GameStateService } from '../../services/game-state.service';
import { HandAreaComponent } from '../hand-area/hand-area';
import { GameCardModel, PlayerBoardModel, PokemonCardModel, TrainerCardModel } from '../../../../shared/models/game.model';

function isTrainerPlayable(card: TrainerCardModel, myBoard: PlayerBoardModel, oppBoard: PlayerBoardModel | null,
                           isFirstTurn: boolean): boolean {
  const effect = (card.effect ?? '').toLowerCase();
  const name = card.name.toLowerCase();

  if (effect.includes('heal') || name.includes('potion') || name.includes('cura') || name.includes('superpoción')) {
    const hasDamage = myBoard.bench.some(p => p.damageCounters > 0)
      || (myBoard.activePokemon?.damageCounters ?? 0) > 0;
    return hasDamage;
  }

  if (effect.includes('from your discard pile') || name.includes('revive') || name.includes('revivir')) {
    return myBoard.discardPile.some(c => c.type === 'POKEMON');
  }

  if ((effect.includes('discard') && effect.includes('energy') && (effect.includes('opponent') || effect.includes('defending')))
      || name.includes('flare grunt') || name.includes('hammer')) {
    return (oppBoard?.activePokemon?.attachedEnergy?.length ?? 0) > 0;
  }

  if (effect.includes('evolves from') || name.includes('evosoda')) {
    if (isFirstTurn) return false;
    const boardPokemon = myBoard.activePokemon ? [myBoard.activePokemon, ...myBoard.bench] : [...myBoard.bench];
    return boardPokemon.some(p =>
      myBoard.deck.some(c => c.type === 'POKEMON' && (c as PokemonCardModel).evolvesFrom === p.card.name)
    );
  }

  if (effect.includes('switch') || name.includes('switch') || name.includes('cambia')) {
    return myBoard.bench.length > 0;
  }

  return true;
}

@Component({
  selector: 'app-hand-drawer',
  imports: [HandAreaComponent],
  templateUrl: './hand-drawer.html',
  styleUrl: './hand-drawer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HandDrawerComponent {
  readonly gameState = inject(GameStateService);

  readonly _isOpen = signal(false);

  readonly myHand = computed<GameCardModel[]>(() => this.gameState.myBoard()?.board?.hand ?? []);
  readonly isMyTurn = this.gameState.isMyTurn;
  readonly phase = this.gameState.phase;
  readonly hasAttachedEnergy = computed<boolean>(() => this.gameState.turnContext()?.hasAttachedEnergy ?? false);
  readonly handCount = computed<number>(() => this.myHand().length);

  readonly isFirstTurn = computed<boolean>(() => this.gameState.turnContext()?.firstTurn ?? false);

  readonly playableCardIds = computed<Set<string>>(() => {
    const myBoard = this.gameState.myBoard()?.board;
    const oppBoard = this.gameState.opponentBoard()?.board;
    if (!myBoard) return new Set();

    const ids = new Set<string>();
    for (const card of this.myHand()) {
      if (card.type === 'TRAINER') {
        if (isTrainerPlayable(card, myBoard, oppBoard ?? null, this.isFirstTurn())) {
          ids.add(card.id);
        }
      } else {
        ids.add(card.id);
      }
    }
    return ids;
  });

  toggle(): void {
    this._isOpen.update(v => !v);
  }

  close(): void {
    this._isOpen.set(false);
  }
}
