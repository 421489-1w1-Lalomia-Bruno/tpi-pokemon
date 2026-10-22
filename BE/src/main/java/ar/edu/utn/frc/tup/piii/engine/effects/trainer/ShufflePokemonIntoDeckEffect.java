package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Efecto tipo "Cassius": baraja 1 de tus Pokémon (y todas las cartas unidas a él
 * —energías y herramienta—) dentro de tu mazo.
 *
 * Ejemplo: "Shuffle 1 of your Pokémon and all cards attached to it into your deck."
 *
 * El Pokémon objetivo se determina por el targetId del contexto (la carta se
 * arrastra sobre un Pokémon). Si se baraja el Pokémon Activo y hay banca, se
 * promueve el primer Pokémon de la banca para no dejar el puesto activo vacío.
 */
public class ShufflePokemonIntoDeckEffect implements TrainerEffect {

    public static ShufflePokemonIntoDeckEffect fromRule(String rule) {
        if (rule == null) return null;
        String lower = rule.toLowerCase();

        boolean shuffles = lower.contains("shuffle");
        boolean pokemon = lower.contains("pok");
        boolean intoDeck = lower.contains("into your deck");
        boolean fromDiscard = lower.contains("from your discard");

        if (!shuffles || !pokemon || !intoDeck || fromDiscard) {
            return null;
        }
        return new ShufflePokemonIntoDeckEffect();
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        return board.getActivePokemon() != null || !board.getBench().isEmpty();
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        PokemonInPlay target = findTarget(ctx);
        if (target == null) return;

        boolean wasActive = board.getActivePokemon() == target;
        if (wasActive) {
            board.setActivePokemon(null);
            if (!board.getBench().isEmpty()) {
                PokemonInPlay promoted = board.getBench().remove(0);
                board.setActivePokemon(promoted);
            }
        } else {
            board.getBench().remove(target);
        }

        List<GameCard> toDeck = new ArrayList<>();
        toDeck.add(target.getCard());
        toDeck.addAll(target.getAttachedEnergy());
        if (target.getTool() != null) {
            toDeck.add(target.getTool());
        }

        board.getDeck().addAll(toDeck);
        Collections.shuffle(board.getDeck());

        if (ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    GameEventType.TRAINER_PLAYED,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName() + ": shuffled " + target.getCard().getName()
                            + " and its attached cards into the deck");
        }
    }

    @Override
    public String getDescription() {
        return "Shuffle 1 of your Pokemon and all cards attached to it into your deck";
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

        // Sin objetivo explícito: preferimos un Pokémon de banca para no vaciar el activo.
        if (!board.getBench().isEmpty()) {
            return board.getBench().get(0);
        }
        return board.getActivePokemon();
    }
}
