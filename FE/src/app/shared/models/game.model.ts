import { MatchStatus } from './match.model';

// ─────────────────────────────────────────────────────────────────────────────
// Enums del engine (modelados como union types)
// ─────────────────────────────────────────────────────────────────────────────

export type CardType = 'POKEMON' | 'ENERGY' | 'TRAINER';

export type EnergyType =
  | 'FIRE'
  | 'WATER'
  | 'GRASS'
  | 'LIGHTNING'
  | 'PSYCHIC'
  | 'FIGHTING'
  | 'DARKNESS'
  | 'METAL'
  | 'FAIRY'
  | 'DRAGON'
  | 'COLORLESS';

export type PokemonStage = 'BASIC' | 'STAGE1' | 'STAGE2' | 'EX' | 'MEGA';

export type TrainerSubtype = 'ITEM' | 'SUPPORTER' | 'STADIUM' | 'TOOL';

export type AbilityType = 'POKE_BODY' | 'POKE_POWER';

export type SpecialCondition = 'ASLEEP' | 'BURNED' | 'CONFUSED' | 'PARALYZED' | 'POISONED';

export type TurnPhase = 'DRAW' | 'ACTIONS' | 'ATTACK' | 'BETWEEN_TURNS';

// ─────────────────────────────────────────────────────────────────────────────
// Cartas en juego (GameCard polimórfico, discriminado por `type`)
// ─────────────────────────────────────────────────────────────────────────────

export interface Attack {
  name: string;
  cost: EnergyType[];
  damage: number;
  effectText: string | null;
}

export interface Ability {
  name: string;
  type: AbilityType;
  effectText: string | null;
}

export interface PokemonCardModel {
  type: 'POKEMON';
  id: string;
  name: string;
  hp: number;
  stage: PokemonStage;
  types: EnergyType[];
  evolvesFrom: string | null;
  attacks: Attack[];
  abilities: Ability[];
  weaknessType: string | null;
  resistanceType: string | null;
  retreatCost: number | null;
  // El BE aún no expone getter de imageUrl; queda opcional hasta que lo agregue.
  imageUrl?: string | null;
}

export interface EnergyCardModel {
  type: 'ENERGY';
  id: string;
  name: string;
  energyType: EnergyType;
  special: boolean;
  wildcard?: boolean;
  providedEnergies: EnergyType[];
  imageUrl?: string | null;
}

export interface TrainerCardModel {
  type: 'TRAINER';
  id: string;
  name: string;
  subtype: TrainerSubtype;
  effect: string | null;
  imageUrl?: string | null;
}

/** Carta genérica en cualquier zona. Discriminada por `type`. */
export type GameCardModel = PokemonCardModel | EnergyCardModel | TrainerCardModel | HiddenCardModel;

export interface HiddenCardModel {
  type: 'HIDDEN';
  id: string;
  name: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Estructura del tablero
// ─────────────────────────────────────────────────────────────────────────────

export interface PokemonInPlayModel {
  card: PokemonCardModel;
  currentHp: number;
  damageCounters: number;
  attachedEnergy: EnergyCardModel[];
  tool: TrainerCardModel | null;
  specialConditions: SpecialCondition[];
  active: boolean;
}

export interface PlayerBoardModel {
  playerId: string;
  activePokemon: PokemonInPlayModel | null;
  bench: PokemonInPlayModel[];
  hand: GameCardModel[];
  deck: GameCardModel[];
  discardPile: GameCardModel[];
  prizeCards: GameCardModel[];
  prizeCardsTaken: number;
  pendingPrizeCount: number;
  revealedCards: GameCardModel[] | null;
  hasPendingReveal: boolean;
  pendingMultiCoinTargets: number;
  pendingMultiCoinEnergyType: string | null;
}

export interface GameBoardModel {
  player1Board: PlayerBoardModel;
  player2Board: PlayerBoardModel;
  sharedStadium: TrainerCardModel | null;
}

export interface TurnContextModel {
  currentPlayerId: string;
  phase: TurnPhase;
  firstTurn: boolean;
  hasDrawn: boolean;
  hasPlayedSupporter: boolean;
  hasPlayedStadium: boolean;
  hasAttachedEnergy: boolean;
  hasEvolved: boolean;
  hasUsedAbility: boolean;
  hasRetreated: boolean;
  turnStartTimeMs: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Snapshot de la partida (fuente de verdad enviada por el socket)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Snapshot completo de la partida que el backend envía por el socket.
 * Refleja el contrato de `MatchSnapshot` del engine.
 *
 * Nota anti-cheat: cuando el backend implemente el enmascaramiento (Semana 5),
 * las cartas ocultas del oponente (mano, mazo, premios) llegarán ofuscadas.
 */
export interface MatchSnapshotModel {
  matchId: string;
  player1Id: string;
  player2Id: string | null;
  player1DeckId: string | null;
  player2DeckId: string | null;
  currentPlayerId: string | null;
  firstPlayerId: string | null;
  turnNumber: number;
  status: MatchStatus;
  winnerId: string | null;
  board: GameBoardModel | null;
  turnContext: TurnContextModel | null;
  player1TimeRemainingMs: number;
  player2TimeRemainingMs: number;
  abandonmentAtMs?: number;
  // Sorteo inicial cara/cruz.
  coinCallerId?: string | null;
  awaitingCoinCall?: boolean;
  coinCall?: 'HEADS' | 'TAILS' | null;
  coinFlipResult?: 'HEADS' | 'TAILS' | null;
}

// ─────────────────────────────────────────────────────────────────────────────
// Protocolo de mensajería WebSocket
// ─────────────────────────────────────────────────────────────────────────────

export type GameActionType =
  | 'READY'
  | 'PLACE_POKEMON'
  | 'CONFIRM_SETUP'
  | 'CALL_COIN'
  | 'ATTACH_ENERGY'
  | 'PLAY_TRAINER'
  | 'EVOLVE'
  | 'RETREAT'
  | 'ATTACK'
  | 'USE_ABILITY'
  | 'PASS_TURN'
  | 'SET_ACTIVE'
  | 'SURRENDER'
  | 'TAKE_PRIZE'
  | 'RESOLVE_REVEAL'
  | 'SELECT_TARGET';

export type GameEventType =
  | 'MULLIGAN_EXECUTED'
  | 'CARD_DRAWN'
  | 'POKEMON_PLACED'
  | 'ENERGY_ATTACHED'
  | 'TRAINER_PLAYED'
  | 'POKEMON_EVOLVED'
  | 'POKEMON_RETREATED'
  | 'ATTACK_EXECUTED'
  | 'ATTACK_FAILED'
  | 'POKEMON_KNOCKED_OUT'
  | 'PRIZE_TAKEN'
  | 'PRIZE_NEEDED'
  | 'BETWEEN_TURNS_PROCESSED'
  | 'SPECIAL_CONDITION_APPLIED'
  | 'SPECIAL_CONDITION_REMOVED'
  | 'COIN_FLIP_RESULT'
  | 'REVEAL_CARDS'
  | 'MULTI_COIN_FLIP'
  | 'TURN_CHANGED'
  | 'PHASE_CHANGED'
  | 'GAME_FINISHED'
  | 'ABILITY_USED';

export type GameStateUpdateType = 'STATE_UPDATE' | 'EVENT' | 'ERROR' | 'NOTIFICATION';

export type GameActionPayload = Record<string, string | number | string[] | undefined>;

export interface GameActionMessage {
  matchId: string;
  type: GameActionType;
  payload: GameActionPayload;
  timestamp: string;
}

export interface GameEvent {
  type: GameEventType;
  playerId: string;
  description: string;
  timestamp: string;
  source?: string;
  target?: string;
  value?: number;
  card?: GameCardModel;
}

export interface GameStateUpdateMessage {
  type: GameStateUpdateType;
  state?: MatchSnapshotModel;
  event?: GameEvent;
  error?: string;
  notification?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Perspectiva del tablero (vista del jugador local vs oponente)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Vista de un lado del tablero desde la perspectiva de un jugador concreto.
 * Permite que cada cliente renderice su lado como "propio" sin importar si es
 * player1 o player2.
 */
export interface PlayerPerspective {
  playerId: string;
  board: PlayerBoardModel | null;
  isCurrentTurn: boolean;
  isWinner: boolean;
}
