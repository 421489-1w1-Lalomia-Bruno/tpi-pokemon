package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PoisonedEffectTest {

    private PoisonedEffect sut;
    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        sut = new PoisonedEffect();
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        pokemon = new PokemonInPlay(card);
        pokemon.getSpecialConditions().add(SpecialCondition.POISONED);
    }

    @Test
    void getType_returnsPoisoned() {
        assertEquals(SpecialCondition.POISONED, sut.getType());
    }

    @Test
    void applyBetweenTurns_addsOneDamageCounter() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(1, pokemon.getDamageCounters());
        assertEquals(90, pokemon.getCurrentHp());
    }

    @Test
    void applyBetweenTurns_accumulatesOverMultipleTurns() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        sut.applyBetweenTurns(pokemon, new GameBoard());
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(3, pokemon.getDamageCounters());
        assertEquals(70, pokemon.getCurrentHp());
    }

    @Test
    void onAttack_doesNothing() {
        AttackContext context = new AttackContext();
        sut.onAttack(pokemon, context);
        assertTrue(context.isAttackSuccessful());
    }

    @Test
    void canCoexist_withPoisoned_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.POISONED));
    }

    @Test
    void canCoexist_withBurned_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.BURNED));
    }

    @Test
    void canCoexist_withAsleep_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.ASLEEP));
    }

    @Test
    void canCoexist_withConfused_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.CONFUSED));
    }

    @Test
    void canCoexist_withParalyzed_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.PARALYZED));
    }

    @Test
    void remove_removesPoisonedFromConditions() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
        sut.remove(pokemon);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
    }
}