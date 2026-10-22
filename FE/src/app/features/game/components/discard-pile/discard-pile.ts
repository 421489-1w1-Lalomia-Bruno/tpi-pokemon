import { Component, ChangeDetectionStrategy, computed, input, inject } from '@angular/core';
import { GameCardModel } from '../../../../shared/models/game.model';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';
import { mapGameCardToCardModel } from '../../../../shared/utils/card-mapper';

@Component({
  selector: 'app-discard-pile',
  templateUrl: './discard-pile.html',
  styleUrl: './discard-pile.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DiscardPileComponent {
  private readonly cardZoom = inject(CardZoomService);

  readonly cards = input<GameCardModel[]>([]);
  readonly label = input('Descarte');

  readonly count = computed(() => this.cards().length);
  readonly isEmpty = computed(() => this.count() === 0);
  readonly topCard = computed<GameCardModel | null>(() => (this.count() > 0 ? this.cards()[this.count() - 1] : null));

  readonly topCardType = computed<string>(() => {
    const card = this.topCard();
    if (!card) return '';
    if (card.type === 'ENERGY') return card.energyType;
    return card.type;
  });

  onTopCardClick(): void {
    const card = this.topCard();
    if (card) this.cardZoom.open(mapGameCardToCardModel(card));
  }
}
