import { CardModel, AttackModel, AbilityModel } from '../models/card.model';
import { EnergyCardModel, EnergyType, GameCardModel, PokemonInPlayModel } from '../models/game.model';

export function mapGameCardToCardModel(card: GameCardModel): CardModel {
  const base: CardModel = {
    id: card.id,
    externalId: card.id,
    name: card.name,
    supertype: '',
    subtypes: [],
    hp: null,
    types: [],
    stage: null,
    evolvesFrom: null,
    attacks: [],
    abilities: [],
    weaknessType: null,
    resistanceType: null,
    retreatCost: null,
    imageUrl: '',
    rarity: null,
    cardNumber: '',
    setId: '',
    rules: null,
    specialEnergy: false,
  };

  if (card.type === 'POKEMON') {
    const attacks: AttackModel[] = (card.attacks ?? []).map((a) => ({
      name: a.name,
      cost: a.cost ?? [],
      damage: a.damage ?? 0,
      effectText: a.effectText ?? null,
    }));
    const abilities: AbilityModel[] = (card.abilities ?? []).map((a) => ({
      name: a.name,
      type: a.type,
      effectText: a.effectText ?? null,
    }));
    return {
      ...base,
      supertype: 'Pokémon',
      subtypes: [card.stage],
      hp: card.hp ?? null,
      types: card.types ?? [],
      stage: card.stage ?? null,
      evolvesFrom: card.evolvesFrom ?? null,
      attacks,
      abilities,
      weaknessType: card.weaknessType ?? null,
      resistanceType: card.resistanceType ?? null,
      retreatCost: card.retreatCost ?? null,
      imageUrl: card.imageUrl ?? '',
    };
  }

  if (card.type === 'ENERGY') {
    return {
      ...base,
      supertype: 'Energy',
      subtypes: card.special ? ['Special'] : ['Basic'],
      types: [card.energyType],
      imageUrl: card.imageUrl ?? '',
      specialEnergy: card.special,
    };
  }

  if (card.type === 'HIDDEN') {
    return {
      ...base,
      supertype: 'Hidden',
      subtypes: [],
      types: [],
      imageUrl: '',
    };
  }

  // Trainer
  return {
    ...base,
    supertype: 'Trainer',
    subtypes: [card.subtype],
    rules: card.effect ? [card.effect] : null,
    imageUrl: card.imageUrl ?? '',
  };
}

export function mapPokemonInPlayToCardModel(pip: PokemonInPlayModel): CardModel {
  return mapGameCardToCardModel(pip.card);
}

export function flatEnergyTypes(attached: EnergyCardModel[]): EnergyType[] {
  return attached.flatMap(e =>
    e.providedEnergies?.length ? e.providedEnergies : [e.energyType].filter((t): t is EnergyType => t != null),
  );
}
