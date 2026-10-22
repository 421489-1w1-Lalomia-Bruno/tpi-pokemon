package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

public class ConfusionCheckStep implements AttackStep {

    private final StatusEffectManager statusEffectManager;
    private AttackStep next;

    public ConfusionCheckStep() {
        this.statusEffectManager = new StatusEffectManager();
    }

    public ConfusionCheckStep(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public void execute(AttackContext context) {
        PokemonInPlay attacker = context.getAttacker();

        statusEffectManager.onAttack(attacker, context);

        if (!context.isAttackSuccessful()) {
            return;
        }

        executeNext(context);
    }

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    private void executeNext(AttackContext context) {
        if (next != null && context.isAttackSuccessful()) {
            next.execute(context);
        }
    }
}
