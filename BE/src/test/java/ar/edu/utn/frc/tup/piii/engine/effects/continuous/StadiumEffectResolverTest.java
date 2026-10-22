package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StadiumEffectResolverTest {

    @Test
    void resolve_fairyGarden_returnsFairyGardenEffect() {
        TrainerCard stadium = new TrainerCard();
        stadium.setId("fg-1");
        stadium.setName("Fairy Garden");
        stadium.setSubtype(TrainerSubtype.STADIUM);
        stadium.setEffect("[\"Each Pokémon that has any Fairy Energy attached to it " +
                "(both yours and your opponent's) has no Retreat Cost.\"]");

        StadiumEffect effect = StadiumEffectResolver.resolve(stadium);
        assertInstanceOf(FairyGardenEffect.class, effect);
    }

    @Test
    void resolve_shadowCircle_returnsShadowCircleEffect() {
        TrainerCard stadium = new TrainerCard();
        stadium.setId("sc-1");
        stadium.setName("Shadow Circle");
        stadium.setSubtype(TrainerSubtype.STADIUM);
        stadium.setEffect("[\"Each Pokémon that has any Darkness Energy attached to it " +
                "(both yours and your opponent's) has no Weakness.\"]");

        StadiumEffect effect = StadiumEffectResolver.resolve(stadium);
        assertInstanceOf(ShadowCircleEffect.class, effect);
    }

    @Test
    void resolve_nullStadium_returnsNoEffect() {
        StadiumEffect effect = StadiumEffectResolver.resolve(null);
        assertEquals(2, effect.modifyRetreatCost(null, 2));
        assertFalse(effect.hasNoWeakness(null));
    }

    @Test
    void resolve_unknownStadium_returnsNoEffect() {
        TrainerCard stadium = new TrainerCard();
        stadium.setId("s1");
        stadium.setName("Unknown Stadium");
        stadium.setSubtype(TrainerSubtype.STADIUM);
        stadium.setEffect("[\"This stadium does something else.\"]");

        StadiumEffect effect = StadiumEffectResolver.resolve(stadium);
        assertEquals(5, effect.modifyRetreatCost(new PokemonInPlay(), 5));
        assertFalse(effect.hasNoWeakness(null));
    }
}
