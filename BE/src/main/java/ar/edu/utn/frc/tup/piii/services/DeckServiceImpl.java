package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import ar.edu.utn.frc.tup.piii.exceptions.DeckValidationException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final DeckMapper deckMapper;
    private final DeckValidationService deckValidationService;

    @PersistenceContext
    private EntityManager entityManager;

    public DeckServiceImpl(DeckRepository deckRepository,
                           DeckMapper deckMapper,
                           DeckValidationService deckValidationService) {
        this.deckRepository = deckRepository;
        this.deckMapper = deckMapper;
        this.deckValidationService = deckValidationService;
    }

    @Override
    public DeckValidationResultDTO validateDeck(CreateDeckDTO dto) {
        return deckValidationService.validateDeck(dto);
    }

    @Override
    @Transactional
    public DeckDTO createDeck(CreateDeckDTO dto, String creatorId) {
        DeckValidationResultDTO validation = deckValidationService.validateDeck(dto);
        if (!validation.getIsValid()) {
            throw new DeckValidationException(
                    "Deck validation failed: " + String.join("; ", validation.getErrors()));
        }

        DeckEntity entity = deckMapper.toEntity(dto);
        entity.setCreatorId(creatorId);
        entity.setIsActive(true);
        entity.getCards().forEach(card -> card.setDeck(entity));

        DeckEntity saved = deckRepository.save(entity);
        return toDtoWithValidation(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeckDTO getDeckById(UUID id, String creatorId) {
        DeckEntity entity = findReadableById(id, creatorId);
        return toDtoWithValidation(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeckDTO> getDecksByCreator(String creatorId) {
        List<DeckEntity> all = new ArrayList<>(
                deckRepository.findByCreatorIdAndIsActive(creatorId, true));
        all.addAll(deckRepository.findBySourceAndIsActive(DeckSource.PREDETERMINED, true));
        return all.stream()
                .map(this::toDtoWithValidation)
                .toList();
    }

    @Override
    @Transactional
    public DeckDTO updateDeck(UUID id, CreateDeckDTO dto, String creatorId) {
        DeckValidationResultDTO validation = deckValidationService.validateDeck(dto);
        if (!validation.getIsValid()) {
            throw new DeckValidationException(
                    "Deck validation failed: " + String.join("; ", validation.getErrors()));
        }

        DeckEntity existing = findOwnedById(id, creatorId);
        existing.setName(dto.getName());
        existing.getCards().clear();
        entityManager.flush();

        List<DeckCardEntity> newCards = deckMapper.toEntityList(dto.getCards());
        newCards.forEach(card -> card.setDeck(existing));
        existing.getCards().addAll(newCards);

        DeckEntity saved = deckRepository.save(existing);
        return toDtoWithValidation(saved);
    }

    @Override
    @Transactional
    public void deleteDeck(UUID id, String creatorId) {
        DeckEntity entity = findOwnedById(id, creatorId);
        entity.setIsActive(false);
        deckRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public DeckValidationResultDTO validateDeckById(UUID id, String creatorId) {
        DeckEntity entity = findReadableById(id, creatorId);
        CreateDeckDTO dto = deckMapper.toCreateDto(entity);
        return deckValidationService.validateDeck(dto);
    }

    private DeckEntity findOwnedById(UUID id, String creatorId) {
        DeckEntity entity = deckRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found: " + id));
        if (entity.getSource() == DeckSource.PREDETERMINED) return entity;
        if (!entity.getIsActive() || !entity.getCreatorId().equals(creatorId)) {
            throw new EntityNotFoundException("Deck not found: " + id);
        }
        return entity;
    }

    private DeckEntity findReadableById(UUID id, String creatorId) {
        DeckEntity entity = deckRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deck not found: " + id));
        if (entity.getSource() == DeckSource.PREDETERMINED) return entity;
        if (!entity.getIsActive() || !entity.getCreatorId().equals(creatorId)) {
            throw new EntityNotFoundException("Deck not found: " + id);
        }
        return entity;
    }

    private DeckDTO toDtoWithValidation(DeckEntity entity) {
        DeckDTO dto = deckMapper.toDto(entity);
        CreateDeckDTO createDto = deckMapper.toCreateDto(entity);
        DeckValidationResultDTO validation = deckValidationService.validateDeck(createDto);
        dto.setIsValid(validation.getIsValid());
        dto.setValidationErrors(validation.getErrors());
        dto.setTotalCards(validation.getTotalCards());
        return dto;
    }
}
