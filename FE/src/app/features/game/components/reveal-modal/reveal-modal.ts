import { Component, ChangeDetectionStrategy, input, output, computed } from '@angular/core';
import { GameCardModel } from '../../../../shared/models/game.model';
import { CardModel } from '../../../../shared/models/card.model';
import { mapGameCardToCardModel } from '../../../../shared/utils/card-mapper';
import { CardComponent } from '../../../../shared/components/card/card';

@Component({
  selector: 'app-reveal-modal',
  imports: [CardComponent],
  templateUrl: './reveal-modal.html',
  styleUrl: './reveal-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RevealModalComponent {
  readonly cards = input<GameCardModel[]>([]);
  readonly faceDown = input(false);
  readonly cardSelected = output<string>();
  readonly skip = output<void>();

  readonly pokemonCards = computed(() =>
    this.cards().filter(c => c.type === 'POKEMON')
  );

  readonly hasPokemon = computed(() => this.pokemonCards().length > 0);

  toCardModel(card: GameCardModel): CardModel {
    return mapGameCardToCardModel(card);
  }

  onCardClick(cardId: string): void {
    if (this.faceDown()) {
      this.cardSelected.emit(cardId);
      return;
    }
    const card = this.cards().find(c => c.id === cardId);
    if (card && card.type === 'POKEMON') {
      this.cardSelected.emit(cardId);
    }
  }

  onSkip(): void {
    this.skip.emit();
  }
}
