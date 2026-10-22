---
name: test-engine
description: Writes tests for the Pokemon TCG game engine and backend services achieving JaCoCo coverage targets. Use when writing JUnit 5 tests, Mockito tests, integration tests, or E2E tests for the Pokemon TCG project. Triggers on test, spec, coverage, JaCoCo, unit test, integration test, E2E, Mockito.
---

# Test Engine Skill - Pokemon TCG

You are writing tests for a Pokemon Trading Card Game backend. The project uses **JUnit 5**, **Mockito**, **Spring Boot Test**, and **Testcontainers**. Coverage targets are strict: **≥80% global, ≥90% for critical components**.

## Testing Stack

- **JUnit 5** (`org.junit.jupiter`) - Test framework
- **Mockito** (`org.mockito`) - Mocking framework
- **Spring Boot Test** (`@SpringBootTest`) - Integration tests
- **MockMvc** - Controller tests
- **Testcontainers** - Integration tests with real PostgreSQL (not H2) for JSONB validation
- **JaCoCo** - Code coverage (configured via Maven)

## Coverage Requirements

| Component | Target Coverage |
|---|---|
| Global | **≥ 80%** instructions |
| `GameEngine` | **≥ 90%** |
| `DamageCalculator` | **≥ 90%** |
| `RuleValidator` | **≥ 90%** |
| `TurnManager` | **≥ 90%** |
| `VictoryConditionChecker` | **≥ 90%** |
| `StatusEffectManager` | **≥ 90%** |
| `DeckValidationService` | **≥ 90%** |
| `MatchService` | **≥ 90%** |

## Test Directory Structure

```
BE/src/test/java/ar/edu/utn/frc/tup/piii/
├── controllers/
│   ├── PingControllerTest.java          (exists)
│   ├── CardControllerTest.java
│   ├── DeckControllerTest.java
│   └── MatchControllerTest.java
├── services/
│   ├── CardServiceTest.java
│   ├── DeckServiceTest.java
│   ├── MatchServiceTest.java
│   └── GameStatePersistenceServiceTest.java
├── engine/
│   ├── state/
│   │   ├── WaitingStateTest.java
│   │   ├── SetupStateTest.java
│   │   ├── ActiveStateTest.java
│   │   └── FinishedStateTest.java
│   ├── combat/
│   │   ├── AttackResolverTest.java
│   │   ├── DamageCalculatorTest.java
│   │   └── steps/
│   │       ├── EnergyValidationStepTest.java
│   │       ├── ConfusionCheckStepTest.java
│   │       ├── TargetSelectionStepTest.java
│   │       ├── PreAttackEffectStepTest.java
│   │       ├── AttackModifierStepTest.java
│   │       ├── DamageCalculationStepTest.java
│   │       └── PostDamageEffectStepTest.java
│   ├── effects/
│   │   ├── AsleepEffectTest.java
│   │   ├── BurnedEffectTest.java
│   │   ├── ConfusedEffectTest.java
│   │   ├── ParalyzedEffectTest.java
│   │   ├── PoisonedEffectTest.java
│   │   └── StatusEffectManagerTest.java
│   ├── rules/
│   │   ├── RuleValidatorTest.java
│   │   ├── TurnManagerTest.java
│   │   └── VictoryConditionCheckerTest.java
│   └── GameEngineTest.java
├── mappers/
│   ├── CardMapperTest.java
│   ├── DeckMapperTest.java
│   └── MatchMapperTest.java
├── repositories/
│   ├── CardRepositoryTest.java
│   └── DeckRepositoryTest.java
└── security/
    └── CrossIdentityInjectionTest.java
```

## Test Conventions

### Object Mother Pattern

Use `GameTestFactory` to reduce boilerplate test data setup:

```java
public class GameTestFactory {
    public static PlayerBoard createPlayerBoardWithBasicPokemon() { ... }
    public static PokemonCard createBasicPokemonCard() { ... }
    public static GameBoard createStandardGameBoard() { ... }
    public static MatchSnapshot createMatchSnapshotForTesting() { ... }
    public static EnergyCard createBasicEnergyCard(EnergyType type) { ... }
    public static EnergyCard createDoubleColorlessEnergy() { ... }
}
```

### Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class SomeServiceTest {

    @Mock
    private SomeRepository someRepository;

    @InjectMocks
    private SomeService someService;

    // Test methods...
}
```

No Spring context instantiation for unit tests — strict mock isolation.

### Controller Test Template

```java
@SpringBootTest
class CardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void getAllCards_returnsOk() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
```

### Integration Test Template (Testcontainers)

Use Testcontainers with real PostgreSQL for integration tests that validate JSONB serialization:

```java
@Testcontainers
@SpringBootTest
class GameStatePersistenceServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private GameStatePersistenceService gameStatePersistenceService;

    @Test
    void persistAndRecover_MatchSnapshot_NoDataLoss() {
        MatchSnapshot snapshot = GameTestFactory.createMatchSnapshotForTesting();
        gameStatePersistenceService.save(snapshot);
        MatchSnapshot recovered = gameStatePersistenceService.findByMatchId(snapshot.getMatchId());
        // Assert field-by-field that no data was lost
    }
}
```

### Security Test (STOMP Cross-Identity)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrossIdentityInjectionTest {

    // Test that a WebSocket client authenticated as Session A
    // sending a payload with Player B's ID is rejected
    @Test
    void actionWithDifferentPlayerId_rejectedWithInvalidActionException() {
        // WebSocket client authenticated as player1
        // Sends action with player2's ID in payload
        // System must reject with InvalidActionException
        // Board state must remain unchanged
    }
}
```

## Critical Test Scenarios by Component

### GameEngine (≥ 90%)

The facade that delegates to all subsystems. Test that:
- Actions are properly delegated to state machine
- State transitions occur correctly
- Actions in wrong phase are rejected
- Events are published after actions (via ApplicationEventPublisher)
- State is persisted after actions (via GameStatePersistenceService)

### DamageCalculator (≥ 90%)

Test cases for damage calculation:

| Scenario | Base | Weakness | Resistance | Expected |
|---|---|---|---|---|
| Normal hit | 30 | - | - | 30 |
| Super effective (weakness) | 30 | x2 | - | 60 |
| Resisted | 30 | - | -20 | 10 |
| Weakness + resistance | 30 | x2 | -20 | 40 |
| Minimum damage | 5 | - | -20 | 0 (min 0) |
| Damage counters | 33 | - | - | 4 counters (ceil(33/10)) |

### RuleValidator (≥ 90%)

Test cases:
- Deck validation: exactly 60 cards, max 4 per name (except Basic Energy), at least 1 Basic Pokemon
- ACE SPEC limit is explicitly **ignored** (mazo puede tener multiples ACE SPEC sin fallar validacion)
- Evolution validation: must evolve from correct base, cannot evolve same turn placed, **no player can evolve on their first turn of the game**
- Energy attachment: max 1 per turn
- Retreat validation: must have enough energy for retreat cost, **cannot retreat if Asleep or Paralyzed**
- No attacks on first turn of starting player

### TurnManager (≥ 90%)

Test cases:
- First turn: starting player skips Draw phase, cannot attack
- First turn: no player can evolve on their first turn of the game
- Draw phase: player draws 1 card, deck empty during Draw phase = loss
- Actions phase: track `hasPlayedSupporter` and `hasPlayedStadium` separately (not combined `hasPlayedTrainer`)
- Attack phase: ends turn
- Between-turns: process status effects ONLY for the player whose turn is ENDING (Poisoned→Burned→Asleep→Paralyzed)
- PASS_TURN: voluntarily ends the ACTIONS phase, transitions to BETWEEN_TURNS
- SURRENDER: immediately ends the match, opponent wins

### StatusEffectManager (≥ 90%)

Test cases for each effect:

**BurnedEffect**:
- Between turns: add 2 damage counters
- Flip heads: remove condition
- Flip tails: keep condition
- Can coexist with Poisoned

**PoisonedEffect**:
- Between turns: add 1 damage counter
- Cannot coexist with another Poisoned
- Can coexist with Burned

**AsleepEffect**:
- Between turns: flip to wake
- Heads: wakes up, can act normally
- Tails: stays asleep, skips turn
- Mutually exclusive with Confused and Paralyzed
- **Cannot retreat** while Asleep

**ParalyzedEffect**:
- Cannot attack OR retreat while Paralyzed
- Auto-removes at end of opponent's next turn (between-turns processing)
- Mutually exclusive with Asleep and Confused
- **Cannot retreat** while Paralyzed
- Removal methods: evolution (NOT retreat — retreat is blocked)

**ConfusedEffect**:
- On attack: flip coin
- Heads: attack proceeds normally
- Tails: attack fails, 30 self-damage (3 damage counters)
- Mutually exclusive with Asleep and Paralyzed

**Incompatibility rules**:
- Applying Asleep removes Confused and Paralyzed
- Applying Confused removes Asleep and Paralyzed
- Applying Paralyzed removes Asleep and Confused
- Applying Burned/Poisoned does NOT remove Asleep/Confused/Paralyzed
- All conditions are cleared by evolution or retreat (except Asleep and Paralyzed block retreat)

### VictoryConditionChecker (≥ 90%)

Test cases:
- Win by taking last prize card
- Win by knockout (no bench Pokemon)
- Win by opponent deck empty **during Draw phase only** (effects mid-turn do NOT cause immediate loss)
- Both players meet condition simultaneously → Sudden Death (restart with 1 prize each)
- Game continues if no victory condition met

### Setup Phase (≥ 90%)

Test cases:
- Shuffle and draw 7 cards for both players
- Player with no Basic Pokemon: mulligan
- Mulligan: reveal hand, reshuffle, redraw 7 cards
- **Simultaneous mulligans**: If both players mulligan, mulligans cancel one-to-one (e.g., P1 does 2, P2 does 1 → P2 draws only 1 extra card)
- Place 1 Basic Pokemon as active (mandatory)
- Place up to 5 Basic Pokemon on bench (optional)
- Set 6 prize cards face down
- Coin flip to determine starting player

### Mandatory Edge Cases

1. **Special Energy Pool Validation**: Attack with 2 COLORLESS cost is viable with 1 Double Colorless Energy card (validates `getAvailableEnergyPool()`).
2. **Between-Turns KO Pause**: Poison kills a Pokemon during `BETWEEN_TURNS` → engine must pause and require `SET_ACTIVE` action before proceeding.
3. **Simultaneous KOs**: Both active Pokemon die → both clients paused, resolution in strict order (turn owner's opponent promotes first, per SDD rule).
4. **Deck-out timing**: Only causes loss during Draw phase. Mid-turn effects that exhaust the deck do NOT cause immediate loss.
5. **Paralyzed restrictions**: Cannot attack AND cannot retreat. Auto-removes at end of opponent's next turn.

### PlayerIdInterceptor Tests (Phase 1: X-Player-Id Header)

Add tests for the `PlayerIdInterceptor`:

```java
@SpringBootTest
class PlayerIdInterceptorTest {
    @Test
    void validHeader_playerIdExtractedAsRequestAttribute() { ... }
    @Test
    void missingHeader_returns401() { ... }
    @Test
    void emptyHeader_returns401() { ... }
}
```

When JWT is implemented (Phase 2), add corresponding `OncePerRequestFilter` tests for JWT validation.

## Integration Test Scenarios

### Full Match Flow Test

Test a complete game from start to finish:
1. Create match
2. Both players join
3. Setup phase: draw, place basics, prize cards
4. Turn 1 (starting player): skip draw → main phase → PASS_TURN
5. Turn 2 (second player): draw → attach energy → attack
6. Continue until victory condition

### Deck Validation Integration Test

1. Create deck with 60 valid cards → should pass
2. Create deck with 59 cards → should fail
3. Create deck with 5 copies of same card (non-energy) → should fail
4. Create deck with multiple ACE SPEC cards → should pass (ACE SPEC limit explicitly ignored)
5. Create deck with no Basic Pokemon → should fail
6. Create deck with unlimited Basic Energy copies → should pass

## Running Tests and Coverage

```bash
# Run all tests
cd BE && mvn test

# Run specific test class
mvn test -Dtest=DamageCalculatorTest

# Run with coverage report
mvn test jacoco:report

# Coverage report location
# BE/target/site/jacoco/index.html

# Verify coverage thresholds
mvn verify jacoco:check

# Run integration tests with Testcontainers (requires Docker)
mvn verify -Dspring.profiles.active=test
```

## JaCoCo Configuration

Add to `pom.xml` if not present:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
            <rule>
                <element>CLASS</element>
                <includes>
                    <include>ar.edu.utn.frc.tup.piii.engine.GameEngine</include>
                    <include>ar.edu.utn.frc.tup.piii.engine.combat.DamageCalculator</include>
                    <include>ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator</include>
                    <include>ar.edu.utn.frc.tup.piii.engine.rules.TurnManager</include>
                    <include>ar.edu.utn.frc.tup.piii.engine.rules.VictoryConditionChecker</include>
                    <include>ar.edu.utn.frc.tup.piii.engine.effects.StatusEffectManager</include>
                </includes>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.90</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

## E2E Test Requirements

At least **1 E2E test** is required. This should test the full flow:
1. Start the application
2. Create a match via REST
3. Connect via WebSocket
4. Perform game actions
5. Verify state changes

Use `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` with `TestRestTemplate` and a WebSocket test client.

## Frontend Testing

Angular 20 + Jasmine tests for critical services and Signal-derived state:

```bash
cd FE
npm run test:headless
```

Focus on:
- `GameStateService` signal updates on STATE_UPDATE
- `computed()` perspective-driven boards (myBoard/opponentBoard)
- SetupPhase transitions when game phase changes from SETUP to ACTIVE

## Checklist

When writing tests:
- [ ] Test class in correct package under `BE/src/test/java/ar/edu/utn/frc/tup/piii/`
- [ ] Uses JUnit 5 (`@Test`, `@BeforeEach`, `@ExtendWith`)
- [ ] Uses Mockito (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`) for unit tests
- [ ] Uses Testcontainers with PostgreSQL for integration tests involving JSONB
- [ ] Test method names are descriptive: `methodName_scenario_expectedResult`
- [ ] Each test has 3 phases: Arrange, Act, Assert
- [ ] Critical components have ≥90% coverage
- [ ] Edge cases tested (empty deck, KO, mulligan, simultaneous KOs, sudden death, deck-out timing)
- [ ] Status effect incompatibility rules tested
- [ ] Damage calculation edge cases tested (weakness, resistance, minimum, special energy)
- [ ] Between-turns KO pause and SET_ACTIVE requirement tested
- [ ] Cross-identity injection security test exists
- [ ] ACE SPEC validation rule is explicitly **ignored** (multiple ACE SPEC does not fail)
- [ ] At least 1 E2E test exists
- [ ] JaCoCo configured with ≥80% global, ≥90% critical thresholds
- [ ] GameTestFactory (Object Mother) used to reduce boilerplate