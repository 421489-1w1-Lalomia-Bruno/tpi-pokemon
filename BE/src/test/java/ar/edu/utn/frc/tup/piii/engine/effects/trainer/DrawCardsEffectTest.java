package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.enums.CardType;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DrawCardsEffectTest {

    private PlayerBoard playerBoard;
    private GameBoard gameBoard;
    private GameEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        playerBoard = new PlayerBoard();
        playerBoard.setPlayerId("player-1");

        PlayerBoard opponentBoard = new PlayerBoard();
        opponentBoard.setPlayerId("player-2");

        gameBoard = new GameBoard();
        gameBoard.setPlayer1Board(playerBoard);
        gameBoard.setPlayer2Board(opponentBoard);

        eventPublisher = mock(GameEventPublisher.class);
    }

    private void fillDeck(int count) {
        for (int i = 0; i < count; i++) {
            EnergyCard card = new EnergyCard();
            card.setId("card-" + i);
            card.setName("Energy " + i);
            card.setEnergyType(EnergyType.GRASS);
            card.setProvidedEnergies(List.of(EnergyType.GRASS));
            playerBoard.getDeck().add(card);
        }
    }

    private void fillHand(int count) {
        for (int i = 0; i < count; i++) {
            EnergyCard card = new EnergyCard();
            card.setId("hand-" + i);
            card.setName("Hand Card " + i);
            card.setEnergyType(EnergyType.GRASS);
            card.setProvidedEnergies(List.of(EnergyType.GRASS));
            playerBoard.getHand().add(card);
        }
    }

    private TrainerEffectContext buildContext(TrainerCard card) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, null, eventPublisher);
    }

    private TrainerCard buildTrainer(String effectJson) {
        TrainerCard card = new TrainerCard();
        card.setId("trainer-1");
        card.setName("Test Trainer");
        card.setEffect(effectJson);
        return card;
    }

    @Test
    void fromRule_draw3cards_parsesCorrectly() {
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");
        assertNotNull(effect);
        assertEquals(3, effect.getCardsToDraw());
        assertFalse(effect.isDiscardHandFirst());
        assertFalse(effect.isShuffleHandFirst());
    }

    @Test
    void fromRule_drawUpTo7cards_parsesCorrectly() {
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw up to 7 cards.");
        assertNotNull(effect);
        assertEquals(7, effect.getCardsToDraw());
    }

    @Test
    void fromRule_discardHandAndDraw_parsesCorrectly() {
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Discard your hand. Draw 7 cards.");
        assertNotNull(effect);
        assertEquals(7, effect.getCardsToDraw());
        assertTrue(effect.isDiscardHandFirst());
    }

    @Test
    void fromRule_shuffleHandAndDraw_parsesCorrectly() {
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Shuffle your hand into your deck and draw 5 cards.");
        assertNotNull(effect);
        assertEquals(5, effect.getCardsToDraw());
        assertTrue(effect.isShuffleHandFirst());
    }

    @Test
    void fromRule_noDrawMatch_returnsNull() {
        assertNull(DrawCardsEffect.fromRule("Heal 30 damage from 1 of your Pokemon."));
        assertNull(DrawCardsEffect.fromRule(null));
        assertNull(DrawCardsEffect.fromRule(""));
    }

    @Test
    void apply_drawsCardsFromDeck() {
        fillDeck(10);
        TrainerCard trainer = buildTrainer("[\"Draw 3 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");

        effect.apply(ctx);

        assertEquals(3, playerBoard.getHand().size());
        assertEquals(7, playerBoard.getDeck().size());
    }

    @Test
    void apply_drawsLessIfDeckInsufficient() {
        fillDeck(2);
        TrainerCard trainer = buildTrainer("[\"Draw 5 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 5 cards.");

        effect.apply(ctx);

        assertEquals(2, playerBoard.getHand().size());
        assertEquals(0, playerBoard.getDeck().size());
    }

    @Test
    void apply_emptyDeck_drawsNothing() {
        TrainerCard trainer = buildTrainer("[\"Draw 3 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");

        effect.apply(ctx);

        assertEquals(0, playerBoard.getHand().size());
    }

    @Test
    void apply_discardHandFirst_movesHandToDiscard() {
        fillHand(4);
        fillDeck(10);
        TrainerCard trainer = buildTrainer("[\"Discard your hand. Draw 7 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Discard your hand. Draw 7 cards.");

        effect.apply(ctx);

        assertEquals(7, playerBoard.getHand().size());
        assertEquals(4, playerBoard.getDiscardPile().size());
        assertEquals(3, playerBoard.getDeck().size());
    }

    @Test
    void apply_shuffleHandFirst_shufflesHandIntoDeck() {
        fillHand(3);
        fillDeck(2);
        TrainerCard trainer = buildTrainer("[\"Shuffle your hand into your deck and draw 5 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Shuffle your hand into your deck and draw 5 cards.");

        effect.apply(ctx);

        assertEquals(5, playerBoard.getHand().size());
        assertEquals(0, playerBoard.getDeck().size());
        assertEquals(0, playerBoard.getDiscardPile().size());
    }

    @Test
    void canApply_returnsTrueWhenValid() {
        fillDeck(5);
        TrainerCard trainer = buildTrainer("[\"Draw 3 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_returnsFalseWhenNullContext() {
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");
        assertFalse(effect.canApply(null));
    }

    @Test
    void apply_publishesEvent() {
        fillDeck(10);
        TrainerCard trainer = buildTrainer("[\"Draw 3 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");

        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }

    @Test
    void apply_noDeck_noEventPublished() {
        TrainerCard trainer = buildTrainer("[\"Draw 3 cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        DrawCardsEffect effect = DrawCardsEffect.fromRule("Draw 3 cards.");

        effect.apply(ctx);

        verify(eventPublisher, never()).publish(any(), anyString(), anyString(), anyString());
    }

    @Test
    void getDescription_returnsCorrectDescription() {
        DrawCardsEffect simple = new DrawCardsEffect(3, false, false);
        assertEquals("Draw 3 cards", simple.getDescription());

        DrawCardsEffect discard = new DrawCardsEffect(7, true, false);
        assertTrue(discard.getDescription().contains("Discard your hand"));

        DrawCardsEffect shuffle = new DrawCardsEffect(5, false, true);
        assertTrue(shuffle.getDescription().contains("Shuffle your hand"));
    }
}
