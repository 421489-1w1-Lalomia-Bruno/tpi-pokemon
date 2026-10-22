package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public class ParalyzedEffect implements StatusEffectStrategy {

    @Override
    public SpecialCondition getType() {
        return SpecialCondition.PARALYZED;
    }

    @Override
    public int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board) {
        pokemon.getSpecialConditions().remove(SpecialCondition.PARALYZED);
        return 0;
    }

    @Override
    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
        context.setAttackSuccessful(false);
        context.setFailureReason("Pokemon is paralyzed and cannot attack");
    }

    @Override
    public boolean canCoexist(SpecialCondition other) {
        return other != SpecialCondition.ASLEEP && other != SpecialCondition.CONFUSED;
    }

    @Override
    public void remove(PokemonInPlay pokemon) {
        pokemon.getSpecialConditions().remove(SpecialCondition.PARALYZED);
    }
}
