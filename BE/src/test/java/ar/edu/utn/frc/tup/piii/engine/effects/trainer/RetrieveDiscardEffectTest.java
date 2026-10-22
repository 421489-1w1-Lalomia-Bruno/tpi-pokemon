package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RetrieveDiscardEffectTest {

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

    private TrainerEffectContext buildContext(TrainerCard card) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, null, eventPublisher);
    }

    private PokemonCard buildPokemon(String id, String name, PokemonStage stage) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setName(name);
        card.setHp(60);
        card.setStage(stage);
        return card;
    }

    private EnergyCard buildEnergy(String id, String name, boolean special) {
        EnergyCard card = new EnergyCard();
        card.setId(id);
        card.setName(name);
        card.setEnergyType(special ? EnergyType.COLORLESS : EnergyType.GRASS);
        card.setSpecial(special);
        card.setProvidedEnergies(List.of(card.getEnergyType()));
        return card;
    }

    @Test
    void fromRule_energyRetrieval_parsesCorrectly() {
        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put up to 2 basic Energy cards from your discard pile into your hand.");
        assertNotNull(effect);
        assertEquals("energy", effect.getSearchTarget());
        assertTrue(effect.isBasicOnly());
        assertEquals(2, effect.getMaxResults());
        assertEquals("hand", effect.getDestination());
    }

    @Test
    void fromRule_sacredAsh_parsesCorrectly() {
        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Shuffle 5 Pokemon cards from your discard pile into your deck.");
        assertNotNull(effect);
        assertEquals("pokemon", effect.getSearchTarget());
        assertFalse(effect.isBasicOnly());
        assertEquals(5, effect.getMaxResults());
        assertEquals("deck", effect.getDestination());
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(RetrieveDiscardEffect.fromRule("Draw 3 cards."));
        assertNull(RetrieveDiscardEffect.fromRule(null));
    }

    @Test
    void apply_retrievesEnergyToHand() {
        playerBoard.getDiscardPile().add(buildEnergy("e1", "Grass Energy", false));
        playerBoard.getDiscardPile().add(buildEnergy("e2", "Fire Energy", false));
        playerBoard.getDiscardPile().add(buildPokemon("p1", "Pikachu", PokemonStage.BASIC));

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Energy Retrieval");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put up to 2 basic Energy cards from your discard pile into your hand.");
        effect.apply(ctx);

        assertEquals(2, playerBoard.getHand().size());
        assertEquals(1, playerBoard.getDiscardPile().size());
        assertEquals("Pikachu", playerBoard.getDiscardPile().get(0).getName());
    }

    @Test
    void apply_retrievesPokemonToHand() {
        playerBoard.getDiscardPile().add(buildPokemon("p1", "Pikachu", PokemonStage.BASIC));
        playerBoard.getDiscardPile().add(buildPokemon("p2", "Charizard", PokemonStage.STAGE2));

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokemon card from your discard pile into your hand.");
        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertEquals(1, playerBoard.getDiscardPile().size());
    }

    @Test
    void apply_basicOnlyEnergy_filtersSpecial() {
        EnergyCard special = buildEnergy("e1", "Rainbow Energy", true);
        EnergyCard basic = buildEnergy("e2", "Grass Energy", false);
        playerBoard.getDiscardPile().add(special);
        playerBoard.getDiscardPile().add(basic);

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put up to 2 basic Energy cards from your discard pile into your hand.");
        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertEquals("Grass Energy", playerBoard.getHand().get(0).getName());
        assertTrue(playerBoard.getDiscardPile().contains(special));
    }

    @Test
    void apply_shufflesIntoDeck() {
        playerBoard.getDiscardPile().add(buildPokemon("p1", "Pikachu", PokemonStage.BASIC));
        playerBoard.getDiscardPile().add(buildPokemon("p2", "Charmander", PokemonStage.BASIC));

        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Sacred Ash");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Shuffle 5 Pokemon cards from your discard pile into your deck.");
        effect.apply(ctx);

        assertEquals(0, playerBoard.getHand().size());
        assertEquals(2, playerBoard.getDeck().size());
        assertTrue(playerBoard.getDiscardPile().isEmpty());
    }

    @Test
    void apply_emptyDiscard_doesNothing() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokemon card from your discard pile into your hand.");
        effect.apply(ctx);

        assertTrue(playerBoard.getHand().isEmpty());
    }

    @Test
    void canApply_trueWhenDiscardHasMatchingCard() {
        playerBoard.getDiscardPile().add(buildPokemon("p1", "Bulbasaur", PokemonStage.BASIC));
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokemon card from your discard pile into your hand.");
        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenDiscardLacksMatchingType() {
        playerBoard.getDiscardPile().add(buildEnergy("e1", "Grass Energy", false));
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokemon card from your discard pile into your hand.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void canApply_falseWhenDiscardEmpty() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokemon card from your discard pile into your hand.");
        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_publishesEvent() {
        playerBoard.getDiscardPile().add(buildEnergy("e1", "Grass Energy", false));
        TrainerCard trainer = new TrainerCard();
        trainer.setId("t1");
        trainer.setName("Energy Retrieval");
        TrainerEffectContext ctx = buildContext(trainer);

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put up to 2 basic Energy cards from your discard pile into your hand.");
        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }
}
