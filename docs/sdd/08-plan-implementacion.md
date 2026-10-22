# 08 - Plan de Implementación

## Cronograma: 6 Semanas

> **Nota de Gestión:** Este cronograma de 6 semanas fue reestructurado para balancear las cargas de trabajo, garantizar la entrega continua de software funcional y eliminar los cuellos de botella mediante contratos estricto de DTOs y WebSockets.

---

## Semana 1: Fundación, Modelo Relacional y Catálogo Base

**Objetivo:** Base de datos configurada, sincronización externa robusta y listado estático de cartas operativo en REST y UI.

### Backend (Fundación y Data Layer)
1. **Configuración Inicial:** Configurar variables de entorno (`POKEMON_TCG_API_KEY`), `.gitignore` y dependencias base de Maven.
2. **Estructura Común:** Implementar `BaseEntity`, Enums globales (10 archivos) y excepciones custom con su `GlobalExceptionHandler`.
3. **Esquema Relacional Estático:** Crear las entidades JPA `CardEntity`, `DeckEntity` y `DeckCardEntity` con sus respectivos `CardRepository` y `DeckRepository`.
4. **Migraciones Base:** Crear los scripts Flyway `V1__create_cards_schema.sql` y `V2__create_decks_management.sql`.
5. **Sincronización Core:** Implementar el servicio `CardService` con el método `/api/cards/sync` para persistir en batch el set XY1 de la API externa.
6. **API de Consulta:** Habilitar endpoints REST `/api/cards`, `/api/cards/{id}` y `/api/cards/search` usando `CardMapper` y `Page<CardDTO>`.

### Frontend (Infraestructura y Pokedex)
1. **Entorno Base:** Configurar `app.config.ts` (ZoneChange, HttpClient con ErrorInterceptor) y rutas base de navegación en `app.routes.ts`.
2. **Feature Pokedex:** Desarrollar `PokedexPage` utilizando control flow nativo (`@for`) y estrategia `OnPush`. Consume `/api/cards` mediante `CardService`.

### Entregable Semana 1

- Pokedex funcional con datos reales de cartas xy1
- API REST de cartas operativa
- Configuración base completa (CORS, exceptions, HTTP client)

---

## Semana 2: Gestión de Mazos, Cache-Aside y Lógica de Validación

**Objetivo:** CRUD completo de mazos operable en UI con validación algorítmica estricta de reglas en el servidor. Catálogo de cartas con cache local (Caffeine) y refresh programado desde API externa.

### Backend (Negocio de Mazos)
1. **Contratos de Entrada:** Implementar DTOs de validación (`CreateDeckDTO`, `DeckValidationResultDTO`) con Bean Validation (`@Valid`).
2. **Algoritmo de Reglas:** Implementar `DeckValidationService`. Debe validar: 60 cartas exactas, mínimo 1 básico, máximo 4 copias por nombre. **Ignorar el límite para cartas ACE SPEC** (RF-04.2).
3. **CRUD Controller:** Habilitar el endpoint `/api/decks` (CRUD completo indexado por `creatorId`).

### Backend (Cache-Aside de Cartas — P3)
1. **Configuración de Cache:** Implementar `CacheConfig` con Caffeine (`@EnableCaching`, TTL 14 días, max 500 entradas) y `SchedulingConfig` (`@EnableScheduling`).
2. **Cache-Aside en CardService:** Agregar `@Cacheable` en `listCards()`, `getCardById()`, `searchCards()` y `@CacheEvict(allEntries=true)` en `syncCards()`.
3. **Scheduler Programado:** Implementar `CardSyncScheduler` con `@Scheduled(cron = "0 0 0 ? * SUN")` que verifica `sync_metadata.last_sync_at` y refresca si pasaron ≥ 14 días.
4. **Persistencia de Metadata:** Crear `SyncMetadataEntity`, `SyncMetadataRepository` y migración `V3__create_sync_metadata.sql`. Modificar `syncCards()` para persistir `last_sync_at` y `synced_count`.
5. **Cache Warmup:** Implementar `CacheWarmupRunner` (`ApplicationRunner` con `@Order(2)`) que carga el catálogo en cache al inicio.

### Frontend (Formularios e Interactividad)
1. **Feature Deck Builder:** Desarrollar `DeckBuilderPage` dividida en componentes atómicos:
   - `CardCatalogComponent`: Buscador lateral reactivo de cartas.
   - `DeckEditorComponent`: Formulario reactivo para añadir/quitar copias.
   - `DeckSummaryComponent`: Renderizado de alertas de error calculadas por el backend.
2. **Integración:** Conectar componentes al servicio HTTP de mazos.

### Entregable Semana 2

- Deck builder funcional con validación (60 cartas, 4 copias, ACE SPEC, 1+ Basic)
- CRUD completo de mazos
- Cache-Aside operativo con Caffeine (TTL 14 días) y refresh programado los domingos a las 00:00
- Migración V3 (`sync_metadata`) integrada

---

## Semana 3: Lobby, Emparejamiento e Infraestructura Real-Time

**Objetivo:** Creación de salas de juego en el Lobby y establecimiento de la arquitectura de red bidireccional por WebSockets STOMP.

### Backend(Salas y Conectividad)
1. **Persistencia de Matches:** Implementar `MatchEntity` y el script de migración `V4__create_matches_and_lobby.sql`. Habilitar índices compuestos para el estado `WAITING`.
2. **Lobby REST Controller:** Implementar `/api/matches` para permitir crear y unirse a partidas (Exigir `JoinMatchDTO` con mazo asignado).
3. **Infraestructura WebSocket:** Configurar `WebSocketConfig` y el manejador asíncrono `GameWebSocketHandler`. Definir contratos estructurales de red (`GameActionMessage` y `GameStateUpdateMessage`).

### Frontend (Lobby y Conexión de Sockets)
1. **Feature Lobby:** Desarrollar `LobbyPage` con listados reactivos de salas disponibles consumiendo la API REST.
2. **Core Socket Service:** Implementar `websocket.service.ts` para gestionar la conexión STOMP, reintentos robustos y reconexión limpia.

### Entregable Semana 3

- Lobby funcional con creación y unión a partidas
- WebSocket conectado y operativo
- Persistencia de estado de partida

---

## Semana 4: El Motor de Juego (Ciclo Macro y Turnos)

**Objetivo:** Implementación de la máquina de estados de la partida, control de fases de turno y persistencia en bloques JSONB.

### Backend(Core Engine y State)
1. **Persistencia Atómica:** Crear `GameStateEntity` y la migración `V5__create_realtime_engine_state.sql` (`JSONB snapshot`).
2. **State Pattern:** Implementar la interfaz `MatchState` y sus manejadores de ciclo macro: `WaitingState`, `SetupState` (reparto, monedas y mulligans automatizados) y `FinishedState`.
3. **TurnManager:** Implementar las transiciones micro del turno (`TurnPhase`: DRAW, ACTIONS, ATTACK) y las restricciones del `RuleValidator` (bloqueo de evolución y ataque en primeros turnos).
4. **Aislamiento de Infraestructura:** Configurar el sistema de eventos de dominio (`ApplicationEventPublisher`) y el listener de red que convierte eventos internos en ticks de WebSocket.
5. **[Fix] Filtro type en catálogo:** Corregir `CardServiceImpl.listCards()` para usar `root.get("types").as(String.class)` en vez de `cb.function("JSON_VALUE", ...)`. H2 2.3.232 no soporta `JSON_VALUE` y `CAST` via Criteria genera sintaxis incorrecta.
6. **[Fix] ModelMapper en DeckMapper:** Eliminar dependencia de ModelMapper en `DeckMapper.toEntity()` y `DeckMapper.toDto()`. Construir objetos manualmente para evitar error `MergingCollectionConverter` al mapear colecciones Hibernate.
7. **[Fix] Columnas NOT NULL en game_states:** Agregar migraciones `V9__allow_null_current_turn_id.sql` y `V10__allow_null_turn_phase.sql`. Las columnas `current_turn_id` y `turn_phase` deben ser NULL durante las fases WAITING y SETUP porque no hay turno activo aún.

### Frontend (El Tablero Reactivo y Perspectivas)
1. **Feature Game Board:** Crear `GamePage` enrutada a `/game/:matchId`. Debe capturar el snapshot por el socket y alojarlo en el signal `matchState`.
2. **Cálculo de Perspectivas:** Implementar en el servicio local las propiedades calculadas (`computed()`) `myBoard` y `opponentBoard` para invertir dinámicamente la mesa según el ID del jugador local.
3. **Setup UI:** Desarrollar `SetupPhaseComponent` para congelar la pantalla hasta que ambos hagan click en READY.

### Entregable Semana 4

- Game engine funcional con state machine
- Gestión de turnos y validación de reglas
- Integración WebSocket ↔ Engine
- Correcciones de bugs: filtro type, ModelMapper en DeckMapper, columnas NOT NULL en game_states

---

## Semana 5: Sistema de Usuarios, Pipeline de Combate y Tablero Interactivo

**Objetivo:** Implementar autenticación completa (registro/login JWT), perfil de usuario, ejecución de daños con modificadores en cadena, y tablero visual interactivo completo con las validaciones pendientes del Deck Builder.

### Backend (Autenticación y Usuarios)

1. **User Entity y Migración:** Crear `UserEntity`, `Gender` enum y la migración `V11__create_users_table.sql`. El usuario tiene: `id` (UUID), `username` (único, 30 chars), `passwordHash` (BCrypt), `gender` (opcional). Diseñado para ser extensible (nivel, experiencia, avatar, logros futuros).
2. **Auth Controller:** Implementar `POST /api/auth/register` y `POST /api/auth/login`. El registro valida username único y password ≥ 8 caracteres. El login verifica contra BCrypt. Ambos devuelven `{ token, user }`.
3. **User Controller:** Implementar `GET /api/users/me` que devuelve perfil del usuario autenticado + lista de sus mazos.
4. **JWT Filter:** Implementar `JwtAuthenticationFilter` (OncePerRequestFilter) que reemplaza al `PlayerIdInterceptor`. Extrae el token del header `Authorization: Bearer <token>`, valida firma y expiración, e inyecta `userId` como `request.setAttribute("playerId", userId)`.
5. **SecurityConfig:** Actualizar para rutas públicas (`/api/auth/**`, `/api/cards/**` si se desea) y protegidas con JWT (`/api/decks/**`, `/api/matches/**`, `/api/users/**`).
6. **WebSocket Auth:** Actualizar `AuthChannelInterceptor` para leer y validar JWT desde el header `Authorization` en la trama STOMP CONNECT.
7. **Migración de identidad:** Los datos legacy (mazos con `creatorId = "player-xxx"`) coexisten sin cambios. Se documenta limpieza futura.

### Backend (Combate y Estrategias de Estado)

1. **Chain of Responsibility:** Implementar el pipeline secuencial en `engine/combat/steps/` (7 clases físicas individuales de `AttackStep`), calculando el pool energético dinámico mediante `getAvailableEnergyPool()`.
2. **Strategy Pattern (Efectos):** Crear la interfaz `StatusEffectStrategy` e implementar los 5 archivos físicos de condiciones especiales independientes (`PoisonedEffect`, `BurnedEffect`, etc.).
3. **Auditoría:** Guardar de forma automatizada las acciones en la tabla `game_events` indexada por timestamp.

### Frontend (Autenticación)

1. **AuthService:** Servicio con métodos `login()`, `register()`, `logout()`, `getToken()`, `isLoggedIn()`. Guarda el JWT en `localStorage` y expone un signal `currentUser` con los datos del usuario autenticado.
2. **JwtInterceptor:** Interceptor HTTP que adjunta `Authorization: Bearer <token>` a todas las peticiones excepto `/api/auth/**`.
3. **AuthGuard:** Guard de ruta que verifica `AuthService.isLoggedIn()`. Si no hay token, redirige a `/login`.
4. **LoginPage:** Formulario de inicio de sesión (usuario + contraseña). Integrado con `AuthService.login()`.
5. **RegisterPage:** Formulario de registro (usuario, contraseña, repetir contraseña, género). Integrado con `AuthService.register()`.
6. **ProfilePage:** Muestra datos del usuario (username, género, avatar según género, fecha de creación) y lista completa de sus mazos con opción de eliminar.
7. **Rutas protegidas:** Configurar `app.routes.ts` con `AuthGuard` en todas las rutas excepto `/login` y `/register`. Ruta por defecto (`''`) redirige a `/pokedex` si hay sesión, o a `/login` si no.

### Frontend (Fixes pendientes del Deck Builder)

1. **F1 — Límite 4 por nombre:** Cambiar validación en `onCardSelected()` e `onIncrementCard()` para agrupar por `card.name` en vez de `card.id`. Dos prints distintos del mismo Pokémon cuentan juntos para el límite de 4.
2. **F2 — Energy básicas sin límite:** Eximir a las cartas con `supertype === 'Energy'` y `subtypes.includes('Basic')` del límite de 4 copias. El botón `+` no se deshabilita para estas cartas.
3. **F3+F6 — Validación local pre-save:** Crear `validateDeckLocally()` en el page component que imita las reglas del backend (60 cartas exactas, min 1 básico, max 4 por nombre). Ejecutarla automáticamente al cambiar cartas o nombre, actualizando `validationResult`.
4. **F4 — Mostrar error real del backend:** En el `error()` handler de `onSave()`, extraer `err.error?.message` del backend y mostrarlo en vez del mensaje genérico "Error al guardar el mazo".
5. **F5 — Save button condicional:** Deshabilitar el botón Guardar también cuando `totalCards() !== 60`.

### Frontend (Mazo equipado para unirse a partidas)

1. En el Deck Builder, agregar botón "Usar en partida" en cada mazo de la lista.
2. Al clickear, guardar `localStorage.setItem('deck-for-{playerId}', deckId)` y mostrar indicador visual del mazo seleccionado.
3. En `MatchListComponent.onJoin()`, el `deckId` ahora existe en localStorage y permite unirse exitosamente.
4. Actualizar el `JoinMatchDTO` backend para que valide que el mazo pertenece al jugador (ya implementado).

### Frontend (Tablero Físico y Eventos)

1. **Mapeo del Layout:** Desarrollar los subcomponentes del tablero dentro de `PlayerAreaComponent` y `OpponentAreaComponent`: `HandAreaComponent`, `BenchAreaComponent`, `ActivePokemonComponent`, `DeckPileComponent`, `DiscardPileComponent` y `PrizeCardsComponent`.
2. **Interactividad CDK:** Envolver el layout bajo `cdkDropListGroup`. Configurar el arrastre de cartas emitiendo mensajes inmediatos (`PLACE_POKEMON`, `ATTACH_ENERGY`) al socket sin mutar arreglos locales.
3. **Pausas y Controles:** Desarrollar `ActionPanelComponent` (con botón "Pasar Turno") y el componente superior `PromoteModalComponent` para capturar la acción obligatoria `SET_ACTIVE` tras un KO.

### Entregable Semana 5

- Sistema de usuarios completo (registro, login, JWT, perfil)
- Rutas protegidas con AuthGuard (todo excepto login/register)
- Deck Builder con validaciones correctas (límite por nombre, energy básicas sin límite, pre-validación local)
- "Mazo equipado" funcional para unirse a partidas
- Game engine completo con todas las reglas de combate
- Tablero de juego interactivo con multiplayer en tiempo real

---

## Semana 6: Migración a PostgreSQL, Cobertura y Cierre de Calidad

**Objetivo:** Migrar de H2 a PostgreSQL como base de datos única, alcanzar cobertura JaCoCo ≥ 80%, y completar la documentación de entrega.

### Backend (Migración PostgreSQL)

1. **Eliminación de H2:** Remover dependencia `com.h2database:h2` del `pom.xml`. Eliminar perfil H2 de `application-dev.properties`.
2. **PostgreSQL como BD única:** Configurar `application-dev.properties` para PostgreSQL 16 en `localhost:5432/pokemon_tcg`. Actualizar `application-test.properties` para embedded PostgreSQL.
3. **Migraciones V13–V14:**
   - V13: Convertir columnas `subtypes`, `types` (cards) y `board_state_json` (game_states) a `JSONB` nativo de PostgreSQL.
   - V14: Reemplazar índice `idx_matches_status_created` por índice parcial `WHERE status = 'WAITING'`.
4. **Embedded PostgreSQL para tests:** Agregar dependencia `io.zonky.test:embedded-database-spring-test:2.5.1` (test scope). Crear clase base `EmbeddedPostgresTestBase` con `@AutoConfigureEmbeddedDatabase`. Migrar los 11 `@SpringBootTest` existentes a extender esta base.
5. **Testing TDD Final:** Completar tests de integración de flujos de partidas completas y asegurar mediante JaCoCo cobertura global ≥ 80% y crítica ≥ 90% (`DamageCalculator`, `RuleValidator`, etc.).
6. **Auditoría de Código:** Ejecución final de Checkstyle y PMD sin violaciones.

### Frontend (E2E y Polish)

1. **Testing de Flujo:** Implementar al menos 1 test End-to-End completo (Cypress/Playwright) simulando una partida controlada desde el login del lobby hasta la victoria.
2. **Performance Tuning:** Asegurar la ausencia total de tipados tipo `any` en TypeScript y verificar que no existan fugas de memoria en la desconexión de sockets.
3. **Entrega:** Compilación final de producción (`npm run build`).

### Documentación

| # | Tarea | Archivos |
|---|---|---|
| 1 | Actualizar SDD con migración PostgreSQL | `00-overview.md`, `07-base-datos.md`, `08-plan-implementacion.md`, `10-testing.md`, `13-checklist-entrega.md`, `17-setup-ejecucion.md` |
| 2 | README con instrucciones de setup | `README.md` |
| 3 | Diagramas UML | `BE/docs/app_doc/diagrams/*.puml` |
| 4 | JavaDoc | `mvn javadoc:javadoc` |

### Entregable Semana 6

- PostgreSQL 16 como base de datos única (dev + test + prod)
- Embedded PostgreSQL para tests sin Docker
- Migraciones Flyway completas (V1–V14) con JSONB nativo e índice parcial
- Cobertura de testing cumplida (≥ 80% global, ≥ 90% crítico)

---

## Matriz de Dependencias Críticas entre Módulos

```text
[Semana 1: Cards DB & REST] ────► [Semana 2: Deck CRUD, Cache-Aside & Rules]
           │
           ▼
[Semana 3: Lobby & WS Infra] ───► [Semana 4: Engine State & Snapshots]
                                               │
                                               ▼
                                  [Semana 5: Combat CoR & CDK Drag]
                                               │
                                               ▼
                                   [Semana 6: JaCoCo, E2E & PostgreSQL]
```

---

## Semana 5 — Apéndice: Fix del Flujo WAITING → SETUP → ACTIVE (P1)

**Asignación:** P1 (responsable full-stack de este fix)

**Problema:** `SetupPhaseComponent` solo tiene dos estados locales (`idle`/`waiting`) y nunca reacciona al `gameState.status()`. Cuando el engine transiciona de `WAITING` a `SETUP` y envía el snapshot con las manos de ambos jugadores, el frontend sigue mostrando el spinner "Configurando partida..." indefinidamente porque no tiene UI para colocar Pokémon durante SETUP.

**Diagnóstico técnico:**

```
Cliente (FE)                          Servidor (Engine)
─────────────────────────────────────────────────────────────────
WAITING: player1 conecta              WaitingState activo
WAITING: player2 se une (REST)        MatchEntity.player2Id seteado
WAITING: ambos clickean READY        → ready.size() ≥ 2 → SetupState
SETUP:   ??? (sin UI)                ← broadcastToMatch: status=SETUP + hands
         nunca envían PLACE_POKEMON  → juego congelado en SETUP eterno
```

**Objetivo:** Completar el flujo WAITING → SETUP → ACTIVE con UI reactiva para las 3 fases.

### Tareas

#### 1. `setup-phase.ts` — Reemplazar señales locales por computadas reactivas

- **Eliminar** `_phase`, `isIdle`, `isWaiting` locales
- **Agregar** computadas reactivas desde `gameState`:
  - `isWaitingForOpponent`: `status === 'WAITING' && !opponentId` — spinner
  - `isReadyPhase`: `status === 'WAITING' && opponentId` — botón READY
  - `isSetupPhase`: `status === 'SETUP'` — UI de colocación de Pokémon
- **Agregar** propiedad `hand` leyendo `gameState.myBoard()?.board?.hand`
- **Agregar** señal local `selectedCardId` para tracking de selección + `selectedZone` para zona destino
- **Reemplazar** `ws.sendAction()` por `gameState.setReady()` y `gameState.dispatchAction('PLACE_POKEMON', ...)`
- **Agregar** propiedad `placedPokemon` computada desde `myBoard()?.board?.activePokemon` y `bench` para mostrar feedback cuando el jugador ya colocó

#### 2. `setup-phase.html` — 3 bloques `@if` con UI para cada fase

- **WAITING sin rival:** spinner animado + "Esperando que se una un rival..."
- **WAITING con rival:** botón "READY" con gradiente, llama a `gameState.setReady()`. Si ya hizo click → botón deshabilitado + "Esperando al rival..."
- **SETUP:** 
  - Lista horizontal de cartas de la mano (scrollable). Solo Pokémon `BASIC`/`EX` son clickeables. Las demás cartas aparecen atenuadas.
  - Al clickear carta: se selecciona (borde iluminado) + aparecen 2 botones debajo: "Colocar en Activo" y "Colocar en Banca (bench)"
  - Si el jugador ya colocó un Pokémon en Activo: mostrar check verde + mensaje "Pokémon colocado. Esperando al rival..."
  - Zonas visuales: slot ACTIVO + hasta 5 slots de BENCH mostrando las cartas ya colocadas

#### 3. `setup-phase.css` — Estilos para la UI de SETUP

- Contenedor `.hand-list`: flex horizontal con overflow-x auto, gap, padding
- `.hand-card`: tamaño compacto (120x168px), borde sutil, hover effect
- `.hand-card.selected`: borde `var(--accent-bright)`, glow, transform scale(1.05)
- `.hand-card.disabled`: opacity 0.4, cursor not-allowed
- `.zone-slots`: grid para Active + Bench
- `.zone-slot`: drop target visual con borde dashed, texto "Colocar aquí"
- `.zone-slot.occupied`: muestra la carta colocada
- `.placement-actions`: botones de acción debajo de carta seleccionada
- `.ready-button`: gradiente accent, animación hover
- `.spinner`: animación de carga

#### 4. `gameState.dispatchAction()` — Verificar compatibilidad con PLACE_POKEMON

- Confirmar que `dispatchAction('PLACE_POKEMON', { cardId, zone })` se serializa correctamente en `websocket.service.ts.sendAction()`
- El backend (`SetupState.handleAction()`) espera `payload.get("cardId")` (String) y `payload.get("zone")` ("ACTIVE" o "BENCH")

### Flujo completo corregido

```
match creado
  │
  ├─ player1 conecta WebSocket → WAITING
  │   └─ UI: spinner "Esperando rival..."
  │
  ├─ player2 se une (REST) → WAITING (ambos)
  │   └─ snapshot actualizado: player2Id seteado
  │   └─ UI para player1: aparece botón READY
  │   └─ UI para player2: botón READY al conectar
  │
  ├─ ambos clickean READY → transición a SETUP
  │   └─ Engine: shuffle, draw 7, mulligan, snapshot con hands
  │   └─ broadcastToMatch: STATE_UPDATE status=SETUP + board
  │   └─ UI: overlay muestra mano + zonas Active/Bench
  │
  ├─ ambos colocan 1+ Pokémon Basic/EX → Engine: ambos placed → ACTIVE
  │   └─ Engine: set 6 prize cards, coin flip, first turn
  │   └─ broadcastToMatch: STATE_UPDATE status=ACTIVE
  │   └─ UI: overlay desaparece → se revela <app-game-board>
  │
  └─ juego en curso (ActiveState + TurnManager)
```

### Entregable

- Flujo WAITING → SETUP → ACTIVE funcional con dos jugadores
- UI de colocación de Pokémon durante SETUP
- Transición automática al tablero de juego cuando ambos colocaron
