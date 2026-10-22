package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchDeckEffect implements TrainerEffect {

    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "search\\s+your\\s+deck\\s+for\\s+(?:up\\s+to\\s+)?(?:(a|an|\\d+)\\s+)?"
                    + "(basic\\s+)?(pok[eé]mon|energy)",
            Pattern.CASE_INSENSITIVE);

    private final String searchTarget;    // "pokemon" or "energy"
    private final boolean basicOnly;
    private final int maxResults;

    SearchDeckEffect(String searchTarget, boolean basicOnly, int maxResults) {
        this.searchTarget = searchTarget;
        this.basicOnly = basicOnly;
        this.maxResults = maxResults;
    }

    public static SearchDeckEffect fromRule(String rule) {
        if (rule == null) return null;

        Matcher matcher = SEARCH_PATTERN.matcher(rule);
        if (!matcher.find()) return null;

        String quantifier = matcher.group(1);
        String basicModifier = matcher.group(2);
        // Normalizamos a "pokemon"/"energy" (la API usa "Pokémon" con acento).
        String target = matcher.group(3).toLowerCase().startsWith("pok") ? "pokemon" : "energy";

        boolean basicOnly = basicModifier != null && basicModifier.toLowerCase().contains("basic");

        int maxResults = 1;
        if (quantifier != null && !quantifier.equalsIgnoreCase("a") && !quantifier.equalsIgnoreCase("an")) {
            try {
                maxResults = Integer.parseInt(quantifier);
            } catch (NumberFormatException ignored) {
            }
        }

        return new SearchDeckEffect(target, basicOnly, maxResults);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        return !board.getDeck().isEmpty();
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        List<GameCard> matches = GameActionUtils.searchDeck(board, searchTarget, basicOnly, maxResults);
        if (!matches.isEmpty() && ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    ar.edu.utn.frc.tup.piii.engine.events.GameEventType.CARD_DRAWN,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName() + ": found " + matches.size()
                            + " " + searchTarget + " card" + (matches.size() > 1 ? "s" : ""));
        }
    }
    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("Search your deck for ");
        if (maxResults > 1) sb.append("up to ").append(maxResults).append(" ");
        else sb.append("a ");
        if (basicOnly) sb.append("Basic ");
        sb.append(searchTarget).append(" card").append(maxResults > 1 ? "s" : "");
        return sb.toString();
    }

    public String getSearchTarget() { return searchTarget; }
    public boolean isBasicOnly() { return basicOnly; }
    public int getMaxResults() { return maxResults; }
}
