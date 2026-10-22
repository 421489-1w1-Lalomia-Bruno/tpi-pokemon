---
name: websocket-sync
description: Implements WebSocket communication for real-time game state synchronization in the Pokemon TCG project. Use when creating or editing files related to WebSocket, STOMP, real-time communication, game actions, state updates, or the GameWebSocketHandler. Triggers on WebSocket, STOMP, real-time, socket, ws, game action, state sync, lobby, match connection.
---

# WebSocket Sync Skill - Pokemon TCG

You are implementing the real-time communication layer for a Pokemon Trading Card Game. The backend is the ONLY source of truth during gameplay. All game actions go through WebSocket, NOT REST.

## Architecture

### Communication Model

```
CLIENT (Angular)                          SERVER (Spring Boot)
    │                                          │
    │  ─── HTTP REST ──────────────────────►   │  Cards, Decks, Match CRUD
    │                                          │
    │  ─── WebSocket Connect ─────────────►   │  /ws/game
    │                                          │
    │  ─── GameActionMessage ──────────────►   │  All in-game actions
    │                                          │
    │  ◄── GameStateUpdateMessage ──────────    │  State sync, events, errors
    │                                          │
```

### Key Constraint

**During gameplay (after match SETUP phase starts), NO REST API calls are allowed.** All actions go through WebSocket → GameEngine → State persistence → Broadcast.

The lobby (creating/joining matches) and deck builder still use REST. Only the live game uses WebSocket.

## Server-Side Implementation

### WebSocketConfig

Located at: `configs/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/game")
                .setAllowedOriginPatterns("http://localhost:4200")
                .withSockJS();
    }
}
```

Note: `/user` prefix is required for private channel messaging (`/user/{playerId}/queue/match`).

### Security: Auth Channel Interceptor (Phase Approach)

**Phase 1 (Weeks 1-3): X-Player-Id Header**

During initial development, authentication uses a simple `X-Player-Id` header:

```java
@Component
public class PlayerIdChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String playerId = accessor.getFirstNativeHeader("X-Player-Id");
            if (playerId == null || playerId.isBlank()) {
                throw new MessagingException("Missing X-Player-Id header");
            }
            // Assign a simple Principal with the playerId
            accessor.setUser(() -> playerId);
        }
        return message;
    }
}
```

**Phase 2 (Weeks 4-5): JWT Migration**

Replace with `AuthChannelInterceptor` that validates JWT tokens:

```java
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtTokenProvider.validate(token)) {
                throw new MessagingException("Invalid JWT token");
            }
            accessor.setUser(jwtTokenProvider.getAuthentication(token));
        }
        return message;
    }
}
```

Register in `WebSocketConfig`:

```java
@Override
public void configureClientInboundRegistration(ChannelRegistration registration) {
    registration.interceptors(authChannelInterceptor);
}
```

### GameWebSocketHandler

Located at: `websocket/GameWebSocketHandler.java`

Responsible for:
1. Managing connections per match
2. Routing incoming `GameActionMessage` to `GameEngine.processAction()`
3. Broadcasting `GameStateUpdateMessage` to both players in a match via **private channels**
4. Handling reconnection (players receive full state on reconnect)
5. Anti-cheat: cross-identity validation (Principal vs payload playerId)

```java
@Component
public class GameWebSocketHandler {

    private final GameEngine gameEngine;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Key: matchId, Value: Set of session IDs
    private final Map<String, Set<String>> matchSessions = new ConcurrentHashMap<>();

    // Key: sessionId, Value: matchId
    private final Map<String, String> sessionMatch = new ConcurrentHashMap<>();

    // Key: sessionId, Value: playerId (from authenticated Principal)
    private final Map<String, String> sessionPlayer = new ConcurrentHashMap<>();

    public void handleConnect(String sessionId, String matchId, Principal principal) { ... }
    public void handleDisconnect(String sessionId) { ... }
    public void handleAction(String sessionId, GameActionMessage action, Principal principal) { ... }
    public void broadcastToMatch(String matchId, GameStateUpdateMessage message) { ... }
    public void sendToPlayer(String sessionId, GameStateUpdateMessage message) { ... }
}
```

### Event Listener (Async decoupling)

The WebSocket layer subscribes to Spring `ApplicationEventPublisher` events from the game engine:

```java
@Component
public class GameEventListener {

    @EventListener
    @Async
    public void handleGameEvent(GameEvent event) {
        // Translate domain event to GameStateUpdateMessage
        // Broadcast to appropriate match channel
    }
}
```

This keeps the core engine decoupled from the network protocol.

### Message Types

#### Client → Server: `GameActionMessage`

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameActionMessage {
    private String matchId;
    // playerId is NOT included. Extracted from Principal in the_authenticated session.
    private GameActionType type;
    private Map<String, Object> payload;
    private String timestamp;
}
```

Action types:

| Type | Description | Payload |
|---|---|---|
| `READY` | Player ready (setup phase) | `{}` |
| `PLACE_POKEMON` | Place Basic Pokemon on bench | `{ "cardId": "uuid", "zone": "BENCH" }` |
| `SET_ACTIVE` | Set active Pokemon (setup/KO) | `{ "cardId": "uuid" }` |
| `ATTACH_ENERGY` | Attach energy to Pokemon | `{ "energyCardId": "uuid", "targetPokemonId": "uuid" }` |
| `PLAY_TRAINER` | Play trainer card | `{ "cardId": "uuid" }` |
| `EVOLVE` | Evolve Pokemon | `{ "basePokemonId": "uuid", "evolutionCardId": "uuid" }` |
| `RETREAT` | Retreat active Pokemon | `{ "benchPokemonId": "uuid", "energyCardsToDiscard": ["uuid1", "uuid2"] }` |
| `ATTACK` | Declare attack | `{ "attackIndex": 0 }` |
| `USE_ABILITY` | Use Pokemon ability | `{ "abilityName": "Fire Breath" }` |
| `PASS_TURN` | Voluntarily end ACTIONS phase | `{}` |
| `SURRENDER` | Forfeit match (immediate) | `{}` |

**Note:** `DRAW_CARD` is NOT a client action. Drawing is automatic in the DRAW phase — the engine draws a card at the start of each turn without client intervention.

Key differences from previous version:
- `RETREAT` requires `energyCardsToDiscard` array (energy cards discarded for retreat cost)
- `ATTACK` uses `attackIndex` (integer), not `attackName`
- `PASS_TURN` transitions to BETWEEN_TURNS phase
- `SURRENDER` immediately ends the match with the opponent as winner
- `GameActionMessage` does NOT contain `playerId` — it's extracted from the authenticated Principal

#### Server → Client: `GameStateUpdateMessage`

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameStateUpdateMessage {
    private String type;  // STATE_UPDATE, EVENT, ERROR, NOTIFICATION
    private MatchSnapshot state;
    private GameEvent event;
    private String error;
    private String notification;
}
```

Message types:
- `STATE_UPDATE`: Full state sync after an action
- `EVENT`: Game event notification (KO, prize taken, etc.)
- `ERROR`: Action was invalid with reason
- `NOTIFICATION`: Info message (turn changed, phase changed, etc.)

### Security Rules for State Broadcasts

When broadcasting `MatchSnapshot`:
1. **Opponent's hand is NEVER sent.** Only show `handSize` for the opponent.
2. **Deck is never revealed.** Only show `deckCount`.
3. **Prize cards are face-down.** Only show `prizeCardsRemaining` count.
4. **Opponent cannot see face-down cards.**

This means `GameWebSocketHandler` must send DIFFERENT state snapshots to each player via private channels:

```java
public void broadcastToMatch(String matchId, GameStateUpdateMessage baseMessage) {
    MatchSnapshot fullSnapshot = baseMessage.getState();

    // Create player-specific views with masking
    MatchSnapshot p1View = sanitizeForPlayer(fullSnapshot, player1Id);
    MatchSnapshot p2View = sanitizeForPlayer(fullSnapshot, player2Id);

    // Send via private channels
    messagingTemplate.convertAndSendToUser(player1Id, "/queue/match", 
        new GameStateUpdateMessage("STATE_UPDATE", p1View, null, null, null));
    messagingTemplate.convertAndSendToUser(player2Id, "/queue/match", 
        new GameStateUpdateMessage("STATE_UPDATE", p2View, null, null, null));
}

private MatchSnapshot sanitizeForPlayer(MatchSnapshot snapshot, String playerId) {
    // Hide opponent's hand (show only count)
    // Hide deck order (show only count)
    // Hide prize cards content (show only count)
    // Keep own hand visible
    // Keep own prize cards visible when taken
}
```

### Anti-Cheat: Cross-Identity Protection

The `GameActionMessage` does NOT contain a `playerId` field. The server extracts the real user ID from the authenticated `Principal` in the STOMP session:

```java
public void handleAction(String sessionId, GameActionMessage action, Principal principal) {
    String authenticatedPlayerId = principal.getName();
    // playerId is derived from the authenticated session, never from the payload
    // Verify the authenticated player is allowed to act in this match/turn
    // If not allowed: send ERROR response, leave board unchanged
}
```

## Client-Side Implementation

### WebsocketService

```typescript
@Injectable({ providedIn: 'root' })
export class WebsocketService {
  private readonly connectionStatus = signal<'connected' | 'disconnected' | 'connecting'>('disconnected');
  private stompClient: Client | null = null;

  readonly status = computed(() => this.connectionStatus());

  connect(matchId: string, playerId: string): void {
    this.connectionStatus.set('connecting');
    this.stompClient = new Client({
      brokerURL: 'ws://localhost:8080/ws/game',
      connectHeaders: {
        Authorization: `Bearer ${this.getJwtToken()}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connectionStatus.set('connected');
        // Subscribe to PRIVATE channel (player-specific view)
        this.stompClient?.subscribe(`/user/queue/match`, (message) => {
          this.handleMessage(JSON.parse(message.body));
        });
        this.stompClient?.publish({
          destination: `/app/match/${matchId}/connect`,
          body: JSON.stringify({ playerId })
        });
      },
      onDisconnect: () => this.connectionStatus.set('disconnected')
    });
    this.stompClient.activate();
  }

  sendAction(action: GameActionMessage): void {
    this.stompClient?.publish({
      destination: `/app/match/${action.matchId}/action`,
      body: JSON.stringify(action)
    });
  }

  disconnect(): void {
    this.stompClient?.deactivate();
    this.connectionStatus.set('disconnected');
  }

  private getJwtToken(): string {
    // Retrieve from auth service or localStorage
    return '';
  }
}
```

### GameWebsocketService

```typescript
@Injectable({ providedIn: 'root' })
export class GameWebsocketService {
  private readonly websocket = inject(WebsocketService);
  private readonly gameStateService = inject(GameStateService);

  connectToMatch(matchId: string, playerId: string): void {
    this.websocket.connect(matchId, playerId);
  }

  sendAction(type: GameActionType, payload: Record<string, unknown> = {}): void {
    const action: GameActionMessage = {
      matchId: this.matchId(),
      type,
      payload,
      timestamp: new Date().toISOString()
    };
    this.websocket.sendAction(action);
  }

  private handleStateUpdate(message: GameStateUpdateMessage): void {
    switch (message.type) {
      case 'STATE_UPDATE':
        this.gameStateService.updateState(message.state!);
        break;
      case 'EVENT':
        this.gameStateService.addEvent(message.event!);
        break;
      case 'ERROR':
        this.gameStateService.addNotification({ type: 'error', text: message.error! });
        break;
      case 'NOTIFICATION':
        this.gameStateService.addNotification({ type: 'info', text: message.notification! });
        break;
    }
  }
}
```

### Reconnection via REST

If WebSocket reconnects, the client must re-sync state:

```typescript
// After WebSocket reconnection, request current state via REST
// playerId comes from X-Player-Id header (Phase 1) or JWT (Phase 2)
this.http.get<MatchSnapshotModel>(`/api/matches/${matchId}/state`, {
  headers: { 'X-Player-Id': this.playerId() }
}).subscribe(state => this.gameStateService.updateState(state));
```

This `GET /api/matches/{id}/state` endpoint uses the `X-Player-Id` header (Phase 1) for data masking. When JWT is implemented (Phase 2), it will use the Authorization header instead.

## Reconnection Flow

1. Client disconnects (network issue, tab close, etc.)
2. Server keeps match state (persisted after every action as JSONB)
3. Client reconnects via WebSocket with same matchId and JWT
4. Server sends full `MatchSnapshot` to reconnecting player
5. Client rebuilds UI from the snapshot
6. Any actions the player "missed" are already in the snapshot

## STOMP Endpoints

| Endpoint | Direction | Description |
|---|---|---|
| `/ws/game` | Client connects | WebSocket handshake endpoint (with JWT) |
| `/app/match/{matchId}/connect` | Client → Server | Player connects to match |
| `/app/match/{matchId}/action` | Client → Server | Player sends game action |
| `/user/queue/match` | Server → Client | Private channel for player-specific state updates |

Note: Private channels (`/user/{playerId}/queue/match`) ensure each player receives a different masked view of the game state.

## Performance: Action Log Separation

To achieve the <50ms WebSocket response time (RNF-01), the full `actionLog`/`eventLog` is NOT included in every `STATE_UPDATE` message. Instead:

- Events are delivered separately via `EVENT`-type messages
- Clients can request paginated event history via REST if needed
- The `game_events` table stores immutable audit logs for debugging and replay

## Checklist

When implementing WebSocket features:
- [ ] `WebSocketConfig` uses STOMP withSockJS and `/user` prefix for private channels
- [ ] CORS allows `http://localhost:4200`
- [ ] Auth interceptor validates X-Player-Id (Phase 1) / JWT (Phase 2) on STOMP CONNECT frames
- [ ] Game actions are routed through `GameEngine.processAction()`
- [ ] State is sanitized per-player before broadcast (no opponent hand, no deck order)
- [ ] Private channels `/user/queue/match` used for player-specific views
- [ ] Cross-identity protection: playerId from Principal/session, NOT from payload
- [ ] `GameActionMessage` does NOT contain `playerId` field
- [ ] Reconnection: REST endpoint `GET /api/matches/{id}/state` with X-Player-Id header
- [ ] `GameStatePersistenceService` called after every action (atomic JSONB write)
- [ ] Event log NOT included in MatchSnapshot (delivered separately for <50ms target)
- [ ] Frontend uses `@stomp/stompjs` STOMP client
- [ ] Connection status exposed as Angular signal
- [ ] STOMP channel unsubscribed on route exit (no memory leaks)
- [ ] Error messages are user-friendly
- [ ] Match sessions cleaned up on disconnect