package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

import java.util.Iterator;
import java.util.List;

public class MoveEnergyEffect implements AbilityEffect {

    private final EnergyType energyType;

    public MoveEnergyEffect(EnergyType energyType) {
        this.energyType = energyType;
    }

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        PlayerBoard sourceBoard = ctx.getSourceBoard();
        if (sourceBoard == null) return false;

        String sourceTargetId = ctx.getSourceTargetId();
        if (sourceTargetId == null) return false;

        PokemonInPlay sourcePokemon = findPokemonOnBoard(sourceBoard, sourceTargetId);
        if (sourcePokemon == null) return false;

        String targetId = ctx.getTargetId();
        if (targetId == null) return false;

        PokemonInPlay targetPokemon = findPokemonOnBoard(sourceBoard, targetId);
        if (targetPokemon == null) return false;

        return hasMatchingEnergy(sourcePokemon);
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
        PlayerBoard sourceBoard = ctx.getSourceBoard();
        if (sourceBoard == null) return;

        String sourceTargetId = ctx.getSourceTargetId();
        String targetId = ctx.getTargetId();
        if (sourceTargetId == null || targetId == null) return;

        PokemonInPlay sourcePokemon = findPokemonOnBoard(sourceBoard, sourceTargetId);
        PokemonInPlay targetPokemon = findPokemonOnBoard(sourceBoard, targetId);
        if (sourcePokemon == null || targetPokemon == null) return;

        EnergyCard moved = removeMatchingEnergy(sourcePokemon);
        if (moved == null) return;

        targetPokemon.getAttachedEnergy().add(moved);

        if (ctx.getPublisher() != null) {
            ctx.getPublisher().publish(
                GameEventType.ABILITY_USED, ctx.getMatchId(), ctx.getPlayerId(),
                ctx.getSourcePokemon().getCard().getName() + " moved " + energyType + " Energy from "
                    + sourcePokemon.getCard().getName() + " to " + targetPokemon.getCard().getName()
            );
        }
    }

    private boolean hasMatchingEnergy(PokemonInPlay pokemon) {
        return pokemon.getAttachedEnergy().stream()
                .anyMatch(e -> e.getEnergyType() == energyType
                    || (e.getProvidedEnergies() != null && e.getProvidedEnergies().contains(energyType)));
    }

    private EnergyCard removeMatchingEnergy(PokemonInPlay pokemon) {
        List<EnergyCard> energies = pokemon.getAttachedEnergy();
        for (Iterator<EnergyCard> it = energies.iterator(); it.hasNext(); ) {
            EnergyCard e = it.next();
            if (e.getEnergyType() == energyType
                    || (e.getProvidedEnergies() != null && e.getProvidedEnergies().contains(energyType))) {
                it.remove();
                return e;
            }
        }
        return null;
    }

    private PokemonInPlay findPokemonOnBoard(PlayerBoard board, String cardId) {
        if (board.getActivePokemon() != null
                && board.getActivePokemon().getCard().getId().equals(cardId)) {
            return board.getActivePokemon();
        }
        for (PokemonInPlay bench : board.getBench()) {
            if (bench.getCard().getId().equals(cardId)) {
                return bench;
            }
        }
        return null;
    }
}
