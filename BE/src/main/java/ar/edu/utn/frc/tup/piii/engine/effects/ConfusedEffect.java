package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public class ConfusedEffect implements StatusEffectStrategy {

    private static final int CONFUSION_SELF_DAMAGE = 30;

    @Override
    public SpecialCondition getType() {
        return SpecialCondition.CONFUSED;
    }

    @Override
    public int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board) {
        return 0;
    }

    @Override
    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
        boolean heads = CoinFlipManager.flip();
        if (!heads) {
            pokemon.takeDamage(CONFUSION_SELF_DAMAGE);
            context.setAttackSuccessful(false);
            context.setFailureReason("Attacker is confused and hit itself for " + CONFUSION_SELF_DAMAGE + " damage");
        }
    }

    @Override
    public boolean canCoexist(SpecialCondition other) {
        return other != SpecialCondition.ASLEEP && other != SpecialCondition.PARALYZED;
    }

    @Override
    public void remove(PokemonInPlay pokemon) {
        pokemon.getSpecialConditions().remove(SpecialCondition.CONFUSED);
    }
}
