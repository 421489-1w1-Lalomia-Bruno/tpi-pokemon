package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;

public class FinishedState implements MatchState {

    private final String winnerId;

    public FinishedState(String winnerId) {
        this.winnerId = winnerId;
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.FINISHED;
    }

    @Override
    public void enter(GameEngine engine, String matchId) {
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot != null) {
            snapshot.setWinnerId(winnerId);
            snapshot.setStatus(MatchStatus.FINISHED);
            engine.saveSnapshot(matchId, snapshot);
            engine.setMatchWinner(matchId, winnerId);
            engine.updatePlayerStats(matchId, winnerId);
        }
        GameEventPublisher ep = engine.getEventPublisher();
        if (ep != null) {
            ep.publish(GameEventType.GAME_FINISHED, matchId, winnerId,
                    "Game finished. Winner: " + winnerId);
        }
    }

    @Override
    public void exit(GameEngine engine, String matchId) {
    }

    @Override
    public void handleAction(GameEngine engine, GameActionMessage action, String playerId) {
        throw new InvalidActionException("Game is already finished. No actions allowed.");
    }
}