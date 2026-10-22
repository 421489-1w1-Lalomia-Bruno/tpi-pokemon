package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PreventDamageEffectTest {

    @Test
    void canApply_alwaysReturnsTrue() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ACTIONS);
        PreventDamageEffect effect = new PreventDamageEffect(0, false);

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void apply_noOp() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ACTIONS);
        PreventDamageEffect effect = new PreventDamageEffect(0, false);

        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void filterDamage_belowThreshold_returnsDamage() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ACTIONS);
        PreventDamageEffect effect = new PreventDamageEffect(30, false);

        assertEquals(20, effect.filterDamage(20, ctx));
    }

    @Test
    void filterDamage_aboveThreshold_returnsZero() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ACTIONS);
        PreventDamageEffect effect = new PreventDamageEffect(30, false);

        assertEquals(0, effect.filterDamage(50, ctx));
    }

    @Test
    void filterDamage_equalToThreshold_returnsDamage() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ACTIONS);
        PreventDamageEffect effect = new PreventDamageEffect(30, false);

        assertEquals(30, effect.filterDamage(30, ctx));
    }

    @Test
    void filterDamage_opponentTurnOnly_notAttackPhase_returnsZero() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.DRAW);
        PreventDamageEffect effect = new PreventDamageEffect(0, true);

        assertEquals(0, effect.filterDamage(10, ctx));
    }

    @Test
    void filterDamage_opponentTurnOnly_attackPhase_returnsDamage() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ATTACK);
        PreventDamageEffect effect = new PreventDamageEffect(100, true);

        assertEquals(50, effect.filterDamage(50, ctx));
    }

    @Test
    void filterDamage_opponentTurnOnly_attackPhase_aboveThreshold_returnsZero() {
        AbilityEffectContext ctx = new AbilityEffectContext("m", "p", new PokemonInPlay(),
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), mock(GameEventPublisher.class), TurnPhase.ATTACK);
        PreventDamageEffect effect = new PreventDamageEffect(30, true);

        assertEquals(0, effect.filterDamage(50, ctx));
    }

    @Test
    void getThreshold_returnsValue() {
        PreventDamageEffect effect = new PreventDamageEffect(50, true);
        assertEquals(50, effect.getThreshold());
    }

    @Test
    void isOnlyOpponentTurn_returnsValue() {
        PreventDamageEffect effect = new PreventDamageEffect(0, true);
        assertTrue(effect.isOnlyOpponentTurn());
    }
}
