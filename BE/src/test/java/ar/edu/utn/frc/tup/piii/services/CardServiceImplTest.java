package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncResponseDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.exceptions.EntityNotFoundException;
import ar.edu.utn.frc.tup.piii.mappers.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.SyncMetaDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PokemonTcgApiClient apiClient;

    @Mock
    private SyncMetaDataRepository syncMetaDataRepository;

    private CardMapper cardMapper;

    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        cardMapper = new CardMapper(new ObjectMapper());
        cardService = new CardServiceImpl(cardRepository, cardMapper, apiClient, syncMetaDataRepository);
    }

    @Test
    void getAllCards_shouldReturnPageOfCards() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setPage(0);
        filter.setSize(20);
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Bulbasaur", result.getContent().get(0).getName());
    }

    @Test
    void getAllCards_withSupertypeFilter_shouldReturnFilteredCards() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setSupertype("Pokémon");
        filter.setPage(0);
        filter.setSize(20);
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCardById_shouldReturnCardWhenExists() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CardEntity entity = buildCardEntity();
        when(cardRepository.findById(uuid)).thenReturn(Optional.of(entity));

        CardDTO result = cardService.getCardById(uuid);

        assertNotNull(result);
        assertEquals("Bulbasaur", result.getName());
        assertEquals(uuid.toString(), result.getId());
    }

    @Test
    void getCardById_shouldThrowWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(cardRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cardService.getCardById(uuid));
    }

    @Test
    void searchByName_shouldReturnMatchingCards() {
        CardEntity entity = buildCardEntity();
        when(cardRepository.findByNameContainingIgnoreCase("Bulb")).thenReturn(List.of(entity));

        List<CardDTO> result = cardService.searchCards("Bulb");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Bulbasaur", result.get(0).getName());
    }

    @Test
    void searchByName_shouldReturnEmptyListWhenNoMatch() {
        when(cardRepository.findByNameContainingIgnoreCase("zzz")).thenReturn(List.of());

        List<CardDTO> result = cardService.searchCards("zzz");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void syncCards_shouldPersistNewCards() {
        PokemonTcgApiClient.PokemonTcgCardResponse apiCard = new PokemonTcgApiClient.PokemonTcgCardResponse();
        apiCard.setId("xy1-1");
        apiCard.setName("Bulbasaur");
        apiCard.setSupertype("Pokémon");
        apiCard.setHp("60");
        apiCard.setTypes(List.of("Grass"));
        apiCard.setAttacks(List.of());
        apiCard.setAbilities(List.of());
        apiCard.setImages(new PokemonTcgApiClient.CardImagesDTO());
        apiCard.getImages().setLarge("https://example.com/bulbasaur.png");
        apiCard.setNumber("1");
        apiCard.setRarity("Common");

        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(List.of(apiCard));
        apiResponse.setTotalCount(1);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>());
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(1, result.getSynced());
        verify(cardRepository).saveAll(anyList());
    }

    @Test
    void syncCards_shouldSkipExistingCard() {
        PokemonTcgApiClient.PokemonTcgCardResponse apiCard = new PokemonTcgApiClient.PokemonTcgCardResponse();
        apiCard.setId("xy1-1");
        apiCard.setName("Bulbasaur");
        apiCard.setSupertype("Pokémon");
        apiCard.setHp("60");
        apiCard.setTypes(List.of("Grass"));
        apiCard.setAttacks(List.of());
        apiCard.setAbilities(List.of());
        apiCard.setImages(new PokemonTcgApiClient.CardImagesDTO());
        apiCard.getImages().setLarge("https://example.com/bulbasaur.png");
        apiCard.setNumber("1");

        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(List.of(apiCard));
        apiResponse.setTotalCount(1);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>(Set.of("xy1-1")));

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(0, result.getSynced());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void syncCards_shouldReturnZeroWhenApiReturnsNullData() {
        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(null);

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(0, result.getSynced());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void listCards_withNullFilter_shouldUseConjunction() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<CardDTO> result = cardService.listCards(null, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listCards_withNameFilter_shouldFilterByName() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setName("Bulba");
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listCards_withEnergyTypeFilter_shouldFilterByType() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setType(EnergyType.GRASS);
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listCards_withStageFilter_shouldFilterByStage() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setStage(PokemonStage.BASIC);
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void listCards_withSetIdFilter_shouldFilterBySetId() {
        CardEntity entity = buildCardEntity();
        Page<CardEntity> page = new PageImpl<>(List.of(entity));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        CardFilterDTO filter = new CardFilterDTO();
        filter.setSetId("xy1");
        Page<CardDTO> result = cardService.listCards(filter, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void syncCards_whenApiDataIsNull_shouldReturnZero() {
        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(null);
        apiResponse.setTotalCount(0);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>());

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(0, result.getSynced());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void syncCards_withMultiplePages_shouldSyncAllPages() {
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>());

        PokemonTcgApiClient.PokemonTcgResponse page1 = new PokemonTcgApiClient.PokemonTcgResponse();
        page1.setData(List.of(buildApiCard("xy1-1", "Bulbasaur"), buildApiCard("xy1-2", "Ivysaur")));
        page1.setTotalCount(251);

        PokemonTcgApiClient.PokemonTcgResponse page2 = new PokemonTcgApiClient.PokemonTcgResponse();
        page2.setData(List.of(buildApiCard("xy1-3", "Venusaur")));
        page2.setTotalCount(251);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(page1);
        when(apiClient.fetchCardsBySet(eq("xy1"), eq(2), eq(250))).thenReturn(page2);
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(3, result.getSynced());
        verify(cardRepository, times(2)).saveAll(anyList());
        verify(apiClient).fetchCardsBySet(eq("xy1"), eq(1), eq(250));
        verify(apiClient).fetchCardsBySet(eq("xy1"), eq(2), eq(250));
    }

    @Test
    void syncCards_withMultiplePagesAndExistingIds_shouldSkipDuplicates() {
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>(Set.of("xy1-1")));

        PokemonTcgApiClient.PokemonTcgResponse page1 = new PokemonTcgApiClient.PokemonTcgResponse();
        page1.setData(List.of(buildApiCard("xy1-1", "Bulbasaur"), buildApiCard("xy1-2", "Ivysaur")));
        page1.setTotalCount(251);

        PokemonTcgApiClient.PokemonTcgResponse page2 = new PokemonTcgApiClient.PokemonTcgResponse();
        page2.setData(List.of(buildApiCard("xy1-3", "Venusaur")));
        page2.setTotalCount(251);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(page1);
        when(apiClient.fetchCardsBySet(eq("xy1"), eq(2), eq(250))).thenReturn(page2);
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(2, result.getSynced());
        verify(cardRepository, times(2)).saveAll(anyList());
    }

    @Test
    void syncCards_whenAllCardsAlreadyExist_shouldNotSaveAny() {
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>(Set.of("xy1-1")));

        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(List.of(buildApiCard("xy1-1", "Bulbasaur")));
        apiResponse.setTotalCount(1);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);

        SyncResponseDTO result = cardService.syncCards(new SyncRequestDTO());

        assertEquals(0, result.getSynced());
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void syncCards_shouldSaveSyncMetadata() {
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>());

        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(List.of(buildApiCard("xy1-1", "Bulbasaur")));
        apiResponse.setTotalCount(1);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        cardService.syncCards(new SyncRequestDTO());

        ArgumentCaptor<SyncMetaDataEntity> captor = ArgumentCaptor.forClass(SyncMetaDataEntity.class);
        verify(syncMetaDataRepository).save(captor.capture());
        SyncMetaDataEntity saved = captor.getValue();
        assertEquals("xy1", saved.getSetId());
        assertEquals(1, saved.getSyncedCount());
        assertNotNull(saved.getLastSyncAt());
    }

    @Test
    void syncCards_whenMetadataAlreadyExists_shouldUpdateExisting() {
        when(cardRepository.findAllExternalIdsBySetId("xy1")).thenReturn(new HashSet<>());

        PokemonTcgApiClient.PokemonTcgResponse apiResponse = new PokemonTcgApiClient.PokemonTcgResponse();
        apiResponse.setData(List.of(buildApiCard("xy1-1", "Bulbasaur")));
        apiResponse.setTotalCount(1);

        when(apiClient.fetchCardsBySet(eq("xy1"), eq(1), eq(250))).thenReturn(apiResponse);
        when(cardRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SyncMetaDataEntity existingMetadata = new SyncMetaDataEntity();
        existingMetadata.setSetId("xy1");
        existingMetadata.setLastSyncAt(java.time.LocalDateTime.now().minusDays(1));
        existingMetadata.setSyncedCount(5);
        when(syncMetaDataRepository.findBySetId("xy1")).thenReturn(Optional.of(existingMetadata));

        cardService.syncCards(new SyncRequestDTO());

        ArgumentCaptor<SyncMetaDataEntity> captor = ArgumentCaptor.forClass(SyncMetaDataEntity.class);
        verify(syncMetaDataRepository).save(captor.capture());
        SyncMetaDataEntity saved = captor.getValue();
        assertEquals("xy1", saved.getSetId());
        assertEquals(1, saved.getSyncedCount());
        assertNotNull(saved.getLastSyncAt());
        assertTrue(saved.getLastSyncAt().isAfter(java.time.LocalDateTime.now().minusMinutes(1)));
    }

    private CardEntity buildCardEntity() {
        CardEntity entity = CardEntity.builder()
                .externalId("xy1-1")
                .name("Bulbasaur")
                .supertype("Pok\u00e9mon")
                .subtypes(List.of("Basic"))
                .hp(60)
                .types(new HashSet<>(Set.of(EnergyType.GRASS)))
                .stage(PokemonStage.BASIC)
                .weaknessType("FIRE")
                .retreatCost(1)
                .imageUrl("https://example.com/bulbasaur.png")
                .rarity("Common")
                .cardNumber("1")
                .setId("xy1")
                .attacksJson("[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\",\"COLORLESS\"],\"damage\":\"30\",\"text\":\"\"}]")
                .build();
        entity.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        return entity;
    }

    private PokemonTcgApiClient.PokemonTcgCardResponse buildApiCard(String id, String name) {
        PokemonTcgApiClient.PokemonTcgCardResponse card = new PokemonTcgApiClient.PokemonTcgCardResponse();
        card.setId(id);
        card.setName(name);
        card.setSupertype("Pok\u00e9mon");
        card.setHp("60");
        card.setTypes(List.of("Grass"));
        card.setAttacks(List.of());
        card.setAbilities(List.of());
        card.setImages(new PokemonTcgApiClient.CardImagesDTO());
        card.getImages().setLarge("https://example.com/" + name.toLowerCase() + ".png");
        card.setNumber(id.contains("-") ? id.substring(id.indexOf('-') + 1) : id);
        card.setRarity("Common");
        return card;
    }
}
