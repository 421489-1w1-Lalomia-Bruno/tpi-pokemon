package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreAttackEffectStepTest {

    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";

    private PreAttackEffectStep sut;
    private boolean nextExecuted;

    @BeforeEach
    void setUp() {
        sut = new PreAttackEffectStep();
        nextExecuted = false;
        sut.setNext(new AttackStep() {
            @Override
            public void execute(AttackContext context) {
                nextExecuted = true;
            }
            @Override
            public void setNext(AttackStep next) {
            }
        });
    }

    @Test
    void execute_delegatesToNext() {
        AttackContext ctx = GameTestFactory.createAttackContext();

        sut.execute(ctx);

        assertTrue(nextExecuted);
    }

    @Test
    void execute_whenAttackFailed_doesNotProceed() {
        AttackContext ctx = GameTestFactory.createAttackContext();
        ctx.setAttackSuccessful(false);

        sut.execute(ctx);

        assertFalse(nextExecuted);
    }

    @Test
    void execute_withHandDisruption_setsFaceDownCards() {
        Attack attack = new Attack("Astonish", List.of(EnergyType.COLORLESS), 10,
                "Choose a random card from your opponent's hand and shuffle it into their deck.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameCard card1 = GameTestFactory.createBasicEnergyCard("c1", EnergyType.FIRE);
        GameCard card2 = GameTestFactory.createBasicEnergyCard("c2", EnergyType.WATER);
        p2Board.getHand().add(card1);
        p2Board.getHand().add(card2);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertNotNull(ctx.getFaceDownCardsForReveal());
        assertEquals(2, ctx.getFaceDownCardsForReveal().size());
        assertEquals("opp-hand-0", ctx.getFaceDownCardsForReveal().get(0).getId());
        assertEquals("opp-hand-1", ctx.getFaceDownCardsForReveal().get(1).getId());
        assertNotNull(ctx.getOpponentHandForReveal());
        assertEquals(2, ctx.getOpponentHandForReveal().size());
        assertEquals(P2_ID, ctx.getOpponentPlayerIdForReveal());
    }

    @Test
    void execute_withHandDisruption_opponentEmptyHand_doesNothing() {
        Attack attack = new Attack("Astonish", List.of(EnergyType.COLORLESS), 10,
                "Choose a random card from your opponent's hand and shuffle it into their deck.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertNull(ctx.getFaceDownCardsForReveal());
    }

    @Test
    void execute_withoutHandDisruption_doesNothing() {
        Attack attack = new Attack("Tackle", List.of(EnergyType.COLORLESS), 30,
                "Deal 30 damage.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        GameCard card1 = GameTestFactory.createBasicEnergyCard("c1", EnergyType.FIRE);
        p2Board.getHand().add(card1);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertNull(ctx.getFaceDownCardsForReveal());
        assertNull(ctx.getOpponentHandForReveal());
    }

    @Test
    void execute_withHandDisruption_matchesShuffleIntoDeck() {
        Attack attack = new Attack("Confusion Ray", List.of(EnergyType.PSYCHIC), 30,
                "Choose a random card from your opponent's hand and shuffle it back into their deck.");
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1Board = GameTestFactory.createPlayerBoardWithActive(P1_ID, attacker);
        PlayerBoard p2Board = GameTestFactory.createPlayerBoardWithActive(P2_ID, defender);
        p2Board.getHand().add(new PokemonCard());
        p2Board.getHand().add(new PokemonCard());
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, defender, attack, board);
        ctx.setCurrentPlayerId(P1_ID);

        sut.execute(ctx);

        assertTrue(nextExecuted);
        assertNotNull(ctx.getFaceDownCardsForReveal());
        assertEquals(2, ctx.getFaceDownCardsForReveal().size());
    }
}
