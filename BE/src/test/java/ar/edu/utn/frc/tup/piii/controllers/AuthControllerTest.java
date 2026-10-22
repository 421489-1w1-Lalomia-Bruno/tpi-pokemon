package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.auth.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserDTO;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
import ar.edu.utn.frc.tup.piii.exceptions.DuplicateUsernameException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends EmbeddedPostgresTestBase {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_validCredentials_returnsAuthResponse() throws Exception {
        AuthResponse response = buildAuthResponse();
        when(userService.login("testuser", "password123")).thenReturn(response);

        LoginRequest request = new LoginRequest("testuser", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        when(userService.login("wrong", "wrong"))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        LoginRequest request = new LoginRequest("wrong", "wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_newUser_returnsAuthResponse() throws Exception {
        AuthResponse response = buildAuthResponse();
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        RegisterRequest request = new RegisterRequest("testuser", "password123", UserGender.MALE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    void register_duplicateUsername_returnsConflict() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateUsernameException("Username already taken"));

        RegisterRequest request = new RegisterRequest("existing", "password123", UserGender.MALE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    private AuthResponse buildAuthResponse() {
        UserDTO userDTO = new UserDTO(UUID.randomUUID().toString(), "testuser", UserGender.MALE, LocalDateTime.now(), 0, 0);
        return new AuthResponse("jwt-token", userDTO);
    }
}
