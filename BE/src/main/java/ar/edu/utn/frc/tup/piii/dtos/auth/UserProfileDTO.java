package ar.edu.utn.frc.tup.piii.dtos.auth;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDTO extends UserDTO {
    private List<DeckDTO> decks;
}
