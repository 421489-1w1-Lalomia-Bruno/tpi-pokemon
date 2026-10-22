package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrainerEffectResolver {

    public List<TrainerEffect> resolve(TrainerCard card) {
        List<String> rules = parseRules(card.getEffect());
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<TrainerEffect> effects = new ArrayList<>();
        for (String rule : rules) {
            TrainerEffect effect = matchEffect(rule);
            if (effect != null && !hasDuplicateEffect(effects, effect)) {
                effects.add(effect);
            }
        }
        return effects;
    }

    private boolean hasDuplicateEffect(List<TrainerEffect> effects, TrainerEffect candidate) {
        for (TrainerEffect existing : effects) {
            if (existing.getClass().equals(candidate.getClass())) {
                return true;
            }
        }
        return false;
    }

    private TrainerEffect matchEffect(String rule) {
        if (rule == null) return null;
        String lower = rule.toLowerCase();

        // Great Ball: "look at the top N cards". Debe ir primero porque su texto
        // ("shuffle ... into your deck", "Pokémon") también matchea a Cassius.
        if (lower.contains("look at the top")) {
            GreatBallEffect greatBallEffect = GreatBallEffect.fromRule(rule);
            if (greatBallEffect != null) return greatBallEffect;
        }

        // Red Card: el rival baraja su mano y roba. Debe ir antes del draw genérico.
        if (lower.contains("opponent") && lower.contains("shuffle") && lower.contains("hand")) {
            RedCardEffect redCard = RedCardEffect.fromRule(rule);
            if (redCard != null) return redCard;
        }

        // Cassius: baraja un Pokémon propio (y lo unido) dentro de tu mazo.
        if (lower.contains("shuffle") && lower.contains("pok")
                && lower.contains("into your deck") && !lower.contains("from your discard")) {
            ShufflePokemonIntoDeckEffect shuffleEffect = ShufflePokemonIntoDeckEffect.fromRule(rule);
            if (shuffleEffect != null) return shuffleEffect;
        }

        // Evosoda: busca una evolución en el mazo. Debe ir antes del search genérico.
        if (lower.contains("search your deck")
                && (lower.contains("evolves from") || lower.contains("that evolves"))) {
            EvolveFromDeckEffect evolveEffect = EvolveFromDeckEffect.fromRule(rule);
            if (evolveEffect != null) return evolveEffect;
        }

        // Roller Skates: "flip a coin. If heads, draw N". Antes del draw genérico.
        if ((lower.contains("flip") && lower.contains("coin")) || lower.contains("coin flip")) {
            FlipCoinEffect coinEffect = FlipCoinEffect.fromRule(rule);
            if (coinEffect != null) return coinEffect;
        }

        if (lower.contains("draw") && lower.contains("card")) {
            return DrawCardsEffect.fromRule(rule);
        }

        if (lower.contains("switch") && lower.contains("active")
                && lower.contains("pok")) {
            return SwitchActiveEffect.fromRule(rule);
        }

        if ((lower.contains("discard") || lower.contains("discard"))
                && lower.contains("energy")
                && (lower.contains("opponent") || lower.contains("defending"))) {
            return DiscardEnergyEffect.fromRule(rule);
        }

        if (lower.contains("heal") && lower.contains("damage")) {
            return HealDamageEffect.fromRule(rule);
        }

        if (lower.contains("from your discard pile")) {
            return RetrieveDiscardEffect.fromRule(rule);
        }

        if (lower.contains("search your deck")) {
            return SearchDeckEffect.fromRule(rule);
        }

        return null;
    }

    public static List<String> parseRules(String effectJson) {
        List<String> rules = new ArrayList<>();
        if (effectJson == null || effectJson.isBlank()) {
            return rules;
        }
        String trimmed = effectJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return rules;
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return rules;
        }
        Matcher m = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inner);
        while (m.find()) {
            String value = m.group(1);
            value = value.replace("\\\"", "\"")
                         .replace("\\\\", "\\")
                         .replace("\\n", "\n")
                         .replace("\\t", "\t");
            rules.add(value);
        }
        return rules;
    }
}
