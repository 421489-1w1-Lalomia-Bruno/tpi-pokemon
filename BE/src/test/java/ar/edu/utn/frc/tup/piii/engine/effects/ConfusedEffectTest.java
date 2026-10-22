package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfusedEffectTest {

    private ConfusedEffect sut;
    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        sut = new ConfusedEffect();
        PokemonCard card = new PokemonCard();
        card.setHp(100);
        pokemon = new PokemonInPlay(card);
        pokemon.getSpecialConditions().add(SpecialCondition.CONFUSED);
    }

    @Test
    void getType_returnsConfused() {
        assertEquals(SpecialCondition.CONFUSED, sut.getType());
    }

    @Test
    void applyBetweenTurns_doesNothing() {
        sut.applyBetweenTurns(pokemon, new GameBoard());
        assertEquals(0, pokemon.getDamageCounters());
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }

    @Test
    void onAttack_flipCanEitherSucceedOrSelfDamage() {
        int successCount = 0;
        int failCount = 0;
        for (int i = 0; i < 200; i++) {
            PokemonCard card = new PokemonCard();
            card.setHp(200);
            PokemonInPlay p = new PokemonInPlay(card);
            p.getSpecialConditions().add(SpecialCondition.CONFUSED);
            AttackContext context = new AttackContext();
            sut.onAttack(p, context);
            if (context.isAttackSuccessful()) {
                successCount++;
            } else {
                failCount++;
                assertEquals(3, p.getDamageCounters());
                assertNotNull(context.getFailureReason());
                assertTrue(context.getFailureReason().contains("confused"));
            }
        }
        assertTrue(successCount > 0, "Confused should sometimes allow the attack");
        assertTrue(failCount > 0, "Confused should sometimes cause self-damage");
    }

    @Test
    void onAttack_withHeads_doesNotModifyDamage() {
        for (int i = 0; i < 200; i++) {
            PokemonCard card = new PokemonCard();
            card.setHp(200);
            PokemonInPlay p = new PokemonInPlay(card);
            p.getSpecialConditions().add(SpecialCondition.CONFUSED);
            AttackContext context = new AttackContext();
            sut.onAttack(p, context);
            if (context.isAttackSuccessful()) {
                assertEquals(0, p.getDamageCounters());
                return;
            }
        }
    }

    @Test
    void onAttack_withTails_appliesThirtySelfDamageAndInterrupts() {
        for (int i = 0; i < 100; i++) {
            PokemonCard card = new PokemonCard();
            card.setHp(200);
            PokemonInPlay p = new PokemonInPlay(card);
            p.getSpecialConditions().add(SpecialCondition.CONFUSED);
            AttackContext context = new AttackContext();
            sut.onAttack(p, context);
            if (!context.isAttackSuccessful()) {
                assertEquals(3, p.getDamageCounters());
                assertFalse(context.isAttackSuccessful());
                return;
            }
        }
    }

    @Test
    void canCoexist_withAsleep_returnsFalse() {
        assertFalse(sut.canCoexist(SpecialCondition.ASLEEP));
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
    void canCoexist_withConfused_returnsTrue() {
        assertTrue(sut.canCoexist(SpecialCondition.CONFUSED));
    }

    @Test
    void remove_removesConfusedFromConditions() {
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        sut.remove(pokemon);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }
}