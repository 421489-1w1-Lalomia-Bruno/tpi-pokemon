---
name: jpa-entity
description: Implements JPA entities, DTOs, and database schema for the Pokemon TCG project. Use when creating or editing files under entities/, dtos/, repositories/, or discussing database schema, persistence, migrations. Triggers on entity, JPA, DTO, repository, database, table, schema, migration, persistence.
---

# JPA Entity Skill - Pokemon TCG

You are implementing the persistence layer for a Pokemon Trading Card Game application using Spring Data JPA.

## Database Configuration

### Development (H2)
- URL: `jdbc:h2:mem:test;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL`
- Driver: `org.h2.Driver`
- Username: `sa`, Password: (empty)
- Dialect: `org.hibernate.dialect.H2Dialect`
- `spring.jpa.defer-datasource-initialization=true`
- Needs: `spring.jpa.hibernate.ddl-auto=update`

### Production (PostgreSQL)
- URL: `jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`
- Driver: `org.postgresql.Driver`
- Dialect: `org.hibernate.dialect.PostgreSQLDialect`
- `spring.jpa.hibernate.ddl-auto=validate`
- Credentials via environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

## Entity Definitions

### BaseEntity

All entities extend `BaseEntity` which provides:

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### CardEntity

```java
@Entity
@Table(name = "cards")
public class CardEntity extends BaseEntity {
    @Column(name = "external_id", unique = true, nullable = false, length = 50)
    private String externalId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "supertype", nullable = false, length = 30)
    private String supertype;              // "Pokémon", "Trainer", "Energy"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subtypes", columnDefinition = "JSONB")
    private List<String> subtypes;         // ["Basic","EX"], ["Stage 1"], ["Supporter"], etc.

    @Column(name = "hp")
    private Integer hp;                    // null for Trainer/Energy

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "types", columnDefinition = "JSONB")
    private Set<EnergyType> types;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 20)
    private PokemonStage stage;            // Derived from subtypes

    @Column(name = "evolves_from", length = 100)
    private String evolvesFrom;            // Name of pre-evolution (nullable). Lookup by name.

    @Column(name = "retreat_cost")
    private Integer retreatCost;

    @Column(name = "weakness_type", length = 30)
    private String weaknessType;           // EnergyType name as String (nullable)

    @Column(name = "resistance_type", length = 30)
    private String resistanceType;          // EnergyType name as String (nullable)

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "attacks_json", columnDefinition = "TEXT")
    private String attacksJson;             // Raw JSON from API

    @Column(name = "abilities_json", columnDefinition = "TEXT")
    private String abilitiesJson;           // Raw JSON from API (nullable)

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "rarity", length = 50)
    private String rarity;

    @Column(name = "card_number", length = 20)
    private String cardNumber;

    @Column(name = "set_id", length = 20)
    @Builder.Default
    private String setId = "xy1";
}
```

### DeckEntity

```java
@Entity
@Table(name = "decks")
public class DeckEntity extends BaseEntity {
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "creator_id", length = 100)
    private String creatorId;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeckCardEntity> cards = new ArrayList<>();
}
```

### DeckCardEntity

```java
@Entity
@Table(name = "deck_cards")
public class DeckCardEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private CardEntity card;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
```

Note: Composite unique constraint on `(deck_id, card_id)`.

### MatchEntity

```java
@Entity
@Table(name = "matches")
public class MatchEntity extends BaseEntity {
    @Column(name = "player_1_id", nullable = false, length = 100)
    private String player1Id;

    @Column(name = "player_2_id", length = 100)
    private String player2Id;

    @Column(name = "player_1_deck_id")
    private UUID player1DeckId;

    @Column(name = "player_2_deck_id")
    private UUID player2DeckId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.WAITING;

    @Column(name = "winner_id", length = 100)
    private String winnerId;
}
```

Note: `MatchStatus` is an enum: `WAITING, SETUP, ACTIVE, FINISHED, ABANDONED` (distinct from `GamePhase` in engine). Includes deck FKs and ABANDONED status.

### GameStateEntity

```java
@Entity
@Table(name = "game_states")
public class GameStateEntity {
    @Id
    private UUID id;

    @Column(name = "match_id", unique = true, nullable = false)
    private UUID matchId;

    @Column(name = "current_turn_id", length = 100)
    private String currentTurnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_phase", length = 20)
    private GamePhase gamePhase;

    @Enumerated(EnumType.STRING)
    @Column(name = "turn_phase", length = 20)
    private TurnPhase turnPhase;

    @Column(name = "board_state_json", columnDefinition = "TEXT")
    private String boardStateJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Note: A single `boardStateJson` column stores the full `MatchSnapshot` atomically. This ensures persistence is atomic and keeps WebSocket response times under 50ms (RNF-01).

### GameEventEntity

```java
@Entity
@Table(name = "game_events")
public class GameEventEntity extends BaseEntity {
    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @Column(name = "player_id", length = 100)
    private String playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private GameEventType eventType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
```

Note: `game_events` is a separate table for immutable audit logging. Events are NOT embedded inside the game state snapshot to keep payload size small for WebSocket transmission.

## Enums

All enums are in `ar.edu.utn.frc.tup.piii.enums`:

| Enum | Values |
|---|---|
| `CardType` | `POKEMON, ENERGY, TRAINER` |
| `EnergyType` | `FIRE, WATER, GRASS, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, METAL, FAIRY, DRAGON, COLORLESS` |
| `GamePhase` | `WAITING, SETUP, ACTIVE, FINISHED` |
| `GameZone` | `HAND, DECK, DISCARD, ACTIVE, BENCH, PRIZE, STADIUM` |
| `PokemonStage` | `BASIC, STAGE1, STAGE2, EX, MEGA` |
| `SpecialCondition` | `ASLEEP, BURNED, CONFUSED, PARALYZED, POISONED` |
| `TrainerSubtype` | `ITEM, SUPPORTER, STADIUM, TOOL, ACE_SPEC, PLASMA` |
| `TurnPhase` | `DRAW, ACTIONS, ATTACK, BETWEEN_TURNS` |
| `MatchStatus` | `WAITING, SETUP, ACTIVE, FINISHED, ABANDONED` |

Note: `GamePhase` (4 values) is for the engine's in-memory state machine. `MatchStatus` (5 values) is for persistence in `MatchEntity` — it includes `ABANDONED` for surrendered/abandoned matches. `TurnPhase` (4 values) includes `BETWEEN_TURNS` for the between-turns processing phase within `ActiveState`.

All enums should be stored as `STRING` in the database (`@Enumerated(EnumType.STRING)`).

## Database Indexes

Per `docs/sdd/07-base-datos.md`:
- `idx_cards_search_name` on `cards(name)` — card search
- `idx_cards_sync_ext` on `cards(external_id)` — sync deduplication
- `idx_decks_creator` on `decks(creator_id, is_active)` — user deck listing
- `idx_matches_lobby` on `matches(status)` WHERE status='WAITING' — lobby queries
- `idx_game_events_stream` on `game_events(match_id, timestamp ASC)` — reconnection event replay

## Repositories

All repositories extend `JpaRepository<EntityName, UUID>`:

```java
@Repository
public interface CardRepository extends JpaRepository<CardEntity, UUID> {
    List<CardEntity> findBySetId(String setId);
    Page<CardEntity> findByStage(PokemonStage stage, Pageable pageable);
    Optional<CardEntity> findByExternalId(String externalId);
    List<CardEntity> findByNameContainingIgnoreCase(String name);
}
```

## DTO Mapping Rules

- Entity → DTO: Use ModelMapper via mapper classes
- DTO → Entity: Use ModelMapper via mapper classes
- For partial updates: Use `mergerMapper` (only maps non-null fields)
- GameState serialization: Use Jackson ObjectMapper to/from JSON strings (single `boardStateJson` column)

## Migrations (Flyway)

Migrations go in `BE/src/main/resources/db/migration/`:
- `V1__create_cards_table.sql`
- `V2__create_decks_tables.sql`
- `V3__create_matches_table.sql`
- `V4__create_game_states_and_events_tables.sql`
- `V5__seed_xy1_cards.sql`

For now, H2 auto-creates tables via `ddl-auto=update`. Migrations are required for production PostgreSQL deployment.

## Checklist

When implementing entities or DTOs:
- [ ] Entity extends `BaseEntity` (except `GameStateEntity` and `GameEventEntity` which have custom ID handling)
- [ ] Lombok `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` present
- [ ] `@Entity` and `@Table(name = "...")` annotations
- [ ] Enum fields use `@Enumerated(EnumType.STRING)`
- [ ] JSON fields use `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "JSON"` or `columnDefinition = "TEXT"`
- [ ] Relationships use correct cascade and fetch types
- [ ] Indexes match the database schema document
- [ ] UUID as primary key type
- [ ] DTO uses validation annotations (`@NotBlank`, etc.) where appropriate
- [ ] Mapper class injects `ModelMapper` and has mapToDto/mapToEntity methods
- [ ] `GameStateEntity` stores snapshot in single JSONB column
- [ ] `GameEventEntity` is in separate table for audit logging
- [ ] `MatchEntity` includes `player1DeckId` and `player2DeckId` FKs
- [ ] `MatchStatus` enum includes ABANDONED status (separate from GamePhase)
- [ ] `CardEntity` has `evolvesFrom` field (String, nullable)
- [ ] `CardEntity` has `abilitiesJson` field (TEXT, nullable)
- [ ] `GameStateEntity` uses `boardStateJson` column (not `snapshot`)
- [ ] `GameEventEntity` has `playerId` as nullable (system events have no player)
- [ ] Subtypes column uses JSONB type