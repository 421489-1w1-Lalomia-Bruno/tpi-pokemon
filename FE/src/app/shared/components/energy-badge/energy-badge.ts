import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { EnergyType } from '../../models/game.model';

export type BadgeVariant = 'circle' | 'chip' | 'pill';
export type BadgeSize = 'sm' | 'md';

const ENERGY_LABELS: Record<string, string> = {
  FIRE: 'Fuego', WATER: 'Agua', GRASS: 'Planta',
  LIGHTNING: 'Rayo', PSYCHIC: 'Psíquico', FIGHTING: 'Lucha',
  DARKNESS: 'Oscuridad', METAL: 'Metal', FAIRY: 'Hada',
  DRAGON: 'Dragón', COLORLESS: 'Incoloro',
};

@Component({
  selector: 'app-energy-badge',
  templateUrl: './energy-badge.html',
  styleUrl: './energy-badge.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-type]': 'type()',
    '[attr.data-variant]': 'variant()',
    '[attr.data-size]': 'size()',
    '[attr.title]': 'label()',
  },
})
export class EnergyBadgeComponent {
  type = input<EnergyType | null>(null);
  variant = input<BadgeVariant>('circle');
  size = input<BadgeSize>('md');

  label = computed(() => {
    const t = this.type();
    return t ? (ENERGY_LABELS[t] ?? t) : '';
  });

  firstChar = computed(() => {
    const t = this.type();
    return t ? t.charAt(0) : '';
  });
}
