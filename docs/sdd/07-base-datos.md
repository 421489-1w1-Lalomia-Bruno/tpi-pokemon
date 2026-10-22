# 07 - Base de Datos

## Configuración

El sistema utiliza **PostgreSQL 16** como única base de datos en todos los entornos. Las migraciones y la carga inicial del set XY1 están completamente automatizadas mediante scripts SQL versionados con Flyway.

### Desarrollo

Perfil `dev` activo por defecto. Requiere PostgreSQL 16 instalado localmente:

```properties
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pokemon_tcg
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=postgres
spring.datasource.password=postgres

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### Testing

Los tests que levantan contexto de Spring (`@SpringBootTest`) utilizan **Embedded PostgreSQL** vía `io.zonky.test:embedded-database-spring-test`. La clase base `EmbeddedPostgresTestBase` configura `@AutoConfigureEmbeddedDatabase` para reemplazar el `DataSource` por una instancia PostgreSQL real ejecutada como subproceso JVM. **No requiere Docker ni instalación manual.**

```properties
# src/test/resources/application.properties
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

## Modelo Entidad-Relación (Schema SQL)
```
  ┌───────────────┐          ┌───────────────────┐          ┌───────────────┐
  │     cards     │          │    deck_cards     │          │     decks     │
  ├───────────────┤          ├───────────────────┤          ├───────────────┤
  │ PK  id        ◄──┐       │ PK,FK1 deck_id    │     ┌───►│ PK  id        │
  │     ...       │  └───────┤ PK,FK2 card_id    │     │    │     ...       │
  └───────────────┘          │        quantity   │     │    └───────────────┘
                             └───────────────────┘     │
  ┌───────────────┐                                    │
  │  game_events  │                                    │
  ├───────────────┤          ┌───────────────────┐     │
  │ PK  id        │          │      matches      │     │
  │ FK  match_id  ├─────────►│ PK     id         │     │
  │     ...       │          │   FK1  p1_deck_id ├─────┘
  └───────────────┘          │   FK2  p2_deck_id ├─────┐
                             │        ...        │     │    ┌───────────────┐
                             └───────────────────┘     │    │  game_states  │
                                                       │    ├───────────────┤
  ┌──────────────────┐                                 │    │     ...       │
  │  sync_metadata   │                                 │    └───────────────┘
  ├──────────────────┤                                 │
  │ PK  id           │                                 │
  │     set_id (UQ)  │                                 │
  │     last_sync_at │                                 │
  │     synced_count │                                 │
  └──────────────────┘                                 │
```

### Tabla: `cards`
Almacena el catálogo estático de cartas del set XY1 sincronizado desde la API externa. Las columnas `subtypes` y `types` usan `JSONB` nativo de PostgreSQL.

```sql
CREATE TABLE cards (
    id UUID PRIMARY KEY,
    external_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    supertype VARCHAR(30) NOT NULL,
    subtypes JSONB,
    hp INTEGER,
    types JSONB,
    stage VARCHAR(20),
    evolves_from VARCHAR(100),
    attacks_json TEXT,
    abilities_json TEXT,
    rules TEXT,
    retreat_cost INTEGER,
    weakness_type VARCHAR(30),
    resistance_type VARCHAR(30),
    image_url VARCHAR(500) NOT NULL,
    rarity VARCHAR(50),
    card_number VARCHAR(20) NOT NULL,
    set_id VARCHAR(20) DEFAULT 'xy1' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Tabla: `decks`
Cabecera de los mazos creados por los usuarios.

```sql
CREATE TABLE decks (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    creator_id VARCHAR(100),
    source VARCHAR(20) DEFAULT 'USER' NOT NULL,
    description VARCHAR(200),
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Tabla: `deck_cards`

Tabla intermedia Many-to-Many entre mazos y cartas con cuantificador de copias.

```sql
CREATE TABLE deck_cards (
    id UUID PRIMARY KEY,
    deck_id UUID NOT NULL,
    card_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_deck_card UNIQUE (deck_id, card_id),
    CONSTRAINT fk_deck_cards_deck FOREIGN KEY (deck_id) REFERENCES decks(id) ON DELETE CASCADE,
    CONSTRAINT fk_deck_cards_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE RESTRICT
);
```

### Tabla: `sync_metadata`

Registro de auditoría para la sincronización programada del catálogo de cartas.

```sql
CREATE TABLE sync_metadata (
    id UUID PRIMARY KEY,
    set_id VARCHAR(20) NOT NULL UNIQUE,
    last_sync_at TIMESTAMP NOT NULL,
    synced_count INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Tabla: `matches`

Entidad principal para la orquestación del emparejamiento (Lobby) y auditoría de sesiones de juego.

```sql
CREATE TABLE matches (
    id UUID PRIMARY KEY,
    player1_id VARCHAR(100) NOT NULL,
    player2_id VARCHAR(100),
    player1_deck_id UUID,
    player2_deck_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    winner_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Tabla: `game_states`

Mantiene la última instantánea completa del motor de juego para recuperación inmediata ante caídas de red (RF-05 / RF-06). Relación 1:1 con `matches`. La columna `board_state_json` usa `JSONB` nativo de PostgreSQL.

```sql
CREATE TABLE game_states (
    match_id UUID PRIMARY KEY,
    current_turn_id VARCHAR(100),
    game_phase VARCHAR(30) NOT NULL,
    turn_phase VARCHAR(30),
    board_state_json JSONB NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_game_states_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);
```

### Tabla: `game_events`

Log inmutable de eventos para auditoría y reconexión de clientes.

```sql
CREATE TABLE game_events (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    player_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_game_events_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);
```

### Tabla: `users`

Almacena las cuentas de usuario para el sistema de autenticación JWT.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    gender VARCHAR(10),
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_users_username UNIQUE (username)
);
```

**Notas:**
- `username` es único y obligatorio
- `password_hash` almacena el hash BCrypt de la contraseña (60 caracteres)
- `gender` es opcional; determina el sprite de entrenador en la UI
- `wins` y `losses` se agregan en V12
- Campos futuros como `level`, `experience`, `avatar_url` se agregan sin romper el schema existente

## Versionado de Migraciones (Estructura Flyway)

Los archivos se alojan en `src/main/resources/db/migration/`:

```
resources/db/migration/
├── V1__create_cards_schema.sql            -- Catálogo base de cartas
├── V2__create_decks_tables.sql             -- Mazos y relación deck_cards
├── V3__create_sync_metadata.sql            -- Auditoría de sincronización
├── V4__create_matches_and_lobby.sql        -- Emparejamiento de salas
├── V5__create_realtime_engine_state.sql    -- Snapshots de juego y eventos
├── V6__drop_match_snapshots.sql            -- Limpieza tabla legacy
├── V7__fix_game_events_default_uuid.sql    -- Corrección default UUID
├── V8__allow_null_current_turn_id.sql      -- current_turn_id nullable
├── V9__allow_null_turn_phase.sql           -- turn_phase nullable
├── V10__create_users_table.sql             -- Tabla de usuarios JWT
├── V11__add_deck_source.sql                -- source y description en decks
├── V12__add_wins_losses_to_users.sql       -- wins y losses en users
├── V13__convert_columns_to_jsonb.sql       -- JSON/TEXT → JSONB nativo PostgreSQL
└── V14__partial_index_matches_lobby.sql    -- Índice parcial WHERE status = 'WAITING'
```

> **Nota sobre energías básicas:** La API de pokemontcg.io puede no incluir las cartas de energía básica del set XY1. Si no están presentes, se debe insertar manualmente las 9 energías básicas (Grass, Fire, Water, Lightning, Psychic, Fighting, Darkness, Metal, Fairy).

## Sincronización y Cache-Aside

El catálogo de cartas utiliza un patrón **Cache-Aside** con refresh programado.

### Flujo de Sincronización

1. **Startup:** `DataInitializer` sincroniza cartas desde `pokemontcg.io` si la DB está vacía. `CacheWarmupRunner` carga el catálogo en memoria (Caffeine).
2. **Read (Cache-Aside):** Las consultas a `/api/cards` primero verifican el cache en memoria. Si hay hit, se retorna directamente. Si hay miss, se consulta la DB y se almacena en cache.
3. **Refresh programado:** `CardSyncScheduler` ejecuta cada domingo a las 00:00. Verifica `sync_metadata.last_sync_at`: si pasaron ≥ 14 días desde la última sincronización, ejecuta `syncCards()` → actualiza DB → evicta el cache.
4. **Persistencia de metadata:** Cada sincronización exitosa actualiza `sync_metadata` con `last_sync_at` y `synced_count`.

### Endpoints

| Método | Path | Descripción |
|---|---|---|
| GET | `/api/cards` | Listar cartas con paginación y filtros (cache hit) |
| GET | `/api/cards/{id}` | Obtener carta por ID (cache hit) |
| POST | `/api/cards/sync` | Forzar sincronización manual (evict + refresh) |

## Seed Data

El proyecto no incluye un script SQL de seed data estático porque las cartas se sincronizan dinámicamente desde la API externa `pokemontcg.io` al iniciar el backend con la base de datos vacía. El `DataInitializer` ejecuta `POST /api/cards/sync` automáticamente, descargando las 146 cartas del set XY1.

### Datos que se cargan automáticamente

| Dato | Origen | Cuándo |
|---|---|---|
| 146 cartas del set XY1 | API pokemontcg.io v2 | Al iniciar con DB vacía |
| Mazo temático opcional | No implementado (futura mejora) | — |

### Para forzar una recarga manual

```bash
curl -X POST http://localhost:8080/api/cards/sync
```

Esto evicta el cache Caffeine y vuelve a sincronizar desde la API.

---

## Estrategia de Indexación y Rendimiento

| Tabla | Índice | Columnas | Propósito |
|---|---|---|---|
| `cards` | `idx_cards_search_name` | `name` | Búsqueda por texto en Deck Builder |
| `cards` | `idx_cards_sync_ext` | `external_id` | Verificación de existencia durante Sync |
| `sync_metadata` | `idx_sync_metadata_set_id` | `set_id` | Consulta de última sincronización |
| `decks` | `idx_decks_creator` | `creator_id`, `is_active` | Mazos del usuario en dashboard |
| `matches` | `idx_matches_lobby_waiting` | `created_at DESC WHERE status = 'WAITING'` | Índice parcial para Lobby (PostgreSQL) |
| `game_events` | `idx_game_events_stream` | `match_id`, `created_at ASC` | Historial cronológico de jugadas |
| `users` | `idx_users_username` | `username` | Búsqueda por username |

```sql
CREATE INDEX idx_cards_search_name ON cards(name);
CREATE INDEX idx_cards_sync_ext ON cards(external_id);
CREATE INDEX idx_sync_metadata_set_id ON sync_metadata(set_id);
CREATE INDEX idx_decks_creator ON decks(creator_id, is_active);
CREATE INDEX idx_matches_lobby_waiting ON matches(created_at DESC) WHERE status = 'WAITING';
CREATE INDEX idx_game_events_stream ON game_events(match_id, created_at ASC);
CREATE INDEX idx_users_username ON users(username);
```
