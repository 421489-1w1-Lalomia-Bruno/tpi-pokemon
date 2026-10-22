import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  computed,
  input,
  output,
} from '@angular/core';
import { UpperCasePipe } from '@angular/common';
import { CardModel } from '../../models/card.model';
import { EnergyCardModel, EnergyType, TrainerCardModel } from '../../models/game.model';
import { CardComponent } from '../card/card';
import { trainerEffectEs } from '../../utils/trainer-text';

@Component({
  selector: 'app-card-zoom',
  imports: [CardComponent, UpperCasePipe],
  templateUrl: './card-zoom.html',
  styleUrl: './card-zoom.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardZoomComponent {
  /** Carta a mostrar en grande; si es null el modal no se renderiza. */
  readonly card = input<CardModel | null>(null);
  readonly attachedEnergy = input<EnergyCardModel[]>([]);
  readonly attachedTool = input<TrainerCardModel | null>(null);
  readonly close = output<void>();

  readonly hasAttacks = computed(() => (this.card()?.attacks?.length ?? 0) > 0);
  readonly hasAbilities = computed(() => (this.card()?.abilities?.length ?? 0) > 0);

  /** True cuando la carta mostrada es un Entrenador (Trainer). */
  readonly isTrainer = computed(() => this.card()?.supertype === 'Trainer');

  /** Texto del efecto de la carta Entrenador, traducido al español. */
  readonly trainerEffect = computed<string>(() => {
    const c = this.card();
    if (!c || c.supertype !== 'Trainer') return '';
    return trainerEffectEs(c.name, c.rules?.[0] ?? null);
  });
  /** Slots para representar el costo de retirada con íconos. */
  readonly retreatSlots = computed<number[]>(() => {
    const cost = this.card()?.retreatCost ?? 0;
    return Array.from({ length: cost }, (_, i) => i);
  });

  readonly flatAttachedEnergies = computed<EnergyType[]>(() => {
    return this.attachedEnergy().flatMap(e =>
      e.providedEnergies?.length ? e.providedEnergies : (e.energyType ? [e.energyType] : []),
    );
  });

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.card()) this.close.emit();
  }
}
