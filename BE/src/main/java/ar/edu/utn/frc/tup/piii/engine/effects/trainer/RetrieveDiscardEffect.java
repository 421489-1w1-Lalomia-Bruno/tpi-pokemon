package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RetrieveDiscardEffect implements TrainerEffect {

    private static final Pattern RETRIEVE_PATTERN = Pattern.compile(
            "(?:put|take|shuffle)\\s+(?:up\\s+to\\s+)?(?:(a|an|\\d+)\\s+)?"
                    + "(basic\\s+)?(pok[eé]mon|energy)(?:\\s+cards?)?\\s+"
                    + "from your discard pile",
            Pattern.CASE_INSENSITIVE);

    private final String searchTarget;
    private final boolean basicOnly;
    private final int maxResults;
    private final String destination;
    /** True para "on top of your deck": se coloca encima del mazo sin barajar. */
    private final boolean deckTop;

    RetrieveDiscardEffect(String searchTarget, boolean basicOnly, int maxResults,
                          String destination, boolean deckTop) {
        this.searchTarget = searchTarget;
        this.basicOnly = basicOnly;
        this.maxResults = maxResults;
        this.destination = destination;
        this.deckTop = deckTop;
    }

    public static RetrieveDiscardEffect fromRule(String rule) {
        if (rule == null) return null;

        Matcher matcher = RETRIEVE_PATTERN.matcher(rule);
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

        // El destino se infiere del texto completo: mazo (barajado), encima del mazo, o mano.
        String lower = rule.toLowerCase();
        String destination = "hand";
        boolean deckTop = false;
        if (lower.contains("on top of your deck")) {
            destination = "deck";
            deckTop = true;
        } else if (lower.contains("into your deck") || lower.contains("in your deck")) {
            destination = "deck";
        }

        return new RetrieveDiscardEffect(target, basicOnly, maxResults, destination, deckTop);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        List<GameCard> discard = ctx.getCurrentPlayerBoard().getDiscardPile();
        if (discard.isEmpty()) return false;
        if ("pokemon".equals(searchTarget)) {
            return discard.stream().anyMatch(c -> c instanceof PokemonCard);
        }
        if ("energy".equals(searchTarget)) {
            return discard.stream().anyMatch(c -> c instanceof EnergyCard);
        }
        if ("trainer".equals(searchTarget)) {
            return discard.stream().anyMatch(c -> c instanceof TrainerCard);
        }
        return true;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        List<GameCard> matches = GameActionUtils.retrieveFromDiscard(
                board, searchTarget, basicOnly, maxResults, destination, deckTop);
        if (!matches.isEmpty() && ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    ar.edu.utn.frc.tup.piii.engine.events.GameEventType.CARD_DRAWN,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName() + ": retrieved " + matches.size()
                            + " " + searchTarget + " card" + (matches.size() > 1 ? "s" : "")
                            + " from discard to " + destination);
        }
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        if ("deck".equals(destination)) sb.append("Shuffle ");
        else sb.append("Put ");
        if (maxResults > 1) sb.append("up to ").append(maxResults).append(" ");
        else sb.append("a ");
        if (basicOnly) sb.append("Basic ");
        sb.append(searchTarget).append(" card").append(maxResults > 1 ? "s" : "");
        sb.append(" from your discard pile into your ").append(destination);
        return sb.toString();
    }

    public String getSearchTarget() { return searchTarget; }
    public boolean isBasicOnly() { return basicOnly; }
    public int getMaxResults() { return maxResults; }
    public String getDestination() { return destination; }
    public boolean isDeckTop() { return deckTop; }
}
