package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WaitingState implements MatchState {

    private static final Logger log = LoggerFactory.getLogger(WaitingState.class);
    private final Map<String, Set<String>> readyPlayers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> transitioning = new ConcurrentHashMap<>();

    @Override
    public GamePhase getPhase() {
        return GamePhase.WAITING;
    }

    @Override
    public void enter(GameEngine engine, String matchId) {
        readyPlayers.put(matchId, ConcurrentHashMap.newKeySet());
        try {
            GameEventPublisher ep = engine.getEventPublisher();
            if (ep != null) {
                ep.publish(GameEventType.MATCH_CREATED, matchId, null, "Match created, waiting for players");
            }
        } catch (Exception e) {
            log.warn("Failed to publish MATCH_CREATED event for match {}: {}", matchId, e.getMessage());
        }
    }

    @Override
    public void exit(GameEngine engine, String matchId) {
        readyPlayers.remove(matchId);
    }

    @Override
    public void handleAction(GameEngine engine, GameActionMessage action, String playerId) {
        String matchId = action.getMatchId();

        if (action.getType() != GameActionType.READY) {
            throw new InvalidActionException(
                    "Only READY action is allowed in WAITING phase. Received: " + action.getType());
        }

        Set<String> ready = readyPlayers.get(matchId);
        if (ready == null) {
            throw new InvalidActionException("Match not found in WAITING state: " + matchId);
        }

        if (ready.contains(playerId)) {
            return;
        }

        ready.add(playerId);

        if (ready.size() >= 2 && transitioning.putIfAbsent(matchId, true) == null) {
            try {
                GameEventPublisher ep = engine.getEventPublisher();
                if (ep != null) {
                    ep.publish(GameEventType.MATCH_STARTED, matchId, null, "Both players ready, match starting");
                }
            } catch (Exception e) {
                log.warn("Failed to publish MATCH_STARTED event for match {}: {}", matchId, e.getMessage());
            }
            engine.transitionTo(matchId, new SetupState());
        }
    }
}