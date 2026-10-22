import { Component, ChangeDetectionStrategy, input, computed } from '@angular/core';
import { SpecialCondition } from '../../models/game.model';

const CONDITION_CONFIG: Record<string, { label: string; color: string }> = {
  ASLEEP:    { label: 'Dormido',  color: 'var(--status-asleep)' },
  BURNED:    { label: 'Quemado',  color: 'var(--status-burned)' },
  CONFUSED:  { label: 'Confuso',  color: 'var(--status-confused)' },
  PARALYZED: { label: 'Paralizado', color: 'var(--status-paralyzed)' },
  POISONED:  { label: 'Envenenado', color: 'var(--status-poisoned)' },
};

@Component({
  selector: 'app-status-indicator',
  templateUrl: './status-indicator.html',
  styleUrl: './status-indicator.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-condition]': 'condition()',
    '[attr.title]': 'config().label',
  },
})
export class StatusIndicatorComponent {
  condition = input.required<SpecialCondition>();
  showLabel = input(false);

  config = computed(() =>
    CONDITION_CONFIG[this.condition()] ?? { label: this.condition(), color: '#666' }
  );
}
