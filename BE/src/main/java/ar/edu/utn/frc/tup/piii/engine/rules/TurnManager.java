package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;

public class TurnManager {

    private final String matchId;
    private TurnContext context;
    private String currentPlayerId;
    private String opponentId;
    private boolean isFirstTurnOfGame;
    private int turnCount;
    private StatusEffectManager effectManager;
    private GameEventPublisher eventPublisher;

    public TurnManager(String matchId, String player1Id, String player2Id, TurnContext initialContext,
                       StatusEffectManager effectManager, GameEventPublisher eventPublisher) {
        this.matchId = matchId;
        this.currentPlayerId = initialContext.getCurrentPlayerId() != null ? initialContext.getCurrentPlayerId() : player1Id;
        this.opponentId = this.currentPlayerId.equals(player1Id) ? player2Id : player1Id;
        this.context = initialContext;
        this.isFirstTurnOfGame = true;
        this.turnCount = 0;
        this.effectManager = effectManager;
        this.eventPublisher = eventPublisher;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public void setPhase(TurnPhase phase) {
        context.setPhase(phase);
    }

    public TurnPhase getPhase() {
        return context.getPhase();
    }

    public TurnContext getContext() {
        return context;
    }

    public boolean canDraw(String playerId) {
        return playerId.equals(currentPlayerId)
                && context.getPhase() == TurnPhase.DRAW
                && !context.isHasDrawn();
    }

    public boolean canPerformAction(String playerId, ar.edu.utn.frc.tup.piii.websocket.GameActionType action) {
        if (!playerId.equals(currentPlayerId)) {
            return false;
        }
        return context.getPhase() == TurnPhase.ACTIONS;
    }

    public void executeDrawPhase(GameEngine engine, GameBoard board) {
        if (isFirstTurnOfGame) {
            isFirstTurnOfGame = false;
            setPhase(TurnPhase.ACTIONS);
            turnCount++;
            return;
        }

        PlayerBoard playerBoard = getCurrentPlayerBoard(board);
        if (playerBoard.getDeck().isEmpty()) {
            engine.declareLoser(matchId, currentPlayerId, "Deck is empty, cannot draw");
            return;
        }

        playerBoard.getHand().add(playerBoard.getDeck().remove(0));
        context.setHasDrawn(true);

        if (eventPublisher != null) {
            eventPublisher.publish(GameEventType.TURN_CHANGED, matchId, currentPlayerId,
                    "Turn changed to turn " + turnCount);
        }

        setPhase(TurnPhase.ACTIONS);
        turnCount++;
    }

    public void executeAttackPhase(GameEngine engine) {
        setPhase(TurnPhase.ATTACK);
    }

    public void executeBetweenTurns(GameEngine engine, GameBoard board) {
        setPhase(TurnPhase.BETWEEN_TURNS);
        PlayerBoard currentBoard = getCurrentPlayerBoard(board);
        if (currentBoard != null && currentBoard.getActivePokemon() != null) {
            effectManager.processBetweenTurns(currentBoard.getActivePokemon(), board, matchId,
                currentPlayerId, eventPublisher,
                SpecialCondition.ASLEEP, SpecialCondition.PARALYZED);
        }

        if (eventPublisher != null) {
            eventPublisher.publish(GameEventType.BETWEEN_TURNS_PROCESSED, matchId, currentPlayerId,
                    "Between turns processed");
        }
    }

    public void executeStartOfTurnEffects(GameEngine engine, GameBoard board) {
        PlayerBoard currentBoard = getCurrentPlayerBoard(board);
        if (currentBoard != null && currentBoard.getActivePokemon() != null) {
            effectManager.processBetweenTurns(currentBoard.getActivePokemon(), board, matchId,
                currentPlayerId, eventPublisher,
                SpecialCondition.POISONED, SpecialCondition.BURNED);
        }
    }

    public void nextTurn(MatchSnapshot snapshot) {
        long now = System.currentTimeMillis();
        long elapsed = now - context.getTurnStartTimeMs();
        if (elapsed > 0) {
            if (currentPlayerId.equals(snapshot.getPlayer1Id())) {
                snapshot.setPlayer1TimeRemainingMs(Math.max(0, snapshot.getPlayer1TimeRemainingMs() - elapsed));
            } else {
                snapshot.setPlayer2TimeRemainingMs(Math.max(0, snapshot.getPlayer2TimeRemainingMs() - elapsed));
            }
        }

        context.getCompletedFirstTurns().add(currentPlayerId);

        String temp = currentPlayerId;
        currentPlayerId = opponentId;
        opponentId = temp;

        context.setCurrentPlayerId(currentPlayerId);
        context.setPhase(TurnPhase.DRAW);
        context.setFirstTurn(false);
        context.setHasDrawn(false);
        context.setHasPlayedSupporter(false);
        context.setHasPlayedStadium(false);
        context.setHasEvolved(false);
        context.setHasAttachedEnergy(false);
        context.setHasUsedAbility(false);
        context.setHasRetreated(false);
        context.setTurnStartTimeMs(now);
    }

    public boolean isFirstTurnOfGame() {
        return isFirstTurnOfGame;
    }

    public void setFirstTurnOfGame(boolean firstTurnOfGame) {
        isFirstTurnOfGame = firstTurnOfGame;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public String getMatchId() {
        return matchId;
    }

    private PlayerBoard getCurrentPlayerBoard(GameBoard board) {
        if (board.getPlayer1Board().getPlayerId().equals(currentPlayerId)) {
            return board.getPlayer1Board();
        }
        return board.getPlayer2Board();
    }

    private PlayerBoard getOpponentBoard(GameBoard board) {
        if (board.getPlayer1Board().getPlayerId().equals(opponentId)) {
            return board.getPlayer1Board();
        }
        return board.getPlayer2Board();
    }
}
