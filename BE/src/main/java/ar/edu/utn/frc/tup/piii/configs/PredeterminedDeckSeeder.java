package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Component
@Order(3)
public class PredeterminedDeckSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PredeterminedDeckSeeder.class);

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;

    public PredeterminedDeckSeeder(CardRepository cardRepository,
                                   DeckRepository deckRepository) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() == 0) {
            log.info("No cards synced yet, skipping predetermined deck seeding");
            return;
        }

        deckRepository.findBySourceAndIsActive(DeckSource.PREDETERMINED, true)
                .forEach(d -> {
                    if ("Mazo test".equals(d.getName())) {
                        deckRepository.delete(d);
                        log.info("Deleted existing predetermined deck 'Mazo test' for reseed");
                    }
                });
        deckRepository.flush();

        Map<String, CardEntity> byExternalId = new HashMap<>();
        for (CardEntity c : cardRepository.findAll()) {
            if (c.getExternalId() != null) {
                byExternalId.put(c.getExternalId(), c);
            }
        }

        DeckEntity deck = new DeckEntity();
        deck.setName("Mazo test");
        deck.setDescription("Mazo balanceado del set XY1 con cartas para probar todas las mecánicas del juego.");
        deck.setSource(DeckSource.PREDETERMINED);
        deck.setCreatorId(null);
        deck.setIsActive(true);

        addCard(deck, byExternalId, "xy1-12", 2);
        addCard(deck, byExternalId, "xy1-13", 1);
        addCard(deck, byExternalId, "xy1-14", 2);
        addCard(deck, byExternalId, "xy1-42", 2);
        addCard(deck, byExternalId, "xy1-43", 1);
        addCard(deck, byExternalId, "xy1-54", 2);
        addCard(deck, byExternalId, "xy1-55", 1);
        addCard(deck, byExternalId, "xy1-74", 2);
        addCard(deck, byExternalId, "xy1-76", 1);
        addCard(deck, byExternalId, "xy1-83", 1);
        addCard(deck, byExternalId, "xy1-84", 1);
        addCard(deck, byExternalId, "xy1-85", 1);
        addCard(deck, byExternalId, "xy1-92", 2);
        addCard(deck, byExternalId, "xy1-93", 1);
        addCard(deck, byExternalId, "xy1-79", 1);
        addCard(deck, byExternalId, "xy1-1", 1);

        addCard(deck, byExternalId, "xy1-116", 2);
        addCard(deck, byExternalId, "xy1-118", 2);
        addCard(deck, byExternalId, "xy1-119", 2);
        addCard(deck, byExternalId, "xy1-121", 2);
        addCard(deck, byExternalId, "xy1-120", 2);
        addCard(deck, byExternalId, "xy1-123", 2);
        addCard(deck, byExternalId, "xy1-124", 1);
        addCard(deck, byExternalId, "xy1-125", 1);
        addCard(deck, byExternalId, "xy1-128", 2);
        addCard(deck, byExternalId, "xy1-122", 4);
        addCard(deck, byExternalId, "xy1-127", 2);
        addCard(deck, byExternalId, "xy1-129", 1);
        addCard(deck, byExternalId, "xy1-117", 1);

        addCard(deck, byExternalId, "xy1-136", 3);
        addCard(deck, byExternalId, "xy1-140", 3);
        addCard(deck, byExternalId, "xy1-135", 2);
        addCard(deck, byExternalId, "xy1-132", 2);
        addCard(deck, byExternalId, "xy1-138", 1);
        addCard(deck, byExternalId, "xy1-139", 1);
        addCard(deck, byExternalId, "xy1-130", 1);
        addCard(deck, byExternalId, "xy1-131", 1);

        deck.getCards().forEach(c -> c.setDeck(deck));
        deckRepository.save(deck);
        log.info("Predetermined deck 'Mazo test' created with {} cards", deck.getCards().size());
    }

    private void addCard(DeckEntity deck, Map<String, CardEntity> cards,
                         String externalId, int quantity) {
        CardEntity card = cards.get(externalId);
        if (card == null) {
            log.warn("Card {} not found in database, skipping for predetermined deck", externalId);
            return;
        }
        DeckCardEntity entry = DeckCardEntity.builder()
                .card(card)
                .quantity(quantity)
                .build();
        deck.getCards().add(entry);
    }
}
