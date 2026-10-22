package ar.edu.utn.frc.tup.piii.engine.effects.ability;

public interface AbilityEffect {
    boolean canApply(AbilityEffectContext ctx);
    void apply(AbilityEffectContext ctx);

    default boolean blocksTrainerPlay(String trainerSubtype, AbilityEffectContext ctx) {
        return false;
    }
}
