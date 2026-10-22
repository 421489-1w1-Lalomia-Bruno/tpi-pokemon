package ar.edu.utn.frc.tup.piii.engine.effects.attack;

public class AttackEffect {
    private final EffectOperation operation;
    private final TargetScope target;
    private final int amount;
    private final int targetCount;
    private final EffectCondition condition;
    private int minDamage;
    private final String subType;
    private final EffectTiming timing;
    private final boolean optional;

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         String subType, EffectTiming timing, boolean optional) {
        this(operation, target, amount, 0, EffectCondition.ALWAYS, 0, subType, timing, optional);
    }

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         int targetCount, String subType, EffectTiming timing, boolean optional) {
        this(operation, target, amount, targetCount, EffectCondition.ALWAYS, 0, subType, timing, optional);
    }

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         int targetCount, EffectCondition condition, String subType, EffectTiming timing, boolean optional) {
        this(operation, target, amount, targetCount, condition, 0, subType, timing, optional);
    }

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         int targetCount, EffectCondition condition, int minDamage, String subType, EffectTiming timing, boolean optional) {
        this.operation = operation;
        this.target = target;
        this.amount = amount;
        this.targetCount = targetCount;
        this.condition = condition;
        this.minDamage = minDamage;
        this.subType = subType;
        this.timing = timing;
        this.optional = optional;
    }

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         EffectTiming timing) {
        this(operation, target, amount, 0, EffectCondition.ALWAYS, 0, null, timing, false);
    }

    public AttackEffect(EffectOperation operation, TargetScope target, int amount,
                         String subType, EffectTiming timing) {
        this(operation, target, amount, 0, EffectCondition.ALWAYS, 0, subType, timing, false);
    }

    public EffectOperation getOperation() { return operation; }
    public TargetScope getTarget() { return target; }
    public int getAmount() { return amount; }
    public int getTargetCount() { return targetCount; }
    public EffectCondition getCondition() { return condition; }
    public int getMinDamage() { return minDamage; }
    public void setMinDamage(int minDamage) { this.minDamage = minDamage; }
    public String getSubType() { return subType; }
    public EffectTiming getTiming() { return timing; }
    public boolean isOptional() { return optional; }
}
