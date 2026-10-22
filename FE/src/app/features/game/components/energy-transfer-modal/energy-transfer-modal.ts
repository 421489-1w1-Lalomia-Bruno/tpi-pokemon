import { Component, ChangeDetectionStrategy, input, output, signal, computed } from '@angular/core';
import { EnergyType, PokemonInPlayModel } from '../../../../shared/models/game.model';
import { CardComponent } from '../../../../shared/components/card/card';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { mapPokemonInPlayToCardModel } from '../../../../shared/utils/card-mapper';
import { CardModel } from '../../../../shared/models/card.model';

@Component({
  selector: 'app-energy-transfer-modal',
  imports: [CardComponent, ButtonComponent],
  templateUrl: './energy-transfer-modal.html',
  styleUrl: './energy-transfer-modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnergyTransferModalComponent {
  readonly pokemonList = input<PokemonInPlayModel[]>([]);
  readonly energyType = input<EnergyType>('FAIRY');
  readonly transfer = output<{ sourceId: string; targetId: string }>();
  readonly cancel = output<void>();

  readonly selectedSource = signal<string | null>(null);
  readonly selectedTarget = signal<string | null>(null);

  readonly sourcePokemon = computed<PokemonInPlayModel | null>(() => {
    const id = this.selectedSource();
    if (!id) return null;
    return this.pokemonList().find(p => p.card.id === id) ?? null;
  });

  readonly targetPokemon = computed<PokemonInPlayModel | null>(() => {
    const id = this.selectedTarget();
    if (!id) return null;
    return this.pokemonList().find(p => p.card.id === id) ?? null;
  });

  readonly canTransfer = computed(() => {
    return this.selectedSource() !== null && this.selectedTarget() !== null
      && this.selectedSource() !== this.selectedTarget();
  });

  readonly sourcesWithEnergy = computed<PokemonInPlayModel[]>(() => {
    const type = this.energyType();
    return this.pokemonList().filter(p =>
      p.attachedEnergy.some(e => e.energyType === type || e.providedEnergies?.includes(type))
    );
  });

  toCardModel(pip: PokemonInPlayModel): CardModel {
    return mapPokemonInPlayToCardModel(pip);
  }

  onTransfer(): void {
    const source = this.selectedSource();
    const target = this.selectedTarget();
    if (source && target && source !== target) {
      this.transfer.emit({ sourceId: source, targetId: target });
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
