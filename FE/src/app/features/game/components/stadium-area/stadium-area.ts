import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { TrainerCardModel } from '../../../../shared/models/game.model';
import { parseTrainerEffect } from '../../../../shared/utils/trainer-text';

@Component({
  selector: 'app-stadium-area',
  templateUrl: './stadium-area.html',
  styleUrl: './stadium-area.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StadiumAreaComponent {
  readonly stadium = input<TrainerCardModel | null>(null);
  readonly parseEffect = parseTrainerEffect;
}
