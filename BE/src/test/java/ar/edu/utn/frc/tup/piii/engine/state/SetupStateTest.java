package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SetupStateTest {

    private GameEngine engine;
    private GameEventPublisher eventPublisher;
    private SetupState state;
    private MatchSnapshot snapshot;
    private static final String MATCH_ID = "match-1";
    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";
    private static final UUID P1_DECK_ID = UUID.randomUUID();
    private static final UUID P2_DECK_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        engine = mock(GameEngine.class);
        eventPublisher = mock(GameEventPublisher.class);
        state = new SetupState();
        snapshot = new MatchSnapshot();
        snapshot.setPlayer1Id(P1_ID);
        snapshot.setPlayer2Id(P2_ID);
        snapshot.setPlayer1DeckId(P1_DECK_ID);
        snapshot.setPlayer2DeckId(P2_DECK_ID);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);
    }

    @Test
    void getPhase_returnsSetup() {
        assertEquals(GamePhase.SETUP, state.getPhase());
    }

    @Test
    void enter_handlesNullSnapshot() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(null);
        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));
    }

    @Test
    void enter_buildsPlayerBoards() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
        verify(eventPublisher).publish(eq(GameEventType.PHASE_CHANGED), eq(MATCH_ID), isNull(), anyString());
        verify(eventPublisher, never()).publish(eq(GameEventType.MULLIGAN_EXECUTED), eq(MATCH_ID), anyString(), anyString());
    }

    @Test
    void exit_cleansUp() {
        assertDoesNotThrow(() -> state.exit(engine, MATCH_ID));
    }

    @Test
    void handleAction_placePokemon_activeZone() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);

        GameCard pikachu = findCardInHand(snapshot.getBoard().getPlayer1Board(), "pikachu");
        assertNotNull(pikachu);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", pikachu.getId(), "zone", "ACTIVE"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertNotNull(snapshot.getBoard().getPlayer1Board().getActivePokemon());
        assertEquals(pikachu.getId(), snapshot.getBoard().getPlayer1Board().getActivePokemon().getCard().getId());
        verify(eventPublisher).publish(eq(GameEventType.POKEMON_PLACED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_placePokemon_benchZone() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithTwoBasics("bulbasaur", "squirtle"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard bulbasaur = findCardInHand(p1Board, "bulbasaur");
        assertNotNull(bulbasaur);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", bulbasaur.getId(), "zone", "BENCH"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals(1, p1Board.getBench().size());
        assertEquals(bulbasaur.getId(), p1Board.getBench().get(0).getCard().getId());
    }

    @Test
    void handleAction_nonPlacePokemon_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_nonPokemonCard_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        TrainerCard trainer = new TrainerCard();
        trainer.setId("trainer-1");
        trainer.setName("Potion");
        trainer.setSubtype(TrainerSubtype.ITEM);
        p1Board.getHand().add(trainer);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", trainer.getId(), "zone", "ACTIVE"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_nonBasicPokemon_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        PokemonCard raichu = new PokemonCard();
        raichu.setId("raichu-id");
        raichu.setName("Raichu");
        raichu.setHp(90);
        raichu.setStage(PokemonStage.STAGE1);
        raichu.setEvolvesFrom("Pikachu");
        p1Board.getHand().add(raichu);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", raichu.getId(), "zone", "ACTIVE"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_bothPlayersConfirmSetup_finalizes() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);

        GameCard p1Card = snapshot.getBoard().getPlayer1Board().getHand().stream()
                .filter(c -> c instanceof PokemonCard)
                .findFirst().orElse(null);
        GameCard p2Card = snapshot.getBoard().getPlayer2Board().getHand().stream()
                .filter(c -> c instanceof PokemonCard)
                .findFirst().orElse(null);

        GameActionMessage placeAction1 = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", p1Card.getId(), "zone", "ACTIVE"))
                .build();
        GameActionMessage placeAction2 = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", p2Card.getId(), "zone", "ACTIVE"))
                .build();

        state.handleAction(engine, placeAction1, P1_ID);
        state.handleAction(engine, placeAction2, P2_ID);

        GameActionMessage confirm1 = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CONFIRM_SETUP)
                .payload(Map.of())
                .build();
        GameActionMessage confirm2 = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CONFIRM_SETUP)
                .payload(Map.of())
                .build();

        state.handleAction(engine, confirm1, P1_ID);
        state.handleAction(engine, confirm2, P2_ID);

        // Tras confirmar ambos, se espera el sorteo cara/cruz (no se pasa a ACTIVE aún).
        assertTrue(snapshot.isAwaitingCoinCall());
        assertNotNull(snapshot.getCoinCallerId());
        assertEquals(MatchStatus.SETUP, snapshot.getStatus());
        verify(engine, never()).transitionTo(eq(MATCH_ID), isA(ActiveState.class));

        // El jugador llamador elige cara; se tira la moneda y arranca la fase activa.
        GameActionMessage callCoin = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CALL_COIN)
                .payload(Map.of("call", "HEADS"))
                .build();
        state.handleAction(engine, callCoin, snapshot.getCoinCallerId());

        assertFalse(snapshot.isAwaitingCoinCall());
        assertEquals(MatchStatus.ACTIVE, snapshot.getStatus());
        assertNotNull(snapshot.getFirstPlayerId());
        verify(engine).transitionTo(eq(MATCH_ID), isA(ActiveState.class));
        verify(eventPublisher, times(2)).publish(eq(GameEventType.POKEMON_PLACED), eq(MATCH_ID), anyString(), anyString());
    }

    @Test
    void enter_deckWithoutBasic_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithoutBasic());
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        assertThrows(InvalidActionException.class, () -> state.enter(engine, MATCH_ID));
    }

    @Test
    void enter_allBasicsInHand_proceedsToSetup() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildFullBasicDeck());
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildFullBasicDeck());

        state.enter(engine, MATCH_ID);

        assertEquals(MatchStatus.SETUP, snapshot.getStatus());
        verify(engine, never()).declareLoser(anyString(), anyString(), anyString());
    }

    @Test
    void handleAction_activeTakenBenchFull_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard pikachu = findCardInHand(p1Board, "pikachu");
        assertNotNull(pikachu);

        GameActionMessage actionActive = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", pikachu.getId(), "zone", "ACTIVE"))
                .build();
        state.handleAction(engine, actionActive, P1_ID);
        assertNotNull(p1Board.getActivePokemon());

        for (int i = 0; i < 5; i++) {
            PokemonCard filler = new PokemonCard();
            filler.setId("bench-" + i);
            filler.setName("Bench " + i);
            filler.setHp(50);
            filler.setStage(PokemonStage.BASIC);
            filler.setAttacks(new ArrayList<>());
            p1Board.getBench().add(new PokemonInPlay(filler));
        }
        assertEquals(5, p1Board.getBench().size());

        PokemonCard extra = new PokemonCard();
        extra.setId("extra-poke");
        extra.setName("Extra Basic");
        extra.setHp(50);
        extra.setStage(PokemonStage.BASIC);
        extra.setAttacks(new ArrayList<>());
        p1Board.getHand().add(extra);

        GameActionMessage actionExtra = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", extra.getId(), "zone", "ACTIVE"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, actionExtra, P1_ID));
    }

    @Test
    void enter_nullDeckId_throws() {
        snapshot.setPlayer1DeckId(null);
        snapshot.setPlayer2DeckId(null);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getMatchEntity(MATCH_ID)).thenReturn(null);

        assertThrows(InvalidActionException.class,
                () -> state.enter(engine, MATCH_ID));
    }

    @Test
    void enter_resolvesDeckIdsFromMatchEntity() {
        snapshot.setPlayer1DeckId(null);
        snapshot.setPlayer2DeckId(null);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setPlayer1DeckId(P1_DECK_ID);
        matchEntity.setPlayer2DeckId(P2_DECK_ID);
        when(engine.getMatchEntity(MATCH_ID)).thenReturn(matchEntity);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        assertEquals(P1_DECK_ID, snapshot.getPlayer1DeckId());
        assertEquals(P2_DECK_ID, snapshot.getPlayer2DeckId());
        assertEquals(MatchStatus.SETUP, snapshot.getStatus());
    }

    @Test
    void handleConfirmSetup_withoutActivePokemon_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CONFIRM_SETUP)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleConfirmSetup_onePlayerDoesNotFinalize() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard pikachu = findCardInHand(p1Board, "pikachu");
        assertNotNull(pikachu);

        GameActionMessage placeAction = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", pikachu.getId(), "zone", "ACTIVE"))
                .build();
        state.handleAction(engine, placeAction, P1_ID);

        GameActionMessage confirmAction = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CONFIRM_SETUP)
                .payload(Map.of())
                .build();
        state.handleAction(engine, confirmAction, P1_ID);

        verify(engine, never()).transitionTo(anyString(), any());
    }

    @Test
    void handlePlacePokemon_invalidZone_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard pikachu = findCardInHand(p1Board, "pikachu");

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", pikachu.getId(), "zone", "INVALID"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handlePlacePokemon_activeFree_choosesBench_placesOnBench() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithTwoBasics("bulbasaur", "squirtle"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard bulbasaur = findCardInHand(p1Board, "bulbasaur");
        assertNotNull(bulbasaur);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", bulbasaur.getId(), "zone", "BENCH"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertNull(p1Board.getActivePokemon());
        assertEquals(1, p1Board.getBench().size());
        assertEquals(bulbasaur.getId(), p1Board.getBench().get(0).getCard().getId());
    }

    @Test
    void handleAction_nullSnapshot_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(null);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", "any", "zone", "ACTIVE"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- SetupState: mulligan path ----

    @Test
    void enter_mulligan_reshufflesWhenNoBasicInHand() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        List<GameCard> deck1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PokemonCard p = new PokemonCard();
            p.setId("basic-" + i);
            p.setName("Basic " + i);
            p.setHp(60);
            p.setStage(PokemonStage.BASIC);
            p.setAttacks(new ArrayList<>());
            deck1.add(p);
        }
        for (int i = 10; i < 14; i++) {
            EnergyCard e = new EnergyCard();
            e.setId("energy-" + i);
            e.setName("Energy " + i);
            deck1.add(e);
        }

        List<GameCard> deck2 = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            PokemonCard p = new PokemonCard();
            p.setId("p2-basic-" + i);
            p.setName("P2 Basic " + i);
            p.setHp(60);
            p.setStage(PokemonStage.BASIC);
            p.setAttacks(new ArrayList<>());
            deck2.add(p);
        }

        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(deck1);
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(deck2);

        state.enter(engine, MATCH_ID);

        assertNotNull(snapshot.getBoard());
        assertEquals(MatchStatus.SETUP, snapshot.getStatus());
        assertTrue(snapshot.getBoard().getPlayer1Board().getHand().stream()
                .anyMatch(c -> c instanceof PokemonCard));
    }

    @Test
    void enter_mulliganGivesOpponentExtraCards() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        List<GameCard> deck1 = new ArrayList<>();
        PokemonCard basic = new PokemonCard();
        basic.setId("p1-only-basic");
        basic.setName("P1 Only Basic");
        basic.setHp(60);
        basic.setStage(PokemonStage.BASIC);
        basic.setAttacks(new ArrayList<>());
        deck1.add(basic);
        for (int i = 0; i < 13; i++) {
            EnergyCard e = new EnergyCard();
            e.setId("p1-energy-" + i);
            e.setName("P1 Energy " + i);
            deck1.add(e);
        }

        List<GameCard> deck2 = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            PokemonCard p = new PokemonCard();
            p.setId("p2-basic-" + i);
            p.setName("P2 Basic " + i);
            p.setHp(60);
            p.setStage(PokemonStage.BASIC);
            p.setAttacks(new ArrayList<>());
            deck2.add(p);
        }

        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(deck1);
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(deck2);

        state.enter(engine, MATCH_ID);

        assertNotNull(snapshot.getBoard());
        assertEquals(MatchStatus.SETUP, snapshot.getStatus());
    }

    // ---- SetupState: coin call edge cases ----

    @Test
    void handleCoinCall_notAwaiting_throws() {
        snapshot.setAwaitingCoinCall(false);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CALL_COIN)
                .payload(Map.of("call", "HEADS"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleCoinCall_wrongCaller_throws() {
        snapshot.setAwaitingCoinCall(true);
        snapshot.setCoinCallerId(P2_ID);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CALL_COIN)
                .payload(Map.of("call", "HEADS"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleCoinCall_invalidCall_throws() {
        snapshot.setAwaitingCoinCall(true);
        snapshot.setCoinCallerId(P1_ID);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CALL_COIN)
                .payload(Map.of("call", "INVALID"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- SetupState: place pokemon edge cases ----

    @Test
    void handlePlacePokemon_noPayload_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));
        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(null)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handlePlacePokemon_cardNotInHand_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));
        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", "nonexistent-id", "zone", "ACTIVE"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- SetupState: enter with null publisher ----

    @Test
    void enter_nullPublisher_doesNotThrow() {
        when(engine.getEventPublisher()).thenReturn(null);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithBasic("pikachu"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));
    }

    @Test
    void handleAction_confirmWithoutSetupInit_throws() {
        snapshot.setStatus(MatchStatus.SETUP);
        snapshot.setBoard(new GameBoard());
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CONFIRM_SETUP)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void enter_mulliganWithNullPublisher_doesNotThrow() {
        // Deck with no basic in first draw (10 energy cards + 1 basic)
        List<GameCard> deck1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            EnergyCard e = new EnergyCard();
            e.setId("p1-energy-" + i);
            e.setName("P1 Energy " + i);
            deck1.add(e);
        }
        PokemonCard basic = new PokemonCard();
        basic.setId("p1-basic");
        basic.setName("P1 Basic");
        basic.setHp(60);
        basic.setStage(PokemonStage.BASIC);
        basic.setAttacks(new ArrayList<>());
        deck1.add(basic);

        List<GameCard> deck2 = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            PokemonCard p = new PokemonCard();
            p.setId("p2-basic-" + i);
            p.setName("P2 Basic " + i);
            p.setHp(60);
            p.setStage(PokemonStage.BASIC);
            p.setAttacks(new ArrayList<>());
            deck2.add(p);
        }

        when(engine.getEventPublisher()).thenReturn(null);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(deck1);
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(deck2);

        assertDoesNotThrow(() -> state.enter(engine, MATCH_ID));
    }

    // ---- SetupState: confirm without active pokemon ----

    @Test
    void handlePlacePokemon_activeTaken_benchNotFull_placesOnBench() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(buildDeckWithTwoBasics("bulbasaur", "squirtle"));
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));
        state.enter(engine, MATCH_ID);

        reset(engine);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        PlayerBoard p1Board = snapshot.getBoard().getPlayer1Board();
        GameCard bulbasaur = findCardInHand(p1Board, "bulbasaur");
        GameCard squirtle = findCardInHand(p1Board, "squirtle");

        GameActionMessage activeAction = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", bulbasaur.getId(), "zone", "ACTIVE"))
                .build();
        state.handleAction(engine, activeAction, P1_ID);
        assertNotNull(p1Board.getActivePokemon());

        GameActionMessage benchAction = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", squirtle.getId(), "zone", "ACTIVE"))
                .build();
        state.handleAction(engine, benchAction, P1_ID);

        assertEquals(1, p1Board.getBench().size());
        assertEquals(squirtle.getId(), p1Board.getBench().get(0).getCard().getId());
    }

    // ---- SetupState: coin call with null payload ----

    @Test
    void handleCoinCall_nullPayload_throws() {
        snapshot.setAwaitingCoinCall(true);
        snapshot.setCoinCallerId(P1_ID);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.CALL_COIN)
                .payload(null)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- SetupState: enter with empty deck for one player ----

    @Test
    void enter_emptyPlayerDeck_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.loadDeckCards(P1_DECK_ID)).thenReturn(new ArrayList<>());
        when(engine.loadDeckCards(P2_DECK_ID)).thenReturn(buildDeckWithBasic("charmander"));

        assertThrows(InvalidActionException.class,
                () -> state.enter(engine, MATCH_ID));
    }

    private List<GameCard> buildDeckWithBasic(String name) {
        PokemonCard pokemon = new PokemonCard();
        pokemon.setId(name + "-id");
        pokemon.setName(name);
        pokemon.setHp(60);
        pokemon.setStage(PokemonStage.BASIC);
        pokemon.setAttacks(new ArrayList<>());
        List<GameCard> deck = new ArrayList<>();
        deck.add(pokemon);
        for (int i = 0; i < 5; i++) {
            PokemonCard filler = new PokemonCard();
            filler.setId("filler-" + i);
            filler.setName("Filler " + i);
            filler.setHp(50);
            filler.setStage(PokemonStage.BASIC);
            filler.setAttacks(new ArrayList<>());
            deck.add(filler);
        }
        return deck;
    }

    private List<GameCard> buildDeckWithTwoBasics(String name1, String name2) {
        List<GameCard> deck = buildDeckWithBasic(name1);
        PokemonCard second = new PokemonCard();
        second.setId(name2 + "-id");
        second.setName(name2);
        second.setHp(60);
        second.setStage(PokemonStage.BASIC);
        second.setAttacks(new ArrayList<>());
        deck.add(second);
        return deck;
    }

    private List<GameCard> buildDeckWithoutBasic() {
        List<GameCard> deck = new ArrayList<>();
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-1");
        energy.setName("Electric Energy");
        deck.add(energy);
        return deck;
    }

    private List<GameCard> buildFullBasicDeck() {
        List<GameCard> deck = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            PokemonCard pokemon = new PokemonCard();
            pokemon.setId("basic-" + i);
            pokemon.setName("Basic " + i);
            pokemon.setHp(60);
            pokemon.setStage(PokemonStage.BASIC);
            pokemon.setAttacks(new ArrayList<>());
            deck.add(pokemon);
        }
        return deck;
    }

    private GameCard findCardInHand(PlayerBoard board, String name) {
        for (GameCard card : board.getHand()) {
            if (card.getName().toLowerCase().contains(name.toLowerCase())) {
                return card;
            }
        }
        return null;
    }
}
