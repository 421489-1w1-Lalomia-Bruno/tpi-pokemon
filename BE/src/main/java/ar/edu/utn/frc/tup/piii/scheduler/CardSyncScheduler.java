package ar.edu.utn.frc.tup.piii.scheduler;

import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.entities.SyncMetaDataEntity;
import ar.edu.utn.frc.tup.piii.repositories.SyncMetaDataRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import ar.edu.utn.frc.tup.piii.services.CardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class CardSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(CardSyncScheduler.class);
    private final CardService cardService;
    private final SyncMetaDataRepository syncMetaDataRepository;

    public CardSyncScheduler(CardService cardService, SyncMetaDataRepository syncMetaDataRepository) {
        this.cardService = cardService;
        this.syncMetaDataRepository = syncMetaDataRepository;
    }

    @Value("${app.sync.interval-days}")
    private int intervalDays;

    @Value("${app.sync.set-id:xy1}")
    private String syncSetId;

    @Scheduled(cron = "${app.sync.cron}")
    public void scheduledSync() {
        try {
            if (shouldSync()) {
                cardService.syncCards(new SyncRequestDTO());
            }
        } catch (Exception e) {
            log.error("Scheduled sync failed: {}", e.getMessage(), e);
        }
    }

    private boolean shouldSync() {
        Optional<SyncMetaDataEntity> metadata = syncMetaDataRepository.findBySetId(syncSetId);
        if (metadata.isEmpty()) {
            return true;
        }

        LocalDateTime lastSync = metadata.get().getLastSyncAt();
        return ChronoUnit.DAYS.between(lastSync, LocalDateTime.now()) >= intervalDays;
    }
}
