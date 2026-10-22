package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

public interface StadiumEffect {

    int modifyRetreatCost(PokemonInPlay pokemon, int baseCost);

    boolean hasNoWeakness(PokemonInPlay pokemon);
}
