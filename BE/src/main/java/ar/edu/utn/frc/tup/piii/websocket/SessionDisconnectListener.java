package ar.edu.utn.frc.tup.piii.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class SessionDisconnectListener {

    private static final Logger log = LoggerFactory.getLogger(SessionDisconnectListener.class);

    private final GameWebSocketHandler gameWebSocketHandler;

    public SessionDisconnectListener(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.debug("WebSocket session disconnected: {}", sessionId);
        gameWebSocketHandler.handleDisconnect(sessionId);
    }
}
