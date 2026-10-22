package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Cubre las 4 cartas Entrenador de xy1 que estaban rotas y se corrigieron:
 * Red Card, Cassius, Max Revive y Evosoda. Verifica tanto el ruteo del
 * resolver como el comportamiento de cada efecto.
 */
class FixedTrainerEffectsTest {

    private PlayerBoard playerBoard;
    private PlayerBoard opponentBoard;
    private GameBoard gameBoard;
    private GameEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        playerBoard = new PlayerBoard();
        playerBoard.setPlayerId("player-1");
        opponentBoard = new PlayerBoard();
        opponentBoard.setPlayerId("player-2");
        gameBoard = new GameBoard();
        gameBoard.setPlayer1Board(playerBoard);
        gameBoard.setPlayer2Board(opponentBoard);
        eventPublisher = mock(GameEventPublisher.class);
    }

    private TrainerEffectContext ctx(TrainerCard card, String targetId) {
        return new TrainerEffectContext(gameBoard, "player-1", "match-1", card, targetId, eventPublisher);
    }

    private TrainerCard trainer(String name, String effectJson) {
        TrainerCard card = new TrainerCard();
        card.setId("t-" + name);
        card.setName(name);
        card.setEffect(effectJson);
        return card;
    }

    private PokemonCard pokemon(String id, String name, PokemonStage stage, String evolvesFrom, int hp) {
        PokemonCard card = new PokemonCard();
        card.setId(id);
        card.setName(name);
        card.setStage(stage);
        card.setEvolvesFrom(evolvesFrom);
        card.setHp(hp);
        return card;
    }

    private EnergyCard energy(String id) {
        EnergyCard e = new EnergyCard();
        e.setId(id);
        e.setName("Energy " + id);
        return e;
    }

    // ─── Red Card ───────────────────────────────────────────────────────────

    @Test
    void redCard_routesToRedCardEffect() {
        TrainerCard card = trainer("Red Card",
                "[\"Your opponent shuffles his or her hand into his or her deck and draws 4 cards.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(RedCardEffect.class, effects.get(0));
        assertEquals(4, ((RedCardEffect) effects.get(0)).getCardsToDraw());
    }

    @Test
    void redCard_opponentReshufflesAndDrawsFour() {
        for (int i = 0; i < 6; i++) opponentBoard.getHand().add(pokemon("h" + i, "Hand" + i, PokemonStage.BASIC, null, 50));
        for (int i = 0; i < 10; i++) opponentBoard.getDeck().add(pokemon("d" + i, "Deck" + i, PokemonStage.BASIC, null, 50));

        RedCardEffect effect = RedCardEffect.fromRule(
                "Your opponent shuffles his or her hand into his or her deck and draws 4 cards.");
        assertNotNull(effect);
        TrainerCard card = trainer("Red Card", null);
        assertTrue(effect.canApply(ctx(card, null)));
        effect.apply(ctx(card, null));

        assertEquals(4, opponentBoard.getHand().size());
        // 6 viejas + 10 mazo - 4 robadas = 12 en mazo
        assertEquals(12, opponentBoard.getDeck().size());
    }

    // ─── Cassius ────────────────────────────────────────────────────────────

    @Test
    void cassius_routesToShufflePokemonEffect() {
        TrainerCard card = trainer("Cassius",
                "[\"Shuffle 1 of your Pokémon and all cards attached to it into your deck.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(ShufflePokemonIntoDeckEffect.class, effects.get(0));
    }

    @Test
    void cassius_shufflesBenchedPokemonAndAttachmentsIntoDeck() {
        PokemonInPlay benched = new PokemonInPlay(pokemon("b1", "Pikachu", PokemonStage.BASIC, null, 60));
        benched.getAttachedEnergy().add(energy("e1"));
        playerBoard.getBench().add(benched);

        ShufflePokemonIntoDeckEffect effect = ShufflePokemonIntoDeckEffect.fromRule(
                "Shuffle 1 of your Pokémon and all cards attached to it into your deck.");
        assertNotNull(effect);
        TrainerCard card = trainer("Cassius", null);
        effect.apply(ctx(card, "b1"));

        assertTrue(playerBoard.getBench().isEmpty());
        // El Pokémon + 1 energía van al mazo.
        assertEquals(2, playerBoard.getDeck().size());
    }

    // ─── Max Revive ─────────────────────────────────────────────────────────

    @Test
    void maxRevive_routesToRetrieveDiscardEffect() {
        TrainerCard card = trainer("Max Revive",
                "[\"Put a Pokémon from your discard pile on top of your deck.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(RetrieveDiscardEffect.class, effects.get(0));
        RetrieveDiscardEffect rd = (RetrieveDiscardEffect) effects.get(0);
        assertEquals("pokemon", rd.getSearchTarget());
        assertEquals("deck", rd.getDestination());
        assertTrue(rd.isDeckTop());
    }

    @Test
    void maxRevive_putsPokemonOnTopOfDeck() {
        playerBoard.getDiscardPile().add(pokemon("p1", "Charizard", PokemonStage.STAGE2, "Charmeleon", 150));
        playerBoard.getDeck().add(pokemon("d1", "Filler", PokemonStage.BASIC, null, 50));

        RetrieveDiscardEffect effect = RetrieveDiscardEffect.fromRule(
                "Put a Pokémon from your discard pile on top of your deck.");
        assertNotNull(effect);
        effect.apply(ctx(trainer("Max Revive", null), null));

        assertTrue(playerBoard.getDiscardPile().isEmpty());
        assertEquals(2, playerBoard.getDeck().size());
        assertEquals("Charizard", playerBoard.getDeck().get(0).getName());
    }

    // ─── Evosoda ────────────────────────────────────────────────────────────

    @Test
    void evosoda_routesToEvolveFromDeckEffect() {
        TrainerCard card = trainer("Evosoda",
                "[\"Search your deck for a card that evolves from 1 of your Pokémon and put it onto that Pokémon. Shuffle your deck afterward.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(EvolveFromDeckEffect.class, effects.get(0));
    }

    @Test
    void evosoda_evolvesTargetFromDeck() {
        PokemonInPlay active = new PokemonInPlay(pokemon("a1", "Charmander", PokemonStage.BASIC, null, 60));
        active.setCurrentHp(50); // recibió 10 de daño
        playerBoard.setActivePokemon(active);
        playerBoard.getDeck().add(pokemon("d1", "Charmeleon", PokemonStage.STAGE1, "Charmander", 90));

        EvolveFromDeckEffect effect = EvolveFromDeckEffect.fromRule(
                "Search your deck for a card that evolves from 1 of your Pokémon and put it onto that Pokémon. Shuffle your deck afterward.");
        assertNotNull(effect);
        TrainerCard card = trainer("Evosoda", null);
        assertTrue(effect.canApply(ctx(card, "a1")));
        effect.apply(ctx(card, "a1"));

        assertEquals("Charmeleon", playerBoard.getActivePokemon().getCard().getName());
        // El daño de 10 se arrastra: 90 - 10 = 80.
        assertEquals(80, playerBoard.getActivePokemon().getCurrentHp());
        assertTrue(playerBoard.getDeck().isEmpty());
        assertEquals(1, playerBoard.getDiscardPile().size());
    }

    // ─── Great Ball ─────────────────────────────────────────────────────────

    @Test
    void greatBall_routesToGreatBallEffect() {
        TrainerCard card = trainer("Great Ball",
                "[\"Look at the top 7 cards of your deck. You may reveal a Pokémon you find there and put it into your hand. Shuffle the other cards back into your deck.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(GreatBallEffect.class, effects.get(0));
    }

    @Test
    void greatBall_revealsTopCardsForChoice() {
        playerBoard.getDeck().add(energy("e1"));
        playerBoard.getDeck().add(pokemon("p1", "Pikachu", PokemonStage.BASIC, null, 60));
        playerBoard.getDeck().add(energy("e2"));

        GreatBallEffect effect = GreatBallEffect.fromRule(
                "Look at the top 7 cards of your deck. You may reveal a Pokémon you find there and put it into your hand.");
        assertNotNull(effect);
        effect.apply(ctx(trainer("Great Ball", null), null));

        // Deja las cartas reveladas a la espera de RESOLVE_REVEAL (no toca la mano todavía).
        assertTrue(playerBoard.isHasPendingReveal());
        assertNotNull(playerBoard.getRevealedCards());
        assertEquals(3, playerBoard.getRevealedCards().size());
        assertTrue(playerBoard.getHand().isEmpty());
        assertTrue(playerBoard.getDeck().isEmpty());
    }

    // ─── Roller Skates ───────────────────────────────────────────────────────

    @Test
    void rollerSkates_routesToFlipCoinEffect() {
        TrainerCard card = trainer("Roller Skates", "[\"Flip a coin. If heads, draw 3 cards.\"]");
        List<TrainerEffect> effects = new TrainerEffectResolver().resolve(card);
        assertEquals(1, effects.size());
        assertInstanceOf(FlipCoinEffect.class, effects.get(0));
        assertEquals(3, ((FlipCoinEffect) effects.get(0)).getHeadsDrawAmount());
    }

    @Test
    void rollerSkates_drawsZeroOrThree() {
        for (int i = 0; i < 5; i++) playerBoard.getDeck().add(pokemon("d" + i, "Deck" + i, PokemonStage.BASIC, null, 50));

        FlipCoinEffect effect = FlipCoinEffect.fromRule("Flip a coin. If heads, draw 3 cards.");
        assertNotNull(effect);
        int before = playerBoard.getHand().size();
        effect.apply(ctx(trainer("Roller Skates", null), null));
        int after = playerBoard.getHand().size();

        // Cruz: no roba; Cara: roba 3.
        assertTrue(after == before || after == before + 3);
    }
}
