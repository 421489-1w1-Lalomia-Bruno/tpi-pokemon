import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { PokemonCardModel, PokemonInPlayModel } from '../../../../shared/models/game.model';

export interface ValidEvolutionPair {
  target: PokemonInPlayModel;
  evolution: PokemonCardModel;
}

@Component({
  selector: 'app-evolution-selector',
  templateUrl: './evolution-selector.html',
  styleUrl: './evolution-selector.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvolutionSelectorComponent {
  readonly show = input(false);
  readonly pairs = input<ValidEvolutionPair[]>([]);

  readonly pairSelected = output<number>();
  readonly cancel = output<void>();
}
