package ar.edu.utn.frc.tup.piii.engine.effects.ability;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlockTrainerEffectTest {

    private final BlockTrainerEffect effect =
            new BlockTrainerEffect(Set.of("ITEM", "SUPPORTER"));

    @Test
    void canApply_alwaysTrue() {
        assertTrue(effect.canApply(null));
    }

    @Test
    void apply_isNoOp() {
        assertDoesNotThrow(() -> effect.apply(null));
    }

    @Test
    void blocksTrainerPlay_trueForBlockedSubtype_caseInsensitive() {
        assertTrue(effect.blocksTrainerPlay("item", null));
        assertTrue(effect.blocksTrainerPlay("SUPPORTER", null));
    }

    @Test
    void blocksTrainerPlay_falseForNonBlockedSubtype() {
        assertFalse(effect.blocksTrainerPlay("STADIUM", null));
    }

    @Test
    void blocksTrainerPlay_falseForNullSubtype() {
        assertFalse(effect.blocksTrainerPlay(null, null));
    }

    @Test
    void getBlockedSubtypes_returnsConfiguredSet() {
        assertEquals(Set.of("ITEM", "SUPPORTER"), effect.getBlockedSubtypes());
    }
}
