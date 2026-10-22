package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.CardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CardService cardService;
    private final CardRepository cardRepository;

    public DataInitializer(CardService cardService, CardRepository cardRepository) {
        this.cardService = cardService;
        this.cardRepository = cardRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() == 0) {
            try {
                cardService.syncCards(new SyncRequestDTO());
                log.info("Card sync completed on startup");
            } catch (Exception e) {
                log.error("Auto-sync failed: {}", e.getMessage());
            }
        }
    }
}