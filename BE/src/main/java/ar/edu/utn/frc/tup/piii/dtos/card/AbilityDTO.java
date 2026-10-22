package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import lombok.Data;

@Data
public class AbilityDTO {
    private String name;
    private AbilityType type;
    private String effectText;
}
