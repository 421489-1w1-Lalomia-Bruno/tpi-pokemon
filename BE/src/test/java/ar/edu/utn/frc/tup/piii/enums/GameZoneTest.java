package ar.edu.utn.frc.tup.piii.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameZoneTest {

    @Test
    void enum_hasExpectedValues() {
        GameZone[] values = GameZone.values();
        assertEquals(7, values.length);
        assertArrayEquals(new GameZone[]{
                GameZone.HAND,
                GameZone.DECK,
                GameZone.DISCARD,
                GameZone.ACTIVE,
                GameZone.BENCH,
                GameZone.PRIZE,
                GameZone.STADIUM
        }, values);
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertEquals(GameZone.HAND, GameZone.valueOf("HAND"));
        assertEquals(GameZone.DECK, GameZone.valueOf("DECK"));
        assertEquals(GameZone.ACTIVE, GameZone.valueOf("ACTIVE"));
        assertEquals(GameZone.BENCH, GameZone.valueOf("BENCH"));
        assertEquals(GameZone.STADIUM, GameZone.valueOf("STADIUM"));
    }
}
