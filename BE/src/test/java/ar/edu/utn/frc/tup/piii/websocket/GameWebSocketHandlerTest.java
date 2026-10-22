package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameWebSocketHandlerTest {

    @Mock
    private GameEngine gameEngine;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameStatePersistenceService persistenceService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> messageCaptor;

    private GameWebSocketHandler handler;
    private static final String SESSION_ID = "session-1";
    private static final String MATCH_ID = "match-123";
    private static final String PLAYER_ID = "player-1";
    private static final String OTHER_PLAYER_ID = "player-2";

    @BeforeEach
    void setUp() {
        handler = new GameWebSocketHandler(gameEngine, objectMapper, messagingTemplate, persistenceService);
    }

    @Test
    void handleConnect_snapshotExists_sendsSanitizedState() {
        Principal principal = () -> PLAYER_ID;
        MatchSnapshot snapshot = new MatchSnapshot();
        MatchSnapshot sanitized = new MatchSnapshot();
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(persistenceService.sanitizeSnapshotForPlayer(snapshot, PLAYER_ID)).thenReturn(sanitized);

        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> msg = messageCaptor.getValue();
        assertEquals("STATE_UPDATE", msg.get("type"));
        assertSame(sanitized, msg.get("state"));
    }

    @Test
    void handleConnect_noSnapshot_doesNotSend() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);

        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleConnect_storesSessionMappings() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);

        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();
        MatchSnapshot result = new MatchSnapshot();
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
    }

    @Test
    void handleConnect_snapshotThrowsException_sessionMappingsStillStored() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenThrow(new RuntimeException("Engine error"));

        assertThrows(RuntimeException.class, () -> handler.handleConnect(SESSION_ID, MATCH_ID, principal));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();
        MatchSnapshot result = new MatchSnapshot();
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
    }

    @Test
    void handleDisconnect_cleansUpMappings() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        handler.handleDisconnect(SESSION_ID);
    }

    @Test
    void handleDisconnect_emptySessions_removesMatchEntry() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        handler.handleDisconnect(SESSION_ID);

        MatchSnapshot snapshot = new MatchSnapshot();
        handler.broadcastToMatch(MATCH_ID, snapshot);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleDisconnect_nonEmptySessions_keepsMatchEntry() {
        Principal principalP1 = () -> PLAYER_ID;
        Principal principalP2 = () -> OTHER_PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect("session-p1", MATCH_ID, principalP1);
        handler.handleConnect("session-p2", MATCH_ID, principalP2);

        handler.handleDisconnect("session-p1");

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setPlayer2Id(OTHER_PLAYER_ID);
        MatchSnapshot sanitized = new MatchSnapshot();
        when(persistenceService.sanitizeSnapshotForPlayer(snapshot, OTHER_PLAYER_ID)).thenReturn(sanitized);

        handler.broadcastToMatch(MATCH_ID, snapshot);
        verify(messagingTemplate).convertAndSendToUser(eq(OTHER_PLAYER_ID), eq("/queue/match"), any());
    }

    @Test
    void handleDisconnect_unknownSession_doesNotThrow() {
        handler.handleDisconnect("unknown-session");
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleAction_validAction_processesAndBroadcasts() {
        Principal principal = () -> PLAYER_ID;
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();
        MatchSnapshot result = new MatchSnapshot();
        result.setPlayer1Id(PLAYER_ID);
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(result);
        when(persistenceService.sanitizeSnapshotForPlayer(result, PLAYER_ID)).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> msg = messageCaptor.getValue();
        assertEquals("STATE_UPDATE", msg.get("type"));
    }

    @Test
    void handleAction_identityMismatch_returnsError() {
        Principal principal = () -> PLAYER_ID;
        Principal otherPrincipal = () -> "player-999";
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();

        handler.handleAction(SESSION_ID, action, otherPrincipal);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> msg = messageCaptor.getValue();
        assertEquals("ERROR", msg.get("type"));
        assertEquals("Session identity mismatch", msg.get("error"));
        verify(gameEngine, never()).processAction(anyString(), anyString(), anyString(), any());
    }

    @Test
    void handleAction_nullMatchId_registersSessionAndDoesNotBroadcast() {
        Principal principal = () -> PLAYER_ID;
        GameActionMessage action = GameActionMessage.builder()
                .type(GameActionType.READY).payload(Map.of()).build();

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine, never()).processAction(anyString(), anyString(), anyString(), any());
    }

    @Test
    void handleAction_processReturnsNull_doesNotBroadcast() {
        Principal principal = () -> PLAYER_ID;
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(null);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void handleAction_payloadGetterFails_fallsBackToObjectMapper() {
        GameActionMessage action = mock(GameActionMessage.class);
        when(action.getMatchId()).thenReturn(MATCH_ID);
        when(action.getType()).thenReturn(GameActionType.READY);
        when(action.getPayload()).thenThrow(new RuntimeException("Getter failed"));

        Map<String, Object> payloadMap = Map.of("key", "value");
        when(objectMapper.convertValue(action, Map.class)).thenReturn(payloadMap);

        Principal principal = () -> PLAYER_ID;
        MatchSnapshot result = new MatchSnapshot();
        result.setPlayer1Id(PLAYER_ID);
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", payloadMap)).thenReturn(result);
        when(persistenceService.sanitizeSnapshotForPlayer(result, PLAYER_ID)).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", payloadMap);
    }

    @Test
    void handleAction_reflectionFailsCompletely_fallsBackToObjectMapper() {
        GameActionMessage action = mock(GameActionMessage.class);
        when(action.getMatchId()).thenThrow(new RuntimeException("Reflection failed"));

        Map<String, Object> fallback = Map.of(
                "matchId", MATCH_ID,
                "type", "READY",
                "payload", Map.of()
        );
        when(objectMapper.convertValue(action, Map.class)).thenReturn(fallback);

        Principal principal = () -> PLAYER_ID;
        MatchSnapshot result = new MatchSnapshot();
        result.setPlayer1Id(PLAYER_ID);
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(result);
        when(persistenceService.sanitizeSnapshotForPlayer(result, PLAYER_ID)).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
    }

    @Test
    void handleAction_newSessionViaAction_registersMappings() {
        Principal principal = () -> PLAYER_ID;
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID).type(GameActionType.READY).payload(Map.of()).build();
        MatchSnapshot result = new MatchSnapshot();
        result.setPlayer1Id(PLAYER_ID);
        when(gameEngine.processAction(MATCH_ID, PLAYER_ID, "READY", Map.of())).thenReturn(result);
        when(persistenceService.sanitizeSnapshotForPlayer(result, PLAYER_ID)).thenReturn(result);

        handler.handleAction(SESSION_ID, action, principal);

        verify(gameEngine).processAction(MATCH_ID, PLAYER_ID, "READY", Map.of());
        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), any());
    }

    @Test
    void broadcastToMatch_sendsSanitizedSnapshotToEachPlayer() {
        Principal principalP1 = () -> PLAYER_ID;
        Principal principalP2 = () -> OTHER_PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect("session-p1", MATCH_ID, principalP1);
        handler.handleConnect("session-p2", MATCH_ID, principalP2);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setPlayer1Id(PLAYER_ID);
        snapshot.setPlayer2Id(OTHER_PLAYER_ID);
        MatchSnapshot sanitized1 = new MatchSnapshot();
        MatchSnapshot sanitized2 = new MatchSnapshot();

        when(persistenceService.sanitizeSnapshotForPlayer(snapshot, PLAYER_ID)).thenReturn(sanitized1);
        when(persistenceService.sanitizeSnapshotForPlayer(snapshot, OTHER_PLAYER_ID)).thenReturn(sanitized2);

        handler.broadcastToMatch(MATCH_ID, snapshot);

        verify(persistenceService).sanitizeSnapshotForPlayer(snapshot, PLAYER_ID);
        verify(persistenceService).sanitizeSnapshotForPlayer(snapshot, OTHER_PLAYER_ID);
        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), any());
        verify(messagingTemplate).convertAndSendToUser(eq(OTHER_PLAYER_ID), eq("/queue/match"), any());
    }

    @Test
    void broadcastToMatch_noSessions_logsWarning() {
        MatchSnapshot snapshot = new MatchSnapshot();
        handler.broadcastToMatch(MATCH_ID, snapshot);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendToPlayer_unknownSession_doesNothing() {
        handler.sendToPlayer("unknown-session", Map.of("type", "TEST"));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void sendToPlayer_knownSession_sendsMessage() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        handler.sendToPlayer(SESSION_ID, Map.of("type", "TEST"));

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), eq(Map.of("type", "TEST")));
    }

    @Test
    void sendToPlayer_nullMessage_sendsNull() {
        Principal principal = () -> PLAYER_ID;
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);
        handler.handleConnect(SESSION_ID, MATCH_ID, principal);

        handler.sendToPlayer(SESSION_ID, null);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_ID), eq("/queue/match"), eq(null));
    }
}
