# 09 - Plan de Ramas Git

## Estrategia de Branching (GitFlow Sincronizado)

Para evitar colisiones de código en los modelos en memoria y asegurar que el Frontend pueda avanzar utilizando los contratos preestablecidos, las ramas de características (*features*) se abrirán de forma paralela entre capas y se integrarán semanalmente a `develop`.

```text
main ────────────────────────────────────────────────────────────────────────► (producción)
  ╲
    develop ──────────────────────────────────────────────────────────────────► (integración)
      │
      ├── Semanas 1-2 (Fundación e Inventarios)
      │     ├── feature/cards-sync-be
      │     ├── feature/pokedex-fe
      │     ├── feature/deck-builder-be
      │     ├── feature/deck-builder-fe
      │     └── feature/card-cache-be
      │
      ├── Semana 3 (Red e Infraestructura)
      │     ├── feature/matches-lobby-be
      │     └── feature/lobby-sockets-fe
      │
      ├── Semana 4 (Core Engine y Tablero Base)
      │     ├── feature/game-engine-core-be   (State Machine + TurnManager + JSONB)
      │     └── feature/game-board-layout-fe   (GameStateService + SetupPhase)
      │
      ├── Semana 5 (Sistema de Usuarios, Combate e Interacción)
      │     ├── feature/user-auth-be           (JWT, UserEntity, AuthController, WS JWT)
      │     ├── feature/user-auth-fe           (AuthService, Login, Register, Navbar)
      │     ├── feature/game-combat-steps-be   (Chain of Resp + Strategy Effects)
      │     └── feature/game-actions-ui-fe     (ActionPanel + Drag&Drop + PromoteModal)
      │
      └── Semana 6 (Estabilización)
            └── feature/testing-quality-ci
```

## Asignación de Personas

| Desarrollador | Semana 1 | Semana 2 | Semana 3 | Semana 4 | Semana 5 | Semana 6 |
|---|---|---|---|---|---|--|
| P1 (BE Core) | Base del proyecto, Enums, Excepciones y Scripts Flyway V1/V2. | Algoritmo del `DeckValidationService` (Reglas de cartas y ACE SPEC). | Esquema de MatchEntity, script V4 e índices compuestos del Lobby.| Máquina de Estados del motor (`MatchState`: Waiting, Setup, Finished). | Gender enum, UserEntity, UserRepository, V11 migration, BCrypt bean + JwtAuthenticationFilter en SecurityConfig. DamagePipeline (DamageCalculator + 7 AttackStep). | Optimización de índices en PostgreSQL y Tuning de Queries.
| **P2** (BE Feat) | Cliente HTTP externo para pokemontcg.io y endpoint `/api/cards/sync`. | CRUD de `/api/decks` y mapeo de contratos con `ModelMapper`. | API REST de `/api/matches` para el Lobby (Crear/Unirse). | Implementación de flags de validación dentro de `TurnContext`. | DTOs de auth, UserMapper, UserService/Impl, AuthController + UserController. StatusEffectStrategy (5 efectos). | Cobertura final de tests unitarios e integración en JaCoCo >= 80%.
| **P3** (BE Real-Time) | Soporte en el setup inicial de entidades y DTOs de cartas. | Implementación de Cache-Aside con Caffeine (`@Cacheable`, `@CacheEvict`), `CardSyncScheduler` programado (domingos 00:00), migración V3 (`sync_metadata`), `CacheWarmupRunner`. | Configuración de `WebSocketConfig`, `GameWebSocketHandler` y mensajes STOMP. | Lógica de `TurnManager` (`TurnPhase`), Sistema de Eventos de dominio y persistencia atómica en `JSONB snapshot`, migración V5. | Migrar AuthChannelInterceptor + PlayerIdChannelInterceptor a JWT. Tests WS con JWT. Soporte en pruebas del pipeline de combate. | Cobertura de pruebas sobre el pipeline de combate y simulación de KOs.
| **P4** (FE Core) | Inicialización de Angular 20, `app.config.ts`, interceptores y ruteo base.| Modelos TypeScript, tipados estrictos de cartas y `DeckService`. | Creación de `websocket.service.ts` (Manejo de conexión y reintentos STOMP). | Centralización del `MatchSnapshot` en el Signal de `GameStateService`. | AuthService, JwtInterceptor, AuthGuard, actualizar routes + config + error.interceptor. Orquestación de GamePage. | Perfil de compilación de producción, remoción de fugas de memoria y linters.
| **P5** (FE UI) | Diseño y maquetado de `PokedexPage` con control flow nativo (`@for`). | Desarrollo de `CardCatalogComponent` y `DeckSummaryComponent` (Alertas). | Maquetado de `LobbyPage` y componentes de listas de salas disponibles. | Desarrollo del layout de bloqueo inicial de la partida (`SetupPhaseComponent`). | LoginPage, RegisterPage, ProfilePage. Subcomponentes del tablero (PlayerArea, OpponentArea, Piles). | Implementación de pruebas de humo y flujos de interfaz con Cypress/Playwright.
| **P6** (FE Interactivity) | Creación del componente compartido `CardComponent` y manejo de estilos optimizados. | Formulario reactivo y lógicas de adición/quitado en `DeckEditorComponent`. | Desarrollo del cuadro de diálogo para la creación de partidas con mazo. | Soporte en el enrutamiento interactivo dinámico de la ruta `/game/:matchId`. | Navbar en AppComponent (login/logout/profile según auth). CDK Drag & Drop + PromoteModal. | Ajustes de accesibilidad, pulido de animaciones CSS y toques estéticos finales.

#Matriz de Asignación de Roles por Feature

El equipo se divide de forma balanceada para asegurar que cada característica REST o Real-Time se desarrolle en simultáneo (Backend + Frontend), mitigando riesgos de desalineación en DTOs.

| Célula | Responsabilidad | Ramas Backend (BE) | Personas BE | Ramas Frontend (FE) | Personas FE | Semanas
|---|---|---|---|---|---|---|
| Célula A | Catálogos y Mazos | `feature/cards-sync-be`, `feature/deck-builder-be`, `feature/card-cache-be` | P1, P2, P3 | `feature/pokedex-fe`, `feature/deck-builder-fe` | P4, P5, P6 | 1 - 2 |
| Célula B | Conectividad y Lobby | `feature/matches-lobby-be` | P2, P3 | `feature/lobby-sockets-fe` | P4, P5, P6 | 3 |
| Célula C | Lógica y Ciclo del Motor | `feature/game-engine-core-be` | P1, P3 | `feature/game-board-layout-fe` | P4, P5, P6 | 4 |
| Célula D | Sistema de Usuarios | `feature/user-auth-be` | P1, P2, P3 | `feature/user-auth-fe` | P4, P5, P6 | 5 |
| Célula E | Combate e Interacción | `feature/game-combat-steps-be` | P1, P2 | `feature/game-actions-ui-fe` | P4, P5, P6 | 5 |
| Todo el Equipo | QA y Despliegue | `feature/testing-quality-ci` | Todos | `feature/testing-quality-ci` | Todos | 6 |

## Políticas de Commit y Convención de Mensajes (Conventional Commits)
Los agentes y desarrolladores deben estructurar sus mensajes de commit obligatoriamente bajo el estándar formal. Esto automatiza la lectura de logs y evita la pérdida de contexto.

```text
<tipo>(<ámbito>): <descripción corta en minúsculas y modo imperativo>

[cuerpo del mensaje opcional con detalles técnicos]
```

## Tipos Permitidos
- `feat`: Una nueva característica de software (ej. `feat(deck): implementar validador de cartas ace spec`).
- `fix`: Resolución de un bug técnico (ej. `fix(engine): corregir pausa de turnos ante KO por veneno`).
- `docs`: Modificaciones exclusivas en la documentación del proyecto o diagramas (ej. `docs(architecture): actualizar esquema de carpetas frontend`).
- `style`: Cambios cosméticos que no alteran la lógica (espacios, formateo, linters).
- `refactor`: Reestructuración de código que no añade features ni corrige bugs (ej. `refactor(engine): mudar fases de turno al turnmanager`).
- `test`: Adición o corrección de pruebas unitarias o e2e (ej. `test(calculator): cubrir debilidades del daño base`).
- `chore`: Actualización de dependencias de Maven, npm o tareas de mantenimiento del build.

## Flujo de Trabajo para Pull Requests (PR)
Reglas de Oro para Integración
1. Queda estrictamente prohibido realizar commits directos sobre las ramas `main` y `develop`.
2. Toda característica nace de `develop` con la nomenclatura `feature/nombre-corto`.
3. Antes de abrir un PR hacia `develop`, el desarrollador debe realizar obligatoriamente un `git pull origin develop` y resolver conflictos de forma local en su rama de trabajo.

## Reglas de Branching

1. **`main`** solo recibe merges de `develop` (releases)
2. **`develop`** es la rama de integración
3. **`feature/*`** se crea desde `develop` actualizado
4. **Cada feature tiene su propia rama** - no se comparten ramas
5. **PRs a `develop` requieren al menos 1 review** de otro integrante
6. **Antes de hacer PR, hacer `git rebase develop`** en la rama feature

## Flujo de Integración Semanal

```
Lunes:     Cada persona crea/actualiza su rama desde develop
Miércoles: Rebase de develop → feature (traer cambios de otros)
Viernes:   PR a develop (review cruzado entre 2 personas)
Sábado:    Merge a develop (solo si pasa CI)
```

## Dependencias entre Ramas

```
Semana 1:
  feature/cards-sync-be (P1 — base, enums, entidades; P2 — sync HTTP y endpoints)
  feature/pokedex-fe (P4 — app.config, modelos, rutas; P5 — layout; P6 — CardComponent)

Semana 2:
  feature/deck-builder-be (P1 — DeckValidationService; P2 — CRUD y mapeo)
     └── depende de feature/cards-sync-be
  feature/deck-builder-fe (P5 — Catalog+Summary; P6 — DeckEditor)
     └── depende de feature/pokedex-fe + feature/cards-sync-be
  feature/card-cache-be (P3 — CacheConfig, CardSyncScheduler, SyncMetadataEntity)
     └── depende de feature/cards-sync-be (ya mergeada a develop)

Semana 3:
  feature/matches-lobby-be (P2 — REST /api/matches; P3 — WebSocketConfig y STOMP)
     └── depende de feature/cards-sync-be
  feature/lobby-sockets-fe (P4 — websocket.service.ts; P5 — LobbyPage; P6 — CreateMatchDialog)
     └── depende de feature/pokedex-fe

Semana 4:
  feature/game-engine-core-be (P1 — State Machine MatchState; P3 — TurnManager + V5)
     └── depende de feature/matches-lobby-be
  feature/game-board-layout-fe (P4 — GameStateService + computed; P5 — SetupPhase; P6 — routing /game/:matchId)
     └── depende de feature/lobby-sockets-fe + feature/game-engine-core-be

Semana 5:
  feature/user-auth-be (P1 — entidad+migración+security; P2 — DTOs+servicios+controllers; P3 — WS JWT)
     └── independiente (solo necesita develop post-Semana 4)
  feature/user-auth-fe (P4 — servicios+guard+config; P5 — páginas; P6 — navbar)
     └── independiente (solo necesita develop post-Semana 4)
  feature/game-combat-steps-be (P1 — DamagePipeline AttackStep; P2 — StatusEffect Strategy)
     └── depende de feature/game-engine-core-be
  feature/game-actions-ui-fe (P4 — GamePage; P5 — PlayerArea/OpponentArea; P6 — Drag&Drop + PromoteModal)
     └── depende de feature/game-board-layout-fe + feature/game-engine-core-be
```

## Archivos Compartidos (Puntos de Conflicto)

| Archivo | Quién lo toca | Cuándo | Cómo se maneja |
|---|---|---|---|
| `application.properties` | P1 (Semana 1), P3 (Semana 2: propiedades de cache y scheduler) | Semana 1 y 2 | P1 agrega dependencias de Caffeine; P3 agrega propiedades de cache. Coordinar en PR review. |
| `pom.xml` | P1 (Semana 1), P3 (Semana 2: dependencia Caffeine) | Semana 1 y 2 | P1 agrega `spring-boot-starter-cache` y `caffeine`. Ya hecho en develop. |
| `.gitignore` | P1 | Semana 1 | Se define una vez |
| `app.config.ts` | P4 | Semana 1 (HttpClient), Semana 3 (WebSocket providers) | P4 es responsable de toda la configuración base de Angular |
| `app.routes.ts` | P4 | Semana 1 (rutas base), Semana 4 (ruta dinámica /game/:matchId) | Cada adición se hace en la rama feature correspondiente |
| `enums/*` (BE) | P1 | Semana 1 | Se definen todos de una vez |
| `exceptions/*` (BE) | P1 | Semana 1 | Se implementan todos de una vez |
| `shared/models/` (FE) | P4 | Semanas 1-3 | P4 crea y mantiene todos los modelos TypeScript (card, deck, match, game) |
| `SecurityConfig.java` | P1 | Semana 5 | P1 agrega `BCryptPasswordEncoder` bean + registra `JwtAuthenticationFilter` |
| `app.ts` (AppComponent) | P6 | Semana 5 | P6 agrega navbar con login/logout/profile según estado de auth |
| `AuthChannelInterceptor.java` | P3 | Semana 5 | P3 migra de `X-Player-Id` a validación JWT en CONNECT STOMP |

## Desglose por Persona por Rama

Esta tabla detalla las responsabilidades específicas de cada persona dentro de cada rama feature, eliminando cualquier ambigüedad sobre quién trabaja en qué.

| Semana | Rama | Persona | Tareas específicas |
|--------|------|---------|-------------------|
| 1 | `feature/cards-sync-be` | P1 | BaseEntity, Enums, Excepciones, GlobalExceptionHandler, Scripts Flyway V1/V2, CardEntity, CardRepository |
| 1 | `feature/cards-sync-be` | P2 | CardService (sync pokemontcg.io), CardController (/api/cards), CardMapper, DTOs de carta |
| 1 | `feature/pokedex-fe` | P4 | app.config.ts, ErrorInterceptor, app.routes.ts, modelos TypeScript (card.model.ts), constants |
| 1 | `feature/pokedex-fe` | P5 | PokedexPage con @for, OnPush, consumo de /api/cards |
| 1 | `feature/pokedex-fe` | P6 | CardComponent compartido, estilos base |
| 2 | `feature/deck-builder-be` | P1 | DeckValidationService, algoritmo de reglas (60 cartas, básicos, ACE SPEC) |
| 2 | `feature/deck-builder-be` | P2 | DeckController CRUD /api/decks, DeckMapper, CreateDeckDTO, DeckValidationResultDTO |
| 2 | `feature/card-cache-be` | P3 | CacheConfig (@EnableCaching, Caffeine), SchedulingConfig (@EnableScheduling), CardSyncScheduler (@Scheduled cron), SyncMetadataEntity, SyncMetadataRepository, CacheWarmupRunner (@Order(2)), modificar CardServiceImpl (@Cacheable, @CacheEvict), migración V3 |
| 2 | `feature/deck-builder-fe` | P4 | Modelos TypeScript de cartas y mazo, tipados estrictos, DeckService |
| 2 | `feature/deck-builder-fe` | P5 | CardCatalogComponent, DeckSummaryComponent (alertas de validación) |
| 2 | `feature/deck-builder-fe` | P6 | DeckEditorComponent (formulario reactivo, adición/quitado de cartas) |
| 3 | `feature/matches-lobby-be` | P2 | MatchController REST /api/matches, MatchDTO, CreateMatchDTO, JoinMatchDTO |
| 3 | `feature/matches-lobby-be` | P3 | WebSocketConfig, GameWebSocketHandler, mensajes STOMP, AuthChannelInterceptor, GameActionMessage |
| 3 | `feature/lobby-sockets-fe` | P4 | websocket.service.ts (conexión STOMP, reintentos, reconexión) |
| 3 | `feature/lobby-sockets-fe` | P5 | LobbyPage layout, MatchListComponent (listado de salas) |
| 3 | `feature/lobby-sockets-fe` | P6 | CreateMatchDialog (diálogo para crear partida con mazo) |
| 4 | `feature/game-engine-core-be` | P1 | MatchState, WaitingState, SetupState, FinishedState, RuleValidator |
| 4 | `feature/game-engine-core-be` | P3 | TurnManager (TurnPhase), GameStateEntity, migración V5, GameEventPublisher |
| 4 | `feature/game-board-layout-fe` | P4 | GameStateService con Signals, MatchSnapshot synchronization, computed myBoard/opponentBoard |
| 4 | `feature/game-board-layout-fe` | P5 | SetupPhaseComponent, GamePage shell |
| 4 | `feature/game-board-layout-fe` | P6 | Enrutamiento dinámico /game/:matchId |
| 5 | `feature/user-auth-be` | P1 | Gender enum, UserEntity, UserRepository, V11 migration (CREATE TABLE users), BCryptPasswordEncoder bean en SecurityConfig, JwtAuthenticationFilter (OncePerRequestFilter) |
| 5 | `feature/user-auth-be` | P2 | RegisterRequestDTO, LoginRequestDTO, AuthResponseDTO, UserProfileDTO, UserMapper, UserService + UserServiceImpl, AuthController (POST /api/auth/register, /login), UserController (GET /api/users/me) |
| 5 | `feature/user-auth-be` | P3 | Migrar AuthChannelInterceptor de X-Player-Id a Authorization Bearer JWT, migrar PlayerIdChannelInterceptor, tests de integración WS con JWT |
| 5 | `feature/user-auth-fe` | P4 | AuthService (login/register/logout con signals + localStorage), JwtInterceptor (adjunta Bearer token), AuthGuard (protege rutas), actualizar app.config.ts, app.routes.ts (+login/register/profile), error.interceptor.ts (redirect 401) |
| 5 | `feature/user-auth-fe` | P5 | LoginPage (formulario reactivo), RegisterPage (username+password+gender), ProfilePage (datos del usuario + lista de mazos) |
| 5 | `feature/user-auth-fe` | P6 | Navbar en AppComponent (botones Login/Register si anónimo, username+Profile+Logout si autenticado, ocultar según auth state) |
| 5 | `feature/game-combat-steps-be` | P1 | AttackResolver, DamageCalculator, 7 AttackStep (Chain of Responsibility) |
| 5 | `feature/game-combat-steps-be` | P2 | StatusEffectStrategy, PoisonedEffect, BurnedEffect, AsleepEffect, ParalyzedEffect, ConfusedEffect |
| 5 | `feature/game-combat-steps-be` | P3 | Pruebas de integración del pipeline de combate, validación de acciones STOMP en tiempo real |
| 5 | `feature/game-actions-ui-fe` | P4 | GamePage orquestación con computed perspectives |
| 5 | `feature/game-actions-ui-fe` | P5 | PlayerArea, OpponentArea, ActivePokemon, BenchArea, HandArea, Piles |
| 5 | `feature/game-actions-ui-fe` | P6 | Angular CDK Drag&Drop, PromoteModal, ActionPanel |
| 6 | `feature/testing-quality-ci` | P1 | Optimización PostgreSQL, índices compuestos |
| 6 | `feature/testing-quality-ci` | P2 | JaCoCo >= 80%, tests unitarios e integración |
| 6 | `feature/testing-quality-ci` | P3 | Tests de pipeline de combate, simulación de KOs |
| 6 | `feature/testing-quality-ci` | P4 | Build de producción, eliminación de fugas de memoria, linters |
| 6 | `feature/testing-quality-ci` | P5 | Tests E2E Cypress/Playwright |
| 6 | `feature/testing-quality-ci` | P6 | Accesibilidad, pulido de animaciones CSS |

## Convención de Commits

```
<type>(<scope>): <description>

Types:
  feat     - Nueva funcionalidad
  fix      - Corrección de bug
  docs     - Documentación
  style    - Formato, sin cambio de código
  refactor - Refactorización
  test     - Tests
  chore    - Tareas de mantenimiento

Ejemplos:
  feat(card): implementar CardEntity y CardRepository
  feat(deck): agregar validación de mazo
  fix(engine): corregir cálculo de daño con debilidad
  docs(doc): actualizar diagrama de secuencia
  test(service): agregar tests de DeckService
```

## Pull Request Template
Al abrir un PR en GitHub, se debe completar la siguiente estructura analítica:

```markdown
## Descripción
<!-- Qué cambia y por qué -->

## Cambios
<!-- Lista de cambios principales -->

## Testing
<!-- Qué se probó y cómo -->

## Dependencias
<!-- ¿Depende de otro PR? -->

## Checklist
- [ ] Código compila sin errores
- [ ] Tests pasan
- [ ] No hay conflictos con develop
- [ ] Documentación actualizada (si aplica)
```

## CI Básico (GitHub Actions)

El archivo `.github/workflows/ci.yml` automatiza la validación de calidad. El PR NO se podrá fusionar si el pipeline falla o si la cobertura de JaCoCo desciende de los mínimos del RNF-03.

```yaml
name: CI/CD Pokemon TCG Quality Gate

on:
  pull_request:
    branches: [ develop ]

jobs:
  backend-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and Verify Code Quality
        run: |
          cd BE
          mvn clean verify checkstyle:check pmd:check

      - name: Enforce JaCoCo Coverage Thresholds
        run: |
          cd BE
          mvn jacoco:check

  frontend-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: './FE/package-lock.json'

      - name: Install Dependencies & Lint
        run: |
          cd FE
          npm ci
          npm run lint

      - name: Execute Unit Tests
        run: |
          cd FE
          npm run test:headless
```
