package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "matches")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchEntity extends BaseEntity {

    @Column(name = "player1_id", nullable = false, length = 100)
    private String player1Id;

    @Column(name = "player2_id", length = 100)
    private String player2Id;

    @Column(name = "player1_deck_id")
    private UUID player1DeckId;

    @Column(name = "player2_deck_id")
    private UUID player2DeckId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private MatchStatus status;

    @Column(name = "winner_id", length = 100)
    private String winnerId;
}
