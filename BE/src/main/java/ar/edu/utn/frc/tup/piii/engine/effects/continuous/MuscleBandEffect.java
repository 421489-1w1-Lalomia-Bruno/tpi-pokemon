package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuscleBandEffect implements ToolEffect {

    private static final Pattern MUSCLE_BAND_PATTERN = Pattern.compile(
            "(?:attacks|attack).*?(?:do|does)\\s+(\\d+)\\s+more\\s+damage",
            Pattern.CASE_INSENSITIVE);

    private final int bonus;

    MuscleBandEffect(int bonus) {
        this.bonus = bonus;
    }

    public static MuscleBandEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = MUSCLE_BAND_PATTERN.matcher(rule);
        if (!m.find()) return null;
        if (!rule.toLowerCase().contains("before applying weakness")) {
            return null;
        }
        int bonus = Integer.parseInt(m.group(1));
        return new MuscleBandEffect(bonus);
    }

    @Override
    public int modifyAttackDamage(int damage) {
        return damage + bonus;
    }

    @Override
    public int modifyDefenseDamage(int damage) {
        return damage;
    }

    @Override
    public int modifyRetreatCost(int baseCost) {
        return baseCost;
    }
}
