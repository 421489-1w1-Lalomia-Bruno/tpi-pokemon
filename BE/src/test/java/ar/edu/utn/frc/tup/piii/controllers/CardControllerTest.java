package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncResponseDTO;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.services.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardControllerTest extends EmbeddedPostgresTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public CardService cardService() {
            return Mockito.mock(CardService.class);
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CardService cardService;

    private MockMvc mockMvc;

    private CardDTO sampleCardDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        sampleCardDTO = new CardDTO();
        sampleCardDTO.setId(UUID.randomUUID().toString());
        sampleCardDTO.setExternalId("xy1-1");
        sampleCardDTO.setName("Bulbasaur");
        sampleCardDTO.setSupertype("Pokémon");
        sampleCardDTO.setHp(60);
        sampleCardDTO.setStage(PokemonStage.BASIC);
        sampleCardDTO.setSetId("xy1");
    }

    @Test
    void getAllCards_shouldReturnPageOfCards() throws Exception {
        Page<CardDTO> page = new PageImpl<>(List.of(sampleCardDTO), PageRequest.of(0, 20), 1);
        when(cardService.listCards(any(CardFilterDTO.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Bulbasaur"));
    }

    @Test
    void getCardById_shouldReturnCardWhenExists() throws Exception {
        UUID uuid = UUID.fromString(sampleCardDTO.getId());
        when(cardService.getCardById(uuid)).thenReturn(sampleCardDTO);

        mockMvc.perform(get("/api/cards/{id}", uuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bulbasaur"))
                .andExpect(jsonPath("$.id").value(uuid.toString()));
    }

    @Test
    void getCardById_shouldReturn404WhenNotFound() throws Exception {
        UUID invalidId = UUID.randomUUID();
        when(cardService.getCardById(invalidId)).thenThrow(new EntityNotFoundException("Card not found with id: " + invalidId));

        mockMvc.perform(get("/api/cards/{id}", invalidId))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchCards_shouldReturnListOfCards() throws Exception {
        when(cardService.searchCards("Bulb")).thenReturn(List.of(sampleCardDTO));

        mockMvc.perform(get("/api/cards/search")
                        .param("name", "Bulb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Bulbasaur"));
    }

    @Test
    void syncCards_shouldReturnSyncedCount() throws Exception {
        when(cardService.syncCards(any(SyncRequestDTO.class))).thenReturn(new SyncResponseDTO(146));

        mockMvc.perform(post("/api/cards/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setId\":\"xy1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(146));
    }

    @Test
    void syncCards_shouldDefaultToXy1() throws Exception {
        when(cardService.syncCards(any(SyncRequestDTO.class))).thenReturn(new SyncResponseDTO(146));

        mockMvc.perform(post("/api/cards/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.synced").value(146));
    }
}
