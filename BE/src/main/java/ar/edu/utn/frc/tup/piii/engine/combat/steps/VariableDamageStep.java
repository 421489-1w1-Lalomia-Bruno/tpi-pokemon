package ar.edu.utn.frc.tup.piii.engine.combat.steps;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VariableDamageStep implements AttackStep {

    private static final Logger log = LoggerFactory.getLogger(VariableDamageStep.class);

    private static final Pattern VARIABLE_PREFIX = Pattern.compile(
        "^(\\d+)[\u00d7xX]\\s", Pattern.CASE_INSENSITIVE);

    private static final Pattern VARIABLE_PLUS_PREFIX = Pattern.compile(
        "^(\\d+)\\+\\s", Pattern.CASE_INSENSITIVE);

    private static final Pattern BENCHED_PATTERN = Pattern.compile(
        "for\\s+each\\s+of\\s+your\\s+Benched\\s+Pok[eé]mon", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_ENERGY_SELF_NX = Pattern.compile(
        "por cada Energía unida a este Pokémon(?!\\s*a\\s*ambos)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_ENERGY_SELF_TYPE = Pattern.compile(
        "\\+(\\d+)\\s*por cada Energía (\\w+) unida a este Pokémon", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_ENERGY_BOTH = Pattern.compile(
        "\\+(\\d+)\\s*por cada Energía unida a ambos Activos", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_COUNTER_SELF = Pattern.compile(
        "\\+(\\d+)\\s*por cada contador(?: de daño)? en este Pokémon", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_COUNTER_DEF = Pattern.compile(
        "\\+(\\d+)\\s*por cada contador(?: de daño)? en el Activo rival", Pattern.CASE_INSENSITIVE);

    private static final Pattern PER_DISTINCT_ENERGY = Pattern.compile(
        "\\+(\\d+)\\s*por cada tipo de Energía Básica diferente unida", Pattern.CASE_INSENSITIVE);

    private static final Pattern IF_EX = Pattern.compile(
        "\\+(\\d+)\\s*si el Activo rival es Pokémon-EX", Pattern.CASE_INSENSITIVE);

    private static final Pattern IF_LUNATONE = Pattern.compile(
        "\\+(\\d+)\\s*si Lunatone está en tu Banca", Pattern.CASE_INSENSITIVE);

    private static final Pattern IF_DEF_DAMAGED = Pattern.compile(
        "\\+(\\d+)\\s*si el Activo rival ya tiene contadores de daño", Pattern.CASE_INSENSITIVE);

    private static final Pattern IF_SPECIAL_COND = Pattern.compile(
        "\\+(\\d+)\\s*(?:si el Activo rival tiene Condición Especial" +
        "|more damage.*?special condition|does.*?more damage.*?special condition)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern IF_TYPE_MATCH = Pattern.compile(
        "\\+(\\d+)\\s*si el Activo rival es tipo (\\w+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern IF_HAS_ENERGY = Pattern.compile(
        "Si tiene Energía (\\w+),?\\s*\\+?(\\d+)", Pattern.CASE_INSENSITIVE);

    private AttackStep next;

    @Override
    public void execute(AttackContext context) {
        if (!context.isAttackSuccessful()) {
            log.debug("Attack not successful, skipping variable damage");
            executeNext(context);
            return;
        }

        Attack attack = context.getSelectedAttack();
        if (attack == null || attack.getEffectText() == null) {
            log.debug("No attack or no effectText, skipping variable damage");
            executeNext(context);
            return;
        }

        String primaryText = attack.getEffectText();
        String branchText = context.getBranchEffectText();
        boolean matched = tryMatchPattern(primaryText, context);
        if (!matched && branchText != null) {
            log.debug("[VariableDamageStep] primary no match, trying branchText='{}'", branchText);
            matched = tryMatchPattern(branchText, context);
        }
        if (!matched) {
            log.debug("[VariableDamageStep] no matching prefix, skipping");
            executeNext(context);
            return;
        }
        executeNext(context);
    }

    private int countCondition(String body, AttackContext context) {
        if (BENCHED_PATTERN.matcher(body).find()) {
            PlayerBoard attackerBoard = getAttackerBoard(context);
            return attackerBoard != null ? attackerBoard.getBench().size() : 0;
        }

        if (PER_ENERGY_SELF_NX.matcher(body).find()) {
            PlayerBoard attackerBoard = getAttackerBoard(context);
            if (attackerBoard != null && attackerBoard.getActivePokemon() != null) {
                return attackerBoard.getActivePokemon().getAttachedEnergy().size();
            }
            return 0;
        }

        return 0;
    }

    private int[] evaluatePlusBonus(String body, AttackContext ctx) {
        PokemonInPlay attacker = ctx.getAttacker();
        PokemonInPlay defender = ctx.getDefender();

        Matcher energyTypeMatcher = PER_ENERGY_SELF_TYPE.matcher(body);
        if (energyTypeMatcher.find()) {
            int bonus = Integer.parseInt(energyTypeMatcher.group(1));
            String typeName = energyTypeMatcher.group(2);
            int count = countEnergyOfType(attacker, typeName);
            return new int[]{bonus, count};
        }

        Matcher energyBothMatcher = PER_ENERGY_BOTH.matcher(body);
        if (energyBothMatcher.find()) {
            int bonus = Integer.parseInt(energyBothMatcher.group(1));
            int count = countAllEnergy(ctx);
            return new int[]{bonus, count};
        }

        Matcher counterSelfMatcher = PER_COUNTER_SELF.matcher(body);
        if (counterSelfMatcher.find()) {
            int bonus = Integer.parseInt(counterSelfMatcher.group(1));
            return new int[]{bonus, attacker.getDamageCounters()};
        }

        Matcher counterDefMatcher = PER_COUNTER_DEF.matcher(body);
        if (counterDefMatcher.find()) {
            int bonus = Integer.parseInt(counterDefMatcher.group(1));
            return new int[]{bonus, defender.getDamageCounters()};
        }

        Matcher distinctEnergyMatcher = PER_DISTINCT_ENERGY.matcher(body);
        if (distinctEnergyMatcher.find()) {
            int bonus = Integer.parseInt(distinctEnergyMatcher.group(1));
            return new int[]{bonus, countDistinctEnergyTypes(attacker)};
        }

        Matcher exMatcher = IF_EX.matcher(body);
        if (exMatcher.find()) {
            int bonus = Integer.parseInt(exMatcher.group(1));
            return new int[]{bonus, defender.getCard().getStage() == PokemonStage.EX ? 1 : 0};
        }

        Matcher lunatoneMatcher = IF_LUNATONE.matcher(body);
        if (lunatoneMatcher.find()) {
            int bonus = Integer.parseInt(lunatoneMatcher.group(1));
            PlayerBoard attackerBoard = getAttackerBoard(ctx);
            boolean hasLunatone = attackerBoard != null && hasPokemonOnBench(attackerBoard, "Lunatone");
            return new int[]{bonus, hasLunatone ? 1 : 0};
        }

        Matcher damagedMatcher = IF_DEF_DAMAGED.matcher(body);
        if (damagedMatcher.find()) {
            int bonus = Integer.parseInt(damagedMatcher.group(1));
            return new int[]{bonus, defender.getDamageCounters() > 0 ? 1 : 0};
        }

        Matcher condMatcher = IF_SPECIAL_COND.matcher(body);
        if (condMatcher.find()) {
            int bonus = Integer.parseInt(condMatcher.group(1));
            return new int[]{bonus, !defender.getSpecialConditions().isEmpty() ? 1 : 0};
        }

        Matcher typeMatcher = IF_TYPE_MATCH.matcher(body);
        if (typeMatcher.find()) {
            int bonus = Integer.parseInt(typeMatcher.group(1));
            String typeName = typeMatcher.group(2);
            return new int[]{bonus, isPokemonType(defender, typeName) ? 1 : 0};
        }

        Matcher hasEnergyMatcher = IF_HAS_ENERGY.matcher(body);
        if (hasEnergyMatcher.find()) {
            String typeName = hasEnergyMatcher.group(1);
            int bonus = Integer.parseInt(hasEnergyMatcher.group(2));
            return new int[]{bonus, countEnergyOfType(attacker, typeName) > 0 ? 1 : 0};
        }

        return new int[]{0, 0};
    }

    private int countEnergyOfType(PokemonInPlay pokemon, String typeName) {
        if (pokemon == null || typeName == null) return 0;
        return (int) pokemon.getAvailableEnergyPool().stream()
            .filter(e -> e.name().equalsIgnoreCase(typeName))
            .count();
    }

    private int countDistinctEnergyTypes(PokemonInPlay pokemon) {
        if (pokemon == null) return 0;
        return pokemon.getAvailableEnergyPool().stream()
            .collect(Collectors.toSet())
            .size();
    }

    private int countAllEnergy(AttackContext ctx) {
        int total = 0;
        if (ctx.getAttacker() != null) {
            total += ctx.getAttacker().getAttachedEnergy().size();
        }
        if (ctx.getDefender() != null) {
            total += ctx.getDefender().getAttachedEnergy().size();
        }
        return total;
    }

    private static PlayerBoard getAttackerBoard(AttackContext context) {
        GameBoard board = context.getBoard();
        String currentPlayerId = context.getCurrentPlayerId();
        if (board == null || currentPlayerId == null) return null;

        if (board.getPlayer1Board() != null
                && currentPlayerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null
                && currentPlayerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    private static PlayerBoard getOpponentBoard(AttackContext context) {
        GameBoard board = context.getBoard();
        String currentPlayerId = context.getCurrentPlayerId();
        if (board == null || currentPlayerId == null) return null;

        if (board.getPlayer1Board() != null
                && !currentPlayerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null
                && !currentPlayerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    private boolean hasPokemonOnBench(PlayerBoard board, String name) {
        if (board == null || name == null) return false;
        return board.getBench().stream()
            .anyMatch(p -> p.getCard() != null && name.equals(p.getCard().getName()));
    }

    private boolean isPokemonType(PokemonInPlay pokemon, String typeName) {
        if (pokemon == null || pokemon.getCard() == null || typeName == null) return false;
        return pokemon.getCard().getTypes().stream()
            .anyMatch(t -> t.name().equalsIgnoreCase(typeName));
    }

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    private boolean tryMatchPattern(String effectText, AttackContext context) {
        Matcher prefixMatcher = VARIABLE_PREFIX.matcher(effectText);
        Matcher plusMatcher = VARIABLE_PLUS_PREFIX.matcher(effectText);

        if (prefixMatcher.find()) {
            int multiplier = Integer.parseInt(prefixMatcher.group(1));
            String body = effectText.substring(prefixMatcher.end());
            log.debug("[VariableDamageStep] N× path: multiplier={} body='{}'", multiplier, body);

            int count = countCondition(body, context);
            log.debug("[VariableDamageStep] count={}", count);

            if (count > 0) {
                int computedDamage = multiplier * count;
                context.setBaseDamage(computedDamage);
                log.debug("[VariableDamageStep] SET baseDamage={} ({} × {})", computedDamage, multiplier, count);
            } else {
                log.debug("[VariableDamageStep] count=0, baseDamage stays at {}", context.getBaseDamage());
                if (BENCHED_PATTERN.matcher(body).find()) {
                    context.setVariableDamageZeroDescription(
                        "0 Pokémon en banca. El ataque no hizo daño.");
                }
            }
            return true;
        }

        if (plusMatcher.find()) {
            int baseDamage = Integer.parseInt(plusMatcher.group(1));
            String body = effectText.substring(plusMatcher.end());
            log.debug("[VariableDamageStep] N+ path: base={} body='{}'", baseDamage, body);

            int[] result = evaluatePlusBonus(body, context);
            int bonus = result[0];
            int count = result[1];
            log.debug("[VariableDamageStep] bonus={} count={}", bonus, count);

            if (count > 0 && bonus > 0) {
                int computedDamage = baseDamage + bonus * count;
                context.setBaseDamage(computedDamage);
                log.debug("[VariableDamageStep] SET baseDamage={} ({} + {} × {})", computedDamage, baseDamage, bonus, count);
            } else {
                context.setBaseDamage(baseDamage);
                log.debug("[VariableDamageStep] no bonus, baseDamage set to {}", baseDamage);
            }
            return true;
        }

        return false;
    }

    private void executeNext(AttackContext context) {
        if (next != null && context.isAttackSuccessful()) {
            next.execute(context);
        }
    }
}
