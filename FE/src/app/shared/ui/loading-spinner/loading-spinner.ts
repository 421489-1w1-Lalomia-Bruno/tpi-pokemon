import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type SpinnerSize = 'sm' | 'md' | 'lg';
export type SpinnerVariant = 'spinner' | 'skeleton';

@Component({
  selector: 'app-loading-spinner',
  templateUrl: './loading-spinner.html',
  styleUrl: './loading-spinner.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-size]': 'size()',
    '[attr.data-variant]': 'variant()',
    '[attr.role]': '"status"',
    '[attr.aria-label]': 'text() || "Cargando"',
  },
})
export class LoadingSpinnerComponent {
  size = input<SpinnerSize>('md');
  variant = input<SpinnerVariant>('spinner');
  text = input('');
}
