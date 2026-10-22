package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;

public class TrainerEffectContext {

    private final GameBoard board;
    private final String currentPlayerId;
    private final String matchId;
    private final TrainerCard sourceCard;
    private final String targetId;
    private final GameEventPublisher eventPublisher;
    private final PlayerBoard currentPlayerBoard;
    private final PlayerBoard opponentBoard;

    public TrainerEffectContext(GameBoard board,
                                String currentPlayerId,
                                String matchId,
                                TrainerCard sourceCard,
                                String targetId,
                                GameEventPublisher eventPublisher) {
        this.board = board;
        this.currentPlayerId = currentPlayerId;
        this.matchId = matchId;
        this.sourceCard = sourceCard;
        this.targetId = targetId;
        this.eventPublisher = eventPublisher;

        PlayerBoard p1 = board.getPlayer1Board();
        PlayerBoard p2 = board.getPlayer2Board();
        if (p1 != null && currentPlayerId.equals(p1.getPlayerId())) {
            this.currentPlayerBoard = p1;
            this.opponentBoard = p2;
        } else {
            this.currentPlayerBoard = p2;
            this.opponentBoard = p1;
        }
    }

    public GameBoard getBoard() { return board; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public String getMatchId() { return matchId; }
    public TrainerCard getSourceCard() { return sourceCard; }
    public String getTargetId() { return targetId; }
    public GameEventPublisher getEventPublisher() { return eventPublisher; }
    public PlayerBoard getCurrentPlayerBoard() { return currentPlayerBoard; }
    public PlayerBoard getOpponentBoard() { return opponentBoard; }
}
