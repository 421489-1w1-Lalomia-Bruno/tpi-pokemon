import { Component, ChangeDetectionStrategy, computed, input, inject } from '@angular/core';
import { CardComponent } from '../../../../shared/components/card/card';
import { DamageCounterComponent } from '../../../../shared/components/damage-counter/damage-counter';
import { StatusIndicatorComponent } from '../../../../shared/components/status-indicator/status-indicator';
import { CardModel } from '../../../../shared/models/card.model';
import { EnergyType, PlayerBoardModel, PokemonInPlayModel, TrainerCardModel } from '../../../../shared/models/game.model';
import { flatEnergyTypes, mapGameCardToCardModel, mapPokemonInPlayToCardModel } from '../../../../shared/utils/card-mapper';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';

const MAX_BENCH = 5;

@Component({
  selector: 'app-opponent-area',
  imports: [CardComponent, DamageCounterComponent, StatusIndicatorComponent],
  templateUrl: './opponent-area.html',
  styleUrl: './opponent-area.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OpponentAreaComponent {
  private readonly cardZoom = inject(CardZoomService);

  readonly board = input<PlayerBoardModel | null>(null);

  readonly activePokemon = computed(() => this.board()?.activePokemon ?? null);
  readonly bench = computed<PokemonInPlayModel[]>(() => this.board()?.bench ?? []);
  readonly handCount = computed(() => this.board()?.hand.length ?? 0);
  readonly deckCount = computed(() => this.board()?.deck.length ?? 0);
  readonly discardCount = computed(() => this.board()?.discardPile.length ?? 0);
  readonly prizeCount = computed(() => this.board()?.prizeCards.length ?? 0);

  readonly slots = computed(() => {
    const result: (PokemonInPlayModel | null)[] = [];
    for (let i = 0; i < MAX_BENCH; i++) {
      result.push(this.bench()[i] ?? null);
    }
    return result;
  });

  readonly activeCardModel = computed<CardModel | null>(() => {
    const poke = this.activePokemon();
    return poke ? mapPokemonInPlayToCardModel(poke) : null;
  });

  readonly activeEnergyTypes = computed<EnergyType[]>(() => {
    const poke = this.activePokemon();
    if (!poke) return [];
    return flatEnergyTypes(poke.attachedEnergy);
  });

  readonly isActiveConfused = computed<boolean>(() => {
    const poke = this.activePokemon();
    return poke?.specialConditions.includes('CONFUSED') ?? false;
  });

  toCardModel(pip: PokemonInPlayModel): CardModel {
    return mapPokemonInPlayToCardModel(pip);
  }

  toEnergyTypes(pip: PokemonInPlayModel): EnergyType[] {
    return flatEnergyTypes(pip.attachedEnergy);
  }

  onActiveCardClick(): void {
    const card = this.activeCardModel();
    if (card) this.cardZoom.open(card);
  }

  onBenchCardClick(pip: PokemonInPlayModel): void {
    this.cardZoom.open(this.toCardModel(pip));
  }

  onActiveToolClick(tool: TrainerCardModel): void {
    this.cardZoom.open(mapGameCardToCardModel(tool));
  }
}
