package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParalyzedEffectTest {

    private ParalyzedEffect sut;
    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        sut = new ParalyzedEffect();
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        pokemon = new PokemonInPlay(card);
        pokemon.getSpecialConditions().add(SpecialCondition.PARALYZED);
    }

    @Test
    void getType_returnsParalyzed() {
        assertEquals(SpecialCondition.PARALYZED, sut.getType());
    }

    @Test
    void applyBetweenTurns_removesParalyzed() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void applyBetweenTurns_doesNotAddDamage() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(0, pokemon.getDamageCounters());
    }

    @Test
    void onAttack_blocksAttack() {
        AttackContext context = new AttackContext();
        sut.onAttack(pokemon, context);
        assertFalse(context.isAttackSuccessful());
        assertNotNull(context.getFailureReason());
    }

    @Test
    void canCoexist_withAsleep_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.ASLEEP));
    }

    @Test
    void canCoexist_withConfused_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.CONFUSED));
    }

    @Test
    void canCoexist_withPoisoned_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.POISONED));
    }

    @Test
    void canCoexist_withBurned_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.BURNED));
    }

    @Test
    void canCoexist_withParalyzed_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.PARALYZED));
    }

    @Test
    void remove_removesParalyzedFromConditions() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
        sut.remove(pokemon);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }
}