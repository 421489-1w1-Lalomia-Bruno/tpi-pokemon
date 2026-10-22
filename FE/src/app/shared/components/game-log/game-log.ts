import {
  Component,
  ChangeDetectionStrategy,
  input,
  viewChild,
  ElementRef,
  signal,
  effect,
  OnDestroy,
} from '@angular/core';
import { GameEvent } from '../../models/game.model';

@Component({
  selector: 'app-game-log',
  templateUrl: './game-log.html',
  styleUrl: './game-log.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameLogComponent implements OnDestroy {
  entries = input<GameEvent[]>([]);
  myPlayerId = input<string | null>(null);

  readonly collapsed = signal(false);

  private listEnd = viewChild<ElementRef>('listEnd');
  private scrollTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    effect(() => {
      const len = this.entries().length;
      if (len > 0 && !this.collapsed()) {
        this.scheduleScroll(100);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.scrollTimeout !== null) clearTimeout(this.scrollTimeout);
  }

  private scheduleScroll(delay: number): void {
    if (this.scrollTimeout !== null) clearTimeout(this.scrollTimeout);
    this.scrollTimeout = setTimeout(() => this.scrollToBottom(), delay);
  }

  private scrollToBottom() {
    const el = this.listEnd()?.nativeElement;
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  }

  formatTime(timestamp: string): string {
    const date = new Date(timestamp);
    const h = date.getHours().toString().padStart(2, '0');
    const m = date.getMinutes().toString().padStart(2, '0');
    const s = date.getSeconds().toString().padStart(2, '0');
    return `${h}:${m}:${s}`;
  }

  toggleCollapse() {
    this.collapsed.update(v => !v);
    if (!this.collapsed()) {
      this.scheduleScroll(50);
    }
  }

  isDamage(entry: GameEvent): boolean {
    return (entry.value ?? 0) > 0;
  }

  isHeal(entry: GameEvent): boolean {
    return (entry.value ?? 0) < 0;
  }

  hasStructuredDamage(entry: GameEvent): boolean {
    return entry.value != null && !!entry.source && !!entry.target;
  }

  isMyAttack(entry: GameEvent): boolean {
    return entry.playerId === this.myPlayerId();
  }
}
