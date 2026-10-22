package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;

public class VictoryConditionChecker {

    private static final int MAX_PRIZES = 6;

    public String checkVictory(MatchSnapshot snapshot) {
        if (snapshot == null || snapshot.getBoard() == null) {
            return null;
        }

        GameBoard board = snapshot.getBoard();
        PlayerBoard p1Board = board.getPlayer1Board();
        PlayerBoard p2Board = board.getPlayer2Board();

        if (p1Board == null || p2Board == null) {
            return null;
        }

        String currentPlayerId = snapshot.getCurrentPlayerId();
        if (currentPlayerId == null) return null;

        PlayerBoard currentBoard = currentPlayerId.equals(p1Board.getPlayerId()) ? p1Board : p2Board;
        PlayerBoard opponentBoard = currentBoard == p1Board ? p2Board : p1Board;

        if (checkAllPrizesTaken(currentBoard)) {
            return currentPlayerId;
        }

        if (checkAllPrizesTaken(opponentBoard)) {
            return getOpponentId(snapshot, currentPlayerId);
        }

        if (checkNoPokemonInPlay(opponentBoard)) {
            return currentPlayerId;
        }

        if (checkNoPokemonInPlay(currentBoard)) {
            return getOpponentId(snapshot, currentPlayerId);
        }

        if (checkDeckOut(currentBoard, snapshot)) {
            return getOpponentId(snapshot, currentPlayerId);
        }

        if (checkDeckOut(opponentBoard, snapshot)) {
            return currentPlayerId;
        }

        return null;
    }

    private boolean checkAllPrizesTaken(PlayerBoard board) {
        return board.getPrizeCardsTaken() != null && board.getPrizeCardsTaken() >= MAX_PRIZES;
    }

    private boolean checkNoPokemonInPlay(PlayerBoard board) {
        boolean activeNull = board.getActivePokemon() == null;
        boolean benchEmpty = board.getBench() == null || board.getBench().isEmpty();
        return activeNull && benchEmpty;
    }

    private boolean checkDeckOut(PlayerBoard board, MatchSnapshot snapshot) {
        TurnContext turnCtx = snapshot.getTurnContext();
        if (turnCtx == null) return false;
        boolean isEmptyDeck = board.getDeck() == null || board.getDeck().isEmpty();
        boolean isDrawPhase = turnCtx.getPhase() == TurnPhase.DRAW;
        return isEmptyDeck && isDrawPhase;
    }

    private String getOpponentId(MatchSnapshot snapshot, String playerId) {
        return snapshot.getPlayer1Id().equals(playerId)
                ? snapshot.getPlayer2Id()
                : snapshot.getPlayer1Id();
    }
}