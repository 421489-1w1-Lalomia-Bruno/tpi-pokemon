import { Component, ChangeDetectionStrategy, computed, input, output } from '@angular/core';
import { GameCardModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-prize-cards',
  templateUrl: './prize-cards.html',
  styleUrl: './prize-cards.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrizeCardsComponent {
  readonly cards = input<GameCardModel[]>([]);
  readonly label = input('Premios');
  readonly selectable = input(false);

  readonly prizeSelected = output<number>();

  readonly count = computed(() => this.cards().length);
  readonly isEmpty = computed(() => this.count() === 0);

  readonly slots = computed(() => {
    const total = this.count();
    const result: number[] = [];
    for (let i = 0; i < 6; i++) {
      result.push(i < total ? 1 : 0);
    }
    return result;
  });

  onSlotClick(index: number): void {
    if (this.selectable()) {
      this.prizeSelected.emit(index);
    }
  }
}
