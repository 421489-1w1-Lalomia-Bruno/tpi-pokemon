# Fix: Doble creación + Mostrar ID de sala

## Issue 1: Doble creación al clickear nuevamente después de "Partida creada"

**Root cause**: `creating` se pone en `false` inmediatamente al recibir respuesta HTTP, y `selectedDeckId` no se limpia. El botón "Crear Partida" queda habilitado durante los 1.5s antes del redirect.

**Fix 1a**: `create-match-dialog.html` — Ocultar `dialog-footer` cuando `successful()` es true
```diff
+@if (!successful()) {
   <div class="dialog-footer">
     ...
   </div>
+}
```

**Fix 1b**: `create-match-dialog.ts` — Limpiar `selectedDeckId` en success (seguridad extra)
```diff
  next: (match: MatchModel) => {
    this.creating.set(false);
+   this.selectedDeckId.set(null);
    this.successful.set(true);
```

## Issue 2: ID de sala no visible en pantalla de espera

`setup-phase.html` muestra el username pero nunca el `matchId`, que está disponible como `@Input()`.

**Fix 2a**: `setup-phase.html` — Agregar `matchId` en WAITING y SETUP
```diff
  <h1>Preparando Partida</h1>
+ <p class="match-id">Sala: {{ matchId() }}</p>
  <p class="player-id">Jugador: {{ username() }}</p>
```

También en la sección SETUP (debajo del h2):
```diff
  <h2>Colocá un Pokémon Básico</h2>
+ <p class="match-id">Sala: {{ matchId() }}</p>
```

**Fix 2b**: `setup-phase.css` — Estilo para `.match-id`
```css
.match-id {
  font-family: var(--font-mono, 'Courier New', monospace);
  font-size: 0.8rem;
  color: var(--poke-blue);
  background: rgba(79, 155, 255, 0.12);
  padding: 0.25rem 0.75rem;
  border-radius: var(--r-full);
  display: inline-block;
  user-select: all;
}
```

## Verificar: `npx ng build --configuration development`
