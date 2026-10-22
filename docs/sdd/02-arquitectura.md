# 02 - Arquitectura

## Arquitectura General

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (Angular)                         │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────┐  │
│  │ Pokedex  │  │ Deck Builder │  │  Lobby   │  │ Game Board │  │
│  │ Feature  │  │   Feature    │  │ Feature  │  │  Feature   │  │
│  └────┬─────┘  └──────┬───────┘  └────┬─────┘  └─────┬──────┘  │
│       │               │               │               │         │
│  ┌────┴───────────────┴───────────────┴───────────────┴──────┐  │
│  │                    Core Layer                              │  │
│  │  Services │ Interceptors │ Models │ Constants │ Pipes │ UI │  │
│  └────────────────────────┬──────────────────────────────────┘  │
└───────────────────────────┼─────────────────────────────────────┘
                            │
              HTTP REST API │  WebSocket (tiempo real)
                            │
┌───────────────────────────┼─────────────────────────────────────┐
│                        SERVER (Spring Boot)                      │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Card   │  │   Deck   │  │  Match   │  │   Game Engine    │  │
│  │Controller│  │Controller│  │Controller│  │   (Core Logic)   │  │
│  └────┬────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │            │            │                   │            │
│  ┌────┴────────────┴────────────┴───────────────────┴────────┐  │
│  │                    Service Layer (Cache-Aside)             │  │
│  │  CardService@Cacheable │ DeckService │ MatchService │ ... │  │
│  └────────────────────────┬──────────────────────────────────┘  │
│                           │                                     │
│  ┌────────────────────────┴──────────────────────────────────┐  │
│  │              Cache Layer (Caffeine)                        │  │
│  │  cards │ cards-search  ← TTL 14d, max 500, evict on sync  │  │
│  └────────────────────────┬──────────────────────────────────┘  │
│                           │  Miss                               │
│  ┌────────────────────────┴──────────────────────────────────┐  │
│  │                  Repository Layer (JPA)                    │  │
│  │  CardRepo │ DeckRepo │ MatchRepo │ GameStateRepo          │  │
│  └────────────────────────┬──────────────────────────────────┘  │
└───────────────────────────┼─────────────────────────────────────┘
                            │
                    ┌───────┴───────┐
                    │  H2 (dev)     │
                    │  PostgreSQL   │
                    │  (prod)       │
                    └───────────────┘
```

## Patrones de Diseño (RNF-04)

### State Pattern

**Ubicación:** `engine/state/`

**Propósito:** Gestionar el ciclo de vida global de la partida. Las sub-fases de turno son delegadas al `TurnManager` para evitar el acoplamiento y la explosión de complejidad en la máquina de estados principal.

```
MatchState (interfaz)
├── WaitingState   → espera jugadores
├── SetupState     → configuración inicial (mulligan, monedas, colocación inicial)
├── ActiveState    → juego activo (DRAW → ACTIONS → ATTACK → BETWEEN_TURNS)
└── FinishedState  → partida terminada con registro de ganador
```

**Transiciones:**
```
WAITING ──[player joins]──► SETUP ──[both ready]──► ACTIVE
                                                        │
                          [win condition met] ──► FINISHED
```

### Strategy Pattern

**Ubicación:** `engine/effects/`

**Propósito:** Implementar el comportamiento dinámico de las condiciones especiales de forma intercambiable y extensible.

```
StatusEffectStrategy (interfaz)
├── BurnedEffect     → entre turnos: 2 contadores de daño, flip para quitar al final de su turno
├── PoisonedEffect   → entre turnos: 1 contador de daño permanente
├── AsleepEffect     → entre turnos: flip para despertar. Si falla, permanece dormido y saltea turno. Impide retirada.
├── ParalyzedEffect  → impide atacar y retirar. Se quita automáticamente al final del próximo turno del dueño.
└── ConfusedEffect   → al atacar: flip de moneda. Heads = ejecuta; Tails = 30 de auto-daño e interrupción.
```

### Chain of Responsibility

**Ubicación:** `engine/combat/steps/`

**Propósito:** Pipeline de resolución de ataques con 7 pasos encadenados.

```
EnergyValidationStep → ConfusionCheckStep → TargetSelectionStep
→ PreAttackEffectStep → AttackModifierStep → DamageCalculationStep
→ PostDamageEffectStep
```

Cada paso recibe el contexto del ataque (`AttackContext`) y decide de forma autónoma si continuar o interrumpir la cadena.

### Observer Pattern (Desacoplado de Infraestructura)

**Ubicación:** `engine/events/` (Publisher) y `websocket/listeners/` (Listeners)

**Propósito:** El motor de juego publica eventos de dominio puro utilizando mecanismos nativos de Spring (`ApplicationEventPublisher`). La capa de infraestructura de WebSockets se suscribe a ellos de forma asíncrona, traduciéndolos a mensajes STOMP (`GameStateUpdateMessage`), aislando por completo el core de la aplicación del protocolo de red.

```
GameEventType (enum) → GameEvent (modelo) → GameEventPublisher (publicador)
```

### Repository Pattern

**Ubicación:** `repositories/`

**Propósito:** Abstracción completa del acceso a datos relacionales sobre JPA, permitiendo el intercambio transparente entre H2 en archivo (dev) y PostgreSQL en producción.

```
CardRepository extends JpaRepository<CardEntity, UUID>
DeckRepository extends JpaRepository<DeckEntity, UUID>
MatchRepository extends JpaRepository<MatchEntity, UUID>
GameStateRepository extends JpaRepository<GameStateEntity, UUID>
```

### Facade Pattern

**Ubicación:** `engine/GameEngine.java`

**Propósito:** Interfaz unificada y simplificada para los subsistemas complejos del engine (state machine, combat pipeline, rule validation, effects managment) hacia el exterior (servicios de aplicación).

### Mapper Pattern

**Ubicación:** `mappers/`

**Propósito:** Conversión DTO ↔ Entity usando ModelMapper.

```
CardMapper, DeckMapper, MatchMapper
```

## Estructura de Paquetes Backend

```
ar.edu.utn.frc.tup.piii/
├── Application.java
├── configs/
│   ├── CacheConfig.java
│   ├── CorsConfig.java
│   ├── DataInitializer.java
│   ├── MappersConfig.java
│   ├── RestTemplateConfig.java
│   ├── SchedulingConfig.java
│   ├── SecurityConfig.java
│   ├── SpringDocConfig.java
│   └── WebSocketConfig.java
├── controllers/
│   ├── CardController.java
│   ├── DeckController.java
│   ├── MatchController.java
│   └── PingController.java
├── dtos/
│   ├── card/
│   │   ├── CardDTO.java
│   │   └── CardFilterDTO.java
│   ├── common/
│   │   └── ErrorApi.java
│   ├── deck/
│   │   ├── CreateDeckDTO.java
│   │   ├── DeckDTO.java
│   │   └── DeckValidationResultDTO.java
│   └── match/
│       ├── CreateMatchDTO.java
│       ├── MatchDTO.java
│       └── JoinMatchDTO.java
├── entities/
│   ├── BaseEntity.java
│   ├── CardEntity.java
│   ├── DeckCardEntity.java
│   ├── DeckEntity.java
│   ├── GameStateEntity.java
│   ├── MatchEntity.java
│   └── SyncMetadataEntity.java
├── engine/
│   ├── GameEngine.java
│   ├── combat/
│   │   ├── AttackResolver.java
│   │   ├── DamageCalculator.java
│   │   └── steps/
│   │       ├── AttackModifierStep.java
│   │       ├── AttackStep.java
│   │       ├── ConfusionCheckStep.java
│   │       ├── DamageCalculationStep.java
│   │       ├── EnergyValidationStep.java
│   │       ├── PostDamageEffectStep.java
│   │       ├── PreAttackEffectStep.java
│   │       └── TargetSelectionStep.java
│   ├── effects/
│   │   ├── StatusEffectManager.java
│   │   └── StatusEffectStrategy.java
│   ├── events/
│   │   ├── GameEvent.java
│   │   ├── GameEventPublisher.java
│   │   └── GameEventType.java
│   ├── models/
│   │   ├── EnergyCard.java
│   │   ├── GameBoard.java
│   │   ├── GameCard.java
│   │   ├── MatchSnapshot.java
│   │   ├── PlayerBoard.java
│   │   ├── PokemonCard.java
│   │   ├── PokemonInPlay.java
│   │   ├── TrainerCard.java
│   │   └── TurnContext.java
│   ├── rules/
│   │   ├── RuleValidator.java
│   │   ├── TurnManager.java
│   │   └── VictoryConditionChecker.java
│   └── state/
│       ├── ActiveState.java
│       ├── FinishedState.java
│       ├── MatchState.java
│       ├── SetupState.java
│       └── WaitingState.java
├── enums/
│   ├── AbilityType.java
│   ├── CardType.java
│   ├── EnergyType.java
│   ├── GamePhase.java
│   ├── GameZone.java
│   ├── MatchStatus.java
│   ├── PokemonStage.java
│   ├── SpecialCondition.java
│   ├── TrainerSubtype.java
│   └── TurnPhase.java
├── exceptions/
│   ├── DeckValidationException.java
│   ├── EntityNotFoundException.java
│   ├── GameRuleViolationException.java
│   ├── GlobalExceptionHandler.java
│   └── InvalidActionException.java
├── mappers/
│   ├── CardMapper.java
│   ├── DeckMapper.java
│   └── MatchMapper.java
├── repositories/
│   ├── CardRepository.java
│   ├── DeckRepository.java
│   ├── GameStateRepository.java
│   ├── MatchRepository.java
│   └── SyncMetadataRepository.java
├── scheduler/
│   ├── CacheWarmupRunner.java
│   └── CardSyncScheduler.java
├── services/
│   ├── CardService.java
│   ├── DeckService.java
│   ├── GameStatePersistenceService.java
│   └── MatchService.java
└── websocket/
    ├── GameWebSocketHandler.java
	 ├── listeners/
    │   └── EngineEventListener.java
    └── messages/
        ├── GameActionMessage.java
        └── GameStateUpdateMessage.java
```

## Estructura de Carpetas Frontend
El frontend adopta estrictamente la guía de estilos de Angular para la nomenclatura de archivos (`.component.ts`, `.service.ts`, `.routes.ts`, `.config.ts`) con el fin de garantizar consistencia con las herramientas CLI del ecosistema.

```
src/app/
├── app.ts / app.html / app.css
├── app.routes.ts
├── app.config.ts
├── core/
│   ├── constants/
│   │   ├── card-types.ts
│   │   ├── energy-types.ts
│   │   ├── game-phases.ts
│   │   └── special-conditions.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   ├── interceptors/
│   │   └── error.interceptor.ts
│   └── services/
│       └── websocket.service.ts
├── features/
│   ├── deck-builder/
│   │   ├── components/
│   │   │   ├── card-catalog/card-catalog.component.ts
│   │   │   ├── card-detail-modal/card-detail-modal.component.ts
│   │   │   ├── deck-editor/deck-editor.component.ts
│   │   │   ├── deck-list/deck-list.component.ts
│   │   │   └── deck-summary/deck-summary.component.ts
│   │   ├── pages/
│   │   │   └── deck-builder-page/deck-builder-page.component.ts
│   │   └── services/
│   │       ├── card.service.ts
│   │       └── deck.service.ts
│   ├── game/
│   │   ├── components/
│   │   │   │   │   │   ├── active-pokemon/active-pokemon.component.ts
│   │   │   ├── bench-area/bench-area.component.ts
│   │   │   ├── action-panel/action-panel.component.ts
│   │   │   ├── attack-selector/attack-selector.component.ts
│   │   │   ├── deck-pile/deck-pile.component.ts
│   │   │   ├── discard-pile/discard-pile.component.ts
│   │   │   ├── energy-attachment/energy-attachment.component.ts
│   │   │   ├── evolution-selector/evolution-selector.component.ts
│   │   │   ├── game-over/game-over.component.ts
│   │   │   ├── hand-area/hand-area.component.ts
│   │   │   ├── notification-toast/notification-toast.component.ts
│   │   │   ├── opponent-area/opponent-area.component.ts
│   │   │   ├── player-area/player-area.component.ts
│   │   │   ├── prize-cards/prize-cards.component.ts
│   │   │   ├── setup-phase/setup-phase.component.ts
│   │   │   └── stadium-area/stadium-area.component.ts
│   │   ├── pages/
│   │   │   └── game-page/game-page.component.ts
│   │   └── services/
│   │       ├── game-state.service.ts
│   │       └── game-websocket.service.ts
│   ├── lobby/
│   │   ├── components/
│   │   │   ├── create-match-dialog/create-match-dialog.component.ts
│   │   │   └── match-list/match-list.component.ts
│   │   ├── pages/
│   │   │   └── lobby-page/lobby-page.component.ts
│   │   └── services/
│   │       └── lobby.service.ts
│   └── pokedex/
│       └── pages/
│           └── pokedex-page/pokedex-page.component.ts
└── shared/
    ├── components/
	 │   ├── card/card.component.ts
    │   ├── card-stack/card-stack.component.ts
    │   ├── damage-counter/damage-counter.component.ts
    │   ├── energy-badge/energy-badge.component.ts
    │   ├── game-log/game-log.component.ts
    │   └── status-indicator/status-indicator.component.ts
    ├── directives/
    │   └── draggable.directive.ts
    ├── models/
    │   ├── card.model.ts
    │   ├── deck.model.ts
    │   ├── game.model.ts
    │   └── match.model.ts
	 ├── dtos/
    │   └── api-contracts.model.ts
    ├── pipes/
    │   ├── energy-type.pipe.ts
    │   └── hp-display.pipe.ts
    └── ui/
        ├── button/
        ├── loading-spinner/
        └── modal/
```

## Diagramas UML Sugeridos

Los diagramas base se ubicarán en `BE/docs/app_doc/diagrams/` expandiendo los archivos `.puml` iniciales:

1. **Class Diagram** (`class.puml`): Modelado detallado de Entidades JPA y Modelos de Dominio en memoria.
2. **Component Diagram** (`components.puml`): Desacoplamiento estricto de capas (Presentation, Service, Engine Core, Infrastructure, DB).
3. **Sequence Diagram** (`sequences.puml`): Flujo bidi completo de extremo a extremo desde el trigger de acción WebSocket en Frontend hasta el broadcast de sincronización del Engine.
4. **State Diagram** (`states.puml`): Máquina de estados global de la partida (`MatchState`) y ciclo de fases de juego administrado por el `TurnManager`.
5. **ER Diagram**(`er_diagram.puml`): Diagrama Entidad-Relación explícito con restricciones relacionales.
