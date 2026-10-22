package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class StatusEffectManagerTest {

    private StatusEffectManager sut;
    private PokemonInPlay pokemon;
    private GameBoard board;
    private GameEventPublisher mockPublisher;

    @BeforeEach
    void setUp() {
        sut = new StatusEffectManager();
        PokemonCard card = new PokemonCard();
        card.setHp(200);
        card.setName("Test Pokemon");
        pokemon = new PokemonInPlay(card);
        board = new GameBoard();
        mockPublisher = mock(GameEventPublisher.class);
    }

    @AfterEach
    void tearDown() {
        CoinFlipManager.setRng(null);
    }

    @Test
    void addEffect_addsCondition() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
    }

    @Test
    void addEffect_withDuplicate_skips() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        assertEquals(1, pokemon.getSpecialConditions().size());
    }

    @Test
    void addEffect_withNullPokemon_doesNothing() {
        assertDoesNotThrow(() -> sut.addEffect(null, SpecialCondition.POISONED));
    }

    @Test
    void addEffect_withNullCondition_doesNothing() {
        assertDoesNotThrow(() -> sut.addEffect(pokemon, null));
    }

    @Test
    void addEffect_withIncompatibleCondition_replacesExisting() {
        sut.addEffect(pokemon, SpecialCondition.ASLEEP);
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP));

        sut.addEffect(pokemon, SpecialCondition.CONFUSED);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP),
                "CONFUSED should replace ASLEEP (mutually exclusive)");
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }

    @Test
    void addEffect_withIncompatibleParalyzed_replacesConfused() {
        sut.addEffect(pokemon, SpecialCondition.CONFUSED);
        sut.addEffect(pokemon, SpecialCondition.PARALYZED);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED),
                "PARALYZED should replace CONFUSED (mutually exclusive)");
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void addEffect_allowsBurnedAndPoisonedTogether() {
        sut.addEffect(pokemon, SpecialCondition.BURNED);
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertEquals(2, pokemon.getSpecialConditions().size());
    }

    @Test
    void removeEffect_removesCondition() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.removeEffect(pokemon, SpecialCondition.POISONED);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
    }

    @Test
    void removeEffect_withNull_doesNothing() {
        assertDoesNotThrow(() -> sut.removeEffect(null, SpecialCondition.POISONED));
        assertDoesNotThrow(() -> sut.removeEffect(pokemon, null));
    }

    @Test
    void processBetweenTurns_withNullPokemon_doesNothing() {
        assertDoesNotThrow(() -> sut.processBetweenTurns(null, board, "match-1", "p1", mockPublisher));
    }

    @Test
    void processBetweenTurns_withNoConditions_doesNothing() {
        assertDoesNotThrow(() -> sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher));
        assertEquals(0, pokemon.getDamageCounters());
    }

    @Test
    void processBetweenTurns_poisoned_addsTenDamage() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher);
        assertEquals(1, pokemon.getDamageCounters());
        assertEquals(190, pokemon.getCurrentHp());
    }

    @Test
    void processBetweenTurns_burned_addsTwentyDamage() {
        sut.addEffect(pokemon, SpecialCondition.BURNED);
        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher);
        assertEquals(2, pokemon.getDamageCounters());
        assertEquals(180, pokemon.getCurrentHp());
    }

    @Test
    void processBetweenTurns_poisonedAndBurned_accumulatesDamageInOrder() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.addEffect(pokemon, SpecialCondition.BURNED);
        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher);
        assertEquals(3, pokemon.getDamageCounters(),
                "Poisoned (1 counter) + Burned (2 counters) should total 3 damage counters");
        assertEquals(170, pokemon.getCurrentHp());
    }

    @Test
    void processBetweenTurns_paralyzed_removesParalyzed() {
        sut.addEffect(pokemon, SpecialCondition.PARALYZED);
        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher);
        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void onAttack_withConfused_delegatesToConfusedEffect() {
        AttackContext context = new AttackContext();
        sut.addEffect(pokemon, SpecialCondition.CONFUSED);

        sut.onAttack(pokemon, context);

        if (!context.isAttackSuccessful()) {
            assertEquals(3, pokemon.getDamageCounters());
            assertNotNull(context.getFailureReason());
        }
    }

    @Test
    void onAttack_withoutConfused_doesNothing() {
        AttackContext context = new AttackContext();
        sut.addEffect(pokemon, SpecialCondition.POISONED);

        sut.onAttack(pokemon, context);

        assertTrue(context.isAttackSuccessful());
        assertEquals(0, pokemon.getDamageCounters());
    }

    @Test
    void onAttack_withNull_doesNothing() {
        assertDoesNotThrow(() -> sut.onAttack(null, new AttackContext()));
        assertDoesNotThrow(() -> sut.onAttack(pokemon, null));
    }

    @Test
    void clearAll_removesAllConditions() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.addEffect(pokemon, SpecialCondition.BURNED);
        sut.addEffect(pokemon, SpecialCondition.CONFUSED);
        assertEquals(3, pokemon.getSpecialConditions().size());

        sut.clearAll(pokemon);

        assertTrue(pokemon.getSpecialConditions().isEmpty());
    }

    @Test
    void clearAll_withNull_doesNothing() {
        assertDoesNotThrow(() -> sut.clearAll(null));
    }

    // ---- onAttack with ASLEEP ----

    @Test
    void onAttack_withAsleep_failsAttack() {
        AttackContext context = new AttackContext();
        sut.addEffect(pokemon, SpecialCondition.ASLEEP);

        sut.onAttack(pokemon, context);

        assertFalse(context.isAttackSuccessful());
        assertEquals("Pokemon is asleep and cannot attack", context.getFailureReason());
    }

    // ---- onAttack with PARALYZED ----

    @Test
    void onAttack_withParalyzed_failsAttack() {
        AttackContext context = new AttackContext();
        sut.addEffect(pokemon, SpecialCondition.PARALYZED);

        sut.onAttack(pokemon, context);

        assertFalse(context.isAttackSuccessful());
        assertEquals("Pokemon is paralyzed and cannot attack", context.getFailureReason());
    }

    // ---- processBetweenTurns with ASLEEP ----

    @Test
    void processBetweenTurns_asleep_heads_wakesUp() {
        CoinFlipManager.setRng(new Random(42) {
            @Override
            public boolean nextBoolean() { return true; }
        });
        sut.addEffect(pokemon, SpecialCondition.ASLEEP);
        sut.addEffect(pokemon, SpecialCondition.POISONED);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher,
                SpecialCondition.ASLEEP, SpecialCondition.POISONED);

        assertFalse(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertEquals(1, pokemon.getDamageCounters());
    }

    @Test
    void processBetweenTurns_asleep_tails_staysAsleep() {
        CoinFlipManager.setRng(new Random(42) {
            @Override
            public boolean nextBoolean() { return false; }
        });
        sut.addEffect(pokemon, SpecialCondition.ASLEEP);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher,
                SpecialCondition.ASLEEP);

        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    // ---- processBetweenTurns with specific conditions filter ----

    @Test
    void processBetweenTurns_withOnlyThese_filters() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);
        sut.addEffect(pokemon, SpecialCondition.BURNED);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher,
                SpecialCondition.POISONED);

        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertTrue(pokemon.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertEquals(1, pokemon.getDamageCounters(),
                "Only poisoned should be processed, so only 1 damage counter");
    }

    // ---- processBetweenTurns publishes events ----

    @Test
    void processBetweenTurns_poisoned_publishesEvent() {
        sut.addEffect(pokemon, SpecialCondition.POISONED);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher);

        verify(mockPublisher, atLeastOnce()).publish(
                eq(GameEventType.SPECIAL_CONDITION_APPLIED),
                eq("match-1"),
                eq("p1"),
                anyString(),
                anyString(),
                anyString(),
                anyInt());
    }

    @Test
    void processBetweenTurns_noDamageConditions_noEventPublished() {
        sut.addEffect(pokemon, SpecialCondition.ASLEEP);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher,
                SpecialCondition.ASLEEP);

        verify(mockPublisher, never()).publish(
                eq(GameEventType.SPECIAL_CONDITION_APPLIED),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyInt());
    }

    // ---- processBetweenTurns null/empty conditions set ----

    @Test
    void processBetweenTurns_nullConditionsSet_doesNothing() {
        pokemon.setSpecialConditions(null);
        assertDoesNotThrow(() -> sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher));
    }

    @Test
    void processBetweenTurns_varargsEmptyArray_usesDefaultConditions() {
        // Llamar al overload varargs con array vacío → rama onlyThese.length == 0 (lista por defecto).
        sut.addEffect(pokemon, SpecialCondition.POISONED);

        sut.processBetweenTurns(pokemon, board, "match-1", "p1", mockPublisher, new SpecialCondition[0]);

        assertEquals(1, pokemon.getDamageCounters(),
                "Con array vacío debe usar la lista por defecto y procesar POISONED");
    }

}