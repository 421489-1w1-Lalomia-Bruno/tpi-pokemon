package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.PreventDamageEffect;
import ar.edu.utn.frc.tup.piii.engine.models.Ability;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

import java.util.List;

@Deprecated
public class PostDamageEffectStep implements AttackStep {

    private final StatusEffectManager statusEffectManager;
    private final AbilityEffectResolver abilityResolver;
    private AttackStep next;

    public PostDamageEffectStep() {
        this.statusEffectManager = new StatusEffectManager();
        this.abilityResolver = new AbilityEffectResolver();
    }

    public PostDamageEffectStep(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
        this.abilityResolver = new AbilityEffectResolver();
    }

    @Override
    public void execute(AttackContext context) {
        if (!context.isAttackSuccessful()) {
            return;
        }

        PokemonInPlay defender = context.getDefender();
        Integer finalDamage = context.getFinalDamage();

        if (finalDamage != null && finalDamage > 0) {
            int prevented = 0;
            if (defender != null && defender.getCard().getAbilities() != null) {
                prevented = applyDefenderPokeBodies(defender, finalDamage, context);
            }
            int actualDamage = Math.max(finalDamage - prevented, 0);
            if (actualDamage > 0) {
                defender.takeDamage(actualDamage);
            }
        }

        List<SpecialCondition> effects = context.getEffectsToApply();
        if (effects != null && !effects.isEmpty()) {
            for (SpecialCondition effect : effects) {
                statusEffectManager.addEffect(defender, effect);
            }
        }

        executeNext(context);
    }

    private int applyDefenderPokeBodies(PokemonInPlay defender, int incomingDamage, AttackContext context) {
        int totalPrevented = 0;
        for (Ability ability : defender.getCard().getAbilities()) {
            if (ability.getType() != AbilityType.POKE_BODY) continue;

            List<AbilityEffect> effects = abilityResolver.resolve(ability);
            for (AbilityEffect effect : effects) {
                if (effect instanceof PreventDamageEffect pde) {
                    GameBoard board = context.getBoard();
                    String attackerId = context.getCurrentPlayerId();
                    PlayerBoard oppBoard = null;
                    PlayerBoard sourceBoard = null;
                    if (board != null) {
                        if (board.getPlayer1Board() != null && attackerId.equals(board.getPlayer1Board().getPlayerId())) {
                            oppBoard = board.getPlayer2Board();
                            sourceBoard = board.getPlayer1Board();
                        } else if (board.getPlayer2Board() != null) {
                            oppBoard = board.getPlayer1Board();
                            sourceBoard = board.getPlayer2Board();
                        }
                    }
                    AbilityEffectContext actx = new AbilityEffectContext(
                        context.getMatchId(), attackerId, defender,
                        board, sourceBoard, oppBoard, null, TurnPhase.ATTACK
                    );
                    totalPrevented += pde.filterDamage(incomingDamage, actx);
                }
            }
        }
        return totalPrevented;
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
