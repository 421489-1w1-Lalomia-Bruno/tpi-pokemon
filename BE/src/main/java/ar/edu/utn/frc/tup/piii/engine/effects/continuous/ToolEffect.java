package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

public interface ToolEffect {

    int modifyAttackDamage(int damage);

    int modifyDefenseDamage(int damage);

    int modifyRetreatCost(int baseCost);
}
