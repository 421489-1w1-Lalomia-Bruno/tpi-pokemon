package ar.edu.utn.frc.tup.piii.engine.combat;

import ar.edu.utn.frc.tup.piii.engine.effects.continuous.StadiumEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.continuous.StadiumEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.effects.continuous.ToolEffect;
import ar.edu.utn.frc.tup.piii.engine.effects.continuous.ToolEffectResolver;
import ar.edu.utn.frc.tup.piii.engine.models.Attack;
import ar.edu.utn.frc.tup.piii.engine.models.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.models.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;

public class DamageCalculator {

    public int calculateDamage(AttackContext context) {
        PokemonInPlay defender = context.getDefender();
        int baseDamage = context.getBaseDamage() != null ? context.getBaseDamage() : 0;
        int damage = baseDamage;

        EnergyType attackType = resolveAttackType(context.getSelectedAttack());

        if (attackType != null) {
            String defenderWeakness = defender.getCard().getWeaknessType();
            String defenderResistance = defender.getCard().getResistanceType();

            boolean ignoreWeakness = false;
            if (context.getBoard() != null && context.getBoard().getSharedStadium() != null) {
                StadiumEffect stadium = StadiumEffectResolver.resolve(
                        context.getBoard().getSharedStadium());
                ignoreWeakness = stadium.hasNoWeakness(defender);
            }

            if (defenderWeakness != null && matchesType(attackType, defenderWeakness)
                    && !ignoreWeakness) {
                damage *= 2;
            }

            if (defenderResistance != null && matchesType(attackType, defenderResistance)) {
                damage = Math.max(damage - 20, 0);
            }
        }

        if (defender.getTool() != null) {
            ToolEffect tool = ToolEffectResolver.resolve(defender.getTool());
            damage = tool.modifyDefenseDamage(damage);
        }

        return Math.max(damage, 0);
    }

    private EnergyType resolveAttackType(Attack attack) {
        if (attack == null || attack.getCost() == null || attack.getCost().isEmpty()) {
            return null;
        }
        for (EnergyType cost : attack.getCost()) {
            if (cost != EnergyType.COLORLESS) {
                return cost;
            }
        }
        return EnergyType.COLORLESS;
    }

    private boolean matchesType(EnergyType attackType, String typeStr) {
        if (attackType == null || typeStr == null) {
            return false;
        }
        try {
            return attackType == EnergyType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
