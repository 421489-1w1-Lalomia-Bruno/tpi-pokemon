package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;

import java.util.Collections;

/**
 * Efecto tipo "Evosoda": busca en tu mazo una carta que evolucione a 1 de tus
 * Pokémon y la coloca sobre ese Pokémon (cuenta como evolucionar).
 *
 * Ejemplo: "Search your deck for a card that evolves from 1 of your Pokémon and
 *           put it onto that Pokémon. Shuffle your deck afterward."
 *
 * El Pokémon objetivo se determina por el targetId del contexto (la carta se
 * arrastra sobre un Pokémon). Replica la lógica de evolución del engine:
 * descarta la carta previa, conserva las energías y arrastra el daño recibido.
 */
public class EvolveFromDeckEffect implements TrainerEffect {

    public static EvolveFromDeckEffect fromRule(String rule) {
        if (rule == null) return null;
        String lower = rule.toLowerCase();

        boolean searchesDeck = lower.contains("search your deck");
        boolean evolves = lower.contains("evolves from") || lower.contains("that evolves");
        if (!searchesDeck || !evolves) {
            return null;
        }
        return new EvolveFromDeckEffect();
    }

    @Override
    public boolean canApply(TrainerEffectContext ctx) {
        if (ctx == null || ctx.getCurrentPlayerBoard() == null) return false;
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        if (board.getDeck().isEmpty()) return false;
        PokemonInPlay target = findTarget(ctx);
        return target != null && findEvolution(board, target) != null;
    }

    @Override
    public void apply(TrainerEffectContext ctx) {
        PlayerBoard board = ctx.getCurrentPlayerBoard();
        PokemonInPlay target = findTarget(ctx);
        if (target == null) return;

        PokemonCard evolution = findEvolution(board, target);
        if (evolution == null) return;

        board.getDeck().remove(evolution);
        board.getDiscardPile().add(target.getCard());

        int oldMaxHp = target.getCard().getHp();
        int damageTaken = Math.max(0, oldMaxHp - target.getCurrentHp());
        target.setCard(evolution);
        target.setCurrentHp(Math.max(0, evolution.getHp() - damageTaken));
        target.getSpecialConditions().clear();

        Collections.shuffle(board.getDeck());

        if (ctx.getEventPublisher() != null) {
            ctx.getEventPublisher().publish(
                    GameEventType.POKEMON_EVOLVED,
                    ctx.getMatchId(),
                    ctx.getCurrentPlayerId(),
                    ctx.getSourceCard().getName() + ": evolved into " + evolution.getName());
        }
    }

    @Override
    public String getDescription() {
        return "Search your deck for a card that evolves from 1 of your Pokemon and put it onto that Pokemon";
    }

    private PokemonCard findEvolution(PlayerBoard board, PokemonInPlay target) {
        String baseName = target.getCard().getName();
        for (GameCard card : board.getDeck()) {
            if (card instanceof PokemonCard pokemon
                    && pokemon.getEvolvesFrom() != null
                    && pokemon.getEvolvesFrom().equalsIgnoreCase(baseName)) {
                return pokemon;
            }
        }
        return null;
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
}
