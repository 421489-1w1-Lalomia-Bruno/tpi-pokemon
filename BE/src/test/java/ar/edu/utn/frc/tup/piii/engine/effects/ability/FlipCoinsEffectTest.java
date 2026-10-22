package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlipCoinsEffectTest {

    private AbilityEffectContext ctx;
    private GameEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = mock(GameEventPublisher.class);
        PokemonCard card = new PokemonCard();
        card.setName("Test Pokemon");
        PokemonInPlay source = new PokemonInPlay(card);
        ctx = new AbilityEffectContext("m", "p", source, new GameBoard(),
                new PlayerBoard(), new PlayerBoard(), publisher, TurnPhase.ACTIONS);
    }

    @AfterEach
    void tearDown() {
        CoinFlipManager.setRng(null);
    }

    @Test
    void canApply_positiveCoins_returnsTrue() {
        FlipCoinsEffect effect = new FlipCoinsEffect(1);
        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_zeroCoins_returnsFalse() {
        FlipCoinsEffect effect = new FlipCoinsEffect(0);
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_flipResultSetsHeadsAndTails() {
        CoinFlipManager.setRng(new Random(42) {
            private int calls;
            @Override
            public boolean nextBoolean() { return calls++ < 2; }
        });
        FlipCoinsEffect effect = new FlipCoinsEffect(3);

        effect.apply(ctx);

        assertEquals(2, ctx.getFlipHeads());
        assertEquals(1, ctx.getFlipTails());
    }

    @Test
    void apply_publishesFlipResult() {
        CoinFlipManager.setRng(new Random(42) {
            @Override
            public boolean nextBoolean() { return true; }
        });
        FlipCoinsEffect effect = new FlipCoinsEffect(1);

        effect.apply(ctx);

        verify(publisher, atLeastOnce()).publish(any(), eq("m"), eq("p"), anyString(), isNull(), isNull(), anyInt());
    }

    @Test
    void apply_nullPublisher_doesNotThrow() {
        PokemonCard card = new PokemonCard();
        card.setName("Test");
        PokemonInPlay pokemon = new PokemonInPlay(card);
        AbilityEffectContext noPubCtx = new AbilityEffectContext("m", "p", pokemon,
                new GameBoard(), new PlayerBoard(), new PlayerBoard(), null, TurnPhase.ACTIONS);
        FlipCoinsEffect effect = new FlipCoinsEffect(1);

        assertDoesNotThrow(() -> effect.apply(noPubCtx));
    }
}
