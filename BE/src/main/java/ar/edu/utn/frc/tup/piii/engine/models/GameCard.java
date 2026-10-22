package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.CardType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PokemonCard.class, name = "POKEMON"),
    @JsonSubTypes.Type(value = EnergyCard.class, name = "ENERGY"),
    @JsonSubTypes.Type(value = TrainerCard.class, name = "TRAINER"),
    @JsonSubTypes.Type(value = HiddenCard.class, name = "HIDDEN")
})
public interface GameCard {
    String getId();
    void setId(String id);
    String getName();
    CardType getType();
}
