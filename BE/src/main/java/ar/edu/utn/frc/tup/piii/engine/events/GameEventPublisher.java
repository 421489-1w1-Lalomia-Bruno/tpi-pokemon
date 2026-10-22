package ar.edu.utn.frc.tup.piii.engine.events;

import ar.edu.utn.frc.tup.piii.entities.GameEventEntity;
import ar.edu.utn.frc.tup.piii.repositories.GameEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GameEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameEventRepository gameEventRepository;

    public void publishEvent(GameEvent event) {
        GameEventEntity entity = new GameEventEntity();
        entity.setMatchId(UUID.fromString(event.getMatchId()));
        entity.setPlayerId(event.getPlayerId());
        entity.setEventType(event.getType());
        entity.setDescription(event.getDescription());
        gameEventRepository.save(entity);

        applicationEventPublisher.publishEvent(event);
    }

    public void publish(GameEventType type, String matchId, String playerId, String description) {
        GameEvent event = new GameEvent(type, matchId, playerId, description, LocalDateTime.now());
        publishEvent(event);
    }

    public void publish(GameEventType type, String matchId, String playerId, String description,
                        String source, String target, Integer value) {
        GameEvent event = new GameEvent(type, matchId, playerId, description, LocalDateTime.now(),
                                        source, target, value);
        publishEvent(event);
    }
}
