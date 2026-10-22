package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

public class PreventDamageEffect implements AbilityEffect {

    private final int threshold;
    private final boolean onlyOpponentTurn;

    public PreventDamageEffect(int threshold, boolean onlyOpponentTurn) {
        this.threshold = threshold;
        this.onlyOpponentTurn = onlyOpponentTurn;
    }

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        return true;
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
    }

    public int filterDamage(int incomingDamage, AbilityEffectContext ctx) {
        if (onlyOpponentTurn && ctx.getCurrentPhase() != TurnPhase.ATTACK) {
            return 0;
        }
        if (incomingDamage <= threshold) {
            return incomingDamage;
        }
        return 0;
    }

    public int getThreshold() { return threshold; }
    public boolean isOnlyOpponentTurn() { return onlyOpponentTurn; }
}
