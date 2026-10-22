package ar.edu.utn.frc.tup.piii.engine.combat;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttackFlipResolver {

    private static final Pattern FLIP_PATTERN = Pattern.compile(
            "flip\\s+(\\d+)\\s+coins?.*?(\\d+)\\s+damage.*?heads", Pattern.CASE_INSENSITIVE);

    public static boolean isFlipAttack(String effectText) {
        if (effectText == null) return false;
        return FLIP_PATTERN.matcher(effectText).find();
    }

    public static FlipResult resolve(String effectText) {
        if (effectText == null) return new FlipResult(0, 0, 0);

        Matcher matcher = FLIP_PATTERN.matcher(effectText);
        if (!matcher.find()) return new FlipResult(0, 0, 0);

        int totalCoins = Integer.parseInt(matcher.group(1));
        int damagePerHeads = Integer.parseInt(matcher.group(2));

        int heads = CoinFlipManager.flipHeads(totalCoins);

        int damage = heads * damagePerHeads;
        return new FlipResult(heads, totalCoins, damage);
    }

    public static FlipResult resolveWithEvents(String effectText, GameEventPublisher publisher,
                                                String matchId, String playerId, String sourceName) {
        if (effectText == null) return new FlipResult(0, 0, 0);
        Matcher matcher2 = FLIP_PATTERN.matcher(effectText);
        if (!matcher2.find()) return new FlipResult(0, 0, 0);

        int totalCoins = Integer.parseInt(matcher2.group(1));
        int damagePerHeads = Integer.parseInt(matcher2.group(2));

        int heads = 0;
        for (int i = 0; i < totalCoins; i++) {
            boolean isHead = CoinFlipManager.flip();
            if (isHead) heads++;
            if (publisher != null) {
                String desc = sourceName + ": Coin " + (i + 1) + "/" + totalCoins
                        + " \u2192 " + (isHead ? "Heads" : "Tails");
                publisher.publish(GameEventType.MULTI_COIN_FLIP, matchId, playerId,
                        desc, null, null, isHead ? 1 : 0);
            }
        }

        int damage = heads * damagePerHeads;
        return new FlipResult(heads, totalCoins, damage);
    }

    private static final Pattern CONDITIONAL_FLIP_PATTERN = Pattern.compile(
            "flip\\s+a\\s+coin.*?if\\s+heads", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static boolean isConditionalFlipAttack(String effectText) {
        if (effectText == null) return false;
        return CONDITIONAL_FLIP_PATTERN.matcher(effectText).find();
    }

    public static String extractBranchText(String effectText, boolean isHeads) {
        if (effectText == null) return null;
        String lower = effectText.toLowerCase();
        int headsIdx = lower.indexOf("if heads");
        int tailsIdx = lower.indexOf("if tails");

        if (headsIdx < 0) return effectText;

        if (isHeads) {
            int start = headsIdx + "if heads".length();
            if (tailsIdx > headsIdx) {
                return effectText.substring(start, tailsIdx);
            }
            return effectText.substring(start);
        } else {
            if (tailsIdx > 0) {
                int start = tailsIdx + "if tails".length();
                return effectText.substring(start);
            }
            return "";
        }
    }

    private static final Pattern BONUS_DAMAGE = Pattern.compile(
            "attack does (\\d+) more damage", Pattern.CASE_INSENSITIVE);

    public static int extractBonusDamage(String text) {
        if (text == null) return 0;
        Matcher m = BONUS_DAMAGE.matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    public static List<SpecialCondition> parseSpecialConditions(String effectText) {
        List<SpecialCondition> effects = new ArrayList<>();
        if (effectText == null) return effects;
        String upper = effectText.toUpperCase();
        if (upper.contains("POISON")) effects.add(SpecialCondition.POISONED);
        if (upper.contains("BURN")) effects.add(SpecialCondition.BURNED);
        if (upper.contains("ASLEEP") || upper.contains("SLEEP")) effects.add(SpecialCondition.ASLEEP);
        if (upper.contains("PARALYZ") || upper.contains("PARALYSE")) effects.add(SpecialCondition.PARALYZED);
        if (upper.contains("CONFUS")) effects.add(SpecialCondition.CONFUSED);
        return effects;
    }

    public static class FlipResult {
        private final int heads;
        private final int totalCoins;
        private final int damage;

        public FlipResult(int heads, int totalCoins, int damage) {
            this.heads = heads;
            this.totalCoins = totalCoins;
            this.damage = damage;
        }

        public int getHeads() { return heads; }
        public int getTotalCoins() { return totalCoins; }
        public int getDamage() { return damage; }
    }
}
