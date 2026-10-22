package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketHandler.class);

    private final GameEngine gameEngine;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameStatePersistenceService gameStatePersistenceService;

    private final Map<String, Set<String>> matchSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionMatch = new ConcurrentHashMap<>();
    private final Map<String, String> sessionPlayer = new ConcurrentHashMap<>();

    public GameWebSocketHandler(GameEngine gameEngine, ObjectMapper objectMapper,
                                 SimpMessagingTemplate messagingTemplate,
                                 GameStatePersistenceService gameStatePersistenceService) {
        this.gameEngine = gameEngine;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.gameStatePersistenceService = gameStatePersistenceService;
    }

    public void handleConnect(String sessionId, String matchId, Principal principal) {
        String playerId = principal.getName();
        log.info("Player {} connecting to match {} (session {})", playerId, matchId, sessionId);

        sessionPlayer.put(sessionId, playerId);
        sessionMatch.put(sessionId, matchId);
        matchSessions.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        MatchSnapshot state = gameEngine.getSnapshot(matchId);
        if (state != null) {
            MatchSnapshot sanitized = sanitizeSnapshotForPlayer(state, playerId);
            sendToPlayer(sessionId, createMessage("STATE_UPDATE", sanitized, "Reconnected to match", null));
        }
    }

    public void handleDisconnect(String sessionId) {
        String playerId = sessionPlayer.get(sessionId);
        String matchId = sessionMatch.get(sessionId);
        log.info("Player {} disconnecting from match {} (session {})", playerId, matchId, sessionId);

        sessionPlayer.remove(sessionId);
        sessionMatch.remove(sessionId);

        if (matchId != null) {
            Set<String> sessions = matchSessions.get(matchId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    matchSessions.remove(matchId);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public void handleAction(String sessionId, GameActionMessage action, Principal principal) {
        String authenticatedPlayerId = principal.getName();
        String matchId;
        String type;
        Map<String, Object> payload;
        try {
            Object matchIdObj = GameActionMessage.class.getMethod("getMatchId").invoke(action);
            matchId = matchIdObj == null ? null : String.valueOf(matchIdObj);
            Object typeObj = GameActionMessage.class.getMethod("getType").invoke(action);
            type = typeObj == null ? null : String.valueOf(typeObj);
            try {
                payload = (Map<String, Object>) GameActionMessage.class.getMethod("getPayload").invoke(action);
            } catch (Exception ex) {
                payload = objectMapper.convertValue(action, Map.class);
            }
        } catch (Exception e) {
            Map<?, ?> map = objectMapper.convertValue(action, Map.class);
            Object m = map.get("matchId");
            matchId = m == null ? null : String.valueOf(m);
            Object t = map.get("type");
            type = t == null ? null : String.valueOf(t);
            payload = (Map<String, Object>) map.get("payload");
        }

        if (matchId == null) {
            sendError(sessionId, "matchId is required");
            return;
        }

        String sessionPlayerId = sessionPlayer.get(sessionId);
        if (sessionPlayerId != null && !sessionPlayerId.equals(authenticatedPlayerId)) {
            log.warn("Session mismatch: session {} claims player {} but principal is {}",
                    sessionId, sessionPlayerId, authenticatedPlayerId);
            sendError(sessionId, "Session identity mismatch");
            return;
        }

        sessionPlayer.put(sessionId, authenticatedPlayerId);
        sessionMatch.put(sessionId, matchId);
        matchSessions.computeIfAbsent(matchId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info("Processing action {} for player {} in match {}", type, authenticatedPlayerId, matchId);

        // Temp diagnostic log
        log.info("[WS] Raw action: class={}, type string={}, matchId={}",
                action.getClass().getSimpleName(), type, matchId);

        MatchSnapshot result;
        try {
            result = gameEngine.processAction(matchId, authenticatedPlayerId, type, payload);
        } catch (Exception e) {
            log.error("Action failed: match={} player={} type={} error={}",
                    matchId, authenticatedPlayerId, type, e.getMessage(), e);
            sendError(sessionId, e.getMessage());
            return;
        }

        log.info("processAction result for match {}: status={}, boardPresent={}, currentPlayer={}",
                matchId,
                result != null ? result.getStatus() : "null",
                result != null && result.getBoard() != null,
                result != null ? result.getCurrentPlayerId() : "null");
        if (result != null) {
            try {
                broadcastToMatch(matchId, result);
            } catch (Exception e) {
                log.warn("Broadcast failed for match {} after action {}: {}", matchId, type, e.getMessage());
            }
        } else {
            log.warn("No snapshot returned for match {} after action {}", matchId, type);
        }
    }

    public void broadcastToMatch(String matchId, MatchSnapshot snapshot) {
        Set<String> sessions = matchSessions.get(matchId);
        if (sessions == null) {
            log.warn("No sessions found for match {}", matchId);
            return;
        }

        log.info("Broadcasting to match {}: status={}, sessions={}", matchId, snapshot.getStatus(), sessions.size());

        for (String sessionId : sessions) {
            String playerId = sessionPlayer.get(sessionId);
            if (playerId == null) continue;

            MatchSnapshot sanitized = sanitizeSnapshotForPlayer(snapshot, playerId);
            if (sanitized == null) continue;
            log.debug("Sending STATE_UPDATE to player {} for match {}: status={}", playerId, matchId, sanitized.getStatus());
            messagingTemplate.convertAndSendToUser(playerId, "/queue/match", createMessage("STATE_UPDATE", sanitized, null, null));
        }
    }

    public void sendToPlayer(String sessionId, Object message) {
        String playerId = sessionPlayer.get(sessionId);
        if (playerId == null) {
            log.warn("No player found for session {}", sessionId);
            return;
        }
        messagingTemplate.convertAndSendToUser(playerId, "/queue/match", message);
    }

    private void sendError(String sessionId, String errorMessage) {
        String playerId = sessionPlayer.get(sessionId);
        if (playerId == null) return;

        messagingTemplate.convertAndSendToUser(playerId, "/queue/match", createMessage("ERROR", null, null, errorMessage));
    }

    private MatchSnapshot sanitizeSnapshotForPlayer(MatchSnapshot snapshot, String playerId) {
        return gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);
    }

    private Object createMessage(String type, MatchSnapshot state, String message, String error) {
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("type", type);
        if (state != null) {
            messageBody.put("state", state);
        }
        if (message != null) {
            messageBody.put("message", message);
        }
        if (error != null) {
            messageBody.put("error", error);
        }
        return messageBody;
    }
}
