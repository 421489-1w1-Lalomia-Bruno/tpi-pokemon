package ar.edu.utn.frc.tup.piii.dtos.deck;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeckCardEntryDTO {

    @NotBlank
    private String cardId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
