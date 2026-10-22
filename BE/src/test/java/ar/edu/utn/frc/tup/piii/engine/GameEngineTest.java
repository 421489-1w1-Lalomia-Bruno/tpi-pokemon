package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.clients.dtos.AbilityApiDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.AttackApiDTO;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import ar.edu.utn.frc.tup.piii.engine.state.*;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.repositories.*;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameEngineTest {

    private GameStatePersistenceService persistenceService;
    private DeckRepository deckRepository;
    private CardRepository cardRepository;
    private MatchRepository matchRepository;
    private ObjectMapper objectMapper;
    private GameEventPublisher eventPublisher;
    private RuleValidator ruleValidator;
    private UserRepository userRepository;
    private GameEngine engine;

    private static final UUID MATCH_UUID = UUID.randomUUID();
    private static final String MATCH_ID = MATCH_UUID.toString();
    private static final String P1_ID = "player-1";
    private static final String P2_ID = "player-2";

    @BeforeEach
    void setUp() {
        persistenceService = mock(GameStatePersistenceService.class);
        deckRepository = mock(DeckRepository.class);
        cardRepository = mock(CardRepository.class);
        matchRepository = mock(MatchRepository.class);
        objectMapper = mock(ObjectMapper.class);
        eventPublisher = mock(GameEventPublisher.class);
        ruleValidator = mock(RuleValidator.class);
        userRepository = mock(UserRepository.class);
        engine = new GameEngine(persistenceService, deckRepository, cardRepository,
                matchRepository, objectMapper, eventPublisher, ruleValidator, userRepository);
    }

    @Test
    void initializeMatch_createsWaitingState() {
        engine.initializeMatch(MATCH_ID);

        MatchState state = engine.getCurrentState(MATCH_ID);
        assertNotNull(state);
        assertInstanceOf(WaitingState.class, state);
        assertEquals(GamePhase.WAITING, state.getPhase());
    }

    @Test
    void transitionTo_exitsPreviousAndEntersNewState() {
        engine.initializeMatch(MATCH_ID);

        FinishedState finished = new FinishedState(P1_ID);
        engine.transitionTo(MATCH_ID, finished);

        assertSame(finished, engine.getCurrentState(MATCH_ID));
        assertEquals(GamePhase.FINISHED, engine.getCurrentState(MATCH_ID).getPhase());
    }

    @Test
    void processAction_nullState_returnsNull() {
        MatchSnapshot result = engine.processAction(MATCH_ID, P1_ID, "READY", Map.of());

        assertNull(result);
    }

    @Test
    void processAction_validAction_returnsSnapshot() {
        engine.initializeMatch(MATCH_ID);
        when(persistenceService.loadSnapshot(any())).thenReturn(new MatchSnapshot());

        MatchSnapshot result = engine.processAction(MATCH_ID, P1_ID, "READY", Map.of());

        assertNotNull(result);
        verify(persistenceService).loadSnapshot(MATCH_UUID);
    }

    @Test
    void processAction_invalidActionType_throws() {
        engine.initializeMatch(MATCH_ID);

        assertThrows(IllegalArgumentException.class,
                () -> engine.processAction(MATCH_ID, P1_ID, "NOT_A_VALID_ACTION", Map.of()));
    }

    @Test
    void declareLoser_validPlayers_transitionsToFinished() {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setPlayer1Id(P1_ID);
        snapshot.setPlayer2Id(P2_ID);
        when(persistenceService.loadSnapshot(any())).thenReturn(snapshot);

        engine.initializeMatch(MATCH_ID);
        engine.declareLoser(MATCH_ID, P1_ID, "Surrender");

        MatchState state = engine.getCurrentState(MATCH_ID);
        assertInstanceOf(FinishedState.class, state);
        assertEquals(GamePhase.FINISHED, state.getPhase());
    }

    @Test
    void declareLoser_nullSnapshot_doesNothing() {
        when(persistenceService.loadSnapshot(any())).thenReturn(null);

        engine.initializeMatch(MATCH_ID);
        MatchState before = engine.getCurrentState(MATCH_ID);

        engine.declareLoser(MATCH_ID, P1_ID, "Surrender");

        assertSame(before, engine.getCurrentState(MATCH_ID));
    }

    @Test
    void setMatchWinner_validEntity_updatesAndSaves() {
        MatchEntity entity = new MatchEntity();
        when(matchRepository.findById(MATCH_UUID)).thenReturn(Optional.of(entity));

        engine.setMatchWinner(MATCH_ID, P1_ID);

        assertEquals(P1_ID, entity.getWinnerId());
        assertEquals(MatchStatus.FINISHED, entity.getStatus());
        verify(matchRepository).save(entity);
    }

    @Test
    void setMatchWinner_nullEntity_doesNothing() {
        when(matchRepository.findById(MATCH_UUID)).thenReturn(Optional.empty());

        engine.setMatchWinner(MATCH_ID, P1_ID);

        verify(matchRepository, never()).save(any());
    }

    @Test
    void updateMatchStatus_snapshotExists_updatesAndPersists() {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setStatus(MatchStatus.ACTIVE);
        when(persistenceService.loadSnapshot(MATCH_UUID)).thenReturn(snapshot);

        engine.updateMatchStatus(MATCH_ID, MatchStatus.FINISHED);

        assertEquals(MatchStatus.FINISHED, snapshot.getStatus());
        verify(persistenceService).saveSnapshot(snapshot);
    }

    @Test
    void updateMatchStatus_nullSnapshot_doesNothing() {
        when(persistenceService.loadSnapshot(MATCH_UUID)).thenReturn(null);

        engine.updateMatchStatus(MATCH_ID, MatchStatus.FINISHED);

        verify(persistenceService, never()).saveSnapshot(any());
    }

    @Test
    void loadDeckCards_validDeck_returnsMappedCards() {
        UUID deckId = UUID.randomUUID();
        CardEntity cardEntity = new CardEntity();
        cardEntity.setId(UUID.randomUUID());
        cardEntity.setName("Pikachu");
        cardEntity.setSupertype("Pok\u00e9mon");
        cardEntity.setStage(PokemonStage.BASIC);
        cardEntity.setHp(60);
        cardEntity.setRetreatCost(1);
        cardEntity.setTypes(Set.of(EnergyType.LIGHTNING));
        cardEntity.setImageUrl("https://example.com/pikachu.png");
        DeckCardEntity deckCard = DeckCardEntity.builder()
                .card(cardEntity)
                .quantity(2)
                .build();
        DeckEntity deck = DeckEntity.builder()
                .name("Test Deck")
                .cards(List.of(deckCard))
                .build();
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        List<GameCard> cards = engine.loadDeckCards(deckId);

        assertEquals(2, cards.size());
        assertInstanceOf(PokemonCard.class, cards.get(0));
        assertEquals("Pikachu", cards.get(0).getName());
        assertEquals(60, ((PokemonCard) cards.get(0)).getHp());
        assertInstanceOf(PokemonCard.class, cards.get(1));
    }

    @Test
    void loadDeckCards_nullDeck_returnsEmptyList() {
        UUID deckId = UUID.randomUUID();
        when(deckRepository.findById(deckId)).thenReturn(Optional.empty());

        List<GameCard> cards = engine.loadDeckCards(deckId);

        assertTrue(cards.isEmpty());
    }

    @Test
    void getSnapshot_delegatesToPersistence() {
        MatchSnapshot expected = new MatchSnapshot();
        when(persistenceService.loadSnapshot(MATCH_UUID)).thenReturn(expected);

        MatchSnapshot actual = engine.getSnapshot(MATCH_ID);

        assertSame(expected, actual);
        verify(persistenceService).loadSnapshot(MATCH_UUID);
    }

    @Test
    void saveSnapshot_delegatesToPersistence() {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setMatchId(MATCH_UUID);

        engine.saveSnapshot(MATCH_ID, snapshot);

        verify(persistenceService).saveSnapshot(snapshot);
    }

    @Test
    void getMatchEntity_found_returnsEntity() {
        MatchEntity entity = new MatchEntity();
        entity.setPlayer1Id(P1_ID);
        when(matchRepository.findById(MATCH_UUID)).thenReturn(Optional.of(entity));

        MatchEntity result = engine.getMatchEntity(MATCH_ID);

        assertSame(entity, result);
    }

    @Test
    void getMatchEntity_notFound_returnsNull() {
        when(matchRepository.findById(MATCH_UUID)).thenReturn(Optional.empty());

        MatchEntity result = engine.getMatchEntity(MATCH_ID);

        assertNull(result);
    }

    @Test
    void toGameCard_pokemon_mapsCorrectly() throws Exception {
        CardEntity entity = new CardEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setName("Pikachu");
        entity.setSupertype("Pok\u00e9mon");
        entity.setHp(60);
        entity.setStage(PokemonStage.BASIC);
        entity.setTypes(Set.of(EnergyType.LIGHTNING));
        entity.setEvolvesFrom("Pichu");
        entity.setWeaknessType("Fighting");
        entity.setResistanceType("Flying");
        entity.setRetreatCost(1);
        entity.setImageUrl("https://example.com/pikachu.png");
        String attacksJson = "[{\"name\":\"Tackle\"}]";
        String abilitiesJson = "[{\"name\":\"Static\"}]";
        entity.setAttacksJson(attacksJson);
        entity.setAbilitiesJson(abilitiesJson);

        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.defaultInstance());

        AttackApiDTO attackDto = new AttackApiDTO();
        attackDto.setName("Tackle");
        attackDto.setCost(List.of("COLORLESS"));
        attackDto.setDamage("10");
        attackDto.setText("A tackle");
        AbilityApiDTO abilityDto = new AbilityApiDTO();
        abilityDto.setName("Static");
        abilityDto.setType("POKE_BODY");
        abilityDto.setText("May cause paralysis");
        when(objectMapper.readValue(eq(attacksJson), any(JavaType.class))).thenReturn(List.of(attackDto));
        when(objectMapper.readValue(eq(abilitiesJson), any(JavaType.class))).thenReturn(List.of(abilityDto));

        GameCard result = engine.toGameCard(entity);

        assertInstanceOf(PokemonCard.class, result);
        PokemonCard card = (PokemonCard) result;
        assertEquals(id.toString(), card.getId());
        assertEquals("Pikachu", card.getName());
        assertEquals(60, card.getHp());
        assertEquals(PokemonStage.BASIC, card.getStage());
        assertEquals(Set.of(EnergyType.LIGHTNING), card.getTypes());
        assertEquals("Pichu", card.getEvolvesFrom());
        assertEquals("Fighting", card.getWeaknessType());
        assertEquals("Flying", card.getResistanceType());
        assertEquals(1, card.getRetreatCost());
        assertEquals("https://example.com/pikachu.png", card.getImageUrl());
        assertEquals(1, card.getAttacks().size());
        assertEquals("Tackle", card.getAttacks().get(0).getName());
        assertEquals(1, card.getAbilities().size());
        assertEquals("Static", card.getAbilities().get(0).getName());
    }

    @Test
    void toGameCard_energy_returnsEnergyCard() {
        CardEntity entity = new CardEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setName("Fire Energy");
        entity.setSupertype("Energy");
        entity.setTypes(Set.of(EnergyType.FIRE));

        GameCard result = engine.toGameCard(entity);

        assertInstanceOf(EnergyCard.class, result);
        EnergyCard card = (EnergyCard) result;
        assertEquals(id.toString(), card.getId());
        assertEquals("Fire Energy", card.getName());
        assertEquals(EnergyType.FIRE, card.getEnergyType());
        assertEquals(List.of(EnergyType.FIRE), card.getProvidedEnergies());
        assertFalse(card.isSpecial());
    }

    @Test
    void toGameCard_trainer_returnsTrainerCard() {
        CardEntity entity = new CardEntity();
        UUID id = UUID.randomUUID();
        entity.setId(id);
        entity.setName("Potion");
        entity.setSupertype("Trainer");
        entity.setSubtypes(List.of("ITEM"));
        entity.setRules("Heal 20 damage");

        GameCard result = engine.toGameCard(entity);

        assertInstanceOf(TrainerCard.class, result);
        TrainerCard card = (TrainerCard) result;
        assertEquals(id.toString(), card.getId());
        assertEquals("Potion", card.getName());
        assertEquals(TrainerSubtype.ITEM, card.getSubtype());
        assertEquals("Heal 20 damage", card.getEffect());
    }

    @Test
    void toGameCard_unknownSupertype_returnsNull() {
        CardEntity entity = new CardEntity();
        entity.setSupertype("Unknown");

        GameCard result = engine.toGameCard(entity);

        assertNull(result);
    }

    @Test
    void toGameCard_pokemonBlankJson_returnsEmptyAttacksAndAbilities() throws Exception {
        CardEntity entity = new CardEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Pikachu");
        entity.setSupertype("Pok\u00e9mon");
        entity.setHp(60);
        entity.setStage(PokemonStage.BASIC);
        entity.setTypes(Set.of(EnergyType.LIGHTNING));
        entity.setRetreatCost(1);
        entity.setImageUrl("https://example.com/pikachu.png");
        entity.setAttacksJson("");
        entity.setAbilitiesJson(null);

        GameCard result = engine.toGameCard(entity);

        assertInstanceOf(PokemonCard.class, result);
        PokemonCard card = (PokemonCard) result;
        assertTrue(card.getAttacks().isEmpty());
        assertTrue(card.getAbilities().isEmpty());
        verify(objectMapper, never()).readValue(anyString(), any(JavaType.class));
    }

    @Test
    void toGameCard_pokemonInvalidJson_returnsEmptyAttacksAndAbilities() throws Exception {
        CardEntity entity = new CardEntity();
        entity.setId(UUID.randomUUID());
        entity.setName("Pikachu");
        entity.setSupertype("Pok\u00e9mon");
        entity.setHp(60);
        entity.setStage(PokemonStage.BASIC);
        entity.setTypes(Set.of(EnergyType.LIGHTNING));
        entity.setRetreatCost(1);
        entity.setImageUrl("https://example.com/pikachu.png");
        entity.setAttacksJson("invalid");
        entity.setAbilitiesJson("invalid");

        when(objectMapper.getTypeFactory()).thenReturn(TypeFactory.defaultInstance());
        when(objectMapper.readValue(anyString(), any(JavaType.class)))
                .thenThrow(new JsonProcessingException("error") { private static final long serialVersionUID = 1L; });

        GameCard result = engine.toGameCard(entity);

        assertInstanceOf(PokemonCard.class, result);
        PokemonCard card = (PokemonCard) result;
        assertTrue(card.getAttacks().isEmpty());
        assertTrue(card.getAbilities().isEmpty());
    }

    @Test
    void loadDeckCards_mixedCardTypes_returnsMappedCards() {
        UUID deckId = UUID.randomUUID();
        CardEntity pokemonEntity = new CardEntity();
        pokemonEntity.setId(UUID.randomUUID());
        pokemonEntity.setName("Pikachu");
        pokemonEntity.setSupertype("Pok\u00e9mon");
        pokemonEntity.setStage(PokemonStage.BASIC);
        pokemonEntity.setHp(60);
        pokemonEntity.setTypes(Set.of(EnergyType.LIGHTNING));
        pokemonEntity.setRetreatCost(1);
        pokemonEntity.setImageUrl("https://example.com/pikachu.png");

        CardEntity energyEntity = new CardEntity();
        energyEntity.setId(UUID.randomUUID());
        energyEntity.setName("Fire Energy");
        energyEntity.setSupertype("Energy");
        energyEntity.setTypes(Set.of(EnergyType.FIRE));

        CardEntity trainerEntity = new CardEntity();
        trainerEntity.setId(UUID.randomUUID());
        trainerEntity.setName("Potion");
        trainerEntity.setSupertype("Trainer");
        trainerEntity.setSubtypes(List.of("ITEM"));
        trainerEntity.setRules("Heal 20 damage");

        DeckCardEntity pokeDeckCard = DeckCardEntity.builder().card(pokemonEntity).quantity(1).build();
        DeckCardEntity energyDeckCard = DeckCardEntity.builder().card(energyEntity).quantity(2).build();
        DeckCardEntity trainerDeckCard = DeckCardEntity.builder().card(trainerEntity).quantity(1).build();
        DeckEntity deck = DeckEntity.builder().name("Mixed Deck").cards(List.of(pokeDeckCard, energyDeckCard, trainerDeckCard)).build();
        when(deckRepository.findById(deckId)).thenReturn(Optional.of(deck));

        List<GameCard> cards = engine.loadDeckCards(deckId);

        assertEquals(4, cards.size());
        assertInstanceOf(PokemonCard.class, cards.get(0));
        assertInstanceOf(EnergyCard.class, cards.get(1));
        assertInstanceOf(EnergyCard.class, cards.get(2));
        assertInstanceOf(TrainerCard.class, cards.get(3));
        assertEquals("Pikachu", cards.get(0).getName());
        assertEquals("Fire Energy", cards.get(1).getName());
        assertEquals("Potion", cards.get(3).getName());
    }

    @Test
    void processAction_nonNullState_nullSnapshot_returnsNull() {
        engine.initializeMatch(MATCH_ID);
        when(persistenceService.loadSnapshot(any())).thenReturn(null);

        MatchSnapshot result = engine.processAction(MATCH_ID, P1_ID, "READY", Map.of());

        assertNull(result);
    }

    @Test
    void declareLoser_player2Loser_transitionsToFinished() {
        MatchSnapshot snapshot = new MatchSnapshot();
        snapshot.setPlayer1Id(P1_ID);
        snapshot.setPlayer2Id(P2_ID);
        when(persistenceService.loadSnapshot(any())).thenReturn(snapshot);
        engine.initializeMatch(MATCH_ID);

        engine.declareLoser(MATCH_ID, P2_ID, "Surrender");

        MatchState state = engine.getCurrentState(MATCH_ID);
        assertInstanceOf(FinishedState.class, state);
        assertEquals(GamePhase.FINISHED, state.getPhase());
    }
}
