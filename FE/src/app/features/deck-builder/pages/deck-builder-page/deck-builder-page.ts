import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, forkJoin, takeUntil } from 'rxjs';
import { CardCatalogComponent } from '../../components/card-catalog/card-catalog';
import { DeckEditorComponent } from '../../components/deck-editor/deck-editor';
import { DeckSummaryComponent } from '../../components/deck-summary/deck-summary';
import { DeckListComponent } from '../../components/deck-list/deck-list';
import { CardZoomComponent } from '../../../../shared/components/card-zoom/card-zoom';
import { ModalComponent } from '../../../../shared/ui/modal/modal';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { DeckService } from '../../services/deck.service';
import { CardService } from '../../../../core/services/card.service';
import { CardModel } from '../../../../shared/models/card.model';
import { PageResponse } from '../../../../shared/models/card.model';
import { DeckModel, DeckCardModel, CreateDeckModel, DeckValidationResultModel } from '../../../../shared/models/deck.model';

@Component({
  selector: 'app-deck-builder-page',
  imports: [
    CardCatalogComponent,
    DeckEditorComponent,
    DeckSummaryComponent,
    DeckListComponent,
    CardZoomComponent,
    ModalComponent,
    ButtonComponent
  ],
  templateUrl: './deck-builder-page.html',
  styleUrl: './deck-builder-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckBuilderPage implements OnDestroy {
  private readonly deckService = inject(DeckService);
  private readonly cardService = inject(CardService);
  private readonly destroy$ = new Subject<void>();
  private newDeckCounter = 0;

  readonly mode = signal<'list' | 'edit'>('list');
  readonly decks = signal<DeckModel[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly currentDeckId = signal<string | null>(null);
  readonly currentDeckName = signal('');
  readonly currentDeckCards = signal<DeckCardModel[]>([]);
  readonly validationResult = signal<DeckValidationResultModel | null>(null);
  readonly selectedCard = signal<CardModel | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly pendingDeleteDeck = signal<DeckModel | null>(null);
  readonly showDeleteModal = signal(false);
  readonly deleting = signal(false);

  /** ID del mazo seleccionado para usar en partida (persistido en localStorage). */
  readonly selectedDeckId = signal<string | null>(null);

  readonly isEditing = computed(() => this.currentDeckId() !== null);

  /** IDs de cartas cuyo límite por nombre (4) ya fue alcanzado. Energías básicas excluidas. */
  readonly maxedCardIds = computed(() => {
    const cards = this.currentDeckCards();
    const nameCounts = new Map<string, number>();
    for (const dc of cards) {
      if (!dc?.card) continue;
      const prev = nameCounts.get(dc.card.name) ?? 0;
      nameCounts.set(dc.card.name, prev + dc.quantity);
    }
    return new Set(
      cards
        .filter(dc => {
          if (!dc?.card) return false;
          if (dc.card.supertype === 'Energy' && dc.card.subtypes?.includes('Basic')) return false;
          return (nameCounts.get(dc.card.name) ?? 0) >= 4;
        })
        .map(dc => dc.card.id)
    );
  });

  /** IDs de cartas evolutivas cuya preevolución no está en el mazo. */
  readonly blockedCardIds = computed(() => {
    const cards = this.currentDeckCards();
    const namesInDeck = new Set(cards.map(dc => dc.card.name));
    return new Set(
      cards
        .filter(dc => dc.card.evolvesFrom && !namesInDeck.has(dc.card.evolvesFrom))
        .map(dc => dc.card.id)
    );
  });

  private isBasicEnergy(card: CardModel): boolean {
    return card.supertype === 'Energy' && card.subtypes?.includes('Basic');
  }

  private countCopiesByName(cards: DeckCardModel[], name: string): number {
    return cards
      .filter(c => c.card.name === name)
      .reduce((sum, c) => sum + c.quantity, 0);
  }

  private get playerId(): string {
    let id = localStorage.getItem('playerId');
    if (!id) {
      id = prompt('Ingresá tu ID de jugador:') ?? 'player-1';
      localStorage.setItem('playerId', id);
    }
    return id;
  }

  constructor() {
    this.loadDecks();
    this.restoreEquippedDeck();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Restaura el mazo equipado desde localStorage al iniciar. */
  private restoreEquippedDeck(): void {
    const equipped = localStorage.getItem(`deck-for-${this.playerId}`);
    if (equipped) this.selectedDeckId.set(equipped);
  }

  loadDecks(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.deckService.getDecks(this.playerId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (decks: DeckModel[]) => {
        this.decks.set(decks.slice().reverse());
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Error al cargar los mazos. Verificá que el backend esté corriendo.');
        this.loading.set(false);
      }
    });
  }

  onSelectDeck(deck: DeckModel): void {
    this.currentDeckId.set(deck.id);
    this.currentDeckName.set(deck.name);
    this.currentDeckCards.set(
      deck.cards.filter(c => c?.card != null).map(c => ({
        card: c.card,
        quantity: c.quantity
      }))
    );
    this.validationResult.set(null);
    this.errorMessage.set(null);
    this.mode.set('edit');
  }

  onCreateNew(): void {
    this.currentDeckId.set(null);
    this.currentDeckName.set(`New-Deck-${this.newDeckCounter++}`);
    this.currentDeckCards.set([]);
    this.validationResult.set(null);
    this.errorMessage.set(null);
    this.selectedCard.set(null);
    this.mode.set('edit');
  }

  onDeleteDeck(deck: DeckModel): void {
    this.pendingDeleteDeck.set(deck);
    this.showDeleteModal.set(true);
  }

  onConfirmDelete(): void {
    const deck = this.pendingDeleteDeck();
    if (!deck) return;
    this.deleting.set(true);
    this.showDeleteModal.set(false);
    this.deckService.deleteDeck(deck.id, this.playerId).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        if (this.selectedDeckId() === deck.id) {
          localStorage.removeItem(`deck-for-${this.playerId}`);
          this.selectedDeckId.set(null);
        }
        this.loadDecks();
        this.deleting.set(false);
      },
      error: () => {
        this.errorMessage.set('Error al eliminar el mazo.');
        this.deleting.set(false);
      }
    });
    this.pendingDeleteDeck.set(null);
  }

  onCancelDelete(): void {
    this.showDeleteModal.set(false);
    this.pendingDeleteDeck.set(null);
  }

  /** Marca un mazo como "equipado" para unirse a partidas desde el lobby. */
  onUseInMatch(deck: DeckModel): void {
    const key = `deck-for-${this.playerId}`;
    if (this.selectedDeckId() === deck.id) {
      localStorage.removeItem(key);
      this.selectedDeckId.set(null);
    } else {
      localStorage.setItem(key, deck.id);
      this.selectedDeckId.set(deck.id);
    }
    this.onSelectDeck(deck);
  }

  onCardSelected(card: CardModel): void {
    this.currentDeckCards.update(cards => {
      if (card.evolvesFrom) {
        const hasPreEvolution = cards.some(dc => dc.card.name === card.evolvesFrom);
        if (!hasPreEvolution) {
          return cards;
        }
      }

      if (this.isBasicEnergy(card)) {
        const existing = cards.find(c => c.card.id === card.id);
        if (existing) {
          return cards.map(c =>
            c.card.id === card.id ? { ...c, quantity: c.quantity + 1 } : c
          );
        }
        return [{ card, quantity: 1 }, ...cards];
      }

      const totalByName = this.countCopiesByName(cards, card.name);
      if (totalByName >= 4) return cards;

      const existing = cards.find(c => c.card.id === card.id);
      if (existing) {
        return cards.map(c =>
          c.card.id === card.id ? { ...c, quantity: c.quantity + 1 } : c
        );
      }
      return [{ card, quantity: 1 }, ...cards];
    });
  }

  onIncrementCard(cardId: string): void {
    this.currentDeckCards.update(cards => {
      const target = cards.find(c => c.card.id === cardId);
      if (!target) return cards;

      if (this.isBasicEnergy(target.card)) {
        return cards.map(c =>
          c.card.id === cardId ? { ...c, quantity: c.quantity + 1 } : c
        );
      }

      const totalByName = this.countCopiesByName(cards, target.card.name);
      if (totalByName >= 4) return cards;

      return cards.map(c =>
        c.card.id === cardId ? { ...c, quantity: c.quantity + 1 } : c
      );
    });
  }

  onDecrementCard(cardId: string): void {
    this.currentDeckCards.update(cards => {
      const updated = cards.map(c =>
        c.card.id === cardId && c.quantity > 1
          ? { ...c, quantity: c.quantity - 1 }
          : c
      );
      return updated.filter(c => c.quantity > 0);
    });
  }

  onRemoveCard(cardId: string): void {
    this.currentDeckCards.update(cards => cards.filter(c => c.card.id !== cardId));
  }

  onCardDetail(card: CardModel): void {
    this.selectedCard.set(card);
  }

  onCloseDetail(): void {
    this.selectedCard.set(null);
  }

  onSave(): void {
    const name = this.currentDeckName().trim();
    const totalCards = this.currentDeckCards().reduce((sum, c) => sum + c.quantity, 0);
    if (!name || totalCards !== 60) return;

    this.saving.set(true);
    this.errorMessage.set(null);

    const deckData: CreateDeckModel = {
      name,
      cards: this.currentDeckCards().map(c => ({
        cardId: c.card.id,
        quantity: c.quantity
      }))
    };

    const save$ = this.currentDeckId()
      ? this.deckService.updateDeck(this.currentDeckId()!, deckData, this.playerId)
      : this.deckService.createDeck(deckData, this.playerId);

    save$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (saved: DeckModel) => {
        this.saving.set(false);
        this.currentDeckId.set(saved.id);
        this.loadDecks();
        this.mode.set('list');
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.errorMessage.set(err.error?.message ?? 'Error al guardar el mazo.');
      }
    });
  }

  onDelete(): void {
    const id = this.currentDeckId();
    if (!id) return;
    if (!confirm('¿Eliminar este mazo?')) return;

    this.deckService.deleteDeck(id, this.playerId).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loadDecks();
        this.mode.set('list');
      },
      error: () => {
        this.errorMessage.set('Error al eliminar el mazo.');
      }
    });
  }

  onBackToList(): void {
    this.mode.set('list');
    this.validationResult.set(null);
    this.errorMessage.set(null);
  }

  onClearAll(): void {
    this.currentDeckCards.set([]);
  }

  onRandomFill(): void {
    const existing = this.currentDeckCards();
    const currentTotal = existing.reduce((sum, c) => sum + c.quantity, 0);
    const slots = 60 - currentTotal;
    if (slots <= 0) return;

    const nameCounts = new Map<string, number>();
    for (const dc of existing) {
      nameCounts.set(dc.card.name, (nameCounts.get(dc.card.name) ?? 0) + dc.quantity);
    }

    const hasBasic = existing.some(dc =>
      dc.card.supertype === 'Pokémon' && dc.card.stage === 'BASIC'
    );

    let aceSpecCount = existing.filter(dc =>
      dc.card.subtypes?.some((s) => s.toUpperCase() === 'ACE SPEC')
    ).reduce((sum, c) => sum + c.quantity, 0);
    const maxAceSpec = 1;

    forkJoin({
      pokemon: this.cardService.getCards({ supertype: 'Pokémon', size: 200 }),
      trainers: this.cardService.getCards({ supertype: 'Trainer', size: 200 }),
      energy: this.cardService.getCards({ supertype: 'Energy', size: 100 }),
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: ({ pokemon, trainers, energy }: { pokemon: PageResponse<CardModel>; trainers: PageResponse<CardModel>; energy: PageResponse<CardModel> }) => {
        const pool = [...pokemon.content, ...trainers.content, ...energy.content];
        this.shuffleArray(pool);

        const added: DeckCardModel[] = existing.map(c => ({ ...c }));
        let addedCount = 0;

        if (!hasBasic) {
          const basic = pool.find(c =>
            c.supertype === 'Pokémon' && c.stage === 'BASIC' &&
            (nameCounts.get(c.name) ?? 0) < 4
          );
          if (basic) {
            added.push({ card: basic, quantity: 1 });
            nameCounts.set(basic.name, (nameCounts.get(basic.name) ?? 0) + 1);
            addedCount++;
          }
        }

        for (const card of pool) {
          if (addedCount >= slots) break;
          if (card.subtypes?.some((s) => s.toUpperCase() === 'ACE SPEC') && aceSpecCount >= maxAceSpec) continue;
          const count = nameCounts.get(card.name) ?? 0;
          const isBasicEnergy = card.supertype === 'Energy' && card.subtypes?.includes('Basic');
          const maxForCard = isBasicEnergy ? Infinity : 4;
          if (count >= maxForCard) continue;

          if (card.evolvesFrom) {
            const preEvolutionInDeck = added.some(dc => dc.card.name === card.evolvesFrom);
            if (!preEvolutionInDeck) continue;
          }

          const idx = added.findIndex(c => c.card.id === card.id);
          if (idx >= 0) {
            added[idx] = { ...added[idx], quantity: added[idx].quantity + 1 };
          } else {
            added.push({ card, quantity: 1 });
          }
          nameCounts.set(card.name, count + 1);
          if (card.subtypes?.some((s) => s.toUpperCase() === 'ACE SPEC')) aceSpecCount++;
          addedCount++;
        }

        const finalTotal = added.reduce((sum, c) => sum + c.quantity, 0);
        if (finalTotal < 60) {
          this.errorMessage.set(`No se encontraron suficientes cartas (${finalTotal}/60). Probá ejecutar de nuevo.`);
        }

        this.currentDeckCards.set(added);
      },
      error: () => {
        this.errorMessage.set('Error al cargar cartas. Verificá que el backend esté corriendo.');
      }
    });
  }

  private shuffleArray<T>(array: T[]): void {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
  }

  private validateDeck(deckId: string): void {
    this.deckService.validateDeck(deckId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (result: DeckValidationResultModel) => {
        this.validationResult.set(result);
      }
    });
  }
}
