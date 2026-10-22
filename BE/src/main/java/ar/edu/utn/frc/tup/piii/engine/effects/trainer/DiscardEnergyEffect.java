package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscardEnergyEffect implements TrainerEffect {

    private static final Pattern DISCARD_PATTERN = Pattern.compile(
            "discard\\s+(?:an?\\s+)?(special\\s+)?energy\\s+(?:card\\s+)?",
            Pattern.CASE_INSENSITIVE);

    private final boolean specialOnly;

    DiscardEnergyEffect(boolean specialOnly) {
        this.specialOnly = specialOnly;
    }

    public static DiscardEnergyEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = DISCARD_PATTERN.matcher(rule);
        if (!m.find()) return null;
        if (!rule.toLowerCase().contains("opponent")
                && !rule.toLowerCase().contains("defending")) {
            return null;
        }
        boolean specialOnly = m.group(1) != null
                && m.group(1).toLowerCase().contains("special");
        return new DiscardEnergyEffect(specialOnly);
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getOpponentBoard() == null) return false;
        PokemonInPlay target = findTarget(ctx);
        if (target == null || target.getAttachedEnergy().isEmpty()) return false;
        if (specialOnly) {
            return target.getAttachedEnergy().stream().anyMatch(EnergyCard::isSpecial);
        }
        return true;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard opponentBoard = ctx.getOpponentBoard();
        PokemonInPlay target = findTarget(ctx);
        if (target == null) return;
        GameActionUtils.discardEnergy(target, specialOnly, opponentBoard,
                ctx.getEventPublisher(), ctx.getMatchId(), ctx.getCurrentPlayerId(),
                ctx.getSourceCard().getName());
    }

    @Override
    public String getDescription() {
        return "Discard a" + (specialOnly ? " Special" : "n")
                + " Energy from your opponent's Pokemon";
    }

    private PokemonInPlay findTarget(TrainerEffectContext ctx) {
        PlayerBoard opponentBoard = ctx.getOpponentBoard();
        String targetId = ctx.getTargetId();

        if (targetId != null) {
            if (opponentBoard.getActivePokemon() != null
                    && opponentBoard.getActivePokemon().getCard().getId().equals(targetId)) {
                return opponentBoard.getActivePokemon();
            }
            for (PokemonInPlay bench : opponentBoard.getBench()) {
                if (bench.getCard().getId().equals(targetId)) {
                    return bench;
                }
            }
        }

        return opponentBoard.getActivePokemon();
    }

    public boolean isSpecialOnly() { return specialOnly; }
}
