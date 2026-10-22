package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import lombok.Data;

@Data
public class CardFilterDTO {
    private String name;
    private EnergyType type;
    private PokemonStage stage;
    private String supertype;
    private String setId;
    private Integer page;
    private Integer size;
}
