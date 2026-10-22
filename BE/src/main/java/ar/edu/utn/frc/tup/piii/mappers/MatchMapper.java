package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MatchMapper {

    private static final Logger log = LoggerFactory.getLogger(MatchMapper.class);
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;

    public MatchMapper(ModelMapper modelMapper, UserRepository userRepository) {
        this.modelMapper = modelMapper;
        this.userRepository = userRepository;
    }

    public MatchDTO toDto(MatchEntity entity) {
        if (entity == null) return null;
        MatchDTO dto = modelMapper.map(entity, MatchDTO.class);
        // BaseEntity proporciona id y timestamps; modelMapper debe mapearlos automáticamente
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setPlayer1DeckId(entity.getPlayer1DeckId());
        dto.setPlayer2DeckId(entity.getPlayer2DeckId());
        resolveUsernames(dto, entity);
        return dto;
    }

    private void resolveUsernames(MatchDTO dto, MatchEntity entity) {
        try {
            if (entity.getPlayer1Id() != null) {
                UUID pid = UUID.fromString(entity.getPlayer1Id());
                log.info("resolveUsernames: player1Id={} matchId={}", pid, entity.getId());
                userRepository.findById(pid)
                        .ifPresentOrElse(
                            u -> {
                                dto.setPlayer1Username(u.getUsername());
                                log.info("resolveUsernames: set player1Username={} for player1Id={}", u.getUsername(), pid);
                            },
                            () -> log.warn("resolveUsernames: no user found for player1Id={}", pid)
                        );
            }
        } catch (Exception e) {
            log.warn("resolveUsernames: error for player1Id={} matchId={}: {}", entity.getPlayer1Id(), entity.getId(), e.getMessage());
        }
        try {
            if (entity.getPlayer2Id() != null) {
                UUID pid = UUID.fromString(entity.getPlayer2Id());
                log.info("resolveUsernames: player2Id={} matchId={}", pid, entity.getId());
                userRepository.findById(pid)
                        .ifPresentOrElse(
                            u -> {
                                dto.setPlayer2Username(u.getUsername());
                                log.info("resolveUsernames: set player2Username={} for player2Id={}", u.getUsername(), pid);
                            },
                            () -> log.warn("resolveUsernames: no user found for player2Id={}", pid)
                        );
            }
        } catch (Exception e) {
            log.warn("resolveUsernames: error for player2Id={} matchId={}: {}", entity.getPlayer2Id(), entity.getId(), e.getMessage());
        }
    }

    public MatchEntity fromCreate(String playerId, UUID player1DeckId) {
        MatchEntity entity = new MatchEntity();
        entity.setPlayer1Id(playerId);
        entity.setPlayer1DeckId(player1DeckId);
        entity.setStatus(ar.edu.utn.frc.tup.piii.enums.MatchStatus.WAITING);
        return entity;
    }
}
