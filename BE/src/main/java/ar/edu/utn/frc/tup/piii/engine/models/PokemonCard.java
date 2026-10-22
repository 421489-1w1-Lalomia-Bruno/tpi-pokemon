package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.CardType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;

import java.util.List;
import java.util.Set;

public class PokemonCard implements  GameCard {
    private String id;
    private String name;
    private Integer hp;
    private PokemonStage stage;
    private Set<EnergyType> types;
    private String evolvesFrom;
    private List<Attack> attacks;
    private List<Ability> abilities;
    private String weaknessType;
    private String resistanceType;
    private Integer retreatCost;
    private String imageUrl;

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    @Override
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public CardType getType() { return CardType.POKEMON; }

    public Integer getHp() { return hp; }
    public void setHp(Integer hp) { this.hp = hp; }

    public PokemonStage getStage() { return stage; }
    public void setStage(PokemonStage stage) { this.stage = stage; }

    public Set<EnergyType> getTypes() { return types; }
    public void setTypes(Set<EnergyType> types) { this.types = types; }

    public String getEvolvesFrom() { return evolvesFrom; }
    public void setEvolvesFrom(String evolvesFrom) { this.evolvesFrom = evolvesFrom; }

    public List<Attack> getAttacks() { return attacks; }
    public void setAttacks(List<Attack> attacks) { this.attacks = attacks; }

    public List<Ability> getAbilities() { return abilities; }
    public void setAbilities(List<Ability> abilities) { this.abilities = abilities; }

    public String getWeaknessType() { return weaknessType; }
    public void setWeaknessType(String weaknessType) { this.weaknessType = weaknessType; }

    public String getResistanceType() { return resistanceType; }
    public void setResistanceType(String resistanceType) { this.resistanceType = resistanceType; }

    public Integer getRetreatCost() { return retreatCost; }
    public void setRetreatCost(Integer retreatCost) { this.retreatCost = retreatCost; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

}
