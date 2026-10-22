package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MuscleBandEffectTest {

    @Test
    void fromRule_muscleBand_parsesCorrectly() {
        MuscleBandEffect effect = MuscleBandEffect.fromRule(
                "The attacks of the Pokémon this card is attached to do 20 more damage " +
                "to your opponent's Active Pokémon (before applying Weakness and Resistance).");
        assertNotNull(effect);
        assertEquals(50, effect.modifyAttackDamage(30));
    }

    @Test
    void fromRule_noBeforeWeakness_returnsNull() {
        MuscleBandEffect effect = MuscleBandEffect.fromRule(
                "The attacks of this Pokémon do 20 more damage.");
        assertNull(effect);
    }

    @Test
    void fromRule_realApiText_parsesCorrectly() {
        MuscleBandEffect effect = MuscleBandEffect.fromRule(
                "The attacks of the Pokémon this card is attached to do 20 more damage " +
                "to your opponent's Active Pokémon (before applying Weakness and Resistance).");
        assertNotNull(effect);
        assertEquals(50, effect.modifyAttackDamage(30));
    }

    @Test
    void fromRule_noMatch_returnsNull() {
        assertNull(MuscleBandEffect.fromRule("Draw 3 cards."));
        assertNull(MuscleBandEffect.fromRule(null));
    }

    @Test
    void modifyAttackDamage_adds20() {
        MuscleBandEffect effect = new MuscleBandEffect(20);
        assertEquals(50, effect.modifyAttackDamage(30));
    }

    @Test
    void modifyDefenseDamage_noChange() {
        MuscleBandEffect effect = new MuscleBandEffect(20);
        assertEquals(30, effect.modifyDefenseDamage(30));
    }

    @Test
    void modifyRetreatCost_noChange() {
        MuscleBandEffect effect = new MuscleBandEffect(20);
        assertEquals(2, effect.modifyRetreatCost(2));
    }
}
