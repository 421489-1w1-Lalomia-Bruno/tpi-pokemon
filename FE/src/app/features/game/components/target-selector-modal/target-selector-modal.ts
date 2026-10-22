import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { PokemonInPlayModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-target-selector-modal',
  templateUrl: './target-selector-modal.html',
  styleUrl: './target-selector-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TargetSelectorModalComponent {
  readonly show = input(false);
  readonly benchPokemon = input<PokemonInPlayModel[]>([]);
  readonly remaining = input(1);

  readonly targetSelected = output<string>();
  readonly cancel = output<void>();
}
