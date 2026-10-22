package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.AbilityType;

public class Ability {
    private String name;
    private AbilityType type;
    private String effectText;

    public Ability() {}

    public Ability(String name, AbilityType type, String effectText) {
        this.name = name;
        this.type = type;
        this.effectText = effectText;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AbilityType getType() { return type; }
    public void setType(AbilityType type) { this.type = type; }

    public String getEffectText() { return effectText; }
    public void setEffectText(String effectText) { this.effectText = effectText; }
}
