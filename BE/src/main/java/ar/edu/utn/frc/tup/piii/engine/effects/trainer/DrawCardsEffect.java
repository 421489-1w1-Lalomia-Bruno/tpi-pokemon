package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DrawCardsEffect implements TrainerEffect {

    private static final Pattern DRAW_PATTERN = Pattern.compile(
            "draw\\s+(?:up\\s+to\\s+)?(\\d+)\\s+cards?", Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_HAND_PATTERN = Pattern.compile(
            "discard\\s+your\\s+hand", Pattern.CASE_INSENSITIVE);

    private static final Pattern SHUFFLE_HAND_PATTERN = Pattern.compile(
            "shuffle\\s+your\\s+hand\\s+into\\s+your\\s+deck", Pattern.CASE_INSENSITIVE);

    private final int cardsToDraw;
    private final boolean discardHandFirst;
    private final boolean shuffleHandFirst;

    DrawCardsEffect(int cardsToDraw, boolean discardHandFirst, boolean shuffleHandFirst) {
        this.cardsToDraw = cardsToDraw;
        this.discardHandFirst = discardHandFirst;
        this.shuffleHandFirst = shuffleHandFirst;
    }

    public static DrawCardsEffect fromRule(String rule) {
        if (rule == null) return null;

        Matcher drawMatcher = DRAW_PATTERN.matcher(rule);
        if (!drawMatcher.find()) return null;

        int cardsToDraw = Integer.parseInt(drawMatcher.group(1));
        boolean discardHandFirst = DISCARD_HAND_PATTERN.matcher(rule).find();
        boolean shuffleHandFirst = SHUFFLE_HAND_PATTERN.matcher(rule).find();

        return new DrawCardsEffect(cardsToDraw, discardHandFirst, shuffleHandFirst);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        return ctx != null && ctx.getCurrentPlayerBoard() != null && cardsToDraw > 0;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        GameActionUtils.drawCards(board, cardsToDraw, discardHandFirst, shuffleHandFirst,
                ctx.getEventPublisher(), ctx.getMatchId(), ctx.getCurrentPlayerId(),
                ctx.getSourceCard().getName());
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if (discardHandFirst) sb.append("Discard your hand. ");
        else if (shuffleHandFirst) sb.append("Shuffle your hand into your deck. ");
        sb.append("Draw ").append(cardsToDraw).append(" card").append(cardsToDraw > 1 ? "s" : "");
        return sb.toString();
    }

    public int getCardsToDraw() { return cardsToDraw; }
    public boolean isDiscardHandFirst() { return discardHandFirst; }
    public boolean isShuffleHandFirst() { return shuffleHandFirst; }
}
