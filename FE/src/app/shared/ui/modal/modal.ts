import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  effect,
  ElementRef,
  inject,
  HostListener,
  OnDestroy,
} from '@angular/core';

export type ModalSize = 'sm' | 'md' | 'lg' | 'fullscreen';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.html',
  styleUrl: './modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'role': 'dialog',
    'aria-modal': 'true',
    '[attr.aria-label]': 'title()',
  },
})
export class ModalComponent implements OnDestroy {
  show = input(false);
  title = input('');
  size = input<ModalSize>('md');
  closeOnBackdrop = input(true);
  close = output<void>();

  private el = inject(ElementRef);
  private focusTimeout: ReturnType<typeof setTimeout> | null = null;
  private previousActiveElement: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.show()) {
        document.body.style.overflow = 'hidden';
        this.previousActiveElement = document.activeElement as HTMLElement;
        if (this.focusTimeout !== null) clearTimeout(this.focusTimeout);
        this.focusTimeout = setTimeout(() => {
          const firstFocusable = this.el.nativeElement.querySelector(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
          ) as HTMLElement | null;
          firstFocusable?.focus();
        }, 100);
      } else {
        document.body.style.overflow = '';
      }
    });
  }

  ngOnDestroy(): void {
    if (this.focusTimeout !== null) clearTimeout(this.focusTimeout);
    document.body.style.overflow = '';
    this.previousActiveElement?.focus();
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.show()) this.close.emit();
  }

  @HostListener('keydown', ['$event'])
  onKeydown(event: KeyboardEvent) {
    if (event.key !== 'Tab' || !this.show()) return;
    const focusable = this.el.nativeElement.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    ) as NodeListOf<HTMLElement>;
    if (focusable.length === 0) return;
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  onBackdropClick(event: MouseEvent) {
    if (event.target === event.currentTarget && this.closeOnBackdrop()) {
      this.close.emit();
    }
  }
}
