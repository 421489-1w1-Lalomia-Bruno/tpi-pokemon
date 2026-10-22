package ar.edu.utn.frc.tup.piii.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionDisconnectListenerTest {

    @Mock
    private GameWebSocketHandler gameWebSocketHandler;

    @Mock
    private SessionDisconnectEvent event;

    private SessionDisconnectListener listener;

    @BeforeEach
    void setUp() {
        listener = new SessionDisconnectListener(gameWebSocketHandler);
    }

    @Test
    void handleSessionDisconnect_delegatesToHandler() {
        when(event.getSessionId()).thenReturn("session-1");

        listener.handleSessionDisconnect(event);

        verify(gameWebSocketHandler).handleDisconnect("session-1");
    }
}
