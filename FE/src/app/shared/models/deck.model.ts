import { CardModel } from './card.model';

export interface DeckCardModel {
  card: CardModel;
  quantity: number;
}

export interface DeckModel {
  id: string;
  name: string;
  creatorId: string | null;
  source: 'USER' | 'PREDETERMINED';
  description?: string;
  cards: DeckCardModel[];
  totalCards: number;
  isValid: boolean;
  validationErrors: string[];
}

export interface DeckCardEntryModel {
  cardId: string;
  quantity: number;
}

export interface CreateDeckModel {
  name: string;
  cards: DeckCardEntryModel[];
}

export interface DeckValidationResultModel {
  isValid: boolean;
  errors: string[];
  totalCards: number;
  basicPokemonCount: number;
  cardNameCounts: Record<string, number>;
}
