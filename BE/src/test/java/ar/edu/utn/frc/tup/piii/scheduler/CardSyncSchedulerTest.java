package ar.edu.utn.frc.tup.piii.scheduler;

import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;
import ar.edu.utn.frc.tup.piii.repositories.SyncMetaDataRepository;
import ar.edu.utn.frc.tup.piii.services.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardSyncSchedulerTest {

    @Mock
    private CardService cardService;

    @Mock
    private SyncMetaDataRepository syncMetaDataRepository;

    @Captor
    private ArgumentCaptor<SyncRequestDTO> syncRequestCaptor;

    private CardSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new CardSyncScheduler(cardService, syncMetaDataRepository);
        ReflectionTestUtils.setField(scheduler, "intervalDays", 14);
        ReflectionTestUtils.setField(scheduler, "syncSetId", "xy1");
    }

    @Test
    void scheduledSync_shouldSyncWhenNoPreviousMetadata() {
        when(syncMetaDataRepository.findBySetId("xy1")).thenReturn(Optional.empty());

        scheduler.scheduledSync();

        verify(cardService).syncCards(syncRequestCaptor.capture());
        assertEquals("xy1", syncRequestCaptor.getValue().getSetId());
    }

    @Test
    void scheduledSync_shouldSyncWhenLastSyncExceedsInterval() {
        SyncMetaDataEntity metadata = new SyncMetaDataEntity();
        metadata.setLastSyncAt(LocalDateTime.now().minusDays(15));
        when(syncMetaDataRepository.findBySetId("xy1")).thenReturn(Optional.of(metadata));

        scheduler.scheduledSync();

        verify(cardService).syncCards(any(SyncRequestDTO.class));
    }

    @Test
    void scheduledSync_shouldNotSyncWhenLastSyncIsRecent() {
        SyncMetaDataEntity metadata = new SyncMetaDataEntity();
        metadata.setLastSyncAt(LocalDateTime.now().minusDays(7));
        when(syncMetaDataRepository.findBySetId("xy1")).thenReturn(Optional.of(metadata));

        scheduler.scheduledSync();

        verify(cardService, never()).syncCards(any());
    }

    @Test
    void scheduledSync_shouldSyncWhenLastSyncIsExactlyAtInterval() {
        SyncMetaDataEntity metadata = new SyncMetaDataEntity();
        metadata.setLastSyncAt(LocalDateTime.now().minusDays(14).minusMinutes(1));
        when(syncMetaDataRepository.findBySetId("xy1")).thenReturn(Optional.of(metadata));

        scheduler.scheduledSync();

        verify(cardService).syncCards(any(SyncRequestDTO.class));
    }

    @Test
    void scheduledSync_shouldHandleExceptionGracefully() {
        when(syncMetaDataRepository.findBySetId("xy1")).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> scheduler.scheduledSync());
    }
}
