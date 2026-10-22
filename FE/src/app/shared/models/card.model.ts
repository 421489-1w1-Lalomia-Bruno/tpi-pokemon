export interface AttackModel {
  name: string;
  cost: string[];
  damage: number;
  effectText: string | null;
}

export interface AbilityModel {
  name: string;
  type: string;
  effectText: string | null;
}

export interface CardModel {
  id: string;
  externalId: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: number | null;
  types: string[];
  stage: string | null;
  evolvesFrom: string | null;
  attacks: AttackModel[];
  abilities: AbilityModel[];
  weaknessType: string | null;
  resistanceType: string | null;
  retreatCost: number | null;
  imageUrl: string;
  rarity: string | null;
  cardNumber: string;
  setId: string;
  rules: string[] | null;
  specialEnergy?: boolean;
}

export interface CardFilterModel {
  name?: string;
  type?: string;
  stage?: string;
  supertype?: string;
  setId?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}