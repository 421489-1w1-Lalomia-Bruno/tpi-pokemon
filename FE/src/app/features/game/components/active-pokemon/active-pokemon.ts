import { Component, ChangeDetectionStrategy, computed, input, output, inject, signal } from '@angular/core';
import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { CardComponent } from '../../../../shared/components/card/card';
import { DamageCounterComponent } from '../../../../shared/components/damage-counter/damage-counter';
import { StatusIndicatorComponent } from '../../../../shared/components/status-indicator/status-indicator';
import { CardModel } from '../../../../shared/models/card.model';
import { EnergyType, PokemonInPlayModel, SpecialCondition, TrainerCardModel, TurnPhase } from '../../../../shared/models/game.model';
import { flatEnergyTypes, mapGameCardToCardModel, mapPokemonInPlayToCardModel } from '../../../../shared/utils/card-mapper';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';

@Component({
  selector: 'app-active-pokemon',
  imports: [CdkDropList, CardComponent, DamageCounterComponent, StatusIndicatorComponent],
  templateUrl: './active-pokemon.html',
  styleUrl: './active-pokemon.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivePokemonComponent {
  private readonly cardZoom = inject(CardZoomService);

  readonly pokemon = input<PokemonInPlayModel | null>(null);
  readonly isMyTurn = input(false);
  readonly phase = input<TurnPhase | null>(null);
  readonly evolvablePokemonIds = input<Set<string>>(new Set());

  readonly cardDropped = output<CdkDragDrop<PokemonInPlayModel | null>>();

  readonly canEvolve = computed<boolean>(() => {
    const poke = this.pokemon();
    return poke !== null && this.evolvablePokemonIds().has(poke.card.id);
  });

  readonly showAttacks = signal(false);

  readonly cardModel = computed<CardModel | null>(() => {
    const poke = this.pokemon();
    return poke ? mapPokemonInPlayToCardModel(poke) : null;
  });

  readonly attachedEnergyTypes = computed<EnergyType[]>(() => {
    const poke = this.pokemon();
    if (!poke) return [];
    return flatEnergyTypes(poke.attachedEnergy);
  });

  readonly specialConditions = computed<SpecialCondition[]>(() => {
    const poke = this.pokemon();
    return poke?.specialConditions ?? [];
  });

  readonly isConfused = computed<boolean>(() => {
    return this.specialConditions().includes('CONFUSED');
  });

  onCardClick(): void {
    const poke = this.pokemon();
    const card = this.cardModel();
    if (card && poke) this.cardZoom.openWithAttachments(card, poke.attachedEnergy, poke.tool);
  }

  onToolClick(tool: TrainerCardModel): void {
    this.cardZoom.open(mapGameCardToCardModel(tool));
  }
}
