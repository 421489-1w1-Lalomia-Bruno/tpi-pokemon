import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { DeckService } from '../../../deck-builder/services/deck.service';
import { UserProfileModel } from '../../../../shared/models/user.model';
import { ButtonComponent } from '../../../../shared/ui/button/button';
import { LoadingSpinnerComponent } from '../../../../shared/ui/loading-spinner/loading-spinner';

@Component({
  selector: 'app-profile-page',
  imports: [RouterLink, DatePipe, ButtonComponent, LoadingSpinnerComponent],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly deckService = inject(DeckService);
  private readonly destroy$ = new Subject<void>();

  readonly profile = signal<UserProfileModel | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly deletingDeckId = signal<string | null>(null);

  readonly gamesPlayed = computed(() => {
    const p = this.profile();
    if (!p) return 0;
    return p.wins + p.losses;
  });

  readonly winPercentage = computed(() => {
    const played = this.gamesPlayed();
    const p = this.profile();
    if (!p || played === 0) return 0;
    return Math.round((p.wins / played) * 100);
  });

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);

    this.auth.getProfile().pipe(takeUntil(this.destroy$)).subscribe({
      next: (p: UserProfileModel) => {
        this.profile.set(p);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Error al cargar el perfil.');
      },
    });
  }

  deleteDeck(deckId: string): void {
    const user = this.auth.currentUser();
    if (!user) return;

    this.deletingDeckId.set(deckId);

    this.deckService.deleteDeck(deckId, user.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.deletingDeckId.set(null);
        this.profile.update((p) => {
          if (!p) return p;
          return { ...p, decks: p.decks.filter((d) => d.id !== deckId) };
        });
      },
      error: () => {
        this.deletingDeckId.set(null);
      },
    });
  }

  genderLabel(): string {
    const g = this.profile()?.gender;
    if (!g) return 'No especificado';
    const labels: Record<string, string> = {
      MALE: 'Masculino',
      FEMALE: 'Femenino',
      OTHER: 'Otro',
    };
    return labels[g] ?? g;
  }

  genderEmoji(): string {
    const g = this.profile()?.gender;
    if (!g) return '?';
    const emojis: Record<string, string> = {
      MALE: '\u2642\uFE0F',
      FEMALE: '\u2640\uFE0F',
      OTHER: '\u26A7\uFE0F',
    };
    return emojis[g] ?? '?';
  }
}
