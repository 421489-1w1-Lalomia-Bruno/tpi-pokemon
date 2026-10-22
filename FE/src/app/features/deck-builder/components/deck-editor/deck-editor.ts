import { ChangeDetectionStrategy, Component, input, model, output, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CdkDrag, CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { CardModel } from '../../../../shared/models/card.model';
import { DeckCardModel } from '../../../../shared/models/deck.model';

@Component({
  selector: 'app-deck-editor',
  imports: [FormsModule, CdkDropList, CdkDrag],
  templateUrl: './deck-editor.html',
  styleUrl: './deck-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckEditorComponent {
  readonly cards = input<DeckCardModel[]>([]);
  readonly deckName = model('');
  readonly saving = input(false);
  readonly editing = input(false);
  readonly maxedCardIds = input<Set<string>>(new Set());

  readonly addCard = output<string>();
  readonly cardDropped = output<CardModel>();
  readonly removeCard = output<string>();
  readonly incrementCard = output<string>();
  readonly decrementCard = output<string>();
  readonly save = output<void>();
  readonly fillRandom = output<void>();
  readonly clearAll = output<void>();

  readonly totalCards = computed(() =>
    this.cards().reduce((sum, c) => sum + c.quantity, 0)
  );

  readonly diag = computed(() => {
    const s = this.saving();
    const n = this.deckName();
    const t = this.totalCards();
    console.log('[deck-editor]', { saving: s, name: `"${n}"`, nameLen: n.trim().length, totalCards: t, blocked: s || n.trim().length === 0 || t !== 60 });
    return '';
  });

  readonly maxCardsReached = computed(() => this.totalCards() >= 60);
  readonly progress = computed(() => Math.min(100, (this.totalCards() / 60) * 100));

  /** Drop desde el catálogo: agrega la carta arrastrada al mazo. */
  onDrop(event: CdkDragDrop<DeckCardModel[]>): void {
    if (event.previousContainer === event.container) return;
    const card = event.item.data as CardModel | undefined;
    if (card) this.cardDropped.emit(card);
  }
}
