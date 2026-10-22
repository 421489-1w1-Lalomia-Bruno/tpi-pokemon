import { Component, ChangeDetectionStrategy, input, output, signal, effect, afterNextRender } from '@angular/core';
import { EnergyCardModel, PokemonInPlayModel } from '../../../../shared/models/game.model';

@Component({
  selector: 'app-energy-attachment',
  templateUrl: './energy-attachment.html',
  styleUrl: './energy-attachment.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnergyAttachmentComponent {
  readonly show = input(false);
  readonly energyCards = input<EnergyCardModel[]>([]);
  readonly targets = input<PokemonInPlayModel[]>([]);

  readonly pairSelected = output<{ cardId: string; targetId: string }>();
  readonly cancel = output<void>();

  readonly selectedEnergyId = signal<string | null>(null);
  readonly selectedTargetId = signal<string | null>(null);

  constructor() {
    afterNextRender(() => {
      effect(() => {
        if (!this.show()) return;
        const energies = this.energyCards();
        const targs = this.targets();
        if (energies.length === 1 && targs.length === 1) {
          this.pairSelected.emit({ cardId: energies[0].id, targetId: targs[0].card.id });
        }
      });
    });
  }

  onSelectEnergy(cardId: string): void {
    this.selectedEnergyId.set(cardId);
    this.tryComplete();
  }

  onSelectTarget(targetId: string): void {
    this.selectedTargetId.set(targetId);
    this.tryComplete();
  }

  private tryComplete(): void {
    const energyId = this.selectedEnergyId();
    const targetId = this.selectedTargetId();
    if (energyId && targetId) {
      this.pairSelected.emit({ cardId: energyId, targetId });
      this.selectedEnergyId.set(null);
      this.selectedTargetId.set(null);
    }
  }
}
