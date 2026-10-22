package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.auth.UserProfileDTO;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private HttpServletRequest request;

    private final String playerId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        request = mock(HttpServletRequest.class);
        when(request.getAttribute("playerId")).thenReturn(playerId);
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getProfile_shouldReturnUserProfile() throws Exception {
        UserProfileDTO profile = buildProfile();

        when(userService.getProfile(playerId)).thenReturn(profile);

        mockMvc.perform(get("/api/users/me")
                        .requestAttr("playerId", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerId))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.decks").isArray());
    }

    @Test
    void getProfile_shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getProfile(playerId))
                .thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/me")
                        .requestAttr("playerId", playerId))
                .andExpect(status().isNotFound());
    }

    private UserProfileDTO buildProfile() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(playerId);
        dto.setUsername("testuser");
        dto.setGender(UserGender.MALE);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setDecks(List.of());
        return dto;
    }
}
