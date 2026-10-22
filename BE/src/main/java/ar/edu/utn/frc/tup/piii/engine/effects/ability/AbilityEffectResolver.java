package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import ar.edu.utn.frc.tup.piii.engine.models.Ability;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbilityEffectResolver {

    private static final Pattern FLIP_COINS = Pattern.compile(
            "(?:flip|flips)\\s+(\\d+)\\s+coins?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOR_EACH_TAILS_DISCARD = Pattern.compile(
            "for\\s+each\\s+tails.*?discard", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCARD_OPPONENT_HAND = Pattern.compile(
            "discard.*card.*(?:opponent|their|his|her).*hand", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREVENT_DAMAGE = Pattern.compile(
            "prevent.*(?:all\\s+)?damage", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAMAGE_THRESHOLD = Pattern.compile(
            "(\\d+)\\s+or\\s+less", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPPONENT_TURN_ONLY = Pattern.compile(
            "during\\s+your\\s+opponent'?s?\\s+turn", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOVE_ENERGY = Pattern.compile(
            "move\\s+a\\s+(\\w+)\\s+Energy.*?to\\s+another\\s+of\\s+your\\s+Pokémon", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOCK_ITEMS = Pattern.compile(
            "(?:rival|opponent).*?(?:can't|cannot|no puede|no pueda).*?(?:play|jugar).*?(?:Item|Objeto)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BLOCK_ALL_TRAINERS = Pattern.compile(
            "(?:rival|opponent).*?(?:can't|cannot|no puede|no pueda).*?(?:play|jugar).*?(?:Trainer|Entrenador|Supporter|Estadio|Stadium)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern CONFUSED_EVOLVE = Pattern.compile(
            "(?:confus|confundid).{0,80}(?:search|busca).{0,80}(?:evol)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public List<AbilityEffect> resolve(Ability ability) {
        List<AbilityEffect> effects = new ArrayList<>();
        if (ability == null || ability.getEffectText() == null) return effects;

        String text = ability.getEffectText();

        if (FLIP_COINS.matcher(text).find()) {
            Matcher m = FLIP_COINS.matcher(text);
            m.find();
            int coins = Integer.parseInt(m.group(1));
            effects.add(new FlipCoinsEffect(coins));

            if (FOR_EACH_TAILS_DISCARD.matcher(text).find()
                    && DISCARD_OPPONENT_HAND.matcher(text).find()) {
                effects.add(new DiscardCardsEffect(true, 1));
            }
        }

        if (PREVENT_DAMAGE.matcher(text).find()) {
            int threshold = 0;
            Matcher tm = DAMAGE_THRESHOLD.matcher(text);
            if (tm.find()) {
                threshold = Integer.parseInt(tm.group(1));
            }
            boolean onlyOpponentTurn = OPPONENT_TURN_ONLY.matcher(text).find();
            effects.add(new PreventDamageEffect(threshold, onlyOpponentTurn));
        }

        Matcher moveMatcher = MOVE_ENERGY.matcher(text);
        if (moveMatcher.find()) {
            String energyName = moveMatcher.group(1);
            EnergyType energyType = parseEnergyType(energyName);
            if (energyType != null) {
                effects.add(new MoveEnergyEffect(energyType));
            }
        }

        if (CONFUSED_EVOLVE.matcher(text).find()) {
            effects.add(new InkayEvolveEffect());
        }

        if (BLOCK_ALL_TRAINERS.matcher(text).find()) {
            effects.add(new BlockTrainerEffect(Set.of("ITEM", "SUPPORTER", "STADIUM")));
        } else if (BLOCK_ITEMS.matcher(text).find()) {
            effects.add(new BlockTrainerEffect(Set.of("ITEM")));
        }

        return effects;
    }

    private EnergyType parseEnergyType(String name) {
        try {
            return EnergyType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
