import { Component, ChangeDetectionStrategy, input, output, signal } from '@angular/core';
import { PokemonInPlayModel, TrainerCardModel } from '../../../../shared/models/game.model';
import { trainerEffectEs } from '../../../../shared/utils/trainer-text';

@Component({
  selector: 'app-trainer-selector',
  templateUrl: './trainer-selector.html',
  styleUrl: './trainer-selector.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TrainerSelectorComponent {
  readonly show = input(false);
  readonly trainers = input<TrainerCardModel[]>([]);
  readonly targets = input<PokemonInPlayModel[]>([]);

  readonly trainerSelected = output<number>();
  readonly targetSelected = output<{ cardId: string; targetId: string }>();
  readonly cancel = output<void>();

  readonly selectedTrainer = signal<TrainerCardModel | null>(null);
  readonly selectedTargetId = signal<string | null>(null);

  readonly effectEs = trainerEffectEs;

  onSelectTrainer(index: number): void {
    const trainer = this.trainers()[index];
    if (!trainer) return;
    if (trainer.subtype === 'TOOL') {
      this.selectedTrainer.set(trainer);
      this.selectedTargetId.set(null);
    } else {
      this.trainerSelected.emit(index);
    }
  }

  onSelectTarget(targetId: string): void {
    const trainer = this.selectedTrainer();
    if (!trainer) return;
    this.selectedTargetId.set(targetId);
    this.targetSelected.emit({ cardId: trainer.id, targetId });
    this.selectedTrainer.set(null);
    this.selectedTargetId.set(null);
  }

  onBack(): void {
    this.selectedTrainer.set(null);
    this.selectedTargetId.set(null);
  }
}
