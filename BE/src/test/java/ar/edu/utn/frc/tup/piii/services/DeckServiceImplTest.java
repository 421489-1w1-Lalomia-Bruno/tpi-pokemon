package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.exceptions.DeckValidationException;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceImplTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckMapper deckMapper;

    @Mock
    private DeckValidationService deckValidationService;

    @Mock
    private EntityManager entityManager;

    private DeckServiceImpl deckService;

    private final UUID deckId = UUID.randomUUID();
    private final UUID cardId1 = UUID.randomUUID();
    private final UUID cardId2 = UUID.randomUUID();
    private final String creatorId = "test-player-123";

    @BeforeEach
    void setUp() {
        deckService = new DeckServiceImpl(deckRepository, deckMapper, deckValidationService);
        ReflectionTestUtils.setField(deckService, "entityManager", entityManager);
    }

    @Test
    void createDeck_validDeck_returnsDeckDTO() {
        CreateDeckDTO dto = buildCreateDeckDTO("My Deck", buildValidEntries());
        DeckValidationResultDTO validResult = buildValidValidationResult();
        DeckEntity entity = buildDeckEntity("My Deck");
        DeckEntity savedEntity = buildDeckEntity("My Deck");
        savedEntity.setId(deckId);
        DeckDTO expectedDto = buildDeckDTO("My Deck");

        when(deckValidationService.validateDeck(dto)).thenReturn(validResult);
        when(deckMapper.toEntity(dto)).thenReturn(entity);
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(savedEntity);
        when(deckMapper.toDto(savedEntity)).thenReturn(expectedDto);
        when(deckMapper.toCreateDto(savedEntity)).thenReturn(dto);

        DeckDTO result = deckService.createDeck(dto, creatorId);

        assertNotNull(result);
        assertEquals("My Deck", result.getName());
        verify(deckRepository).save(any(DeckEntity.class));
    }

    @Test
    void createDeck_invalidDeck_throwsDeckValidationException() {
        CreateDeckDTO dto = buildCreateDeckDTO("Bad Deck", List.of());
        DeckValidationResultDTO invalidResult = DeckValidationResultDTO.builder()
                .isValid(false)
                .errors(List.of("Deck must have exactly 60 cards"))
                .totalCards(0)
                .basicPokemonCount(0)
                .build();

        when(deckValidationService.validateDeck(dto)).thenReturn(invalidResult);

        assertThrows(DeckValidationException.class, () -> deckService.createDeck(dto, creatorId));
        verify(deckRepository, never()).save(any());
    }

    @Test
    void getDeckById_existingAndOwned_returnsDeckDTO() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId(creatorId);
        entity.setIsActive(true);
        DeckDTO expectedDto = buildDeckDTO("My Deck");

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(entity));
        when(deckMapper.toDto(entity)).thenReturn(expectedDto);
        when(deckMapper.toCreateDto(entity)).thenReturn(buildCreateDeckDTO("My Deck", buildValidEntries()));
        when(deckValidationService.validateDeck(any(CreateDeckDTO.class))).thenReturn(buildValidValidationResult());

        DeckDTO result = deckService.getDeckById(deckId, creatorId);

        assertNotNull(result);
        assertEquals("My Deck", result.getName());
    }

    @Test
    void getDeckById_notFound_throwsEntityNotFoundException() {
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> deckService.getDeckById(deckId, creatorId));
    }

    @Test
    void getDeckById_wrongOwner_throwsEntityNotFoundException() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId("other-player");
        entity.setIsActive(true);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(entity));

        assertThrows(EntityNotFoundException.class, () -> deckService.getDeckById(deckId, creatorId));
    }

    @Test
    void getDeckById_inactiveDeck_throwsEntityNotFoundException() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId(creatorId);
        entity.setIsActive(false);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(entity));

        assertThrows(EntityNotFoundException.class, () -> deckService.getDeckById(deckId, creatorId));
    }

    @Test
    void getDecksByCreator_returnsListOfDeckDTOs() {
        DeckEntity entity1 = buildDeckEntity("Deck 1");
        entity1.setCreatorId(creatorId);
        entity1.setIsActive(true);
        DeckEntity entity2 = buildDeckEntity("Deck 2");
        entity2.setCreatorId(creatorId);
        entity2.setIsActive(true);

        when(deckRepository.findByCreatorIdAndIsActive(creatorId, true)).thenReturn(List.of(entity1, entity2));
        when(deckMapper.toDto(entity1)).thenReturn(buildDeckDTO("Deck 1"));
        when(deckMapper.toDto(entity2)).thenReturn(buildDeckDTO("Deck 2"));
        when(deckMapper.toCreateDto(entity1)).thenReturn(buildCreateDeckDTO("Deck 1", buildValidEntries()));
        when(deckMapper.toCreateDto(entity2)).thenReturn(buildCreateDeckDTO("Deck 2", buildValidEntries()));
        when(deckValidationService.validateDeck(any(CreateDeckDTO.class))).thenReturn(buildValidValidationResult());

        List<DeckDTO> result = deckService.getDecksByCreator(creatorId);

        assertEquals(2, result.size());
        assertEquals("Deck 1", result.get(0).getName());
        assertEquals("Deck 2", result.get(1).getName());
    }

    @Test
    void updateDeck_validDeck_returnsUpdatedDeckDTO() {
        CreateDeckDTO dto = buildCreateDeckDTO("Updated Deck", buildValidEntries());
        DeckValidationResultDTO validResult = buildValidValidationResult();
        DeckEntity existing = buildDeckEntity("Old Name");
        existing.setId(deckId);
        existing.setCreatorId(creatorId);
        existing.setIsActive(true);
        DeckDTO expectedDto = buildDeckDTO("Updated Deck");

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(existing));
        when(deckValidationService.validateDeck(dto)).thenReturn(validResult);
        when(deckMapper.toEntityList(dto.getCards())).thenReturn(List.of(buildDeckCardEntity(cardId1, 4)));
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(existing);
        when(deckMapper.toDto(existing)).thenReturn(expectedDto);
        when(deckMapper.toCreateDto(existing)).thenReturn(dto);

        DeckDTO result = deckService.updateDeck(deckId, dto, creatorId);

        assertNotNull(result);
        assertEquals("Updated Deck", existing.getName());
        verify(deckRepository).save(existing);
    }

    @Test
    void updateDeck_invalidDeck_throwsDeckValidationException() {
        CreateDeckDTO dto = buildCreateDeckDTO("Bad Deck", List.of());
        DeckValidationResultDTO invalidResult = DeckValidationResultDTO.builder()
                .isValid(false)
                .errors(List.of("Deck must have exactly 60 cards"))
                .totalCards(0)
                .basicPokemonCount(0)
                .build();

        when(deckValidationService.validateDeck(dto)).thenReturn(invalidResult);

        assertThrows(DeckValidationException.class, () -> deckService.updateDeck(deckId, dto, creatorId));
        verify(deckRepository, never()).findById(any());
        verify(deckRepository, never()).save(any());
    }

    @Test
    void deleteDeck_existingDeck_setsInactive() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId(creatorId);
        entity.setIsActive(true);

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(entity));
        when(deckRepository.save(entity)).thenReturn(entity);

        deckService.deleteDeck(deckId, creatorId);

        assertFalse(entity.getIsActive());
        verify(deckRepository).save(entity);
    }

    @Test
    void validateDeckById_existingDeck_returnsValidationResult() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId(creatorId);
        entity.setIsActive(true);
        CreateDeckDTO dto = buildCreateDeckDTO("My Deck", buildValidEntries());
        DeckValidationResultDTO expectedResult = buildValidValidationResult();

        when(deckRepository.findById(deckId)).thenReturn(Optional.of(entity));
        when(deckMapper.toCreateDto(entity)).thenReturn(dto);
        when(deckValidationService.validateDeck(dto)).thenReturn(expectedResult);

        DeckValidationResultDTO result = deckService.validateDeckById(deckId, creatorId);

        assertNotNull(result);
        assertTrue(result.getIsValid());
    }

    private CreateDeckDTO buildCreateDeckDTO(String name, List<DeckCardEntryDTO> entries) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(name);
        dto.setCards(entries);
        return dto;
    }

    private List<DeckCardEntryDTO> buildValidEntries() {
        List<DeckCardEntryDTO> entries = new java.util.ArrayList<>();
        entries.add(buildEntry(cardId1, 4));
        entries.add(buildEntry(cardId2, 56));
        return entries;
    }

    private DeckCardEntryDTO buildEntry(UUID cardId, int quantity) {
        DeckCardEntryDTO entry = new DeckCardEntryDTO();
        entry.setCardId(cardId.toString());
        entry.setQuantity(quantity);
        return entry;
    }

    private DeckValidationResultDTO buildValidValidationResult() {
        return DeckValidationResultDTO.builder()
                .isValid(true)
                .errors(List.of())
                .totalCards(60)
                .basicPokemonCount(4)
                .build();
    }

    private DeckEntity buildDeckEntity(String name) {
        DeckEntity entity = new DeckEntity();
        entity.setName(name);
        entity.setCards(new java.util.ArrayList<>());
        return entity;
    }

    private DeckDTO buildDeckDTO(String name) {
        DeckDTO dto = new DeckDTO();
        dto.setName(name);
        dto.setCreatorId(creatorId);
        dto.setCards(List.of());
        dto.setTotalCards(60);
        dto.setIsValid(true);
        dto.setValidationErrors(List.of());
        return dto;
    }

    private DeckCardEntity buildDeckCardEntity(UUID cardId, int quantity) {
        CardEntity card = buildCardEntity(cardId);
        DeckCardEntity entity = new DeckCardEntity();
        entity.setCard(card);
        entity.setQuantity(quantity);
        return entity;
    }

    private CardEntity buildCardEntity(UUID id) {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-1")
                .name("Pikachu")
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .types(Set.of(EnergyType.LIGHTNING))
                .stage(PokemonStage.BASIC)
                .imageUrl("https://example.com/pikachu.png")
                .cardNumber("1")
                .setId("xy1")
                .build();
        card.setId(id);
        return card;
    }
}
