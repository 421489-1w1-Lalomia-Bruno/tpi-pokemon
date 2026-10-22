# Plan: Structured damage info in game log

## Overview
Add `source`, `target`, `value` fields to `GameEvent` (BE + FE) so the game log can display structured damage info like:
`+60 damage: Pikachu → Charmander`

## BE Changes

### 1. GameEvent.java — Add structured fields
```diff
  private GameEventType eventType;
  private String matchId;
  private String playerId;
  private String description;
  private LocalDateTime timestamp;
+ private String source;   // attacking card or effect name (e.g. "Pikachu", "Poison")
+ private String target;   // receiving card name
+ private Integer value;   // damage/heal amount (null for non-damage events)
```
- Remove `@AllArgsConstructor`, add constructors for backward compat
- New constructor: `(type, matchId, playerId, description, timestamp, source, target, value)`

### 2. GameEventPublisher.java — Add overloaded publish
```java
public void publish(GameEventType type, String matchId, String playerId,
    String description, String source, String target, Integer value)
```

### 3. ActiveState.java — ATTACK_EXECUTED with damage info
```java
publisher.publish(GameEventType.ATTACK_EXECUTED, matchId, currentPlayerId,
    attacker.getCard().getName() + " used " + selectedAttack.getName(),
    attacker.getCard().getName(),                    // source
    context.getDefender().getCard().getName(),       // target
    context.getFinalDamage());                       // value
```

### 4. StatusEffectManager.java — Publish SPECIAL_CONDITION_APPLIED for between-turns damage
- Add `matchId`, `playerId`, `publisher` parameters to `processBetweenTurns()`
- When poison applies (10 dmg): publish `SPECIAL_CONDITION_APPLIED` with source="Poison", target=pokemon name, value=10
- When burn applies (20 dmg): publish `SPECIAL_CONDITION_APPLIED` with source="Burn", target=pokemon name, value=20
- When sleep resolves: publish `SPECIAL_CONDITION_REMOVED` with source="Sleep", target=pokemon name

### 5. TurnManager.java — Pass publisher info to processBetweenTurns
```diff
- effectManager.processBetweenTurns(currentBoard.getActivePokemon(), board);
+ effectManager.processBetweenTurns(currentBoard.getActivePokemon(), board, matchId, currentPlayerId, eventPublisher);
```

## FE Changes

### 6. game.model.ts — Update GameEvent interface + GameEventType union
```diff
  export type GameEventType = ...
+   | 'BETWEEN_TURNS_PROCESSED'
    | 'SPECIAL_CONDITION_APPLIED'
    | 'SPECIAL_CONDITION_REMOVED' ...;

  export interface GameEvent {
    type: GameEventType;
    playerId: string;
    description: string;
    timestamp: string;
+   source?: string;
+   target?: string;
+   value?: number;
  }
```

### 7. game-log.html — Structured damage display
```html
@for (entry of entries(); track entry.timestamp) {
  <div class="log__entry" [class.log__entry--damage]="isDamage(entry)"
                          [class.log__entry--heal]="isHeal(entry)">
    <span class="log__entry-time">{{ formatTime(entry.timestamp) }}</span>
    <span class="log__entry-desc">
      @if (entry.value != null && entry.source && entry.target) {
        <span class="log__entry-sign">{{ entry.value! > 0 ? '+' : '' }}{{ entry.value }}</span>
        damage:
        <span class="log__entry-actor">{{ entry.source }}</span>
        →
        <span class="log__entry-actor">{{ entry.target }}</span>
        <span class="log__entry-note">{{ entry.description }}</span>
      } @else {
        {{ entry.description }}
      }
    </span>
  </div>
}
```

### 8. game-log.ts — Helper methods
```typescript
isDamage(entry: GameEvent): boolean { return (entry.value ?? 0) > 0; }
isHeal(entry: GameEvent): boolean { return (entry.value ?? 0) < 0; }
```

### 9. game-log.css — Visual styling
```css
.log__entry--damage { border-left: 3px solid var(--poke-red); }
.log__entry--heal { border-left: 3px solid var(--poke-green); }
.log__entry-sign { font-weight: var(--fw-bold); color: var(--poke-red); }
.log__entry--heal .log__entry-sign { color: var(--poke-green); }
.log__entry-actor { font-weight: var(--fw-semibold); color: #fff; }
.log__entry-note { color: var(--text-dim); font-size: 0.7rem; }
```

## Verify
- `mvn compile -q` (BE)
- `npx ng build --configuration development` (FE)
