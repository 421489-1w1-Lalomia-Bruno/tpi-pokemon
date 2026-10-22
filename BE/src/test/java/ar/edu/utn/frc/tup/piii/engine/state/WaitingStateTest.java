package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WaitingStateTest {

    private GameEngine engine;
    private GameEventPublisher eventPublisher;
    private WaitingState state;
    private static final String MATCH_ID = "match-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";

    @BeforeEach
    void setUp() {
        engine = mock(GameEngine.class);
        eventPublisher = mock(GameEventPublisher.class);
        state = new WaitingState();
        when(engine.getEventPublisher()).thenReturn(eventPublisher);
    }

    @Test
    void getPhase_returnsWaiting() {
        assertEquals(GamePhase.WAITING, state.getPhase());
    }

    @Test
    void enter_initializesState() {
        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));

        verify(eventPublisher).publish(eq(GameEventType.MATCH_CREATED), eq(MATCH_ID), isNull(), anyString());
    }

    @Test
    void exit_cleansUp() {
        state.enter(engine, MATCH_ID);
        assertDoesNotThrow(() -> state.exit(engine, MATCH_ID));
    }

    @Test
    void handleAction_ready_addsPlayer() {
        state.enter(engine, MATCH_ID);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        assertDoesNotThrow(() -> state.handleAction(engine, action, PLAYER_1));
        verify(engine, never()).transitionTo(anyString(), any());
    }

    @Test
    void handleAction_ready_bothPlayers_transitionsToSetup() {
        state.enter(engine, MATCH_ID);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        state.handleAction(engine, action, PLAYER_1);
        state.handleAction(engine, action, PLAYER_2);

        verify(engine).transitionTo(eq(MATCH_ID), isA(SetupState.class));
        verify(eventPublisher).publish(eq(GameEventType.MATCH_STARTED), eq(MATCH_ID), isNull(), anyString());
    }

    @Test
    void handleAction_ready_samePlayerTwice_idempotent() {
        state.enter(engine, MATCH_ID);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        state.handleAction(engine, action, PLAYER_1);
        state.handleAction(engine, action, PLAYER_1);
        state.handleAction(engine, action, PLAYER_2);

        verify(engine, times(1)).transitionTo(eq(MATCH_ID), isA(SetupState.class));
    }

    @Test
    void handleAction_nonReady_throws() {
        state.enter(engine, MATCH_ID);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        InvalidActionException ex = assertThrows(
                InvalidActionException.class,
                () -> state.handleAction(engine, action, PLAYER_1)
        );
        assertTrue(ex.getMessage().contains("READY"));
    }

    @Test
    void handleAction_uninitializedMatch_throws() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId("unknown-match")
                .type(GameActionType.READY)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, PLAYER_1));
    }

    @Test
    void enter_publishFails_doesNotThrow() {
        doThrow(new RuntimeException("Publisher error"))
                .when(eventPublisher).publish(any(), any(), any(), anyString());

        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));
    }

    @Test
    void handleAction_bothReady_publishFails_doesNotThrow() {
        state.enter(engine, MATCH_ID);
        reset(eventPublisher);
        doThrow(new RuntimeException("Publisher error"))
                .when(eventPublisher).publish(any(), any(), any(), anyString());

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        state.handleAction(engine, action, PLAYER_1);
        assertDoesNotThrow(() -> state.handleAction(engine, action, PLAYER_2));

        verify(engine).transitionTo(eq(MATCH_ID), isA(SetupState.class));
    }

    @Test
    void handleAction_bothReady_transitionAlreadyInProgress_noDuplicateTransition() {
        state.enter(engine, MATCH_ID);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        state.handleAction(engine, action, PLAYER_1);
        state.handleAction(engine, action, PLAYER_2);

        // Third ready should not trigger another transition
        state.handleAction(engine, action, PLAYER_1);

        verify(engine, times(1)).transitionTo(eq(MATCH_ID), isA(SetupState.class));
    }
}
