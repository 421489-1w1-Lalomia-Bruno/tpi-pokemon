import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'ghost'
  | 'danger'
  | 'action-attack'
  | 'action-evolve'
  | 'action-retreat'
  | 'action-energy'
  | 'action-pass'
  | 'action-trainer'
  | 'action-ability';

export type ButtonSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  templateUrl: './button.html',
  styleUrl: './button.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-variant]': 'variant()',
    '[attr.data-size]': 'size()',
    '[attr.data-loading]': 'loading() ? "" : undefined',
    '[attr.data-disabled]': 'disabled() ? "" : undefined',
  },
})
export class ButtonComponent {
  variant = input<ButtonVariant>('primary');
  size = input<ButtonSize>('md');
  disabled = input(false);
  loading = input(false);
  type = input<'button' | 'submit'>('button');
}
