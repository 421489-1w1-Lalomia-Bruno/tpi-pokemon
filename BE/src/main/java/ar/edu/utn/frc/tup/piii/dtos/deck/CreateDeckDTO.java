package ar.edu.utn.frc.tup.piii.dtos.deck;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateDeckDTO {

    @NotBlank
    private String name;

    @NotEmpty
    private List<@Valid DeckCardEntryDTO> cards;
}
