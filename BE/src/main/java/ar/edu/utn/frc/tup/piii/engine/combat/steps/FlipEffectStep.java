package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectContext;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.AbilityEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.ability.FlipCoinsEffect;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.Ability;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;

import java.util.List;

@Deprecated
public class FlipEffectStep implements AttackStep {

    private final AbilityEffectResolver resolver = new AbilityEffectResolver();
    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        if (!context.isAttackSuccessful()) return;

        String effectText = context.getSelectedAttack() != null
                ? context.getSelectedAttack().getEffectText() : null;

        if (effectText == null) {
            executeNext(context);
            return;
        }

        Ability pseudo = new Ability();
        pseudo.setName(context.getSelectedAttack().getName());
        pseudo.setEffectText(effectText);
        pseudo.setType(AbilityType.POKE_POWER);

        List<AbilityEffect> effects = resolver.resolve(pseudo);
        if (effects.isEmpty()) {
            executeNext(context);
            return;
        }

        GameBoard board = context.getBoard();
        String attackerId = context.getCurrentPlayerId();
        PokemonInPlay attacker = context.getAttacker();
        PlayerBoard sourceBoard = null;
        PlayerBoard oppBoard = null;
        if (board != null) {
            if (board.getPlayer1Board() != null && attackerId.equals(board.getPlayer1Board().getPlayerId())) {
                sourceBoard = board.getPlayer1Board();
                oppBoard = board.getPlayer2Board();
            } else if (board.getPlayer2Board() != null) {
                sourceBoard = board.getPlayer2Board();
                oppBoard = board.getPlayer1Board();
            }
        }

        AbilityEffectContext actx = new AbilityEffectContext(
                context.getMatchId(), attackerId, attacker,
                board, sourceBoard, oppBoard, context.getEventPublisher(), TurnPhase.ATTACK
        );

        for (AbilityEffect effect : effects) {
            if (effect instanceof FlipCoinsEffect && context.isFlipResolvedByAttack()) {
                continue;
            }
            if (effect.canApply(actx)) {
                effect.apply(actx);
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
