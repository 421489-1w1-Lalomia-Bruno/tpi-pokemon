package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.engine.models.GameBoard;
import ar.edu.utn.frc.tup.piii.engine.models.HiddenCard;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.engine.models.PlayerBoard;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.entities.GameStateEntity;
import ar.edu.utn.frc.tup.piii.repositories.GameStateRepository;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class GameStatePersistenceServiceImpl implements GameStatePersistenceService {

    private final GameStateRepository gameStateRepository;
    private final ObjectMapper objectMapper;

    public GameStatePersistenceServiceImpl(GameStateRepository gameStateRepository,
                                           ObjectMapper objectMapper) {
        this.gameStateRepository = gameStateRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public MatchSnapshot loadSnapshot(UUID matchId) {
        return gameStateRepository.findById(matchId)
                .map(entity -> {
                    try {
                        return objectMapper.readValue(entity.getBoardStateJson(), MatchSnapshot.class);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialize snapshot for match " + matchId, e);
                    }
                })
                .orElseGet(() -> {
                    MatchSnapshot fallback = new MatchSnapshot();
                    fallback.setMatchId(matchId);
                    return fallback;
                });
    }

    @Override
    public MatchSnapshot sanitizeSnapshotForPlayer(MatchSnapshot snapshot, String playerId) {
        if (snapshot == null) {
            return new MatchSnapshot();
        }
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            MatchSnapshot sanitized = objectMapper.readValue(json, MatchSnapshot.class);
            GameBoard board = sanitized.getBoard();
            if (board != null) {
                if (board.getPlayer1Board() != null && !playerId.equals(board.getPlayer1Board().getPlayerId())) {
                    sanitizePlayerBoard(board.getPlayer1Board());
                }
                if (board.getPlayer2Board() != null && !playerId.equals(board.getPlayer2Board().getPlayerId())) {
                    sanitizePlayerBoard(board.getPlayer2Board());
                }
            }
            return sanitized;
        } catch (Exception e) {
            return snapshot;
        }
    }

    private void sanitizePlayerBoard(PlayerBoard board) {
        int handSize = board.getHand().size();
        board.getHand().clear();
        for (int i = 0; i < handSize; i++) {
            board.getHand().add(new HiddenCard("hidden-hand-" + i));
        }
        int deckSize = board.getDeck().size();
        board.getDeck().clear();
        for (int i = 0; i < deckSize; i++) {
            board.getDeck().add(new HiddenCard("hidden-deck-" + i));
        }
        int prizeSize = board.getPrizeCards().size();
        board.getPrizeCards().clear();
        for (int i = 0; i < prizeSize; i++) {
            board.getPrizeCards().add(new HiddenCard("hidden-prize-" + i));
        }
        board.setRevealedCards(null);
    }

    @Override
    public void saveSnapshot(MatchSnapshot snapshot) {
        try {
            String json = objectMapper.writeValueAsString(snapshot);
            GameStateEntity entity = gameStateRepository.findById(snapshot.getMatchId())
                    .orElse(GameStateEntity.builder()
                            .matchId(snapshot.getMatchId())
                            .build());
            entity.setBoardStateJson(json);
            entity.setCurrentTurnId(snapshot.getCurrentPlayerId());
            entity.setGamePhase(snapshot.getStatus() != null
                ? switch (snapshot.getStatus()) {
                    case WAITING -> GamePhase.WAITING;
                    case SETUP -> GamePhase.SETUP;
                    case ACTIVE -> GamePhase.ACTIVE;
                    case FINISHED, ABANDONED -> GamePhase.FINISHED;
                }
                : GamePhase.WAITING);
            if (snapshot.getTurnContext() != null && snapshot.getTurnContext().getPhase() != null) {
                entity.setTurnPhase(snapshot.getTurnContext().getPhase());
            }
            entity.setUpdatedAt(LocalDateTime.now());
            gameStateRepository.save(entity);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist snapshot for match " + snapshot.getMatchId(), e);
        }
    }
}
