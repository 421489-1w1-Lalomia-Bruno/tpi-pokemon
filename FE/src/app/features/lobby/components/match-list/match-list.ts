import { ChangeDetectionStrategy, Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { LobbyService } from '../../services/lobby.service';
import { DeckService } from '../../../deck-builder/services/deck.service';
import { MatchModel } from '../../../../shared/models/match.model';
import { DeckModel } from '../../../../shared/models/deck.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';
import { TimeRemainingPipe } from '../../../../shared/pipes/time-remaining.pipe';

@Component({
  selector: 'app-match-list',
  imports: [ButtonComponent, LoadingSpinnerComponent, TimeRemainingPipe],
  templateUrl: './match-list.html',
  styleUrl: './match-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MatchListComponent implements OnInit, OnDestroy {
  private readonly lobbyService = inject(LobbyService);
  private readonly deckService = inject(DeckService);
  private readonly router = inject(Router);

  readonly availableMatches = this.lobbyService.availableMatches;
  readonly myMatches = this.lobbyService.myMatches;
  readonly loading = this.lobbyService.loading;
  readonly myLoading = this.lobbyService.myLoading;
  readonly errorMessage = signal<string | null>(null);
  readonly joining = signal(false);
  readonly cancelling = signal(false);
  readonly rejoiningId = signal<string | null>(null);
  readonly leavingId = signal<string | null>(null);

  readonly playerDecks = signal<DeckModel[]>([]);
  readonly equippedDeckId = signal<string | null>(null);
  readonly deckLoading = signal(false);
  readonly timerTick = signal(0);

  private pollInterval?: ReturnType<typeof setInterval>;
  private timerSubscription?: Subscription;

  readonly playerId: string = localStorage.getItem('playerId') ?? 'anonymous';

  ngOnInit(): void {
    this.lobbyService.getMatches();
    this.lobbyService.getMyMatches();
    this.pollInterval = setInterval(() => {
      this.lobbyService.getMatches();
      this.lobbyService.getMyMatches();
    }, 3000);
    this.timerSubscription = interval(1000).subscribe(() => this.timerTick.update(t => t + 1));
    this.loadDecks();
    this.restoreEquippedDeck();
  }

  ngOnDestroy(): void {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
    this.timerSubscription?.unsubscribe();
  }

  refresh(): void {
    this.lobbyService.getMatches();
  }

  private loadDecks(): void {
    this.deckLoading.set(true);
    this.deckService.getDecks(this.playerId).subscribe({
      next: (decks: DeckModel[]) => {
        this.playerDecks.set(decks.filter((d: DeckModel) => d.isValid));
        this.deckLoading.set(false);
      },
      error: () => this.deckLoading.set(false),
    });
  }

  private restoreEquippedDeck(): void {
    const equipped = localStorage.getItem(`deck-for-${this.playerId}`);
    if (equipped) this.equippedDeckId.set(equipped);
  }

  onEquipDeck(deckId: string): void {
    const key = `deck-for-${this.playerId}`;
    if (this.equippedDeckId() === deckId) {
      localStorage.removeItem(key);
      this.equippedDeckId.set(null);
    } else {
      localStorage.setItem(key, deckId);
      this.equippedDeckId.set(deckId);
    }
    this.errorMessage.set(null);
  }

  getInitials(id: string | null): string {
    if (!id) return '?';
    const clean = id.replace(/[^a-zA-Z0-9]/g, '');
    return (clean.slice(0, 2) || '?').toUpperCase();
  }

  onJoin(matchId: string): void {
    const deckId = this.equippedDeckId();
    if (!deckId) {
      this.errorMessage.set('Seleccioná un mazo para poder unirte a una partida.');
      return;
    }

    const deckExists = this.playerDecks().some(d => d.id === deckId);
    if (!deckExists) {
      localStorage.removeItem(`deck-for-${this.playerId}`);
      this.equippedDeckId.set(null);
      this.errorMessage.set('El mazo seleccionado ya no existe. Seleccioná otro.');
      return;
    }

    this.errorMessage.set(null);
    this.joining.set(true);

    this.lobbyService.getMatch(matchId).subscribe({
      next: () => {
        this.lobbyService.joinMatch(matchId, deckId).subscribe({
          next: (match: MatchModel) => this.router.navigate(['/game', match.id]),
          error: (err: HttpErrorResponse) => {
            const msg = err.error?.message ?? '';
            if (err.status === 404) {
              this.errorMessage.set('La partida ya no está disponible.');
              this.lobbyService.getMatches();
              this.lobbyService.getMyMatches();
            } else if (err.status === 409 && msg.includes('already full')) {
              this.errorMessage.set('La partida ya está llena.');
            } else if (err.status === 409) {
              this.errorMessage.set('No se pudo unir a la partida. ' + msg);
            } else {
              this.errorMessage.set('Error inesperado al unirse a la partida.');
            }
            this.joining.set(false);
          }
        });
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.errorMessage.set('La partida ya no está disponible.');
          this.lobbyService.getMatches();
          this.lobbyService.getMyMatches();
        } else {
          this.errorMessage.set('Error al verificar la partida.');
        }
        this.joining.set(false);
      }
    });
  }

  onCancel(matchId: string): void {
    this.cancelling.set(true);
    this.lobbyService.cancelMatch(matchId).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.lobbyService.getMatches();
        this.lobbyService.getMyMatches();
      },
      error: () => {
        this.cancelling.set(false);
        this.errorMessage.set('No se pudo cancelar la partida.');
      }
    });
  }

  onLeave(matchId: string): void {
    this.leavingId.set(matchId);
    this.lobbyService.leaveMatch(matchId).subscribe({
      next: () => {
        this.leavingId.set(null);
        this.lobbyService.getMatches();
        this.lobbyService.getMyMatches();
      },
      error: () => {
        this.leavingId.set(null);
        this.errorMessage.set('No se pudo abandonar la partida.');
      }
    });
  }

  onRejoin(matchId: string): void {
    this.rejoiningId.set(matchId);
    this.router.navigate(['/game', matchId]);
  }

  isActiveMatch(match: MatchModel): boolean {
    return match.status === 'SETUP' || match.status === 'ACTIVE' || (match.status === 'WAITING' && match.player2Id !== null);
  }

  getDisconnectCountdown(match: MatchModel): number {
    this.timerTick();
    if (!match.abandonmentAtMs) return 0;
    return Math.max(0, match.abandonmentAtMs - Date.now());
  }

  opponentLabel(match: MatchModel): string {
    if (this.playerId === match.player1Id) {
      return match.player2Username ?? match.player2Id ?? 'Esperando…';
    }
    return match.player1Username ?? match.player1Id;
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'WAITING': return 'Esperando';
      case 'SETUP': return 'Preparando';
      case 'ACTIVE': return 'En curso';
      default: return status;
    }
  }
}
