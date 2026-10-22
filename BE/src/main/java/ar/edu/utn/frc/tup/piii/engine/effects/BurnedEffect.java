package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public class BurnedEffect implements StatusEffectStrategy {

    private static final int BURN_DAMAGE = 20;

    @Override
    public SpecialCondition getType() {
        return SpecialCondition.BURNED;
    }

    @Override
    public int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board) {
        pokemon.takeDamage(BURN_DAMAGE);
        if (CoinFlipManager.flip()) {
            pokemon.getSpecialConditions().remove(SpecialCondition.BURNED);
        }
        return BURN_DAMAGE;
    }

    @Override
    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
    }

    @Override
    public boolean canCoexist(SpecialCondition other) {
        return other != SpecialCondition.BURNED;
    }

    @Override
    public void remove(PokemonInPlay pokemon) {
        pokemon.getSpecialConditions().remove(SpecialCondition.BURNED);
    }
}
