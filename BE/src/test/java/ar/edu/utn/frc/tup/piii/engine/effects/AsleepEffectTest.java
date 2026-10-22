package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsleepEffectTest {

    private AsleepEffect sut;
    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        sut = new AsleepEffect();
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        pokemon = new PokemonInPlay(card);
        pokemon.getSpecialConditions().add(SpecialCondition.ASLEEP);
    }

    @Test
    void getType_returnsAsleep() {
        assertEquals(SpecialCondition.ASLEEP, sut.getType());
    }

    @Test
    void applyBetweenTurns_doesNotAddDamage() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(0, pokemon.getDamageCounters());
    }

    @Test
    void applyBetweenTurns_flipCanEitherRemoveOrKeepAsleep() {
        int removedCount = 0;
        int keptCount = 0;
        for (int i = 0; i < 200; i++) {
            PokemonInPlay p = createAsleepPokemon();
            sut.applyBetweenTurns(p, new GameBoard());
            if (p.getSpecialConditions().contains(SpecialCondition.ASLEEP)) {
                keptCount++;
            } else {
                removedCount++;
            }
        }
        assertTrue(removedCount > 0, "Asleep should sometimes be removed by the flip");
        assertTrue(keptCount > 0, "Asleep should sometimes persist after the flip");
    }

    @Test
    void onAttack_blocksAttack() {
        AttackContext context = new AttackContext();
        sut.onAttack(pokemon, context);
        assertFalse(context.isAttackSuccessful());
        assertNotNull(context.getFailureReason());
    }

    @Test
    void canCoexist_withConfused_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.CONFUSED));
    }

    @Test
    void canCoexist_withParalyzed_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.PARALYZED));
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
    void canCoexist_withAsleep_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.ASLEEP));
    }

    @Test
    void remove_removesAsleepFromConditions() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        sut.remove(pokemon);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    private PokemonInPlay createAsleepPokemon() {
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        PokemonInPlay p = new PokemonInPlay(card);
        p.getSpecialConditions().add(SpecialCondition.ASLEEP);
        return p;
    }
}