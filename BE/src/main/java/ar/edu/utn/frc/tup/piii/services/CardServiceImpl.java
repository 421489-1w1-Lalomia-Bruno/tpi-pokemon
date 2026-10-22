package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient.PokemonTcgResponse;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncResponseDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.SyncMetaDataRepository;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CardServiceImpl implements CardService {

    private static final int PAGE_SIZE = 250;
    private static final Logger log = LoggerFactory.getLogger(CardServiceImpl.class);

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final PokemonTcgApiClient apiClient;
    private final SyncMetaDataRepository syncMetaDataRepository;
    
    public CardServiceImpl(CardRepository cardRepository, CardMapper cardMapper, PokemonTcgApiClient apiClient,SyncMetaDataRepository syncMetaDataRepository) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
        this.apiClient = apiClient;
        this.syncMetaDataRepository= syncMetaDataRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"cards", "cards-search"}, allEntries = true)
    public SyncResponseDTO syncCards(SyncRequestDTO request) {
        String setId = request.getSetId();
        int page = 1;
        int totalSynced = 0;
        int totalPages;

        Set<String> existingIds = cardRepository.findAllExternalIdsBySetId(setId);

        do {
            PokemonTcgResponse apiResponse = apiClient.fetchCardsBySet(setId, page, PAGE_SIZE);
            if (apiResponse == null || apiResponse.getData() == null) break;

            totalPages = (int) Math.ceil((double) apiResponse.getTotalCount() / PAGE_SIZE);

            List<CardEntity> newCards = new ArrayList<>();
            for (PokemonTcgApiClient.PokemonTcgCardResponse apiCard : apiResponse.getData()) {
                if (!existingIds.contains(apiCard.getId())) {
                    newCards.add(cardMapper.toEntity(apiCard));
                }
            }
            if (!newCards.isEmpty()) {
                cardRepository.saveAll(newCards);
                totalSynced += newCards.size();
                newCards.stream()
                        .map(CardEntity::getExternalId)
                        .forEach(existingIds::add);
            }
            page++;
        } while (page <= totalPages);

        log.info("Card sync completed: {} new cards synced for set '{}'", totalSynced, setId);
        SyncMetaDataEntity metadata = syncMetaDataRepository.findBySetId(request.getSetId())
                .orElseGet(SyncMetaDataEntity::new);
        metadata.setSetId(request.getSetId());
        metadata.setLastSyncAt(LocalDateTime.now());
        metadata.setSyncedCount(totalSynced);
        syncMetaDataRepository.save(metadata);
        
        return new SyncResponseDTO(totalSynced);
    }

    @Override
    @Cacheable(value = "cards", key = "#filter.toString() + '-' + #pageable")
    public Page<CardDTO> listCards(CardFilterDTO filter, Pageable pageable) {
        Specification<CardEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) return cb.conjunction();
            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.getName().toLowerCase() + "%"));
            }
            if (filter.getType() != null) {
                // "types" es una columna JSONB (array, ej: ["GRASS"]). PostgreSQL no permite
                // castear jsonb a varchar, así que un LIKE sobre el cast rompía la consulta.
                // jsonb_exists(types, 'GRASS') comprueba la pertenencia exacta del elemento.
                predicates.add(cb.isTrue(cb.function(
                    "jsonb_exists", Boolean.class,
                    root.get("types"),
                    cb.literal(filter.getType().name())
                )));
            }
            if (filter.getStage() != null) {
                predicates.add(cb.equal(root.get("stage"), filter.getStage()));
            }
            if (filter.getSupertype() != null) {
                predicates.add(cb.equal(root.get("supertype"), filter.getSupertype()));
            }
            if (filter.getSetId() != null) {
                predicates.add(cb.equal(root.get("setId"), filter.getSetId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return cardRepository.findAll(spec, pageable).map(cardMapper::toDTO);
    }

    @Override
    @Cacheable(value = "cards", key = "#id")
    public CardDTO getCardById(UUID id) {
        CardEntity entity = cardRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + id));
        return cardMapper.toDTO(entity);
    }

    @Override
    @Cacheable(value = "cards-search", key = "#name")
    public List<CardDTO> searchCards(String name) {
        return cardRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(cardMapper::toDTO)
                .toList();
    }
}
