# Plan: "Al Azar" y "Borrar" buttons en deck-builder

## Overview
Add "Al Azar" (fill random) and "Borrar todo" (clear all) buttons to the deck editor, alongside "Guardar mazo".

## Files to modify

### 1. `deck-editor.ts` — Add new outputs
```diff
+ readonly fillRandom = output<void>();
+ readonly clearAll = output<void>();
```

### 2. `deck-editor.html` — Restructure actions area
Move `editor__actions` outside `@if (cards().length > 0)` so it's always visible.  
Order: **Al Azar** → **Guardar mazo** → **Borrar todo** → Eliminar

```html
<div class="editor__actions">
  <button (click)="fillRandom.emit()" [disabled]="maxCardsReached() || saving()">
    Al Azar
  </button>
  @if (cards().length > 0) {
    <button class="editor__save-btn" (click)="save.emit()"
      [disabled]="saving() || deckName().trim().length === 0 || totalCards() !== 60">
      {{ saving() ? 'Guardando…' : 'Guardar mazo' }}
    </button>
  }
  @if (cards().length > 0) {
    <button class="editor__clear-btn" (click)="clearAll.emit()" [disabled]="saving()">
      Borrar todo
    </button>
  }
  @if (editing() && cards().length > 0) {
    <button class="editor__delete-btn" (click)="delete.emit()" [disabled]="saving()">
      Eliminar
    </button>
  }
</div>
```

### 3. `deck-builder-page.ts` — Add onRandomFill() + onClearAll()
- Inject `CardService`
- `onClearAll()` → `this.currentDeckCards.set([])`
- `onRandomFill()` → algorithm:
  1. Calculate remaining slots (60 - currentTotal)
  2. Fetch Pokemon (size:100), Trainer (size:100), Energy (size:50) pools
  3. Shuffle and pick cards randomly, respecting:
     - Max 4 copies by name (except Basic Energy unlimited)
     - At least 1 Basic Pokemon if deck is empty
  4. Update `currentDeckCards` with new cards

### 4. `deck-builder-page.html` — Wire new outputs
```diff
  <app-deck-editor
    ...
    (save)="onSave()"
+   (fillRandom)="onRandomFill()"
+   (clearAll)="onClearAll()"
  />
```

### 5. `deck-editor.css` — Style new buttons
- `.editor__random-btn`: purple/gold gradient
- `.editor__clear-btn`: red/danger style
- `.editor__actions`: flex layout for multiple buttons

## Verify
- `npx ng build --configuration development`
