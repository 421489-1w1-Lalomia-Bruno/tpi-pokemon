package ar.edu.utn.frc.tup.piii.engine.effects.attack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AttackEffectResolver {

    private static final Pattern DRAW_PATTERN = Pattern.compile(
            "(?:Roba|Draw|drew)\\s+(\\d+)\\s+(?:cartas?|cards?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HEAL_PATTERN = Pattern.compile(
            "(?:Cura|Heal|cura)\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SWITCH_SELF_PATTERN = Pattern.compile(
            "(?:Cambia|Switch|change|changes?|switch es).*?(?:este|this|your).*?(?:Pok[eé]mon|Pokemon).*?(?:por|with|to).*?(?:uno|1).*?(?:de|of|from).*?(?:tu|your).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SWITCH_OPPONENT_PATTERN = Pattern.compile(
            "(?:rival|opponent|defending).*?(?:cambia|switch(?:es)?).*?(?:su|their|its).*?(?:Activo|Active).*?(?:por|with|to).*?(?:uno|one).*?(?:de|of|from).*?(?:su|their).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SWITCH_OPPONENT_SPANISH_PATTERN = Pattern.compile(
            "(?:cambia|switch(?:es)?).*?(?:Activo|Active).*?(?:rival|opponent|defending).*?(?:por|with|to).*?(?:uno|one).*?(?:de|of|from).*?(?:su|their).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SWITCH_OPPONENT_SHORT_PATTERN = Pattern.compile(
            "(?:rival|opponent|defending).*?(?:cambia|switch(?:es)?).*?(?:su|their|its).*?(?:Activo|Active)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_ENERGY_PATTERN = Pattern.compile(
            "(?:descarta|discard|discards?)\\s+(?:(?:una?|1|an?|\\d+)\\s+)?(?:Energ[íi]a|Energy)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_ENERGY_SELF_PATTERN = Pattern.compile(
            "(?:descarta|discard|discards?).*?(?:de|from).*?(?:este|this|su|its)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCARD_GENERAL_PATTERN = Pattern.compile(
            "(?:descarta|discard|discards?)\\s+(\\d+)\\s+(?:cartas?|cards?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHUFFLE_HAND_PATTERN = Pattern.compile(
            "(?:mezcla|shuffle|mezclar).*?(?:mano|hand).*?(?:en|into).*?(?:mazo|deck)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHUFFLE_DECK_PATTERN = Pattern.compile(
            "(?:baraja|shuffle).*?(?:mazo|deck)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FLIP_STATUS_PATTERN = Pattern.compile(
            "(?:Moneda|Coin|coin|flip).*?(?:Paraliza|Paraly|Confunde|Confus|Envenena|Poison|Dormido|Asleep|Sleep|Quema|Burn)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONDITIONAL_DAMAGE_PATTERN = Pattern.compile(
            "(?:If|Si)\\s+(heads|tails|caras|cruces).*?(?:does|hace)\\s+(\\d+)\\s+(?:more\\s+)?damage",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DAMAGE_BENCH_SIMPLE_PATTERN = Pattern.compile(
            "(?:a|to|para|contra)\\s+(\\d+)\\s+(?:daño|damage).*?(?:Banca|Bench|benched)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SELF_DAMAGE_PATTERN = Pattern.compile(
            "(?:Este|this).*?(?:Pok[eé]mon|Pokemon).*?(?:se|does).*?(?:hace|take).*?(\\d+).*?(?:de|of).*?(?:daño|damage)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ENERGY_ATTACH_PATTERN = Pattern.compile(
            "(?:une|attach|search.*attach|busca.*une).*?(?:(?:una?|1|\\d+)\\s+)?(?:(\\w+)\\s+)?(?:Energ[íi]a|Energy)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DRAW_SHUFFLE_PATTERN = Pattern.compile(
            "(?:mezcla|shuffle|mezclar).*?(?:mano|hand).*?(?:en|into).*?(?:mazo|deck).*?(?:roba|draw)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DRAW_OPPONENT_SHUFFLE_PATTERN = Pattern.compile(
            "(?:rival|opponent|defending).*?(?:mezcla|shuffle).*?(?:mano|hand).*?(?:en|into).*?(?:mazo|deck).*?(?:roba|draw)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SEARCH_PATTERN = Pattern.compile(
            "(?:Busca|Search|search).*?(?:en|your).*?(?:mazo|deck)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern RECYCLE_PATTERN = Pattern.compile(
            "(?:Pon|Put|put|take).*?(?:1|una?|an?|\\d+).*?(?:carta|card).*?(?:de|from).*?(?:tu|your).*?(?:descarte|discard)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_OPPONENT_HAND_PATTERN = Pattern.compile(
            "(?:descarta|discard|discards?).*?(?:1|una?|\\d+).*?(?:carta|card).*?(?:mano|hand).*?(?:rival|opponent|de tu rival)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CONDITIONAL_TIMING_PATTERN = Pattern.compile(
            "(?:Si|If).*?(?:daño|damage).*?(\\d+)\\s*(?:o|or)\\s*(?:m[aá]s|more)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_FROM_DECK_OPP_PATTERN = Pattern.compile(
            "descarta(?:r)? (?:la|una?|1)?\\s*carta superior del mazo (?:rival|oponente|de tu rival)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_FROM_DECK_SELF_PATTERN = Pattern.compile(
            "descarta(?:r)? (?:la|una?|1)?\\s*carta superior del mazo(?!.*(?:rival|oponente|de tu rival|opponent))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_ATTACH_IF_PATTERN = Pattern.compile(
            "Si es (\\w+),?\\s*únela",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_DAMAGE_BONUS_PATTERN = Pattern.compile(
            "Si es (?:Energía\\s+)?(\\w+),?\\s*\\+(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DECK_PEEK_SELF_PATTERN = Pattern.compile(
            "Mira las (\\d+) cartas superiores de tu mazo",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DECK_PEEK_OPP_PATTERN = Pattern.compile(
            "Mira (?:la|una?|1)?\\s*carta superior del mazo (?:rival|oponente|de tu rival)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DAMAGE_BENCH_SPLASH_PATTERN = Pattern.compile(
            "(\\d+)\\s+(?:daño|damage)\\s+(?:a|to)\\s+(\\d+).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXTRA_BENCH_DAMAGE_PATTERN = Pattern.compile(
            "\\+(\\d+)\\s+(?:a|to)\\s+(\\d+).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SELF_BENCH_DAMAGE_PATTERN = Pattern.compile(
            "(\\d+)\\s+(?:daño|damage).*?(?:cada|each).*?(?:Pok[eé]mon|Pokemon).*?(?:de|of).*?(?:tu|your).*?(?:Banca|Bench)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DISCARD_ALL_SELF_PATTERN = Pattern.compile(
            "(?:descarta|discard)\\s+(?:toda[s]?|all)(?:\\s+(?:la[s]?\\s+)?)?(?:Energ[íi]a[s]?|Energy).*?(?:attached\\s+to\\s+(?:this|este)|adjunta|de este|from this|de su|its)?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern COND_DISCARD_ENERGY_OPP_PATTERN = Pattern.compile(
            "if\\s+(heads|tails)[^.]*?discard[^.]*?energy",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern COND_DISCARD_ENERGY_SELF_PATTERN = Pattern.compile(
            "if\\s+(heads|tails)[^.]*?discard[^.]*?energ[^.]*?(?:from|de)\\s+(?:this|este)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern MOVE_ENERGY_PATTERN = Pattern.compile(
            "(?:Mueve|Move)\\s+(\\d+)\\s+Energ(?:[íi]a|y).*?(?:de este|from this).*?(?:a tu Banca|to.*?Bench)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern REMOVE_CONDITION_PATTERN = Pattern.compile(
            "(?:remove|remueve|elimina).*?(?:special condition|condición especial)",
            Pattern.CASE_INSENSITIVE);

    public List<AttackEffect> resolve(String effectText) {
        if (effectText == null || effectText.isBlank()) {
            return Collections.emptyList();
        }

        String lower = effectText.toLowerCase();
        List<AttackEffect> effects = new ArrayList<>();

        if (DRAW_OPPONENT_SHUFFLE_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:roba|draw)s?\\s+(\\d+)\\s+(?:cartas?|cards?)", 1);
            effects.add(new AttackEffect(EffectOperation.DRAW_OPPONENT_SHUFFLE,
                    TargetScope.OPPONENT, amount, EffectTiming.INSTEAD_OF_DAMAGE));
            return effects;
        }

        if (DRAW_SHUFFLE_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:roba|draw)s?\\s+(\\d+)\\s+(?:cartas?|cards?)", 1);
            effects.add(new AttackEffect(EffectOperation.DRAW_THEN_SHUFFLE,
                    TargetScope.SELF, amount, EffectTiming.INSTEAD_OF_DAMAGE));
            return effects;
        }

        if (DRAW_PATTERN.matcher(effectText).find()) {
            int amount = extractInt(lower, "(?:roba|draw)s?\\s+(\\d+)\\s+(?:cartas?|cards?)", 1);
            if (effectText.toUpperCase().contains("DOUBLE DRAW")
                    || effectText.toUpperCase().contains("FILCH")
                    || effectText.toUpperCase().contains("ME FIRST")) {
                effects.add(new AttackEffect(EffectOperation.DRAW,
                        TargetScope.SELF, amount, EffectTiming.INSTEAD_OF_DAMAGE));
            } else {
                effects.add(new AttackEffect(EffectOperation.DRAW,
                        TargetScope.SELF, amount, EffectTiming.WITH_DAMAGE));
            }
        }

        if (HEAL_PATTERN.matcher(effectText).find()) {
            int amount = extractInt(lower, "(?:cura|heal)s?\\s+(\\d+)", 10);
            boolean isBench = lower.contains("bench") || lower.contains("banca")
                    || lower.contains("each") || lower.contains("cada uno");
            TargetScope healTarget = isBench ? TargetScope.SELF_BENCH : TargetScope.SELF_ACTIVE;
            effects.add(new AttackEffect(EffectOperation.HEAL, healTarget, amount, EffectTiming.AFTER_DAMAGE));
        }

        if (SWITCH_SELF_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SWITCH_SELF,
                    TargetScope.SELF, 0, EffectTiming.AFTER_DAMAGE));
        }

        if (SWITCH_OPPONENT_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SWITCH_OPPONENT,
                    TargetScope.OPPONENT, 0, EffectTiming.AFTER_DAMAGE));
        } else if (SWITCH_OPPONENT_SPANISH_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SWITCH_OPPONENT,
                    TargetScope.OPPONENT, 0, EffectTiming.AFTER_DAMAGE));
        } else if (SWITCH_OPPONENT_SHORT_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SWITCH_OPPONENT,
                    TargetScope.OPPONENT, 0, EffectTiming.AFTER_DAMAGE));
        }

        if (CONDITIONAL_DAMAGE_PATTERN.matcher(lower).find()) {
            Matcher m = CONDITIONAL_DAMAGE_PATTERN.matcher(lower);
            if (m.find()) {
                String condStr = m.group(1).toLowerCase();
                EffectCondition cond = condStr.contains("head") || condStr.contains("cara") 
                                       ? EffectCondition.HEADS : EffectCondition.TAILS;
                int amount = Integer.parseInt(m.group(2));
                effects.add(new AttackEffect(EffectOperation.ADD_DAMAGE,
                        TargetScope.OPP_ACTIVE, amount, 0, cond, null, EffectTiming.WITH_DAMAGE, false));
            }
        }

        if (SELF_BENCH_DAMAGE_PATTERN.matcher(lower).find()) {
            int amount = 10;
            Matcher m = SELF_BENCH_DAMAGE_PATTERN.matcher(lower);
            if (m.find()) amount = Integer.parseInt(m.group(1));
            effects.add(new AttackEffect(EffectOperation.SELF_BENCH_DAMAGE,
                    TargetScope.SELF_BENCH, amount, 0, null, EffectTiming.WITH_DAMAGE, false));
        }

        Matcher splashM = DAMAGE_BENCH_SPLASH_PATTERN.matcher(lower);
        boolean hasSplash = splashM.find();

        Matcher extraM = EXTRA_BENCH_DAMAGE_PATTERN.matcher(lower);
        boolean hasExtra = extraM.find();

        if (hasSplash) {
            int amount = Integer.parseInt(splashM.group(1));
            int targetCount = Integer.parseInt(splashM.group(2));
            effects.add(new AttackEffect(EffectOperation.DAMAGE_BENCH_SPLASH,
                    TargetScope.OPP_BENCH, amount, targetCount, null, EffectTiming.WITH_DAMAGE, false));
        } else if (hasExtra) {
            int amount = Integer.parseInt(extraM.group(1));
            int targetCount = Integer.parseInt(extraM.group(2));
            effects.add(new AttackEffect(EffectOperation.DAMAGE_BENCH_SPLASH,
                    TargetScope.OPP_BENCH, amount, targetCount, null, EffectTiming.WITH_DAMAGE, false));
        } else if (DAMAGE_BENCH_SIMPLE_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:a|to|para|contra)\\s+(\\d+)\\s+(?:daño|damage)", 10);
            effects.add(new AttackEffect(EffectOperation.DAMAGE_BENCH,
                    TargetScope.OPP_BENCH, amount, 0, null, EffectTiming.WITH_DAMAGE, false));
        }

        if (SELF_DAMAGE_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:hace|take)s?\\s+(\\d+)", 10);
            effects.add(new AttackEffect(EffectOperation.DAMAGE_SELF,
                    TargetScope.SELF_ACTIVE, amount, EffectTiming.AFTER_DAMAGE));
        }

        if (MOVE_ENERGY_PATTERN.matcher(lower).find()) {
            Matcher m = MOVE_ENERGY_PATTERN.matcher(lower);
            int amount = 1;
            if (m.find()) {
                try { amount = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
            }
            effects.add(new AttackEffect(EffectOperation.MOVE_ENERGY,
                    TargetScope.SELF_BENCH, amount, EffectTiming.AFTER_DAMAGE));
        }

        boolean isMultiCoin = lower.contains("monedas") && lower.contains("por cada cara");
        boolean handledAsMultiCoin = false;

        if (isMultiCoin) {
            int coinCount = extractInt(lower, "(?:hasta\\s+)?(\\d+)\\s+monedas", 3);
            if (ENERGY_ATTACH_PATTERN.matcher(lower).find()) {
                Matcher m = ENERGY_ATTACH_PATTERN.matcher(lower);
                String energyType = null;
                if (m.find()) {
                    energyType = m.group(1);
                }
                boolean toBench = lower.contains("a tu banca") || lower.contains("to your bench");
                TargetScope target = toBench ? TargetScope.SELF_BENCH : TargetScope.SELF;
                String encoded = "energy_attach:" + (energyType != null ? energyType : "");
                effects.add(new AttackEffect(EffectOperation.MULTI_COIN, target, coinCount, encoded, EffectTiming.AFTER_DAMAGE));
                handledAsMultiCoin = true;
            }
        }

        if (!handledAsMultiCoin && ENERGY_ATTACH_PATTERN.matcher(lower).find()) {
            Matcher m = ENERGY_ATTACH_PATTERN.matcher(lower);
            String energyType = null;
            if (m.find()) {
                energyType = m.group(1);
            }
            boolean fromDiscard = lower.contains("descarte") || lower.contains("from your discard");
            boolean toBench = lower.contains("a tu banca") || lower.contains("to your bench");
            TargetScope target = toBench ? TargetScope.SELF_BENCH : TargetScope.SELF;

            if (fromDiscard) {
                effects.add(new AttackEffect(EffectOperation.ENERGY_ATTACH_FROM_DISCARD,
                        target, 1, energyType, EffectTiming.AFTER_DAMAGE));
            } else {
                effects.add(new AttackEffect(EffectOperation.ENERGY_ATTACH,
                        target, 1, energyType, EffectTiming.AFTER_DAMAGE));
            }
        }

        if (DISCARD_ALL_SELF_PATTERN.matcher(effectText).find()
                && !lower.contains("opponent") && !lower.contains("rival")
                && !lower.contains("defending") && !lower.contains("del rival")) {
            effects.add(new AttackEffect(EffectOperation.DISCARD_ALL_ENERGY_SELF,
                    TargetScope.SELF_ACTIVE, 0, EffectTiming.BEFORE_DAMAGE));
        }

        boolean hasDiscard = DISCARD_ENERGY_PATTERN.matcher(effectText).find()
                && !DISCARD_ALL_SELF_PATTERN.matcher(effectText).find();
        if (hasDiscard) {
            Matcher condSelfM = COND_DISCARD_ENERGY_SELF_PATTERN.matcher(lower);
            Matcher condOppM = COND_DISCARD_ENERGY_OPP_PATTERN.matcher(lower);
            if (condSelfM.find()) {
                String condStr = condSelfM.group(1).toLowerCase();
                EffectCondition cond = (condStr.contains("head") || condStr.contains("cara"))
                        ? EffectCondition.HEADS : EffectCondition.TAILS;
                effects.add(new AttackEffect(EffectOperation.DISCARD_ENERGY_SELF,
                        TargetScope.SELF_ACTIVE, 1, 0, cond, null, EffectTiming.WITH_DAMAGE, false));
            } else if (condOppM.find()) {
                String condStr = condOppM.group(1).toLowerCase();
                EffectCondition cond = (condStr.contains("head") || condStr.contains("cara"))
                        ? EffectCondition.HEADS : EffectCondition.TAILS;
                effects.add(new AttackEffect(EffectOperation.DISCARD_ENERGY,
                        TargetScope.OPP_ACTIVE, 1, 0, cond, null, EffectTiming.WITH_DAMAGE, false));
            } else if (DISCARD_ENERGY_SELF_PATTERN.matcher(lower).find()
                    && !lower.contains("opponent") && !lower.contains("rival")
                    && !lower.contains("defending") && !lower.contains("activo")) {
                effects.add(new AttackEffect(EffectOperation.DISCARD_ENERGY_SELF,
                        TargetScope.SELF_ACTIVE, 1, EffectTiming.WITH_DAMAGE));
            } else {
                effects.add(new AttackEffect(EffectOperation.DISCARD_ENERGY,
                        TargetScope.OPP_ACTIVE, 1, EffectTiming.WITH_DAMAGE));
            }
        }

        if (RECYCLE_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:pon|put|take).*?(\\d+).*?(?:carta|card)", 1);
            String subType = null;
            if (lower.contains("cima") || lower.contains("top of")) {
                subType = "deck_top";
            } else if (lower.contains("objeto") || lower.contains("item")) {
                subType = "trainer";
            } else if (lower.contains("energ")) {
                subType = "energy";
            }
            effects.add(new AttackEffect(EffectOperation.RECYCLE,
                    TargetScope.SELF, amount, subType, EffectTiming.AFTER_DAMAGE));
        }

        if (DISCARD_FROM_DECK_OPP_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.DISCARD_FROM_DECK,
                    TargetScope.OPPONENT, 1, EffectTiming.WITH_DAMAGE));
        } else if (DISCARD_FROM_DECK_SELF_PATTERN.matcher(lower).find()) {
            String subType = null;
            Matcher dmgBonusM = DISCARD_DAMAGE_BONUS_PATTERN.matcher(lower);
            Matcher attachM = DISCARD_ATTACH_IF_PATTERN.matcher(lower);
            if (dmgBonusM.find()) {
                String eType = dmgBonusM.group(1);
                String bonus = dmgBonusM.group(2);
                subType = "damage_bonus:" + bonus + ":" + eType;
            } else if (attachM.find()) {
                String eType = attachM.group(1);
                subType = "attach_if:" + eType;
            }
            effects.add(new AttackEffect(EffectOperation.DISCARD_FROM_DECK,
                    TargetScope.SELF, 1, subType, EffectTiming.WITH_DAMAGE));
        }

        if (DISCARD_OPPONENT_HAND_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:descarta|discard|discards?)\\s+(\\d+)", 1);
            effects.add(new AttackEffect(EffectOperation.DISCARD_OPPONENT,
                    TargetScope.OPPONENT, amount, EffectTiming.AFTER_DAMAGE));
        } else if (DISCARD_GENERAL_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:descarta|discard|discards?)\\s+(\\d+)", 1);
            effects.add(new AttackEffect(EffectOperation.DISCARD,
                    TargetScope.SELF, amount, EffectTiming.AFTER_DAMAGE));
        }

        if (SEARCH_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "(?:search for|busca|search).*?(?:hasta\\s+)?(\\d+).*?(?:carta|card)", 1);
            String type = null;
            if (lower.contains("trainer") || lower.contains("entrenador")
                    || lower.contains("partidario") || lower.contains("objeto")
                    || lower.contains("item") || lower.contains("objetos")) {
                type = "trainer";
            } else if (lower.contains("energy") || lower.contains("energía")
                    || lower.contains("energia") || lower.contains("energías")) {
                type = "energy";
            }
            effects.add(new AttackEffect(EffectOperation.SEARCH,
                    TargetScope.SELF, amount, type, EffectTiming.INSTEAD_OF_DAMAGE));
        }

        if (SHUFFLE_HAND_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SHUFFLE_HAND_INTO_DECK,
                    TargetScope.SELF, 0, EffectTiming.AFTER_DAMAGE));
        }

        if (SHUFFLE_DECK_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.SHUFFLE_DECK,
                    TargetScope.SELF, 0, EffectTiming.AFTER_DAMAGE));
        }

        if (DECK_PEEK_SELF_PATTERN.matcher(lower).find()) {
            int amount = extractInt(lower, "Mira las (\\d+) cartas", 3);
            effects.add(new AttackEffect(EffectOperation.DECK_PEEK,
                    TargetScope.SELF, amount, EffectTiming.INSTEAD_OF_DAMAGE));
        } else if (DECK_PEEK_OPP_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.DECK_PEEK,
                    TargetScope.OPPONENT, 1, EffectTiming.INSTEAD_OF_DAMAGE));
        }

        if (REMOVE_CONDITION_PATTERN.matcher(lower).find()) {
            effects.add(new AttackEffect(EffectOperation.REMOVE_CONDITION,
                    TargetScope.OPP_ACTIVE, 0, EffectTiming.AFTER_DAMAGE));
        }

        if (FLIP_STATUS_PATTERN.matcher(effectText).find()) {
            String statusSubType = null;
            String upper = effectText.toUpperCase();
            if (upper.contains("PARALIZ") || upper.contains("PARALY")) statusSubType = "PARALYZED";
            else if (upper.contains("CONFUS") || upper.contains("CONFUND")) statusSubType = "CONFUSED";
            else if (upper.contains("ENVENENA") || upper.contains("POISON")) statusSubType = "POISONED";
            else if (upper.contains("DORMIDO") || upper.contains("ASLEEP") || upper.contains("SLEEP")) statusSubType = "ASLEEP";
            else if (upper.contains("QUEMA") || upper.contains("BURN")) statusSubType = "BURNED";
            effects.add(new AttackEffect(EffectOperation.FLIP_COIN_STATUS,
                    TargetScope.OPP_ACTIVE, 0, statusSubType, EffectTiming.WITH_DAMAGE));
        }

        Matcher timingMatcher = CONDITIONAL_TIMING_PATTERN.matcher(lower);
        if (timingMatcher.find()) {
            try {
                int minDamage = Integer.parseInt(timingMatcher.group(1));
                for (AttackEffect e : effects) {
                    e.setMinDamage(minDamage);
                }
            } catch (NumberFormatException ignored) {}
        }

        return effects;
    }

    private int extractInt(String text, String regex, int defaultVal) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
