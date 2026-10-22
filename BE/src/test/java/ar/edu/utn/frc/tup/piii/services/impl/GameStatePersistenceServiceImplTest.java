package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.GameCard;
import ar.edu.utn.frc.tup.piii.engine.models.HiddenCard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.engine.models.TurnContext;
import ar.edu.utn.frc.tup.piii.entities.GameStateEntity;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.repositories.GameStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStatePersistenceServiceImplTest {

    @Mock
    private GameStateRepository gameStateRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GameStatePersistenceServiceImpl gameStatePersistenceService;

    private final UUID matchId = UUID.randomUUID();
    private final String playerId = "player1";
    private final String serializedJson = "{\"matchId\":\"" + matchId + "\"}";

    @Test
    void loadSnapshot_entityPresentAndValidJson_returnsMatchSnapshot() throws Exception {
        MatchSnapshot expected = new MatchSnapshot();
        expected.setMatchId(matchId);
        GameStateEntity entity = GameStateEntity.builder()
                .matchId(matchId)
                .boardStateJson(serializedJson)
                .build();
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.of(entity));
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(expected);

        MatchSnapshot result = gameStatePersistenceService.loadSnapshot(matchId);

        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        verify(gameStateRepository).findById(matchId);
    }

    @Test
    void loadSnapshot_entityPresentAndInvalidJson_throwsRuntimeException() throws Exception {
        String badJson = "not-valid-json";
        GameStateEntity entity = GameStateEntity.builder()
                .matchId(matchId)
                .boardStateJson(badJson)
                .build();
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.of(entity));
        when(objectMapper.readValue(badJson, MatchSnapshot.class))
                .thenThrow(new RuntimeException("Deserialization error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameStatePersistenceService.loadSnapshot(matchId));
        assertTrue(ex.getMessage().contains("Failed to deserialize"));
    }

    @Test
    void loadSnapshot_entityNotPresent_returnsFallbackWithMatchId() {
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        MatchSnapshot result = gameStatePersistenceService.loadSnapshot(matchId);

        assertNotNull(result);
        assertEquals(matchId, result.getMatchId());
        assertNull(result.getStatus());
        assertNull(result.getBoard());
    }

    @Test
    void saveSnapshot_serializationOkAndEntityExists_updatesExistingEntity() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setCurrentPlayerId("currentP1");
        snapshot.setStatus(MatchStatus.ACTIVE);
        GameStateEntity existingEntity = GameStateEntity.builder()
                .matchId(matchId)
                .boardStateJson("old-state")
                .build();
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.of(existingEntity));

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        GameStateEntity saved = captor.getValue();
        assertEquals(matchId, saved.getMatchId());
        assertEquals(serializedJson, saved.getBoardStateJson());
        assertEquals("currentP1", saved.getCurrentTurnId());
        assertEquals(GamePhase.ACTIVE, saved.getGamePhase());
        assertNull(saved.getTurnPhase());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveSnapshot_serializationOkAndEntityNotExists_createsNewEntity() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setCurrentPlayerId("currentP1");
        snapshot.setStatus(MatchStatus.SETUP);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        GameStateEntity saved = captor.getValue();
        assertEquals(matchId, saved.getMatchId());
        assertEquals(serializedJson, saved.getBoardStateJson());
        assertEquals("currentP1", saved.getCurrentTurnId());
        assertEquals(GamePhase.SETUP, saved.getGamePhase());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveSnapshot_serializationFails_throwsRuntimeException() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        when(objectMapper.writeValueAsString(snapshot))
                .thenThrow(new RuntimeException("Serialization error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameStatePersistenceService.saveSnapshot(snapshot));
        assertTrue(ex.getMessage().contains("Failed to persist snapshot"));
        verify(gameStateRepository, never()).save(any());
    }

    @Test
    void saveSnapshot_turnContextNull_doesNotSetTurnPhase() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        snapshot.setTurnContext(null);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertNull(captor.getValue().getTurnPhase());
    }

    @Test
    void saveSnapshot_turnContextWithPhase_setsTurnPhase() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        TurnContext turnContext = new TurnContext();
        turnContext.setPhase(TurnPhase.ATTACK);
        snapshot.setTurnContext(turnContext);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(TurnPhase.ATTACK, captor.getValue().getTurnPhase());
    }

    @Test
    void saveSnapshot_turnContextNotNullButPhaseNull_doesNotSetTurnPhase() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        TurnContext turnContext = new TurnContext();
        turnContext.setPhase(null);
        snapshot.setTurnContext(turnContext);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertNull(captor.getValue().getTurnPhase());
    }

    @Test
    void saveSnapshot_statusNull_defaultsToGamePhaseWaiting() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(null);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.WAITING, captor.getValue().getGamePhase());
    }

    @Test
    void saveSnapshot_statusWaiting_mapsToGamePhaseWaiting() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.WAITING);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());
        when(gameStateRepository.save(any())).thenReturn(null);

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.WAITING, captor.getValue().getGamePhase());
    }

    @Test
    void saveSnapshot_statusSetup_mapsToGamePhaseSetup() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.SETUP);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());
        when(gameStateRepository.save(any())).thenReturn(null);

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.SETUP, captor.getValue().getGamePhase());
    }

    @Test
    void saveSnapshot_statusActive_mapsToGamePhaseActive() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());
        when(gameStateRepository.save(any())).thenReturn(null);

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.ACTIVE, captor.getValue().getGamePhase());
    }

    @Test
    void saveSnapshot_statusFinished_mapsToGamePhaseFinished() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.FINISHED);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());
        when(gameStateRepository.save(any())).thenReturn(null);

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.FINISHED, captor.getValue().getGamePhase());
    }

    @Test
    void saveSnapshot_statusAbandoned_mapsToGamePhaseFinished() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ABANDONED);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(gameStateRepository.findById(matchId)).thenReturn(Optional.empty());
        when(gameStateRepository.save(any())).thenReturn(null);

        gameStatePersistenceService.saveSnapshot(snapshot);

        ArgumentCaptor<GameStateEntity> captor = ArgumentCaptor.forClass(GameStateEntity.class);
        verify(gameStateRepository).save(captor.capture());
        assertEquals(GamePhase.FINISHED, captor.getValue().getGamePhase());
    }

    @Test
    void sanitizeSnapshotForPlayer_nullSnapshot_returnsNewMatchSnapshot() {
        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(null, playerId);

        assertNotNull(result);
        assertNull(result.getMatchId());
    }

    @Test
    void sanitizeSnapshotForPlayer_snapshotWithNullBoard_returnsSnapshotAsIs() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        snapshot.setBoard(null);
        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(snapshot);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        assertEquals(matchId, result.getMatchId());
        assertNull(result.getBoard());
    }

    @Test
    void sanitizeSnapshotForPlayer_boardWithP1NotPlayer_sanitizesP1Board() throws Exception {
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId("otherPlayer");
        p1Board.getHand().add(mock(GameCard.class));
        p1Board.getDeck().add(mock(GameCard.class));
        p1Board.getDeck().add(mock(GameCard.class));
        p1Board.getPrizeCards().add(mock(GameCard.class));
        p1Board.getPrizeCards().add(mock(GameCard.class));
        p1Board.getPrizeCards().add(mock(GameCard.class));

        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(playerId);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setBoard(board);

        PlayerBoard copyP1 = new PlayerBoard();
        copyP1.setPlayerId("otherPlayer");
        copyP1.getHand().add(mock(GameCard.class));
        copyP1.getDeck().add(mock(GameCard.class));
        copyP1.getDeck().add(mock(GameCard.class));
        copyP1.getPrizeCards().add(mock(GameCard.class));
        copyP1.getPrizeCards().add(mock(GameCard.class));
        copyP1.getPrizeCards().add(mock(GameCard.class));

        PlayerBoard copyP2 = new PlayerBoard();
        copyP2.setPlayerId(playerId);

        GameBoard copyBoard = new GameBoard();
        copyBoard.setPlayer1Board(copyP1);
        copyBoard.setPlayer2Board(copyP2);

        MatchSnapshot copy = new MatchSnapshot();
        copy.setBoard(copyBoard);

        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(copy);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        GameBoard resultBoard = result.getBoard();
        assertNotNull(resultBoard);

        PlayerBoard resultP1 = resultBoard.getPlayer1Board();
        assertEquals(1, resultP1.getHand().size());
        assertInstanceOf(HiddenCard.class, resultP1.getHand().get(0));
        assertEquals(2, resultP1.getDeck().size());
        assertInstanceOf(HiddenCard.class, resultP1.getDeck().get(0));
        assertInstanceOf(HiddenCard.class, resultP1.getDeck().get(1));
        assertEquals(3, resultP1.getPrizeCards().size());
        assertInstanceOf(HiddenCard.class, resultP1.getPrizeCards().get(0));
        assertInstanceOf(HiddenCard.class, resultP1.getPrizeCards().get(1));
        assertInstanceOf(HiddenCard.class, resultP1.getPrizeCards().get(2));

        PlayerBoard resultP2 = resultBoard.getPlayer2Board();
        assertEquals(0, resultP2.getHand().size());
        assertEquals(0, resultP2.getDeck().size());
        assertEquals(0, resultP2.getPrizeCards().size());
    }

    @Test
    void sanitizeSnapshotForPlayer_boardWithP2NotPlayer_sanitizesP2Board() throws Exception {
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(playerId);

        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId("otherPlayer");
        p2Board.getHand().add(mock(GameCard.class));
        p2Board.getDeck().add(mock(GameCard.class));
        p2Board.getPrizeCards().add(mock(GameCard.class));

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setBoard(board);

        PlayerBoard copyP1 = new PlayerBoard();
        copyP1.setPlayerId(playerId);

        PlayerBoard copyP2 = new PlayerBoard();
        copyP2.setPlayerId("otherPlayer");
        copyP2.getHand().add(mock(GameCard.class));
        copyP2.getDeck().add(mock(GameCard.class));
        copyP2.getPrizeCards().add(mock(GameCard.class));

        GameBoard copyBoard = new GameBoard();
        copyBoard.setPlayer1Board(copyP1);
        copyBoard.setPlayer2Board(copyP2);

        MatchSnapshot copy = new MatchSnapshot();
        copy.setBoard(copyBoard);

        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(copy);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        GameBoard resultBoard = result.getBoard();
        assertNotNull(resultBoard);

        PlayerBoard resultP2 = resultBoard.getPlayer2Board();
        assertEquals(1, resultP2.getHand().size());
        assertInstanceOf(HiddenCard.class, resultP2.getHand().get(0));
        assertEquals(1, resultP2.getDeck().size());
        assertInstanceOf(HiddenCard.class, resultP2.getDeck().get(0));
        assertEquals(1, resultP2.getPrizeCards().size());
        assertInstanceOf(HiddenCard.class, resultP2.getPrizeCards().get(0));

        PlayerBoard resultP1 = resultBoard.getPlayer1Board();
        assertEquals(0, resultP1.getHand().size());
    }

    @Test
    void sanitizeSnapshotForPlayer_bothBoardsNotPlayer_sanitizesBothBoards() throws Exception {
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId("otherP1");
        p1Board.getHand().add(mock(GameCard.class));

        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId("otherP2");
        p2Board.getHand().add(mock(GameCard.class));
        p2Board.getHand().add(mock(GameCard.class));

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setBoard(board);

        PlayerBoard copyP1 = new PlayerBoard();
        copyP1.setPlayerId("otherP1");
        copyP1.getHand().add(mock(GameCard.class));

        PlayerBoard copyP2 = new PlayerBoard();
        copyP2.setPlayerId("otherP2");
        copyP2.getHand().add(mock(GameCard.class));
        copyP2.getHand().add(mock(GameCard.class));

        GameBoard copyBoard = new GameBoard();
        copyBoard.setPlayer1Board(copyP1);
        copyBoard.setPlayer2Board(copyP2);

        MatchSnapshot copy = new MatchSnapshot();
        copy.setBoard(copyBoard);

        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(copy);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        GameBoard resultBoard = result.getBoard();
        assertInstanceOf(HiddenCard.class, resultBoard.getPlayer1Board().getHand().get(0));
        assertInstanceOf(HiddenCard.class, resultBoard.getPlayer2Board().getHand().get(0));
        assertInstanceOf(HiddenCard.class, resultBoard.getPlayer2Board().getHand().get(1));
    }

    @Test
    void sanitizeSnapshotForPlayer_bothBoardsBelongToPlayer_noSanitization() throws Exception {
        PlayerBoard p1Board = new PlayerBoard();
        p1Board.setPlayerId(playerId);
        p1Board.getHand().add(mock(GameCard.class));

        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId(playerId);
        p2Board.getHand().add(mock(GameCard.class));

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setBoard(board);

        PlayerBoard copyP1 = new PlayerBoard();
        copyP1.setPlayerId(playerId);
        copyP1.getHand().add(mock(GameCard.class));

        PlayerBoard copyP2 = new PlayerBoard();
        copyP2.setPlayerId(playerId);
        copyP2.getHand().add(mock(GameCard.class));

        GameBoard copyBoard = new GameBoard();
        copyBoard.setPlayer1Board(copyP1);
        copyBoard.setPlayer2Board(copyP2);

        MatchSnapshot copy = new MatchSnapshot();
        copy.setBoard(copyBoard);

        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(copy);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        GameBoard resultBoard = result.getBoard();
        assertNotNull(resultBoard.getPlayer1Board().getHand().get(0));
        assertNotNull(resultBoard.getPlayer2Board().getHand().get(0));
    }

    @Test
    void sanitizeSnapshotForPlayer_jsonProcessingFails_returnsOriginalSnapshot() throws Exception {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(matchId);
        snapshot.setStatus(MatchStatus.ACTIVE);
        when(objectMapper.writeValueAsString(snapshot))
                .thenThrow(new RuntimeException("JSON write error"));

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        assertSame(snapshot, result);
    }

    @Test
    void sanitizeSnapshotForPlayer_boardPresentAndP1Null_doesNotThrow() throws Exception {
        PlayerBoard p2Board = new PlayerBoard();
        p2Board.setPlayerId("otherPlayer");
        p2Board.getHand().add(mock(GameCard.class));

        GameBoard board = new GameBoard();
        board.setPlayer1Board(null);
        board.setPlayer2Board(p2Board);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setBoard(board);

        PlayerBoard copyP2 = new PlayerBoard();
        copyP2.setPlayerId("otherPlayer");
        copyP2.getHand().add(mock(GameCard.class));

        GameBoard copyBoard = new GameBoard();
        copyBoard.setPlayer1Board(null);
        copyBoard.setPlayer2Board(copyP2);

        MatchSnapshot copy = new MatchSnapshot();
        copy.setBoard(copyBoard);

        when(objectMapper.writeValueAsString(snapshot)).thenReturn(serializedJson);
        when(objectMapper.readValue(serializedJson, MatchSnapshot.class)).thenReturn(copy);

        MatchSnapshot result = gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);

        assertNotNull(result);
        assertNull(result.getBoard().getPlayer1Board());
        assertEquals(1, result.getBoard().getPlayer2Board().getHand().size());
        assertInstanceOf(HiddenCard.class, result.getBoard().getPlayer2Board().getHand().get(0));
    }
}
