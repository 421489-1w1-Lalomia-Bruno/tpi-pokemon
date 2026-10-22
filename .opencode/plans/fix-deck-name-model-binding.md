# Fix: Guardar Mazo siempre deshabilitado

## Root Cause
El pattern `[ngModel]="deckName()" + (ngModelChange)` con `OnPush` requiere un round-trip al parent para que `deckName` actualice su valor en el hijo. Durante la primera pasada de CD, `deckName()` sigue vacío → `deckName().trim().length === 0 === true` → botón deshabilitado.

## Fix: model() en lugar de input() + output()

### 1. deck-editor.ts
```diff
-import { ChangeDetectionStrategy, Component, input, output, computed } from '@angular/core';
+import { ChangeDetectionStrategy, Component, input, output, model, computed } from '@angular/core';
 ...
-readonly deckName = input('');
+readonly deckName = model('');
 ...
-readonly nameChange = output<string>();
...
-onNameChange(value: string): void {
-  this.nameChange.emit(value);
-}
```
`model()` actualiza localmente de inmediato y propaga al parent sin round-trip.

### 2. deck-editor.html
```diff
-<input [ngModel]="deckName()" (ngModelChange)="onNameChange($event)" ... />
+<input [(ngModel)]="deckName" ... />
```

### 3. deck-builder-page.html
```diff
-<app-deck-editor
-  [deckName]="currentDeckName()"
-  (nameChange)="onNameChange($event)"
-  ...
/>
+<app-deck-editor
+  [(deckName)]="currentDeckName"
+  ...
/>
```

### 4. deck-builder-page.ts
```diff
-onNameChange(name: string): void {
-  this.currentDeckName.set(name);
-}
```
Eliminar método - el two-way binding del modelo lo maneja automáticamente.

### Verificar: `npx ng build --configuration development`
