import { Component, ChangeDetectionStrategy, inject, output, signal, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { DeckModel } from '../../../../shared/models/deck.model';
import { MatchModel } from '../../../../shared/models/match.model';
import { DeckService } from '../../../deck-builder/services/deck.service';
import { LobbyService } from '../../services/lobby.service';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-create-match-dialog',
  imports: [RouterLink, ButtonComponent, LoadingSpinnerComponent],
  templateUrl: './create-match-dialog.html',
  styleUrl: './create-match-dialog.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateMatchDialogComponent implements OnInit, OnDestroy {
  private readonly deckService = inject(DeckService);
  private readonly lobbyService = inject(LobbyService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();
  private navTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly close = output<void>();

  readonly decks = signal<DeckModel[]>([]);
  readonly selectedDeckId = signal<string | null>(null);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly successful = signal(false);

  ngOnInit(): void {
    this.loadDecks();
  }

  ngOnDestroy(): void {
    if (this.navTimeout !== null) clearTimeout(this.navTimeout);
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadDecks(): void {
    const playerId = localStorage.getItem('playerId') ?? 'anonymous';
    this.loading.set(true);
    this.error.set(null);

    this.deckService.getDecks(playerId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (decks: DeckModel[]) => {
        this.decks.set(decks.filter((d: DeckModel) => d.isValid));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus mazos. Intentá de nuevo.');
        this.loading.set(false);
      }
    });
  }

  selectDeck(deckId: string): void {
    this.selectedDeckId.set(deckId);
    this.error.set(null);
  }

  onCreate(): void {
    const deckId = this.selectedDeckId();
    if (!deckId) return;

    this.creating.set(true);
    this.error.set(null);

    this.lobbyService.createMatch(deckId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (match: MatchModel) => {
        this.creating.set(false);
        this.selectedDeckId.set(null);
        this.successful.set(true);
        this.navTimeout = setTimeout(() => this.router.navigate(['/game', match.id]), 1500);
      },
      error: (err: HttpErrorResponse) => {
        this.creating.set(false);
        this.error.set(err.error?.message ?? 'Error al crear la partida. Verificá que tu mazo sea válido.');
      }
    });
  }

  onCancel(): void {
    this.close.emit();
  }
}
