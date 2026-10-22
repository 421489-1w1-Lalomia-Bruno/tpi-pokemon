package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnContextTest {

    @Test
    void defaultValues() {
        TurnContext ctx = new TurnContext();
        assertNull(ctx.getCurrentPlayerId());
        assertNull(ctx.getPhase());
        assertFalse(ctx.isFirstTurn());
        assertFalse(ctx.isHasDrawn());
        assertFalse(ctx.isHasPlayedSupporter());
        assertFalse(ctx.isHasPlayedStadium());
        assertFalse(ctx.isHasAttachedEnergy());
        assertFalse(ctx.isHasEvolved());
        assertFalse(ctx.isHasUsedAbility());
        assertFalse(ctx.isHasRetreated());
    }

    @Test
    void settersAndGetters() {
        TurnContext ctx = new TurnContext();
        ctx.setCurrentPlayerId("p1");
        ctx.setPhase(TurnPhase.ACTIONS);
        ctx.setFirstTurn(true);
        ctx.setHasDrawn(true);
        ctx.setHasPlayedSupporter(true);
        ctx.setHasPlayedStadium(true);
        ctx.setHasAttachedEnergy(true);
        ctx.setHasEvolved(true);
        ctx.setHasUsedAbility(true);
        ctx.setHasRetreated(true);

        assertEquals("p1", ctx.getCurrentPlayerId());
        assertEquals(TurnPhase.ACTIONS, ctx.getPhase());
        assertTrue(ctx.isFirstTurn());
        assertTrue(ctx.isHasDrawn());
        assertTrue(ctx.isHasPlayedSupporter());
        assertTrue(ctx.isHasPlayedStadium());
        assertTrue(ctx.isHasAttachedEnergy());
        assertTrue(ctx.isHasEvolved());
        assertTrue(ctx.isHasUsedAbility());
        assertTrue(ctx.isHasRetreated());
    }

    @Test
    void phaseStartsNull() {
        TurnContext ctx = new TurnContext();
        assertNull(ctx.getPhase());
    }
}
