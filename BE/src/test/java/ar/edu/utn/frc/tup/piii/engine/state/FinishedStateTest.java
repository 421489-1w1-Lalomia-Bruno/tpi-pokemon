package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FinishedStateTest {

    private GameEngine engine;
    private GameEventPublisher eventPublisher;
    private MatchSnapshot snapshot;
    private FinishedState state;
    private static final String MATCH_ID = "match-1";
    private static final String WINNER_ID = "player-winner";

    @BeforeEach
    void setUp() {
        engine = mock(GameEngine.class);
        eventPublisher = mock(GameEventPublisher.class);
        snapshot = mock(MatchSnapshot.class);
        state = new FinishedState(WINNER_ID);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);
    }

    @Test
    void getPhase_returnsFinished() {
        assertEquals(GamePhase.FINISHED, state.getPhase());
    }

    @Test
    void constructor_storesWinnerId() {
        FinishedState s = new FinishedState("custom-winner");
        assertNotNull(s);
    }

    @Test
    void enter_setsWinnerInSnapshotAndDb() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        state.enter(engine, MATCH_ID);

        verify(snapshot).setWinnerId(WINNER_ID);
        verify(snapshot).setStatus(MatchStatus.FINISHED);
        verify(engine).saveSnapshot(MATCH_ID, snapshot);
        verify(engine).setMatchWinner(MATCH_ID, WINNER_ID);
        verify(eventPublisher).publish(eq(GameEventType.GAME_FINISHED), eq(MATCH_ID), eq(WINNER_ID), anyString());
    }

    @Test
    void enter_handlesNullSnapshot() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(null);

        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));

        verify(engine, never()).saveSnapshot(anyString(), any());
        verify(engine, never()).setMatchWinner(anyString(), anyString());
    }

    @Test
    void exit_doesNothing() {
        assertDoesNotThrow(() -> state.exit(engine, MATCH_ID));
    }

    @Test
    void handleAction_throwsForAttack() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .payload(Map.of())
                .build();

        InvalidActionException ex = assertThrows(
                InvalidActionException.class,
                () -> state.handleAction(engine, action, "any-player")
        );
        assertTrue(ex.getMessage().contains("finished"));
    }

    @Test
    void handleAction_throwsForReady() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, "any-player"));
    }

    @Test
    void handleAction_throwsForSurrender() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SURRENDER)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, "any-player"));
    }
}
