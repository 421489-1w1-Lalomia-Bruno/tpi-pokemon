package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HealDamageEffect implements TrainerEffect {

    private static final Pattern HEAL_PATTERN = Pattern.compile(
            "heal\\s+(\\d+)\\s+damage", Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_ENERGY_FROM_HEALED = Pattern.compile(
            "discard\\s+(?:an?\\s+)?energy\\s+(?:card\\s+)?",
            Pattern.CASE_INSENSITIVE);

    private final int healAmount;
    private final boolean discardEnergyAfter;

    HealDamageEffect(int healAmount, boolean discardEnergyAfter) {
        this.healAmount = healAmount;
        this.discardEnergyAfter = discardEnergyAfter;
    }

    public static HealDamageEffect fromRule(String rule) {
        if (rule == null) return null;

        Matcher healMatcher = HEAL_PATTERN.matcher(rule);
        if (!healMatcher.find()) return null;

        int healAmount = Integer.parseInt(healMatcher.group(1));
        boolean discardEnergyAfter = DISCARD_ENERGY_FROM_HEALED.matcher(rule).find();

        return new HealDamageEffect(healAmount, discardEnergyAfter);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        PokemonInPlay target = findTarget(ctx);
        return target != null && target.getCurrentHp() < target.getCard().getHp();
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PokemonInPlay target = findTarget(ctx);
        if (target == null) return;
        GameActionUtils.healDamage(target, healAmount, discardEnergyAfter,
                ctx.getCurrentPlayerBoard(), ctx.getEventPublisher(),
                ctx.getMatchId(), ctx.getCurrentPlayerId(), ctx.getSourceCard().getName());
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder("Heal ").append(healAmount).append(" damage");
        if (discardEnergyAfter) sb.append(" and discard 1 Energy");
        return sb.toString();
    }

    private PokemonInPlay findTarget(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        String targetId = ctx.getTargetId();

        if (targetId != null) {
            if (board.getActivePokemon() != null
                    && board.getActivePokemon().getCard().getId().equals(targetId)) {
                return board.getActivePokemon();
            }
            for (PokemonInPlay bench : board.getBench()) {
                if (bench.getCard().getId().equals(targetId)) {
                    return bench;
                }
            }
        }

        return board.getActivePokemon();
    }

    public int getHealAmount() { return healAmount; }
    public boolean isDiscardEnergyAfter() { return discardEnergyAfter; }
}
