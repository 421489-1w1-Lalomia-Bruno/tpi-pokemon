package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.CardType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.List;

public class EnergyCard implements  GameCard {
    private String id;
    private String name;
    private EnergyType energyType;
    private boolean isSpecial;
    private boolean wildcard;
    private List<EnergyType> providedEnergies;
    private String imageUrl;

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }

    @Override
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public CardType getType() { return CardType.ENERGY; }

    public EnergyType getEnergyType() { return energyType; }
    public void setEnergyType(EnergyType energyType) { this.energyType = energyType; }

    public boolean isSpecial() { return isSpecial; }
    public void setSpecial(boolean special) { isSpecial = special; }

    public boolean isWildcard() { return wildcard; }
    public void setWildcard(boolean wildcard) { this.wildcard = wildcard; }

    public List<EnergyType> getProvidedEnergies() { return providedEnergies; }
    public void setProvidedEnergies(List<EnergyType> providedEnergies) { this.providedEnergies = providedEnergies; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
