package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserProfileDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
import ar.edu.utn.frc.tup.piii.exceptions.DuplicateUsernameException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private DeckService deckService;

    private UserMapper userMapper;
    private UserServiceImpl userService;

    private final UUID userId = UUID.randomUUID();
    private final String username = "testuser";
    private final String password = "password123";
    private final String encodedPassword = "$2a$10$encoded";
    private final String token = "jwt-token";

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserServiceImpl(userRepository, passwordEncoder, jwtTokenProvider, deckService, userMapper);
    }

    @Test
    void register_newUser_returnsAuthResponse() {
        RegisterRequest request = new RegisterRequest(username, password, UserGender.MALE);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        UserEntity savedEntity = buildUserEntity();
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(jwtTokenProvider.generateToken(userId.toString())).thenReturn(token);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertNotNull(response.getUser());
        assertEquals(username, response.getUser().getUsername());
        assertEquals(UserGender.MALE, response.getUser().getGender());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest(username, password, null);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(buildUserEntity()));

        assertThrows(DuplicateUsernameException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_dataIntegrityViolation_throwsException() {
        RegisterRequest request = new RegisterRequest(username, password, null);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(UserEntity.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DuplicateUsernameException.class, () -> userService.register(request));
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        UserEntity entity = buildUserEntity();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        when(jwtTokenProvider.generateToken(userId.toString())).thenReturn(token);

        AuthResponse response = userService.login(username, password);

        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertEquals(username, response.getUser().getUsername());
    }

    @Test
    void login_invalidUsername_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.login("unknown", password));
    }

    @Test
    void login_invalidPassword_throwsException() {
        UserEntity entity = buildUserEntity();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrong", encodedPassword)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.login(username, "wrong"));
    }

    @Test
    void getProfile_existingUser_returnsProfile() {
        UserEntity entity = buildUserEntity();
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(deckService.getDecksByCreator(userId.toString())).thenReturn(List.of());

        UserProfileDTO profile = userService.getProfile(userId.toString());

        assertNotNull(profile);
        assertEquals(userId.toString(), profile.getId());
        assertEquals(username, profile.getUsername());
        assertNotNull(profile.getDecks());
    }

    @Test
    void getProfile_nonexistentUser_throwsException() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getProfile(UUID.randomUUID().toString()));
    }

    @Test
    void register_nullUsername_throwsException() {
        RegisterRequest request = new RegisterRequest(null, password, UserGender.MALE);

        assertThrows(Exception.class, () -> userService.register(request));
    }

    @Test
    void register_emptyUsername_registerSuccess() {
        RegisterRequest request = new RegisterRequest("", password, UserGender.MALE);
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        UserEntity savedEntity = buildUserEntity();
        savedEntity.setUsername("");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(jwtTokenProvider.generateToken(userId.toString())).thenReturn(token);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(token, response.getToken());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_nullGender_registerSuccess() {
        RegisterRequest request = new RegisterRequest(username, password, null);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        UserEntity savedEntity = buildUserEntity();
        savedEntity.setGender(null);
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(jwtTokenProvider.generateToken(userId.toString())).thenReturn(token);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(token, response.getToken());
        assertNull(response.getUser().getGender());
    }

    @Test
    void getProfile_withDecks_returnsProfileWithDecks() {
        UserEntity entity = buildUserEntity();
        DeckDTO deck1 = new DeckDTO();
        deck1.setId("deck-1");
        DeckDTO deck2 = new DeckDTO();
        deck2.setId("deck-2");
        List<DeckDTO> decks = List.of(deck1, deck2);
        when(userRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(deckService.getDecksByCreator(userId.toString())).thenReturn(decks);

        UserProfileDTO profile = userService.getProfile(userId.toString());

        assertNotNull(profile);
        assertEquals(userId.toString(), profile.getId());
        assertEquals(2, profile.getDecks().size());
        assertEquals("deck-1", profile.getDecks().get(0).getId());
        assertEquals("deck-2", profile.getDecks().get(1).getId());
    }

    @Test
    void getProfile_invalidUUIDFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.getProfile("not-a-uuid"));
    }

    private UserEntity buildUserEntity() {
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername(username);
        entity.setPasswordHash(encodedPassword);
        entity.setGender(UserGender.MALE);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
