package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class VictoryConditionCheckerTest {

    private VictoryConditionChecker checker;
    private MatchSnapshot snapshot;
    private GameBoard board;
    private PlayerBoard p1Board;
    private PlayerBoard p2Board;
    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";

    @BeforeEach
    void setUp() {
        checker = new VictoryConditionChecker();
        snapshot = new MatchSnapshot();
        board = new GameBoard();
        p1Board = new PlayerBoard();
        p2Board = new PlayerBoard();

        p1Board.setPlayerId(P1_ID);
        p2Board.setPlayerId(P2_ID);

        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        snapshot.setBoard(board);
        snapshot.setPlayer1Id(P1_ID);
        snapshot.setPlayer2Id(P2_ID);
    }

    @Test
    void checkVictory_noWinner_returnsNull() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(new PokemonInPlay());
        p1Board.setPrizeCardsTaken(3);
        p2Board.setActivePokemon(new PokemonInPlay());
        p2Board.setPrizeCardsTaken(3);

        assertNull(checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_opponentNoPokemon_returnsCurrentPlayer() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(new PokemonInPlay());
        p2Board.setActivePokemon(null);
        p2Board.setBench(new ArrayList<>());

        assertEquals(P1_ID, checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_currentPlayerNoPokemon_returnsOpponent() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(null);
        p1Board.setBench(new ArrayList<>());
        p2Board.setActivePokemon(new PokemonInPlay());

        assertEquals(P2_ID, checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_currentPlayerTookAllPrizes_returnsCurrentPlayer() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(new PokemonInPlay());
        p1Board.setPrizeCardsTaken(6);
        p2Board.setActivePokemon(new PokemonInPlay());
        p2Board.setPrizeCardsTaken(0);

        assertEquals(P1_ID, checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_opponentTookAllPrizes_returnsOpponent() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(new PokemonInPlay());
        p1Board.setPrizeCardsTaken(0);
        p2Board.setActivePokemon(new PokemonInPlay());
        p2Board.setPrizeCardsTaken(6);

        assertEquals(P2_ID, checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_nullSnapshot_returnsNull() {
        assertNull(checker.checkVictory(null));
    }

    @Test
    void checkVictory_nullBoard_returnsNull() {
        snapshot.setBoard(null);
        assertNull(checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_nullPlayerBoard_returnsNull() {
        board.setPlayer1Board(null);
        snapshot.setCurrentPlayerId(P1_ID);
        assertNull(checker.checkVictory(snapshot));
    }

    @Test
    void checkVictory_bothNoPokemonInPlay_returnsCurrentPlayer() {
        snapshot.setCurrentPlayerId(P1_ID);
        p1Board.setActivePokemon(null);
        p1Board.setBench(new ArrayList<>());
        p2Board.setActivePokemon(null);
        p2Board.setBench(new ArrayList<>());

        assertEquals(P1_ID, checker.checkVictory(snapshot));
    }
}
