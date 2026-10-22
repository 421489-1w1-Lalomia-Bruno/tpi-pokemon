# 06 - Frontend

**Nota de Arquitectura:** Esta especificación alinea por completo el desarrollo del cliente en Angular con el rigor técnico, seguridad y flujo reactivo impuestos por el nuevo motor de juego del backend. Toda la interfaz se rige por un flujo unidireccional impulsado por eventos WebSocket y gobernado por Signals.

## Estándares de Código Obligatorios (AGENTS.md)

El desarrollo de la interfaz de usuario sigue de forma estricta las directrices modernas de la guía de estilos de Angular y las convenciones del equipo (RNF-02). Los agentes y desarrolladores deben programar bajo estas reglas inquebrantables:

* **Standalone Components Nativo:** Toda la aplicación prescinde de `NgModules`. Al estar bajo Angular 20+, las clases son componentes standalone por defecto; la propiedad `standalone: true` **NO** se debe declarar de forma explícita en los decoradores.
* **Gestión de Estado Unificada con Signals:** El estado local y global se maneja mediante mecanismos reactivos primitivos (`signal()`). Está terminantemente prohibido el uso de librerías de terceros (como NgRx) o el uso de variables mutables clásicas de JS.
* **Detección de Cambios Eficiente:** Todos los componentes de las features y compartidos deben configurar la estrategia de detección de cambios en **`changeDetection: ChangeDetectionStrategy.OnPush`**.
* **Flujo de Control Moderno:** Se exige el uso exclusivo de la sintaxis nativa de control flow (`@if`, `@for`, `@switch`). Queda prohibido el uso de las directivas heredadas `*ngIf` y `*ngFor`.
* **Enlaces Limpios (Bindings):** * No usar `ngClass` ni `ngStyle`. Utilizar bindings nativos a propiedades de clase (`[class.nombre-clase]="condicion"`) o estilos inline dinámicos.
    * No usar `@HostBinding` ni `@HostListener`. Toda interacción con el elemento anfitrión debe declararse en el objeto `host: {}` dentro del decorador `@Component`.
* **Inyección Inversa:** Toda dependencia (servicios, HTTP, enrutadores) se resuelve mediante la función **`inject()`** en la definición de propiedades de la clase, eliminando la inyección clásica por parámetros del constructor.

---
## Patrón de Gestión del Tablero (`GameStateService`)

El juego en tiempo real (Semana 5) es puramente reactivo y depende de las actualizaciones del servidor (fuente de verdad). El cliente es un mero intérprete visual.

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        GameWebSocketService                            │
│            (Escucha streams STOMP raw desde /ws/game)                  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │ (Push de MatchSnapshot)
                                     ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          GameStateService                              │
│  - matchState = signal<MatchSnapshot | null>(null)                     │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         ▼ (computed)                ▼ (computed)                ▼ (computed)
┌──────────────────┐        ┌──────────────────┐        ┌──────────────────┐
│   currentPlayer  │        │   activePokemon  │        │     myHand       │
└──────────────────┘        └──────────────────┘        └──────────────────┘
```

1- **El State Signal Principal**: El `GameStateService` centraliza la partida en un único `WritableSignal<MatchSnapshot | null>(null)`. Al recibir un mensaje STOMP del tipo `STATE_UPDATE`, pisa el valor del signal.
2- **Estado Derivado Imutable**: Los componentes del tablero no guardan estado propio. Consumen propiedades derivadas usando `computed()` del servicio (ej. `this.activePokemon = computed(() => this.gameStateService.matchState()?.board.player1Board.activePokemon)`). Esto garantiza renderizados instantáneos y libres de desfases temporales.
## Auth System (Semana 5)

### AuthService (`core/services/auth.service.ts`)

Servicio singleton que gestiona la sesión del usuario. Utiliza signals para estado reactivo.

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
    private readonly http = inject(HttpClient);
    private readonly router = inject(Router);
    private readonly _currentUser = signal<UserProfile | null>(null);
    private readonly _isLoggedIn = signal(false);

    readonly currentUser = this._currentUser.asReadonly();
    readonly isLoggedIn = computed(() => this._isLoggedIn());

    login(username: string, password: string): Observable<void> { ... }
    register(username: string, password: string, gender?: string): Observable<void> { ... }
    logout(): void { ... }
    getToken(): string | null { ... }
    private saveSession(token: string, user: UserProfile): void { ... }
    private restoreSession(): void { ... }
}
```

**Flujo:**
- Al login/register exitoso, guarda `{ token, user }` en `localStorage` y actualiza `_currentUser` / `_isLoggedIn`
- Al iniciar la app (`restoreSession()`), verifica si hay token en `localStorage` y lo restaura
- `logout()` limpia `localStorage` y redirige a `/login`
- `getToken()` retorna el JWT para adjuntarlo en peticiones HTTP

### JwtInterceptor (`core/interceptors/jwt.interceptor.ts`)

Interceptor HTTP que adjunta el token JWT a todas las peticiones excepto `/api/auth/**`.

```typescript
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const token = authService.getToken();
    if (token && !req.url.includes('/api/auth/')) {
        req = req.clone({
            headers: req.headers.set('Authorization', `Bearer ${token}`)
        });
    }
    return next(req);
};
```

### AuthGuard (`core/guards/auth.guard.ts`)

Protege las rutas que requieren autenticación.

```typescript
export const authGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    if (authService.isLoggedIn()) {
        return true;
    }
    return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
    });
};
```

### LoginPage (`features/auth/pages/login-page`)

Formulario de inicio de sesión. Si el usuario ya está autenticado, redirige a `/pokedex`.

| Campo | Tipo | Validación |
|---|---|---|
| Username | Texto | Requerido |
| Contraseña | Password | Requerido |

**Comportamiento:** Al login exitoso, redirige a `returnUrl` (query param) o a `/pokedex` por defecto.

### RegisterPage (`features/auth/pages/register-page`)

Formulario de creación de cuenta.

| Campo | Tipo | Validación |
|---|---|---|
| Username | Texto | Requerido, 3-30 caracteres, alfanumérico |
| Contraseña | Password | Requerido, mínimo 8 caracteres |
| Repetir contraseña | Password | Debe coincidir con contraseña |
| Género | Select | Opcional: Masculino / Femenino / Prefiero no decirlo / Otro |

**Comportamiento:** Al registro exitoso, redirige a `/pokedex` (queda autenticado automáticamente).

### ProfilePage (`features/profile/pages/profile-page`)

Página de perfil del usuario autenticado. Protegida con `AuthGuard`.

| Sección | Contenido |
|---|---|
| **Datos del usuario** | Username, género, avatar según género (MALE/FEMALE = sprite de entrenador), fecha de creación |
| **Mazos** | Lista completa de mazos del usuario con nombre, cantidad de cartas, estado de validación, botón eliminar |
| **Espacio futuro** | Zona reservada para nivel, experiencia, logros, estadísticas de partidas |

### Navegación con Auth

- Sin sesión: solo accesible `/login` y `/register`. Cualquier otra ruta redirige a `/login`
- Con sesión: accesible todo. El header/menú muestra el username del usuario autenticado
- Al hacer logout: redirige a `/login` y limpia localStorage

### app.routes.ts (actualizado)

```typescript
export const routes: Routes = [
    { path: 'login', loadComponent: () => import('./features/auth/pages/login-page').then(m => m.LoginPage) },
    { path: 'register', loadComponent: () => import('./features/auth/pages/register-page').then(m => m.RegisterPage) },
    {
        path: 'pokedex',
        canActivate: [authGuard],
        loadComponent: () => import('./features/pokedex/pages/pokedex-page').then(m => m.PokedexPage)
    },
    {
        path: 'deck-builder',
        canActivate: [authGuard],
        loadComponent: () => import('./features/deck-builder/pages/deck-builder-page').then(m => m.DeckBuilderPage)
    },
    {
        path: 'lobby',
        canActivate: [authGuard],
        loadComponent: () => import('./features/lobby/pages/lobby-page').then(m => m.LobbyPage)
    },
    {
        path: 'game/:matchId',
        canActivate: [authGuard],
        loadComponent: () => import('./features/game/pages/game-page').then(m => m.GamePage)
    },
    {
        path: 'profile',
        canActivate: [authGuard],
        loadComponent: () => import('./features/profile/pages/profile-page').then(m => m.ProfilePage)
    },
    { path: '', redirectTo: 'pokedex', pathMatch: 'full' },
    { path: '**', redirectTo: '' }
];
```

### app.config.ts (actualizado)

Agregar `jwtInterceptor` al `provideHttpClient`:

```typescript
provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor]))
```

### Actualización de servicios existentes

- `DeckService.playerHeaders()`, `LobbyService.playerHeaders()` y todos los lugares que usaban `X-Player-Id` ahora leen el token JWT del `AuthService` o simplemente envían el header `Authorization` (manejado automáticamente por `jwtInterceptor`)
- `WebsocketService.connect()` ahora envía `Authorization` en vez de `X-Player-Id` en el header del CONNECT STOMP

---

## Features

### Pokedex (`/pokedex`) — Protegida con AuthGuard

Consulta, filtrado estricto y visualización del catálogo completo de cartas del set XY1 almacenadas en la base de datos local del servidor.

| Componente | Responsabilidad |
|---|---|
| `PokedexPage` | Página principal con grid de cartas |
| `CardComponent` (shared) | Visualización individual de carta |

**Servicios:** `CardService` (Peticiones HTTP REST orientadas a /`api/cards`)

**Estado:** Lista de cartas, filtros, paginación

### Deck Builder (`/deck-builder`) — Protegido con AuthGuard

Creador e inspector interactivo de mazos de 60 cartas. Ejecuta validaciones automáticas contra la API REST.

| Componente | Responsabilidad |
|---|---|
| `DeckBuilderPage` | Layout general de la mesa de edición. |
| `CardCatalogComponent` | Catálogo lateral con buscador dinámico por texto. |
| `CardDetailModalComponent` | Modal con detalle de carta |
| `DeckEditorComponent` | Lista de cartas actuales del mazo con selectores de cantidades.|
| `DeckListComponent` | Lista de mazos guardados. Incluye botón "Usar en partida". |
| `DeckSummaryComponent` | Alertas visuales de errores devueltos por el validador (ej. "Falta Pokémon Básico"). |

**Fixes aplicados en Semana 5:**
1. **F1 — Límite 4 por nombre:** Las validaciones de cantidad máxima se agrupan por `card.name`, no por `card.id`. Dos prints distintos del mismo Pokémon comparten el límite de 4 copias.
2. **F2 — Energy básicas sin límite:** Las cartas con `supertype === 'Energy'` y `subtypes.includes('Basic')` no tienen límite de copias.
3. **F3+F6 — Validación local pre-save:** `validateDeckLocally()` se ejecuta automáticamente al cambiar cartas, simulando las reglas del backend y mostrando errores antes de guardar.
4. **F4 — Error real del backend:** El mensaje de error en `onSave()` extrae `err.error?.message` del backend en lugar del mensaje genérico.
5. **F5 — Save condicional:** El botón Guardar se deshabilita si `totalCards() !== 60`.
6. **"Mazo equipado":** Botón "Usar en partida" en `DeckListComponent` que guarda el deckId en localStorage. `MatchListComponent.onJoin()` lo usa para unirse a partidas.

**Servicios:** `CardService`, `DeckService`

**Estado:** Cartas disponibles, mazo actual, errores de validación

### Lobby (`/lobby`) — Protegido con AuthGuard

Sala de encuentro multijugador para crear y listar partidas activas.

| Componente | Responsabilidad |
|---|---|
| `LobbyPage` | Dashboard principal |
| `MatchListComponent` | Lista de partidas `WAITING` |
| `CreateMatchDialogComponent` | Selector de mazo para abrir mesa|

**Servicios:** `LobbyService`, `WebsocketService`

**Estado:** Partidas disponibles, partida actual

### Game Board (`/game`)
El núcleo interactivo del juego en tiempo real por WebSockets. Se divide en sub-componentes especializados:

| Componente | Responsabilidad |
|---|---|
| `GamePage` | Orquestador de la vista. Suscribe al WebSocket al cargar y limpia canales al salir. |
| `SetupPhaseComponent` | Pantalla de bloqueo inicial. Muestra el botón "READY". Al pulsarse, emite la acción y renderiza un spinner de espera mientas el backend resuelve mulligans de forma automática.|
| `PlayerAreaComponent`/ `OpponentAreaComponent`| Zonas simétricas del tablero (Mano, Premios, Mazo, Descarte, Activo, Banca). El área del oponente enmascara las cartas según el snapshot de seguridad. |
| `ActivePokemonComponent`/ `BenchAreaComponent` | Representación de los Pokémon en juego. Renderizan dinámicamente los contadores de daño y badges de condiciones especiales (`ASLEEP`, `POISONED`, etc.).|
| `ActionPanelComponent` | Botones de acciones contextuales deshabilitados por flags de fase (`TurnContext`). Contiene el botón obligatorio "Pasar Turno" (`PASS_TURN`) para finalizar la fase principal voluntariamente. |
| `PromoteModalComponent` | Modal de interrupción táctica. Si el motor backend envía una pausa por KO, este modal se despliega de forma obligatoria bloqueando el juego y exigiendo al usuario hacer clic sobre un Pokémon de su banca para emitir el mensaje `SET_ACTIVE`.|


### Game (`/game/:matchId`)

Tablero de juego interactivo en tiempo real.

| Componente | Responsabilidad |
|---|---|
| `GamePage` | Página principal del juego |
| `PlayerAreaComponent` | Área del jugador local |
| `OpponentAreaComponent` | Área del oponente |
| `ActivePokemonComponent` | Pokémon activo del jugador |
| `BenchAreaComponent` | Banca del jugador (hasta 5 Pokémon) |
| `HandAreaComponent` | Mano del jugador (cartas ocultas del oponente) |
| `StadiumAreaComponent` | Zona de estadio |
| `ActionPanelComponent` | Panel de acciones (botones contextuales) |
| `AttackSelectorComponent` | Selector de ataque |
| `DeckPileComponent` | Visualización del mazo (cantidad) |
| `DiscardPileComponent` | Pila de descarte |
| `PrizeCardsComponent` | Cartas de premio (cantidad restante) |
| `SetupPhaseComponent` | Fase de configuración inicial |
| `EvolutionSelectorComponent` | Selector de evolución |
| `GameOverComponent` | Pantalla de fin de partida |
| `NotificationToastComponent` | Notificaciones visuales |

**Servicios:** `GameStateService`, `GameWebsocketService`

**Estado:** Tablero, turno, fase, carta seleccionada, notificaciones

## Arquitectura de Listas Conectadas
Las zonas del tablero se definen como contenedores conectados mediante directivas del CDK:
```HTML
<div cdkDropList 
     [cdkDropListData]="myHand()" 
     [cdkDropListConnectedTo]="['activeZone', 'benchZone']"
     (cdkDropListDropped)="onCardDropped($event)">
  @for (card of myHand(); track card.id) {
    <div cdkDrag [cdkDragData]="card">
      <app-card [card]="card"></app-card>
    </div>
  }
</div>
```

## Manejo del Evento de Caída (`Dropped`)
Al soltar una carta en una zona válida, el componente frontend NO altera los arreglos de datos locales. Captura el evento, extrae la información del objeto `CdkDragDrop` y despacha de forma inmediata el mensaje estructurado de acción al WebSocket:

- De `HAND` a `BENCH` → Envía `PLACE_POKEMON` con `{ zone: 'BENCH' }`.
- De `HAND` a `ActivePokemonComponent` (Carta tipo Energía) → Envía `ATTACH_ENERGY` inyectando el ID del Pokémon objetivo.

## Componentes Compartidos (`Shared`)

- **CardComponent**: Directiva de renderizado de cartas. Usa bindings de imágenes con resoluciones optimizadas de `imageUrl`. Cambia visualmente su escala si es una carta de tamaño Pokémon-EX o Entrenador.
- `DamageCounterComponent`: Superposición circular de color rojo con números. Calcula su escala física según la cantidad de contadores (un contador por cada 10 puntos de daño).
- `StatusIndicatorComponent`: Iconos de alertas e inmunidades estéticas para el Pokémon Activo (ej: Calavera morada para `POISONED`, Alarma naranja para `CONFUSED`).
- `GameLogComponent`: Consola inmutable inferior que itera sobre la lista `eventLog` del snapshot traduciendo los strings técnicos en strings legibles para el usuario en tiempo real.

## Configuración del Entorno (`app.config.ts`)
Para asegurar la conectividad con el servidor Spring Boot y el correcto arranque asíncrono, el archivo de configuración base global se define de la siguiente manera:

```typescript
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { appRoutes } from './app.routes';
import { errorInterceptor } from './core/interceptors/error.interceptor';export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(appRoutes, withComponentInputBinding()), // Habilita inyección de Route Params directamente como inputs()
    provideHttpClient(withInterceptors([errorInterceptor])) // Registra interceptor global de sanitización de errores HTTP
  ]
};
```


## Routing (Semana 5)

```typescript
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Públicas
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login-page')
      .then(m => m.LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/pages/register-page')
      .then(m => m.RegisterPage)
  },
  // Protegidas
  {
    path: 'pokedex',
    canActivate: [authGuard],
    loadComponent: () => import('./features/pokedex/pages/pokedex-page')
      .then(m => m.PokedexPage)
  },
  {
    path: 'deck-builder',
    canActivate: [authGuard],
    loadComponent: () => import('./features/deck-builder/pages/deck-builder-page')
      .then(m => m.DeckBuilderPage)
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lobby/pages/lobby-page')
      .then(m => m.LobbyPage)
  },
  {
    path: 'game/:matchId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/pages/game-page')
      .then(m => m.GamePage)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/pages/profile-page')
      .then(m => m.ProfilePage)
  },
  {
    path: '',
    redirectTo: 'pokedex',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: ''
  }
];
```

## Gestión de Estado (Signals)

### GameStateService

```typescript
@Injectable({ providedIn: 'root' })
export class GameStateService {
  private board = signal<GameBoard | null>(null);
  private turn = signal<TurnContext | null>(null);
  private phase = signal<GamePhase | null>(null);
  private selectedCard = signal<GameCard | null>(null);
  private notifications = signal<Notification[]>([]);

  readonly boardState = computed(() => this.board());
  readonly currentTurn = computed(() => this.turn());
  readonly currentPhase = computed(() => this.phase());
  readonly isMyTurn = computed(() => this.turn()?.currentPlayerId === this.myPlayerId());

  updateState(snapshot: MatchSnapshot): void { ... }
  selectCard(card: GameCard | null): void { ... }
  addNotification(message: string): void { ... }
}
```

### GameWebsocketService

```typescript
@Injectable({ providedIn: 'root' })
export class GameWebsocketService {
  private connectionStatus = signal<'connected' | 'disconnected' | 'connecting'>('disconnected');
  private lastMessage = signal<GameStateUpdateMessage | null>(null);

  readonly status = computed(() => this.connectionStatus());

  connect(matchId: string): void { ... }
  disconnect(): void { ... }
  sendAction(action: GameActionMessage): void { ... }
}
```

### CardService

```typescript
@Injectable({ providedIn: 'root' })
export class CardService {
  private cards = signal<CardModel[]>([]);
  private loading = signal(false);
  private filters = signal<CardFilterDTO>({});

  readonly filteredCards = computed(() => { ... });

  loadCards(filters?: CardFilterDTO): void { ... }
  searchCards(name: string): void { ... }
  syncCards(): void { ... }
}
```

### DeckService

```typescript
@Injectable({ providedIn: 'root' })
export class DeckService {
  private decks = signal<DeckModel[]>([]);
  private currentDeck = signal<DeckModel | null>(null);
  private validation = signal<DeckValidationResultDTO | null>(null);

  readonly isValid = computed(() => this.validation()?.isValid ?? false);
  readonly validationErrors = computed(() => this.validation()?.errors ?? []);

  loadDecks(): void { ... }
  createDeck(name: string, cards: DeckCardEntryDTO[]): void { ... }
  updateDeck(id: string, cards: DeckCardEntryDTO[]): void { ... }
  deleteDeck(id: string): void { ... }
  validateDeck(id: string): void { ... }
}
```

### LobbyService

```typescript
@Injectable({ providedIn: 'root' })
export class LobbyService {
  private matches = signal<MatchModel[]>([]);
  private currentMatch = signal<MatchModel | null>(null);

  readonly availableMatches = computed(() => this.matches().filter(m => m.status === 'WAITING'));

  loadMatches(): void { ... }
  createMatch(deckId: string): void { ... }
  joinMatch(matchId: string): void { ... }
  leaveMatch(matchId: string): void { ... }
}
```

## Componentes Compartidos

| Componente | Uso |
|---|---|
| `CardComponent` | Visualización de carta en cualquier contexto |
| `CardStackComponent` | Pila de cartas (mazo, descarte) |
| `DamageCounterComponent` | Contador de daño visual |
| `EnergyBadgeComponent` | Badge de tipo de energía |
| `GameLogComponent` | Log de acciones del juego |
| `StatusIndicatorComponent` | Indicador de condición especial |

## UI Components

| Componente | Uso |
|---|---|
| `ButtonComponent` | Botón reutilizable con variantes |
| `ModalComponent` | Modal genérico |
| `LoadingSpinnerComponent` | Indicador de carga |

## Pipes

| Pipe | Transformación |
|---|---|
| `HpDisplayPipe` | Formatea HP para visualización |
| `EnergyTypePipe` | Convierte tipo de energía a color/icono |

## Directivas

| Directiva | Propósito |
|---|---|
| `DraggableDirective` | Habilitar drag & drop en elementos |

## Configuración Requerida

### app.config.ts (Semana 5)

Agregar `jwtInterceptor` para adjuntar el token JWT automáticamente:

```typescript
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor, errorInterceptor]))
  ]
};
```
