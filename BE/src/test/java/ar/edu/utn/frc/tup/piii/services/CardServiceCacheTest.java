package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.CardFilterDTO;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.SyncMetaDataRepository;
import ar.edu.utn.frc.tup.piii.scheduler.CacheWarmupRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardServiceCacheTest extends EmbeddedPostgresTestBase {

    @MockitoBean
    private CardRepository cardRepository;

    @MockitoBean
    private PokemonTcgApiClient apiClient;

    @MockitoBean
    private SyncMetaDataRepository syncMetaDataRepository;

    @MockitoBean
    private CacheWarmupRunner cacheWarmupRunner;

    @Autowired
    private CardService cardService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }

    @Test
    void getCardById_shouldCacheResult() {
        UUID id = UUID.randomUUID();
        CardEntity entity = new CardEntity();
        entity.setId(id);
        entity.setExternalId("xy1-1");
        entity.setName("Bulbasaur");
        when(cardRepository.findById(id)).thenReturn(Optional.of(entity));

        CardDTO first = cardService.getCardById(id);
        CardDTO second = cardService.getCardById(id);

        assertNotNull(first);
        assertNotNull(second);
        verify(cardRepository, times(1)).findById(id);
    }

    @Test
    void searchCards_shouldCacheResult() {
        CardEntity entity = new CardEntity();
        entity.setExternalId("xy1-1");
        entity.setName("Bulbasaur");
        when(cardRepository.findByNameContainingIgnoreCase("Bulba")).thenReturn(List.of(entity));

        cardService.searchCards("Bulba");
        cardService.searchCards("Bulba");

        verify(cardRepository, times(1)).findByNameContainingIgnoreCase("Bulba");
    }

    @Test
    void listCards_shouldCacheResult() {
        when(cardRepository.findAll(any(Specification.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of()));

        CardFilterDTO filter = new CardFilterDTO();
        PageRequest pageable = PageRequest.of(0, 20);

        cardService.listCards(filter, pageable);
        cardService.listCards(filter, pageable);

        verify(cardRepository, times(1)).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void syncCards_shouldEvictCache() {
        UUID id = UUID.randomUUID();
        CardEntity entity = new CardEntity();
        entity.setId(id);
        entity.setExternalId("xy1-1");
        entity.setName("Bulbasaur");
        when(cardRepository.findById(id)).thenReturn(Optional.of(entity));
        when(apiClient.fetchCardsBySet(anyString(), anyInt(), anyInt())).thenReturn(null);
        when(syncMetaDataRepository.findBySetId(anyString())).thenReturn(Optional.empty());

        cardService.getCardById(id);
        verify(cardRepository, times(1)).findById(id);

        cardService.syncCards(new SyncRequestDTO());

        cardService.getCardById(id);
        verify(cardRepository, times(2)).findById(id);
    }
}
