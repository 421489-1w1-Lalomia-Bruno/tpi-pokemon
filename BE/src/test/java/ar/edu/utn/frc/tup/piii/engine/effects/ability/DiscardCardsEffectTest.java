package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscardCardsEffectTest {

    private AbilityEffectContext ctx;
    private PlayerBoard opponentBoard;
    private GameEventPublisher publisher;
    private PokemonInPlay source;

    @BeforeEach
    void setUp() {
        publisher = mock(GameEventPublisher.class);
        PokemonCard card = new PokemonCard();
        card.setName("Attacker");
        source = new PokemonInPlay(card);
        opponentBoard = new PlayerBoard();
        ctx = new AbilityEffectContext("m", "p", source, new GameBoard(),
                new PlayerBoard(), opponentBoard, publisher, TurnPhase.ACTIONS);
    }

    @Test
    void canApply_withHeadsAndCards_returnsTrue() {
        ctx.setFlipHeads(2);
        opponentBoard.getHand().addAll(List.of(new PokemonCard(), new PokemonCard()));
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_zeroTriggerCount_returnsFalse() {
        ctx.setFlipHeads(0);
        opponentBoard.getHand().add(new PokemonCard());
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_triggerOnTails_usesTailsCount() {
        ctx.setFlipTails(1);
        opponentBoard.getHand().add(new PokemonCard());
        DiscardCardsEffect effect = new DiscardCardsEffect(true, 1);

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_opponentHandEmpty_returnsFalse() {
        ctx.setFlipHeads(1);
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_opponentBoardNull_returnsFalse() {
        AbilityEffectContext nullOpp = new AbilityEffectContext("m", "p", source,
                new GameBoard(), new PlayerBoard(), null, publisher, TurnPhase.ACTIONS);
        nullOpp.setFlipHeads(1);
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertFalse(effect.canApply(nullOpp));
    }

    @Test
    void apply_discardsCardsFromOpponentHand() {
        ctx.setFlipHeads(2);
        PokemonCard c1 = new PokemonCard(); c1.setId("c1"); c1.setName("Card1");
        PokemonCard c2 = new PokemonCard(); c2.setId("c2"); c2.setName("Card2");
        PokemonCard c3 = new PokemonCard(); c3.setId("c3"); c3.setName("Card3");
        opponentBoard.getHand().addAll(List.of(c1, c2, c3));
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        effect.apply(ctx);

        assertTrue(opponentBoard.getDiscardPile().size() > 0);
        assertTrue(opponentBoard.getHand().size() < 3);
        verify(publisher).publish(any(), eq("m"), eq("p"), anyString());
    }

    @Test
    void apply_nullPublisher_doesNotThrow() {
        AbilityEffectContext noPub = new AbilityEffectContext("m", "p", source,
                new GameBoard(), new PlayerBoard(), opponentBoard, null, TurnPhase.ACTIONS);
        noPub.setFlipHeads(1);
        opponentBoard.getHand().add(new PokemonCard());
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertDoesNotThrow(() -> effect.apply(noPub));
    }

    @Test
    void apply_opponentBoardNull_doesNothing() {
        AbilityEffectContext nullOpp = new AbilityEffectContext("m", "p", source,
                new GameBoard(), new PlayerBoard(), null, publisher, TurnPhase.ACTIONS);
        nullOpp.setFlipHeads(1);
        DiscardCardsEffect effect = new DiscardCardsEffect(false, 1);

        assertDoesNotThrow(() -> effect.apply(nullOpp));
    }
}
