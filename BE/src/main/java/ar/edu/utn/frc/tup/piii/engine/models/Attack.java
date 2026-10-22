package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.List;

public class Attack {
    private String name;
    private List<EnergyType> cost;
    private int damage;
    private String effectText;

    public Attack() {}

    public Attack(String name, List<EnergyType> cost, int damage, String effectText) {
        this.name = name;
        this.cost = cost;
        this.damage = damage;
        this.effectText = effectText;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<EnergyType> getCost() { return cost; }
    public void setCost(List<EnergyType> cost) { this.cost = cost; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public String getEffectText() { return effectText; }
    public void setEffectText(String effectText) { this.effectText = effectText; }
}
