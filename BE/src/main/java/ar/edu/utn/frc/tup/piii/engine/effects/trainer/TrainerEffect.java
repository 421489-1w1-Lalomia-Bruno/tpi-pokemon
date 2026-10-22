package ar.edu.utn.frc.tup.piii.engine.effects.trainer;

public interface TrainerEffect {

    boolean canApply(TrainerEffectContext ctx);

    void apply(TrainerEffectContext ctx);

    String getDescription();
}
