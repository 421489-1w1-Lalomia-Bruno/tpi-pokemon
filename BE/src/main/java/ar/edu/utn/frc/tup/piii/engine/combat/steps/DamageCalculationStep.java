package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.combat.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;

public class DamageCalculationStep implements AttackStep {

    private final DamageCalculator damageCalculator;
    private AttackStep next;

    public DamageCalculationStep() {
        this.damageCalculator = new DamageCalculator();
    }

    public DamageCalculationStep(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public void execute(AttackContext context) {
        int finalDamage = damageCalculator.calculateDamage(context);
        context.setFinalDamage(finalDamage);
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
