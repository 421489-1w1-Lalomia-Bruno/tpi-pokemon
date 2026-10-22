package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HardCharmEffectTest {

    @Test
    void fromRule_hardCharm_parsesCorrectly() {
        HardCharmEffect effect = HardCharmEffect.fromRule(
                "Any damage done to the Pokémon this card is attached to by an opponent's " +
                "attack is reduced by 20 (after applying Weakness and Resistance).");
        assertNotNull(effect);
        assertEquals(10, effect.modifyDefenseDamage(30));
    }

    @Test
    void fromRule_noAfterWeakness_returnsNull() {
        HardCharmEffect effect = HardCharmEffect.fromRule(
                "Damage done to this Pokémon is reduced by 20.");
        assertNull(effect);
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(HardCharmEffect.fromRule("Draw 3 cards."));
        assertNull(HardCharmEffect.fromRule(null));
    }

    @Test
    void modifyAttackDamage_noChange() {
        HardCharmEffect effect = new HardCharmEffect(20);
        assertEquals(30, effect.modifyAttackDamage(30));
    }

    @Test
    void modifyDefenseDamage_subtracts20() {
        HardCharmEffect effect = new HardCharmEffect(20);
        assertEquals(10, effect.modifyDefenseDamage(30));
    }

    @Test
    void modifyDefenseDamage_minimumZero() {
        HardCharmEffect effect = new HardCharmEffect(20);
        assertEquals(0, effect.modifyDefenseDamage(10));
    }

    @Test
    void modifyRetreatCost_noChange() {
        HardCharmEffect effect = new HardCharmEffect(20);
        assertEquals(2, effect.modifyRetreatCost(2));
    }
}
