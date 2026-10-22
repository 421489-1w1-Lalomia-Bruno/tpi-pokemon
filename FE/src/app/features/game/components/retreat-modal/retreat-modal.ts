import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { PokemonInPlayModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-retreat-modal',
  templateUrl: './retreat-modal.html',
  styleUrl: './retreat-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RetreatModalComponent {
  readonly show = input(false);
  readonly benchPokemon = input<PokemonInPlayModel[]>([]);

  readonly retreat = output<number>();
  readonly cancel = output<void>();
}
