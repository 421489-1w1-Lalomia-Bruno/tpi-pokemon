import { Component, ChangeDetectionStrategy, computed, input } from '@angular/core';
import { GameCardModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-deck-pile',
  templateUrl: './deck-pile.html',
  styleUrl: './deck-pile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckPileComponent {
  readonly cards = input<GameCardModel[]>([]);
  readonly label = input('Mazo');

  readonly count = computed(() => this.cards().length);
  readonly isEmpty = computed(() => this.count() === 0);
}
