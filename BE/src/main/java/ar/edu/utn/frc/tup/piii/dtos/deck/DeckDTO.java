package ar.edu.utn.frc.tup.piii.dtos.deck;

import lombok.Data;

import java.util.List;

@Data
public class DeckDTO {

    private String id;
    private String name;
    private String creatorId;
    private String source;
    private String description;
    private List<DeckCardDTO> cards;
    private Integer totalCards;
    private Boolean isValid;
    private List<String> validationErrors;
}
