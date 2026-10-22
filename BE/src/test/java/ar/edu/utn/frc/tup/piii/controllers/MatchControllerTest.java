package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MatchControllerTest {

    private MockMvc mockMvc;
    private MatchService matchService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        MatchController controller = new MatchController(matchService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void listMatches_ok() throws Exception {
        MatchDTO m = new MatchDTO();
        m.setId(UUID.randomUUID());
        when(matchService.listAvailableMatches("player-1")).thenReturn(List.of(m));

        mockMvc.perform(get("/api/matches").requestAttr("playerId", "player-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createMatch_created() throws Exception {
        CreateMatchDTO dto = new CreateMatchDTO();
        dto.setPlayer1DeckId(UUID.randomUUID().toString());

        MatchDTO created = new MatchDTO();
        created.setId(UUID.randomUUID());
        when(matchService.createMatch(eq("player-1"), any(CreateMatchDTO.class))).thenReturn(created);

        mockMvc.perform(post("/api/matches")
                        .requestAttr("playerId", "player-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/matches/" + created.getId().toString()));
    }

    @Test
    void joinMatch_ok() throws Exception {
        UUID id = UUID.randomUUID();
        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(UUID.randomUUID().toString());
        MatchDTO updated = new MatchDTO();
        updated.setId(id);
        when(matchService.joinMatch(eq("player-2"), eq(id), any(JoinMatchDTO.class))).thenReturn(updated);

        mockMvc.perform(post("/api/matches/" + id + "/join")
                        .requestAttr("playerId", "player-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void leaveMatch_noContent() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(matchService).leaveMatch("player-1", id);

        mockMvc.perform(post("/api/matches/" + id + "/leave").requestAttr("playerId", "player-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getState_ok() throws Exception {
        UUID id = UUID.randomUUID();
        MatchSnapshot snapshot = new MatchSnapshot();
        when(matchService.getMatchState("player-1", id)).thenReturn(snapshot);

        mockMvc.perform(get("/api/matches/" + id + "/state")
                        .requestAttr("playerId", "player-1")
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
