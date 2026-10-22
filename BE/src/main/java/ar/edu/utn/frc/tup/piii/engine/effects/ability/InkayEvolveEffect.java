package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

import java.util.Collections;

/**
 * Habilidad de Inkay "Upside-Down Evolution": si está Confundido, busca su evolución
 * en el mazo y evoluciona. Al evolucionar se limpian todas las condiciones especiales.
 */
public class InkayEvolveEffect implements AbilityEffect {

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        if (ctx == null || ctx.getSourcePokemon() == null) return false;
        return ctx.getSourcePokemon().getSpecialConditions().contains(SpecialCondition.CONFUSED);
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
        PokemonInPlay pokemon = ctx.getSourcePokemon();
        PlayerBoard board = ctx.getSourceBoard();

        PokemonCard evolution = findEvolution(board, pokemon);
        if (evolution == null) {
            if (ctx.getPublisher() != null) {
                ctx.getPublisher().publish(
                        GameEventType.NOTIFICATION, ctx.getMatchId(), ctx.getPlayerId(),
                        "No evolution found in deck for " + pokemon.getCard().getName());
            }
            return;
        }

        board.getDeck().remove(evolution);
        board.getDiscardPile().add(pokemon.getCard());

        int damageTaken = Math.max(0, pokemon.getCard().getHp() - pokemon.getCurrentHp());
        pokemon.setCard(evolution);
        pokemon.setCurrentHp(Math.max(0, evolution.getHp() - damageTaken));
        pokemon.getSpecialConditions().clear();

        Collections.shuffle(board.getDeck());

        if (ctx.getPublisher() != null) {
            ctx.getPublisher().publish(
                    GameEventType.POKEMON_EVOLVED, ctx.getMatchId(), ctx.getPlayerId(),
                    "Upside-Down Evolution: evolved into " + evolution.getName() + "!");
        }
    }

    private PokemonCard findEvolution(PlayerBoard board, PokemonInPlay target) {
        if (board == null) return null;
        String baseName = target.getCard().getName();
        for (GameCard card : board.getDeck()) {
            if (card instanceof PokemonCard pk
                    && pk.getEvolvesFrom() != null
                    && pk.getEvolvesFrom().equalsIgnoreCase(baseName)) {
                return pk;
            }
        }
        return null;
    }
}
