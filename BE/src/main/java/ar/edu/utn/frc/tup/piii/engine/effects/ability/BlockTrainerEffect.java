package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import java.util.Set;

public class BlockTrainerEffect implements AbilityEffect {

    private final Set<String> blockedSubtypes;

    public BlockTrainerEffect(Set<String> blockedSubtypes) {
        this.blockedSubtypes = blockedSubtypes;
    }

    @Override
    public boolean canApply(AbilityEffectContext ctx) {
        return true;
    }

    @Override
    public void apply(AbilityEffectContext ctx) {
        // Passive effect — enforced externally at action validation time
    }

    @Override
    public boolean blocksTrainerPlay(String trainerSubtype, AbilityEffectContext ctx) {
        return trainerSubtype != null && blockedSubtypes.contains(trainerSubtype.toUpperCase());
    }

    public Set<String> getBlockedSubtypes() {
        return blockedSubtypes;
    }
}
