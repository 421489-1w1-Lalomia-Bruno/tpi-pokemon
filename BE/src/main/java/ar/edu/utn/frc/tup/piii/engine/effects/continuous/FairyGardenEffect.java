package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FairyGardenEffect implements StadiumEffect {

    private static final Pattern FAIRY_GARDEN_PATTERN = Pattern.compile(
            "has\\s+no\\s+retreat\\s+cost", Pattern.CASE_INSENSITIVE);

    FairyGardenEffect() {
    }

    public static FairyGardenEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = FAIRY_GARDEN_PATTERN.matcher(rule);
        if (!m.find()) return null;
        if (!rule.toLowerCase().contains("fairy energy")) return null;
        return new FairyGardenEffect();
    }

    @Override
    public int modifyRetreatCost(PokemonInPlay pokemon, int baseCost) {
        for (EnergyCard e : pokemon.getAttachedEnergy()) {
            if (e.getEnergyType() == EnergyType.FAIRY) {
                return 0;
            }
        }
        return baseCost;
    }

    @Override
    public boolean hasNoWeakness(PokemonInPlay pokemon) {
        return false;
    }
}
