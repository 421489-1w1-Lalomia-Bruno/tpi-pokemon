package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BurnedEffectTest {

    private BurnedEffect sut;
    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        sut = new BurnedEffect();
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        pokemon = new PokemonInPlay(card);
        pokemon.getSpecialConditions().add(SpecialCondition.BURNED);
    }

    @Test
    void getType_returnsBurned() {
        assertEquals(SpecialCondition.BURNED, sut.getType());
    }

    @Test
    void applyBetweenTurns_addsTwoDamageCounters() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(2, pokemon.getDamageCounters());
        assertEquals(80, pokemon.getCurrentHp());
    }

    @Test
    void applyBetweenTurns_damageIsAlwaysTwentyRegardlessOfFlip() {
        for (int i = 0; i < 50; i++) {
            PokemonInPlay p = createBurnedPokemon();
            sut.applyBetweenTurns(p, new GameBoard());
            assertEquals(2, p.getDamageCounters(), "Burned should always add 2 damage counters regardless of flip");
        }
    }

    @Test
    void applyBetweenTurns_flipCanEitherRemoveOrKeepBurned() {
        int removedCount = 0;
        int keptCount = 0;
        for (int i = 0; i < 200; i++) {
            PokemonInPlay p = createBurnedPokemon();
            sut.applyBetweenTurns(p, new GameBoard());
            if (p.getSpecialConditions().contains(SpecialCondition.BURNED)) {
                keptCount++;
            } else {
                removedCount++;
            }
        }
        assertTrue(removedCount > 0, "Burned should sometimes be removed by the flip");
        assertTrue(keptCount > 0, "Burned should sometimes persist after the flip");
    }

    @Test
    void onAttack_doesNothing() {
        AttackContext context = new AttackContext();
        sut.onAttack(pokemon, context);
        assertTrue(context.isAttackSuccessful());
    }

    @Test
    void canCoexist_withBurned_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.BURNED));
    }

    @Test
    void canCoexist_withPoisoned_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.POISONED));
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
    void remove_removesBurnedFromConditions() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.BURNED));
        sut.remove(pokemon);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.BURNED));
    }

    private PokemonInPlay createBurnedPokemon() {
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        PokemonInPlay p = new PokemonInPlay(card);
        p.getSpecialConditions().add(SpecialCondition.BURNED);
        return p;
    }
}