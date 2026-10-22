import { Injectable, inject, OnDestroy, signal } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';
import { GameActionMessage, GameStateUpdateMessage } from '../../shared/models/game.model';
import { WS_URL } from '../tokens/ws-url.token';

const RECONNECT_DELAY_MS = 5000;
const MAX_RECONNECT_ATTEMPTS = 5;

@Injectable({ providedIn: 'root' })
export class WebsocketService implements OnDestroy {
  private readonly wsUrl = inject(WS_URL);
  private client: Client | null = null;
  private readonly subs = new Map<string, StompSubscription>();
  private reconnectAttempts = 0;

  readonly connected = signal(false);
  readonly clientActive = signal(false);
  readonly connecting = signal(false);
  readonly error = signal<string | null>(null);

  private readonly messageSubject = new Subject<GameStateUpdateMessage>();
  readonly messages$: Observable<GameStateUpdateMessage> = this.messageSubject.asObservable();

  connect(token: string): void {
    if (this.client?.active) return;

    this.connecting.set(true);
    this.error.set(null);
    this.reconnectAttempts = 0;

    this.client = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: RECONNECT_DELAY_MS,
      onConnect: () => {
        this.connected.set(true);
        this.clientActive.set(true);
        this.connecting.set(false);
        this.reconnectAttempts = 0;
        this.error.set(null);
      },
      onDisconnect: () => {
        this.connected.set(false);
        this.clientActive.set(false);
      },
      onStompError: (frame) => {
        this.error.set(frame.headers['message'] ?? 'Error de conexión STOMP');
        this.connected.set(false);
        this.clientActive.set(false);
        this.connecting.set(false);
      },
      onWebSocketError: () => {
        // Only count errors during initial connection, not transient errors
        // on established connections (STOMP handles reconnection internally)
        if (!this.connected() && !this.client?.active) {
          this.reconnectAttempts++;
          if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            this.error.set('No se pudo conectar al servidor después de varios intentos.');
            this.disconnect();
          }
        }
      },
    });

    this.client.activate();
  }

  subscribeToMatch(matchId: string): void {
    if (!this.client?.active) return;

    if (this.subs.has(matchId)) return;

    const sub = this.client.subscribe('/user/queue/match', (msg: IMessage) => {
      const parsed = JSON.parse(msg.body) as GameStateUpdateMessage;
      if (parsed.type === 'STATE_UPDATE' && parsed.state?.matchId !== matchId) return;
      this.messageSubject.next(parsed);
    });

    this.subs.set(matchId, sub);

    this.client.publish({
      destination: '/app/game/connect',
      body: JSON.stringify({ matchId }),
    });
  }

  unsubscribeFromMatch(matchId: string): void {
    if (this.client?.active && this.subs.has(matchId)) {
      this.client.publish({
        destination: '/app/game/disconnect',
        body: JSON.stringify({ matchId }),
      });
    }

    this.subs.get(matchId)?.unsubscribe();
    this.subs.delete(matchId);
  }

  sendAction(action: GameActionMessage): void {
    if (!this.client?.active || !this.connected()) {
      console.warn('[WS] sendAction dropped: client not active/connected', action.type);
      return;
    }

    console.log('[WS] Sending action:', action.type, action.matchId, action.payload);
    this.client.publish({
      destination: `/app/game/action`,
      body: JSON.stringify({ ...action, timestamp: new Date().toISOString() }),
    });
  }

  disconnect(): void {
    this.subs.forEach((sub) => sub.unsubscribe());
    this.subs.clear();
    this.client?.deactivate();
    this.client = null;
    this.connected.set(false);
    this.clientActive.set(false);
    this.connecting.set(false);
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.messageSubject.complete();
  }
}
