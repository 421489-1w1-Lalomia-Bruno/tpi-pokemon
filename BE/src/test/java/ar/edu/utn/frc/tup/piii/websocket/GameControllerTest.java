package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private Principal principal;

    private GameController controller;
    private static final String SESSION_ID = "test-session-id";

    @BeforeEach
    void setUp() {
        controller = new GameController(gameWebSocketHandler);
        when(headerAccessor.getSessionId()).thenReturn(SESSION_ID);
    }

    @Test
    void handleGameAction_delegatesToHandler() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId("match-1")
                .type(GameActionType.ATTACK)
                .payload(Map.of("attackIndex", 0))
                .build();

        controller.handleGameAction(action, headerAccessor, principal);

        verify(gameWebSocketHandler).handleAction(SESSION_ID, action, principal);
    }

    @Test
    void handleConnect_delegatesToHandler() {
        Map<String, Object> payload = Map.of("matchId", "match-1");

        controller.handleConnect(payload, headerAccessor, principal);

        verify(gameWebSocketHandler).handleConnect(SESSION_ID, "match-1", principal);
    }

    @Test
    void handleDisconnect_delegatesToHandler() {
        controller.handleDisconnect(headerAccessor);

        verify(gameWebSocketHandler).handleDisconnect(SESSION_ID);
    }

    @Test
    void handleConnect_nullMatchId_delegatesWithNull() {
        Map<String, Object> payload = Map.of();

        controller.handleConnect(payload, headerAccessor, principal);

        verify(gameWebSocketHandler).handleConnect(SESSION_ID, null, principal);
    }
}
