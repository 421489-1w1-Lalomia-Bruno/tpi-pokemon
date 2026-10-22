package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.continuous.ToolEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.continuous.ToolEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

public class AttackModifierStep implements AttackStep {

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        if (context.isAttackSuccessful()) {
            PokemonInPlay attacker = context.getAttacker();
            if (attacker != null && attacker.getTool() != null) {
                ToolEffect tool = ToolEffectResolver.resolve(attacker.getTool());
                int damage = context.getBaseDamage() != null ? context.getBaseDamage() : 0;
                context.setBaseDamage(tool.modifyAttackDamage(damage));
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
