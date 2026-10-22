package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.combat.AttackFlipResolver;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

import java.util.List;

public class ConditionalFlipStep implements AttackStep {

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        if (!context.isNeedsConditionalFlip()) {
            executeNext(context);
            return;
        }
        if (!context.isAttackSuccessful()) {
            return;
        }

        String effectText = context.getSelectedAttack() != null
                ? context.getSelectedAttack().getEffectText() : null;

        boolean isHeads = CoinFlipManager.flip();
        context.setConditionalFlipHeads(isHeads);

        String branchText = AttackFlipResolver.extractBranchText(effectText, isHeads);
        context.setBranchEffectText(branchText);
        List<SpecialCondition> conditions = AttackFlipResolver.parseSpecialConditions(branchText);
        context.setEffectsToApply(conditions);

        if (context.getEventPublisher() != null) {
            String attackerName = context.getAttacker() != null
                    ? context.getAttacker().getCard().getName() : "Attacker";
            String attackName = context.getSelectedAttack() != null
                    ? context.getSelectedAttack().getName() : "";
            context.getEventPublisher().publish(
                    GameEventType.COIN_FLIP_RESULT,
                    context.getMatchId(),
                    context.getCurrentPlayerId(),
                    attackerName + " used " + attackName + ": " + (isHeads ? "Heads" : "Tails"),
                    attackName, null, isHeads ? 1 : 0);
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
