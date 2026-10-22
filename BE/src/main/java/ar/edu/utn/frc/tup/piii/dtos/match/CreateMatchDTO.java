package ar.edu.utn.frc.tup.piii.dtos.match;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para crear una nueva partida (lobby).
 */
@Data
public class CreateMatchDTO {

    @NotBlank
    private String player1DeckId;
}
