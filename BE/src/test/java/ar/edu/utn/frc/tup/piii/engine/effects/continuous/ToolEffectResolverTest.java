package ar.edu.utn.frc.tup.piii.engine.effects.continuous;

import ar.edu.utn.frc.tup.piii.engine.models.TrainerCard;
import ar.edu.utn.frc.tup.piii.enums.TrainerSubtype;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolEffectResolverTest {

    @Test
    void resolve_muscleBand_returnsMuscleBandEffect() {
        TrainerCard tool = new TrainerCard();
        tool.setId("mb-1");
        tool.setName("Muscle Band");
        tool.setSubtype(TrainerSubtype.TOOL);
        tool.setEffect("[\"The attacks of the Pokémon this card is attached to do 20 more damage " +
                "to your opponent's Active Pokémon (before applying Weakness and Resistance).\"]");

        ToolEffect effect = ToolEffectResolver.resolve(tool);
        assertInstanceOf(MuscleBandEffect.class, effect);
        assertEquals(50, effect.modifyAttackDamage(30));
    }

    @Test
    void resolve_muscleBand_realApiText_returnsMuscleBandEffect() {
        TrainerCard tool = new TrainerCard();
        tool.setId("mb-real");
        tool.setName("Muscle Band");
        tool.setSubtype(TrainerSubtype.TOOL);
        tool.setEffect("[\"Attach a Pokémon Tool to 1 of your Pokémon that doesn't already have a Pokémon Tool attached to it.\",\"The attacks of the Pokémon this card is attached to do 20 more damage to your opponent's Active Pokémon (before applying Weakness and Resistance).\",\"You may play as many Item cards as you like during your turn (before your attack).\"]");

        ToolEffect effect = ToolEffectResolver.resolve(tool);
        assertInstanceOf(MuscleBandEffect.class, effect);
        assertEquals(50, effect.modifyAttackDamage(30));
    }

    @Test
    void resolve_hardCharm_returnsHardCharmEffect() {
        TrainerCard tool = new TrainerCard();
        tool.setId("hc-1");
        tool.setName("Hard Charm");
        tool.setSubtype(TrainerSubtype.TOOL);
        tool.setEffect("[\"Any damage done to the Pokémon this card is attached to by an " +
                "opponent's attack is reduced by 20 (after applying Weakness and Resistance).\"]");

        ToolEffect effect = ToolEffectResolver.resolve(tool);
        assertInstanceOf(HardCharmEffect.class, effect);
        assertEquals(10, effect.modifyDefenseDamage(30));
    }

    @Test
    void resolve_nullTool_returnsNoEffect() {
        ToolEffect effect = ToolEffectResolver.resolve(null);
        assertEquals(30, effect.modifyAttackDamage(30));
        assertEquals(30, effect.modifyDefenseDamage(30));
        assertEquals(2, effect.modifyRetreatCost(2));
    }

    @Test
    void resolve_nullEffect_returnsNoEffect() {
        TrainerCard tool = new TrainerCard();
        tool.setId("t1");
        tool.setEffect(null);
        ToolEffect effect = ToolEffectResolver.resolve(tool);
        assertEquals(30, effect.modifyAttackDamage(30));
    }

    @Test
    void resolve_unknownTool_returnsNoEffect() {
        TrainerCard tool = new TrainerCard();
        tool.setId("t1");
        tool.setName("Unknown Tool");
        tool.setSubtype(TrainerSubtype.TOOL);
        tool.setEffect("[\"This tool does something mysterious.\"]");

        ToolEffect effect = ToolEffectResolver.resolve(tool);
        assertEquals(30, effect.modifyAttackDamage(30));
        assertEquals(30, effect.modifyDefenseDamage(30));
    }
}
