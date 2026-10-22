package ar.edu.utn.frc.tup.piii.dtos.deck;

import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import lombok.Data;

@Data
public class DeckCardDTO {

    private String cardId;
    private String name;
    private Integer quantity;
    private CardDTO card;
}
