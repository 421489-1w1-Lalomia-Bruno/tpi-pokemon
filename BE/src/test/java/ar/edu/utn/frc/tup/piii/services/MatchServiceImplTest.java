package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.websocket.GameWebSocketHandler;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.exceptions.DeckOwnershipException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.MatchStateException;
import ar.edu.utn.frc.tup.piii.mappers.MatchMapper;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchServiceImplTest {

    private MatchRepository matchRepository;
    private DeckRepository deckRepository;
    private MatchMapper matchMapper;
    private EntityManager entityManager;
    private GameStatePersistenceService gameStatePersistenceService;
    private GameEngine gameEngine;
    private GameWebSocketHandler gameWebSocketHandler;
    private UserRepository userRepository;
    private MatchServiceImpl matchService;

    @BeforeEach
    void setUp() {
        matchRepository = mock(MatchRepository.class);
        deckRepository = mock(DeckRepository.class);
        userRepository = mock(UserRepository.class);
        matchMapper = new MatchMapper(new org.modelmapper.ModelMapper(), userRepository);
        entityManager = mock(EntityManager.class);
        gameStatePersistenceService = mock(GameStatePersistenceService.class);
        gameEngine = mock(GameEngine.class);
        gameWebSocketHandler = mock(GameWebSocketHandler.class);
        matchService = new MatchServiceImpl(matchRepository, deckRepository, matchMapper, gameStatePersistenceService, entityManager, gameEngine, gameWebSocketHandler);
    }

    @Test
    void createMatch_success() {
        String playerId = "player-1";
        UUID deckId = UUID.randomUUID();
        CreateMatchDTO dto = new CreateMatchDTO();
        dto.setPlayer1DeckId(deckId.toString());

        DeckEntity deck = new DeckEntity();
        deck.setId(deckId);
        deck.setCreatorId(playerId);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(invocation -> {
            MatchEntity arg = invocation.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        doNothing().when(gameStatePersistenceService).saveSnapshot(any());

        MatchDTO result = matchService.createMatch(playerId, dto);

        assertNotNull(result);
        assertEquals(playerId, result.getPlayer1Id());
        assertEquals(deckId, result.getPlayer1DeckId());
        verify(matchRepository).save(any(MatchEntity.class));
        verify(gameStatePersistenceService).saveSnapshot(any());
    }

    @Test
    void createMatch_deckNotFound() {
        String playerId = "player-1";
        UUID deckId = UUID.randomUUID();
        CreateMatchDTO dto = new CreateMatchDTO();
        dto.setPlayer1DeckId(deckId.toString());

        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> matchService.createMatch(playerId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_success() {
        String player1 = "player-1";
        String player2 = "player-2";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(player1);
        existing.setStatus(MatchStatus.WAITING);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        // prepare deck owned by player2
        UUID deckId = UUID.randomUUID();
        var deck = new DeckEntity();
        deck.setId(deckId);
        deck.setCreatorId(player2);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(deckId.toString());

        var snapshot = new ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot();
        when(gameStatePersistenceService.loadSnapshot(matchId)).thenReturn(snapshot);
        doNothing().when(gameStatePersistenceService).saveSnapshot(any());

        // perform join
        MatchDTO result = matchService.joinMatch(player2, matchId, dto);

        assertNotNull(result);
        assertEquals(player1, result.getPlayer1Id());
        assertEquals(player2, result.getPlayer2Id());
        verify(matchRepository).save(any(MatchEntity.class));
    }

    @Test
    void listOpenMatches_returnsList() {
        MatchEntity m1 = new MatchEntity();
        m1.setId(UUID.randomUUID());
        m1.setStatus(MatchStatus.WAITING);
        MatchEntity m2 = new MatchEntity();
        m2.setId(UUID.randomUUID());
        m2.setStatus(MatchStatus.WAITING);

        when(matchRepository.findAvailableMatches(MatchStatus.WAITING)).thenReturn(java.util.List.of(m1, m2));

        var list = matchService.listAvailableMatches("player-1");

        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    void getMatch_existing() {
        UUID id = UUID.randomUUID();
        MatchEntity m = new MatchEntity();
        m.setId(id);
        when(matchRepository.findById(id)).thenReturn(Optional.of(m));
        var dto = matchService.getMatch("player-1", id);
        assertNotNull(dto);
        assertEquals(id, dto.getId());
    }

    @Test
    void leaveMatch_setsWinnerIfOtherPresent() {
        UUID id = UUID.randomUUID();
        String player1 = "p1";
        String player2 = "p2";
        MatchEntity m = new MatchEntity();
        m.setId(id);
        m.setPlayer1Id(player1);
        m.setPlayer2Id(player2);
        m.setStatus(MatchStatus.ACTIVE);

        when(matchRepository.findById(id)).thenReturn(Optional.of(m));
        when(matchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var snapshot = new ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot();
        snapshot.setMatchId(id);
        when(gameStatePersistenceService.loadSnapshot(id)).thenReturn(snapshot);
        doNothing().when(gameStatePersistenceService).saveSnapshot(any());

        matchService.leaveMatch(player1, id);

        verify(matchRepository).save(any(MatchEntity.class));
        assertEquals(player2, m.getWinnerId());
    }

    @Test
    void getState_delegatesToPersistenceAndSanitize() {
        UUID id = UUID.randomUUID();
        MatchEntity m = new MatchEntity();
        m.setId(id);
        m.setPlayer1Id("player-1");
        when(matchRepository.findById(id)).thenReturn(Optional.of(m));

        var snapshot = new ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot();
        when(gameStatePersistenceService.loadSnapshot(id)).thenReturn(snapshot);
        when(gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, "player-1")).thenReturn(snapshot);

        var res = matchService.getMatchState("player-1", id);
        assertNotNull(res);
        verify(gameStatePersistenceService).loadSnapshot(id);
        verify(gameStatePersistenceService).sanitizeSnapshotForPlayer(snapshot, "player-1");
    }

    @Test
    void createMatch_deckOwnershipException() {
        String playerId = "player-1";
        UUID deckId = UUID.randomUUID();
        CreateMatchDTO dto = new CreateMatchDTO();
        dto.setPlayer1DeckId(deckId.toString());

        DeckEntity deck = new DeckEntity();
        deck.setId(deckId);
        deck.setCreatorId("other-player");

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        assertThrows(DeckOwnershipException.class, () -> matchService.createMatch(playerId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_matchNotWaiting() {
        String player1 = "player-1";
        String player2 = "player-2";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(player1);
        existing.setStatus(MatchStatus.ACTIVE);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(UUID.randomUUID().toString());

        assertThrows(MatchStateException.class, () -> matchService.joinMatch(player2, matchId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_ownMatch() {
        String playerId = "player-1";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(playerId);
        existing.setStatus(MatchStatus.WAITING);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(UUID.randomUUID().toString());

        assertThrows(MatchStateException.class, () -> matchService.joinMatch(playerId, matchId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_deckNotFound() {
        String player1 = "player-1";
        String player2 = "player-2";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(player1);
        existing.setStatus(MatchStatus.WAITING);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));

        UUID deckId = UUID.randomUUID();
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(deckId.toString());

        assertThrows(EntityNotFoundException.class, () -> matchService.joinMatch(player2, matchId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_deckOwnershipException() {
        String player1 = "player-1";
        String player2 = "player-2";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(player1);
        existing.setStatus(MatchStatus.WAITING);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));

        UUID deckId = UUID.randomUUID();
        DeckEntity deck = new DeckEntity();
        deck.setId(deckId);
        deck.setCreatorId("other-player");
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(deckId.toString());

        assertThrows(DeckOwnershipException.class, () -> matchService.joinMatch(player2, matchId, dto));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void joinMatch_player2DeckIdSet() {
        String player1 = "player-1";
        String player2 = "player-2";
        UUID matchId = UUID.randomUUID();

        MatchEntity existing = new MatchEntity();
        existing.setId(matchId);
        existing.setPlayer1Id(player1);
        existing.setStatus(MatchStatus.WAITING);

        when(matchRepository.findByIdForUpdate(matchId)).thenReturn(Optional.of(existing));
        when(matchRepository.save(any(MatchEntity.class))).thenAnswer(i -> i.getArgument(0));

        UUID deckId = UUID.randomUUID();
        DeckEntity deck = new DeckEntity();
        deck.setId(deckId);
        deck.setCreatorId(player2);
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        JoinMatchDTO dto = new JoinMatchDTO();
        dto.setDeckId(deckId.toString());

        var snapshot = new ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot();
        when(gameStatePersistenceService.loadSnapshot(matchId)).thenReturn(snapshot);
        doNothing().when(gameStatePersistenceService).saveSnapshot(any());

        matchService.joinMatch(player2, matchId, dto);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        MatchEntity saved = captor.getValue();
        assertEquals(player2, saved.getPlayer2Id());
        assertEquals(deckId, saved.getPlayer2DeckId());
    }

    @Test
    void leaveMatch_playerNotParticipant() {
        UUID matchId = UUID.randomUUID();
        MatchEntity m = new MatchEntity();
        m.setId(matchId);
        m.setPlayer1Id("player-1");
        m.setPlayer2Id("player-2");

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(MatchStateException.class, () -> matchService.leaveMatch("player-3", matchId));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void leaveMatch_player1Leaves_player2Null() {
        UUID matchId = UUID.randomUUID();
        String player1 = "player-1";
        MatchEntity m = new MatchEntity();
        m.setId(matchId);
        m.setPlayer1Id(player1);
        m.setPlayer2Id(null);
        m.setStatus(MatchStatus.WAITING);

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(m));
        when(matchRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var snapshot = new ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot();
        snapshot.setMatchId(matchId);
        when(gameStatePersistenceService.loadSnapshot(matchId)).thenReturn(snapshot);
        doNothing().when(gameStatePersistenceService).saveSnapshot(any());

        matchService.leaveMatch(player1, matchId);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        MatchEntity saved = captor.getValue();
        assertEquals(MatchStatus.ABANDONED, saved.getStatus());
        assertNull(saved.getWinnerId());
    }

    @Test
    void getMatch_notFound() {
        UUID matchId = UUID.randomUUID();
        when(matchRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> matchService.getMatch("player-1", matchId));
    }

    @Test
    void getMatchState_playerNotParticipant() {
        UUID matchId = UUID.randomUUID();
        MatchEntity m = new MatchEntity();
        m.setId(matchId);
        m.setPlayer1Id("player-1");

        when(matchRepository.findById(matchId)).thenReturn(Optional.of(m));

        assertThrows(MatchStateException.class, () -> matchService.getMatchState("player-2", matchId));
        verify(gameStatePersistenceService, never()).loadSnapshot(any());
    }
}
