package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AbilityEffectDefaultTest {

    /** Implementación mínima que no sobreescribe blocksTrainerPlay → debe usar el default (false). */
    private final AbilityEffect effect = new AbilityEffect() {
        @Override
        public boolean canApply(AbilityEffectContext ctx) {
            return true;
        }

        @Override
        public void apply(AbilityEffectContext ctx) {
            // no-op
        }
    };

    @Test
    void blocksTrainerPlay_defaultReturnsFalse() {
        assertFalse(effect.blocksTrainerPlay("ITEM", null));
    }
}
