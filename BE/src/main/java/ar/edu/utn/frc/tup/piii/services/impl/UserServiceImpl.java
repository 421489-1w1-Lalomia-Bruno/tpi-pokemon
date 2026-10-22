package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserProfileDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.exceptions.DuplicateUsernameException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.mappers.UserMapper;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DeckService deckService;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           DeckService deckService,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.deckService = deckService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUsernameException("Username already taken");
        }

        UserEntity entity = UserEntity.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .gender(request.getGender())
                .build();

        try {
            entity = userRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUsernameException("Username already taken");
        }

        String token = jwtTokenProvider.generateToken(entity.getId().toString());
        return userMapper.toAuthResponse(token, entity);
    }

    @Override
    public AuthResponse login(String username, String password) {
        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(password, entity.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(entity.getId().toString());
        return userMapper.toAuthResponse(token, entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String userId) {
        UserEntity entity = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        List<DeckDTO> decks = deckService.getDecksByCreator(entity.getId().toString());
        return userMapper.toProfileDTO(entity, decks);
    }
}
