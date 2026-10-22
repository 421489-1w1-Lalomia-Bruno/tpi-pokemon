package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.effects.GameActionUtils;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwitchActiveEffect implements TrainerEffect {

    private static final Pattern SWITCH_PATTERN = Pattern.compile(
            "switch\\s+your\\s+active\\s+pok[eé]mon\\s+with",
            Pattern.CASE_INSENSITIVE);

    SwitchActiveEffect() {
    }

    public static SwitchActiveEffect fromRule(String rule) {
        if (rule == null) return null;
        Matcher m = SWITCH_PATTERN.matcher(rule);
        if (!m.find()) return null;
        return new SwitchActiveEffect();
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        return board.getActivePokemon() != null && !board.getBench().isEmpty();
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        GameActionUtils.switchActive(board, ctx.getTargetId(),
                ctx.getEventPublisher(), ctx.getMatchId(), ctx.getCurrentPlayerId(),
                ctx.getSourceCard().getName());
    }

    @Override
    public String getDescription() {
        return "Switch your Active Pokemon with 1 of your Benched Pokemon";
    }
}
