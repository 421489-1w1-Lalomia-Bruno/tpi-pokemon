package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FairyGardenEffectTest {

    private PokemonInPlay pokemon;

    @BeforeEach
    void setUp() {
        PokemonCard card = new PokemonCard();
        card.setId("p1");
        card.setName("Pikachu");
        card.setHp(60);
        pokemon = new PokemonInPlay(card);
    }

    @Test
    void fromRule_fairyGarden_parsesCorrectly() {
        FairyGardenEffect effect = FairyGardenEffect.fromRule(
                "Each Pokémon that has any Fairy Energy attached to it " +
                "(both yours and your opponent's) has no Retreat Cost.");
        assertNotNull(effect);
    }

    @Test
    void fromRule_noFairyMention_returnsNull() {
        FairyGardenEffect effect = FairyGardenEffect.fromRule(
                "Each Pokémon that has no Retreat Cost.");
        assertNull(effect);
    }

    @Test
    void fromRule_null_returnsNull() {
        assertNull(FairyGardenEffect.fromRule(null));
    }

    @Test
    void modifyRetreatCost_withFairyEnergy_returnsZero() {
        EnergyCard fairy = new EnergyCard();
        fairy.setEnergyType(EnergyType.FAIRY);
        fairy.setProvidedEnergies(java.util.List.of(EnergyType.FAIRY));
        pokemon.getAttachedEnergy().add(fairy);

        FairyGardenEffect effect = new FairyGardenEffect();
        assertEquals(0, effect.modifyRetreatCost(pokemon, 2));
    }

    @Test
    void modifyRetreatCost_withOutFairyEnergy_returnsBaseCost() {
        EnergyCard grass = new EnergyCard();
        grass.setEnergyType(EnergyType.GRASS);
        grass.setProvidedEnergies(java.util.List.of(EnergyType.GRASS));
        pokemon.getAttachedEnergy().add(grass);

        FairyGardenEffect effect = new FairyGardenEffect();
        assertEquals(2, effect.modifyRetreatCost(pokemon, 2));
    }

    @Test
    void hasNoWeakness_alwaysFalse() {
        FairyGardenEffect effect = new FairyGardenEffect();
        assertFalse(effect.hasNoWeakness(pokemon));
    }
}
