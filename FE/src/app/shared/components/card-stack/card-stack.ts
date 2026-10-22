import { Component, ChangeDetectionStrategy, input } from '@angular/core';

export type StackType = 'deck' | 'discard' | 'prize';

@Component({
  selector: 'app-card-stack',
  templateUrl: './card-stack.html',
  styleUrl: './card-stack.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-type]': 'stackType()',
    '[attr.data-empty]': 'count() === 0 ? "" : undefined',
  },
})
export class CardStackComponent {
  count = input.required<number>();
  label = input('');
  stackType = input<StackType>('deck');
}
