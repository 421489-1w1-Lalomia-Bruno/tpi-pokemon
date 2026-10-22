package ar.edu.utn.frc.tup.piii.engine.events;

import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GameEvent {

    private GameEventType type;
    private String matchId;
    private String playerId;
    private String description;
    private LocalDateTime timestamp;
    private String source;
    private String target;
    private Integer value;
    private GameCard card;

    public GameEvent() {}

    public GameEvent(GameEventType type, String matchId, String playerId, String description, LocalDateTime timestamp) {
        this.type = type;
        this.matchId = matchId;
        this.playerId = playerId;
        this.description = description;
        this.timestamp = timestamp;
    }

    public GameEvent(GameEventType type, String matchId, String playerId, String description, LocalDateTime timestamp,
                     String source, String target, Integer value) {
        this.type = type;
        this.matchId = matchId;
        this.playerId = playerId;
        this.description = description;
        this.timestamp = timestamp;
        this.source = source;
        this.target = target;
        this.value = value;
    }
}
