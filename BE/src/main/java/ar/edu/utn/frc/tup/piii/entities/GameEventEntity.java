package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "game_events")
@Getter
@Setter
public class GameEventEntity extends BaseEntity {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", length = 100)
    private String playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private GameEventType eventType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;
}
