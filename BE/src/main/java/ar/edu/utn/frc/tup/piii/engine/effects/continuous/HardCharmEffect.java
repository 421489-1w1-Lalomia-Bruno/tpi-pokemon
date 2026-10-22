package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardCharmEffect implements ToolEffect {

    private static final Pattern HARD_CHARM_PATTERN = Pattern.compile(
            "damage.*?(?:is|are)?\\s*reduced\\s+by\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private final int reduction;

    HardCharmEffect(int reduction) {
        this.reduction = reduction;
    }

    public static HardCharmEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = HARD_CHARM_PATTERN.matcher(rule);
        if (!m.find()) return null;
        if (!rule.toLowerCase().contains("after applying weakness")) {
            return null;
        }
        int reduction = Integer.parseInt(m.group(1));
        return new HardCharmEffect(reduction);
    }

    @Override
    public int modifyAttackDamage(int damage) {
        return damage;
    }

    @Override
    public int modifyDefenseDamage(int damage) {
        return Math.max(damage - reduction, 0);
    }

    @Override
    public int modifyRetreatCost(int baseCost) {
        return baseCost;
    }
}
