import { Component, ChangeDetectionStrategy, computed, input, output, inject, OnDestroy } from '@angular/core';
import { CdkDrag, CdkDragPlaceholder, CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
import { CardComponent } from '../../../../shared/components/card/card';
import { CardModel } from '../../../../shared/models/card.model';
import { GameCardModel, PlayerBoardModel, TurnPhase } from '../../../../shared/models/game.model';
import { mapGameCardToCardModel } from '../../../../shared/utils/card-mapper';
import { CardZoomService } from '../../../../shared/components/card-zoom/card-zoom.service';

function canDragCard(card: GameCardModel, hasAttachedEnergy: boolean,
                     playableCardIds: Set<string> | null,
                     myBoard: PlayerBoardModel | null, oppBoard: PlayerBoardModel | null): boolean {
  if (card.type === 'POKEMON') {
    return card.stage === 'BASIC' || card.stage === 'EX';
  }
  if (card.type === 'ENERGY') {
    return !hasAttachedEnergy;
  }
  if (card.type === 'TRAINER') {
    if (playableCardIds && myBoard) {
      return playableCardIds.has(card.id);
    }
    return true;
  }
  return false;
}

@Component({
  selector: 'app-hand-area',
  imports: [CdkDropList, CdkDrag, CdkDragPlaceholder, CardComponent],
  templateUrl: './hand-area.html',
  styleUrl: './hand-area.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HandAreaComponent implements OnDestroy {
  private readonly cardZoom = inject(CardZoomService);

  readonly cards = input<GameCardModel[]>([]);
  readonly isMyTurn = input(false);
  readonly phase = input<TurnPhase | null>(null);
  readonly hasAttachedEnergy = input(false);
  readonly playableCardIds = input<Set<string> | null>(null);
  readonly myBoard = input<PlayerBoardModel | null>(null);
  readonly oppBoard = input<PlayerBoardModel | null>(null);

  readonly cardDropped = output<CdkDragDrop<GameCardModel[]>>();
  readonly dragStarted = output<void>();

  readonly canDrag = computed(() => this.isMyTurn() && this.phase() === 'ACTIONS');

  private _wasDragging = false;
  private dragTimeout: ReturnType<typeof setTimeout> | null = null;

  cardDraggable(card: GameCardModel): boolean {
    return canDragCard(card, this.hasAttachedEnergy(), this.playableCardIds(), this.myBoard(), this.oppBoard());
  }

  toCardModel(card: GameCardModel): CardModel {
    return mapGameCardToCardModel(card);
  }

  onDragStarted(): void {
    this._wasDragging = true;
    this.dragStarted.emit();
  }

  onDragEnded(): void {
    if (this.dragTimeout !== null) clearTimeout(this.dragTimeout);
    this.dragTimeout = setTimeout(() => { this._wasDragging = false; }, 50);
  }

  ngOnDestroy(): void {
    if (this.dragTimeout !== null) clearTimeout(this.dragTimeout);
  }

  onCardClick(card: GameCardModel): void {
    if (this._wasDragging) {
      this._wasDragging = false;
      return;
    }
    this.cardZoom.open(this.toCardModel(card));
  }
}
