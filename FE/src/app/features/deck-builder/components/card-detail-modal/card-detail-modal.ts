import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CardModel } from '../../../../shared/models/card.model';
import { CardComponent } from '../../../../shared/components/card/card';

@Component({
  selector: 'app-card-detail-modal',
  imports: [CardComponent],
  templateUrl: './card-detail-modal.html',
  styleUrl: './card-detail-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CardDetailModalComponent {
  readonly card = input<CardModel | null>(null);
  readonly close = output<void>();
}
