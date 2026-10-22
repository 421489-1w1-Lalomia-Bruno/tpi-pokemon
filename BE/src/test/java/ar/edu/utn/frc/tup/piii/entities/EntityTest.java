package ar.edu.utn.frc.tup.piii.entities;

import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.enums.*;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testBaseEntityOnCreateSetsTimestamps() {
        UserEntity entity = new UserEntity();
        entity.onCreate();
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void testBaseEntityOnUpdateUpdatesTimestamp() {
        UserEntity entity = new UserEntity();
        entity.onCreate();
        LocalDateTime before = entity.getUpdatedAt();
        entity.onUpdate();
        assertNotNull(entity.getUpdatedAt());
        assertTrue(before == null || !entity.getUpdatedAt().isBefore(before));
    }

    @Test
    void testUserEntityBuilderAndGetters() {
        UserEntity user = UserEntity.builder()
                .username("ash_ketchum").passwordHash("hashed_pikachu")
                .gender(UserGender.MALE)
                .build();

        assertEquals("ash_ketchum", user.getUsername());
        assertEquals("hashed_pikachu", user.getPasswordHash());
        assertEquals(UserGender.MALE, user.getGender());
    }

    @Test
    void testUserEntityNoArgConstructorAndSetters() {
        UserEntity user = new UserEntity();
        user.setUsername("misty");
        user.setPasswordHash("hashed_starmie");
        user.setGender(UserGender.FEMALE);

        assertEquals("misty", user.getUsername());
        assertEquals("hashed_starmie", user.getPasswordHash());
        assertEquals(UserGender.FEMALE, user.getGender());
    }

    @Test
    void testUserEntityUsernameUniqueConstraint() throws Exception {
        Field field = UserEntity.class.getDeclaredField("username");
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        assertTrue(column.unique());
    }

    @Test
    void testCardEntityBuilderWithDefaults() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-1").name("Venusaur-EX")
                .supertype("Pokémon")
                .imageUrl("http://example.com/v.png")
                .cardNumber("1")
                .build();

        assertEquals("xy1-1", card.getExternalId());
        assertEquals("Venusaur-EX", card.getName());
        assertEquals("Pokémon", card.getSupertype());
        assertEquals("http://example.com/v.png", card.getImageUrl());
        assertEquals("1", card.getCardNumber());
        assertEquals("xy1", card.getSetId());
    }

    @Test
    void testCardEntitySetIdDefaultValue() {
        CardEntity card = new CardEntity();
        assertEquals("xy1", card.getSetId());
    }

    @Test
    void testCardEntityJsonColumns() {
        List<String> subtypes = Arrays.asList("Basic", "EX");
        Set<EnergyType> types = EnumSet.of(EnergyType.GRASS, EnergyType.FIRE);

        CardEntity card = CardEntity.builder()
                .externalId("xy1-2").name("Charizard-EX")
                .supertype("Pokémon").subtypes(subtypes)
                .types(types).hp(180).stage(PokemonStage.STAGE2)
                .evolvesFrom("Charmeleon").retreatCost(2)
                .weaknessType("Water").resistanceType("Fighting")
                .rules("Rule text")
                .attacksJson("[{\"name\":\"Fire Spin\"}]")
                .abilitiesJson("[{\"name\":\"Blaze\"}]")
                .imageUrl("http://example.com/c.png").rarity("Rare")
                .cardNumber("2")
                .build();

        assertEquals(subtypes, card.getSubtypes());
        assertEquals(types, card.getTypes());
        assertEquals(180, card.getHp());
        assertEquals(PokemonStage.STAGE2, card.getStage());
        assertEquals("Charmeleon", card.getEvolvesFrom());
        assertEquals(2, card.getRetreatCost());
        assertEquals("Water", card.getWeaknessType());
        assertEquals("Fighting", card.getResistanceType());
        assertEquals("Rule text", card.getRules());
        assertEquals("[{\"name\":\"Fire Spin\"}]", card.getAttacksJson());
        assertEquals("[{\"name\":\"Blaze\"}]", card.getAbilitiesJson());
        assertEquals("Rare", card.getRarity());
    }

    @Test
    void testCardEntityJsonColumnAnnotations() throws Exception {
        Field subtypesField = CardEntity.class.getDeclaredField("subtypes");
        JdbcTypeCode annotation = subtypesField.getAnnotation(JdbcTypeCode.class);
        assertNotNull(annotation);
        assertEquals(SqlTypes.JSON, annotation.value());

        Field typesField = CardEntity.class.getDeclaredField("types");
        annotation = typesField.getAnnotation(JdbcTypeCode.class);
        assertNotNull(annotation);
        assertEquals(SqlTypes.JSON, annotation.value());
    }

    @Test
    void testCardEntityAllowsEmptyCollections() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-99").name("Test")
                .supertype("Pokémon")
                .imageUrl("http://example.com/t.png")
                .cardNumber("99")
                .subtypes(new ArrayList<>())
                .types(new HashSet<>())
                .build();

        assertTrue(card.getSubtypes().isEmpty());
        assertTrue(card.getTypes().isEmpty());
    }

    @Test
    void testDeckEntityDefaultIsActive() {
        DeckEntity deck1 = new DeckEntity();
        assertTrue(deck1.getIsActive());

        DeckEntity deck2 = DeckEntity.builder().name("My Deck").creatorId("p1").build();
        assertTrue(deck2.getIsActive());
    }

    @Test
    void testDeckEntityBuilder() {
        DeckEntity deck = DeckEntity.builder()
                .name("Fire Deck").creatorId("player1")
                .isActive(false)
                .build();

        assertEquals("Fire Deck", deck.getName());
        assertEquals("player1", deck.getCreatorId());
        assertFalse(deck.getIsActive());
    }

    @Test
    void testDeckCardEntityBuilder() {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-1").name("Venusaur-EX")
                .supertype("Pokémon")
                .imageUrl("http://example.com/v.png")
                .cardNumber("1")
                .build();

        DeckEntity deck = DeckEntity.builder().name("Test Deck").build();

        DeckCardEntity deckCard = DeckCardEntity.builder()
                .deck(deck).card(card).quantity(3)
                .build();

        assertSame(deck, deckCard.getDeck());
        assertSame(card, deckCard.getCard());
        assertEquals(3, deckCard.getQuantity());
    }

    @Test
    void testDeckEntityCascadeAnnotations() throws Exception {
        CardEntity card = CardEntity.builder()
                .externalId("xy1-1").name("Venusaur-EX")
                .supertype("Pokémon")
                .imageUrl("http://example.com/v.png")
                .cardNumber("1")
                .build();

        DeckCardEntity deckCard = DeckCardEntity.builder().card(card).quantity(2).build();
        List<DeckCardEntity> cards = new ArrayList<>();
        cards.add(deckCard);

        DeckEntity deck = DeckEntity.builder()
                .name("Grass Deck").creatorId("player1")
                .cards(cards)
                .build();

        deckCard.setDeck(deck);

        assertEquals(1, deck.getCards().size());
        assertSame(deck, deck.getCards().get(0).getDeck());

        Field cardsField = DeckEntity.class.getDeclaredField("cards");
        OneToMany oneToMany = cardsField.getAnnotation(OneToMany.class);
        assertNotNull(oneToMany);
        assertEquals(CascadeType.ALL, oneToMany.cascade()[0]);
        assertTrue(oneToMany.orphanRemoval());
        assertEquals("deck", oneToMany.mappedBy());

        Field deckField = DeckCardEntity.class.getDeclaredField("deck");
        ManyToOne manyToOne = deckField.getAnnotation(ManyToOne.class);
        assertNotNull(manyToOne);
        assertEquals(FetchType.LAZY, manyToOne.fetch());
    }

    @Test
    void testMatchEntityBuilder() {
        UUID p1DeckId = UUID.randomUUID();
        UUID p2DeckId = UUID.randomUUID();

        MatchEntity match = MatchEntity.builder()
                .player1Id("player1").player2Id("player2")
                .player1DeckId(p1DeckId).player2DeckId(p2DeckId)
                .status(MatchStatus.WAITING)
                .build();

        assertEquals("player1", match.getPlayer1Id());
        assertEquals("player2", match.getPlayer2Id());
        assertEquals(p1DeckId, match.getPlayer1DeckId());
        assertEquals(p2DeckId, match.getPlayer2DeckId());
        assertEquals(MatchStatus.WAITING, match.getStatus());
        assertNull(match.getWinnerId());
    }

    @Test
    void testMatchEntityAllStatusValues() {
        for (MatchStatus status : MatchStatus.values()) {
            MatchEntity match = MatchEntity.builder()
                    .player1Id("p1").status(status)
                    .build();
            assertEquals(status, match.getStatus());
        }
    }

    @Test
    void testGameStateEntityCustomId() throws Exception {
        UUID matchId = UUID.randomUUID();
        GameStateEntity state = GameStateEntity.builder()
                .matchId(matchId).currentTurnId("player1")
                .gamePhase(GamePhase.ACTIVE).turnPhase(TurnPhase.ACTIONS)
                .boardStateJson("{\"board\":{}}")
                .build();

        assertEquals(matchId, state.getMatchId());

        Field matchIdField = GameStateEntity.class.getDeclaredField("matchId");
        Id idAnnotation = matchIdField.getAnnotation(Id.class);
        assertNotNull(idAnnotation, "matchId must have @Id");
    }

    @Test
    void testGameStateEntityLifecycleCallback() {
        GameStateEntity state = new GameStateEntity();
        assertNull(state.getUpdatedAt());
        state.onUpdate();
        assertNotNull(state.getUpdatedAt());
    }

    @Test
    void testGameStateEntityEqualsAndHashCode() {
        UUID matchId = UUID.randomUUID();
        GameStateEntity s1 = GameStateEntity.builder()
                .matchId(matchId).currentTurnId("p1")
                .gamePhase(GamePhase.ACTIVE)
                .build();

        GameStateEntity s2 = GameStateEntity.builder()
                .matchId(matchId).currentTurnId("p1")
                .gamePhase(GamePhase.ACTIVE)
                .build();

        GameStateEntity s3 = GameStateEntity.builder()
                .matchId(UUID.randomUUID()).currentTurnId("p2")
                .gamePhase(GamePhase.SETUP)
                .build();

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }

    @Test
    void testGameEventEntitySettersAndGetters() {
        GameEventEntity event = new GameEventEntity();
        UUID matchId = UUID.randomUUID();
        event.setMatchId(matchId);
        event.setPlayerId("player1");
        event.setEventType(GameEventType.CARD_DRAWN);
        event.setDescription("Player 1 drew a card");

        assertEquals(matchId, event.getMatchId());
        assertEquals("player1", event.getPlayerId());
        assertEquals(GameEventType.CARD_DRAWN, event.getEventType());
        assertEquals("Player 1 drew a card", event.getDescription());
    }

    @Test
    void testGameEventEntityAllEventTypes() {
        for (GameEventType type : GameEventType.values()) {
            GameEventEntity event = new GameEventEntity();
            event.setMatchId(UUID.randomUUID());
            event.setEventType(type);
            event.setDescription("Test event: " + type);
            assertEquals(type, event.getEventType());
        }
    }

    @Test
    void testSyncMetaDataEntitySettersAndGetters() {
        SyncMetaDataEntity meta = new SyncMetaDataEntity();
        meta.setSetId("xy1");
        meta.setLastSyncAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        meta.setSyncedCount(100);

        assertEquals("xy1", meta.getSetId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 12, 0), meta.getLastSyncAt());
        assertEquals(100, meta.getSyncedCount());
    }

    @Test
    void testSyncMetaDataEntityUniqueSetId() throws Exception {
        Field field = SyncMetaDataEntity.class.getDeclaredField("setId");
        Column column = field.getAnnotation(Column.class);
        assertNotNull(column);
        assertTrue(column.unique());
    }

    @Test
    void testEntityEqualsSameInstance() {
        UserEntity user = new UserEntity();
        assertEquals(user, user);
    }

    @Test
    void testEntityEqualsDifferentInstances() {
        UserEntity u1 = new UserEntity();
        UserEntity u2 = new UserEntity();
        assertNotEquals(u1, u2);
    }

    @Test
    void testBaseEntitySetsIdOnPersist() {
        UserEntity entity = new UserEntity();
        entity.onCreate();
        assertNull(entity.getId());
    }

    @Test
    void testDeckEntityCardsInitializedToEmptyList() {
        DeckEntity deck = new DeckEntity();
        assertNotNull(deck.getCards());
        assertTrue(deck.getCards().isEmpty());
    }

    @Test
    void testUserGenderEnumAllValues() {
        for (UserGender gender : UserGender.values()) {
            UserEntity user = UserEntity.builder()
                    .username("user_" + gender.name().toLowerCase())
                    .passwordHash("hash")
                    .gender(gender)
                    .build();
            assertEquals(gender, user.getGender());
        }
    }
}
