package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.GameTestFactory;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TargetSelectionStepTest {

    private TargetSelectionStep sut;

    @BeforeEach
    void setUp() {
        sut = new TargetSelectionStep();
        sut.setNext(new AttackStep() {
            @Override
            public void execute(AttackContext context) {
            }
            @Override
            public void setNext(AttackStep next) {
            }
        });
    }

    @Test
    void execute_bothPlayersPresent_setsDefender() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PokemonInPlay defender = GameTestFactory.createPokemonInPlay();
        GameBoard board = GameTestFactory.createGameBoard("p1", attacker, "p2", defender);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, null, null, board);
        ctx.setCurrentPlayerId("p1");

        sut.execute(ctx);

        assertNotNull(ctx.getDefender());
    }

    @Test
    void execute_noOpponentBoard_fails() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, null, null, board);
        ctx.setCurrentPlayerId("p1");

        sut.execute(ctx);

        assertFalse(ctx.isAttackSuccessful());
        assertNotNull(ctx.getFailureReason());
    }

    @Test
    void execute_opponentNoActive_fails() {
        PokemonInPlay attacker = GameTestFactory.createPokemonInPlay();
        PlayerBoard p1 = GameTestFactory.createPlayerBoardWithActive("p1", attacker);
        PlayerBoard p2 = GameTestFactory.createPlayerBoard("p2");
        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1);
        board.setPlayer2Board(p2);
        AttackContext ctx = GameTestFactory.createAttackContext(attacker, null, null, board);
        ctx.setCurrentPlayerId("p1");

        sut.execute(ctx);

        assertFalse(ctx.isAttackSuccessful());
    }
}
