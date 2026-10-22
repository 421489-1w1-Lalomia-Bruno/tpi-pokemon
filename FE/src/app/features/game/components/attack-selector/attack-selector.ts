import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { Attack } from '../../../../shared/models/game.model';
import { EnergyTypePipe } from '../../../../shared/pipes/energy-type.pipe';

@Component({
  selector: 'app-attack-selector',
  imports: [EnergyTypePipe],
  templateUrl: './attack-selector.html',
  styleUrl: './attack-selector.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AttackSelectorComponent {
  readonly show = input(false);
  readonly attacks = input<Attack[]>([]);

  readonly attackSelected = output<number>();
  readonly cancel = output<void>();
}
