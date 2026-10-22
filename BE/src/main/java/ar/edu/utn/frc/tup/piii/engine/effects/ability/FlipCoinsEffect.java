package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager.CoinFlipResult;

public class FlipCoinsEffect implements AbilityEffect {

    private final int coinCount;

    public FlipCoinsEffect(int coinCount) {
        this.coinCount = coinCount;
    }

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        return coinCount > 0;
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
        CoinFlipResult result = CoinFlipManager.flip(coinCount);
        ctx.setFlipHeads(result.heads());
        ctx.setFlipTails(result.tails());

        CoinFlipManager.publishFlipResult(ctx.getPublisher(), ctx.getMatchId(),
                ctx.getPlayerId(), ctx.getSourcePokemon().getCard().getName(), result);
    }
}
