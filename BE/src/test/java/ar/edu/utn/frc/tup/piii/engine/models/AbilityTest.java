package ar.edu.utn.frc.tup.piii.engine.models;

import ar.edu.utn.frc.tup.piii.enums.AbilityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbilityTest {

    @Test
    void defaultConstructor() {
        Ability a = new Ability();
        assertNull(a.getName());
        assertNull(a.getType());
        assertNull(a.getEffectText());
    }

    @Test
    void parameterizedConstructor() {
        Ability a = new Ability("Pokedex", AbilityType.POKE_BODY, "Look at top cards");
        assertEquals("Pokedex", a.getName());
        assertEquals(AbilityType.POKE_BODY, a.getType());
        assertEquals("Look at top cards", a.getEffectText());
    }

    @Test
    void settersAndGetters() {
        Ability a = new Ability();
        a.setName("Pokemon Power");
        a.setType(AbilityType.POKE_POWER);
        a.setEffectText("Free retreat");
        assertEquals("Pokemon Power", a.getName());
        assertEquals(AbilityType.POKE_POWER, a.getType());
        assertEquals("Free retreat", a.getEffectText());
    }
}
