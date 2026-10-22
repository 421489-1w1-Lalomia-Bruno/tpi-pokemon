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
import ar.edu.utn.frc.tup.piii.dtos.card.CardDTO;
import ar.edu.utn.frc.tup.piii.enums.EnergyType;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeckMapperTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    private DeckMapper deckMapper;

    private final UUID cardId1 = UUID.randomUUID();
    private final UUID cardId2 = UUID.randomUUID();
    private final UUID deckId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        deckMapper = new DeckMapper(cardRepository, cardMapper);
        lenient().when(cardMapper.toDTO(any(CardEntity.class))).thenReturn(new CardDTO());
    }

    @Test
    void toDto_shouldMapDeckEntityToDeckDTO() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId("player-1");
        entity.setIsActive(true);

        DeckDTO result = deckMapper.toDto(entity);

        assertEquals("My Deck", result.getName());
        assertEquals("player-1", result.getCreatorId());
        assertNotNull(result.getCards());
        assertEquals(2, result.getCards().size());

        DeckCardDTO cardDto = result.getCards().get(0);
        assertEquals(cardId1.toString(), cardDto.getCardId());
        assertEquals("Pikachu", cardDto.getName());
        assertEquals(4, cardDto.getQuantity());
    }

    @Test
    void toCardDto_shouldMapDeckCardEntityToDeckCardDTO() {
        CardEntity card = buildCardEntity(cardId1, "Pikachu");
        DeckCardEntity entity = new DeckCardEntity();
        entity.setCard(card);
        entity.setQuantity(4);

        DeckCardDTO result = deckMapper.toCardDto(entity);

        assertEquals(cardId1.toString(), result.getCardId());
        assertEquals("Pikachu", result.getName());
        assertEquals(4, result.getQuantity());
    }

    @Test
    void toCreateDto_shouldMapDeckEntityBackToCreateDeckDTO() {
        DeckEntity entity = buildDeckEntity("My Deck");
        entity.setId(deckId);
        entity.setCreatorId("player-1");

        CreateDeckDTO result = deckMapper.toCreateDto(entity);

        assertEquals("My Deck", result.getName());
        assertNotNull(result.getCards());
        assertEquals(2, result.getCards().size());

        DeckCardEntryDTO entry = result.getCards().get(0);
        assertEquals(cardId1.toString(), entry.getCardId());
        assertEquals(4, entry.getQuantity());
    }

    @Test
    void toEntity_shouldMapCreateDeckDTOToDeckEntity() {
        CardEntity card1 = buildCardEntity(cardId1, "Pikachu");
        CardEntity card2 = buildCardEntity(cardId2, "Grass Energy");
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(card1));
        when(cardRepository.findById(cardId2)).thenReturn(Optional.of(card2));

        CreateDeckDTO dto = buildCreateDeckDTO("My Deck");

        DeckEntity result = deckMapper.toEntity(dto);

        assertEquals("My Deck", result.getName());
        assertNotNull(result.getCards());
        assertEquals(2, result.getCards().size());

        DeckCardEntity deckCard = result.getCards().get(0);
        assertEquals(card1, deckCard.getCard());
        assertEquals(4, deckCard.getQuantity());
    }

    @Test
    void toEntityList_shouldMapCardEntriesToDeckCardEntities() {
        CardEntity card1 = buildCardEntity(cardId1, "Pikachu");
        CardEntity card2 = buildCardEntity(cardId2, "Grass Energy");
        when(cardRepository.findById(cardId1)).thenReturn(Optional.of(card1));
        when(cardRepository.findById(cardId2)).thenReturn(Optional.of(card2));

        List<DeckCardEntryDTO> entries = List.of(
                buildEntry(cardId1, 4),
                buildEntry(cardId2, 56)
        );

        List<DeckCardEntity> result = deckMapper.toEntityList(entries);

        assertEquals(2, result.size());
        assertEquals(card1, result.get(0).getCard());
        assertEquals(4, result.get(0).getQuantity());
        assertEquals(card2, result.get(1).getCard());
        assertEquals(56, result.get(1).getQuantity());
    }

    @Test
    void toEntityList_shouldThrowWhenCardNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(cardRepository.findById(unknownId)).thenReturn(Optional.empty());

        List<DeckCardEntryDTO> entries = List.of(buildEntry(unknownId, 60));

        assertThrows(EntityNotFoundException.class, () -> deckMapper.toEntityList(entries));
    }

    private DeckEntity buildDeckEntity(String name) {
        CardEntity card1 = buildCardEntity(cardId1, "Pikachu");
        CardEntity card2 = buildCardEntity(cardId2, "Grass Energy");

        DeckCardEntity deckCard1 = new DeckCardEntity();
        deckCard1.setCard(card1);
        deckCard1.setQuantity(4);

        DeckCardEntity deckCard2 = new DeckCardEntity();
        deckCard2.setCard(card2);
        deckCard2.setQuantity(56);

        DeckEntity entity = new DeckEntity();
        entity.setName(name);
        entity.setCards(new ArrayList<>(List.of(deckCard1, deckCard2)));
        return entity;
    }

    private CardEntity buildCardEntity(UUID id, String name) {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-1")
                .name(name)
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

    private CreateDeckDTO buildCreateDeckDTO(String name) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(name);
        dto.setCards(List.of(
                buildEntry(cardId1, 4),
                buildEntry(cardId2, 56)
        ));
        return dto;
    }

    private DeckCardEntryDTO buildEntry(UUID cardId, int quantity) {
        DeckCardEntryDTO entry = new DeckCardEntryDTO();
        entry.setCardId(cardId.toString());
        entry.setQuantity(quantity);
        return entry;
    }
}
