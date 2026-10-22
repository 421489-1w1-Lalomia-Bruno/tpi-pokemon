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

class HealDamageEffectTest {

    private PlayerBoard playerBoard;
    private GameBoard gameBoard;
    private GameEventPublisher eventPublisher;
    private PokemonInPlay activePokemon;
    private PokemonInPlay benchPokemon;

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

        PokemonCard activeCard = new PokemonCard();
        activeCard.setId("poke-1");
        activeCard.setName("Charizard");
        activeCard.setHp(120);
        activePokemon = new PokemonInPlay(activeCard);
        activePokemon.setCurrentHp(50);
        activePokemon.setActive(true);
        playerBoard.setActivePokemon(activePokemon);

        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("poke-2");
        benchCard.setName("Pikachu");
        benchCard.setHp(60);
        benchPokemon = new PokemonInPlay(benchCard);
        benchPokemon.setCurrentHp(30);
        benchPokemon.setActive(false);
        playerBoard.getBench().add(benchPokemon);
    }

    private TrainerEffectContext buildContext(TrainerCard card, String targetId) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, targetId, eventPublisher);
    }

    private TrainerCard buildTrainer(String effectJson) {
        TrainerCard card = new TrainerCard();
        card.setId("trainer-1");
        card.setName("Test Trainer");
        card.setEffect(effectJson);
        return card;
    }

    @Test
    void fromRule_potion_parsesCorrectly() {
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");
        assertNotNull(effect);
        assertEquals(30, effect.getHealAmount());
        assertFalse(effect.isDiscardEnergyAfter());
    }

    @Test
    void fromRule_superPotion_parsesCorrectly() {
        HealDamageEffect effect = HealDamageEffect.fromRule(
                "Heal 60 damage from 1 of your Pokemon and discard an Energy card from it.");
        assertNotNull(effect);
        assertEquals(60, effect.getHealAmount());
        assertTrue(effect.isDiscardEnergyAfter());
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(HealDamageEffect.fromRule("Draw 3 cards."));
        assertNull(HealDamageEffect.fromRule(null));
        assertNull(HealDamageEffect.fromRule(""));
    }

    @Test
    void apply_healsActivePokemon() {
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        effect.apply(ctx);

        assertEquals(80, activePokemon.getCurrentHp());
    }

    @Test
    void apply_healsSpecificBenchPokemon() {
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, "poke-2");
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        effect.apply(ctx);

        assertEquals(50, activePokemon.getCurrentHp());
        assertEquals(60, benchPokemon.getCurrentHp());
    }

    @Test
    void apply_doesNotExceedMaxHp() {
        activePokemon.setCurrentHp(110);
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        effect.apply(ctx);

        assertEquals(120, activePokemon.getCurrentHp());
    }

    @Test
    void apply_fullHp_doesNothing() {
        activePokemon.setCurrentHp(120);
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        effect.apply(ctx);

        assertEquals(120, activePokemon.getCurrentHp());
    }

    @Test
    void apply_withDiscardEnergy_discardsEnergy() {
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-1");
        energy.setEnergyType(EnergyType.GRASS);
        activePokemon.getAttachedEnergy().add(energy);

        TrainerCard trainer = buildTrainer(
                "[\"Heal 60 damage from 1 of your Pokemon and discard an Energy card attached to it.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule(
                "Heal 60 damage from 1 of your Pokemon and discard an Energy card attached to it.");

        effect.apply(ctx);

        assertEquals(110, activePokemon.getCurrentHp());
        assertTrue(activePokemon.getAttachedEnergy().isEmpty());
        assertEquals(1, playerBoard.getDiscardPile().size());
    }

    @Test
    void apply_withDiscardEnergy_noEnergy_healsWithoutDiscard() {
        TrainerCard trainer = buildTrainer(
                "[\"Heal 60 damage from 1 of your Pokemon and discard an Energy card attached to it.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule(
                "Heal 60 damage from 1 of your Pokemon and discard an Energy card attached to it.");

        effect.apply(ctx);

        assertEquals(110, activePokemon.getCurrentHp());
        assertTrue(activePokemon.getAttachedEnergy().isEmpty());
    }

    @Test
    void canApply_returnsTrueWhenDamaged() {
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_returnsFalseWhenFullHp() {
        activePokemon.setCurrentHp(120);
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_returnsFalseWhenNoActivePokemon() {
        playerBoard.setActivePokemon(null);
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_publishesEvent() {
        TrainerCard trainer = buildTrainer("[\"Heal 30 damage from 1 of your Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer, null);
        HealDamageEffect effect = HealDamageEffect.fromRule("Heal 30 damage from 1 of your Pokemon.");

        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }

    @Test
    void getDescription_returnsCorrectDescription() {
        HealDamageEffect potion = new HealDamageEffect(30, false);
        assertEquals("Heal 30 damage", potion.getDescription());

        HealDamageEffect superPotion = new HealDamageEffect(60, true);
        assertTrue(superPotion.getDescription().contains("discard 1 Energy"));
    }
}
