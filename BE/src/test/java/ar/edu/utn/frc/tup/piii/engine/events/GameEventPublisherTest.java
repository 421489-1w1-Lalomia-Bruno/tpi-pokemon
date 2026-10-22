package ar.edu.utn.frc.tup.piii.engine.events;

import ar.edu.utn.frc.tup.piii.entities.GameEventEntity;
import ar.edu.utn.frc.tup.piii.repositories.GameEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEventPublisherTest {

    @Mock
    private GameEventRepository gameEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private GameEventPublisher gameEventPublisher;

    @Captor
    private ArgumentCaptor<GameEventEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    @Test
    void publishEvent_savesAndPublishesEvent() {
        GameEvent event = new GameEvent(
            GameEventType.MATCH_CREATED,
            "550e8400-e29b-41d4-a716-446655440000",
            "player-123",
            "Match created",
            LocalDateTime.of(2026, 6, 16, 10, 0)
        );

        gameEventPublisher.publishEvent(event);

        verify(gameEventRepository).save(entityCaptor.capture());
        GameEventEntity savedEntity = entityCaptor.getValue();
        assertEquals(UUID.fromString(event.getMatchId()), savedEntity.getMatchId());
        assertEquals(event.getPlayerId(), savedEntity.getPlayerId());
        assertEquals(event.getType(), savedEntity.getEventType());
        assertEquals(event.getDescription(), savedEntity.getDescription());

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertSame(event, eventCaptor.getValue());
    }

    @Test
    void publish_createsEventAndDelegates() {
        String matchId = "550e8400-e29b-41d4-a716-446655440000";
        String playerId = "player-456";
        String description = "Player joined";

        gameEventPublisher.publish(GameEventType.PLAYER_JOINED, matchId, playerId, description);

        verify(gameEventRepository).save(entityCaptor.capture());
        GameEventEntity savedEntity = entityCaptor.getValue();
        assertEquals(UUID.fromString(matchId), savedEntity.getMatchId());
        assertEquals(playerId, savedEntity.getPlayerId());
        assertEquals(GameEventType.PLAYER_JOINED, savedEntity.getEventType());
        assertEquals(description, savedEntity.getDescription());

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GameEvent publishedEvent = eventCaptor.getValue();
        assertEquals(GameEventType.PLAYER_JOINED, publishedEvent.getType());
        assertEquals(matchId, publishedEvent.getMatchId());
        assertEquals(playerId, publishedEvent.getPlayerId());
        assertEquals(description, publishedEvent.getDescription());
        assertNotNull(publishedEvent.getTimestamp());
    }

    @Test
    void publish_setsCorrectFieldsOnEvent() {
        String matchId = "550e8400-e29b-41d4-a716-446655440000";
        String playerId = "player-789";
        String description = "Attack executed";

        gameEventPublisher.publish(GameEventType.ATTACK_EXECUTED, matchId, playerId, description);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GameEvent publishedEvent = eventCaptor.getValue();

        assertEquals(GameEventType.ATTACK_EXECUTED, publishedEvent.getType());
        assertEquals(matchId, publishedEvent.getMatchId());
        assertEquals(playerId, publishedEvent.getPlayerId());
        assertEquals(description, publishedEvent.getDescription());
        assertNotNull(publishedEvent.getTimestamp());
        assertTrue(publishedEvent.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(publishedEvent.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
    }
}
