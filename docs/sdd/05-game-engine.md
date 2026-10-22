# 05 - Game Engine

> **Nota de Arquitectura:** Esta sección representa el diseño definitivo del motor del juego. Se han desacoplado los estados globales de la partida de las fases internas de los turnos, garantizando un control atómico y libre de bugs de concurrencia o pérdida de contexto.

## Overview

El Game Engine es el núcleo aislado del sistema (RNF-02). Es el único componente con autoridad para modificar el estado del tablero, actuando el backend como la **única fuente de verdad**.

Sus responsabilidades principales son:
1. Gobernar el ciclo de vida de la partida (`MatchState`).
2. Gestionar la secuencia estricta y restricciones de las fases de los turnos (`TurnManager`).
3. Validar de forma robusta e inmutable cada acción declarada (`RuleValidator`).
4. Resolver el combate mediante un pipeline secuencial de 7 pasos (`Chain of Responsibility`).
5. Procesar las condiciones especiales mediante clases aisladas (`Strategy Pattern`).
6. Evaluar y declarar las condiciones de victoria o muerte súbita (`VictoryConditionChecker`).



---

## Máquina de Estados Globales (State Pattern)

El ciclo macro de la partida se implementa en el paquete `engine/state/`. Las transiciones modifican el estado de la mesa entera y la conectividad de los jugadores.

**`GamePhase`** (enum, 4 valores): `WAITING`, `SETUP`, `ACTIVE`, `FINISHED`. Representa el estado macro de la partida.

**`TurnPhase`** (enum, 4 valores): `DRAW`, `ACTIONS`, `ATTACK`, `BETWEEN_TURNS`. Representa las sub-fases del turno dentro de `ActiveState`.

```text
MatchState (Interfaz)
├── WaitingState   → GamePhase.WAITING. Partida creada. Permite acciones 'join' y 'ready'. Transiciona cuando ambos están listos.
├── SetupState     → GamePhase.SETUP. Orquesta la preparación automatizada: barajar mazos, robar 7 cartas, colocar básicos iniciales, separar los 6 premios y lanzar la moneda. Controla mulligans simultáneos.
├── ActiveState    → GamePhase.ACTIVE. Mantiene la partida en juego continuo. Delega el flujo interno al TurnManager (TurnPhase: DRAW → ACTIONS → ATTACK → BETWEEN_TURNS).
└── FinishedState  → GamePhase.FINISHED. Estado terminal inmutable. Registra al ganador en la DB y cierra los sockets de juego.
```

**Nota sobre SURRENDER:** En cualquier momento durante `ActiveState`, un jugador puede enviar la acción `SURRENDER`, lo que transiciona inmediatamente a `FinishedState` con el oponente como ganador.

---

## Secuencia y Reglas del Turno (`TurnManager`)

Dentro de `ActiveState`, el flujo micro es gobernado por el `TurnManager` utilizando el enum `TurnPhase` (DRAW, ACTIONS, ATTACK, BETWEEN_TURNS).

### Ciclo de Fases del Turno
1. **`DRAW` (Robo automático):** El engine roba automáticamente 1 carta para el jugador actual. No existe la acción `DRAW_CARD` en el protocolo — es transparente. *Excepción: Se saltea en el primer turno del jugador que ganó la moneda inicial.* Si el mazo está vacío, el jugador pierde inmediatamente.
2. **`ACTIONS` (Acciones):** Fase principal. El jugador puede ejecutar acciones libres (`PLACE_POKEMON`, `ATTACH_ENERGY`, `PLAY_TRAINER`, `EVOLVE`, `RETREAT`, `USE_ABILITY`).
3. **`ATTACK` (Ataque):** Declaración y resolución de daño. Invoca al pipeline de combate y **cierra el turno inmediatamente**, transitando a `BETWEEN_TURNS`.
4. **`BETWEEN_TURNS` (Procesamiento entre turnos):** Se procesan las condiciones especiales solo para el jugador cuyo turno está terminando. Luego se transiciona al turno del oponente (fase DRAW).

### Restricciones Estrictas de Turno (`RuleValidator`)
* **Evolución:** Ningún jugador puede evolucionar a ningún Pokémon durante su primer turno de la partida.
* **Ataque:** El jugador que va primero en la partida no puede declarar ataques en su primer turno.
* **Energía:** Máximo 1 carta de energía attachada por turno desde la mano a un Pokémon en juego.
* **Soportes y Estadios:** Máximo 1 `Supporter` y 1 `Stadium` jugados por turno.
* **Terminar Turno:** Si el jugador no desea atacar, usa la acción `PASS_TURN` para salir de `ACTIONS` e ir al procesamiento entre turnos (`BETWEEN_TURNS`).

---

### Interfaz MatchState

```java
public interface MatchState {
    GamePhase getPhase();
    void handleAction(GameEngine engine, GameActionMessage action);
    void enter(GameEngine engine);
    void exit(GameEngine engine);
}
```

### WaitingState

- **Entrada:** Partida creada, esperando jugadores
- **Acciones permitidas:** `join`, `ready`
- **Transición:** A `SetupState` cuando ambos jugadores están listos

### SetupState

- **Entrada:** Ambos jugadores conectados y listos
- **Acciones:**
  - Barajar mazos
  - Robar 7 cartas
  - Mostrar mano (solo al jugador correspondiente)
  - Colocar Pokémon Básico en activo
  - Colocar hasta 5 Pokémon en banca
  - Separar 6 cartas de premio
  - Lógica de mulligan (si no hay básicos: mostrar mano, rebarajar, oponente roba +1 por cada mulligan)
  - Lanzar moneda para primer turno
- **Transición:** A `ActiveState` (fase DRAW) cuando ambos terminan el setup

### ActiveState

- **Sub-fases:** DRAW → ACTIONS → ATTACK → BETWEEN_TURNS (dentro de ActiveState)
- **DRAW:** Robo automático de 1 carta (salta si es primer turno del jugador inicial). No existe acción DRAW_CARD.
- **ACTIONS:** Acciones del turno (colocar básicos, evolucionar, attachar energía, jugar trainers, retirar)
- **ATTACK:** Resolver ataque (pipeline de 7 pasos), transiciona a BETWEEN_TURNS
- **BETWEEN_TURNS:** Procesar condiciones especiales solo del jugador cuyo turno termina (Poisoned → Burned → Asleep → Paralyzed check), luego transiciona al turno del oponente
- **Transición:** Loop entre sub-fases hasta condición de victoria

### FinishedState

- **Entrada:** Se cumple una condición de victoria
- **Acciones:** Ninguna (estado terminal)
- **Resultado:** Registrar ganador, persistir estado final

## Pipeline de Resolución de Ataque (Chain of Responsibility)
El ataque se procesa secuencialmente en `engine/combat/steps/`. La interrupción de cualquier paso detiene el daño.
### Interfaz AttackStep

```java
public interface AttackStep {
    void execute(AttackContext context);
    void setNext(AttackStep next);
}
```

### Pasos del Pipeline

1. **EnergyValidationStep** - Verificar que el Pokémon tiene la energía necesaria para el ataque
2. **ConfusionCheckStep** - Si está confundido, flip: heads = normal, tails = auto-daño 30
3. **TargetSelectionStep** - Seleccionar objetivo (Pokémon activo del oponente)
4. **PreAttackEffectStep** - Ejecutar efectos previos al daño (ej: "descartar energía del oponente")
5. **AttackModifierStep** - Aplicar modificadores (Stadium, Tools, Abilities que modifican daño)
6. **DamageCalculationStep** - Calcular daño final:
   - Daño base del ataque
   - Debilidad: ×2 si el tipo del atacante es débil del defensor
   - Resistencia: -20 si el tipo del atacante es resistente del defensor
   - Mínimo 0
   - 1 contador de daño por cada 10 de daño
7. **PostDamageEffectStep** - Aplicar efectos posteriores (condiciones especiales, efectos del ataque)

### AttackContext

```java
public class AttackContext {
    PokemonInPlay attacker;
    PokemonInPlay defender;
    Attack selectedAttack;
    Integer baseDamage;
    Integer finalDamage;
    List<SpecialCondition> effectsToApply;
    boolean attackSuccessful;
    String failureReason;
}
```

## Efectos de Estado (Strategy Pattern)
Para cumplir con SOLID, el motor utiliza el patrón Strategy. **Se deben crear 5 clases físicas obligatorias** que implementen la interfaz `StatusEffectStrategy` en `engine/effects/`:
1. `PoisonedEffect.java`
2. `BurnedEffect.java`
3. `AsleepEffect.java`
4. `ParalyzedEffect.java`
5. `ConfusedEffect.java` *(Nota: Esta clase solo actúa interrumpiendo ataques durante el paso 2 del pipeline).*

### Fase "Entre Turnos" (`BETWEEN_TURNS`)
Cuando un jugador ataca o pasa el turno (`PASS_TURN`), la partida transiciona a `BETWEEN_TURNS`. El `StatusEffectManager` procesa los efectos **solo para el jugador cuyo turno está terminando**, en este orden estricto:
1. **Veneno (`PoisonedEffect`):** Agrega 1 contador de daño (10 HP).
2. **Quemadura (`BurnedEffect`):** Lanza moneda. Heads = se cura. Tails = recibe 2 contadores de daño (20 HP).
3. **Sueño (`AsleepEffect`):** Lanza moneda. Heads = se despierta. Tails = sigue dormido (no podrá atacar ni retirarse).
4. **Parálisis (`ParalyzedEffect`):** No aplica daño. Se verifica si la parálisis debe removerse (se quita automáticamente al final del turno del oponente).

**Nota importante sobre KO durante BETWEEN_TURNS:** Si un KO ocurre durante el procesamiento de condiciones entre turnos, el flujo se pausa obligatoriamente. El motor bloquea el inicio del siguiente turno hasta recibir la acción `SET_ACTIVE` del jugador afectado.
### Interfaz StatusEffectStrategy

```java
public interface StatusEffectStrategy {
    SpecialCondition getType();
    void applyBetweenTurns(PokemonInPlay pokemon, GameBoard board);
    void onAttack(PokemonInPlay pokemon, AttackContext context);
    boolean canCoexist(SpecialCondition other);
    void remove(PokemonInPlay pokemon);
}
```

### Implementaciones

#### BurnedEffect

- **Entre turnos:** Colocar 2 contadores de daño
- **Remoción:** Flip de moneda al final del turno: heads = se quita
- **Coexistencia:** Puede coexistir con cualquier condición excepto otra quemadura

#### PoisonedEffect

- **Entre turnos:** Colocar 1 contador de daño
- **Remoción:** Se mantiene hasta evolución o retirada
- **Coexistencia:** Puede coexistir con cualquier condición excepto otro envenenamiento

#### AsleepEffect

- **Entre turnos:** Flip de moneda: heads = despierta, tails = permanece dormido (saltea turno)
- **Coexistencia:** Mutuamente excluyente con Confused y Paralyzed

#### ParalyzedEffect

- **Efecto:** El Pokémon no puede atacar NI retirarse durante el turno del propietario. No saltea el turno completo — el jugador aún puede realizar otras acciones (evolucionar, jugar trainers, attachar energía).
- **Remoción:** Se quita automáticamente al final del turno del oponente (entre turnos).
- **Coexistencia:** Mutuamente excluyente con Asleep y Confused

#### ConfusedEffect

- **Al atacar:** Flip de moneda: heads = ataque normal, tails = 30 de auto-daño
- **Coexistencia:** Mutuamente excluyente con Asleep y Paralyzed

### StatusEffectManager

```java
public class StatusEffectManager {
    Map<SpecialCondition, StatusEffectStrategy> effects;

    void addEffect(PokemonInPlay pokemon, SpecialCondition condition);
    void removeEffect(PokemonInPlay pokemon, SpecialCondition condition);
    void processBetweenTurns(PokemonInPlay pokemon, GameBoard board);
    void onAttack(PokemonInPlay pokemon, AttackContext context);
    void clearAll(PokemonInPlay pokemon);  // al evolucionar o retirar
}
```

## Reglas del Juego

### TurnManager

```java
public class TurnManager {
    String getCurrentPlayerId();
    void nextTurn();
    boolean canDraw(String playerId);
    boolean canPerformAction(String playerId, ActionType action);
    void setPhase(TurnPhase phase);
    TurnPhase getPhase();
}
```

**Reglas de turno:**

| Regla | Descripción |
|---|---|
| Robo | 1 carta por turno (salta en primer turno del jugador inicial) |
| Energía | Máximo 1 energía attachada por turno |
| Supporter | Máximo 1 carta Supporter por turno |
| Stadium | Máximo 1 carta Stadium jugada por turno |
| Evolución | Solo se puede evolucionar en el turno siguiente a colocar el Pokémon |
| Retiro | 1 retiro por turno (cuesta energía según retreat cost) |
| Ataque | 1 ataque por turno, termina el turno |

### RuleValidator

```java
public class RuleValidator {
    void validateAction(GameActionMessage action, GameBoard board, TurnContext turn);
    void validateDeck(List<GameCard> deck);
    void validateEvolution(PokemonCard base, PokemonCard evolution);
    void validateEnergyAttachment(EnergyCard energy, PokemonInPlay pokemon, TurnContext turn);
}
```

### VictoryConditionChecker

```java
public class VictoryConditionChecker {
    boolean checkVictory(GameBoard board, String playerId);
    String getWinner(GameBoard board);
}
```

**Condiciones de victoria:**

1. **Última carta de premio tomada** - El jugador que toma su última carta de premio gana
2. **Knockout total** - El oponente no tiene Pokémon en juego ni en banca
3. **Mazo vacío** - El oponente no puede robar carta al comenzar su turno (fase DRAW)
4. **Rendición** - Un jugador envía `SURRENDER` y pierde inmediatamente

**Sudden Death:** Si ambos jugadores ganan simultáneamente, se reinicia la partida con 1 carta de premio cada uno. Implementación planificada para semana 4-5.

## Procesamiento de Knockouts (KOs)

Un Pokémon sufre KO cuando sus contadores x 10 >= HP máximo.
1. El Pokémon y todas sus cartas acopladas van a la zona `DISCARD`.
2. El oponente toma 1 carta de `PRIZE` (2 si era un Pokémon-EX).

### KOs por Veneno/Quemadura (Edge Case de Pausa)
Si un KO ocurre en la fase `BETWEEN_TURNS`, el flujo del juego **se pausa obligatoriamente**. El motor bloquea el inicio del siguiente turno hasta recibir la acción `SET_ACTIVE` del jugador afectado promoviendo a alguien de su banca. Si no tiene banca, pierde inmediatamente.

### KOs Simultáneos
Si ambos Pokémon Activos caen al mismo tiempo (ej. por retroceso o veneno), el jugador cuyo turno está terminando promueve primero.

## Condiciones de Victoria (`VictoryConditionChecker`)

El juego termina inmediatamente si se cumple alguna condición:
1. **Premio Final:** Un jugador toma sus 6 cartas de premio.
2. **Knockout Total:** El oponente se queda con 0 Pokémon en juego (Activo y Banca vacíos).
3. **Mazo Vacío (Deck Out):** El oponente debe robar en la fase `DRAW` pero tiene 0 cartas.

*Si ambos jugadores cumplen una condición simultáneamente, la partida se descarta y se inicia una nueva en modo **Sudden Death** (Muerte Súbita) con 1 sola carta de premio por lado.*

## Persistencia Atómica
Para garantizar respuestas WebSocket de menos de 50ms (RNF-01), el `GameStatePersistenceService` toma toda la memoria (tablero, turnos, logs), la empaqueta en el objeto `MatchSnapshot` y hace una única escritura en la columna `board_state_json` de la tabla `game_states`. El log de eventos se persiste por separado en `game_events` para auditoría y reconexión.

## Persistencia del Estado

Después de cada acción, el estado se persiste en `GameStateEntity`:

- `boardStateJson`: Estado completo del tablero serializado como JSON (manos, mazos, banca, activo, descarte, premios)
- `currentTurnId`: ID del jugador actual
- `gamePhase`: Fase macro (WAITING, SETUP, ACTIVE, FINISHED)
- `turnPhase`: Sub-fase del turno (DRAW, ACTIONS, ATTACK, BETWEEN_TURNS)

## Mapeo de Datos de la API al Engine

Los datos del set XY1 se sincronizan desde `pokemontcg.io` via `POST /api/cards/sync`. El engine recibe los datos ya mapeados desde el servicio de sincronización. Los mapeos críticos son:

| Campo API | Mapeo al Engine | Notas |
|---|---|---|
| `attacks[].damage` | `Attack.damage` (int) + `Attack.effectText` (String) | Ver tabla de mapeo de daño en 01-requisitos.md |
| `abilities[]` | `List<Ability>` (puede ser vacía) | `type` siempre "Ability" en XY1; distinguir POKE_BODY/POKE_POWER por texto |
| `weaknesses[0].type` | `weaknessType` (String) | Valor de debilidad siempre ×2 en XY1 |
| `resistances[0].type` | `resistanceType` (String) | Valor de resistencia siempre -20 en XY1 |
| `evolvesFrom` | `evolvesFrom` (String, nullable) | Busqueda de pre-evolución por nombre, no por ID |
| `subtypes` | `PokemonStage` (enum derivado) | Ver tabla en 01-requisitos.md |
| `supertype` | `supertype` (String) | "Pokémon", "Trainer", "Energy" |
