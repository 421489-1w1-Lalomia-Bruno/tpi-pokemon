package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiscardCardsEffect implements AbilityEffect {

    private final boolean triggerOnTails;
    private final int cardsPerTrigger;

    public DiscardCardsEffect(boolean triggerOnTails, int cardsPerTrigger) {
        this.triggerOnTails = triggerOnTails;
        this.cardsPerTrigger = cardsPerTrigger;
    }

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        int triggerCount = triggerOnTails ? ctx.getFlipTails() : ctx.getFlipHeads();
        if (triggerCount <= 0) return false;
        PlayerBoard opp = ctx.getOpponentBoard();
        return opp != null && !opp.getHand().isEmpty();
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
        int triggerCount = triggerOnTails ? ctx.getFlipTails() : ctx.getFlipHeads();
        PlayerBoard opp = ctx.getOpponentBoard();
        if (opp == null || opp.getHand().isEmpty()) return;

        int toDiscard = Math.min(triggerCount * cardsPerTrigger, opp.getHand().size());
        Random rng = new Random();
        List<String> discardedNames = new ArrayList<>();

        for (int i = 0; i < toDiscard; i++) {
            int idx = rng.nextInt(opp.getHand().size());
            GameCard removed = opp.getHand().remove(idx);
            opp.getDiscardPile().add(removed);
            discardedNames.add(removed.getName());
        }

        if (ctx.getPublisher() != null) {
            ctx.getPublisher().publish(
                GameEventType.REVEAL_CARDS, ctx.getMatchId(), ctx.getPlayerId(),
                ctx.getSourcePokemon().getCard().getName() + " discarded " + toDiscard
                    + " card(s) from opponent's hand: " + String.join(", ", discardedNames)
            );
        }
    }
}
