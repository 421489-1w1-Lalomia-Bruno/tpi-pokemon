package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainerEffectResolverTest {

    @Test
    void parseRules_validJson_returnsList() {
        List<String> rules = TrainerEffectResolver.parseRules("[\"Draw 3 cards.\"]");
        assertEquals(1, rules.size());
        assertEquals("Draw 3 cards.", rules.get(0));
    }

    @Test
    void parseRules_multipleRules_returnsAll() {
        List<String> rules = TrainerEffectResolver.parseRules(
                "[\"Heal 30 damage from 1 of your Pokemon.\", \"Draw 2 cards.\"]");
        assertEquals(2, rules.size());
        assertEquals("Heal 30 damage from 1 of your Pokemon.", rules.get(0));
        assertEquals("Draw 2 cards.", rules.get(1));
    }

    @Test
    void parseRules_emptyArray_returnsEmpty() {
        List<String> rules = TrainerEffectResolver.parseRules("[]");
        assertTrue(rules.isEmpty());
    }

    @Test
    void parseRules_nullInput_returnsEmpty() {
        List<String> rules = TrainerEffectResolver.parseRules(null);
        assertTrue(rules.isEmpty());
    }

    @Test
    void parseRules_blankInput_returnsEmpty() {
        List<String> rules = TrainerEffectResolver.parseRules("   ");
        assertTrue(rules.isEmpty());
    }

    @Test
    void parseRules_nonJson_returnsEmpty() {
        List<String> rules = TrainerEffectResolver.parseRules("not json");
        assertTrue(rules.isEmpty());
    }

    @Test
    void parseRules_escapedQuotes_handlesCorrectly() {
        List<String> rules = TrainerEffectResolver.parseRules(
                "[\"He said \\\"hello\\\" to you.\"]");
        assertEquals(1, rules.size());
        assertEquals("He said \"hello\" to you.", rules.get(0));
    }

    @Test
    void resolve_validDrawRule_returnsDrawEffect() {
        TrainerCard card = new TrainerCard();
        card.setId("t1");
        card.setName("Tierno");
        card.setEffect("[\"Draw 3 cards.\"]");

        TrainerEffectResolver resolver = new TrainerEffectResolver();
        List<TrainerEffect> effects = resolver.resolve(card);

        assertEquals(1, effects.size());
        assertInstanceOf(DrawCardsEffect.class, effects.get(0));
        assertEquals(3, ((DrawCardsEffect) effects.get(0)).getCardsToDraw());
    }

    @Test
    void resolve_unknownRule_returnsEmpty() {
        TrainerCard card = new TrainerCard();
        card.setId("t1");
        card.setName("Unknown");
        card.setEffect("[\"Do something mysterious.\"]");

        TrainerEffectResolver resolver = new TrainerEffectResolver();
        List<TrainerEffect> effects = resolver.resolve(card);

        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_nullEffect_returnsEmpty() {
        TrainerCard card = new TrainerCard();
        card.setId("t1");
        card.setName("Null");
        card.setEffect(null);

        TrainerEffectResolver resolver = new TrainerEffectResolver();
        List<TrainerEffect> effects = resolver.resolve(card);

        assertTrue(effects.isEmpty());
    }

    @Test
    void resolve_sycamoreStyle_parsesAsDrawEffect() {
        TrainerCard card = new TrainerCard();
        card.setId("t1");
        card.setName("Professor Sycamore");
        card.setEffect("[\"Discard your hand. Draw 7 cards.\"]");

        TrainerEffectResolver resolver = new TrainerEffectResolver();
        List<TrainerEffect> effects = resolver.resolve(card);

        assertEquals(1, effects.size());
        assertInstanceOf(DrawCardsEffect.class, effects.get(0));
        DrawCardsEffect dce = (DrawCardsEffect) effects.get(0);
        assertEquals(7, dce.getCardsToDraw());
        assertTrue(dce.isDiscardHandFirst());
    }
}
