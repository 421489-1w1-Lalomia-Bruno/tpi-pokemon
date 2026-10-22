import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { DeckCardModel, DeckValidationResultModel } from '../../../../shared/models/deck.model';

interface Requirement {
  readonly label: string;
  readonly detail: string;
  readonly met: boolean;
}

@Component({
  selector: 'app-deck-summary',
  templateUrl: './deck-summary.html',
  styleUrl: './deck-summary.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DeckSummaryComponent {
  /** Cartas del mazo en edición (para requisitos en vivo). */
  readonly deckCards = input<DeckCardModel[]>([]);
  /** Resultado de validación del backend (tras guardar). */
  readonly validationResult = input<DeckValidationResultModel | null>(null);

  readonly errors = computed(() => this.validationResult()?.errors ?? []);

  readonly totalCards = computed(() =>
    this.deckCards().reduce((sum, c) => sum + c.quantity, 0),
  );

  readonly basicPokemonCount = computed(() =>
    this.deckCards()
      .filter((c) => c.card.supertype === 'Pokémon' && c.card.stage === 'BASIC')
      .reduce((sum, c) => sum + c.quantity, 0),
  );

  readonly aceSpecCount = computed(() =>
    this.deckCards()
      .filter((c) => c.card.subtypes?.some((s) => s.toUpperCase() === 'ACE SPEC'))
      .reduce((sum, c) => sum + c.quantity, 0),
  );

  /** Cartas (no Energía básica) que superen las 4 copias por nombre. */
  private readonly overLimitNames = computed(() => {
    const nameCounts = new Map<string, number>();
    for (const dc of this.deckCards()) {
      if (!dc?.card) continue;
      if (dc.card.supertype === 'Energy' && dc.card.subtypes?.some((s) => s.toLowerCase() === 'basic')) continue;
      nameCounts.set(dc.card.name, (nameCounts.get(dc.card.name) ?? 0) + dc.quantity);
    }
    return [...nameCounts.entries()].filter(([, total]) => total > 4).map(([name]) => name);
  });

  /** Distribución por supertipo para el desglose. */
  readonly breakdown = computed(() => {
    const counts = { pokemon: 0, trainer: 0, energy: 0 };
    for (const c of this.deckCards()) {
      if (c.card.supertype === 'Pokémon') counts.pokemon += c.quantity;
      else if (c.card.supertype === 'Trainer') counts.trainer += c.quantity;
      else if (c.card.supertype === 'Energy') counts.energy += c.quantity;
    }
    return counts;
  });

  readonly requirements = computed<Requirement[]>(() => [
    {
      label: 'Exactamente 60 cartas',
      detail: `${this.totalCards()}/60`,
      met: this.totalCards() === 60,
    },
    {
      label: 'Al menos 1 Pokémon Básico',
      detail: `${this.basicPokemonCount()} básico${this.basicPokemonCount() === 1 ? '' : 's'}`,
      met: this.basicPokemonCount() >= 1,
    },
    {
      label: 'Máx. 4 copias por carta',
      detail: this.overLimitNames().length === 0 ? 'OK' : `${this.overLimitNames().length} excedida(s)`,
      met: this.totalCards() > 0 && this.overLimitNames().length === 0,
    },
  ]);

  readonly allMet = computed(() => this.requirements().every((r) => r.met));
  readonly metCount = computed(() => this.requirements().filter((r) => r.met).length);
}
