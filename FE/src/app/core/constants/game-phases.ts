export const GAME_PHASES = {
  WAITING: 'WAITING',
  SETUP: 'SETUP',
  ACTIVE: 'ACTIVE',
  FINISHED: 'FINISHED',
} as const;

export type GamePhase = typeof GAME_PHASES[keyof typeof GAME_PHASES];