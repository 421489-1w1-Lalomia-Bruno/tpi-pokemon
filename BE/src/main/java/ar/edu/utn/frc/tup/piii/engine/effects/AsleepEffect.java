package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public class AsleepEffect implements StatusEffectStrategy {

    @Override
    public SpecialCondition getType() {
        return SpecialCondition.ASLEEP;
    }

    @Override
    public int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board) {
        if (CoinFlipManager.flip()) {
            pokemon.getSpecialConditions().remove(SpecialCondition.ASLEEP);
        }
        return 0;
    }

    @Override
    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
        context.setAttackSuccessful(false);
        context.setFailureReason("Pokemon is asleep and cannot attack");
    }

    @Override
    public boolean canCoexist(SpecialCondition other) {
        return other != SpecialCondition.CONFUSED && other != SpecialCondition.PARALYZED;
    }

    @Override
    public void remove(PokemonInPlay pokemon) {
        pokemon.getSpecialConditions().remove(SpecialCondition.ASLEEP);
    }
}
