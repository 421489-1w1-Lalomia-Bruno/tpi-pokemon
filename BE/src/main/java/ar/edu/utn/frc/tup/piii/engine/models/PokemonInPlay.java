package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PokemonInPlay {

    private PokemonCard card;
    private Integer currentHp;
    private Integer damageCounters;
    private List<EnergyCard> attachedEnergy;
    private TrainerCard tool;
    @JsonDeserialize(contentAs = SpecialCondition.class)
    private Set<SpecialCondition> specialConditions;
    private boolean isActive;

    public PokemonInPlay() {
        this.currentHp = 0;
        this.damageCounters = 0;
        this.attachedEnergy = new ArrayList<>();
        this.specialConditions = new HashSet<>();
    }

    public PokemonInPlay(PokemonCard card) {
        this();
        this.card = card;
        this.currentHp = card.getHp();
    }

    public PokemonCard getCard() { return card; }
    public void setCard(PokemonCard card) { this.card = card; }

    public Integer getCurrentHp() { return currentHp; }
    public void setCurrentHp(Integer currentHp) { this.currentHp = currentHp; }

    public Integer getDamageCounters() { return damageCounters; }
    public void setDamageCounters(Integer damageCounters) { this.damageCounters = damageCounters; }

    public List<EnergyCard> getAttachedEnergy() { return attachedEnergy; }
    public void setAttachedEnergy(List<EnergyCard> attachedEnergy) { this.attachedEnergy = attachedEnergy; }

    public TrainerCard getTool() { return tool; }
    public void setTool(TrainerCard tool) { this.tool = tool; }

    public Set<SpecialCondition> getSpecialConditions() { return specialConditions; }
    public void setSpecialConditions(Set<SpecialCondition> specialConditions) { this.specialConditions = specialConditions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public List<EnergyType> getAvailableEnergyPool() {
        return attachedEnergy.stream()
                .filter(energy -> !energy.isWildcard())
                .flatMap(energy -> {
                    List<EnergyType> p = energy.getProvidedEnergies();
                    return p != null ? p.stream() : Stream.empty();
                })
                .collect(Collectors.toList());
    }

    public int getWildcardCount() {
        return (int) attachedEnergy.stream().filter(EnergyCard::isWildcard).count();
    }

    public void takeDamage(int damage) {
        int hpLoss = Math.max(damage, 0);
        this.damageCounters += hpLoss / 10;
        this.currentHp = Math.max(this.currentHp - hpLoss, 0);
    }

    public boolean isKnockedOut() {
        return this.currentHp <= 0;
    }
}
