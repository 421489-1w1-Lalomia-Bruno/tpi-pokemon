package ar.edu.utn.frc.tup.piii.dtos.match;

import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de representación de una partida para REST.
 */
@Data
public class MatchDTO {

    private UUID id;
    private String player1Id;
    private String player1Username;
    private String player2Id;
    private String player2Username;
    private UUID player1DeckId;
    private UUID player2DeckId;
    private MatchStatus status;
    private String winnerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
