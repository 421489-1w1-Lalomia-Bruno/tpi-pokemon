package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class MatchSnapshot {

    private UUID matchId;
    private String player1Id;
    private String player2Id;
    private UUID player1DeckId;
    private UUID player2DeckId;
    private String currentPlayerId;
    private String firstPlayerId;
    private int turnNumber;
    private MatchStatus status;
    private String winnerId;
    private GameBoard board;
    private TurnContext turnContext;

    /** Tiempo restante en milisegundos para cada jugador (default 40 min c/u). */
    private long player1TimeRemainingMs;
    private long player2TimeRemainingMs;

    // ── Sorteo inicial cara/cruz ──────────────────────────────────────────────
    /** Jugador elegido al azar para llamar cara o cruz (null fuera del sorteo). */
    private String coinCallerId;
    /** True mientras se espera que el llamador elija cara o cruz. */
    private boolean awaitingCoinCall;
    /** Elección hecha por el llamador: "HEADS" o "TAILS" (null hasta elegir). */
    private String coinCall;
    /** Resultado real de la moneda: "HEADS" o "TAILS" (null hasta tirarla). */
    private String coinFlipResult;
}
