package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckValidationResultDTO;
import ar.edu.utn.frc.tup.piii.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckValidationServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    private DeckValidationServiceImpl validationService;

    private final UUID basicPokemonId = UUID.randomUUID();
    private final UUID stage1PokemonId = UUID.randomUUID();
    private final UUID basicEnergyId = UUID.randomUUID();
    private final UUID exPokemonId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        validationService = new DeckValidationServiceImpl(cardRepository);
    }

    @Test
    void validateDeck_validDeck_returnsValid() {
        List<DeckCardEntryDTO> cards = buildValidDeck();
        CreateDeckDTO dto = buildDto("Valid Deck", cards);

        List<CardEntity> entities = buildCardEntities(cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(entities);

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
        assertTrue(result.getErrors().isEmpty());
        assertEquals(60, result.getTotalCards());
        assertTrue(result.getBasicPokemonCount() >= 1);
    }

    @Test
    void validateDeck_not60Cards_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 30));

        CreateDeckDTO dto = buildDto("Small Deck", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of(buildBasicPokemon()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().size() >= 1);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("60 cards")));
        assertEquals(30, result.getTotalCards());
    }

    @Test
    void validateDeck_moreThan4Copies_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 5));
        cards.add(buildEntry(basicEnergyId, 55));

        CreateDeckDTO dto = buildDto("Too Many Copies", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("copies")));
    }

    @Test
    void validateDeck_basicEnergyNoLimit_returnsValid() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 1));
        cards.add(buildEntry(basicEnergyId, 59));

        CreateDeckDTO dto = buildDto("Full Energy", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
    }

    @Test
    void validateDeck_noBasicPokemon_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(stage1PokemonId, 60));

        CreateDeckDTO dto = buildDto("No Basics", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of(buildStage1Pokemon()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Basic Pokémon")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("evolves from")));
        assertEquals(3, result.getErrors().size());
    }

    @Test
    void validateDeck_missingPreEvolution_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(stage1PokemonId, 1));
        cards.add(buildEntry(basicEnergyId, 59));

        CreateDeckDTO dto = buildDto("Missing Pre-evolution", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildStage1Pokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("evolves from")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Pikachu")));
    }

    @Test
    void validateDeck_withPreEvolution_returnsValid() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 4));
        cards.add(buildEntry(stage1PokemonId, 4));
        cards.add(buildEntry(basicEnergyId, 52));

        CreateDeckDTO dto = buildDto("Full Chain", cards);
        List<CardEntity> entities = List.of(buildBasicPokemon(), buildStage1Pokemon(), buildBasicEnergy());
        when(cardRepository.findAllById(anyCollection())).thenReturn(entities);

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void validateDeck_pokemonEXCountsAsBasic() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(exPokemonId, 1));
        cards.add(buildEntry(basicEnergyId, 59));

        CreateDeckDTO dto = buildDto("EX Deck", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildExPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
        assertEquals(1, result.getBasicPokemonCount());
    }

    @Test
    void validateDeck_multipleErrors_returnsAllErrors() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(stage1PokemonId, 5));

        CreateDeckDTO dto = buildDto("Bad Deck", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of(buildStage1Pokemon()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertEquals(4, result.getErrors().size());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("60 cards")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("copies")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Basic Pokémon")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("evolves from")));
    }

    @Test
    void validateDeck_invalidCardId_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(UUID.randomUUID(), 60));

        CreateDeckDTO dto = buildDto("Missing Cards", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of());

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("card IDs")));
    }

    @Test
    void validateDeck_invalidUuidFormat_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        DeckCardEntryDTO entry = new DeckCardEntryDTO();
        entry.setCardId("not-a-uuid");
        entry.setQuantity(60);
        cards.add(entry);

        CreateDeckDTO dto = buildDto("Bad UUID", cards);

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("invalid format")));
    }

    @Test
    void validateDeck_cardNameCountsCorrect() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 3));
        cards.add(buildEntry(basicEnergyId, 57));

        CreateDeckDTO dto = buildDto("Count Test", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertEquals(3, result.getCardNameCounts().get("Pikachu"));
        assertEquals(57, result.getCardNameCounts().get("Grass Energy"));
    }

    @Test
    void validateDeck_exactly4Copies_returnsValid() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 4));
        cards.add(buildEntry(basicEnergyId, 56));

        CreateDeckDTO dto = buildDto("Boundary 4 Copies", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
        assertEquals(4, result.getCardNameCounts().get("Pikachu"));
    }

    @Test
    void validateDeck_duplicateCardIdEntries_sumsQuantities() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 2));
        cards.add(buildEntry(basicPokemonId, 2));
        cards.add(buildEntry(basicEnergyId, 56));

        CreateDeckDTO dto = buildDto("Duplicate Entries", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), buildBasicEnergy()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertTrue(result.getIsValid());
        assertEquals(4, result.getCardNameCounts().get("Pikachu"));
    }

    @Test
    void validateDeck_specialEnergyLimited_returnsError() {
        UUID specialEnergyId = UUID.randomUUID();
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 1));
        cards.add(buildEntry(specialEnergyId, 59));

        CreateDeckDTO dto = buildDto("Special Energy", cards);
        CardEntity specialEnergy = buildSpecialEnergy();
        specialEnergy.setId(specialEnergyId);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), specialEnergy));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("copies")));
    }

    @Test
    void validateDeck_trainerCardLimited_returnsError() {
        UUID trainerItemId = UUID.randomUUID();
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 1));
        cards.add(buildEntry(trainerItemId, 59));

        CreateDeckDTO dto = buildDto("Trainer Item", cards);
        CardEntity trainerItem = buildTrainerItem();
        trainerItem.setId(trainerItemId);
        when(cardRepository.findAllById(anyCollection())).thenReturn(
                List.of(buildBasicPokemon(), trainerItem));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("copies")));
    }

    @Test
    void validateDeck_mixedValidAndInvalidIds_returnsError() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 30));
        cards.add(buildEntry(UUID.randomUUID(), 30));

        CreateDeckDTO dto = buildDto("Mixed IDs", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of(buildBasicPokemon()));

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("card IDs")));
    }

    @Test
    void validateDeck_combinedInvalidUuidAndMissingId_returnsBothErrors() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        DeckCardEntryDTO badUuid = new DeckCardEntryDTO();
        badUuid.setCardId("not-a-uuid");
        badUuid.setQuantity(30);
        cards.add(badUuid);
        cards.add(buildEntry(UUID.randomUUID(), 30));

        CreateDeckDTO dto = buildDto("Combined Errors", cards);
        when(cardRepository.findAllById(anyCollection())).thenReturn(List.of());

        DeckValidationResultDTO result = validationService.validateDeck(dto);

        assertFalse(result.getIsValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("invalid format")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("do not exist")));
    }

    private List<DeckCardEntryDTO> buildValidDeck() {
        List<DeckCardEntryDTO> cards = new ArrayList<>();
        cards.add(buildEntry(basicPokemonId, 4));
        cards.add(buildEntry(stage1PokemonId, 4));
        cards.add(buildEntry(basicEnergyId, 52));
        return cards;
    }

    private CreateDeckDTO buildDto(String name, List<DeckCardEntryDTO> cards) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(name);
        dto.setCards(cards);
        return dto;
    }

    private DeckCardEntryDTO buildEntry(UUID cardId, int quantity) {
        DeckCardEntryDTO entry = new DeckCardEntryDTO();
        entry.setCardId(cardId.toString());
        entry.setQuantity(quantity);
        return entry;
    }

    private List<CardEntity> buildCardEntities(List<DeckCardEntryDTO> entries) {
        List<CardEntity> entities = new ArrayList<>();
        for (DeckCardEntryDTO entry : entries) {
            UUID id = UUID.fromString(entry.getCardId());
            if (id.equals(basicPokemonId)) {
                entities.add(buildBasicPokemon());
            } else if (id.equals(stage1PokemonId)) {
                entities.add(buildStage1Pokemon());
            } else if (id.equals(basicEnergyId)) {
                entities.add(buildBasicEnergy());
            } else if (id.equals(exPokemonId)) {
                entities.add(buildExPokemon());
            }
        }
        return entities;
    }

    private CardEntity buildBasicPokemon() {
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
        card.setId(basicPokemonId);
        return card;
    }

    private CardEntity buildStage1Pokemon() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-2")
                .name("Raichu")
                .supertype("Pokémon")
                .subtypes(List.of("Stage 1"))
                .hp(90)
                .types(Set.of(EnergyType.LIGHTNING))
                .stage(PokemonStage.STAGE1)
                .evolvesFrom("Pikachu")
                .imageUrl("https://example.com/raichu.png")
                .cardNumber("2")
                .setId("xy1")
                .build();
        card.setId(stage1PokemonId);
        return card;
    }

    private CardEntity buildBasicEnergy() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-energy-grass")
                .name("Grass Energy")
                .supertype("Energy")
                .subtypes(List.of("Basic"))
                .imageUrl("https://example.com/grass-energy.png")
                .cardNumber("001")
                .setId("xy1")
                .build();
        card.setId(basicEnergyId);
        return card;
    }

    private CardEntity buildExPokemon() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-ex-001")
                .name("Venusaur EX")
                .supertype("Pokémon")
                .subtypes(List.of("Basic", "EX"))
                .hp(180)
                .types(Set.of(EnergyType.GRASS))
                .stage(PokemonStage.EX)
                .imageUrl("https://example.com/venusaur-ex.png")
                .cardNumber("002")
                .setId("xy1")
                .build();
        card.setId(exPokemonId);
        return card;
    }

    private CardEntity buildSpecialEnergy() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-special-energy")
                .name("Double Colorless Energy")
                .supertype("Energy")
                .subtypes(List.of("Special"))
                .imageUrl("https://example.com/special-energy.png")
                .cardNumber("002")
                .setId("xy1")
                .build();
        return card;
    }

    private CardEntity buildTrainerItem() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-trainer-item")
                .name("Ultra Ball")
                .supertype("Trainer")
                .subtypes(List.of("Item"))
                .imageUrl("https://example.com/ultra-ball.png")
                .cardNumber("080")
                .setId("xy1")
                .build();
        return card;
    }
}
