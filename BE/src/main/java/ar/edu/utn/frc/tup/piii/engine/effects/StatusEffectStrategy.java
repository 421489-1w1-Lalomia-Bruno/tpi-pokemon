package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

public interface StatusEffectStrategy {

    SpecialCondition getType();

    int applyBetweenTurns(PokemonInPlay pokemon, GameBoard board);

    void onAttack(PokemonInPlay pokemon, AttackContext context);

    boolean canCoexist(SpecialCondition other);

    void remove(PokemonInPlay pokemon);
}
