package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShadowCircleEffectTest {

    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        PokemonCard card = new PokemonCard();
        card.setId("p1");
        card.setName("Charizard");
        card.setHp(120);
        pokemon = new PokemonInPlay(card);
    }

    @Test
    void fromRule_shadowCircle_parsesCorrectly() {
        ShadowCircleEffect effect = ShadowCircleEffect.fromRule(
                "Each Pokémon that has any Darkness Energy attached to it " +
                "(both yours and your opponent's) has no Weakness.");
        assertNotNull(effect);
    }

    @Test
    void fromRule_noDarknessMention_returnsNull() {
        ShadowCircleEffect effect = ShadowCircleEffect.fromRule(
                "Each Pokémon has no Weakness.");
        assertNull(effect);
    }

    @Test
    void fromRule_null_returnsNull() {
        assertNull(ShadowCircleEffect.fromRule(null));
    }

    @Test
    void hasNoWeakness_withDarknessEnergy_returnsTrue() {
        EnergyCard darkness = new EnergyCard();
        darkness.setEnergyType(EnergyType.DARKNESS);
        darkness.setProvidedEnergies(java.util.List.of(EnergyType.DARKNESS));
        pokemon.getAttachedEnergy().add(darkness);

        ShadowCircleEffect effect = new ShadowCircleEffect();
        assertTrue(effect.hasNoWeakness(pokemon));
    }

    @Test
    void hasNoWeakness_withOutDarknessEnergy_returnsFalse() {
        EnergyCard fire = new EnergyCard();
        fire.setEnergyType(EnergyType.FIRE);
        fire.setProvidedEnergies(java.util.List.of(EnergyType.FIRE));
        pokemon.getAttachedEnergy().add(fire);

        ShadowCircleEffect effect = new ShadowCircleEffect();
        assertFalse(effect.hasNoWeakness(pokemon));
    }

    @Test
    void modifyRetreatCost_alwaysReturnsBaseCost() {
        ShadowCircleEffect effect = new ShadowCircleEffect();
        assertEquals(2, effect.modifyRetreatCost(pokemon, 2));
    }
}
