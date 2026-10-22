package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShadowCircleEffect implements StadiumEffect {

    private static final Pattern SHADOW_CIRCLE_PATTERN = Pattern.compile(
            "has\\s+no\\s+weakness", Pattern.CASE_INSENSITIVE);

    ShadowCircleEffect() {
    }

    public static ShadowCircleEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = SHADOW_CIRCLE_PATTERN.matcher(rule);
        if (!m.find()) return null;
        if (!rule.toLowerCase().contains("darkness energy")) return null;
        return new ShadowCircleEffect();
    }

    @Override
    public int modifyRetreatCost(PokemonInPlay pokemon, int baseCost) {
        return baseCost;
    }

    @Override
    public boolean hasNoWeakness(PokemonInPlay pokemon) {
        for (EnergyCard e : pokemon.getAttachedEnergy()) {
            if (e.getEnergyType() == EnergyType.DARKNESS) {
                return true;
            }
        }
        return false;
    }
}
