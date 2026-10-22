package ar.edu.utn.frc.tup.piii.dtos.match;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO para unirse a una partida existente.
 */
@Data
public class JoinMatchDTO {

    @NotBlank
    private String deckId;
}
