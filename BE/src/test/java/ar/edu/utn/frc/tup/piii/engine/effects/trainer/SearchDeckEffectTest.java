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

class SearchDeckEffectTest {

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

    private TrainerCard buildTrainer(String effectJson) {
        TrainerCard card = new TrainerCard();
        card.setId("trainer-1");
        card.setName("Test Trainer");
        card.setEffect(effectJson);
        return card;
    }

    private PokemonCard buildPokemon(String id, String name, PokemonStage stage) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setName(name);
        card.setHp(60);
        card.setStage(stage);
        return card;
    }

    private EnergyCard buildEnergy(String id, String name, String subtype) {
        EnergyCard card = new EnergyCard();
        card.setId(id);
        card.setName(name);
        card.setEnergyType(EnergyType.GRASS);
        card.setSpecial("Special".equalsIgnoreCase(subtype));
        card.setProvidedEnergies(List.of(EnergyType.GRASS));
        return card;
    }

    @Test
    void fromRule_pokemonSearch_parsesCorrectly() {
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Pokemon.");
        assertNotNull(effect);
        assertEquals("pokemon", effect.getSearchTarget());
        assertFalse(effect.isBasicOnly());
        assertEquals(1, effect.getMaxResults());
    }

    @Test
    void fromRule_basicPokemonSearch_parsesCorrectly() {
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Basic Pokemon.");
        assertNotNull(effect);
        assertEquals("pokemon", effect.getSearchTarget());
        assertTrue(effect.isBasicOnly());
    }

    @Test
    void fromRule_energySearch_parsesCorrectly() {
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for an Energy card.");
        assertNotNull(effect);
        assertEquals("energy", effect.getSearchTarget());
    }

    @Test
    void fromRule_multipleEnergySearch_parsesCorrectly() {
        SearchDeckEffect effect = SearchDeckEffect.fromRule(
                "Search your deck for up to 2 basic Energy cards.");
        assertNotNull(effect);
        assertEquals("energy", effect.getSearchTarget());
        assertTrue(effect.isBasicOnly());
        assertEquals(2, effect.getMaxResults());
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(SearchDeckEffect.fromRule("Draw 3 cards."));
        assertNull(SearchDeckEffect.fromRule(null));
        assertNull(SearchDeckEffect.fromRule(""));
    }

    @Test
    void apply_findsPokemonInDeck() {
        playerBoard.getDeck().add(buildPokemon("p1", "Charizard", PokemonStage.STAGE2));
        playerBoard.getDeck().add(buildPokemon("p2", "Charmander", PokemonStage.BASIC));
        playerBoard.getDeck().add(buildEnergy("e1", "Fire", "Basic"));

        TrainerCard trainer = buildTrainer("[\"Search your deck for a Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Pokemon.");

        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertEquals("Charizard", playerBoard.getHand().get(0).getName());
        assertEquals(2, playerBoard.getDeck().size());
    }

    @Test
    void apply_basicOnlyPokemon_filtersCorrectly() {
        playerBoard.getDeck().add(buildPokemon("p1", "Charizard", PokemonStage.STAGE2));
        playerBoard.getDeck().add(buildPokemon("p2", "Charmander", PokemonStage.BASIC));

        TrainerCard trainer = buildTrainer("[\"Search your deck for a Basic Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Basic Pokemon.");

        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertEquals("Charmander", playerBoard.getHand().get(0).getName());
    }

    @Test
    void apply_findsEnergyInDeck() {
        playerBoard.getDeck().add(buildPokemon("p1", "Pikachu", PokemonStage.BASIC));
        playerBoard.getDeck().add(buildEnergy("e1", "Grass Energy", "Basic"));

        TrainerCard trainer = buildTrainer("[\"Search your deck for an Energy card.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for an Energy card.");

        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertTrue(playerBoard.getHand().get(0) instanceof EnergyCard);
    }

    @Test
    void apply_basicOnlyEnergy_filtersSpecial() {
        EnergyCard special = buildEnergy("e1", "Rainbow Energy", "Special");
        EnergyCard basic = buildEnergy("e2", "Grass Energy", "Basic");
        playerBoard.getDeck().add(special);
        playerBoard.getDeck().add(basic);

        TrainerCard trainer = buildTrainer("[\"Search your deck for up to 2 basic Energy cards.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule(
                "Search your deck for up to 2 basic Energy cards.");

        effect.apply(ctx);

        assertEquals(1, playerBoard.getHand().size());
        assertEquals("Grass Energy", playerBoard.getHand().get(0).getName());
    }

    @Test
    void apply_noMatch_handlesGracefully() {
        playerBoard.getDeck().add(buildPokemon("p1", "Charizard", PokemonStage.STAGE2));

        TrainerCard trainer = buildTrainer("[\"Search your deck for a Basic Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Basic Pokemon.");

        effect.apply(ctx);

        assertEquals(0, playerBoard.getHand().size());
        assertEquals(1, playerBoard.getDeck().size());
    }

    @Test
    void apply_shufflesDeckAfterSearch() {
        for (int i = 0; i < 10; i++) {
            playerBoard.getDeck().add(buildEnergy("e" + i, "Energy " + i, "Basic"));
        }

        TrainerCard trainer = buildTrainer("[\"Search your deck for an Energy card.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for an Energy card.");

        effect.apply(ctx);

        assertEquals(9, playerBoard.getDeck().size());
        assertEquals(1, playerBoard.getHand().size());
    }

    @Test
    void canApply_returnsTrueWhenDeckNotEmpty() {
        playerBoard.getDeck().add(buildEnergy("e1", "Fire", "Basic"));
        TrainerCard trainer = buildTrainer("[\"Search your deck for an Energy.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for an Energy card.");

        assertTrue(effect.canApply(ctx));
    }

    @Test
    void canApply_returnsFalseWhenDeckEmpty() {
        TrainerCard trainer = buildTrainer("[\"Search your deck for an Energy.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for an Energy card.");

        assertFalse(effect.canApply(ctx));
    }

    @Test
    void apply_publishesEventWhenFound() {
        playerBoard.getDeck().add(buildPokemon("p1", "Pikachu", PokemonStage.BASIC));
        TrainerCard trainer = buildTrainer("[\"Search your deck for a Pokemon.\"]");
        TrainerEffectContext ctx = buildContext(trainer);
        SearchDeckEffect effect = SearchDeckEffect.fromRule("Search your deck for a Pokemon.");

        effect.apply(ctx);

        verify(eventPublisher).publish(any(), eq("match-1"), eq("player-1"), anyString());
    }

    @Test
    void getDescription_returnsCorrectDescription() {
        SearchDeckEffect pokemon = new SearchDeckEffect("pokemon", false, 1);
        assertTrue(pokemon.getDescription().contains("pokemon"));

        SearchDeckEffect basicPokemon = new SearchDeckEffect("pokemon", true, 1);
        assertTrue(basicPokemon.getDescription().contains("Basic"));

        SearchDeckEffect multiEnergy = new SearchDeckEffect("energy", true, 2);
        assertTrue(multiEnergy.getDescription().contains("up to 2"));
        assertTrue(multiEnergy.getDescription().contains("energy"));
    }
}
