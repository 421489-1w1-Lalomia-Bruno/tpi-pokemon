package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.exceptions.DeckValidationException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeckControllerTest extends EmbeddedPostgresTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DeckService deckService() {
            return Mockito.mock(DeckService.class);
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DeckService deckService;

    private MockMvc mockMvc;

    private final UUID deckId = UUID.randomUUID();
    private final UUID cardId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void listDecks_shouldReturnListOfDecks() throws Exception {
        DeckDTO deck1 = buildDeckDTO("Deck 1");
        DeckDTO deck2 = buildDeckDTO("Deck 2");
        when(deckService.getDecksByCreator("test-player-123")).thenReturn(List.of(deck1, deck2));

        mockMvc.perform(get("/api/decks")
                        .requestAttr("playerId", "test-player-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Deck 1"))
                .andExpect(jsonPath("$[1].name").value("Deck 2"));
    }

    @Test
    void getDeckById_shouldReturnDeckWhenExists() throws Exception {
        DeckDTO deck = buildDeckDTO("My Deck");
        when(deckService.getDeckById(eq(deckId), eq("test-player-123"))).thenReturn(deck);

        mockMvc.perform(get("/api/decks/{id}", deckId)
                        .requestAttr("playerId", "test-player-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Deck"))
                .andExpect(jsonPath("$.id").value(deckId.toString()));
    }

    @Test
    void getDeckById_shouldReturn404WhenNotFound() throws Exception {
        when(deckService.getDeckById(eq(deckId), eq("test-player-123")))
                .thenThrow(new EntityNotFoundException("Deck not found: " + deckId));

        mockMvc.perform(get("/api/decks/{id}", deckId)
                        .requestAttr("playerId", "test-player-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createDeck_shouldReturnCreatedDeck() throws Exception {
        CreateDeckDTO dto = buildCreateDeckDTO("New Deck");
        DeckDTO created = buildDeckDTO("New Deck");
        when(deckService.createDeck(any(CreateDeckDTO.class), eq("test-player-123"))).thenReturn(created);

        mockMvc.perform(post("/api/decks")
                        .requestAttr("playerId", "test-player-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Deck"));
    }

    @Test
    void createDeck_shouldReturn400WhenValidationFails() throws Exception {
        CreateDeckDTO dto = buildCreateDeckDTO("Bad Deck");
        when(deckService.createDeck(any(CreateDeckDTO.class), eq("test-player-123")))
                .thenThrow(new DeckValidationException("Deck validation failed: Deck must have exactly 60 cards"));

        mockMvc.perform(post("/api/decks")
                        .requestAttr("playerId", "test-player-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDeck_shouldReturnUpdatedDeck() throws Exception {
        CreateDeckDTO dto = buildCreateDeckDTO("Updated Deck");
        DeckDTO updated = buildDeckDTO("Updated Deck");
        when(deckService.updateDeck(eq(deckId), any(CreateDeckDTO.class), eq("test-player-123"))).thenReturn(updated);

        mockMvc.perform(put("/api/decks/{id}", deckId)
                        .requestAttr("playerId", "test-player-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Deck"));
    }

    @Test
    void deleteDeck_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/decks/{id}", deckId)
                        .requestAttr("playerId", "test-player-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void validateDeck_shouldReturnValidationResult() throws Exception {
        DeckValidationResultDTO result = DeckValidationResultDTO.builder()
                .isValid(true)
                .errors(List.of())
                .totalCards(60)
                .basicPokemonCount(4)
                .build();
        when(deckService.validateDeckById(eq(deckId), eq("test-player-123"))).thenReturn(result);

        mockMvc.perform(post("/api/decks/{id}/validate", deckId)
                        .requestAttr("playerId", "test-player-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.totalCards").value(60));
    }

    private DeckDTO buildDeckDTO(String name) {
        DeckDTO dto = new DeckDTO();
        dto.setId(deckId.toString());
        dto.setName(name);
        dto.setCreatorId("test-player-123");
        dto.setTotalCards(60);
        dto.setIsValid(true);
        dto.setValidationErrors(List.of());
        dto.setCards(List.of());
        return dto;
    }

    private CreateDeckDTO buildCreateDeckDTO(String name) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(name);
        DeckCardEntryDTO entry = new DeckCardEntryDTO();
        entry.setCardId(cardId.toString());
        entry.setQuantity(60);
        dto.setCards(List.of(entry));
        return dto;
    }

    private static String asJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
