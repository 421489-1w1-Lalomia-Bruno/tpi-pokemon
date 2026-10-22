package ar.edu.utn.frc.tup.piii.mappers;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class DeckMapper {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public DeckMapper(CardRepository cardRepository, CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    public DeckEntity toEntity(CreateDeckDTO dto) {
        DeckEntity entity = new DeckEntity();
        entity.setName(dto.getName());
        entity.setCards(toEntityList(dto.getCards()));
        return entity;
    }

    public List<DeckCardEntity> toEntityList(List<DeckCardEntryDTO> entries) {
        return entries.stream()
                .map(this::toDeckCardEntity)
                .toList();
    }

    private DeckCardEntity toDeckCardEntity(DeckCardEntryDTO entry) {
        CardEntity card = cardRepository.findById(UUID.fromString(entry.getCardId()))
                .orElseThrow(() -> new EntityNotFoundException("Card not found: " + entry.getCardId()));
        return DeckCardEntity.builder()
                .card(card)
                .quantity(entry.getQuantity())
                .build();
    }

    public DeckDTO toDto(DeckEntity entity) {
        DeckDTO dto = new DeckDTO();
        dto.setId(entity.getId().toString());
        dto.setName(entity.getName());
        dto.setCreatorId(entity.getCreatorId());
        dto.setSource(entity.getSource().name());
        dto.setDescription(entity.getDescription());
        dto.setCards(entity.getCards().stream()
                .map(this::toCardDto)
                .toList());
        return dto;
    }

    public DeckCardDTO toCardDto(DeckCardEntity entity) {
        DeckCardDTO dto = new DeckCardDTO();
        dto.setCardId(entity.getCard().getId().toString());
        dto.setName(entity.getCard().getName());
        dto.setQuantity(entity.getQuantity());
        dto.setCard(cardMapper.toDTO(entity.getCard()));
        return dto;
    }

    public CreateDeckDTO toCreateDto(DeckEntity entity) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(entity.getName());
        dto.setCards(entity.getCards().stream()
                .map(cardEntity -> {
                    DeckCardEntryDTO entry = new DeckCardEntryDTO();
                    entry.setCardId(cardEntity.getCard().getId().toString());
                    entry.setQuantity(cardEntity.getQuantity());
                    return entry;
                })
                .toList());
        return dto;
    }
}