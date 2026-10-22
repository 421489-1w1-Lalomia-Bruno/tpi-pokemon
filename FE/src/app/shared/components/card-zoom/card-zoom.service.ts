import { Injectable, signal, computed } from '@angular/core';
import { CardModel } from '../../models/card.model';
import { EnergyCardModel, TrainerCardModel } from '../../models/game.model';

@Injectable({ providedIn: 'root' })
export class CardZoomService {
  private readonly _zoomedCard = signal<CardModel | null>(null);
  private readonly _attachedEnergy = signal<EnergyCardModel[]>([]);
  private readonly _attachedTool = signal<TrainerCardModel | null>(null);

  readonly zoomedCard = this._zoomedCard.asReadonly();
  readonly attachedEnergy = this._attachedEnergy.asReadonly();
  readonly attachedTool = this._attachedTool.asReadonly();
  readonly isOpen = computed(() => this._zoomedCard() !== null);

  open(card: CardModel): void {
    this._zoomedCard.set(card);
    this._attachedEnergy.set([]);
    this._attachedTool.set(null);
  }

  openWithAttachments(card: CardModel, energy: EnergyCardModel[], tool: TrainerCardModel | null): void {
    this._zoomedCard.set(card);
    this._attachedEnergy.set(energy);
    this._attachedTool.set(tool);
  }

  close(): void {
    this._zoomedCard.set(null);
  }
}
