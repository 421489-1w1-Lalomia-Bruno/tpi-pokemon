package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.enums.DeckSource;
import ar.edu.utn.frc.tup.piii.enums.UserGender;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Order(4)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestDeckSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDeckSeeder.class);

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestDeckSeeder(CardRepository cardRepository,
                          DeckRepository deckRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() == 0) {
            log.info("No cards synced yet, skipping test deck seeding");
            return;
        }

        Map<String, CardEntity> byExternalId = new HashMap<>();
        for (CardEntity c : cardRepository.findAll()) {
            if (c.getExternalId() != null) {
                byExternalId.put(c.getExternalId(), c);
            }
        }

        UserEntity bruno = getOrCreateUser("bruno", "test123");
        UserEntity bruno2 = getOrCreateUser("bruno2", "test123");

        String brunoId = bruno.getId().toString();
        String bruno2Id = bruno2.getId().toString();

        createDeck("Mazo A — Bruno: Naturaleza + Llama", brunoId, deck1Cards(), byExternalId);
        createDeck("Mazo B — Bruno: Agua + Rayo + Psíquico", brunoId, deck2Cards(), byExternalId);
        createDeck("Mazo A — Bruno2: Lucha + Siniestro + Metal", bruno2Id, deck3Cards(), byExternalId);
        createDeck("Mazo B — Bruno2: Hada + Incoloro", bruno2Id, deck4Cards(), byExternalId);

        log.info("4 test decks created for bruno and bruno2");
    }

    private UserEntity getOrCreateUser(String username, String password) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            UserEntity user = UserEntity.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .gender(UserGender.MALE)
                    .wins(0).losses(0)
                    .build();
            return userRepository.save(user);
        });
    }

    /**
     * DECK 1 — Bruno A: Grass (19) + Fire (9) + Fletchling (1) = 29 Pokemon
     * + 18 Energies + 13 Trainers = 60
     */
    private List<String> deck1Cards() {
        List<String> cards = new ArrayList<>();
        // Grass Pokemon (19)
        for (int i = 1; i <= 19; i++) cards.add("xy1-" + i);
        // Fire Pokemon (9: xy1-20 to xy1-28)
        for (int i = 20; i <= 28; i++) cards.add("xy1-" + i);
        // Fletchling (needed for evolution: Fletchling->Fletchinder->Talonflame)
        cards.add("xy1-113");
        // Energies (18)
        addN(cards, "xy1-132", 6);   // Grass x6
        addN(cards, "xy1-133", 4);   // Fire x4
        addN(cards, "xy1-130", 4);   // DCE x4
        addN(cards, "xy1-131", 4);   // Rainbow x4
        // Trainers (13)
        addN(cards, "xy1-122", 3);   // Professor Sycamore x3
        addN(cards, "xy1-127", 2);   // Shauna x2
        addN(cards, "xy1-115", 1);   // Cassius x1
        addN(cards, "xy1-129", 1);   // Team Flare Grunt x1
        addN(cards, "xy1-118", 2);   // Great Ball x2
        addN(cards, "xy1-116", 1);   // Evosoda x1
        addN(cards, "xy1-128", 1);   // Super Potion x1
        addN(cards, "xy1-121", 1);   // Muscle Band x1
        addN(cards, "xy1-119", 1);   // Hard Charm x1
        return cards;
    }

    /**
     * DECK 2 — Bruno B: Water (13) + Lightning (5) + Psychic (11) = 29 Pokemon
     * + 18 Energies + 13 Trainers = 60
     */
    private List<String> deck2Cards() {
        List<String> cards = new ArrayList<>();
        // Water (13: xy1-29 to xy1-41)
        for (int i = 29; i <= 41; i++) cards.add("xy1-" + i);
        // Lightning (5: xy1-42 to xy1-46)
        for (int i = 42; i <= 46; i++) cards.add("xy1-" + i);
        // Psychic (11: xy1-47 to xy1-57)
        for (int i = 47; i <= 57; i++) cards.add("xy1-" + i);
        // Energies (18)
        addN(cards, "xy1-134", 5);   // Water x5
        addN(cards, "xy1-135", 3);   // Lightning x3
        addN(cards, "xy1-136", 4);   // Psychic x4
        addN(cards, "xy1-130", 3);   // DCE x3
        addN(cards, "xy1-131", 3);   // Rainbow x3
        // Trainers (13)
        addN(cards, "xy1-122", 3);
        addN(cards, "xy1-127", 2);
        addN(cards, "xy1-115", 1);
        addN(cards, "xy1-129", 1);
        addN(cards, "xy1-118", 2);
        addN(cards, "xy1-116", 1);
        addN(cards, "xy1-120", 1);   // Max Revive x1
        addN(cards, "xy1-128", 1);
        addN(cards, "xy1-124", 1);   // Red Card x1
        return cards;
    }

    /**
     * DECK 3 — Bruno2 A: Fighting (10) + Darkness (12) + Metal (7) = 29 Pokemon
     * + 18 Energies + 13 Trainers = 60
     */
    private List<String> deck3Cards() {
        List<String> cards = new ArrayList<>();
        // Fighting (10: xy1-58 to xy1-67)
        for (int i = 58; i <= 67; i++) cards.add("xy1-" + i);
        // Darkness (12: xy1-68 to xy1-79)
        for (int i = 68; i <= 79; i++) cards.add("xy1-" + i);
        // Metal (7: xy1-80 to xy1-86)
        for (int i = 80; i <= 86; i++) cards.add("xy1-" + i);
        // Energies (18)
        addN(cards, "xy1-137", 3);   // Fighting x3
        addN(cards, "xy1-138", 4);   // Darkness x4
        addN(cards, "xy1-139", 3);   // Metal x3
        addN(cards, "xy1-130", 4);
        addN(cards, "xy1-131", 4);
        // Trainers (13)
        addN(cards, "xy1-122", 3);
        addN(cards, "xy1-127", 2);
        addN(cards, "xy1-115", 1);
        addN(cards, "xy1-129", 1);
        addN(cards, "xy1-118", 2);
        addN(cards, "xy1-116", 1);
        addN(cards, "xy1-123", 1);   // Professor's Letter x1
        addN(cards, "xy1-121", 1);
        addN(cards, "xy1-119", 1);
        return cards;
    }

    /**
     * DECK 4 — Bruno2 B: Fairy (11) + Colorless (16) = 27 Pokemon
     * + 20 Energies + 13 Trainers = 60
     */
    private List<String> deck4Cards() {
        List<String> cards = new ArrayList<>();
        // Fairy (11: xy1-87 to xy1-97)
        for (int i = 87; i <= 97; i++) cards.add("xy1-" + i);
        // Colorless (16: xy1-98 to xy1-112, xy1-114) — Fletchling xy1-113 already in Deck 1
        for (int i = 98; i <= 112; i++) cards.add("xy1-" + i);
        cards.add("xy1-114");
        // Energies (20)
        addN(cards, "xy1-140", 5);   // Fairy x5
        addN(cards, "xy1-132", 4);   // Grass x4 (for Colorless)
        addN(cards, "xy1-133", 3);   // Fire x3 (for Colorless)
        addN(cards, "xy1-130", 4);
        addN(cards, "xy1-131", 4);
        // Trainers (13)
        addN(cards, "xy1-122", 3);
        addN(cards, "xy1-127", 2);
        addN(cards, "xy1-115", 1);
        addN(cards, "xy1-129", 1);
        addN(cards, "xy1-118", 2);
        addN(cards, "xy1-116", 1);
        addN(cards, "xy1-123", 1);
        addN(cards, "xy1-128", 1);
        addN(cards, "xy1-121", 1);
        return cards;
    }

    private void addN(List<String> list, String externalId, int quantity) {
        for (int i = 0; i < quantity; i++) {
            list.add(externalId);
        }
    }

    private void createDeck(String name, String creatorId, List<String> cardExternalIds,
                            Map<String, CardEntity> byExternalId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String extId : cardExternalIds) {
            counts.merge(extId, 1, Integer::sum);
        }

        DeckEntity deck = new DeckEntity();
        deck.setName(name);
        deck.setDescription("Mazo de prueba para testear mecánicas del set XY1");
        deck.setSource(DeckSource.USER);
        deck.setCreatorId(creatorId);
        deck.setIsActive(true);

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            CardEntity card = byExternalId.get(entry.getKey());
            if (card == null) {
                log.warn("Card {} not found in database, skipping", entry.getKey());
                continue;
            }
            DeckCardEntity dce = DeckCardEntity.builder()
                    .card(card)
                    .quantity(entry.getValue())
                    .build();
            deck.getCards().add(dce);
        }

        deck.getCards().forEach(c -> c.setDeck(deck));
        deckRepository.save(deck);
        log.info("Test deck '{}' created with {} cards", name, deck.getCards().stream()
                .mapToInt(DeckCardEntity::getQuantity).sum());
    }
}
