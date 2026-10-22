package ar.edu.utn.frc.tup.piii.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionConstructorsTest {

    @Test
    void deckOwnershipException_holdsMessage() {
        DeckOwnershipException ex = new DeckOwnershipException("not yours");
        assertEquals("not yours", ex.getMessage());
    }

    @Test
    void matchStateException_holdsMessage() {
        MatchStateException ex = new MatchStateException("bad state");
        assertEquals("bad state", ex.getMessage());
    }

    @Test
    void gameRuleViolationException_holdsMessage() {
        GameRuleViolationException ex = new GameRuleViolationException("rule broken");
        assertEquals("rule broken", ex.getMessage());
    }

    @Test
    void invalidActionException_holdsMessage() {
        InvalidActionException ex = new InvalidActionException("invalid");
        assertEquals("invalid", ex.getMessage());
    }

    @Test
    void externalApiException_constructors() {
        ExternalApiException ex1 = new ExternalApiException("msg");
        assertEquals("msg", ex1.getMessage());
        assertNull(ex1.getStatusCode());

        ExternalApiException ex2 = new ExternalApiException("msg", new RuntimeException("cause"));
        assertEquals("msg", ex2.getMessage());
        assertNull(ex2.getStatusCode());

        ExternalApiException ex3 = new ExternalApiException("msg", new RuntimeException("cause"), 502);
        assertEquals("msg", ex3.getMessage());
        assertEquals(502, ex3.getStatusCode());
    }

    @Test
    void gameStateEntity_defaultBuilder() {
        ar.edu.utn.frc.tup.piii.entities.GameStateEntity entity =
                ar.edu.utn.frc.tup.piii.entities.GameStateEntity.builder()
                        .matchId(java.util.UUID.randomUUID())
                        .boardStateJson("{}")
                        .build();
        assertNotNull(entity);
    }
}
