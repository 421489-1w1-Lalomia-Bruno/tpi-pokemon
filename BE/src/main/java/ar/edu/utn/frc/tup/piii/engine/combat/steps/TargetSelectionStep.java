package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

public class TargetSelectionStep implements AttackStep {

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        GameBoard board = context.getBoard();
        String currentPlayerId = context.getCurrentPlayerId();

        PlayerBoard opponentBoard = getOpponentBoard(board, currentPlayerId);
        if (opponentBoard == null) {
            context.setAttackSuccessful(false);
            context.setFailureReason("Opponent board not found");
            return;
        }

        PokemonInPlay defender = opponentBoard.getActivePokemon();
        if (defender == null) {
            context.setAttackSuccessful(false);
            context.setFailureReason("Opponent has no active Pokemon");
            return;
        }

        context.setDefender(defender);
        executeNext(context);
    }

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    private void executeNext(AttackContext context) {
        if (next != null && context.isAttackSuccessful()) {
            next.execute(context);
        }
    }

    private PlayerBoard getOpponentBoard(GameBoard board, String currentPlayerId) {
        if (board.getPlayer1Board() != null && !board.getPlayer1Board().getPlayerId().equals(currentPlayerId)) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null && !board.getPlayer2Board().getPlayerId().equals(currentPlayerId)) {
            return board.getPlayer2Board();
        }
        return null;
    }
}
