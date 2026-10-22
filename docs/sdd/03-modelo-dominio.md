# 03 - Modelo de Dominio

## Entidades Persistentes (JPA)

### CardEntity

Representa una carta del TCG. Los datos se obtienen de la API pokemontcg.io y se almacenan localmente via `POST /api/cards/sync`.

| Campo | Tipo | Descripción | Origen API |
|---|---|---|---|
| `id` | UUID | Identificador único (PK) | Generado |
| `externalId` | String | ID de la API externa (único) | `id` |
| `name` | String | Nombre de la carta | `name` |
| `supertype` | String | Super tipo: "Pokémon", "Trainer", "Energy" | `supertype` |
| `subtypes` | JSON / List<String> | Subtipos: ["Basic","EX"], ["Stage 1"], ["Supporter"], etc. | `subtypes` |
| `hp` | Integer | Puntos de vida (null para Trainer/Energy) | `hp` (parseado de String) |
| `types` | JSON / Set<EnergyType> | Tipos de la carta | `types` (mapeado a uppercase) |
| `stage` | Enum (PokemonStage) | Derivado de subtypes | Mapeo de `subtypes` |
| `evolvesFrom` | String | Nombre del pre-evolución (nullable) | `evolvesFrom` |
| `attacksJson` | TEXT | Lista de ataques en JSON crudo de la API | `attacks` |
| `abilitiesJson` | TEXT | Lista de habilidades en JSON crudo (nullable) | `abilities` |
| `weaknessType` | String | Tipo de debilidad (nullable) | `weaknesses[0].type` |
| `resistanceType` | String | Tipo de resistencia (nullable) | `resistances[0].type` |
| `retreatCost` | Integer | Costo de retirada | `convertedRetreatCost` |
| `rules` | TEXT | Reglas de la carta en JSON | `rules` |
| `imageUrl` | String | URL de la imagen grande | `images.large` |
| `rarity` | String | Rareza | `rarity` |
| `cardNumber` | String | Número en el set | `number` |
| `setId` | String | ID del set (default: "xy1") | `set.id` |

**Nota sobre el mapeo de subtypes a PokemonStage**: Ver la tabla de mapeo en `01-requisitos.md` → Decisiones de Diseño.

### DeckEntity

Representa un mazo construido por un jugador.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único (PK) |
| `name` | String | Nombre del mazo |
| `creatorId` | String | ID del creador |
| `isActive` | Boolean | Si el mazo está activo |
| `createdAt` | Timestamp | Fecha de creación |
| `updatedAt` | Timestamp | Fecha de última actualización |

### DeckCardEntity

Relación muchos a muchos entre Deck y Card con cantidad.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único (PK) |
| `deckId` | UUID | FK a DeckEntity |
| `cardId` | UUID | FK a CardEntity |
| `quantity` | Integer | Cantidad de copias (> 0) |

### MatchEntity

Representa una partida entre dos jugadores.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único (PK) |
| `player1Id` | String | ID del jugador 1 (creador) |
| `player2Id` | String | ID del jugador 2 (invitado, nullable) |
| `player1DeckId` | UUID | FK al mazo del jugador 1 |
| `player2DeckId` | UUID | FK al mazo del jugador 2 (nullable) |
| `status` | Enum (MatchStatus) | WAITING, SETUP, ACTIVE, FINISHED, ABANDONED |
| `winnerId` | String | ID del ganador (nullable) |
| `createdAt` | Timestamp | Fecha de creación |
| `updatedAt` | Timestamp | Fecha de última actualización |

**Nota:** `MatchStatus` es un enum distinto de `GamePhase`. `MatchStatus` es para persistencia en DB, `GamePhase` es para el engine en memoria.

### GameStateEntity

Persistencia atómica del estado del tablero. Usa un enfoque híbrido: columnas normalizadas para consultas rápidas + una columna JSONB con el snapshot completo.

| Campo | Tipo | Descripción |
|---|---|---|
| `matchId` | UUID | PK, FK a MatchEntity |
| `currentTurnId` | String | ID del jugador actual |
| `gamePhase` | Enum (GamePhase) | Fase macro del match (WAITING, SETUP, ACTIVE, FINISHED) |
| `turnPhase` | Enum (TurnPhase) | Sub-fase del turno (DRAW, ACTIONS, ATTACK, BETWEEN_TURNS) |
| `boardStateJson` | TEXT | MatchSnapshot completo serializado como JSON |
| `updatedAt` | Timestamp | Fecha de última actualización |

### GameEventEntity

Log inmutable de eventos para auditoría y reconexión de clientes. Separado del GameStateEntity para mantener el payload de WebSocket por debajo de 50ms (RNF-01).

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | UUID | Identificador único (PK) |
| `matchId` | UUID | FK a MatchEntity |
| `playerId` | String | ID del jugador (nullable para eventos del sistema) |
| `eventType` | Enum (GameEventType) | Tipo de evento |
| `description` | TEXT | Descripción legible del evento |
| `timestamp` | Timestamp | Momento del evento |

## Modelos del Engine (en memoria, orientados a objetos)

### GameCard (interfaz)

Contrato base para todos los tipos de carta en juego.

```java
public interface GameCard {
    String getId();
    String getName();
    CardType getType();
}
```

### PokemonCard

Carta de Pokémon con HP, ataques, habilidades y stage.

```java
public class PokemonCard implements GameCard {
    String id;
    String name;
    Integer hp;
    PokemonStage stage;
    Set<EnergyType> types;
    String evolvesFrom;     // Nombre del pre-evolución (nullable). Para buscar la carta, usar lookup por nombre.
    List<Attack> attacks;
    List<Ability> abilities;
    String weaknessType;    // String para permitir valores no estándar (nullable)
    String resistanceType; // String para permitir valores no estándar (nullable)
    Integer retreatCost;
    String imageUrl;
}
```

### Attack

Modela un ataque de un Pokémon. Se parsea desde el JSON de la API.

```java
public class Attack {
    String name;               // "Poison Powder"
    List<EnergyType> cost;     // [GRASS, COLORLESS, COLORLESS]
    int damage;                // 60 (0 si solo tiene efecto, -1 si daño variable)
    String effectText;         // "Your opponent's Active Pokémon is now Poisoned." (nullable)
}
```

**Nota sobre `damage`**: La API devuelve Strings como `"60"`, `""`, `"120+"`, `"10×"`. El mapeo es:
- Número puro (`"60"`) → `damage = 60`, `effectText = null`
- Vacío (`""`) → `damage = 0`, `effectText = null` (ataque sin daño numérico ni efecto textual)
- Vacío (`""`) con texto de efecto → `damage = 0`, `effectText = texto del efecto de la API` (preserva el efecto para que el cliente lo muestre)
- Con modificador (`"120+"`) → `damage = 120`, `effectText = "120+"`
- Multiplicador (`"10×"`) → `damage = 0`, `effectText = "10×" + text`

### Ability

Modela una habilidad de un Pokémon. Se parsea desde el JSON de la API.

```java
public class Ability {
    String name;            // "Venusaur Aura"
    AbilityType type;       // POKE_BODY o POKE_POWER
    String effectText;      // Texto libre del efecto
}
```

**Nota**: En el set XY1, el campo `type` de la API siempre viene como `"Ability"`. La distinción entre Poké-Body y Poké-Power se realiza analizando el texto del efecto:
- Si el efecto dice "no puede ser afectado" o permanece pasivo → `POKE_BODY`
- Si el efecto dice "puedes usar" o requiere activación → `POKE_POWER`

Para las cartas sin `abilities` en la API (la mayoría en XY1), la lista simplemente queda vacía.

### EnergyCard

Carta de energía. Puede ser básica o especial (proveer más de un tipo/cantidad de energía).

```java
public class EnergyCard implements GameCard {
    String id;
    String name;
    EnergyType energyType;
	 boolean isSpecial;
    List<EnergyType> providedEnergies; // Permite soportar cartas como la Doble Energía Incolora
}
```

### TrainerCard

Carta de entrenador con subtipo.

```java
public class TrainerCard implements GameCard {
    String id;
    String name;
    TrainerSubtype subtype;
    String effect;
}
```

### PokemonInPlay

Instancia dinámica de un Pokémon en el tablero con contadores y estados modificables.

```java
public class PokemonInPlay {
    PokemonCard card;
    Integer currentHp;
    Integer damageCounters;
    List<EnergyCard> attachedEnergy;
    TrainerCard tool;
    Set<SpecialCondition> specialConditions;
    boolean isActive;
	 // Método de negocio crítico para el RuleValidator
	 public List<EnergyType> getAvailableEnergyPool() {
        return attachedEnergy.stream()
            .flatMap(energy -> energy.getProvidedEnergies().stream())
            .collect(Collectors.toList());
    }
}
```

### PlayerBoard

Estado completo de las zonas de juego de un jugador.

```java
public class PlayerBoard {
    String playerId;
    PokemonInPlay activePokemon;
    List<PokemonInPlay> bench;           // max 5
    List<GameCard> hand;                 // cartas en mano
    List<GameCard> deck;                 // mazo (orden preservado)
    List<GameCard> discardPile;          // pila de descarte
    List<GameCard> prizeCards;           // cartas de premio (6)
    Integer prizeCardsTaken;
}
```

### GameBoard

Tablero completo de la partida.

```java
public class GameBoard {
    PlayerBoard player1Board;
    PlayerBoard player2Board;
    TrainerCard sharedStadium;
}
```

### TurnContext

Contexto del turno actual.

```java
public class TurnContext {
    String currentPlayerId;
    TurnPhase phase;
    boolean isFirstTurn;
    boolean hasDrawn;
    boolean hasPlayedSupporter;
    boolean hasPlayedStadium;
    boolean hasEvolved;
    boolean hasAttachedEnergy;
    boolean hasUsedAbility;
    boolean hasRetreated;
}
```

### MatchSnapshot

Snapshot serializable estructurado para transferencia por WebSocket y persistencia JSONB. **Nota:** El `eventLog` NO se incluye en el snapshot para mantener el payload por debajo de 50ms (RNF-01). Los eventos se entregan por separado via mensajes de tipo `EVENT`.

```java
public class MatchSnapshot {
    String matchId;
    GamePhase gamePhase;
    GameBoard board;
    TurnContext turn;
    String winnerId;
}
```

## Enums

### CardType

| Valor | Descripción |
|---|---|
| `POKEMON` | Carta de Pokémon |
| `ENERGY` | Carta de energía |
| `TRAINER` | Carta de entrenador |

### EnergyType

| Valor | Descripción |
|---|---|
| `FIRE` | Fuego |
| `WATER` | Agua |
| `GRASS` | Planta |
| `LIGHTNING` | Rayo |
| `PSYCHIC` | Psíquica |
| `FIGHTING` | Lucha |
| `DARKNESS` | Oscura |
| `METAL` | Metálica |
| `FAIRY` | Hada |
| `DRAGON` | Dragón |
| `COLORLESS` | Incolora |

### PokemonStage

| Valor | Descripción |
|---|---|
| `BASIC` | Pokémon básico |
| `STAGE1` | Evolución nivel 1 |
| `STAGE2` | Evolución nivel 2 |
| `EX` | Pokémon-EX (2 premios al KO) |
| `MEGA` | Mega evolución |

### TrainerSubtype

| Valor | Descripción |
|---|---|
| `ITEM` | Objeto (ilimitados por turno) |
| `SUPPORTER` | Adiestrador (1 por turno) |
| `STADIUM` | Estadio (1 por turno, zona compartida) |
| `TOOL` | Herramienta Pokémon (1 por Pokémon) |
| `ACE_SPEC` | ACE SPEC (máx 1 por mazo) |
| `PLASMA` | Equipo Plasma |

### SpecialCondition

| Valor | Descripción |
|---|---|
| `ASLEEP` | Dormido |
| `BURNED` | Quemado |
| `CONFUSED` | Confundido |
| `PARALYZED` | Paralizado |
| `POISONED` | Envenenado |

**Reglas de incompatibilidad:**
- Asleep, Confused y Paralyzed son mutuamente excluyentes
- Burned y Poisoned pueden coexistir con cualquier otra condición

### GamePhase

| Valor | Descripción |
|---|---|
| `WAITING` | Esperando jugadores en el lobby. |
| `SETUP` | Configuración inicial (Mulligans, colocación inicial, premios). |
| `ACTIVE` | Partida en curso (Ciclo de turnos activo). |
| `FINISHED` | Partida concluida con veredicto inmutable. |

### TurnPhase

| Valor | Descripción |
|---|---|
| `DRAW` | Acción obligatoria de robo de carta (automático, no requiere acción del cliente). |
| `ACTIONS` | Fase principal libre (Colocar básicos, energías, entrenadores, evolución). |
| `ATTACK` | Declaración y resolución del ataque (Cierra el turno de forma inmediata). |
| `BETWEEN_TURNS` | Procesamiento de condiciones especiales entre turnos (Veneno, Quemadura, Sueño, Parálisis). |

### GameZone

| Valor | Descripción |
|---|---|
| `HAND` | Mano del jugador |
| `DECK` | Mazo |
| `DISCARD` | Pila de descarte |
| `ACTIVE` | Pokémon activo |
| `BENCH` | Banca |
| `PRIZE` | Cartas de premio |
| `STADIUM` | Estadio |

## Relaciones entre Entidades

```
CardEntity 1 ──── N DeckCardEntity N ──── 1 DeckEntity

MatchEntity 1 ──── 1 GameStateEntity
```

- Una carta puede estar en muchos mazos
- Un mazo tiene muchas cartas (a través de DeckCardEntity)
- Una partida tiene exactamente un estado de juego
- Un jugador puede tener muchos mazos y muchas partidas
