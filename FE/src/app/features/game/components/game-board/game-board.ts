import { Component, ChangeDetectionStrategy, computed, inject, signal, effect } from '@angular/core';
import { CdkDropListGroup } from '@angular/cdk/drag-drop';
import { GameStateService } from '../../services/game-state.service';
import { OpponentAreaComponent } from '../opponent-area/opponent-area';
import { PlayerAreaComponent } from '../player-area/player-area';
import { ActionPanelComponent } from '../action-panel/action-panel';
import { PromoteModalComponent } from '../promote-modal/promote-modal';
import { AttackSelectorComponent } from '../attack-selector/attack-selector';
import { EvolutionSelectorComponent, ValidEvolutionPair } from '../evolution-selector/evolution-selector';
import { EnergyAttachmentComponent } from '../energy-attachment/energy-attachment';
import { TrainerSelectorComponent } from '../trainer-selector/trainer-selector';
import { RetreatModalComponent } from '../retreat-modal/retreat-modal';
import { TargetSelectorModalComponent } from '../target-selector-modal/target-selector-modal';
import { RevealModalComponent } from '../reveal-modal/reveal-modal';
import { EnergyTransferModalComponent } from '../energy-transfer-modal/energy-transfer-modal';
import { StadiumAreaComponent } from '../stadium-area/stadium-area';
import { CardComponent } from '../../../../shared/components/card/card';
import { CardZoomComponent } from '../../../../shared/components/card-zoom/card-zoom';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';
import { mapGameCardToCardModel } from '../../../../shared/utils/card-mapper';
import { CardModel } from '../../../../shared/models/card.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { CoinFlipComponent } from '../coin-flip/coin-flip';
import { PrizeCardsComponent } from '../prize-cards/prize-cards';
import { HandDrawerComponent } from '../hand-drawer/hand-drawer';
import {
  Ability,
  Attack,
  EnergyCardModel,
  GameCardModel,
  PokemonCardModel,
  PokemonInPlayModel,
  TrainerCardModel,
} from '../../../../shared/models/game.model';

@Component({
  selector: 'app-game-board',
  imports: [
    CdkDropListGroup,
    OpponentAreaComponent,
    PlayerAreaComponent,
    ActionPanelComponent,
    PromoteModalComponent,
    AttackSelectorComponent,
    EvolutionSelectorComponent,
    EnergyAttachmentComponent,
    TrainerSelectorComponent,
    RetreatModalComponent,
    TargetSelectorModalComponent,
    RevealModalComponent,
    EnergyTransferModalComponent,
    StadiumAreaComponent,
    CardComponent,
    CardZoomComponent,
    ButtonComponent,
    CoinFlipComponent,
    PrizeCardsComponent,
    HandDrawerComponent,
  ],
  templateUrl: './game-board.html',
  styleUrl: './game-board.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameBoardComponent {
  readonly gameState = inject(GameStateService);
  readonly cardZoom = inject(CardZoomService);

  readonly opponentBoard = computed(() =>
    this.gameState.opponentBoard()?.board ?? null
  );

  private readonly _showAttackSelector = signal(false);
  private readonly _showEvolutionSelector = signal(false);
  private readonly _showEnergyAttachment = signal(false);
  private readonly _showSurrenderConfirm = signal(false);
  private readonly _showTrainerSelector = signal(false);
  private readonly _showRetreatSelector = signal(false);
  private readonly _showQuickCoinFlip = signal(false);
  private readonly _quickFlipResult = signal<'heads' | 'tails' | null>(null);
  private readonly _quickFlipMessage = signal('');
  private readonly _seenCoinTs = new Set<string>();
  readonly _showEnergyTransfer = signal(false);

  readonly showAttackSelector = this._showAttackSelector.asReadonly();
  readonly showEvolutionSelector = this._showEvolutionSelector.asReadonly();
  readonly showEnergyAttachment = this._showEnergyAttachment.asReadonly();
  readonly showSurrenderConfirm = this._showSurrenderConfirm.asReadonly();
  readonly showTrainerSelector = this._showTrainerSelector.asReadonly();
  readonly showRetreatSelector = this._showRetreatSelector.asReadonly();
  readonly showQuickCoinFlip = this._showQuickCoinFlip.asReadonly();
  readonly quickFlipResult = this._quickFlipResult.asReadonly();
  readonly quickFlipMessage = this._quickFlipMessage.asReadonly();
  readonly showEnergyTransfer = this._showEnergyTransfer.asReadonly();

  readonly showTargetSelector = computed<boolean>(() => this.gameState.mustSelectTarget());

  readonly activePokemonAttacks = computed<Attack[]>(() => {
    const active = this.gameState.myActivePokemon();
    return active?.card?.attacks ?? [];
  });

  readonly activePokemonAbilities = computed<Ability[]>(() => {
    const active = this.gameState.myActivePokemon();
    return (active?.card?.abilities ?? []).filter(a => a.type === 'POKE_POWER');
  });

  readonly canUseAbility = computed<boolean>(() => {
    if (!this.gameState.isMyTurn()) return false;
    if (this.gameState.phase() !== 'ACTIONS') return false;
    const abilities = this.activePokemonAbilities();
    if (abilities.length === 0) return false;
    const isMultiUse = abilities.some(a =>
      /as often as you like|veces que quieras|any number of times/i.test(a.effectText ?? '')
    );
    if (!isMultiUse && this.gameState.turnContext()?.hasUsedAbility) return false;
    // Si la habilidad requiere que ESTE Pokémon esté Confundido como precondición
    // (detecta "if [this Pokémon] is Confused" / "si está Confundido", no menciones genéricas de confusion)
    const requiresConfused = abilities.some(a =>
      /if\s+(?:this\s+)?(?:pok[eé]mon\s+)?is\s+confus|si\s+est[aá]\s+confundid/i
        .test(a.effectText ?? '')
    );
    if (requiresConfused) {
      const active = this.gameState.myActivePokemon();
      if (!active?.specialConditions?.includes('CONFUSED')) return false;
    }
    return true;
  });

  readonly validEvolutionPairs = computed<ValidEvolutionPair[]>(() => {
    const board = this.gameState.myBoard()?.board;
    if (!board) return [];
    const hand = board.hand ?? [];
    const bench = board.bench ?? [];
    const active = board.activePokemon;
    const boardPokemon = active ? [active, ...bench] : bench;

    const pairs: ValidEvolutionPair[] = [];
    for (const target of boardPokemon) {
      for (const card of hand) {
        if (card.type === 'POKEMON' && (card as PokemonCardModel).evolvesFrom === target.card.name) {
          pairs.push({ target, evolution: card as PokemonCardModel });
        }
      }
    }
    return pairs;
  });

  readonly evolvablePokemonIds = computed<Set<string>>(() => {
    const ids = new Set<string>();
    for (const pair of this.validEvolutionPairs()) {
      ids.add(pair.target.card.id);
    }
    return ids;
  });

  readonly energyCardsInHand = computed<EnergyCardModel[]>(() => {
    const hand = this.gameState.myBoard()?.board?.hand ?? [];
    return hand.filter((c): c is EnergyCardModel => c.type === 'ENERGY');
  });

  readonly trainerCardsInHand = computed<TrainerCardModel[]>(() => {
    const hand = this.gameState.myBoard()?.board?.hand ?? [];
    return hand.filter((c): c is TrainerCardModel => c.type === 'TRAINER');
  });

  readonly playableTrainers = computed<TrainerCardModel[]>(() =>
    this.trainerCardsInHand()
  );

  readonly validTargets = computed<PokemonInPlayModel[]>(() => {
    const board = this.gameState.myBoard()?.board;
    if (!board) return [];
    const targets: PokemonInPlayModel[] = [];
    if (board.activePokemon) targets.push(board.activePokemon);
    targets.push(...(board.bench ?? []));
    return targets;
  });

  private readonly quickCoinFlipEffect = effect(() => {
    const evt = this.gameState.latestCoinFlipResult();
    if (!evt) return;
    if (evt.playerId === null) return;
    if (this._seenCoinTs.has(evt.timestamp)) return;

    const desc = evt.description;
    const headsMatch = desc.match(/Coin flip: (Heads|Tails)/);
    if (headsMatch) {
      this._seenCoinTs.add(evt.timestamp);
      const result = headsMatch[1].toLowerCase() as 'heads' | 'tails';
      this._quickFlipResult.set(result);
      this._quickFlipMessage.set(desc);
      this._showQuickCoinFlip.set(true);
    }
  });

  onQuickCoinFlipFinished(): void {
    this._showQuickCoinFlip.set(false);
  }
  readonly showPrizeSelection = computed<boolean>(() => this.gameState.mustTakePrize());

  private readonly _showRevealedCard = signal<GameCardModel | null>(null);
  readonly showRevealedCard = this._showRevealedCard.asReadonly();
  readonly revealedCardModel = computed<CardModel | null>(() => {
    const card = this._showRevealedCard();
    return card ? mapGameCardToCardModel(card) : null;
  });

  private readonly revealedCardEffect = effect(() => {
    const card = this.gameState.revealedCard();
    if (card) {
      this._showRevealedCard.set(card);
      setTimeout(() => this._showRevealedCard.set(null), 1500);
    }
  });

  dismissRevealedCard(): void {
    this._showRevealedCard.set(null);
  }

  readonly myPrizeCards = computed<GameCardModel[]>(() => {
    return this.gameState.myBoard()?.board?.prizeCards ?? [];
  });

  readonly isFaceDownReveal = computed(() => {
    const cards = this.gameState.revealedCards();
    return cards.length > 0 && cards[0].type === 'HIDDEN';
  });

  onPrizeSelected(index: number): void {
    const cards = this.myPrizeCards();
    const card = cards[index];
    if (!card) return;
    this.gameState.dispatchAction('TAKE_PRIZE', { cardId: card.id });
  }

  onAttack(): void {
    if (this.activePokemonAttacks().length > 1) {
      this._showAttackSelector.set(true);
    } else {
      this.gameState.dispatchAction('ATTACK');
      this.restoreFocus();
    }
  }

  onAttackSelected(index: number): void {
    this._showAttackSelector.set(false);
    this.gameState.dispatchAction('ATTACK', { attackIndex: index });
    this.restoreFocus();
  }

  onUseAbility(): void {
    const active = this.gameState.myActivePokemon();
    if (!active) return;
    const abilities = this.activePokemonAbilities();
    if (abilities.length === 0) return;

    // If the ability is Fairy Transfer (move energy), open the transfer modal
    const isMoveEnergy = abilities.some(a =>
      /move a \w+ Energy.*to another/i.test(a.effectText ?? '')
    );
    if (isMoveEnergy) {
      this._showEnergyTransfer.set(true);
      return;
    }

    this.gameState.dispatchAction('USE_ABILITY', { cardId: active.card.id });
    this.restoreFocus();
  }

  readonly allMyPokemon = computed<PokemonInPlayModel[]>(() => {
    const board = this.gameState.myBoard()?.board;
    if (!board) return [];
    const result: PokemonInPlayModel[] = [];
    if (board.activePokemon) result.push(board.activePokemon);
    result.push(...(board.bench ?? []));
    return result;
  });

  onTransferEnergy(data: { sourceId: string; targetId: string }): void {
    this._showEnergyTransfer.set(false);
    const active = this.gameState.myActivePokemon();
    if (!active) return;
    this.gameState.dispatchAction('USE_ABILITY', {
      cardId: active.card.id,
      sourceTargetId: data.sourceId,
      targetId: data.targetId,
    });
    this.restoreFocus();
  }

  onEvolve(): void {
    if (this.validEvolutionPairs().length > 0) {
      this._showEvolutionSelector.set(true);
    }
  }

  onEvolutionSelected(index: number): void {
    const pair = this.validEvolutionPairs()[index];
    if (!pair) return;
    this._showEvolutionSelector.set(false);
    this.gameState.dispatchAction('EVOLVE', {
      cardId: pair.evolution.id,
      targetId: pair.target.card.id,
    });
    this.restoreFocus();
  }

  onAttachEnergy(): void {
    if (this.energyCardsInHand().length > 0 && this.validTargets().length > 0) {
      this._showEnergyAttachment.set(true);
    }
  }

  onEnergySelected(data: { cardId: string; targetId: string }): void {
    this._showEnergyAttachment.set(false);
    this.gameState.dispatchAction('ATTACH_ENERGY', {
      cardId: data.cardId,
      targetId: data.targetId,
    });
    this.restoreFocus();
  }

  onPlayTrainer(): void {
    const trainers = this.playableTrainers();
    const nonTool = trainers.filter(t => t.subtype !== 'TOOL');
    const tools = trainers.filter(t => t.subtype === 'TOOL');

    if (nonTool.length === 1 && tools.length === 0) {
      this.gameState.dispatchAction('PLAY_TRAINER', { cardId: nonTool[0].id });
      this.restoreFocus();
    } else {
      this._showTrainerSelector.set(true);
    }
  }

  onTrainerSelected(index: number): void {
    const trainers = this.playableTrainers();
    const trainer = trainers[index];
    if (!trainer) return;
    this._showTrainerSelector.set(false);
    this.gameState.dispatchAction('PLAY_TRAINER', { cardId: trainer.id });
    this.restoreFocus();
  }

  onTrainerTargetSelected(data: { cardId: string; targetId: string }): void {
    this._showTrainerSelector.set(false);
    this.gameState.dispatchAction('PLAY_TRAINER', {
      cardId: data.cardId,
      targetId: data.targetId,
    });
    this.restoreFocus();
  }

  onCancelTrainerSelector(): void {
    this._showTrainerSelector.set(false);
  }

  /** El jugador eligió una carta revelada (Great Ball) para su mano. */
  onRevealCardSelected(cardId: string): void {
    console.log('[Reveal] Selected cardId:', cardId, 'matchId:', this.gameState.matchId(), 'mustResolveReveal:', this.gameState.mustResolveReveal());
    this.gameState.resolveReveal(cardId);
  }

  /** No hay Pokémon entre las reveladas: vuelven al mazo. */
  onRevealSkip(): void {
    this.gameState.resolveReveal();
  }

  onRetreat(): void {
    const bench = this.gameState.myBoard()?.board?.bench;
    if (!bench || bench.length === 0) return;
    if (bench.length === 1) {
      this.gameState.dispatchAction('RETREAT', { targetId: bench[0].card.id });
      this.restoreFocus();
    } else {
      this._showRetreatSelector.set(true);
    }
  }

  onRetreatSelected(index: number): void {
    const bench = this.gameState.myBoard()?.board?.bench;
    if (!bench) return;
    this._showRetreatSelector.set(false);
    this.gameState.dispatchAction('RETREAT', { targetId: bench[index].card.id });
    this.restoreFocus();
  }

  onCancelRetreatSelector(): void {
    this._showRetreatSelector.set(false);
  }

  onTargetSelected(targetId: string): void {
    this.gameState.selectTarget(targetId);
    this.restoreFocus();
  }

  onCancelTargetSelector(): void {
    // Just a no-op — the selector auto-closes when pending count hits 0
  }

  onCancelEnergyTransfer(): void {
    this._showEnergyTransfer.set(false);
  }

  onPassTurn(): void {
    this.gameState.passTurn();
    this.restoreFocus();
  }

  onPromote(benchIndex: number): void {
    this.gameState.promoteActive(benchIndex);
    this.restoreFocus();
  }

  onSurrender(): void {
    this._showSurrenderConfirm.set(true);
  }

  onConfirmSurrender(): void {
    this._showSurrenderConfirm.set(false);
    this.gameState.surrender();
    this.restoreFocus();
  }

  onCancelSurrender(): void {
    this._showSurrenderConfirm.set(false);
  }

  onCancelAttackSelector(): void {
    this._showAttackSelector.set(false);
  }

  onCancelEvolutionSelector(): void {
    this._showEvolutionSelector.set(false);
  }

  onCancelEnergyAttachment(): void {
    this._showEnergyAttachment.set(false);
  }

  private restoreFocus(): void {
    const el = document.querySelector<HTMLElement>('.game-board');
    el?.focus({ preventScroll: true });
  }
}
