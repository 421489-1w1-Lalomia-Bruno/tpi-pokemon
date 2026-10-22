package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class GameController {

    private final GameWebSocketHandler gameWebSocketHandler;

    public GameController(GameWebSocketHandler gameWebSocketHandler) {
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @MessageMapping("/game/action")
    public void handleGameAction(@Payload GameActionMessage action,
                                  SimpMessageHeaderAccessor headerAccessor,
                                  Principal principal) {
        String sessionId = headerAccessor.getSessionId();
        gameWebSocketHandler.handleAction(sessionId, action, principal);
    }

    @MessageMapping("/game/connect")
    public void handleConnect(@Payload Map<String, Object> payload,
                               SimpMessageHeaderAccessor headerAccessor,
                               Principal principal) {
        String sessionId = headerAccessor.getSessionId();
        String matchId = (String) payload.get("matchId");
        gameWebSocketHandler.handleConnect(sessionId, matchId, principal);
    }

    @MessageMapping("/game/disconnect")
    public void handleDisconnect(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        gameWebSocketHandler.handleDisconnect(sessionId);
    }
}
