package ar.edu.utn.frc.tup.piii.engine.effects;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.SpecialCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatusEffectManager {

    private final Map<SpecialCondition, StatusEffectStrategy> strategies;

    public StatusEffectManager() {
        strategies = Map.of(
            SpecialCondition.POISONED, new PoisonedEffect(),
            SpecialCondition.BURNED, new BurnedEffect(),
            SpecialCondition.ASLEEP, new AsleepEffect(),
            SpecialCondition.PARALYZED, new ParalyzedEffect(),
            SpecialCondition.CONFUSED, new ConfusedEffect()
        );
    }

    public void addEffect(PokemonInPlay pokemon, SpecialCondition condition) {
        if (pokemon == null || condition == null) {
            return;
        }

        Set<SpecialCondition> conditions = pokemon.getSpecialConditions();
        if (conditions.contains(condition)) {
            return;
        }

        StatusEffectStrategy strategy = strategies.get(condition);
        if (strategy == null) {
            return;
        }

        List<SpecialCondition> toRemove = new ArrayList<>();
        for (SpecialCondition existing : conditions) {
            if (!strategy.canCoexist(existing)) {
                toRemove.add(existing);
            }
        }
        conditions.removeAll(toRemove);
        conditions.add(condition);
    }

    public void removeEffect(PokemonInPlay pokemon, SpecialCondition condition) {
        if (pokemon == null || condition == null) {
            return;
        }
        pokemon.getSpecialConditions().remove(condition);
    }

    public void processBetweenTurns(PokemonInPlay pokemon, GameBoard board, String matchId,
                                    String playerId, GameEventPublisher publisher) {
        processBetweenTurns(pokemon, board, matchId, playerId, publisher,
            SpecialCondition.POISONED, SpecialCondition.BURNED,
            SpecialCondition.ASLEEP, SpecialCondition.PARALYZED);
    }

    public void processBetweenTurns(PokemonInPlay pokemon, GameBoard board, String matchId,
                                    String playerId, GameEventPublisher publisher,
                                    SpecialCondition... onlyThese) {
        if (pokemon == null) return;
        Set<SpecialCondition> conditions = pokemon.getSpecialConditions();
        if (conditions == null || conditions.isEmpty()) return;

        List<SpecialCondition> toProcess = onlyThese.length > 0
            ? List.of(onlyThese)
            : List.of(SpecialCondition.POISONED, SpecialCondition.BURNED,
                      SpecialCondition.ASLEEP, SpecialCondition.PARALYZED);

        for (SpecialCondition condition : toProcess) {
            if (conditions.contains(condition)) {
                StatusEffectStrategy strategy = strategies.get(condition);
                if (strategy != null) {
                    int damage = strategy.applyBetweenTurns(pokemon, board);
                    if (damage > 0) {
                        String effectName = condition.name();
                        publisher.publish(GameEventType.SPECIAL_CONDITION_APPLIED, matchId, playerId,
                                effectName + " damage: " + pokemon.getCard().getName() + " took " + damage + " damage",
                                effectName, pokemon.getCard().getName(), damage);
                    }
                }
            }
        }
    }

    public void onAttack(PokemonInPlay pokemon, AttackContext context) {
        if (pokemon == null || context == null) {
            return;
        }

        if (pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP)) {
            StatusEffectStrategy strategy = strategies.get(SpecialCondition.ASLEEP);
            if (strategy != null) {
                strategy.onAttack(pokemon, context);
            }
            return;
        }

        if (pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED)) {
            StatusEffectStrategy strategy = strategies.get(SpecialCondition.PARALYZED);
            if (strategy != null) {
                strategy.onAttack(pokemon, context);
            }
            return;
        }

        if (pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED)) {
            StatusEffectStrategy strategy = strategies.get(SpecialCondition.CONFUSED);
            if (strategy != null) {
                strategy.onAttack(pokemon, context);
            }
        }
    }

    public void clearAll(PokemonInPlay pokemon) {
        if (pokemon == null) {
            return;
        }
        pokemon.getSpecialConditions().clear();
    }
}
