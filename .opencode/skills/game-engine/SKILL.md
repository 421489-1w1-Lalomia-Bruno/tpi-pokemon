---
name: game-engine
description: Implements Game Engine logic for the Pokemon TCG project. Use when creating or editing files under engine/, writing game rules, state machine, combat pipeline, status effects, or any game logic in the backend. Triggers on engine, game-engine, game rule, state machine, combat, attack, status effect, turn, victory, knockout, match state.
---

# Game Engine Skill - Pokemon TCG

You are implementing the game engine for a digital Pokemon Trading Card Game (set XY1). The engine is the single source of truth for all game rules. All rules reference the project SDD at `docs/sdd/`.

## Architecture Overview

### Package Structure

```
ar.edu.utn.frc.tup.piii.engine/
├── GameEngine.java          (Facade - unified interface to subsystems)
├── combat/
│   ├── AttackResolver.java   (orchestrates the attack pipeline)
│   ├── DamageCalculator.java (base damage, weakness, resistance)
│   └── steps/
│       ├── AttackStep.java            (interface for pipeline steps)
│       ├── EnergyValidationStep.java  (step 1: check energy cost)
│       ├── ConfusionCheckStep.java    (step 2: confused flip)
│       ├── TargetSelectionStep.java   (step 3: select defender)
│       ├── PreAttackEffectStep.java   (step 4: pre-damage effects)
│       ├── AttackModifierStep.java    (step 5: stadium/tools/abilities)
│       ├── DamageCalculationStep.java (step 6: final damage calc)
│       └── PostDamageEffectStep.java  (step 7: post-damage effects)
├── effects/
│   ├── StatusEffectStrategy.java   (interface)
│   ├── AsleepEffect.java
│   ├── BurnedEffect.java
│   ├── ConfusedEffect.java
│   ├── ParalyzedEffect.java
│   ├── PoisonedEffect.java
│   └── StatusEffectManager.java
├── events/
│   ├── GameEvent.java
│   ├── GameEventType.java
│   └── GameEventPublisher.java
├── models/
│   ├── GameCard.java        (interface)
│   ├── PokemonCard.java
│   ├── EnergyCard.java      (supports isSpecial + providedEnergies)
│   ├── TrainerCard.java
│   ├── PokemonInPlay.java   (includes getAvailableEnergyPool())
│   ├── PlayerBoard.java
│   ├── GameBoard.java
│   ├── TurnContext.java
│   └── MatchSnapshot.java
├── rules/
│   ├── RuleValidator.java
│   ├── TurnManager.java
│   └── VictoryConditionChecker.java
└── state/
    ├── MatchState.java       (interface)
    ├── WaitingState.java
    ├── SetupState.java
    ├── ActiveState.java
    └── FinishedState.java
```

## Required Design Patterns

### 1. State Pattern (`engine/state/`)

Match lifecycle goes through: WAITING → SETUP → ACTIVE → FINISHED

Inside ACTIVE, sub-phases are managed by `TurnManager`: DRAW → ACTIONS → ATTACK, with BETWEEN_TURNS processing.

```java
public interface MatchState {
    GamePhase getPhase();
    void handleAction(GameEngine engine, GameActionMessage action);
    void enter(GameEngine engine);
    void exit(GameEngine engine);
}
```

**WaitingState**: accepts `join`, `ready` actions. Transitions to SetupState when both players ready.

**SetupState**: shuffles decks, draws 7 cards, mulligan logic, place basic Pokemon, set 6 prize cards, coin flip for first turn. Transitions to ActiveState (DRAW phase).

**ActiveState**: delegates sub-phase flow to `TurnManager` (DRAW → ACTIONS → ATTACK → BETWEEN_TURNS). DRAW is automatic (engine draws a card, no client action needed). BETWEEN_TURNS processes status effects for the player whose turn is ending, then transitions to opponent's DRAW phase.

**FinishedState**: terminal state; records winner in DB; closes sockets.

**SURRENDER action**: Available at any time during ActiveState. Immediately transitions to FinishedState with the opponent as winner.

### 2. Strategy Pattern (`engine/effects/`)

```java
public interface StatusEffectStrategy {
    SpecialCondition getType();
    void applyBetweenTurns(PokemonInPlay pokemon, GameBoard board);
    void onAttack(PokemonInPlay pokemon, AttackContext context);
    boolean canCoexist(SpecialCondition other);
    void remove(PokemonInPlay pokemon);
}
```

**Incompatibility rules**:
- Asleep, Confused, Paralyzed are MUTUALLY EXCLUSIVE (applying one replaces the others)
- Burned and Poisoned CAN coexist with any condition (even each other)
- All conditions are cleared by: evolution or retreat

**Between-turns processing order**:
1. Poisoned: +1 damage counter
2. Burned: +2 damage counters, flip to remove (heads = removed)
3. Asleep: flip to wake (heads = wakes up, tails = stays asleep and skips turn)
4. **Paralyzed**: cannot attack OR retreat. Auto-removes at end of opponent's next turn (between-turns processing).

**Special retreat restrictions**:
- A Pokemon that is Asleep or Paralyzed CANNOT retreat.
- Retreating clears ALL special conditions.

### 3. Chain of Responsibility (`engine/combat/steps/`)

7-step attack pipeline executed in strict order:

```java
public interface AttackStep {
    void execute(AttackContext context);
    void setNext(AttackStep next);
}
```

Chain: EnergyValidation → ConfusionCheck → TargetSelection → PreAttackEffect → AttackModifier → DamageCalculation → PostDamageEffect

**Step details**:
1. **EnergyValidation**: Check attacker has required energy for the attack using `getAvailableEnergyPool()`. If not, action is invalid.
2. **ConfusionCheck**: If attacker is Confused, flip coin. Heads = proceed normally. Tails = attack fails, 30 self-damage (3 damage counters).
3. **TargetSelection**: Target is ALWAYS the opponent's active Pokemon.
4. **PreAttackEffect**: Execute effects before damage (e.g., "discard 1 energy from the defending Pokemon").
5. **AttackModifier**: Apply damage modifiers from Stadium card, Pokemon Tools, Abilities.
6. **DamageCalculation**: Base damage. Weakness = x2 if attacker type matches defender's weakness. Resistance = -20 if attacker type matches defender's resistance. Minimum 0. 1 damage counter per 10 damage (round up).
7. **PostDamageEffect**: Apply special conditions and other post-damage effects (e.g., "the defending Pokemon is now Burned").

### 4. Observer Pattern (`engine/events/`)

Uses Spring's `ApplicationEventPublisher` to broadcast domain events. The WebSocket infrastructure layer subscribes asynchronously and translates them to STOMP messages, keeping the core engine decoupled from the network protocol.

```java
public enum GameEventType {
    MATCH_CREATED, PLAYER_JOINED, MATCH_STARTED,
    MULLIGAN_EXECED, READY_CONFIRMED,
    CARD_DRAWN, POKEMON_PLACED, ENERGY_ATTACHED, TRAINER_PLAYED,
    POKEMON_EVOLVED, POKEMON_RETREATED, ATTACK_EXECUTED,
    POKEMON_KNOCKED_OUT, PRIZE_TAKEN,
    SPECIAL_CONDITION_APPLIED, SPECIAL_CONDITION_REMOVED,
    TURN_CHANGED, PHASE_CHANGED,
    BETWEEN_TURNS_PROCESSED,
    GAME_FINISHED
}
```

Events are published via `ApplicationEventPublisher` and consumed by the WebSocket listener layer.

### 5. Facade Pattern (`GameEngine.java`)

`GameEngine` is the single entry point for all game operations. It delegates to subsystems:
- State machine (MatchState)
- Combat (AttackResolver)
- Rules (RuleValidator, TurnManager)
- Effects (StatusEffectManager)
- Victory (VictoryConditionChecker)
- Events (GameEventPublisher via ApplicationEventPublisher)

```java
@Service
public class GameEngine {
    public MatchSnapshot processAction(UUID matchId, GameActionMessage action) { ... }
    public MatchSnapshot getMatchSnapshot(UUID matchId) { ... }
}
```

### 6. Repository Pattern (`repositories/`)

JPA-based data access with transparent swap between H2 and PostgreSQL.

## Game Rules Reference

### Turn Structure (RF-01.2)

Each turn has 3 phases:
- **DRAW**: Draw 1 card (skip if first turn of starting player). If deck empty → LOSE (only during Draw phase; effects that cause drawing mid-turn do not cause loss).
- **ACTIONS**: Place Basic Pokemon (unlimited), Evolve (only from next turn, no player can evolve on their first turn of the game), Attach energy (max 1/turn), Play Item (unlimited), Play Supporter (max 1/turn), Play Stadium (max 1/turn), Play Tool (1 per Pokemon, unlimited per turn), Use Ability, Retreat (max 1/turn, costs energy, CANNOT retreat if Asleep or Paralyzed). A player may also PASS_TURN to voluntarily end the ACTIONS phase.
- **ATTACK**: Declare attack with `attackIndex`. Ends the turn. Cannot attack on first turn of starting player.

### Knockout (RF-01.4)

When damage counters x 10 >= Pokemon HP:
1. Discard the KO'd Pokemon and all attached cards (energy, tools)
2. Opponent takes prize cards: 1 for normal, 2 for Pokemon-EX
3. If both active Pokemon are KO'd simultaneously, the player whose turn is ending must promote a new active Pokemon first
4. Owner must promote a bench Pokemon to active, or if bench is empty → LOSE

**Between-turns KO pause**: If a KO occurs during BETWEEN_TURNS processing (e.g., from Poison/Burn), the game pauses and blocks until the affected player sends a `SET_ACTIVE` action to choose their replacement Pokemon.

### Victory Conditions (RF-01.6)

A player wins if:
1. Takes their last prize card
2. Opponent has no Pokemon in play (active or bench)
3. Opponent cannot draw from empty deck during the Draw phase (deck-out during Draw phase only; effects mid-turn do not cause immediate loss)

**Sudden Death**: If both players win simultaneously → restart with 1 prize card each.

### Deck Validation (RF-04.2)

- Exactly 60 cards
- Max 4 copies of same name (except Basic Energy - unlimited)
- At least 1 Basic Pokemon
- ACE SPEC limit is explicitly **ignored** (mazo puede tener multiples ACE SPEC sin fallar validacion)

## Key Model Definitions

### PokemonCard

```java
public class PokemonCard implements GameCard {
    String id;
    String name;
    Integer hp;
    PokemonStage stage;
    Set<EnergyType> types;
    String evolvesFrom;     // Name of pre-evolution (nullable). Lookup by name, not by ID.
    List<Attack> attacks;
    List<Ability> abilities;
    String weaknessType;    // String to allow non-standard values (nullable)
    String resistanceType;  // String to allow non-standard values (nullable)
    Integer retreatCost;
    String imageUrl;
}
```

### Attack

```java
public class Attack {
    String name;
    List<EnergyType> cost;
    int damage;          // 0 for effect-only attacks; 0 for variable damage (effectText holds formula)
    String effectText;   // Null for pure damage attacks
}
```

**Damage parsing from API**: `"60"` → damage=60; `""` → damage=0, effectText=null; `"120+"` → damage=120, effectText="120+"; `"10×"` → damage=0, effectText="10×" + full text.

### Ability

```java
public class Ability {
    String name;
    AbilityType type;    // POKE_BODY or POKE_POWER
    String effectText;
}
```

**AbilityType derivation**: XY1 API always returns `"Ability"` as type. Distinguish by analyzing effectText: passive effects → POKE_BODY, activated effects → POKE_POWER.

### EnergyCard

```java
public class EnergyCard implements GameCard {
    private String id;
    private String name;
    private EnergyType energyType;
    private boolean isSpecial;
    private List<EnergyType> providedEnergies; // Must be hardcoded manually for special energy cards
}
```

**Note on `providedEnergies`**: The API does not provide this field. For special energy cards like Double Colorless Energy, the `providedEnergies` must be hardcoded manually in the engine or in the sync service.

**Note on Basic Energy**: The XY1 API response may not include basic energy cards. If missing, seed them manually via Flyway V5 with: supertype="Energy", subtypes=["Basic"], stage=null, hp=null.

### PokemonInPlay

Instance of a Pokemon on the board with dynamic state:
- `card`: PokemonCard reference
- `currentHp`: remaining HP
- `damageCounters`: accumulated damage
- `attachedEnergy`: List<EnergyCard>
- `tool`: TrainerCard (or null)
- `specialConditions`: Set<SpecialCondition>
- `isActive`: boolean
- `turnsInPlay`: int (for evolution timing)

Key method: `getAvailableEnergyPool()` — flattens all attached energies (including special energy provided amounts) for attack cost validation.

### TurnContext

Tracks per-turn state:
- `currentPlayerId`: whose turn
- `phase`: DRAW / ACTIONS / ATTACK (TurnPhase enum)
- `isFirstTurn`: is this the first turn of the game
- `hasDrawn`: has player drawn this turn
- `hasPlayedSupporter`: has a Supporter been played
- `hasPlayedStadium`: has a Stadium been played
- `hasEvolved`: has evolution happened
- `hasAttachedEnergy`: has energy been attached
- `hasUsedAbility`: has ability been used
- `hasRetreated`: has retreat been used

### MatchSnapshot

Complete serializable state for WebSocket transfer and JSONB persistence:
- `matchId`, `gamePhase` (GamePhase enum), `board` (GameBoard), `turn` (TurnContext), `winnerId`

Note: `eventLog` is NOT included in MatchSnapshot for performance (RNF-01: <50ms WebSocket). Events are delivered separately and can be paginated.

### GameBoard

- `player1Board`: PlayerBoard
- `player2Board`: PlayerBoard
- `sharedStadium`: TrainerCard (or null)

### PlayerBoard

- `playerId`, `activePokemon` (PokemonInPlay), `bench` (List<PokemonInPlay>, max 5), `hand`, `deck`, `discardPile`, `prizeCards` (6), `prizeCardsTaken` (int)

## Enums

Use `GamePhase` (4 values: WAITING, SETUP, ACTIVE, FINISHED) for match lifecycle states and `TurnPhase` (4 values: DRAW, ACTIONS, ATTACK, BETWEEN_TURNS) for turn sub-phases within ActiveState. `MatchStatus` (5 values: WAITING, SETUP, ACTIVE, FINISHED, ABANDONED) is a separate enum for persistence in MatchEntity — it includes ABANDONED for surrendered/abandoned matches.

## Important Constraints

- **Backend is the ONLY source of truth**. All rule validation happens server-side.
- **After every action**, the full state is persisted atomically as JSONB and broadcast to both clients.
- **Mulligan rule**: If a player has no Basic Pokemon in initial hand, reveal hand, reshuffle deck, redraw 7 cards. Opponent draws 1 extra card per mulligan. If both players mulligan simultaneously, mulligans cancel one-to-one.
- **First-turn restrictions**: No player can evolve on their first turn of the game. The starting player cannot attack on their first turn and skips their Draw phase.
- **Sudden Death**: If both players win simultaneously, restart with 1 prize card each.
- **No REST calls during gameplay** — all actions go through WebSocket.
- **Payload format for RETREAT**: `{ "benchPokemonId": "uuid", "energyCardsToDiscard": ["uuid1", "uuid2"] }`
- **Payload format for ATTACK**: `{ "attackIndex": 0 }` (integer index, not name)
- **PASS_TURN** is a valid action type to voluntarily end the ACTIONS phase.

## Implementation Checklist

When implementing engine classes, ensure:
- [ ] Class uses correct package: `ar.edu.utn.frc.tup.piii.engine.*`
- [ ] State classes implement `MatchState` interface
- [ ] Effect classes implement `StatusEffectStrategy` interface
- [ ] Attack steps implement `AttackStep` interface
- [ ] GameEngine delegates to subsystems (never implements logic directly)
- [ ] All state changes are persisted via `GameStatePersistenceService`
- [ ] All state changes emit events via Spring `ApplicationEventPublisher`
- [ ] GamePhase has 4 values: WAITING, SETUP, ACTIVE, FINISHED
- [ ] TurnPhase has 4 values: DRAW, ACTIONS, ATTACK, BETWEEN_TURNS
- [ ] MatchStatus (for persistence) has 5 values: WAITING, SETUP, ACTIVE, FINISHED, ABANDONED
- [ ] Retreat is blocked if Pokemon is Asleep or Paralyzed
- [ ] Between-turns KO triggers SET_ACTIVE pause requirement
- [ ] EnergyCard supports isSpecial and providedEnergies for Special Energy
- [ ] PokemonInPlay has getAvailableEnergyPool() method
- [ ] RETREAT action requires energyCardsToDiscard array
- [ ] ATTACK action uses attackIndex (integer), not attackName
- [ ] PASS_TURN action type is supported
- [ ] SURRENDER action type is supported (immediately ends match, opponent wins)
- [ ] DRAW is automatic — no DRAW_CARD action in the protocol
- [ ] PokemonCard includes `evolvesFrom` field (String, nullable)
- [ ] Attack `damage` field: parsed from API string, 0 for effect-only, effectText holds formula
- [ ] Ability `type` derived from effectText (POKE_BODY vs POKE_POWER)
- [ ] EnergyCard `providedEnergies` must be hardcoded for special energy cards
- [ ] Weakness value is always ×2 for XY1; resistance value is always -20
- [ ] Paralyzed: cannot attack OR retreat, auto-removes at end of opponent's next turn