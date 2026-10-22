import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { Ability, TurnContextModel } from '../../../../shared/models/game.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';

@Component({
  selector: 'app-action-panel',
  imports: [ButtonComponent],
  templateUrl: './action-panel.html',
  styleUrl: './action-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActionPanelComponent {
  readonly turnContext = input<TurnContextModel | null>(null);
  readonly isMyTurn = input(false);
  readonly hasActive = input(false);
  readonly hasEnergyInHand = input(false);
  readonly hasValidEvolution = input(false);
  readonly hasTrainerInHand = input(false);
  readonly activeAbilities = input<Ability[]>([]);
  readonly canUseAbility = input(false);
  readonly myTimeMs = input(0);
  readonly opponentTimeMs = input(0);
  readonly abandonmentCountdownMs = input(0);
  readonly lastTrainerName = input<string | null>(null);

  readonly passTurn = output<void>();
  readonly attack = output<void>();
  readonly evolve = output<void>();
  readonly retreat = output<void>();
  readonly attachEnergy = output<void>();
  readonly surrender = output<void>();
  readonly playTrainer = output<void>();
  readonly useAbility = output<void>();

  formatTime(ms: number): string {
    const totalSec = Math.max(0, Math.floor(ms / 1000));
    const min = Math.floor(totalSec / 60);
    const sec = totalSec % 60;
    return `${min}:${sec.toString().padStart(2, '0')}`;
  }

  onEvolve(): void {
    this.evolve.emit();
  }

  onRetreat(): void {
    this.retreat.emit();
  }

  onAttack(): void {
    this.attack.emit();
  }

  onPassTurn(): void {
    this.passTurn.emit();
  }

  onAttachEnergy(): void {
    this.attachEnergy.emit();
  }
}
