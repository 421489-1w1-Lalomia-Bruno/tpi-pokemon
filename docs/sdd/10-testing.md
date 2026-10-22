# 10 - Testing

# Estrategia de Testing

> **Nota de Calidad:** Esta estrategia rige las políticas de aceptación inquebrantables para el Quality Gate del CI/CD (RNF-03). Ningún fragmento de código del motor o de la UI se considerará terminado sin su correspondiente suite de pruebas automatizadas.

## Umbrales de Cobertura Estrictos (JaCoCo Metric Gates)

El plugin de JaCoCo en el Backend ejecutará la verificación automatizada en cada Pull Request mediante el comando `mvn jacoco:check`. Se definen dos niveles de exigencia ineludibles:

### 1. Umbral del Core Crítico (Mínimo 90% de Cobertura de Líneas y Ramas)
Aplica a los paquetes que gobiernan las reglas de juego, la integridad matemática del daño y la consistencia del estado del tablero en memoria. Cualquier PR que descienda de este límite será rebotado de forma automática por el CI/CD:

 * `ar.edu.utn.frc.tup.piii.engine.combat.*` (Pipeline de ataque y cálculo de daños en cadena).
* `ar.edu.utn.frc.tup.piii.engine.rules.*` (Validaciones del ciclo del turno y condiciones de victoria).
* `ar.edu.utn.frc.tup.piii.engine.state.*` (Transiciones macro de la máquina de estados global).
* `ar.edu.utn.frc.tup.piii.engine.effects.*` (Estrategias concretas para las condiciones especiales).

### 2. Umbral General de Soporte (Mínimo 80% de Cobertura de Líneas)
Aplica al resto de las capas encargadas de la persistencia, mappers y transporte de red:

* `controllers/`, `services/` (Servicios de aplicación), `mappers/`, `repositories/` y `websocket/listeners/`.

---

## Pruebas Unitarias Aisladas (Backend - JUnit 5 + Mockito)

Queda prohibido instanciar el juego completo o levantar el contexto de Spring para probar lógicas atómicas del motor. Se exige el aislamiento absoluto mediante dobles de prueba (Mocks).

### Reducción de Código Repetitivo (Pattern: Object Mother / Test Data Builder)
Para evitar que las clases de prueba se colapsen con setups repetitivos de tableros, se implementará de forma obligatoria una fábrica centralizada de datos de prueba (`GameTestFactory`):

```java
public class GameTestFactory {
    public static PokemonInPlay createActivePokemon(String cardId, int hp) {
        PokemonCard card = new PokemonCard();
        card.setId(cardId);
        card.setHp(hp);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setCard(card);
        pip.setCurrentHp(hp);
        pip.setDamageCounters(0);
        pip.setSpecialConditions(new HashSet<>());
        pip.setAttachedEnergy(new ArrayList<>());
        return pip;
    }
}
```

###  Ejemplo de Test Unitario del Pipeline (Chain of Responsibility)
Aislamiento puro de un eslabón del combate mediante la inyección del contexto mockeado:

``` java
@ExtendWith(MockitoExtension.class)
class ConfusionCheckStepTest {
    @InjectMocks
    private ConfusionCheckStep confusionCheckStep;
    @Mock
    private AttackContext context;
    @Test
    void should_InterruptPipelineAndApplySelfDamage_When_ConfusionFlipIsTails() {
        // GIVEN: Un Pokémon bajo la condición de CONFUSED
        PokemonInPlay attacker = GameTestFactory.createActivePokemon("pika-01", 60);
        attacker.getSpecialConditions().add(SpecialCondition.CONFUSED);
        
        when(context.getAttacker()).thenReturn(attacker);
        when(context.executeCoinFlip()).thenReturn(CoinResult.TAILS); // Forzamos cruz
        // WHEN: El pipeline ejecuta el paso de confusión
        StepResult result = confusionCheckStep.execute(context);
        // THEN: El ataque se corta de raíz y el atacante se hiere a sí mismo
        assertEquals(StepResult.INTERRUPTED, result);
        assertEquals(30, attacker.getDamageCounters() * 10); // 3 contadores = 30 dmg
        verifyNoInteractions(context.getDefender()); // El rival no sufre alteraciones
    }
}
```

## Pruebas de Integración Relacional (Data Layer - Embedded PostgreSQL)
Dado que las instantáneas dinámicas del tablero se persisten de forma centralizada en una columna de tipo `JSONB` (`game_states`), las bases de datos en memoria convencionales (como H2) pueden enmascarar errores de dialecto o fallos de serialización de Hibernate.

* **Política Obligatoria:** Las pruebas que levantan contexto de Spring (`@SpringBootTest`) deben ejecutarse contra una instancia real de PostgreSQL mediante `embedded-database-spring-test` (zonky). No requiere Docker: descarga y ejecuta binarios nativos de PostgreSQL en un subproceso JVM.
* **Implementación:** La clase base `EmbeddedPostgresTestBase` configura `@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)` para reemplazar el `DataSource` por una instancia PostgreSQL efímera.
* **Qué validar:** El test debe guardar un `MatchSnapshot` complejo (con listas de cartas anidadas en manos, energías especiales attachadas y logs), recuperarlo por ID mediante el `GameStateRepository`, y realizar un `assertThat` campo por campo garantizando que ninguna colección se haya truncado o transformado en nula durante la conversión a JSONB.

## Pruebas de Seguridad y Robustez de Red (STOMP Security Testing)
El canal de comunicación en tiempo real por WebSockets es un vector crítico de ataques. El pipeline de tests de integración debe forzar escenarios de uso malicioso:

* Simulación de Falsificación de Identidad (Cross-Identity Injection): Se debe programar un test de integración donde un cliente WebSocket autenticado con la Sesión A intente enviar un comando formateado con el ID de otro jugador en el payload.
* Comportamiento Esperado: El `GameWebSocketHandler` debe comparar el `Principal` de la sesión STOMP con el `matchId` y el turno actual. El test debe comprobar que el backend capture la disparidad, arroje una excepción de tipo `InvalidActionException`, envíe de vuelta un payload con tipo `ERROR` hacia la Sesión A y mantenga el tablero intacto sin mutaciones de turno.
* **Nota importante:** El `GameActionMessage` NO lleva `playerId` en su payload. La identidad se extrae exclusivamente del `Principal` autenticado en la sesión STOMP (o del header `X-Player-Id` en Fase 1).

### Tests del PlayerIdInterceptor (Fase 1: Header X-Player-Id)

Se deben incluir tests unitarios que validen el comportamiento del `PlayerIdInterceptor`:

* Petición con header `X-Player-Id` válido → el interceptor extrajo correctamente el ID y lo inyectó como atributo del request.
* Petición sin header `X-Player-Id` → respuesta `401 Unauthorized`.
* Petición con header `X-Player-Id` vacío → respuesta `401 Unauthorized`.

### Tests de Migración a JWT (Fase 2)

Cuando se migre a JWT, se deben agregar tests de integración para el `OncePerRequestFilter`:

* Petición con JWT válido → el filtro extrajo correctamente el `playerId` del token y lo inyectó como atributo del request.
* Petición con JWT expirado → respuesta `401 Unauthorized`.
* Petición con JWT malformado → respuesta `401 Unauthorized`.
* Petición sin Authorization header → respuesta `401 Unauthorized`.

## Escenarios Límites Obligatorios (Edge Cases Core)
La suite de integración del motor debe certificar de forma determinista la resolución oficial de los siguientes casos límite:

1. Validación de Pool Energético Especial: Comprobar que un ataque con costo de dos energías `COLORLESS` se declare viable si el Pokémon tiene equipada una única carta física de Double Colorless Energy, validando el método `getAvailableEnergyPool()`.
2. Pausa Obligatoria "Entre Turnos": Simular un escenario donde la resolución de la estrategia de `PoisonedEffect` durante la fase `BETWEEN_TURNS` deja con 0 HP al Pokémon activo del jugador actual. El test debe asertar que el `TurnManager` bloquee el inicio del siguiente turno y cambie el estado de la mesa a una fase de espera inamovible hasta recibir la acción de WebSocket `SET_ACTIVE`.
3. KOs Simultáneos: Forzar mediante veneno que ambos Pokémon activos mueran al mismo tiempo entre turnos. El test debe validar que el motor pause a ambos clientes y exija el mensaje de resolución de puesto activo en orden estricto: primero el dueño del turno saliente, luego el rival.

## Testing Reactivo del Frontend (Angular 21 + Jasmine + fakeAsync)
Las pruebas unitarias del cliente Angular descartan la manipulación directa de variables internas de componentes, enfocándose en la auditoría de Signals derivados (`computed`) y en el retardo asíncrono controlado del flujo de red.

### Uso Obligatorio de `fakeAsync` y `tick()`
Cuando el servidor emite una actualización de inicio de partida tras resolver mulligans, el componente de setup debe transicionar su DOM. Los tiempos de espera deben simularse de forma determinista sin usar setTimeout reales de JS:

```TypeScript
it('should transition layout and hide spinner when game phase changes to ACTIVE', fakeAsync(() => {
  // GIVEN: El WebSocket service inyecta un estado SETUP inicial
  const mockSnapshot = { gamePhase: 'SETUP', board: {} } as MatchSnapshot;
  gameStateService.matchState.set(mockSnapshot);
  fixture.detectChanges();
  
  expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeTruthy();
  // WHEN: El servidor envía el snapshot ACTIVE por el socket tras el mulligan
  gameStateService.matchState.set({ ...mockSnapshot, gamePhase: 'ACTIVE' });
  tick(); // Resuelve microtareas asíncronas y disparadores de Signals de inmediato
  fixture.detectChanges();
  // THEN: El layout muta usando el control flow nativo
  expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeFalsy();
  expect(fixture.nativeElement.querySelector('.player-board-grid')).toBeTruthy();
}));
```

## Pruebas de Carga y Rendimiento (Load Testing - Gatling/JMeter)
Para asegurar el cumplimiento incondicional del requerimiento RNF-01 (Latencia del WebSocket e infraestructura general por debajo de los 50ms bajo estrés), se incorpora una suite automatizada de pruebas de carga ejecutada mediante Gatling:

* Inyección de Carga: El script simulará 50 parejas concurrentes (100 usuarios activos fijos) conectados en simultáneo a través de sockets STOMP independientes.
* Escenario de Estrés: Cada usuario simulará ráfagas consecutivas de juego enviando la acción `ATTACH_ENERGY` y `PLACE_POKEMON` cada 500ms durante una ventana de 2 minutos.
* Criterio de Aceptación: El percentil 95 (p95) de los mensajes de respuesta del tipo `STATE_UPDATE` distribuidos por el servidor debe situarse por debajo de los 50ms. Cualquier cuello de botella en la serialización JSONB o en los bloqueos de hilos de la base de datos que eleve el p95 por encima del umbral provocará el fallo automático de la prueba de rendimiento.
