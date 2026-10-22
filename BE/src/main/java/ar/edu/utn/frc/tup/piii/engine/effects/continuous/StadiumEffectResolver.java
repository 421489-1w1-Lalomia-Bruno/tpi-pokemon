package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.effects.trainer.TrainerEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;

import java.util.List;

public class StadiumEffectResolver {

    private static final StadiumEffect NO_EFFECT = new StadiumEffect() {
        @Override public int modifyRetreatCost(PokemonInPlay p, int c) { return c; }
        @Override public boolean hasNoWeakness(PokemonInPlay p) { return false; }
    };

    public static StadiumEffect resolve(TrainerCard stadium) {
        if (stadium == null || stadium.getEffect() == null) return NO_EFFECT;

        List<String> rules = TrainerEffectResolver.parseRules(stadium.getEffect());
        if (rules.isEmpty()) return NO_EFFECT;

        for (String rule : rules) {
            StadiumEffect effect = matchEffect(rule);
            if (effect != NO_EFFECT) return effect;
        }

        return NO_EFFECT;
    }

    private static StadiumEffect matchEffect(String rule) {
        if (rule == null) return NO_EFFECT;

        StadiumEffect fairy = FairyGardenEffect.fromRule(rule);
        if (fairy != null) return fairy;

        StadiumEffect shadow = ShadowCircleEffect.fromRule(rule);
        if (shadow != null) return shadow;

        return NO_EFFECT;
    }
}
