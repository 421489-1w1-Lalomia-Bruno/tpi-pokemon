export const CARD_SUPERTYPES = {
  POKEMON: 'Pokémon',
  TRAINER: 'Trainer',
  ENERGY: 'Energy',
} as const;

export type CardSupertype = typeof CARD_SUPERTYPES[keyof typeof CARD_SUPERTYPES];