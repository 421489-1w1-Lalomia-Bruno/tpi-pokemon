package ar.edu.utn.frc.tup.piii.engine.effects.attack;

import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

public class AttackEffectContext {
    private final String matchId;
    private final String playerId;
    private final PokemonInPlay attacker;
    private final PokemonInPlay defender;
    private final GameBoard board;
    private final PlayerBoard sourceBoard;
    private final PlayerBoard opponentBoard;
    private final GameEventPublisher publisher;
    private final StatusEffectManager statusEffectManager;
    private final String sourceName;
    private final ar.edu.utn.frc.tup.piii.engine.models.AttackContext attackContext;

    public AttackEffectContext(String matchId, String playerId,
                                 PokemonInPlay attacker, PokemonInPlay defender,
                                 GameBoard board, PlayerBoard sourceBoard,
                                 PlayerBoard opponentBoard,
                                 GameEventPublisher publisher,
                                 StatusEffectManager statusEffectManager,
                                 String sourceName,
                                 ar.edu.utn.frc.tup.piii.engine.models.AttackContext attackContext) {
        this.matchId = matchId;
        this.playerId = playerId;
        this.attacker = attacker;
        this.defender = defender;
        this.board = board;
        this.sourceBoard = sourceBoard;
        this.opponentBoard = opponentBoard;
        this.publisher = publisher;
        this.statusEffectManager = statusEffectManager;
        this.sourceName = sourceName;
        this.attackContext = attackContext;
    }

    public String getMatchId() { return matchId; }
    public String getPlayerId() { return playerId; }
    public PokemonInPlay getAttacker() { return attacker; }
    public PokemonInPlay getDefender() { return defender; }
    public GameBoard getBoard() { return board; }
    public PlayerBoard getSourceBoard() { return sourceBoard; }
    public PlayerBoard getOpponentBoard() { return opponentBoard; }
    public GameEventPublisher getPublisher() { return publisher; }
    public StatusEffectManager getStatusEffectManager() { return statusEffectManager; }
    public String getSourceName() { return sourceName; }
    public ar.edu.utn.frc.tup.piii.engine.models.AttackContext getAttackContext() { return attackContext; }
}
