export type MatchStatus = 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED' | 'ABANDONED';

export interface MatchModel {
  id: string;
  player1Id: string;
  player1Username?: string;
  player2Id: string | null;
  player2Username?: string;
  status: MatchStatus;
  winnerId: string | null;
  createdAt: string;
  abandonmentAtMs?: number;
}

export interface CreateMatchModel {
  player1DeckId: string;
}

export interface JoinMatchModel {
  deckId: string;
}
