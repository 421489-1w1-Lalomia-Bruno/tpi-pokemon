import { Component, ChangeDetectionStrategy, computed, input, output } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { CardModel } from '../../models/card.model';
import { EnergyType } from '../../models/game.model';
import { HoloTiltDirective } from '../../directives/holo-tilt.directive';

type HoloTier = 'none' | 'holo' | 'ultra';

@Component({
  selector: 'app-card',
  imports: [NgOptimizedImage, HoloTiltDirective],
  templateUrl: './card.html',
  styleUrl: './card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CardComponent {
  readonly cardModel = input<CardModel | null>(null);
  readonly energy = input<EnergyType[]>([]);
  /** Tamaño compacto para grillas densas / desactiva metadatos extensos. */
  readonly compact = input(false);
  /** Permite apagar el tilt (p. ej. en listas muy largas o miniaturas). */
  readonly interactive = input(true);
  /** Emite la carta al hacer click o presionar Enter/Espacio. */
  readonly activated = output<CardModel>();

  onActivate(event?: Event): void {
    const card = this.cardModel();
    if (!card) return;
    event?.preventDefault();
    this.activated.emit(card);
  }

  /** Nivel de "premium" según la rareza: define la intensidad del holo. */
  readonly holoTier = computed<HoloTier>(() => {
    const rarity = (this.cardModel()?.rarity ?? '').toLowerCase();
    if (!rarity) return 'none';
    if (/(ex|ultra|secret|full|rainbow|shiny|gold)/.test(rarity)) return 'ultra';
    if (rarity.includes('holo')) return 'holo';
    return 'none';
  });

  readonly primaryType = computed<string>(() => this.cardModel()?.types?.[0] ?? 'COLORLESS');
}
