# Fix: Arrastrar cartas de mano al banco

## Root cause
El `cdkDropList` del bench no tiene hijos con `cdkDrag`. CDK necesita elementos `cdkDrag` para calcular correctamente `currentIndex`. Sin ellos, `currentIndex` siempre es `0`, impidiendo apuntar a los slots visuales correctos.

## Plan

### 1. `bench-area.ts` — Agregar `CdkDrag` a los imports
```diff
-import { CdkDropList, CdkDragDrop } from '@angular/cdk/drag-drop';
+import { CdkDropList, CdkDragDrop, CdkDrag } from '@angular/cdk/drag-drop';
```
Agregar `CdkDrag` al array `imports` del decorador.

### 2. `bench-area.html` — Agregar `cdkDrag` disabled a cada slot
```diff
  @for (slot of slots(); track $index) {
    <div
+     cdkDrag
+     [cdkDragData]="slot"
+     [cdkDragDisabled]="true"
      class="bench-slot"
      ...
```

### 3. Verificar con `npx ng build --configuration development`
