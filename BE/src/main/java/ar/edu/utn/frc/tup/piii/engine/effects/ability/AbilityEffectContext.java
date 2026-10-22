package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

public class AbilityEffectContext {

    private final String matchId;
    private final String playerId;
    private final PokemonInPlay sourcePokemon;
    private final GameEventPublisher publisher;
    private final PlayerBoard sourceBoard;
    private final PlayerBoard opponentBoard;
    private final GameBoard board;
    private final TurnPhase currentPhase;

    private int flipHeads;
    private int flipTails;
    private String sourceTargetId;
    private String targetId;

    public AbilityEffectContext(String matchId, String playerId, PokemonInPlay sourcePokemon,
                                 GameBoard board, PlayerBoard sourceBoard, PlayerBoard opponentBoard,
                                 GameEventPublisher publisher, TurnPhase currentPhase) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.sourcePokemon = sourcePokemon;
        this.board = board;
        this.sourceBoard = sourceBoard;
        this.opponentBoard = opponentBoard;
        this.publisher = publisher;
        this.currentPhase = currentPhase;
    }

    public String getMatchId() { return matchId; }
    public String getPlayerId() { return playerId; }
    public PokemonInPlay getSourcePokemon() { return sourcePokemon; }
    public GameEventPublisher getPublisher() { return publisher; }
    public PlayerBoard getSourceBoard() { return sourceBoard; }
    public PlayerBoard getOpponentBoard() { return opponentBoard; }
    public GameBoard getBoard() { return board; }
    public TurnPhase getCurrentPhase() { return currentPhase; }

    public int getFlipHeads() { return flipHeads; }
    public void setFlipHeads(int flipHeads) { this.flipHeads = flipHeads; }
    public int getFlipTails() { return flipTails; }
    public void setFlipTails(int flipTails) { this.flipTails = flipTails; }

    public String getSourceTargetId() { return sourceTargetId; }
    public void setSourceTargetId(String sourceTargetId) { this.sourceTargetId = sourceTargetId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}
