import { Component, ChangeDetectionStrategy, computed, input, output, inject } from '@angular/core';
import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { CardComponent } from '../../../../shared/components/card/card';
import { DamageCounterComponent } from '../../../../shared/components/damage-counter/damage-counter';
import { StatusIndicatorComponent } from '../../../../shared/components/status-indicator/status-indicator';
import { CardModel } from '../../../../shared/models/card.model';
import { EnergyType, PokemonInPlayModel, TurnPhase } from '../../../../shared/models/game.model';
import { flatEnergyTypes, mapPokemonInPlayToCardModel } from '../../../../shared/utils/card-mapper';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';

const MAX_BENCH = 5;

@Component({
  selector: 'app-bench-area',
  imports: [CdkDropList, CardComponent, DamageCounterComponent, StatusIndicatorComponent],
  templateUrl: './bench-area.html',
  styleUrl: './bench-area.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BenchAreaComponent {
  private readonly cardZoom = inject(CardZoomService);

  readonly pokemon = input<PokemonInPlayModel[]>([]);
  readonly isMyTurn = input(false);
  readonly phase = input<TurnPhase | null>(null);
  readonly evolvablePokemonIds = input<Set<string>>(new Set());

  readonly cardDropped = output<CdkDragDrop<PokemonInPlayModel | null>>();

  readonly slots = computed(() => {
    const result: (PokemonInPlayModel | null)[] = [];
    for (let i = 0; i < MAX_BENCH; i++) {
      result.push(this.pokemon()[i] ?? null);
    }
    return result;
  });

  toCardModel(pip: PokemonInPlayModel): CardModel {
    return mapPokemonInPlayToCardModel(pip);
  }

  toEnergyTypes(pip: PokemonInPlayModel): EnergyType[] {
    return flatEnergyTypes(pip.attachedEnergy);
  }

  canEvolve(pip: PokemonInPlayModel | null): boolean {
    return pip !== null && this.evolvablePokemonIds().has(pip.card.id);
  }

  onCardClick(pip: PokemonInPlayModel): void {
    this.cardZoom.openWithAttachments(this.toCardModel(pip), pip.attachedEnergy, pip.tool);
  }
}
