package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TurnManagerTest {

    private static final String MATCH_ID = "match-1";
    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";

    private TurnContext initialContext;

    private MatchSnapshot createSnapshot() {
        MatchSnapshot snap = new MatchSnapshot();
        snap.setPlayer1Id(P1_ID);
        snap.setPlayer2Id(P2_ID);
        snap.setTurnContext(initialContext);
        snap.setPlayer1TimeRemainingMs(1_200_000);
        snap.setPlayer2TimeRemainingMs(1_200_000);
        return snap;
    }
    private StatusEffectManager effectManager;
    private GameEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        initialContext = new TurnContext();
        initialContext.setTurnStartTimeMs(System.currentTimeMillis());
        effectManager = mock(StatusEffectManager.class);
        eventPublisher = mock(GameEventPublisher.class);
    }

    @Test
    void constructor_usesInitialContextCurrentPlayerId() {
        initialContext.setCurrentPlayerId(P2_ID);

        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        assertEquals(P2_ID, tm.getCurrentPlayerId(),
                "TurnManager should use the currentPlayerId from initialContext (P2 won coin flip)");
    }

    @Test
    void constructor_fallsBackToPlayer1IdWhenContextCurrentPlayerIdIsNull() {
        initialContext.setCurrentPlayerId(null);

        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        assertEquals(P1_ID, tm.getCurrentPlayerId(),
                "TurnManager should fall back to player1Id when initialContext currentPlayerId is null");
    }

    @Test
    void nextTurn_switchesPlayers() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.nextTurn(createSnapshot());

        assertEquals(P2_ID, tm.getCurrentPlayerId());
    }

    @Test
    void nextTurn_resetsIsFirstTurnOnContext() {
        initialContext.setCurrentPlayerId(P1_ID);
        initialContext.setFirstTurn(true);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        assertTrue(initialContext.isFirstTurn());
        tm.nextTurn(createSnapshot());
        assertFalse(initialContext.isFirstTurn(),
                "nextTurn should set isFirstTurn to false after the first player's turn ends");
    }

    @Test
    void nextTurn_resetsTurnFlags() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.getContext().setHasDrawn(true);
        tm.getContext().setHasPlayedSupporter(true);
        tm.getContext().setHasPlayedStadium(true);
        tm.getContext().setHasEvolved(true);
        tm.getContext().setHasAttachedEnergy(true);
        tm.getContext().setHasUsedAbility(true);
        tm.getContext().setHasRetreated(true);

        tm.nextTurn(createSnapshot());

        assertFalse(initialContext.isHasDrawn());
        assertFalse(initialContext.isHasPlayedSupporter());
        assertFalse(initialContext.isHasPlayedStadium());
        assertFalse(initialContext.isHasEvolved());
        assertFalse(initialContext.isHasAttachedEnergy());
        assertFalse(initialContext.isHasUsedAbility());
        assertFalse(initialContext.isHasRetreated());
    }

    @Test
    void executeFirstTurnDrawPhase_skipsDrawAndSetsActionsPhase() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());

        assertEquals(TurnPhase.ACTIONS, tm.getPhase());
        assertFalse(initialContext.isHasDrawn(),
                "First turn should not draw a card");
    }

    @Test
    void executeDrawPhase_onDeckOut_declaresLoser() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());
        tm.nextTurn(createSnapshot());

        GameBoard board = new GameBoard();
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(P2_ID);
        p1Board.setDeck(new ArrayList<>());
        board.setPlayer1Board(p1Board);
        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(P1_ID);
        p2Board.setDeck(new ArrayList<>());
        board.setPlayer2Board(p2Board);

        GameEngine engine = mock(GameEngine.class);
        tm.executeDrawPhase(engine, board);

        verify(engine).declareLoser(MATCH_ID, P2_ID, "Deck is empty, cannot draw");
    }

    @Test
    void canDraw_returnsTrueOnlyIfCurrentPlayerInDrawPhaseAndHasNotDrawn() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        assertFalse(tm.canDraw(P1_ID),
                "Should not be able to draw during first turn draw phase (draw is skipped)");

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());
        assertFalse(tm.canDraw(P1_ID), "Should not draw in ACTIONS phase");

        tm.getContext().setPhase(TurnPhase.DRAW);
        tm.getContext().setHasDrawn(false);
        assertTrue(tm.canDraw(P1_ID));

        tm.getContext().setHasDrawn(true);
        assertFalse(tm.canDraw(P1_ID), "Already drew this turn");

        assertFalse(tm.canDraw(P2_ID), "Not current player");
    }

    @Test
    void canPerformAction_returnsTrueOnlyForCurrentPlayerInActionsPhase() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());

        assertTrue(tm.canPerformAction(P1_ID, null));
        assertFalse(tm.canPerformAction(P2_ID, null), "Opponent cannot act on P1's turn");
    }

    @Test
    void executeDrawPhase_normalDraw_drawsCard() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());
        tm.nextTurn(createSnapshot());

        GameBoard board = new GameBoard();
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(P2_ID);
        p1Board.setDeck(new ArrayList<>(List.of(new ar.edu.utn.frc.tup.piii.engine.models.EnergyCard())));
        board.setPlayer1Board(p1Board);
        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(P1_ID);
        p2Board.setDeck(new ArrayList<>(List.of(new ar.edu.utn.frc.tup.piii.engine.models.EnergyCard())));
        board.setPlayer2Board(p2Board);

        tm.executeDrawPhase(mock(GameEngine.class), board);

        assertEquals(1, p1Board.getHand().size());
        assertTrue(initialContext.isHasDrawn());
        assertEquals(TurnPhase.ACTIONS, tm.getPhase());
    }

    @Test
    void executeDrawPhase_incrementsTurnCount() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeDrawPhase(mock(GameEngine.class), new GameBoard());
        assertEquals(1, tm.getTurnCount());

        tm.nextTurn(createSnapshot());

        GameBoard board = new GameBoard();
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(P2_ID);
        p1Board.setDeck(new ArrayList<>(List.of(new ar.edu.utn.frc.tup.piii.engine.models.EnergyCard())));
        board.setPlayer1Board(p1Board);
        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(P1_ID);
        p2Board.setDeck(new ArrayList<>());
        board.setPlayer2Board(p2Board);

        tm.executeDrawPhase(mock(GameEngine.class), board);

        assertEquals(2, tm.getTurnCount());
    }

    @Test
    void executeAttackPhase_setsPhaseToAttack() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        tm.executeAttackPhase(mock(GameEngine.class));

        assertEquals(TurnPhase.ATTACK, tm.getPhase());
    }

    @Test
    void executeBetweenTurns_processesEffectsAndPublishes() {
        initialContext.setCurrentPlayerId(P1_ID);
        TurnManager tm = new TurnManager(MATCH_ID, P1_ID, P2_ID, initialContext, effectManager, eventPublisher);

        GameBoard board = new GameBoard();
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(P1_ID);
        ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay active = new ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay();
        p1Board.setActivePokemon(active);
        board.setPlayer1Board(p1Board);
        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(P2_ID);
        board.setPlayer2Board(p2Board);

        tm.executeBetweenTurns(mock(GameEngine.class), board);

        verify(effectManager).processBetweenTurns(eq(active), eq(board), eq(MATCH_ID), eq(P1_ID), eq(eventPublisher), eq(SpecialCondition.ASLEEP), eq(SpecialCondition.PARALYZED));
        verify(eventPublisher).publish(eq(GameEventType.BETWEEN_TURNS_PROCESSED), eq(MATCH_ID), eq(P1_ID), anyString());
    }
}
