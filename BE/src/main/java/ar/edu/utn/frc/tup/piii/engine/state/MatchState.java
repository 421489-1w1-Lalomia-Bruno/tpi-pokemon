package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;

public interface MatchState {

    GamePhase getPhase();

    void handleAction(GameEngine engine, GameActionMessage action, String playerId);

    void enter(GameEngine engine, String matchId);

    void exit(GameEngine engine, String matchId);
}