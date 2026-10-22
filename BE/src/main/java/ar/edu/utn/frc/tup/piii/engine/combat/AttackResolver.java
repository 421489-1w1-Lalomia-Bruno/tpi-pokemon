package ar.edu.utn.frc.tup.piii.engine.combat;

import ar.edu.utn.frc.tup.piii.engine.combat.steps.*;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;

import java.util.List;

public class AttackResolver {

    private final List<AttackStep> steps;

    public AttackResolver() {
        AttackStep step1 = new EnergyValidationStep();
        AttackStep step1b = new EnergyDiscardStep();
        AttackStep step2 = new ConfusionCheckStep();
        AttackStep step2b = new ConditionalFlipStep();
        AttackStep step3 = new TargetSelectionStep();
        AttackStep step4 = new PreAttackEffectStep();
        AttackStep step5 = new VariableDamageStep();
        AttackStep step6 = new AttackModifierStep();
        AttackStep step7 = new DamageCalculationStep();
        AttackStep step8 = new AttackEffectStep();

        step1.setNext(step1b);
        step1b.setNext(step2);
        step2.setNext(step2b);
        step2b.setNext(step3);
        step3.setNext(step4);
        step4.setNext(step5);
        step5.setNext(step6);
        step6.setNext(step7);
        step7.setNext(step8);

        this.steps = List.of(step1, step1b, step2, step2b, step3, step4, step5, step6, step7, step8);
    }

    public void resolveAttack(AttackContext context) {
        if (!steps.isEmpty()) {
            steps.get(0).execute(context);
        }
    }
}
