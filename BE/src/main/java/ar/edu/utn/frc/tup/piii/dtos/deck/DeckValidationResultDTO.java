package ar.edu.utn.frc.tup.piii.dtos.deck;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeckValidationResultDTO {

    private Boolean isValid;
    private List<String> errors;
    private Integer totalCards;
    private Integer basicPokemonCount;
    private Map<String, Integer> cardNameCounts;
}
