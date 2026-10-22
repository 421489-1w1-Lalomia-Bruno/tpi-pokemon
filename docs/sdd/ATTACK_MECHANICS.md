# Attack Mechanics Integration Plan - Pokemon TCG

## 1. Overview
The goal is to integrate all attack mechanics from the Pokemon XY1 set. The previous implementation only handled numeric damage and a few hardcoded special conditions, failing to execute attacks that have non-damage effects (e.g., drawing cards, switching Pokemon) or mixed damage+effect behaviors.

## 2. Core Architecture

### 2.1. Attack Effect Resolution
Instead of hardcoding attack logic, the engine now uses a data-driven approach:
- **`AttackEffectResolver`**: Analyzes the `effectText` (English/Spanish) using regular expressions to produce a list of structured `AttackEffect` objects.
- **`AttackEffect`**: A value object defining:
    - `EffectOperation`: What action to perform (e.g., `DRAW`, `HEAL`, `SWITCH_SELF`).
    - `TargetScope`: Who the effect targets (`SELF`, `OPP_ACTIVE`, `OPP_BENCH`, etc.).
    - `amount`: Numeric value for the operation (e.g., number of cards to draw).
    - `timing`: When the effect triggers relative to damage.
    - `subType`: Specific identifier (e.g., for `FLIP_COIN_STATUS` to specify "BURNED").

### 2.2. Effect Timing (`EffectTiming`)
- `INSTEAD_OF_DAMAGE`: The effect replaces damage entirely. If detected, the damage pipeline is bypassed.
- `BEFORE_DAMAGE`: Triggered before damage is calculated/applied.
- `WITH_DAMAGE`: Triggered simultaneously with damage.
- `AFTER_DAMAGE`: Triggered after damage has been applied to the defender.
- `PERSISTENT`: Ongoing effects.

### 2.3. Combat Pipeline Integration
The `AttackResolver` chain was refactored from 10 steps to 8:
1. `EnergyValidationStep`
2. `EnergyDiscardStep`
3. `ConfusionCheckStep`
4. `TargetSelectionStep`
5. `PreAttackEffectStep`
6. `VariableDamageStep`
7. `AttackModifierStep`
8. `DamageCalculationStep` $\rightarrow$ **`AttackEffectStep`** (New)

**`AttackEffectStep`** replaces the legacy `PostDamageEffectStep` and `FlipEffectStep`. It handles:
1. Application of damage (if not `INSTEAD_OF_DAMAGE`).
2. Application of Special Conditions (filtered to avoid duplicates).
3. Execution of all resolved `AttackEffect` objects.

### 2.4. Shared Logic (`GameActionUtils`)
To avoid duplication between `TrainerEffect`, `AbilityEffect`, and `AttackEffect`, all game-state mutating actions are centralized in `GameActionUtils`.
- `drawCards()`, `healDamage()`, `searchDeck()`, `switchActive()`, `discardEnergy()`, etc.

## 3. Implementation Roadmap

### Phase 1: Basic Effects & Infrastructure (Completed ✅)
- [x] Create `GameActionUtils` for shared logic.
- [x] Refactor `TrainerEffect` implementations to use `GameActionUtils`.
- [x] Implement `EffectOperation`, `TargetScope`, and `EffectTiming`.
- [x] Implement `AttackEffectResolver` with basic patterns.
- [x] Implement `AttackEffectStep` and integrate into `AttackResolver` chain.
- [x] **Fix `CardMapper`**: Prevent discarding `effectText` when damage parses as a normal integer.
- [x] Implement condition filtering to prevent double-applying status effects.

### Phase 2: Target & Energy Manipulation (Current)
- [ ] **Bench Damage**: Update `DAMAGE_BENCH` to allow targeting specific numbers of benched Pokemon instead of all.
- [ ] **Conditional Damage**: Integrate coin-flip results directly into `AttackEffect` for more complex triggers.
- [ ] **Energy Attach**: Refine `ENERGY_ATTACH` to support searching for specific energy types.
- [ ] **Opponent Switch**: Ensure `SWITCH_OPPONENT` correctly handles target selection from the opponent's bench.

### Phase 3: Deck & Discard Interactions
- [ ] **Advanced Search**: Support searching for specific card types (Pokemon, Trainer, Energy).
- [ ] **Recycle/Discard**: Implement `RECYCLE` (discard $\rightarrow$ hand) and `DISCARD` (hand $\rightarrow$ discard).
- [ ] **Shuffle Effects**: Implement "shuffle hand into deck" and "shuffle deck" operations.

### Phase 4: Complex Hybrid Patterns
- [ ] **Multi-Effects**: Support attacks that trigger multiple different operations in a specific sequence.
- [ ] **Damage Hybrids**: Handle "Damage + Effect" where the effect depends on the damage dealt.
- [ ] **Conditional Timing**: Implement effects that trigger only if damage is $\geq$ a certain value.

### Phase 5: Edge Cases & Validation
- [ ] **Full XY1 Audit**: Verify every attack in the XY1 set matches the resolver patterns.
- [ ] **Integration Testing**: End-to-end tests for complex cards (e.g., Lunatone's "Double Draw").
- [ ] **Performance Audit**: Ensure regex parsing doesn't introduce latency in the combat loop.

## 4. Critical Technical Decisions
- **Regex-Based Resolution**: Chosen for flexibility over a hardcoded database, allowing the engine to support new cards from the API without code changes, provided patterns are generic enough.
- **Chain of Responsibility**: Maintained the `AttackStep` architecture to keep damage calculation separate from effect execution.
- **Timing-Based Bypassing**: `INSTEAD_OF_DAMAGE` timing is used as a flag to skip the `takeDamage()` call on the defender.
