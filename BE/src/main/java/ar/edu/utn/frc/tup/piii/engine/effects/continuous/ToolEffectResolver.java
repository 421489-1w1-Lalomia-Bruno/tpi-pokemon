package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.effects.trainer.TrainerEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;

import java.util.List;

public class ToolEffectResolver {

    private static final ToolEffect NO_EFFECT = new ToolEffect() {
        @Override public int modifyAttackDamage(int d) { return d; }
        @Override public int modifyDefenseDamage(int d) { return d; }
        @Override public int modifyRetreatCost(int c) { return c; }
    };

    public static ToolEffect resolve(TrainerCard tool) {
        if (tool == null || tool.getEffect() == null) return NO_EFFECT;

        List<String> rules = TrainerEffectResolver.parseRules(tool.getEffect());
        if (rules.isEmpty()) return NO_EFFECT;

        for (String rule : rules) {
            ToolEffect effect = matchEffect(rule);
            if (effect != NO_EFFECT) return effect;
        }

        return NO_EFFECT;
    }

    private static ToolEffect matchEffect(String rule) {
        if (rule == null) return NO_EFFECT;

        ToolEffect muscleBand = MuscleBandEffect.fromRule(rule);
        if (muscleBand != null) return muscleBand;

        ToolEffect hardCharm = HardCharmEffect.fromRule(rule);
        if (hardCharm != null) return hardCharm;

        return NO_EFFECT;
    }
}
