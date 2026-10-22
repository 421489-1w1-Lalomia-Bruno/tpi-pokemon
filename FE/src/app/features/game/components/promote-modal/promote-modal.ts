import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { PokemonInPlayModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-promote-modal',
  templateUrl: './promote-modal.html',
  styleUrl: './promote-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PromoteModalComponent {
  readonly show = input(false);
  readonly benchPokemon = input<PokemonInPlayModel[]>([]);

  readonly promote = output<number>();
}
