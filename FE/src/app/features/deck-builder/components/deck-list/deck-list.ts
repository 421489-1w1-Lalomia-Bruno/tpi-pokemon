import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DeckModel } from '../../../../shared/models/deck.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-deck-list',
  imports: [ButtonComponent, LoadingSpinnerComponent],
  templateUrl: './deck-list.html',
  styleUrl: './deck-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckListComponent {
  readonly decks = input<DeckModel[]>([]);
  readonly loading = input(false);
  readonly selectedDeckId = input<string | null>(null);

  readonly selectDeck = output<DeckModel>();
  readonly deleteDeck = output<DeckModel>();
  readonly createNew = output<void>();
  readonly useInMatch = output<DeckModel>();

  trackById(_index: number, deck: DeckModel): string {
    return deck.id;
  }
}
