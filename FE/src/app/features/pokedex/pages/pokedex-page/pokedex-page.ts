import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { CardService } from '../../../../core/services/card.service';
import { CardModel, PageResponse } from '../../../../shared/models/card.model';
import { CardComponent } from '../../../../shared/components/card/card';
import { CardZoomComponent } from '../../../../shared/components/card-zoom/card-zoom';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-pokedex-page',
  imports: [CardComponent, CardZoomComponent, LoadingSpinnerComponent],
  templateUrl: './pokedex-page.html',
  styleUrl: './pokedex-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PokedexPage implements OnInit, OnDestroy {
  private readonly cardService = inject(CardService);
  private readonly destroy$ = new Subject<void>();

  readonly loading = signal(false);
  readonly cards = signal<CardModel[]>([]);
  readonly error = signal<string | null>(null);
  readonly totalCards = signal(0);

  readonly search = signal('');
  readonly activeFilter = signal<string>('Todos');
  readonly filters: readonly string[] = ['Todos', 'Pokémon', 'Trainer', 'Energy'];

  readonly selectedCard = signal<CardModel | null>(null);

  openCard(card: CardModel): void {
    this.selectedCard.set(card);
  }

  closeCard(): void {
    this.selectedCard.set(null);
  }

  readonly filteredCards = computed<CardModel[]>(() => {
    const term = this.search().trim().toLowerCase();
    const filter = this.activeFilter();
    return this.cards().filter((card) => {
      const matchesType = filter === 'Todos' || card.supertype === filter;
      const matchesTerm = !term || card.name.toLowerCase().includes(term);
      return matchesType && matchesTerm;
    });
  });

  onSearch(value: string): void {
    this.search.set(value);
  }

  setFilter(filter: string): void {
    this.activeFilter.set(filter);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cardService.getCards({ size: 200 }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (page: PageResponse<CardModel>) => {
        this.cards.set(page.content);
        this.totalCards.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Error al cargar las cartas. Intentalo de nuevo.');
        this.loading.set(false);
      }
    });
  }
}
