package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbilityEffectContextTest {

    @Test
    void constructorAndGetters() {
        GameEventPublisher publisher = mock(GameEventPublisher.class);
        PokemonInPlay source = new PokemonInPlay();
        GameBoard board = new GameBoard();
        PlayerBoard srcBoard = new PlayerBoard();
        PlayerBoard oppBoard = new PlayerBoard();

        AbilityEffectContext ctx = new AbilityEffectContext(
                "match-1", "player-1", source, board, srcBoard, oppBoard, publisher, TurnPhase.ACTIONS);

        assertEquals("match-1", ctx.getMatchId());
        assertEquals("player-1", ctx.getPlayerId());
        assertSame(source, ctx.getSourcePokemon());
        assertSame(board, ctx.getBoard());
        assertSame(srcBoard, ctx.getSourceBoard());
        assertSame(oppBoard, ctx.getOpponentBoard());
        assertSame(publisher, ctx.getPublisher());
        assertEquals(TurnPhase.ACTIONS, ctx.getCurrentPhase());
    }

    @Test
    void settersAndGetters() {
        GameEventPublisher publisher = mock(GameEventPublisher.class);
        AbilityEffectContext ctx = new AbilityEffectContext(
                "m", "p", new PokemonInPlay(), new GameBoard(),
                new PlayerBoard(), new PlayerBoard(), publisher, TurnPhase.DRAW);

        ctx.setFlipHeads(3);
        ctx.setFlipTails(2);
        ctx.setSourceTargetId("src-target");
        ctx.setTargetId("target");

        assertEquals(3, ctx.getFlipHeads());
        assertEquals(2, ctx.getFlipTails());
        assertEquals("src-target", ctx.getSourceTargetId());
        assertEquals("target", ctx.getTargetId());
    }
}
