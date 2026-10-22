package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlipCoinEffect implements TrainerEffect {

    private static final Pattern COIN_FLIP_PATTERN = Pattern.compile(
            "flip\\s+a\\s+coin|coin\\s+flip", Pattern.CASE_INSENSITIVE);

    private static final Pattern HEADS_DRAW_PATTERN = Pattern.compile(
            "heads.*?draw\\s+(\\d+)\\s+cards?", Pattern.CASE_INSENSITIVE);

    private final int headsDrawAmount;

    FlipCoinEffect(int headsDrawAmount) {
        this.headsDrawAmount = headsDrawAmount;
    }

    public static FlipCoinEffect fromRule(String rule) {
        if (rule == null) return null;
        if (!COIN_FLIP_PATTERN.matcher(rule).find()) return null;

        Matcher headsDrawMatcher = HEADS_DRAW_PATTERN.matcher(rule);
        int headsDraw = headsDrawMatcher.find() ? Integer.parseInt(headsDrawMatcher.group(1)) : 0;

        return new FlipCoinEffect(headsDraw);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        return ctx != null && ctx.getCurrentPlayerBoard() != null;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        boolean heads = CoinFlipManager.flip();

        if (ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    GameEventType.COIN_FLIP_RESULT,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    "Coin flip: " + (heads ? "Heads" : "Tails"),
                    ctx.getSourceCard().getName(),
                    null,
                    heads ? 1 : 0
            );
        }

        if (heads && headsDrawAmount > 0) {
            GameActionUtils.drawCards(board, headsDrawAmount,
                    ctx.getEventPublisher(), ctx.getMatchId(), ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName());
        }
    }

    @Override
    public String getDescription() {
        return "Flip a coin. If heads, draw " + headsDrawAmount + " card" + (headsDrawAmount > 1 ? "s" : "") + ".";
    }

    public int getHeadsDrawAmount() { return headsDrawAmount; }
}
