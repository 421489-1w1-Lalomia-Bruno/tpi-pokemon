package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreAttackEffectStep implements AttackStep {

    private static final Pattern HAND_DISRUPTION_PATTERN = Pattern.compile(
        "random card from (?:your )?opponent'?s? hand.*?(?:shuffles?|shuffle)\\s.*?(?:into|back into).*?deck",
        Pattern.CASE_INSENSITIVE);

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        Attack attack = context.getSelectedAttack();
        if (attack != null && attack.getEffectText() != null) {
            Matcher matcher = HAND_DISRUPTION_PATTERN.matcher(attack.getEffectText());
            if (matcher.find()) {
                executeHandDisruption(context);
            }
        }
        executeNext(context);
    }

    private void executeHandDisruption(AttackContext context) {
        GameBoard board = context.getBoard();
        String currentPlayerId = context.getCurrentPlayerId();
        if (board == null || currentPlayerId == null) return;

        PlayerBoard opponentBoard = null;
        String opponentPlayerId = null;
        if (board.getPlayer1Board() != null && !currentPlayerId.equals(board.getPlayer1Board().getPlayerId())) {
            opponentBoard = board.getPlayer1Board();
            opponentPlayerId = board.getPlayer1Board().getPlayerId();
        } else if (board.getPlayer2Board() != null && !currentPlayerId.equals(board.getPlayer2Board().getPlayerId())) {
            opponentBoard = board.getPlayer2Board();
            opponentPlayerId = board.getPlayer2Board().getPlayerId();
        }

        if (opponentBoard == null) return;

        List<GameCard> opponentHand = opponentBoard.getHand();
        if (opponentHand == null || opponentHand.isEmpty()) return;

        List<GameCard> faceDownCards = new ArrayList<>();
        for (int i = 0; i < opponentHand.size(); i++) {
            faceDownCards.add(new HiddenCard("opp-hand-" + i));
        }

        context.setOpponentHandForReveal(new ArrayList<>(opponentHand));
        context.setOpponentPlayerIdForReveal(opponentPlayerId);
        context.setFaceDownCardsForReveal(faceDownCards);
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
}
