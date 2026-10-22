# Fix: Cada bench-slot como cdkDropList individual

## Root cause
Un solo `cdkDropList` para todo el bench impide targeting preciso a slots individuales. El `currentIndex` depende de elementos `cdkDrag` hijos y no mapea bien a los slots visuales.

## Plan

### 1. bench-area.html — cdkDropList por slot
```diff
-<div
-  cdkDropList
-  id="benchZone"
-  [cdkDropListData]="slots()"
-  class="bench-area"
+<div class="bench-area"
   [class.bench-area--active]="isMyTurn() && phase() === 'ACTIONS'"
-  (cdkDropListDropped)="cardDropped.emit($event)"
 >
   @for (slot of slots(); track $index) {
     <div
-      cdkDrag
-      [cdkDragData]="slot"
-      [cdkDragDisabled]="true"
+      cdkDropList
+      [id]="'benchSlot-' + $index"
+      [cdkDropListData]="slot"
       class="bench-slot"
+      (cdkDropListDropped)="cardDropped.emit($event)"
       ...
```

### 2. bench-area.ts — Quitar CdkDrag, solo CdkDropList
```diff
-import { CdkDropList, CdkDragDrop, CdkDrag } from '@angular/cdk/drag-drop';
+import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
 ...
-imports: [CdkDropList, CdkDrag, CardComponent, DamageCounterComponent, StatusIndicatorComponent, EnergyBadgeComponent],
+imports: [CdkDropList, CardComponent, DamageCounterComponent, StatusIndicatorComponent, EnergyBadgeComponent],
```

### 3. player-area.ts — Actualizar onCardDropped
```diff
 onCardDropped(event: CdkDragDrop<any>): void {
   const card = event.item.data as GameCardModel;
   const targetZone = event.container.id;

-  if (targetZone === 'benchZone') {
-    const bench = event.container.data as (PokemonInPlayModel | null)[];
-    const targetPokemon = bench[event.currentIndex];
+  if (targetZone.startsWith('benchSlot')) {
+    const targetPokemon = event.container.data as PokemonInPlayModel | null;
```

### 4. bench-area.css — Slots ocupan área + feedback visual
```diff
 .bench-slot {
-  width: 130px;
+  flex: 1;
+  max-width: 130px;
   border-radius: var(--r-sm);
   border: 2px dashed rgba(255, 255, 255, 0.06);
   transition: border-color var(--transition-base);
 }

+.bench-slot.cdk-drop-list-receiving {
+  border-color: var(--poke-blue);
+  background: rgba(79, 155, 255, 0.08);
+  box-shadow: 0 0 12px rgba(79, 155, 255, 0.2);
+}
```

### 5. player-area.css — Bench ocupa ancho completo
```diff
 .player-area__bench {
   display: flex;
+  align-items: stretch;
   justify-content: center;
+  gap: 0.4rem;
   flex-shrink: 0;
   padding: 0.15rem 0;
 }
```

### 6. Verificar build: npx ng build --configuration development
