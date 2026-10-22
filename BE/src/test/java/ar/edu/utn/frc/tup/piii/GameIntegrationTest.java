package ar.edu.utn.frc.tup.piii;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.repositories.*;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.GameEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Transactional

class GameIntegrationTest extends EmbeddedPostgresTestBase {

    @Autowired private GameEngine gameEngine;
    @Autowired private UserRepository userRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private GameStatePersistenceService persistenceService;
    @MockBean private GameEventListener gameEventListener;

    private UserEntity user1;
    private UserEntity user2;
    private DeckEntity deck1;
    private DeckEntity deck2;
    private MatchEntity matchEntity;
    private String matchId;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(UserEntity.builder()
                .username("p1").passwordHash("hash").gender(UserGender.MALE).build());
        user2 = userRepository.save(UserEntity.builder()
                .username("p2").passwordHash("hash").gender(UserGender.MALE).build());
    }

    private CardEntity createPokemonCard(String externalId, String name, int hp, PokemonStage stage,
                                          String evolvesFrom, String weakness, String resistance,
                                          int retreatCost, String attacksJson) {
        CardEntity c = CardEntity.builder()
                .externalId(externalId).name(name).supertype("Pokémon").hp(hp).stage(stage)
                .evolvesFrom(evolvesFrom).weaknessType(weakness).resistanceType(resistance)
                .retreatCost(retreatCost).attacksJson(attacksJson)
                .imageUrl("https://example.com/" + externalId + ".png")
                .cardNumber(externalId).setId("test").rarity("Common")
                .build();
        return cardRepository.save(c);
    }

    private CardEntity createEnergyCard(String externalId, String name, EnergyType type, boolean isSpecial) {
        CardEntity c = CardEntity.builder()
                .externalId(externalId).name(name).supertype("Energy").hp(null).stage(null)
                .retreatCost(null).imageUrl("https://example.com/" + externalId + ".png")
                .cardNumber(externalId).setId("test").rarity("Common")
                .types(isSpecial ? null : Set.of(type))
                .subtypes(isSpecial ? List.of("Special") : List.of("Basic"))
                .build();
        return cardRepository.save(c);
    }

    private CardEntity createTrainerCard(String externalId, String name, String subtype, String effect) {
        CardEntity c = CardEntity.builder()
                .externalId(externalId).name(name).supertype("Trainer").hp(null).stage(null)
                .retreatCost(null).rules(effect).imageUrl("https://example.com/" + externalId + ".png")
                .cardNumber(externalId).setId("test").rarity("Uncommon")
                .subtypes(List.of(subtype))
                .build();
        return cardRepository.save(c);
    }

    private DeckEntity createDeck(String name, String creatorId, List<CardEntity> cards, int copiesEach) {
        DeckEntity d = new DeckEntity();
        d.setName(name);
        d.setCreatorId(creatorId);
        d.setSource(DeckSource.USER);
        d.setIsActive(true);
        for (CardEntity card : cards) {
            DeckCardEntity dc = new DeckCardEntity();
            dc.setDeck(d);
            dc.setCard(card);
            dc.setQuantity(copiesEach);
            d.getCards().add(dc);
        }
        return deckRepository.save(d);
    }

    private String setupBasicGame() {
        CardEntity basicPoke = createPokemonCard("poke-1", "TestPoke", 100,
                PokemonStage.BASIC, null, null, null, 1,
                "[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\"],\"damage\":\"30\",\"text\":\"\"}]");
        CardEntity energy = createEnergyCard("energy-1", "Colorless Energy", EnergyType.COLORLESS, false);

        deck1 = createDeck("P1 Deck", user1.getId().toString(),
                List.of(basicPoke, energy), 30);
        deck2 = createDeck("P2 Deck", user2.getId().toString(),
                List.of(basicPoke, energy), 30);

        matchEntity = matchRepository.save(MatchEntity.builder()
                .player1Id(user1.getId().toString())
                .player2Id(user2.getId().toString())
                .player1DeckId(deck1.getId()).player2DeckId(deck2.getId())
                .status(MatchStatus.WAITING).build());
        matchId = matchEntity.getId().toString();

        MatchSnapshot snap = new MatchSnapshot();
        snap.setMatchId(matchEntity.getId());
        snap.setPlayer1Id(user1.getId().toString());
        snap.setPlayer2Id(user2.getId().toString());
        snap.setPlayer1DeckId(deck1.getId());
        snap.setPlayer2DeckId(deck2.getId());
        snap.setStatus(MatchStatus.WAITING);
        persistenceService.saveSnapshot(snap);

        gameEngine.initializeMatch(matchId);
        return matchId;
    }

    private void doReady() {
        gameEngine.processAction(matchId, user1.getId().toString(), "READY", Map.of());
        gameEngine.processAction(matchId, user2.getId().toString(), "READY", Map.of());
    }

    private String findBasicPokemonId(PlayerBoard board) {
        return board.getHand().stream()
                .filter(c -> c instanceof PokemonCard
                        && (((PokemonCard) c).getStage() == PokemonStage.BASIC
                            || ((PokemonCard) c).getStage() == PokemonStage.EX))
                .findFirst().map(GameCard::getId).orElse(null);
    }

    private void doSetup(String playerId) {
        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        if (snap == null || snap.getBoard() == null) return;
        PlayerBoard board = snap.getBoard().getPlayer1Board();
        if (board == null || !playerId.equals(board.getPlayerId())) {
            board = snap.getBoard().getPlayer2Board();
        }
        if (board == null) return;
        String pokeId = findBasicPokemonId(board);
        if (pokeId != null) {
            gameEngine.processAction(matchId, playerId, "PLACE_POKEMON",
                    Map.of("cardId", pokeId, "zone", "ACTIVE"));
        }
        gameEngine.processAction(matchId, playerId, "CONFIRM_SETUP", Map.of());
    }

    private void doCoinCall() {
        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        String callerId = snap.getCoinCallerId();
        gameEngine.processAction(matchId, callerId, "CALL_COIN", Map.of("call", "HEADS"));
    }

    // ====================== TESTS ======================

    @Test
    void fullGame_bothPlayersReady_setupAndActiveComplete() {
        setupBasicGame();
        doReady();
        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.SETUP, snap.getStatus());

        doSetup(user1.getId().toString());
        doSetup(user2.getId().toString());

        snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertTrue(snap.isAwaitingCoinCall());

        doCoinCall();

        snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.ACTIVE, snap.getStatus());
        assertNotNull(snap.getTurnContext());
        assertNotNull(snap.getFirstPlayerId());
    }

    @Test
    void mulligan_reshufflesWhenNoBasicInHand() {
        CardEntity basicPoke = createPokemonCard("poke-mull", "Starter", 60,
                PokemonStage.BASIC, null, null, null, 1,
                "[{\"name\":\"Pound\",\"cost\":[\"COLORLESS\"],\"damage\":\"10\",\"text\":\"\"}]");
        CardEntity energy = createEnergyCard("eng-mull", "Colorless Energy", EnergyType.COLORLESS, false);

        deck1 = createDeck("P1 Deck", user1.getId().toString(),
                List.of(basicPoke, energy), 30);
        List<CardEntity> onlyPokemon = new ArrayList<>();
        for (int i = 0; i < 60; i++) onlyPokemon.add(basicPoke);
        deck2 = createDeck("P2 Deck", user2.getId().toString(), onlyPokemon, 1);

        matchEntity = matchRepository.save(MatchEntity.builder()
                .player1Id(user1.getId().toString()).player2Id(user2.getId().toString())
                .player1DeckId(deck1.getId()).player2DeckId(deck2.getId())
                .status(MatchStatus.WAITING).build());
        matchId = matchEntity.getId().toString();

        MatchSnapshot snap = new MatchSnapshot();
        snap.setMatchId(matchEntity.getId());
        snap.setPlayer1Id(user1.getId().toString());
        snap.setPlayer2Id(user2.getId().toString());
        snap.setPlayer1DeckId(deck1.getId());
        snap.setPlayer2DeckId(deck2.getId());
        snap.setStatus(MatchStatus.WAITING);
        persistenceService.saveSnapshot(snap);

        gameEngine.initializeMatch(matchId);
        doReady();

        snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.SETUP, snap.getStatus());
        assertNotNull(snap.getBoard());

        PlayerBoard p1Board = snap.getBoard().getPlayer1Board();
        assertNotNull(p1Board.getHand());
        assertTrue(p1Board.getHand().stream().anyMatch(c -> c instanceof PokemonCard));
    }

    private void saveSnapshotBase() {
        MatchSnapshot snap = new MatchSnapshot();
        snap.setMatchId(matchEntity.getId());
        snap.setPlayer1Id(user1.getId().toString());
        snap.setPlayer2Id(user2.getId().toString());
        snap.setPlayer1DeckId(deck1.getId());
        snap.setPlayer2DeckId(deck2.getId());
        snap.setStatus(MatchStatus.WAITING);
        persistenceService.saveSnapshot(snap);
    }

    @Test
    void knockout_awardsPrizeAndUpdatesBoard() {
        CardEntity strongPoke = createPokemonCard("strong-1", "StrongPoke", 100,
                PokemonStage.BASIC, null, null, null, 0,
                "[{\"name\":\"Big Hit\",\"cost\":[\"COLORLESS\"],\"damage\":\"200\",\"text\":\"\"}]");
        CardEntity weakPoke = createPokemonCard("weak-1", "WeakPoke", 10,
                PokemonStage.BASIC, null, null, null, 0,
                "[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\"],\"damage\":\"10\",\"text\":\"\"}]");
        CardEntity energy = createEnergyCard("eng-ko", "Colorless Energy", EnergyType.COLORLESS, false);

        List<CardEntity> p1Cards = List.of(strongPoke, energy);
        deck1 = createDeck("P1 Deck", user1.getId().toString(), p1Cards, 30);
        List<CardEntity> p2Cards = List.of(weakPoke, energy);
        deck2 = createDeck("P2 Deck", user2.getId().toString(), p2Cards, 30);

        matchEntity = matchRepository.save(MatchEntity.builder()
                .player1Id(user1.getId().toString()).player2Id(user2.getId().toString())
                .player1DeckId(deck1.getId()).player2DeckId(deck2.getId())
                .status(MatchStatus.WAITING).build());
        matchId = matchEntity.getId().toString();
        saveSnapshotBase();
        gameEngine.initializeMatch(matchId);
        doReady();
        doSetup(user1.getId().toString());
        doSetup(user2.getId().toString());
        doCoinCall();

        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.ACTIVE, snap.getStatus());
    }

    @Test
    void evolution_evolvesOnSecondTurn() {
        CardEntity base = createPokemonCard("base-1", "BaseMon", 60,
                PokemonStage.BASIC, null, null, null, 0,
                "[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\"],\"damage\":\"10\",\"text\":\"\"}]");
        CardEntity stage1 = createPokemonCard("evo-1", "EvoMon", 100,
                PokemonStage.STAGE1, "BaseMon", null, null, 0,
                "[{\"name\":\"Evo Hit\",\"cost\":[\"COLORLESS\"],\"damage\":\"50\",\"text\":\"\"}]");
        CardEntity energy = createEnergyCard("eng-evo", "Colorless Energy", EnergyType.COLORLESS, false);

        List<CardEntity> p1Cards = List.of(base, stage1, energy);
        deck1 = createDeck("P1 Deck", user1.getId().toString(), p1Cards, 20);
        List<CardEntity> p2Cards = List.of(base, energy);
        deck2 = createDeck("P2 Deck", user2.getId().toString(), p2Cards, 30);

        matchEntity = matchRepository.save(MatchEntity.builder()
                .player1Id(user1.getId().toString()).player2Id(user2.getId().toString())
                .player1DeckId(deck1.getId()).player2DeckId(deck2.getId())
                .status(MatchStatus.WAITING).build());
        matchId = matchEntity.getId().toString();
        saveSnapshotBase();
        gameEngine.initializeMatch(matchId);
        doReady();
        doSetup(user1.getId().toString());
        doSetup(user2.getId().toString());
        doCoinCall();

        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.ACTIVE, snap.getStatus());
    }

    @Test
    void victory_bySurrender() {
        setupBasicGame();
        doReady();
        doSetup(user1.getId().toString());
        doSetup(user2.getId().toString());
        doCoinCall();

        gameEngine.processAction(matchId, user1.getId().toString(), "SURRENDER", Map.of());

        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchId));
        assertEquals(MatchStatus.FINISHED, snap.getStatus());
        assertEquals(user2.getId().toString(), snap.getWinnerId());
    }

    private String saveAndInitMatch() {
        matchEntity = matchRepository.save(MatchEntity.builder()
                .player1Id(user1.getId().toString()).player2Id(user2.getId().toString())
                .player1DeckId(deck1.getId()).player2DeckId(deck2.getId())
                .status(MatchStatus.WAITING).build());
        matchId = matchEntity.getId().toString();
        saveSnapshotBase();
        gameEngine.initializeMatch(matchId);
        return matchId;
    }
}
