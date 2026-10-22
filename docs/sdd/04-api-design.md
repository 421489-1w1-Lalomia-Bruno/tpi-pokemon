# 04 - API Design

## REST Endpoints

### Card Controller (`/api/cards`)

| Método | Path | Descripción | Request | Response |
|---|---|---|---|---|
| GET | `/api/cards` | Listar cartas con paginación y filtros | `?page=0&size=20&type=fire&stage=basic` | `Page<CardDTO>` |
| GET | `/api/cards/{id}` | Obtener carta por ID | - | `CardDTO` |
| GET | `/api/cards/search` | Buscar cartas por nombre | `?name=charizard` | `List<CardDTO>` |
| POST | `/api/cards/sync` | Sincronizar cartas desde pokemontcg.io | `{ "setId": "xy1" }` | `{ "synced": 146 }` |

### Deck Controller (`/api/decks`)

| Método | Path | Descripción | Request | Response |
|---|---|---|---|---|
| GET | `/api/decks` | Listar mazos del usuario | `- (X-Player-Id header)` | `List<DeckDTO>` |
| GET | `/api/decks/{id}` | Obtener detalle de mazo | - | `DeckDTO` |
| POST | `/api/decks` | Crear nuevo mazo | `CreateDeckDTO` | `DeckDTO` |
| PUT | `/api/decks/{id}` | Actualizar mazo | `CreateDeckDTO` | `DeckDTO` |
| DELETE | `/api/decks/{id}` | Eliminar mazo | - | `204 No Content` |
| POST | `/api/decks/{id}/validate` | Validar reglas del mazo | - | `DeckValidationResultDTO` |

### Match Controller (`/api/matches`)

| Método | Path | Descripción | Request | Response |
|---|---|---|---|---|
| GET | `/api/matches` | Listar partidas disponibles (lobby) | - | `List<MatchDTO>` |
| GET | `/api/matches/{id}` | Obtener detalle de partida | - | `MatchDTO` |
| POST | `/api/matches` | Crear nueva partida | `CreateMatchDTO` | `MatchDTO` |
| POST | `/api/matches/{id}/join` | Unirse a una partida | `JoinMatchDTO` | `MatchDTO` |
| POST | `/api/matches/{id}/leave` | Abandonar partida | - (X-Player-Id header) | `204 No Content` |
| GET | `/api/matches/{id}/state` | Obtener estado actual (Reconexión) | - (X-Player-Id header) | `MatchSnapshot` (Filtrado) |

### Auth Controller (`/api/auth`)

| Método | Path | Descripción | Request | Response (200) |
|---|---|---|---|---|
| POST | `/api/auth/register` | Registrar nuevo usuario | `{ username, password, gender }` | `{ token, user: UserProfileDTO }` |
| POST | `/api/auth/login` | Iniciar sesión | `{ username, password }` | `{ token, user: UserProfileDTO }` |

### User Controller (`/api/users`)

| Método | Path | Descripción | Auth | Response |
|---|---|---|---|---|
| GET | `/api/users/me` | Perfil del usuario autenticado + sus mazos | JWT | `UserProfileDTO` |

### UserProfileDTO

```java
public class UserProfileDTO {
    String id;
    String username;
    String gender;
    LocalDateTime createdAt;
    List<DeckDTO> decks;
}
```

### AuthResponseDTO

```java
public class AuthResponseDTO {
    String token;
    UserProfileDTO user;
}
```

### RegisterRequestDTO / LoginRequestDTO

```java
public class RegisterRequestDTO {
    @NotBlank @Size(min = 3, max = 30) String username;
    @NotBlank @Size(min = 8) String password;
    String gender;     // MALE | FEMALE | PREFER_NOT_TO_SAY | OTHER
}

public class LoginRequestDTO {
    @NotBlank String username;
    @NotBlank String password;
}
```

> **Nota sobre identidad del jugador:** Durante las semanas 1-3, el `playerId` se pasaba mediante el header `X-Player-Id`. En Semana 5 se reemplaza por autenticación JWT completa. Un `JwtAuthenticationFilter` extrae el `userId` del token y lo inyecta como `request.setAttribute("playerId", userId)`. Los controllers existentes no cambian.

### Ping Controller (`/ping`)

| Método | Path | Descripción | Response |
|---|---|---|---|
| GET | `/ping` | Health check | `"pong"` |

## DTOs

### CardDTO

```java
public class CardDTO {
    String id;
    String externalId;
    String name;
    String supertype;             // "Pokémon", "Trainer", "Energy"
    List<String> subtypes;        // ["Basic", "EX"], ["Stage 1"], ["Supporter"], etc.
    Integer hp;                   // null para Trainer/Energy
    Set<EnergyType> types;
    PokemonStage stage;           // Derivado de subtypes
    String evolvesFrom;           // Nombre del pre-evolución (nullable)
    List<AttackDTO> attacks;
    List<AbilityDTO> abilities;  // Nullable
    String weaknessType;          // Tipo de debilidad (nullable)
    String resistanceType;        // Tipo de resistencia (nullable)
    Integer retreatCost;
    String imageUrl;
    String rarity;
    String cardNumber;
    String setId;
}
```

### AttackDTO

```java
public class AttackDTO {
    String name;               // "Poison Powder"
    List<EnergyType> cost;     // [GRASS, COLORLESS, COLORLESS]
    int damage;                // 60 (0 si solo tiene efecto)
    String effectText;         // Nullable. Texto del efecto ("Your opponent's Active Pokémon is now Poisoned.")
}
```

### AbilityDTO

```java
public class AbilityDTO {
    String name;              // "Venusaur Aura"
    AbilityType type;         // POKE_BODY o POKE_POWER
    String effectText;        // Texto libre del efecto
}
```

### CardFilterDTO

```java
public class CardFilterDTO {
    String name;
    EnergyType type;
    PokemonStage stage;
    String supertype;    // "Pokémon", "Trainer", "Energy"
    String setId;
    Integer page;
    Integer size;
}
```

### CreateDeckDTO

```java
public class CreateDeckDTO {
    @NotBlank String name;
    @NotEmpty List<DeckCardEntryDTO> cards;
}
// Nota: creatorId NO va en el body. Se extrae del header X-Player-Id via PlayerIdInterceptor.

public class DeckCardEntryDTO {
    String cardId;
    Integer quantity;
}
```

### DeckDTO

```java
public class DeckDTO {
    String id;
    String name;
    String creatorId;
    List<DeckCardDTO> cards;
    Integer totalCards;
    Boolean isValid;
    List<String> validationErrors;
}
```

### DeckValidationResultDTO

```java
public class DeckValidationResultDTO {
    Boolean isValid;
    List<String> errors;
    Integer totalCards;
    Integer basicPokemonCount;
    Integer aceSpecCount;
    Map<String, Integer> cardNameCounts;
}
```

### CreateMatchDTO & JoinMatchDTO

```java
public class CreateMatchDTO {
    @NotBlank String player1DeckId;
}
// Nota: player1Id NO va en el body. Se extrae del header X-Player-Id.

public class JoinMatchDTO {
    @NotBlank String deckId;
}
// Nota: playerId NO va en el body. Se extrae del header X-Player-Id.
```

### MatchDTO

```java
public class MatchDTO {
    String id;
    String player1Id;
    String player2Id;
    MatchStatus status;     // WAITING, SETUP, ACTIVE, FINISHED, ABANDONED
    String winnerId;
    LocalDateTime createdAt;
}
// Nota: MatchStatus es un enum distinto de GamePhase. MatchStatus es para persistencia/REST,
// GamePhase (WAITING, SETUP, ACTIVE, FINISHED) es para el engine en memoria.
```

// MatchStateDTO fue eliminado. Se utiliza directamente MatchSnapshot para representar el tablero completo tanto en REST como en WebSocket

### ErrorApi

```java
public class ErrorApi {
    String timestamp;
    Integer status;
    String error;
    String message;
    String path;
}
```

## Protocolo WebSocket

### Conexión

```
URL: /ws/game
Protocolo: WebSocket (STOMP sobre WebSocket)
```

### Mensajes Cliente → Servidor (`GameActionMessage`)

```typescript
interface GameActionMessage {
  matchId: string;
  // playerId NO se incluye. Se extrae del Principal autenticado en la sesión STOMP.
  type: GameActionType;
  payload: GameActionPayload;
  timestamp: string;
}

type GameActionType =
  | 'READY'               // Listo para comenzar (setup). Gatilla mulligan automático si aplica.
  | 'PLACE_POKEMON'       // Colocar Pokémon en banca
  | 'ATTACH_ENERGY'       // Attachar energía
  | 'PLAY_TRAINER'        // Jugar carta de entrenador
  | 'EVOLVE'              // Evolucionar Pokémon
  | 'RETREAT'             // Retirar Pokémon activo
  | 'ATTACK'              // Atacar
  | 'USE_ABILITY'         // Usar habilidad
  | 'PASS_TURN'           // Finalizar el turno sin atacar (pasa a BETWEEN_TURNS)
  | 'SET_ACTIVE'          // Cambiar Pokémon activo (tras KO)
  | 'SURRENDER'           // Rendirse (termina la partida inmediatamente)
```

> **Nota sobre DRAW_CARD:** No existe la acción `DRAW_CARD` en el protocolo. El robo de carta al inicio de la fase DRAW es **automático**: el engine roba una carta sin intervención del cliente. El cliente recibe un `STATE_UPDATE` después de que el engine ejecute el robo. La única excepción es el primer turno del jugador inicial, donde la fase DRAW se saltea automáticamente.

## Estructuras de Payload Esperadas (GameActionPayload)
El backend esperará claves específicas en el payload dependiendo de la acción:

- **PLACE_POKEMON**: { "cardId": "uuid", "zone": "ACTIVE" | "BENCH" } 

- **ATTACH_ENERGY**: { "energyCardId": "uuid", "targetPokemonId": "uuid" }

- **PLAY_TRAINER**: { "cardId": "uuid", "targetId": "uuid" (opcional según la carta) }

- **EVOLVE**: { "evolutionCardId": "uuid", "targetPokemonId": "uuid" }

- **RETREAT**: { "benchPokemonId": "uuid", "energyCardsToDiscard": ["uuid1", "uuid2"] }

- **ATTACK**: { "attackIndex": 0 }

- **SET_ACTIVE**: { "benchPokemonId": "uuid" }


### Mensajes Servidor → Cliente

```typescript
interface GameStateUpdateMessage {
  type: 'STATE_UPDATE' | 'EVENT' | 'ERROR' | 'NOTIFICATION';
  state?: MatchSnapshot;
  event?: GameEvent;
  error?: string;
  notification?: string;
}

interface GameEvent {
  type: GameEventType;
  playerId: string;
  description: string;
  timestamp: string;
}

type GameEventType =
  | 'MULLIGAN_EXECUTED' // El servidor lo detecta automáticamente al hacer READY
  | 'CARD_DRAWN'
  | 'POKEMON_PLACED'
  | 'ENERGY_ATTACHED'
  | 'TRAINER_PLAYED'
  | 'POKEMON_EVOLVED'
  | 'POKEMON_RETREATED'
  | 'ATTACK_EXECUTED'
  | 'POKEMON_KNOCKED_OUT'
  | 'PRIZE_TAKEN'
  | 'SPECIAL_CONDITION_APPLIED'
  | 'SPECIAL_CONDITION_REMOVED'
  | 'TURN_CHANGED'
  | 'PHASE_CHANGED'
  | 'GAME_FINISHED';
```

## Ejemplos Concretos de Mensajes WebSocket

### Ejemplo 1: Cliente → Servidor (ATTACH_ENERGY)

```json
{
  "matchId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "ATTACH_ENERGY",
  "payload": {
    "energyCardId": "660e8400-e29b-41d4-a716-446655440001",
    "targetPokemonId": "770e8400-e29b-41d4-a716-446655440002"
  },
  "timestamp": "2026-06-27T15:30:00Z"
}
```

### Ejemplo 2: Cliente → Servidor (ATTACK)

```json
{
  "matchId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "ATTACK",
  "payload": {
    "attackIndex": 0
  },
  "timestamp": "2026-06-27T15:31:00Z"
}
```

### Ejemplo 3: Cliente → Servidor (SET_ACTIVE después de KO)

```json
{
  "matchId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "SET_ACTIVE",
  "payload": {
    "benchPokemonId": "880e8400-e29b-41d4-a716-446655440003"
  },
  "timestamp": "2026-06-27T15:32:00Z"
}
```

### Ejemplo 4: Servidor → Cliente (STATE_UPDATE)

```json
{
  "type": "STATE_UPDATE",
  "state": {
    "matchId": "550e8400-e29b-41d4-a716-446655440000",
    "gamePhase": "ACTIVE",
    "board": {
      "player1Board": {
        "playerId": "p1-uuid",
        "activePokemon": {
          "cardId": "990e8400-e29b-41d4-a716-446655440004",
          "cardName": "Charizard",
          "hp": 150,
          "damageCounters": 3,
          "attachedEnergy": [
            { "id": "uuid", "energyType": "FIRE" },
            { "id": "uuid", "energyType": "COLORLESS" }
          ],
          "specialConditions": ["BURNED"],
          "tool": null
        },
        "bench": [],
        "hand": [
          { "id": "uuid-masked", "isKnown": true },
          { "id": "uuid-masked", "isKnown": true }
        ],
        "handCount": 2,
        "deckCount": 45,
        "discardCount": 3,
        "prizeCardsRemaining": 5
      },
      "player2Board": {
        "playerId": "p2-uuid",
        "activePokemon": {
          "cardId": "aae8400-e29b-41d4-a716-446655440005",
          "cardName": "Blastoise",
          "hp": 130,
          "damageCounters": 0,
          "attachedEnergy": [
            { "id": "uuid", "energyType": "WATER" }
          ],
          "specialConditions": [],
          "tool": null
        },
        "bench": [
          {
            "cardId": "bbe8400-e29b-41d4-a716-446655440006",
            "cardName": "Lapras",
            "hp": 100,
            "damageCounters": 0,
            "attachedEnergy": [],
            "specialConditions": [],
            "tool": null
          }
        ],
        "handCount": 4,
        "deckCount": 50,
        "discardCount": 1,
        "prizeCardsRemaining": 6
      },
      "sharedStadium": null
    },
    "turn": {
      "currentPlayerId": "p1-uuid",
      "phase": "ACTIONS",
      "isFirstTurn": false,
      "hasDrawn": true,
      "hasAttachedEnergy": false,
      "hasPlayedSupporter": false,
      "hasPlayedStadium": false,
      "hasRetreated": false,
      "hasEvolved": false
    },
    "winnerId": null
  }
}
```

### Ejemplo 5: Servidor → Cliente (EVENT — Knockout)

```json
{
  "type": "EVENT",
  "event": {
    "type": "POKEMON_KNOCKED_OUT",
    "playerId": "p2-uuid",
    "description": "El Pokémon activo de p2-uuid (Blastoise) ha quedado fuera de combate.",
    "timestamp": "2026-06-27T15:33:00Z"
  }
}
```

### Ejemplo 6: Servidor → Cliente (ERROR — Acción inválida)

```json
{
  "type": "ERROR",
  "error": "No puedes atacar: te falta 1 Energía de Fuego para usar este ataque."
}
```

---

## Códigos de Error HTTP

| Código | Uso |
|---|---|
| 400 | Bad Request (validación fallida) |
| 404 | Not Found (entidad no encontrada) |
| 409 | Conflict (acción no válida en estado actual) |
| 500 | Internal Server Error |

## Seguridad de la API

- **Identidad del Jugador:** El `playerId` NO se envía en el body de los DTOs ni en el `GameActionMessage` de WebSocket. En REST, se extrae del header `X-Player-Id` mediante un `PlayerIdInterceptor` (semanas 1-3, migrando a JWT en semanas 4-5). En WebSocket, se extrae del `Principal` autenticado en la sesión STOMP.
- **Filtrado de Snapshot**: El backend valida TODAS las acciones antes de aplicarlas. La mano y el orden del mazo del oponente nunca se envían al cliente. MatchSnapshot se sanitiza antes de enviarse por el socket o por el endpoint REST.
- **Identidad en Reconexión**: El endpoint `GET /api/matches/{id}/state` usa el header `X-Player-Id` (no query param) para que el `GameStatePersistenceService` sepa desde la perspectiva de qué jugador debe enmascarar los datos.
- **Aislamiento de Lógica**: Durante el juego, no se permiten llamadas REST directas que alteren el tablero (todas las mutaciones van por WebSocket hacia el `GameEngine`).
- **Validación de Inputs**: Todos los DTOs usan anotaciones de validación (ej. `@Valid`, `@NotBlank`, `@NotEmpty`).
- **Protección Anti-Cheat**: El `TurnManager` y `RuleValidator` nunca confían en los IDs pasados en el cuerpo del JSON. En WebSocket, el `playerId` se extrae del `SimpMessageHeaderAccessor` desde el `Principal` autenticado. Si el `Principal` no coincide con el jugador que le corresponde actuar, el servidor bloquea la ejecución.
