package ar.edu.utn.frc.tup.piii.dtos.card;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class CardDTO {
    private String id;
    private String externalId;
    private String name;
    private String supertype;
    private List<String> subtypes;
    private Integer hp;
    private Set<EnergyType> types;
    private PokemonStage stage;
    private String evolvesFrom;
    private List<AttackDTO> attacks;
    private List<AbilityDTO> abilities;
    private String weaknessType;
    private String resistanceType;
    private Integer retreatCost;
    private String imageUrl;
    private String rarity;
    private String cardNumber;
    private String setId;
    private List<String> rules;
}
