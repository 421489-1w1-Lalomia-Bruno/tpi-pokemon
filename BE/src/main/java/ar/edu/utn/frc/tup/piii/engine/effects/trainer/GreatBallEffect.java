package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GreatBallEffect implements TrainerEffect {

    private static final Pattern LOOK_PATTERN = Pattern.compile(
            "look\\s+at\\s+the\\s+top\\s+(\\d+)\\s+cards?", Pattern.CASE_INSENSITIVE);

    private final int lookCount;

    GreatBallEffect(int lookCount) {
        this.lookCount = lookCount;
    }

    public static GreatBallEffect fromRule(String rule) {
        if (rule == null) return null;
        java.util.regex.Matcher matcher = LOOK_PATTERN.matcher(rule);
        if (!matcher.find()) return null;
        int lookCount = Integer.parseInt(matcher.group(1));
        return new GreatBallEffect(lookCount);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        return ctx != null && ctx.getCurrentPlayerBoard() != null
                && ctx.getCurrentPlayerBoard().getDeck().size() >= lookCount;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        List<GameCard> deck = board.getDeck();

        int count = Math.min(lookCount, deck.size());
        List<GameCard> revealed = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            revealed.add(deck.remove(0));
        }

        board.setRevealedCards(revealed);
        board.setHasPendingReveal(true);

        if (ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    GameEventType.REVEAL_CARDS,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName() + ": revealed top " + count + " cards of deck");
        }
    }

    @Override
    public String getDescription() {
        return "Look at the top " + lookCount + " cards of your deck. Choose a Pokemon, reveal it, and put it into your hand. Shuffle the other cards back into your deck.";
    }

    public int getLookCount() { return lookCount; }
}
