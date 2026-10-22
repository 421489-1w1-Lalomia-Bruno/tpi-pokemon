# 01 - Requerimientos Funcionales y No Funcionales

> Este documento contiene **todos** los requerimientos del proyecto extraídos de la especificación oficial. Es la referencia principal para implementar cualquier funcionalidad sin necesidad de consultar los PDF originales.

---

## Requerimientos Funcionales

### RF-01 — Reglas del Juego

El sistema debe implementar las reglas oficiales del Pokémon TCG (set XY1). El backend es la **única fuente de verdad** para todas las reglas.

#### RF-01.1 — Configuración Inicial (Setup)

1. **Barajar:** Ambos jugadores barajan su mazo de 60 cartas.
2. **Robar:** Cada jugador roba las 7 cartas superiores de su mazo.
3. **Mostrar mano:** Cada jugador ve sus propias 7 cartas.
4. **Colocar Pokémon Básico:**
   - Cada jugador debe colocar al menos 1 Pokémon Básico en el puesto activo.
   - Puede colocar hasta 5 Pokémon Básicos adicionales en la banca.
   - Si un jugador no tiene Pokémon Básicos en su mano inicial:
     - Muestra su mano al oponente (verificación).
     - Rebaraja su mazo completo.
     - Roba 7 cartas nuevas.
      - **Mulligans:** El oponente roba 1 carta adicional por cada mulligan que realice el otro jugador. 
     - Este proceso se repite hasta que ambos tengan al menos 1 Básico.
5. **Cartas de Premio:** Cada jugador separa las 6 cartas superiores de su mazo como cartas de premio (boca abajo, sin verlas).
6. **Moneda inicial:** Se lanza una moneda para determinar quién comienza primero.

#### RF-01.2 — Estructura del Turno

Cada turno se divide en **3 fases**:

**Fase 1 — Robo (Draw):**
- El jugador roba 1 carta de su mazo.
- **Excepción:** Si es el primer turno del jugador que comenzó primero, se saltea esta fase.
- Si el jugador no puede robar (mazo vacío), pierde la partida inmediatamente.

**Fase 2 — Acciones (Main):**
El jugador puede realizar las siguientes acciones en cualquier orden:

| Acción | Regla |
|---|---|
| Colocar Pokémon Básico | Ilimitados por turno (desde la mano a la banca) |
| Evolucionar Pokémon | Solo en el turno siguiente a colocar el Pokémon base. No se puede evolucionar el mismo turno que se coloca. **Ningún jugador puede evolucionar un Pokémon durante su primer turno de la partida.** |
| Attachar Energía | Máximo 1 carta de Energía por turno (desde la mano a un Pokémon en juego) |
| Jugar Trainer–Item | Ilimitados por turno |
| Jugar Trainer–Supporter | Máximo 1 por turno |
| Jugar Trainer–Stadium | Máximo 1 por turno (reemplaza el estadio anterior si hay uno) |
| Equipar Trainer–Tool | 1 por Pokémon, ilimitados por turno (a diferentes Pokémon) |
| Usar Habilidad (Ability) | Según la carta, algunas tienen restricciones |
| Retirar Pokémon Activo | 1 retiro por turno. Cuesta energía según el retreat cost. **No se puede retirar si el Pokémon está Dormido (Asleep) o Paralizado (Paralyzed).** |

**Fase 3 — Ataque:**
- El jugador declara un ataque con su Pokémon activo.
- El ataque **resuelve** el turno (no se pueden hacer más acciones después de atacar).
- **No se puede atacar en el primer turno** del jugador que comenzó primero.
- El Pokémon activo debe tener la energía necesaria para el ataque declarado.

#### RF-01.3 — Resolución de Ataque (Pipeline de 7 Pasos)

Cada ataque se resuelve en el siguiente orden:

1. **Verificación de Energía:** El Pokémon activo tiene la energía necesaria para el ataque?
   - Si NO → la acción es inválida, no se ejecuta.
   - Si SÍ → continuar.

2. **Verificación de Confusión:** Si el Pokémon atacante está Confundido:
   - Lanzar moneda.
   - **Heads:** El ataque se ejecuta normalmente.
   - **Tails:** El ataque falla y el Pokémon se coloca 3 contadores de daño a sí mismo.

3. **Selección de Objetivo:** El objetivo es siempre el Pokémon activo del oponente.

4. **Efectos Pre-Ataque:** Ejecutar efectos que ocurren antes del daño (ej: "descartar 1 energía del Pokémon defensor").

5. **Modificadores de Ataque:** Aplicar modificadores de daño:
   - Stadium activo
   - Herramientas equipadas
   - Habilidades que modifican daño
   - Otros efectos activos

6. **Cálculo de Daño:**
   - Daño base del ataque
   - **Debilidad:** Si el tipo del atacante es débil del tipo del defensor → ×2 daño
   - **Resistencia:** Si el tipo del atacante es resistente del tipo del defensor → −20 daño
   - **Mínimo:** El daño nunca puede ser menor a 0
   - **Contadores:** Se coloca 1 contador de daño por cada 10 puntos de daño (redondear hacia arriba)

7. **Efectos Post-Daño:** Aplicar efectos que ocurren después del daño:
   - Condiciones especiales (Burned, Poisoned, etc.)
   - Efectos específicos del ataque (ej: "el defensor queda Quemado")

#### RF-01.4 — Knockout (Fuera de Combate)

Un Pokémon queda Fuera de Combate cuando:
- Los contadores de daño × 10 ≥ HP del Pokémon

Proceso de KO:
1. El Pokémon KO'd se descarta (va a la pila de descarte).
2. Todas las cartas attachadas (energía, herramientas) se descartan.
3. El oponente toma cartas de premio:
   - **1 carta** para Pokémon normal.
   - **2 cartas** para Pokémon-EX.
4. El dueño del Pokémon KO'd debe:
   - Colocar un Pokémon de su banca como nuevo activo, O
   - Si no tiene Pokémon en banca → **pierde la partida**.
5. **KOs Simultáneos:** Si ambos Pokémon activos caen al mismo tiempo, el jugador cuyo turno está terminando debe elegir y promover a su nuevo Pokémon activo primero.

#### RF-01.5 — Condiciones Especiales

Existen 5 condiciones especiales que pueden afectar a un Pokémon:

| Condición | Efecto | Remoción |
|---|---|---|
| **Asleep (Dormido)** | Entre turnos: flip de moneda. Heads = despierta. Tails = permanece dormido y saltea su turno. **Impide retirar.** | Evolución, o flip Heads |
| **Burned (Quemado)** | Entre turnos: colocar 2 contadores de daño. | Evolución, Retirada, o flip Heads al final del turno |
| **Confused (Confundido)** | Al atacar: flip de moneda. Heads = ataque normal. Tails = 30 de auto-daño. | Evolución, Retirada |
| **Paralyzed (Paralizado)** | El Pokémon no puede atacar ni retirarse. Se quita al final del próximo turno del dueño. | Evolución, o fin del próximo turno |
| **Poisoned (Envenenado)** | Entre turnos: colocar 1 contador de daño. | Evolución, Retirada |

**Reglas de incompatibilidad:**
- **Asleep, Confused y Paralyzed son mutuamente excluyentes.** Un Pokémon solo puede tener una de estas tres a la vez.
- **Burned y Poisoned pueden coexistir** con cualquier otra condición (incluso entre sí).
- Si se aplica una condición incompatible, la nueva reemplaza a la existente.

**Orden de procesamiento entre turnos:**
1. Poisoned (1 daño)
2. Burned (flip para quitar, sino 2 daños)
3. Asleep (flip para despertar, sino saltea turno)
4. Paralyzed (se quita al final del turno)

**Todas las condiciones se limpian al:**
- Evolucionar el Pokémon
- Retirar el Pokémon a la banca

#### RF-01.6 — Condiciones de Victoria/Derrota

Un jugador **gana** si se cumple CUALQUIERA de estas condiciones:

1. **Última carta de premio:** El jugador toma su última carta de premio.
2. **Knockout total:** El oponente no tiene ningún Pokémon en juego (ni activo ni en banca).
3. **Mazo vacío:** El oponente no puede robar carta **al comenzar su turno (en la Fase de Robo)**. Quedarse sin cartas en medio del turno por efectos u objetos no causa derrota inmediata.

**Sudden Death:**
- Si ambos jugadores cumplen una condición de victoria simultáneamente, se declara Sudden Death.
- La partida se reinicia con **1 carta de premio** cada jugador.

---

### RF-02 — Tipos de Cartas

El sistema debe soportar todos los tipos de cartas del set XY1.

#### RF-02.1 — Pokémon

| Tipo | Descripción |
|---|---|
| **Basic** | Se puede colocar directamente en juego. |
| **Stage 1** | Evoluciona de un Pokémon Basic. |
| **Stage 2** | Evoluciona de un Pokémon Stage 1. |
| **Pokémon-EX** | Siempre son Basic. Cuando son KO'd, el oponente toma **2 cartas de premio**. |
| **Mega Evolution** | Evoluciona de un Pokémon-EX. Es opcional implementar. |

Cada Pokémon tiene:
- Nombre
- HP (puntos de vida)
- Tipo(s) (Fire, Water, Grass, etc.)
- Ataques (nombre, costo de energía, daño, efecto)
- Habilidades/Abilities (nombre, efecto)
- Debilidad (tipo ×2)
- Resistencia (tipo −20)
- Costo de retirada

#### RF-02.2 — Energía

| Tipo | Descripción | Límite en mazo |
|---|---|---|
| **Basic Energy** | Fire, Water, Grass, Lightning, Psychic, Fighting, Darkness, Metal, Fairy | Ilimitadas |
| **Special Energy** | Energías con efectos especiales | Máximo 4 por nombre |

#### RF-02.3 — Trainer

| Subtipo | Descripción | Límite por turno |
|---|---|---|
| **Item** | Objetos con efectos variados | Ilimitados |
| **Supporter** | Adiestradores con efectos poderosos | 1 por turno |
| **Stadium** | Efectos globales que afectan a ambos jugadores. Zona compartida (reemplaza el anterior) | 1 por turno |
| **Pokémon Tool** | Se equipa a un Pokémon específico. 1 por Pokémon | Ilimitados (a diferentes Pokémon) |
| **ACE SPEC** | Cartas únicas y poderosas. (Nota: Límite por mazo ignorado) | Ilimitados por turno |
| **Plasma** | Cartas del Equipo Plasma | Según reglas específicas |

---

### RF-03 — Gestión de Partidas

#### RF-03.1 — Estados de la Partida

| Estado | Descripción |
|---|---|
| **WAITING** | Partida creada, esperando que un segundo jugador se una. |
| **SETUP** | Ambos jugadores conectados. Configuración inicial (mulligan, moneda, colocar Pokémon). |
| **ACTIVE** | Partida en curso. Sub-fases: DRAW → ACTIONS → ATTACK → BETWEEN_TURNS. |
| **FINISHED** | Partida terminada. Se registró un ganador. |

#### RF-03.2 — Fuente de Verdad

- El **backend es la única fuente de verdad** para el estado del juego.
- El cliente (frontend) solo recibe información que le corresponde ver.
- **No se permiten llamadas API REST directas durante el juego.** Todas las acciones van por WebSocket.

#### RF-03.3 — Log de Acciones

- Se mantiene un **log inmutable** de todas las acciones de la partida.
- Cada entrada del log incluye:
  - Número de turno
  - Jugador que realizó la acción
  - Tipo de acción
  - Resultado de la acción

#### RF-03.4 — Persistencia Automática

- El estado del juego se persiste automáticamente **después de cada acción**.
- Permite recuperar partidas interrumpidas.

---

### RF-04 — Deck Builder

#### RF-04.1 — Set Base

- El set obligatorio es **xy1** (XY - Unlimited, 146 cartas).
- Las cartas se obtienen de la API pokemontcg.io y se almacenan localmente.

#### RF-04.2 — Reglas de Validación del Mazo

| Regla | Descripción |
|---|---|
| **Cantidad exacta** | El mazo debe tener exactamente **60 cartas**. |
| **Máximo 4 copias** | Máximo 4 cartas del mismo nombre (por nombre, no por ID). |
| **Excepción: Basic Energy** | Las cartas de Basic Energy **no tienen límite** de copias. |
| **Mínimo 1 Basic Pokémon** | El mazo debe contener al menos 1 Pokémon Basic para poder comenzar. |
| **Excepción: ACE SPEC** | ~~Máximo 1 ACE SPEC~~ (Regla explícitamente **ignorada**: el mazo puede tener múltiples AS Tácticos sin fallar la validación). |

#### RF-04.3 — CRUD de Mazos

- Los jugadores pueden **crear, leer, actualizar y eliminar** sus mazos.
- Cada mazo tiene un nombre y una lista de cartas con cantidades.
- La validación se ejecuta al crear y al actualizar un mazo.

---

### RF-05 — Persistencia de Estado

El sistema debe persistir el estado completo de la partida para permitir recuperación.

#### RF-05.1 — Datos a Persistir

| Dato | Descripción |
|---|---|
| **Estado del tablero** | Pokémon activo, banca, estadio |
| **Manos** | Cartas en mano de cada jugador (solo para recuperación, no se envían al oponente) |
| **Mazos** | Cartas restantes en cada mazo (incluyendo orden) |
| **Pila de descarte** | Cartas descartadas de cada jugador |
| **Cartas de premio** | Cartas de premio restantes (orden preservado) |
| **Contadores de daño** | Daño acumulado en cada Pokémon en juego |
| **Condiciones especiales** | Estado actual de cada condición en cada Pokémon |
| **Flags de turno** | Si ya robó, si ya attachó energía, si ya jugó Supporter, etc. |
| **Energías attachadas** | Energías en cada Pokémon en juego |
| **Herramientas equipadas** | Tools en cada Pokémon en juego |

#### RF-05.2 — Log de Acciones Inmutable

- Cada acción se registra con: turno, jugador, tipo, resultado.
- El log **nunca se modifica ni se elimina** durante la partida.
- Se utiliza para debugging, replay y recuperación de estado.

---

### RF-06 — Comunicación en Tiempo Real

#### RF-06.1 — WebSocket Obligatorio

- La comunicación durante el juego **debe ser por WebSocket**.
- Se requiere investigación e implementación propia del protocolo.

#### RF-06.2 — Sincronización de Estado

- Después de cada acción, el servidor envía el estado actualizado a ambos clientes.
- El estado se sincroniza completamente (no solo los cambios).
- **Optimización:** Para asegurar que el mensaje de WebSocket tarde menos de 50ms, el historial completo del `actionLog` no se envía en cada sincronización de estado, sino que debe manejarse por separado o de forma paginada.

#### RF-06.3 — Notificación de Eventos

- Los eventos del juego se notifican en tiempo real:
  - Cambio de turno
  - Cambio de fase
  - Ataque ejecutado
  - Pokémon KO'd
  - Carta de premio tomada
  - Condición especial aplicada/removida
  - Fin de partida

#### RF-06.4 — Reconexión

- El sistema debe manejar reconexiones robustas:
  - Si un cliente se desconecta, el estado se mantiene en el servidor.
  - Al reconectar, el cliente recibe el estado actual completo.
  - Las acciones realizadas durante la desconexión se pierden (el servidor es la fuente de verdad).

---

### RF-07 — Interfaz de Usuario

#### RF-07.1 — Lobby

- Lista de partidas disponibles para unirse.
- Crear nueva partida (seleccionando mazo).
- Unirse a partida existente.

#### RF-07.2 — Tablero Interactivo

- Visualización del tablero de juego completo.
- **Drag & Drop** para:
  - Colocar Pokémon de la mano a la banca.
  - Attachar energía de la mano a un Pokémon.
  - Equipar herramientas a un Pokémon.
  - Jugar cartas de entrenador.
- Visualización en tiempo real de:
  - HP actual de cada Pokémon
  - Contadores de daño
  - Energías attachadas
  - Herramientas equipadas
  - Condiciones especiales
  - Cantidad de cartas de premio restantes
  - Cantidad de cartas en la mano del oponente (no las cartas)

#### RF-07.3 — Panel de Acciones

- Botones contextuales habilitados/deshabilitados según:
  - Fase actual del turno
  - Acciones ya realizadas en el turno
  - Cartas disponibles en la mano
  - Estado del tablero

#### RF-07.4 — Log de Acciones

- Historial visual de todas las acciones de la partida.
- Se actualiza en tiempo real.

#### RF-07.5 — Notificaciones Visuales

- Feedback visual al usuario para:
  - Acciones exitosas
  - Errores (acción inválida)
  - Eventos importantes (KO, premio tomado, cambio de turno)

#### RF-07.6 — Habilidades de Pokémon

- Visualización de las Abilities de cada Pokémon.
- Indicación de si la Ability puede usarse en el turno actual.

---

## Requerimientos No Funcionales

### RNF-01 — Rendimiento

| Métrica | Objetivo |
|---|---|
| Respuesta a acciones del juego | **< 200ms** |
| Búsqueda de cartas | **< 500ms** |
| Carga inicial de la aplicación | **< 2s** |
| Mensaje WebSocket | **< 50ms** |

### RNF-02 — Calidad del Código

- **Principios SOLID** aplicados en todo el código.
- **Game Engine aislado** del resto de la aplicación con los siguientes componentes independientes:
  - `RuleValidator` — Validación de reglas
  - `DamageCalculator` — Cálculo de daño
  - `StatusEffectManager` — Gestión de condiciones especiales
  - `VictoryConditionChecker` — Condiciones de victoria
  - `TurnManager` — Gestión de turnos
- **Angular Style Guide** seguida en el frontend (standalone components, signals, OnPush, etc.).

### RNF-03 — Testing

| Requisito | Objetivo |
|---|---|
| Cobertura global (JaCoCo) | **≥ 80%** de instrucciones |
| Cobertura componentes críticos (JaCoCo) | **≥ 90%** de instrucciones |
| Tests de integración | Flujo completo de partida |
| Tests E2E | **Al menos 1** test end-to-end |

**Componentes críticos** (cobertura ≥ 90%):
- `GameEngine.java`
- `DamageCalculator.java`
- `RuleValidator.java`
- `TurnManager.java`
- `VictoryConditionChecker.java`
- `StatusEffectManager.java`
- `DeckValidationService.java`
- `MatchService.java`

### RNF-04 — Patrones de Diseño

El proyecto debe implementar los siguientes patrones de diseño:

| Patrón | Ubicación | Propósito |
|---|---|---|
| **State** | `engine/state/` | Ciclo de vida del match (WAITING → SETUP → ACTIVE → FINISHED) |
| **Strategy** | `engine/effects/` | Comportamiento de condiciones especiales intercambiable |
| **Chain of Responsibility** | `engine/combat/steps/` | Pipeline de resolución de ataques (7 pasos) |
| **Observer** | `engine/events/` | Broadcasting de eventos del juego a clientes WebSocket |
| **Repository** | `repositories/` | Abstracción del acceso a datos sobre JPA |
| **Facade** | `engine/GameEngine.java` | Interfaz unificada a los subsistemas complejos del engine |

### RNF-05 — Seguridad

| Regla | Implementación |
|---|---|
| **Backend valida TODAS las acciones** | Cada acción WebSocket se valida con `RuleValidator` antes de aplicar |
| **Mano del oponente nunca se envía** | `MatchSnapshot` filtra las cartas de mano del oponente |
| **Orden del mazo oculto** | El servidor mantiene el orden real, el cliente solo ve cantidades |
| **Cartas de premio ocultas** | No se revelan hasta que se toman |
| **Sin llamadas REST durante juego** | Todas las acciones van por WebSocket → GameEngine |
| **Validación de inputs** | `@Valid` con Bean Validation en todos los DTOs |

### RNF-06 — Usabilidad

- **Mensajes de error claros:** El usuario entiende por qué una acción fue rechazada.
- **Feedback visual:** Cada acción tiene una respuesta visual inmediata.
- **Transiciones suaves:** Las animaciones y transiciones mejoran la experiencia sin distraer.

---

## Decisiones de Diseño del Engine

### Draw Automático

La Fase de Robo (DRAW) es **automática**: al entrar en la fase DRAW, el engine roba una carta sin intervención del cliente. No existe la acción `DRAW_CARD` en el protocolo WebSocket. El cliente recibe un `STATE_UPDATE` después de que el engine ejecute el robo. La única excepción es el primer turno del jugador inicial, donde la fase DRAW se saltea automáticamente.

### Identidad del Jugador en REST

Durante las semanas 1-3, el `playerId` se pasa mediante el header HTTP `X-Player-Id` (no en el body del DTO). Un `PlayerIdInterceptor` extrae este header y lo inyecta como atributo del request. Cuando se migre a JWT (semana 4-5), se reemplaza el interceptor por un `OncePerRequestFilter` que extrae el ID del token, sin cambiar los controllers.

### Datos de la API pokemontcg.io

Los datos se sincronizan una única vez via `POST /api/cards/sync` y se almacenan localmente. La API devuelve campos heterogéneos que requieren mapeo:

| Campo API | Tipo API | Mapeo a CardEntity | Notas |
|---|---|---|---|
| `attacks` | Array de objetos | `attacks_json` (TEXT) | Cada attack tiene `name`, `cost` (array de strings), `convertedEnergyCost` (int), `damage` (string, puede ser vacío o fórmula), `text` |
| `abilities` | Array de objetos | `abilities_json` (TEXT, nullable) | Cada ability tiene `name`, `text`, `type` ("Ability" en XY1) |
| `weaknesses` | Array `{type, value}` | `weakness_type` (String, primer tipo) | Value siempre `"×2"` en XY1 |
| `resistances` | Array `{type, value}` | `resistance_type` (String, nullable) | Value siempre `"-20"` en XY1 |
| `hp` | String | `hp` (Integer) | Parsear de String a Integer |
| `retreatCost` | Array de strings | `retreat_cost` (Integer) | Usar `convertedRetreatCost` de la API |
| `supertype` | String | `supertype` (String) | "Pokémon", "Trainer", "Energy" |
| `subtypes` | Array de strings | `subtypes` (JSON) | Mapear a PokemonStage/TrainerSubtype |
| `evolvesFrom` | String (nullable) | `evolves_from` (String, nullable) | Nombre del pre-evolución, no ID |
| `evolvesTo` | Array de strings | No se almacena | Se deriva dinámicamente buscando cartas cuyo `evolvesFrom` coincida |
| `rules` | Array de strings | `rules` (TEXT) | Reglas de la carta (Pokémon-EX, Mega, Supporter, etc.) |
| `nationalPokedexNumbers` | Array de ints | No se almacena | No necesario para el juego |
| `flavorText` | String | No se almacena | Texto descriptivo, no funcional |

**Subtypes → PokemonStage/TrainerSubtype**: El mapeo se realiza así:

| API `subtypes` | Modelo |
|---|---|
| `["Basic"]` (Pokémon) | `PokemonStage.BASIC` |
| `["Basic", "EX"]` | `PokemonStage.EX` |
| `["Stage 1"]` | `PokemonStage.STAGE1` |
| `["Stage 2"]` | `PokemonStage.STAGE2` |
| `["MEGA", "EX"]` | `PokemonStage.MEGA` |
| `["Item"]` | `TrainerSubtype.ITEM` |
| `["Supporter"]` | `TrainerSubtype.SUPPORTER` |
| `["Stadium"]` | `TrainerSubtype.STADIUM` |
| `["Tool"]` o `["Pokémon Tool"]` | `TrainerSubtype.TOOL` |
| `["Basic"]` (Energy) | `EnergyCard.isSpecial = false` |
| `["Special"]` (Energy) | `EnergyCard.isSpecial = true` |

**Campo `damage` en ataques**: La API devuelve el daño como String (`"60"`, `""`, `"10×"`, `"120+"`). El mapeo es:
- Número puro (`"60"`) → `damage = 60`, `effectText = null`
- Vacío (`""`) → `damage = 0`, `effectText = null` (ataque sin daño numérico ni efecto textual)
- Vacío (`""`) con texto de efecto → `damage = 0`, `effectText = texto del efecto de la API` (preserva el efecto para que el cliente lo muestre)
- Con modificador (`"120+"`) → `damage = 120`, `effectText = "120+"` (el + indica daño adicional)
- Multiplicador (`"10×"`) → `damage = 0`, `effectText = "10×" + text del efecto` (daño variable, el engine lo calcula dinámicamente)

**Energy Types**: La API usa nombres en inglés capitalizado (`"Grass"`, `"Fire"`, `"Colorless"`). Se mapean al enum `EnergyType` en uppercase (`GRASS`, `FIRE`, `COLORLESS`).

**Energy Especiales**: Para `Double Colorless Energy` (si existe en XY1), el campo `providedEnergies` se codifica manualmente como `["COLORLESS", "COLORLESS"]` ya que la API no provee esta información estructurada.

**Efectos de Ataques y Trainers**: Los textos de efectos (`attack.text`, `ability.text`, `rules`) se almacenan como strings. Su interpretación y ejecución se implementa carta por carta en la semana 5 del plan de implementación.

---

## Criterio de Aprobación

| Categoría | Puntos |
|---|---|
| Reglas del Juego (RF-01 a RF-07) | 40 |
| Arquitectura & Código | 25 |
| Base de Datos | 10 |
| Frontend | 15 |
| Testing | 10 |
| **Total Obligatorio** | **100** |

- **Mínimo para aprobar:** 60% (60 puntos)
- Los puntos opcionales solo cuentan si se cumplen todos los obligatorios.
