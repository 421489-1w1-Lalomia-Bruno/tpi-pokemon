package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private GameEngine gameEngine;

    private GameEventListener listener;

    @Captor
    private ArgumentCaptor<Map<String, Object>> messageCaptor;

    private static final String MATCH_ID = "match-123";
    private static final String PLAYER_1 = "player1";
    private static final String PLAYER_2 = "player2";
    private static final LocalDateTime NOW = LocalDateTime.now();

    private MatchSnapshot snapshot;

    @BeforeEach
    void setUp() {
        listener = new GameEventListener(messagingTemplate, gameEngine);
        snapshot = createSnapshot(PLAYER_1, PLAYER_2);
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
    }

    private GameEvent createEvent(GameEventType type, String playerId) {
        return new GameEvent(type, MATCH_ID, playerId, "test event", NOW);
    }

    private MatchSnapshot createSnapshot(String p1, String p2) {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(UUID.randomUUID());
        snapshot.setPlayer1Id(p1);
        snapshot.setPlayer2Id(p2);
        return snapshot;
    }

    @Test
    void handleGameEvent_eventTypeNull_sendsNotification() {
        GameEvent event = createEvent(null, PLAYER_1);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals(event, message.get("event"));
        assertEquals("NOTIFICATION", message.get("type"));
        assertEquals("Game event", message.get("notification"));
    }

    @ParameterizedTest
    @EnumSource(value = GameEventType.class, names = {
        "CARD_DRAWN", "POKEMON_PLACED", "ENERGY_ATTACHED", "TRAINER_PLAYED",
        "POKEMON_EVOLVED", "POKEMON_RETREATED", "ATTACK_EXECUTED",
        "BETWEEN_TURNS_PROCESSED", "GAME_FINISHED"
    })
    void handleGameEvent_stateUpdateEvents_sendsStateUpdate(GameEventType type) {
        GameEvent event = createEvent(type, PLAYER_1);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("EVENT", message.get("type"));
        assertFalse(message.containsKey("notification"));
    }

    @ParameterizedTest
    @EnumSource(value = GameEventType.class, names = {
        "POKEMON_KNOCKED_OUT", "PRIZE_TAKEN", "SPECIAL_CONDITION_APPLIED",
        "SPECIAL_CONDITION_REMOVED"
    })
    void handleGameEvent_eventTypeEvents_sendsEvent(GameEventType type) {
        GameEvent event = createEvent(type, PLAYER_1);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("EVENT", message.get("type"));
        assertFalse(message.containsKey("notification"));
    }

    @Test
    void handleGameEvent_turnChanged_sendsNotificationWithEventType() {
        GameEvent event = new GameEvent(GameEventType.TURN_CHANGED, MATCH_ID, PLAYER_1, null, NOW);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("NOTIFICATION", message.get("type"));
        assertEquals("Turn changed", message.get("notification"));
    }

    @Test
    void handleGameEvent_phaseChanged_sendsNotificationWithEventType() {
        GameEvent event = new GameEvent(GameEventType.PHASE_CHANGED, MATCH_ID, PLAYER_1, null, NOW);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("NOTIFICATION", message.get("type"));
        assertEquals("Phase changed", message.get("notification"));
    }

    @ParameterizedTest
    @EnumSource(value = GameEventType.class, names = {
        "MATCH_CREATED", "PLAYER_JOINED", "MATCH_STARTED",
        "MULLIGAN_EXECUTED", "READY_CONFIRMED"
    })
    void handleGameEvent_defaultEventType_sendsNotificationWithPrefix(GameEventType type) {
        GameEvent event = createEvent(type, PLAYER_1);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("NOTIFICATION", message.get("type"));
        assertEquals("Game event: " + type.name(), message.get("notification"));
    }

    @Test
    void handleGameEvent_nullPlayerIdWithSnapshot_sendsToPlayer1() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, null);
        MatchSnapshot snapshot = createSnapshot(PLAYER_1, null);
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), any(Map.class));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void handleGameEvent_nullPlayerIdWithBothPlayers_sendsToBoth() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, null);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), any(Map.class));
        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_2), eq("/queue/match"), any(Map.class));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void handleGameEvent_nullPlayerIdNoSnapshot_logsWarningAndReturns() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, null);
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(null);

        listener.handleGameEvent(event);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void handleGameEvent_withPlayerId_sendsToAllSnapshotPlayers() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, "specific-player");

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), any(Map.class));
        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_2), eq("/queue/match"), any(Map.class));
        verifyNoMoreInteractions(messagingTemplate);
    }

    @Test
    void sendEventToPlayer_withNotification_includesNotificationField() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, PLAYER_1);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals(event, message.get("event"));
        assertEquals("EVENT", message.get("type"));
        assertEquals(2, message.size());
    }

    @Test
    void sendEventToPlayer_nullNotification_omitsNotificationField() {
        GameEvent event = new GameEvent(GameEventType.POKEMON_KNOCKED_OUT, MATCH_ID, PLAYER_2, null, NOW);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_2), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        assertEquals("EVENT", message.get("type"));
        assertFalse(message.containsKey("notification"));
    }

    @Test
    void handleGameEvent_verifyMessageContainsFullEvent() {
        GameEvent event = new GameEvent(GameEventType.ATTACK_EXECUTED, MATCH_ID, PLAYER_1, "Pikachu used Thunderbolt", NOW);

        listener.handleGameEvent(event);

        verify(messagingTemplate).convertAndSendToUser(eq(PLAYER_1), eq("/queue/match"), messageCaptor.capture());
        Map<String, Object> message = messageCaptor.getValue();
        GameEvent sentEvent = (GameEvent) message.get("event");
        assertEquals(GameEventType.ATTACK_EXECUTED, sentEvent.getType());
        assertEquals(MATCH_ID, sentEvent.getMatchId());
        assertEquals(PLAYER_1, sentEvent.getPlayerId());
        assertEquals("Pikachu used Thunderbolt", sentEvent.getDescription());
        assertEquals(NOW, sentEvent.getTimestamp());
    }

    @Test
    void handleGameEvent_nullPlayerIdSnapshotWithPlayer1Only_skipPlayer2() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, null);
        MatchSnapshot snapshot = createSnapshot(PLAYER_1, null);
        when(gameEngine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        listener.handleGameEvent(event);

        verify(messagingTemplate, times(1)).convertAndSendToUser(anyString(), anyString(), any(Map.class));
    }

    @Test
    void handleGameEvent_withPlayerId_doesNotCallEngine() {
        GameEvent event = createEvent(GameEventType.CARD_DRAWN, PLAYER_1);

        listener.handleGameEvent(event);

        verify(gameEngine).getSnapshot(MATCH_ID);
        verifyNoMoreInteractions(gameEngine);
    }

    @Test
    void handleGameEvent_nullPlayerIdSnapshotWithBothPlayers_correctMessages() {
        GameEvent event = createEvent(GameEventType.POKEMON_KNOCKED_OUT, null);

        listener.handleGameEvent(event);

        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/match"), messageCaptor.capture());
        var allMessages = messageCaptor.getAllValues();
        for (var msg : allMessages) {
            assertEquals("EVENT", msg.get("type"));
            assertFalse(msg.containsKey("notification"));
        }
    }
}
