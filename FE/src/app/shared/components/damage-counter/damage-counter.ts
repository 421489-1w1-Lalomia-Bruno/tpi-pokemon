import { Component, ChangeDetectionStrategy, input, computed, signal, effect, OnDestroy } from '@angular/core';

@Component({
  selector: 'app-damage-counter',
  templateUrl: './damage-counter.html',
  styleUrl: './damage-counter.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[attr.data-low]': 'isLow() ? "" : undefined',
    '[attr.data-mid]': 'isMid() ? "" : undefined',
    '[attr.data-damaged]': 'damaged() ? "" : undefined',
  },
})
export class DamageCounterComponent implements OnDestroy {
  currentHp = input.required<number>();
  maxHp = input.required<number>();
  damageCounters = input(0);

  protected damaged = signal(false);
  private previousHp = 0;
  private damageTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    effect(() => {
      const hp = this.currentHp();
      if (this.previousHp > 0 && hp < this.previousHp) {
        this.damaged.set(true);
        if (this.damageTimeout !== null) clearTimeout(this.damageTimeout);
        this.damageTimeout = setTimeout(() => this.damaged.set(false), 700);
      }
      this.previousHp = hp;
    });
  }

  ngOnDestroy(): void {
    if (this.damageTimeout !== null) clearTimeout(this.damageTimeout);
  }

  healthPercent = computed(() => {
    const max = this.maxHp();
    if (max <= 0) return 0;
    return Math.round((this.currentHp() / max) * 100);
  });

  isLow = computed(() => this.healthPercent() <= 25);
  isMid = computed(() => this.healthPercent() > 25 && this.healthPercent() <= 50);

  get damageAmount(): number {
    return this.damageCounters() * 10;
  }
}
