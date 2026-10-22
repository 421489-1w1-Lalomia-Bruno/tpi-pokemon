package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public class PoisonedEffect implements StatusEffectStrategy {

    private static final int POISON_DAMAGE = 10;

    @Override
    public SpecialCondition getType() {
        return SpecialCondition.POISONED;
    }

    @Override
    public int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board) {
        pokemon.takeDamage(POISON_DAMAGE);
        return POISON_DAMAGE;
    }

    @Override
    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
    }

    @Override
    public boolean canCoexist(SpecialCondition other) {
        return other != SpecialCondition.POISONED;
    }

    @Override
    public void remove(PokemonInPlay pokemon) {
        pokemon.getSpecialConditions().remove(SpecialCondition.POISONED);
    }
}
