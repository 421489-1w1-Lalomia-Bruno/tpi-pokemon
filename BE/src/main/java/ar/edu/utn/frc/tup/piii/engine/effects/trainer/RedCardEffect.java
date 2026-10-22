package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Efecto tipo "Red Card": el rival baraja su mano dentro de su mazo y luego
 * roba una cantidad fija de cartas.
 *
 * Ejemplo: "Your opponent shuffles his or her hand into his or her deck and draws 4 cards."
 */
public class RedCardEffect implements TrainerEffect {

    private static final Pattern DRAW_PATTERN = Pattern.compile(
            "draws?\\s+(\\d+)\\s+cards?", Pattern.CASE_INSENSITIVE);

    private final int cardsToDraw;

    RedCardEffect(int cardsToDraw) {
        this.cardsToDraw = cardsToDraw;
    }

    public static RedCardEffect fromRule(String rule) {
        if (rule == null) return null;
        String lower = rule.toLowerCase();

        boolean targetsOpponent = lower.contains("opponent");
        boolean shufflesHand = lower.contains("shuffle") && lower.contains("hand");
        if (!targetsOpponent || !shufflesHand) {
            return null;
        }

        int cardsToDraw = 0;
        Matcher m = DRAW_PATTERN.matcher(rule);
        if (m.find()) {
            cardsToDraw = Integer.parseInt(m.group(1));
        }
        return new RedCardEffect(cardsToDraw);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        return ctx != null && ctx.getOpponentBoard() != null;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard opponent = ctx.getOpponentBoard();
        GameActionUtils.redCardEffect(opponent, cardsToDraw,
                ctx.getEventPublisher(), ctx.getMatchId(), ctx.getCurrentPlayerId(),
                ctx.getSourceCard().getName());
    }

    @Override
    public String getDescription() {
        return "Opponent shuffles their hand into their deck and draws " + cardsToDraw + " cards";
    }

    public int getCardsToDraw() { return cardsToDraw; }
}
