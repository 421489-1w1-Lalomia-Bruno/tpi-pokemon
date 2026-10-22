package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "game_states")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStateEntity {

    @Id
    private UUID matchId;

    @Column(name = "current_turn_id", length = 100, nullable = true)
    private String currentTurnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_phase", length = 20)
    private GamePhase gamePhase;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_phase", length = 20, nullable = true)
    private TurnPhase turnPhase;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "board_state_json")
    private String boardStateJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
