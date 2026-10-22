package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.DeckOwnershipException;
import ar.edu.utn.frc.tup.piii.exceptions.MatchStateException;
import ar.edu.utn.frc.tup.piii.mappers.MatchMapper;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import jakarta.persistence.EntityManager;
import ar.edu.utn.frc.tup.piii.engine.models.MatchSnapshot;
import ar.edu.utn.frc.tup.piii.websocket.GameWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchRepository matchRepository;
    private final DeckRepository deckRepository;
    private final MatchMapper matchMapper;
    private final GameStatePersistenceService gameStatePersistenceService;
    private final EntityManager entityManager;
    private final GameEngine gameEngine;
    private final GameWebSocketHandler gameWebSocketHandler;
    private static final Logger log = LoggerFactory.getLogger(MatchServiceImpl.class);

    public MatchServiceImpl(MatchRepository matchRepository,
                            DeckRepository deckRepository,
                            MatchMapper matchMapper,
                            GameStatePersistenceService gameStatePersistenceService,
                            EntityManager entityManager,
                            GameEngine gameEngine,
                            GameWebSocketHandler gameWebSocketHandler) {
        this.matchRepository = matchRepository;
        this.deckRepository = deckRepository;
        this.matchMapper = matchMapper;
        this.gameStatePersistenceService = gameStatePersistenceService;
        this.entityManager = entityManager;
        this.gameEngine = gameEngine;
        this.gameWebSocketHandler = gameWebSocketHandler;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchDTO> listAvailableMatches(String playerId) {
        List<MatchEntity> matches = matchRepository.findAvailableMatches(ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING);
        log.info("listAvailableMatches: playerId={} found {} matches", playerId, matches.size());
        List<MatchDTO> dtos = matches.stream().map(matchMapper::toDto).toList();
        dtos.forEach(d -> log.info("listAvailableMatches match id={} p1={} p1User={} p2={} p2User={}",
                d.getId(), d.getPlayer1Id(), d.getPlayer1Username(), d.getPlayer2Id(), d.getPlayer2Username()));
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDTO getMatch(String playerId, UUID matchId) {
        MatchEntity entity = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        return matchMapper.toDto(entity);
    }

    @Override
    @Transactional
    public MatchDTO createMatch(String playerId, CreateMatchDTO dto) {
        log.info("createMatch: playerId={} deckId={}", playerId, dto.getPlayer1DeckId());
        UUID deckId = UUID.fromString(dto.getPlayer1DeckId());
        DeckEntity deck = deckRepository.findById(deckId).orElseThrow(() -> new EntityNotFoundException("Deck not found: " + deckId));
        if (deck.getSource() != DeckSource.PREDETERMINED
                && (deck.getCreatorId() == null || !deck.getCreatorId().equals(playerId))) {
            throw new DeckOwnershipException("Deck does not belong to player: " + playerId);
        }
        MatchEntity entity = matchMapper.fromCreate(playerId, deckId);
        MatchEntity saved = matchRepository.save(entity);

        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(saved.getId());
        snapshot.setPlayer1Id(playerId);
        snapshot.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING);
        gameStatePersistenceService.saveSnapshot(snapshot);

        try {
            gameEngine.initializeMatch(saved.getId().toString());
        } catch (Exception e) {
            log.warn("Failed to initialize game engine for match {}: {}", saved.getId(), e.getMessage());
        }

        return matchMapper.toDto(saved);
    }

    @Override
    @Transactional
    public MatchDTO joinMatch(String playerId, UUID matchId, JoinMatchDTO dto) {
        // Use repository findById with @Lock(PESSIMISTIC_WRITE)
        MatchEntity entity = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));

        if (entity.getStatus() != ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING) {
            throw new MatchStateException("Match is not in WAITING state: " + matchId);
        }
        if (playerId.equals(entity.getPlayer1Id())) {
            log.warn("Player {} tried to join own match (player1Id={})", playerId, entity.getPlayer1Id());
            throw new MatchStateException("Player cannot join own match");
        }
        if (entity.getPlayer2Id() != null) {
            throw new MatchStateException("Match is already full: " + matchId);
        }

        UUID deckId = UUID.fromString(dto.getDeckId());
        DeckEntity deck = deckRepository.findById(deckId).orElseThrow(() -> new EntityNotFoundException("Deck not found: " + deckId));
        if (deck.getSource() != DeckSource.PREDETERMINED
                && (deck.getCreatorId() == null || !deck.getCreatorId().equals(playerId))) {
            throw new DeckOwnershipException("Deck does not belong to player: " + playerId);
        }

        entity.setPlayer2Id(playerId);
        entity.setPlayer2DeckId(deckId);
        entity.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING);
        MatchEntity saved = matchRepository.save(entity);

        MatchSnapshot snapshot = gameStatePersistenceService.loadSnapshot(matchId);
        snapshot.setPlayer2Id(playerId);
        snapshot.setPlayer2DeckId(deckId);
        snapshot.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING);
        gameStatePersistenceService.saveSnapshot(snapshot);

        try {
            gameWebSocketHandler.broadcastToMatch(matchId.toString(), snapshot);
        } catch (Exception e) {
            log.warn("Failed to broadcast state after player {} joined match {}: {}", playerId, matchId, e.getMessage());
        }

        return matchMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void leaveMatch(String playerId, UUID matchId) {
        MatchEntity entity = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        boolean isPlayer1 = playerId.equals(entity.getPlayer1Id());
        boolean isPlayer2 = playerId.equals(entity.getPlayer2Id());
        if (!isPlayer1 && !isPlayer2) {
            throw new MatchStateException("Player is not participant of match: " + playerId);
        }
        entity.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.ABANDONED);
        if (isPlayer1 && entity.getPlayer2Id() != null) {
            entity.setWinnerId(entity.getPlayer2Id());
        } else if (isPlayer2 && entity.getPlayer1Id() != null) {
            entity.setWinnerId(entity.getPlayer1Id());
        }
        matchRepository.save(entity);

        MatchSnapshot snapshot = gameStatePersistenceService.loadSnapshot(matchId);
        snapshot.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.ABANDONED);
        snapshot.setWinnerId(entity.getWinnerId());
        gameStatePersistenceService.saveSnapshot(snapshot);
    }

    @Override
    @Transactional
    public void cancelMatch(String playerId, UUID matchId) {
        MatchEntity entity = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        if (!playerId.equals(entity.getPlayer1Id())) {
            throw new MatchStateException("Only the match creator can cancel the match");
        }
        if (entity.getStatus() != ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING) {
            throw new MatchStateException("Only waiting matches can be cancelled");
        }
        entity.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.ABANDONED);
        matchRepository.save(entity);

        MatchSnapshot snap = gameStatePersistenceService.loadSnapshot(matchId);
        snap.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.ABANDONED);
        gameStatePersistenceService.saveSnapshot(snap);
    }

    @Override
    @Transactional(readOnly = true)
    public MatchSnapshot getMatchState(String playerId, UUID matchId) {
        MatchEntity entity = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
        if (!playerId.equals(entity.getPlayer1Id()) && !playerId.equals(entity.getPlayer2Id())) {
            throw new MatchStateException("Player is not participant of match: " + playerId);
        }

        // Delegar en GameStatePersistenceService la carga y sanitización del snapshot
        MatchSnapshot snapshot = gameStatePersistenceService.loadSnapshot(matchId);
        return gameStatePersistenceService.sanitizeSnapshotForPlayer(snapshot, playerId);
    }
}
