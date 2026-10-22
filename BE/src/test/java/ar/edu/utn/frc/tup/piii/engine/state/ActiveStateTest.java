package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ActiveStateTest {

    private GameEngine engine;
    private RuleValidator ruleValidator;
    private GameEventPublisher eventPublisher;
    private ActiveState state;
    private MatchSnapshot snapshot;
    private GameBoard board;
    private PlayerBoard p1Board;
    private PlayerBoard p2Board;
    private TurnContext turnContext;

    private static final String MATCH_ID = "match-1";
    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";

    @BeforeEach
    void setUp() {
        engine = mock(GameEngine.class);
        ruleValidator = mock(RuleValidator.class);
        eventPublisher = mock(GameEventPublisher.class);
        state = new ActiveState();

        snapshot = new MatchSnapshot();
        snapshot.setPlayer1Id(P1_ID);
        snapshot.setPlayer2Id(P2_ID);
        snapshot.setCurrentPlayerId(P1_ID);

        board = new GameBoard();
        p1Board = new PlayerBoard();
        p2Board = new PlayerBoard();
        p1Board.setPlayerId(P1_ID);
        p2Board.setPlayerId(P2_ID);

        PokemonCard p1Active = new PokemonCard();
        p1Active.setId("p1-active");
        p1Active.setName("Pikachu");
        p1Active.setHp(60);
        p1Active.setStage(PokemonStage.BASIC);
        p1Active.setRetreatCost(1);
        Attack thunderShock = new Attack("Thunder Shock", List.of(EnergyType.LIGHTNING, EnergyType.COLORLESS), 30, null);
        p1Active.setAttacks(List.of(thunderShock));
        p1Board.setActivePokemon(new PokemonInPlay(p1Active));

        PokemonCard p2Active = new PokemonCard();
        p2Active.setId("p2-active");
        p2Active.setName("Charmander");
        p2Active.setHp(50);
        p2Active.setStage(PokemonStage.BASIC);
        p2Active.setRetreatCost(1);
        p2Active.setAttacks(new ArrayList<>());
        p2Board.setActivePokemon(new PokemonInPlay(p2Active));

        p1Board.setDeck(new ArrayList<>(fillCards(20)));
        p2Board.setDeck(new ArrayList<>(fillCards(20)));

        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        snapshot.setBoard(board);

        turnContext = new TurnContext();
        turnContext.setCurrentPlayerId(P1_ID);
        turnContext.setPhase(TurnPhase.DRAW);
        turnContext.setFirstTurn(true);
        turnContext.setHasDrawn(false);
        snapshot.setTurnContext(turnContext);

        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getRuleValidator()).thenReturn(ruleValidator);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);
        when(engine.getCurrentState(MATCH_ID)).thenReturn(state);

        state.enter(engine, MATCH_ID);

        reset(engine, ruleValidator, eventPublisher);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snapshot);
        when(engine.getRuleValidator()).thenReturn(ruleValidator);
        when(engine.getEventPublisher()).thenReturn(eventPublisher);
        when(engine.getCurrentState(MATCH_ID)).thenReturn(state);
    }

    @Test
    void getPhase_returnsActive() {
        assertEquals(GamePhase.ACTIVE, state.getPhase());
    }

    @Test
    void enter_nullSnapshot_doesNothing() {
        ActiveState s = new ActiveState();
        when(engine.getSnapshot(MATCH_ID)).thenReturn(null);
        assertDoesNotThrow(() -> s.enter(engine, MATCH_ID));
    }

    @Test
    void enter_nullBoard_doesNothing() {
        ActiveState s = new ActiveState();
        MatchSnapshot snap = new MatchSnapshot();
        snap.setPlayer1Id(P1_ID);
        snap.setPlayer2Id(P2_ID);
        when(engine.getSnapshot(MATCH_ID)).thenReturn(snap);
        assertDoesNotThrow(() -> s.enter(engine, MATCH_ID));
    }

    @Test
    void exit_cleanUp() {
        assertDoesNotThrow(() -> state.exit(engine, MATCH_ID));
    }

    @Test
    void handleAction_surrender_callsDeclareLoser() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SURRENDER)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine).declareLoser(MATCH_ID, P1_ID, "Surrender");
    }

    @Test
    void handleAction_attack_transitionsPhases() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    @Test
    void handleAction_passTurn_transitionsPhases() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PASS_TURN)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    @Test
    void handleAction_passTurn_switchesPlayer() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PASS_TURN)
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals(P2_ID, turnContext.getCurrentPlayerId());
        assertEquals(TurnPhase.ACTIONS, turnContext.getPhase());
        assertFalse(turnContext.isFirstTurn(),
                "isFirstTurn should be false after the first player's turn ends");
    }

    @Test
    void handleAction_setActive_promotesBench() {
        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-1");
        benchCard.setName("Squirtle");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        PokemonInPlay benchPoke = new PokemonInPlay(benchCard);
        p1Board.getBench().add(benchPoke);

        turnContext.setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "bench-1"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("bench-1", p1Board.getActivePokemon().getCard().getId());
        assertFalse(p1Board.getBench().contains(benchPoke));
    }

    @Test
    void handleAction_setActive_invalidCardId_throws() {
        turnContext.setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "nonexistent"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_attachEnergy() {
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-1");
        energy.setName("Lightning Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getHand().add(energy);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACH_ENERGY)
                .payload(Map.of("cardId", "energy-1", "targetId", "p1-active"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertFalse(p1Board.getHand().contains(energy));
        PokemonInPlay active = p1Board.getActivePokemon();
        assertTrue(active.getAttachedEnergy().contains(energy));
        assertTrue(turnContext.isHasAttachedEnergy());
        verify(eventPublisher).publish(eq(GameEventType.ENERGY_ATTACHED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_attachEnergy_invalidCard_throws() {
        GameCard pokemon = p1Board.getActivePokemon().getCard();
        p1Board.getHand().add(pokemon);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACH_ENERGY)
                .payload(Map.of("cardId", pokemon.getId(), "targetId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_playTrainer_item() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("trainer-1");
        trainer.setName("Potion");
        trainer.setSubtype(TrainerSubtype.ITEM);
        p1Board.getHand().add(trainer);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "trainer-1"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertFalse(p1Board.getHand().contains(trainer));
        assertTrue(p1Board.getDiscardPile().contains(trainer));
        verify(eventPublisher).publish(eq(GameEventType.TRAINER_PLAYED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_playTrainer_supporter() {
        TrainerCard supporter = new TrainerCard();
        supporter.setId("supporter-1");
        supporter.setName("Professor's Research");
        supporter.setSubtype(TrainerSubtype.SUPPORTER);
        p1Board.getHand().add(supporter);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "supporter-1"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertTrue(turnContext.isHasPlayedSupporter());
        assertTrue(p1Board.getDiscardPile().contains(supporter));
    }

    @Test
    void handleAction_playTrainer_stadium() {
        TrainerCard stadium = new TrainerCard();
        stadium.setId("stadium-1");
        stadium.setName("Training Court");
        stadium.setSubtype(TrainerSubtype.STADIUM);
        p1Board.getHand().add(stadium);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "stadium-1"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertNotNull(board.getSharedStadium());
        assertEquals("stadium-1", board.getSharedStadium().getId());
        assertTrue(turnContext.isHasPlayedStadium());
    }

    @Test
    void handleAction_playTrainer_tool() {
        TrainerCard tool = new TrainerCard();
        tool.setId("tool-1");
        tool.setName("Choice Band");
        tool.setSubtype(TrainerSubtype.TOOL);
        p1Board.getHand().add(tool);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "tool-1", "targetId", "p1-active"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertNotNull(p1Board.getActivePokemon().getTool());
        assertEquals("tool-1", p1Board.getActivePokemon().getTool().getId());
        assertFalse(p1Board.getDiscardPile().contains(tool));
        assertEquals(0, p1Board.getDiscardPile().size());
    }

    @Test
    void handleAction_evolve() {
        PokemonCard base = new PokemonCard();
        base.setId("pichu-id");
        base.setName("Pichu");
        base.setHp(30);
        base.setStage(PokemonStage.BASIC);
        base.setAttacks(new ArrayList<>());
        PokemonInPlay pichu = new PokemonInPlay(base);
        p1Board.getBench().add(pichu);

        PokemonCard evolution = new PokemonCard();
        evolution.setId("pikachu-evo");
        evolution.setName("Pikachu");
        evolution.setHp(60);
        evolution.setStage(PokemonStage.STAGE1);
        evolution.setEvolvesFrom("Pichu");
        evolution.setAttacks(new ArrayList<>());
        p1Board.getHand().add(evolution);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.EVOLVE)
                .payload(Map.of("cardId", "pikachu-evo", "targetId", "pichu-id"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("Pikachu", pichu.getCard().getName());
        assertEquals(Integer.valueOf(60), pichu.getCurrentHp());
        assertTrue(pichu.getSpecialConditions().isEmpty());
        assertTrue(turnContext.isHasEvolved());
        verify(eventPublisher).publish(eq(GameEventType.POKEMON_EVOLVED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_evolve_wrongEvolvesFrom_throws() {
        PokemonCard wrongEvolution = new PokemonCard();
        wrongEvolution.setId("wrong-evo");
        wrongEvolution.setName("Raichu");
        wrongEvolution.setHp(90);
        wrongEvolution.setStage(PokemonStage.STAGE1);
        wrongEvolution.setEvolvesFrom("Pichu");
        wrongEvolution.setAttacks(new ArrayList<>());
        p1Board.getHand().add(wrongEvolution);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.EVOLVE)
                .payload(Map.of("cardId", "wrong-evo", "targetId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_retreat_swapsActiveAndBench() {
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-r");
        energy.setName("Electric Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getActivePokemon().getAttachedEnergy().add(energy);

        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-retreat");
        benchCard.setName("Squirtle");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        PokemonInPlay bench = new PokemonInPlay(benchCard);
        p1Board.getBench().add(bench);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of("targetId", "bench-retreat"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("bench-retreat", p1Board.getActivePokemon().getCard().getId());
        assertTrue(p1Board.getBench().stream()
                .anyMatch(p -> p.getCard().getId().equals("p1-active")));
        assertTrue(p1Board.getActivePokemon().getAttachedEnergy().isEmpty());
        assertTrue(turnContext.isHasRetreated());
        verify(eventPublisher).publish(eq(GameEventType.POKEMON_RETREATED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_retreat_insufficientEnergy_throws() {
        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-retreat");
        benchCard.setName("Squirtle");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(benchCard));

        PokemonCard activeCard = p1Board.getActivePokemon().getCard();
        activeCard.setRetreatCost(5);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of("targetId", "bench-retreat"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_useAbility() {
        Ability ability = new Ability();
        ability.setName("Static");
        ability.setEffectText("Paralyze on contact");

        PokemonCard activeCard = p1Board.getActivePokemon().getCard();
        activeCard.setAbilities(List.of(ability));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.USE_ABILITY)
                .payload(Map.of("cardId", "p1-active"))
                .build();

        assertDoesNotThrow(() -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_useAbility_noAbility_throws() {
        p1Board.getActivePokemon().getCard().setAbilities(new ArrayList<>());

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.USE_ABILITY)
                .payload(Map.of("cardId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_placePokemon_bench() {
        PokemonCard basic = new PokemonCard();
        basic.setId("new-basic");
        basic.setName("Bulbasaur");
        basic.setHp(60);
        basic.setStage(PokemonStage.BASIC);
        basic.setAttacks(new ArrayList<>());
        p1Board.getHand().add(basic);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", "new-basic"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertFalse(p1Board.getHand().contains(basic));
        assertTrue(p1Board.getBench().stream()
                .anyMatch(p -> p.getCard().getId().equals("new-basic")));
        verify(eventPublisher).publish(eq(GameEventType.POKEMON_PLACED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleAction_placePokemon_benchFull_throws() {
        PokemonCard basic = new PokemonCard();
        basic.setId("new-basic");
        basic.setName("Bulbasaur");
        basic.setHp(60);
        basic.setStage(PokemonStage.BASIC);
        basic.setAttacks(new ArrayList<>());
        p1Board.getHand().add(basic);

        for (int i = 0; i < 5; i++) {
            PokemonCard filler = new PokemonCard();
            filler.setId("filler-" + i);
            filler.setName("Filler " + i);
            filler.setHp(50);
            filler.setStage(PokemonStage.BASIC);
            filler.setAttacks(new ArrayList<>());
            p1Board.getBench().add(new PokemonInPlay(filler));
        }

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLACE_POKEMON)
                .payload(Map.of("cardId", "new-basic"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAction_nullSnapshot_throws() {
        when(engine.getSnapshot(MATCH_ID)).thenReturn(null);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
        assertTrue(ex.getMessage().contains("Match not found"));
    }

    @Test
    void handleAction_unknownActionType_throws() {
        doNothing().when(ruleValidator).validateAction(any(), anyString(), any());

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.READY)
                .build();

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
        assertTrue(ex.getMessage().contains("Unknown action") || ex.getMessage().contains("READY"));
    }

    @Test
    void handleAction_attachEnergy_invalidTargetId_throws() {
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-test");
        energy.setName("Test Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getHand().add(energy);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACH_ENERGY)
                .payload(Map.of("cardId", "energy-test", "targetId", "nonexistent-pokemon"))
                .build();

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
        assertTrue(ex.getMessage().contains("Target Pokemon not found"));
    }

    @Test
    void handleAction_retreat_noBench_throws() {
        p1Board.getActivePokemon().getCard().setRetreatCost(0);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of())
                .build();

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
        assertTrue(ex.getMessage().contains("No Pokemon on bench"));
    }

    @Test
    void handleAction_missingPayloadCardId_throws() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACH_ENERGY)
                .payload(Map.of())
                .build();

        InvalidActionException ex = assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
        assertTrue(ex.getMessage().contains("cardId is required"));
    }

    @Test
    void handleAttack_pokemonHasNoAttacks_throws() {
        p1Board.getActivePokemon().getCard().setAttacks(new ArrayList<>());

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleAttack_withKnockout_awardsPrizesAndRemovesDefender() {
        Attack tackle = new Attack("Tackle", List.of(), 30, null);
        PokemonCard attackerCard = p1Board.getActivePokemon().getCard();
        attackerCard.setAttacks(List.of(tackle));

        p2Board.getActivePokemon().setCurrentHp(10);
        p2Board.getActivePokemon().setActive(true);

        PokemonCard prize = new PokemonCard();
        prize.setId("prize-1");
        prize.setName("Prize Card");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p1Board.setPrizeCardsTaken(0);

        EnergyCard energy = new EnergyCard();
        energy.setId("energy-1");
        energy.setName("Lightning Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getActivePokemon().getAttachedEnergy().add(energy);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(eventPublisher).publish(eq(GameEventType.POKEMON_KNOCKED_OUT), eq(MATCH_ID), eq(P2_ID), anyString());
        verify(eventPublisher).publish(eq(GameEventType.PRIZE_NEEDED), eq(MATCH_ID), eq(P1_ID), anyString());
        assertNull(p2Board.getActivePokemon());
        assertEquals(1, p1Board.getPendingPrizeCount());
    }

    @Test
    void handleAttack_prizesPending_returnsEarly() {
        p1Board.setPendingPrizeCount(1);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    @Test
    void handleSetActive_betweenTurns_promotesBenchToActive() {
        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-1");
        benchCard.setName("Squirtle");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        PokemonInPlay benchPoke = new PokemonInPlay(benchCard);
        p1Board.getBench().add(benchPoke);

        turnContext.setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "bench-1"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("bench-1", p1Board.getActivePokemon().getCard().getId());
        assertTrue(p1Board.getBench().stream()
                .anyMatch(p -> p.getCard().getId().equals("p1-active")));
        assertFalse(p1Board.getBench().contains(benchPoke));
    }

    @Test
    void handleTakePrize_selectsCard_fromPrizePile() {
        PokemonCard prize = new PokemonCard();
        prize.setId("prize-take");
        prize.setName("Prize Card");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p1Board.setPrizeCardsTaken(0);
        p1Board.setPendingPrizeCount(1);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "prize-take"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertTrue(p1Board.getPrizeCards().isEmpty());
        assertTrue(p1Board.getHand().stream().anyMatch(c -> c.getId().equals("prize-take")));
        assertEquals(1, p1Board.getPrizeCardsTaken());
        assertEquals(0, p1Board.getPendingPrizeCount());
        verify(eventPublisher).publish(eq(GameEventType.PRIZE_TAKEN), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleTakePrize_noPendingPrizes_throws() {
        p1Board.setPendingPrizeCount(0);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "prize-take"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleRetreat_asleep_throws() {
        p1Board.getActivePokemon().getSpecialConditions().add(SpecialCondition.ASLEEP);

        EnergyCard energy = new EnergyCard();
        energy.setId("energy-r");
        energy.setName("Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getActivePokemon().getAttachedEnergy().add(energy);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleTakePrize_inActionsPhase_doesNotAdvanceTurn() {
        PokemonCard prize = new PokemonCard();
        prize.setId("prize-actions");
        prize.setName("Prize Actions");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p1Board.setPrizeCardsTaken(0);
        p1Board.setPendingPrizeCount(1);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "prize-actions"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertTrue(p1Board.getPrizeCards().isEmpty());
        assertEquals(1, p1Board.getPrizeCardsTaken());
        assertEquals(0, p1Board.getPendingPrizeCount());
        assertEquals(TurnPhase.ACTIONS, snapshot.getTurnContext().getPhase());
        assertEquals(P1_ID, snapshot.getTurnContext().getCurrentPlayerId());
    }

    @Test
    void handleTakePrize_inAttackPhase_advancesTurn() {
        PokemonCard prize = new PokemonCard();
        prize.setId("prize-attack");
        prize.setName("Prize Attack");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p1Board.setPrizeCardsTaken(0);
        p1Board.setPendingPrizeCount(1);

        snapshot.getTurnContext().setPhase(TurnPhase.ATTACK);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "prize-attack"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertTrue(p1Board.getPrizeCards().isEmpty());
        assertEquals(1, p1Board.getPrizeCardsTaken());
        assertEquals(0, p1Board.getPendingPrizeCount());
        assertEquals(P2_ID, snapshot.getTurnContext().getCurrentPlayerId());
    }

    @Test
    void handleAttack_withOpponentPendingPrize_returnsEarly() {
        Attack tackle = new Attack("Tackle", List.of(), 10, null);
        PokemonCard attackerCard = p1Board.getActivePokemon().getCard();
        attackerCard.setAttacks(List.of(tackle));

        p2Board.getActivePokemon().setCurrentHp(100);
        p2Board.setPendingPrizeCount(1);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
        assertNotNull(p2Board.getActivePokemon());
        assertEquals(TurnPhase.ATTACK, snapshot.getTurnContext().getPhase());
    }

    @Test
    void handleSetActive_betweenTurns_bothNeedPromotion_keepsWaiting() {
        PokemonCard bench1 = new PokemonCard();
        bench1.setId("p1-bench");
        bench1.setName("P1 Bench");
        bench1.setHp(60);
        bench1.setStage(PokemonStage.BASIC);
        bench1.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(bench1));

        PokemonCard bench2 = new PokemonCard();
        bench2.setId("p2-bench");
        bench2.setName("P2 Bench");
        bench2.setHp(60);
        bench2.setStage(PokemonStage.BASIC);
        bench2.setAttacks(new ArrayList<>());
        p2Board.getBench().add(new PokemonInPlay(bench2));

        p1Board.setActivePokemon(null);
        p2Board.setActivePokemon(null);

        snapshot.getTurnContext().setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "p1-bench"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("p1-bench", p1Board.getActivePokemon().getCard().getId());
        assertNull(p2Board.getActivePokemon());
        assertEquals(TurnPhase.BETWEEN_TURNS, snapshot.getTurnContext().getPhase());
    }

    @Test
    void handlePassTurn_poisonKO_pausesForPromotion() {
        PokemonCard prize = new PokemonCard();
        prize.setId("p2-prize-ko");
        prize.setName("P2 Prize");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p2Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p2Board.setPrizeCardsTaken(0);

        PokemonCard bench1 = new PokemonCard();
        bench1.setId("p1-bench-ko");
        bench1.setName("P1 Bench");
        bench1.setHp(60);
        bench1.setStage(PokemonStage.BASIC);
        bench1.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(bench1));

        PokemonInPlay active = p1Board.getActivePokemon();
        active.setCurrentHp(10);
        active.getSpecialConditions().add(SpecialCondition.POISONED);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PASS_TURN)
                .build();

        state.handleAction(engine, action, P1_ID);

        // Poison no se aplica al terminar el turno, solo al inicio del turno del controlador
        assertNotNull(p1Board.getActivePokemon());
        assertEquals(10, p1Board.getActivePokemon().getCurrentHp());
        assertTrue(p1Board.getActivePokemon().getSpecialConditions().contains(SpecialCondition.POISONED));
        assertEquals(0, p2Board.getPendingPrizeCount());
        assertEquals(TurnPhase.ACTIONS, snapshot.getTurnContext().getPhase());
        assertEquals(P2_ID, snapshot.getTurnContext().getCurrentPlayerId());
    }

    // ---- handleResolveReveal ----

    @Test
    void handleResolveReveal_withChosenCard_addsToHand() {
        PokemonCard revealedPoke = new PokemonCard();
        revealedPoke.setId("revealed-poke");
        revealedPoke.setName("Revealed Poke");
        revealedPoke.setHp(60);
        revealedPoke.setStage(PokemonStage.BASIC);
        revealedPoke.setAttacks(new ArrayList<>());
        p1Board.setRevealedCards(new ArrayList<>(List.of(revealedPoke)));
        p1Board.setHasPendingReveal(true);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RESOLVE_REVEAL)
                .payload(Map.of("cardId", "revealed-poke"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertFalse(p1Board.isHasPendingReveal());
        assertTrue(p1Board.getHand().contains(revealedPoke));
        assertNull(p1Board.getRevealedCards());
    }

    @Test
    void handleResolveReveal_noCardChosen_returnsToDeck() {
        PokemonCard revealedPoke = new PokemonCard();
        revealedPoke.setId("revealed-poke");
        revealedPoke.setName("Revealed Poke");
        revealedPoke.setHp(60);
        revealedPoke.setStage(PokemonStage.BASIC);
        revealedPoke.setAttacks(new ArrayList<>());
        p1Board.setRevealedCards(new ArrayList<>(List.of(revealedPoke)));
        p1Board.setHasPendingReveal(true);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RESOLVE_REVEAL)
                .payload(Map.of())
                .build();

        state.handleAction(engine, action, P1_ID);

        assertFalse(p1Board.isHasPendingReveal());
        assertNull(p1Board.getRevealedCards());
        assertFalse(p1Board.getHand().contains(revealedPoke));
    }

    @Test
    void handleResolveReveal_noPendingReveal_throws() {
        p1Board.setHasPendingReveal(false);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RESOLVE_REVEAL)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleRetreat without targetId, auto-selects first bench ----

    @Test
    void handleRetreat_withoutTargetId_selectsFirstBench() {
        EnergyCard energy = new EnergyCard();
        energy.setId("energy-r");
        energy.setName("Electric Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getActivePokemon().getAttachedEnergy().add(energy);

        PokemonCard benchCard1 = new PokemonCard();
        benchCard1.setId("bench-1");
        benchCard1.setName("Squirtle");
        benchCard1.setHp(60);
        benchCard1.setStage(PokemonStage.BASIC);
        benchCard1.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(benchCard1));

        PokemonCard benchCard2 = new PokemonCard();
        benchCard2.setId("bench-2");
        benchCard2.setName("Bulbasaur");
        benchCard2.setHp(60);
        benchCard2.setStage(PokemonStage.BASIC);
        benchCard2.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(benchCard2));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of())
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("bench-1", p1Board.getActivePokemon().getCard().getId());
        assertTrue(turnContext.isHasRetreated());
    }

    // ---- handleRetreat while paralyzed ----

    @Test
    void handleRetreat_paralyzed_throws() {
        p1Board.getActivePokemon().getSpecialConditions().add(SpecialCondition.PARALYZED);

        EnergyCard energy = new EnergyCard();
        energy.setId("energy-r");
        energy.setName("Energy");
        energy.setEnergyType(EnergyType.LIGHTNING);
        p1Board.getActivePokemon().getAttachedEnergy().add(energy);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.RETREAT)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleSetActive with pending prizes ----

    @Test
    void handleSetActive_withPendingPrizes_keepsBetweenTurns() {
        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-prize");
        benchCard.setName("Bench Prize");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(benchCard));

        p1Board.setPendingPrizeCount(1);
        turnContext.setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "bench-prize"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals(TurnPhase.BETWEEN_TURNS, snapshot.getTurnContext().getPhase());
        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    // ---- handleSetActive in non-BETWEEN_TURNS phase ----

    @Test
    void handleSetActive_inActionsPhase_setsPhaseToActions() {
        PokemonCard benchCard = new PokemonCard();
        benchCard.setId("bench-actions");
        benchCard.setName("Bench Actions");
        benchCard.setHp(60);
        benchCard.setStage(PokemonStage.BASIC);
        benchCard.setAttacks(new ArrayList<>());
        p1Board.getBench().add(new PokemonInPlay(benchCard));

        turnContext.setPhase(TurnPhase.ACTIONS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SET_ACTIVE)
                .payload(Map.of("cardId", "bench-actions"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals(TurnPhase.ACTIONS, snapshot.getTurnContext().getPhase());
        assertEquals("bench-actions", p1Board.getActivePokemon().getCard().getId());
    }

    // ---- handlePlayTrainer: stadium replacement ----

    @Test
    void handlePlayTrainer_replacesExistingStadium() {
        TrainerCard oldStadium = new TrainerCard();
        oldStadium.setId("old-stadium");
        oldStadium.setName("Old Stadium");
        oldStadium.setSubtype(TrainerSubtype.STADIUM);
        board.setSharedStadium(oldStadium);

        TrainerCard newStadium = new TrainerCard();
        newStadium.setId("new-stadium");
        newStadium.setName("New Stadium");
        newStadium.setSubtype(TrainerSubtype.STADIUM);
        p1Board.getHand().add(newStadium);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "new-stadium"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("new-stadium", board.getSharedStadium().getId());
        assertTrue(p1Board.getDiscardPile().contains(oldStadium));
        assertTrue(turnContext.isHasPlayedStadium());
    }

    // ---- handlePlayTrainer: tool replacement ----

    @Test
    void handlePlayTrainer_replacesExistingTool() {
        TrainerCard oldTool = new TrainerCard();
        oldTool.setId("old-tool");
        oldTool.setName("Old Tool");
        oldTool.setSubtype(TrainerSubtype.TOOL);
        p1Board.getActivePokemon().setTool(oldTool);

        TrainerCard newTool = new TrainerCard();
        newTool.setId("new-tool");
        newTool.setName("New Tool");
        newTool.setSubtype(TrainerSubtype.TOOL);
        p1Board.getHand().add(newTool);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "new-tool", "targetId", "p1-active"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertEquals("new-tool", p1Board.getActivePokemon().getTool().getId());
        assertTrue(p1Board.getDiscardPile().contains(oldTool));
    }

    // ---- handleAttack: no active pokemon ----

    @Test
    void handleAttack_noActivePokemon_throws() {
        p1Board.setActivePokemon(null);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleAttack: flip attack ----

    @Test
    void handleAttack_withFlipAttack_resolvesDamage() {
        Attack flipAttack = new Attack("Coin Flip", List.of(EnergyType.COLORLESS), 10,
                "Flip a coin. If heads, this attack does 40 more damage.");
        p1Board.getActivePokemon().getCard().setAttacks(List.of(flipAttack));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .payload(Map.of("attackIndex", 0))
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    // ---- handleTakePrize in BETWEEN_TURNS continues turn ----

    @Test
    void handleTakePrize_inBetweenTurns_advancesTurn() {
        PokemonCard prize = new PokemonCard();
        prize.setId("prize-bt");
        prize.setName("Prize BT");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));
        p1Board.setPendingPrizeCount(1);
        snapshot.getTurnContext().setPhase(TurnPhase.BETWEEN_TURNS);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "prize-bt"))
                .build();

        state.handleAction(engine, action, P1_ID);

        assertTrue(p1Board.getPrizeCards().isEmpty());
        assertEquals(0, p1Board.getPendingPrizeCount());
    }

    // ---- handleUseAbility with abilityIndex ----

    @Test
    void handleUseAbility_withAbilityIndex() {
        Ability ability = new Ability();
        ability.setName("Quick Draw");
        ability.setEffectText("Once per turn, draw a card.");

        p1Board.getActivePokemon().getCard().setAbilities(List.of(ability));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.USE_ABILITY)
                .payload(Map.of("cardId", "p1-active", "abilityIndex", "0"))
                .build();

        assertDoesNotThrow(() -> state.handleAction(engine, action, P1_ID));
        verify(eventPublisher).publish(eq(GameEventType.ABILITY_USED), eq(MATCH_ID), eq(P1_ID), anyString());
    }

    @Test
    void handleSurrender_savesSnapshotBeforeDeclareLoser() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SURRENDER)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine).declareLoser(MATCH_ID, P1_ID, "Surrender");
    }

    // ---- handlePlayTrainer: supporter already played throws ----

    @Test
    void handlePlayTrainer_supporterRepeated_throws() {
        TrainerCard supporter = new TrainerCard();
        supporter.setId("supporter-2");
        supporter.setName("Second Supporter");
        supporter.setSubtype(TrainerSubtype.SUPPORTER);
        p1Board.getHand().add(supporter);
        turnContext.setHasPlayedSupporter(true);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "supporter-2"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handlePlayTrainer: card not found ----

    @Test
    void handlePlayTrainer_cardNotFound_throws() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "nonexistent"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handlePlayTrainer: card not a trainer ----

    @Test
    void handlePlayTrainer_cardNotTrainer_throws() {
        PokemonCard pokemon = new PokemonCard();
        pokemon.setId("not-trainer");
        pokemon.setName("Not Trainer");
        pokemon.setHp(60);
        pokemon.setStage(PokemonStage.BASIC);
        p1Board.getHand().add(pokemon);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.PLAY_TRAINER)
                .payload(Map.of("cardId", "not-trainer"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleEvolve: card not found ----

    @Test
    void handleEvolve_cardNotFound_throws() {
        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.EVOLVE)
                .payload(Map.of("cardId", "nonexistent", "targetId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleEvolve: card not a pokemon ----

    @Test
    void handleEvolve_cardNotPokemon_throws() {
        TrainerCard trainer = new TrainerCard();
        trainer.setId("evo-not-pokemon");
        trainer.setName("Not Pokemon");
        trainer.setSubtype(TrainerSubtype.ITEM);
        p1Board.getHand().add(trainer);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.EVOLVE)
                .payload(Map.of("cardId", "evo-not-pokemon", "targetId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleEvolve: target not found ----

    @Test
    void handleEvolve_targetNotFound_throws() {
        PokemonCard evolution = new PokemonCard();
        evolution.setId("evo-card");
        evolution.setName("Evo Card");
        evolution.setHp(80);
        evolution.setStage(PokemonStage.STAGE1);
        evolution.setEvolvesFrom("Pichu");
        p1Board.getHand().add(evolution);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.EVOLVE)
                .payload(Map.of("cardId", "evo-card", "targetId", "nonexistent-target"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleUseAbility already used throws ----

    @Test
    void handleUseAbility_alreadyUsed_throws() {
        Ability ability = new Ability();
        ability.setName("Test Ability");
        ability.setEffectText("Once per turn.");
        p1Board.getActivePokemon().getCard().setAbilities(List.of(ability));
        turnContext.setHasUsedAbility(true);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.USE_ABILITY)
                .payload(Map.of("cardId", "p1-active"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleUseAbility with multi-use ability ----

    @Test
    void handleUseAbility_multiUse_allowsRepeatedUse() {
        Ability ability = new Ability();
        ability.setName("Free Use");
        ability.setEffectText("Use as often as you like during your turn.");
        p1Board.getActivePokemon().getCard().setAbilities(List.of(ability));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.USE_ABILITY)
                .payload(Map.of("cardId", "p1-active"))
                .build();

        assertDoesNotThrow(() -> state.handleAction(engine, action, P1_ID));
        assertDoesNotThrow(() -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleAttack: fail + confuse self-damage ----

    @Test
    void handleAttack_confusedSelfHit_triggersKO() {
        Attack tackle = new Attack("Tackle", List.of(), 0, null);
        p1Board.getActivePokemon().getCard().setAttacks(List.of(tackle));
        p1Board.getActivePokemon().setCurrentHp(10);
        p1Board.getActivePokemon().getSpecialConditions().add(SpecialCondition.CONFUSED);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(eventPublisher, atLeast(0)).publish(eq(GameEventType.ATTACK_FAILED), anyString(), anyString(), anyString());
    }

    // ---- handleAttack with faceDownCardsForReveal (hand disruption after success) ----

    @Test
    void handleAttack_withFaceDownCards_pausesForReveal() {
        // Use existing attack that has 10 damage so it succeeds but triggers face-down path
        Attack tackle = new Attack("Astonish", List.of(), 10,
                "Your opponent reveals their hand. Choose a card to shuffle into their deck.");
        p1Board.getActivePokemon().getCard().setAttacks(List.of(tackle));

        // p2 active needs to survive to trigger face-down
        p2Board.getActivePokemon().setCurrentHp(100);
        p2Board.getActivePokemon().setActive(true);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    // ---- handleTakePrize with invalid card ----

    @Test
    void handleTakePrize_invalidCard_throws() {
        p1Board.setPendingPrizeCount(1);
        PokemonCard prize = new PokemonCard();
        prize.setId("real-prize");
        prize.setName("Real Prize");
        prize.setHp(50);
        prize.setStage(PokemonStage.BASIC);
        p1Board.setPrizeCards(new ArrayList<>(List.of(prize)));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.TAKE_PRIZE)
                .payload(Map.of("cardId", "nonexistent"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    // ---- handleAttack with attackIndex ----

    @Test
    void handleAttack_withAttackIndex() {
        Attack tackle = new Attack("Tackle", List.of(), 10, null);
        Attack kick = new Attack("Kick", List.of(), 20, null);
        p1Board.getActivePokemon().getCard().setAttacks(List.of(tackle, kick));

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.ATTACK)
                .payload(Map.of("attackIndex", 1))
                .build();

        state.handleAction(engine, action, P1_ID);

        verify(engine, atLeastOnce()).saveSnapshot(eq(MATCH_ID), any());
    }

    // ---- handleSelectTarget ----

    @Test
    void handleSelectTarget_noPendingTargets_throws() {
        p1Board.setPendingMultiCoinTargets(0);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SELECT_TARGET)
                .payload(Map.of("targetId", "some-target"))
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    @Test
    void handleSelectTarget_emptyTargetId_throws() {
        p1Board.setPendingMultiCoinTargets(1);

        GameActionMessage action = GameActionMessage.builder()
                .matchId(MATCH_ID)
                .type(GameActionType.SELECT_TARGET)
                .payload(Map.of())
                .build();

        assertThrows(InvalidActionException.class,
                () -> state.handleAction(engine, action, P1_ID));
    }

    private List<GameCard> fillCards(int count) {
        List<GameCard> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PokemonCard card = new PokemonCard();
            card.setId("filler-" + i);
            card.setName("Filler " + i);
            card.setHp(50);
            card.setStage(PokemonStage.BASIC);
            card.setAttacks(new ArrayList<>());
            cards.add(card);
        }
        return cards;
    }
}
