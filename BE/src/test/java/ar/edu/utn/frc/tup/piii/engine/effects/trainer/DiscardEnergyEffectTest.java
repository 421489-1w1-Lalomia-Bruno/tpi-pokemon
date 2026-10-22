package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DiscardEnergyEffectTest {

    private PlayerBoard opponentBoard;
    private GameBoard gameBoard;
    private GameEventPublisher eventPublisher;
    private PokemonInPlay opponentActive;

    @BeforeEach
    void setUp() {
        PlayerBoard playerBoard = new PlayerBoard();
        playerBoard.setPlayerId("player-1");

        opponentBoard = new PlayerBoard();
        opponentBoard.setPlayerId("player-2");

        gameBoard = new GameBoard();
        gameBoard.setPlayer1Board(playerBoard);
        gameBoard.setPlayer2Board(opponentBoard);

        eventPublisher = mock(GameEventPublisher.class);

        PokemonCard card = new PokemonCard();
        card.setId("opp-1");
        card.setName("Charizard");
        card.setHp(120);
        opponentActive = new PokemonInPlay(card);
        opponentActive.setActive(true);
        opponentBoard.setActivePokemon(opponentActive);
    }

    private TrainerEffectContext buildContext(TrainerCard card, String targetId) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, targetId, eventPublisher);
    }

    @Test
    void fromRule_enhancedHammer_parsesCorrectly() {
        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard a Special Energy attached to 1 of your opponent's Pokemon.");
        assertNotNull(effect);
        assertTrue(effect.isSpecialOnly());
    }

    @Test
    void fromRule_teamFlareGrunt_parsesCorrectly() {
        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard an Energy attached to your opponent's Active Pokemon.");
        assertNotNull(effect);
        assertFalse(effect.isSpecialOnly());
    }

    @Test
    void fromRule_superPotion_notMatched() {
        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Heal 60 damage from 1 of your Pokemon and discard an Energy card from it.");
        assertNull(effect);
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(DiscardEnergyEffect.fromRule("Draw 3 cards."));
        assertNull(DiscardEnergyEffect.fromRule(null));
    }

    @Test
    void apply_discardsSpecialEnergyFromOpponentActive() {
        EnergyCard special = new EnergyCard();
        special.setId("e1");
        special.setName("Rainbow Energy");
        special.setSpecial(true);
        special.setEnergyType(EnergyType.COLORLESS);
        opponentActive.getAttachedEnergy().add(special);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Enhanced Hammer");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard a Special Energy attached to 1 of your opponent's Pokemon.");
        effect.apply(ctx);

        assertTrue(opponentActive.getAttachedEnergy().isEmpty());
        assertEquals(1, opponentBoard.getDiscardPile().size());
        assertEquals("Rainbow Energy", opponentBoard.getDiscardPile().get(0).getName());
    }

    @Test
    void apply_discardsAnyEnergyFromOpponent() {
        EnergyCard basic = new EnergyCard();
        basic.setId("e1");
        basic.setName("Grass Energy");
        basic.setSpecial(false);
        basic.setEnergyType(EnergyType.GRASS);
        opponentActive.getAttachedEnergy().add(basic);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Team Flare Grunt");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard an Energy attached to your opponent's Active Pokemon.");
        effect.apply(ctx);

        assertTrue(opponentActive.getAttachedEnergy().isEmpty());
        assertEquals(1, opponentBoard.getDiscardPile().size());
    }

    @Test
    void apply_specialOnly_skipsBasicEnergy() {
        EnergyCard basic = new EnergyCard();
        basic.setId("e1");
        basic.setName("Grass Energy");
        basic.setSpecial(false);
        basic.setEnergyType(EnergyType.GRASS);
        opponentActive.getAttachedEnergy().add(basic);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard a Special Energy attached to 1 of your opponent's Pokemon.");
        effect.apply(ctx);

        assertFalse(opponentActive.getAttachedEnergy().isEmpty());
        assertTrue(opponentBoard.getDiscardPile().isEmpty());
    }

    @Test
    void canApply_falseWhenNoOpponentActive() {
        opponentBoard.setActivePokemon(null);
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard a Special Energy attached to 1 of your opponent's Pokemon.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenNoEnergy() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard a Special Energy attached to 1 of your opponent's Pokemon.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_publishesEvent() {
        EnergyCard energy = new EnergyCard();
        energy.setId("e1");
        energy.setName("Fire Energy");
        energy.setSpecial(false);
        energy.setEnergyType(EnergyType.FIRE);
        opponentActive.getAttachedEnergy().add(energy);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Team Flare Grunt");
        TrainerEffectContext ctx = buildContext(trainer, null);

        DiscardEnergyEffect effect = DiscardEnergyEffect.fromRule(
                "Discard an Energy attached to your opponent's Active Pokemon.");
        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }
}
