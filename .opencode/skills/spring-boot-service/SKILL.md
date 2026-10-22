---
name: spring-boot-service
description: Implements Spring Boot backend services, controllers, DTOs, mappers, and repositories for the Pokemon TCG project. Use when creating or editing files under BE/src, particularly controllers/, services/, dtos/, mappers/, repositories/, configs/, exceptions/. Triggers on controller, service, DTO, mapper, repository, endpoint, REST API, Spring Boot.
---

# Spring Boot Service Skill - Pokemon TCG

You are implementing the Spring Boot backend for a Pokemon Trading Card Game application. Follow these conventions strictly.

## Project Setup

- **Java 21** with Spring Boot 3.x / 4.0
- **Base package**: `ar.edu.utn.frc.tup.piii`
- **Build**: Maven
- **DB (dev)**: H2 in-memory with `MODE=PostgreSQL`
- **DB (prod)**: PostgreSQL with environment variables
- **Dependencies**: Lombok, ModelMapper, SpringDoc (Swagger), Validation, Actuator, JPA, Spring Security + JWT, Spring WebSocket (STOMP), Flyway

## Package Structure

```
ar.edu.utn.frc.tup.piii/
├── Application.java
├── configs/       → CorsConfig, MappersConfig, SpringDocConfig, WebSocketConfig, SecurityConfig
├── controllers/   → CardController, DeckController, MatchController, PingController
├── dtos/
│   ├── card/      → CardDTO, CardFilterDTO
│   ├── common/    → ErrorApi
│   ├── deck/      → CreateDeckDTO, DeckDTO, DeckValidationResultDTO
│   └── match/     → CreateMatchDTO, MatchDTO, JoinMatchDTO
├── entities/      → BaseEntity, CardEntity, DeckCardEntity, DeckEntity, MatchEntity, GameStateEntity, GameEventEntity
├── engine/         → Game engine (separate skill)
├── enums/          → CardType, EnergyType, GamePhase, GameZone, MatchStatus, PokemonStage, SpecialCondition, TrainerSubtype, TurnPhase
├── exceptions/     → DeckValidationException, EntityNotFoundException, GameRuleViolationException, GlobalExceptionHandler, InvalidActionException
├── mappers/        → CardMapper, DeckMapper, MatchMapper
├── repositories/   → CardRepository, DeckRepository, GameStateRepository, MatchRepository, GameEventRepository
├── services/       → CardService, DeckService, MatchService, GameStatePersistenceService
└── websocket/      → GameWebSocketHandler, listeners/, messages/
```

## Coding Conventions

### Entities

- Extend `BaseEntity` (which provides `id` as UUID and audit fields)
- Use Lombok `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`
- Use JPA annotations (`@Entity`, `@Table`, `@Column`, etc.)
- JSON fields use `@JdbcTypeCode(SqlTypes.JSON)` or `columnDefinition = "TEXT"` for JSON data
- All entity classes currently exist as stubs (empty classes `{}`) - fill them in

### DTOs

- Use Lombok `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`
- Use `@NotBlank`, `@NotEmpty`, `@NotNull` from `jakarta.validation.constraints` for validation
- Keep DTOs in sub-packages by domain: `dtos/card/`, `dtos/deck/`, `dtos/match/`
- Use `ErrorApi` for error responses (already implemented)

### Repositories

- Extend `JpaRepository<EntityName, UUID>`
- Use Spring Data JPA derived query methods
- Custom queries use `@Query` with JPQL

```java
@Repository
public interface CardRepository extends JpaRepository<CardEntity, UUID> {
    List<CardEntity> findBySetId(String setId);
    Page<CardEntity> findByStage(PokemonStage stage, Pageable pageable);
    Optional<CardEntity> findByExternalId(String externalId);
    List<CardEntity> findByNameContainingIgnoreCase(String name);
}
```

### Mappers

- Inject the `ModelMapper` bean for simple mappings
- Use `@Qualifier("mergerMapper")` ModelMapper bean for update operations (only maps non-null fields)
- Custom mapping logic goes in the mapper class itself as methods

```java
@Component
public class CardMapper {
    private final ModelMapper modelMapper;

    public CardMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public CardDTO mapToDto(CardEntity entity) {
        return modelMapper.map(entity, CardDTO.class);
    }
}
```

### Services

- Annotate with `@Service`
- Use `@Transactional` for write operations
- Throw custom exceptions: `EntityNotFoundException`, `DeckValidationException`, `GameRuleViolationException`, `InvalidActionException`
- Inject repositories and mappers via constructor injection

```java
@Service
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public CardService(CardRepository cardRepository, CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }
}
```

### Controllers

- Annotate with `@RestController` and `@RequestMapping("/api/resource-name")`
- Use `@Operation`, `@ApiResponses`, `@ApiResponse` from SpringDoc
- Validate input with `@Valid`
- Return appropriate HTTP status codes:
  - `200 OK` for GET/PUT
  - `201 Created` for POST
  - `204 No Content` for DELETE
  - `400 Bad Request` for validation errors
  - `404 Not Found` for missing entities
  - `409 Conflict` for invalid game actions

```java
@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @Operation(summary = "Get all cards", description = "Returns paginated list of cards")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful operation"),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
            content = @Content(schema = @Schema(implementation = ErrorApi.class)))
    })
    @GetMapping
    public ResponseEntity<Page<CardDTO>> getAllCards(CardFilterDTO filter) {
        return ResponseEntity.ok(cardService.getAllCards(filter));
    }
}
```

### Exceptions

All custom exceptions already exist as stubs. Fill them in:

```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
```

`GlobalExceptionHandler` should use `@RestControllerAdvice` and map:
- `EntityNotFoundException` → 404
- `DeckValidationException` → 400
- `GameRuleViolationException` → 409
- `InvalidActionException` → 409

## Security

### JWT Authentication (Phase Approach)

Spring Security protects REST and WebSocket endpoints using a phased approach:

**Phase 1 (Weeks 1-3): X-Player-Id Header**
- A `PlayerIdInterceptor` (HandlerInterceptor) intercepts all requests to `/api/decks/**` and `/api/matches/**`
- Extracts `X-Player-Id` from the HTTP header and injects it as a request attribute
- Controllers read it via `@RequestAttribute("playerId") String playerId`
- Returns `401 Unauthorized` if header is missing or empty

**Phase 2 (Weeks 4-5): JWT Migration**
- Replace `PlayerIdInterceptor` with `OncePerRequestFilter` that validates JWT tokens
- JWT secret from `JWT_SECRET` environment variable
- Controllers don't change — they still read `playerId` from request attribute
- WebSocket: STOMP `CONNECT` frame carries JWT in `Authorization` header. A `ChannelInterceptor` validates the token and assigns the authenticated `Principal` to the STOMP session.

### Anti-Cheat

- **Data Masking**: Before sending `MatchSnapshot`, a filter replaces opponent's `Hand`, `Deck`, and `PrizeCards` with generic DTOs containing only an obfuscated ID and a face-down indicator. Messages sent to individual private channels `/user/{playerId}/queue/match`.
- **Cross-Identity Protection**: The `GameWebSocketHandler` never trusts `playerId` from the JSON payload — `GameActionMessage` does NOT have a `playerId` field. It extracts the real user ID from the `Principal` authenticated at the transport layer (or `X-Player-Id` header in Phase 1). Mismatches result in `InvalidActionException`.

### Secrets Management

Required environment variables:
- `JWT_SECRET` — 256-bit minimum
- `POKEMON_TCG_API_KEY` — for initial card sync
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` — for production PostgreSQL

### Rate Limiting

Bucket4j limits:
- Match creation: 5 requests/minute per IP/user ID
- Card search queries: throttled

### CORS

`CorsConfigurationSource` allows only the production Angular domain. Applies to both REST controllers and WebSocket handshake.

## API Endpoints Reference

### Cards (`/api/cards`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/cards` | List cards with pagination/filters |
| GET | `/api/cards/{id}` | Get card by ID |
| GET | `/api/cards/search?name=` | Search cards by name |
| POST | `/api/cards/sync` | Sync cards from pokemontcg.io API |

### Decks (`/api/decks`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/decks` | List decks for user |
| GET | `/api/decks/{id}` | Get deck detail |
| POST | `/api/decks` | Create new deck |
| PUT | `/api/decks/{id}` | Update deck |
| DELETE | `/api/decks/{id}` | Delete deck |
| POST | `/api/decks/{id}/validate` | Validate deck rules |

### Matches (`/api/matches`)

| Method | Path | Description |
|---|---|---|
| GET | `/api/matches` | List available matches (lobby) |
| GET | `/api/matches/{id}` | Get match detail |
| POST | `/api/matches` | Create new match |
| POST | `/api/matches/{id}/join` | Join a match |
| POST | `/api/matches/{id}/leave` | Leave a match |
| GET | `/api/matches/{id}/state` | Get current match state for reconnection (X-Player-Id header, with data masking) |

### Ping (`/ping`)

Already implemented.

## External API Integration

The `CardService.syncCards()` method must:
1. Call `https://api.pokemontcg.io/v2/cards?q=set:xy1&pageSize=250` with API key header (`X-Api-Key`)
2. Map the response to `CardEntity` objects with these critical field mappings:

### API Field Mapping

| API Field | CardEntity Field | Notes |
|---|---|---|
| `id` | `externalId` | Unique external ID |
| `name` | `name` | Card name |
| `supertype` | `supertype` | "Pokémon", "Trainer", "Energy" |
| `subtypes` | `subtypes` (JSONB) | Array of strings |
| `hp` | `hp` (Integer) | Parse from String to Integer |
| `types` | `types` (JSONB) | Map to EnergyType enum (uppercase) |
| `evolvesFrom` | `evolvesFrom` | Nullable String name |
| `attacks` | `attacksJson` (TEXT) | Store raw JSON; parse on demand |
| `abilities` | `abilitiesJson` (TEXT) | Nullable; store raw JSON |
| `weaknesses[0].type` | `weaknessType` | Energy name as String |
| `resistances[0].type` | `resistanceType` | Nullable |
| `convertedRetreatCost` | `retreatCost` | Integer |
| `images.large` | `imageUrl` | Image URL |
| `rarity` | `rarity` | Rarity string |
| `number` | `cardNumber` | Card number in set |
| `set.id` | `setId` | Default "xy1" |
| `rules` | `rules` | Rules array as JSON text |

### PokemonStage Derivation

| API `subtypes` | PokemonStage |
|---|---|
| `["Basic"]` (Pokémon) | `BASIC` |
| `["Basic","EX"]` | `EX` |
| `["Stage 1"]` | `STAGE1` |
| `["Stage 2"]` | `STAGE2` |
| `["MEGA","EX"]` | `MEGA` |

### Damage Parsing

- `"60"` → damage=60, effectText=null
- `""` → damage=0, effectText=null (effect-only attack)
- `"120+"` → damage=120, effectText="120+"
- `"10×"` → damage=0, effectText="10×" + effect text

### Basic Energy Handling

If basic energy cards (Grass, Fire, Water, Lightning, Psychic, Fighting, Darkness, Metal, Fairy) are missing from the API response, seed them manually in `V5__seed_xy1_cards.sql`.

3. Save all cards to the database (upsert by `externalId`)
4. API key comes from `pokemon.api.key` property (env variable `POKEMON_TCG_API_KEY`)

## Required Dependencies (Maven)

Ensure `pom.xml` includes:
- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-websocket`
- `spring-boot-starter-security`
- `springdoc-openapi-starter-webmvc-ui`
- `flyway-core` + `flyway-database-postgresql`
- `postgresql` (runtime scope for production)
- `h2` (runtime scope for development)
- `lombok`
- `modelmapper`
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (JSON Web Token)
- `bucket4j-core` (rate limiting)
- `spring-boot-starter-test`
- `testcontainers` + `postgresql` test container (test scope)

## Checklist

When implementing BE classes:
- [ ] Correct package: `ar.edu.utn.frc.tup.piii.{subpackage}`
- [ ] Lombok annotations present (`@Data`, `@Builder`, etc.)
- [ ] Constructor injection (no `@Autowired` on fields)
- [ ] `@Transactional` on service write methods
- [ ] SpringDoc annotations on controller methods
- [ ] Bean Validation on DTO input fields
- [ ] Custom exceptions thrown from services, not generic ones
- [ ] GlobalExceptionHandler handles all custom exceptions
- [ ] UUID as primary key type for all entities
- [ ] MatchEntity includes `player1DeckId` and `player2DeckId` FKs
- [ ] MatchStatus enum includes ABANDONED
- [ ] GET `/api/matches/{id}/state` endpoint exists for reconnection
- [ ] JWT authentication on `/api/decks/**` and `/api/matches/**`
- [ ] PlayerIdInterceptor extracts `X-Player-Id` header (Phase 1)
- [ ] ChannelInterceptor validates auth on STOMP connections
- [ ] `GameActionMessage` does NOT contain `playerId` field
- [ ] `MatchDTO` uses `MatchStatus` enum (not `GamePhase`)
- [ ] `CreateMatchDTO` does NOT contain `player1Id` (from header)
- [ ] `JoinMatchDTO` does NOT contain `playerId` (from header)
- [ ] `CreateDeckDTO` does NOT contain `creatorId` (from header)
- [ ] CardService.syncCards() maps API fields correctly, including damage parsing