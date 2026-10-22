package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.models.Attack;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.ArrayList;
import java.util.List;

public class EnergyValidationStep implements AttackStep {

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        Attack attack = context.getSelectedAttack();
        if (attack == null || attack.getCost() == null || attack.getCost().isEmpty()) {
            executeNext(context);
            return;
        }

        List<EnergyType> available = new ArrayList<>(context.getAttacker().getAvailableEnergyPool());
        int wildcards = context.getAttacker().getWildcardCount();
        List<EnergyType> required = attack.getCost();

        int colorlessNeeded = 0;
        List<EnergyType> specificNeeded = new ArrayList<>();

        for (EnergyType req : required) {
            if (req == EnergyType.COLORLESS) {
                colorlessNeeded++;
            } else {
                specificNeeded.add(req);
            }
        }

        boolean failed = false;
        for (EnergyType specific : specificNeeded) {
            if (!available.remove(specific)) {
                if (wildcards > 0) {
                    wildcards--;
                } else {
                    context.setAttackSuccessful(false);
                    context.setFailureReason("Missing required energy: " + specific);
                    failed = true;
                    break;
                }
            }
        }
        if (failed) {
            return;
        }

        for (int i = 0; i < colorlessNeeded; i++) {
            if (!available.isEmpty()) {
                available.remove(0);
            } else if (wildcards > 0) {
                wildcards--;
            } else {
                context.setAttackSuccessful(false);
                context.setFailureReason("Not enough energy to cover colorless cost");
                return;
            }
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
