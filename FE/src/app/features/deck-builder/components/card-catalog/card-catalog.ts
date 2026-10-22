import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CdkDrag, CdkDragPreview, CdkDropList } from '@angular/cdk/drag-drop';
import { Subject, switchMap, debounceTime, catchError, of, tap, takeUntil } from 'rxjs';
import { CardService } from '../../../../core/services/card.service';
import { CardModel, CardFilterModel, PageResponse } from '../../../../shared/models/card.model';
import { DeckCardModel } from '../../../../shared/models/deck.model';
import { CARD_SUPERTYPES } from '../../../../core/constants/card-types';
import { ENERGY_TYPES } from '../../../../core/constants/energy-types';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-card-catalog',
  imports: [FormsModule, CdkDrag, CdkDragPreview, CdkDropList, LoadingSpinnerComponent],
  templateUrl: './card-catalog.html',
  styleUrl: './card-catalog.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardCatalogComponent implements OnDestroy {
  private readonly cardService = inject(CardService);

  readonly cardSelected = output<CardModel>();
  readonly cardClicked = output<CardModel>();

  readonly blockedCardIds = input<Set<string>>(new Set());

  readonly supertypes = Object.values(CARD_SUPERTYPES);
  readonly energyTypes = Object.values(ENERGY_TYPES);

  readonly searchTerm = signal('');
  readonly selectedType = signal<string | null>(null);
  readonly selectedSupertype = signal<string | null>(null);
  readonly cards = signal<CardModel[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly totalResults = signal(0);

  readonly deckCards = input<DeckCardModel[]>([]);
  readonly maxedCardIds = input<Set<string>>(new Set());

  readonly deckQuantity = computed(() => {
    const map = new Map<string, number>();
    for (const dc of this.deckCards()) {
      if (!dc?.card) continue;
      map.set(dc.card.id, (map.get(dc.card.id) ?? 0) + dc.quantity);
    }
    return map;
  });

  private readonly searchSubject = new Subject<void>();
  private readonly destroy$ = new Subject<void>();

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      tap(() => {
        this.loading.set(true);
        this.error.set(null);
      }),
      switchMap(() => {
        const filter: CardFilterModel = {
          name: this.searchTerm() || undefined,
          type: this.selectedType() || undefined,
          supertype: this.selectedSupertype() || undefined,
          size: 50
        };
        return this.cardService.getCards(filter).pipe(
          catchError(() => {
            this.error.set('Error al buscar cartas.');
            return of(null);
          })
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe((page: PageResponse<CardModel> | null) => {
      if (page) {
        this.cards.set(page.content);
        this.totalResults.set(page.totalElements);
      }
      this.loading.set(false);
    });

    this.searchSubject.next();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(): void {
    this.searchSubject.next();
  }

  onFilterChange(): void {
    this.searchSubject.next();
  }

  selectCard(card: CardModel, event: MouseEvent): void {
    event.stopPropagation();
    this.cardSelected.emit(card);
  }
}
