import { ChangeDetectionStrategy, Component, ElementRef, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { trigger, transition, style, animate, query } from '@angular/animations';
import { NavBarComponent } from './shared/components/nav-bar/nav-bar';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavBarComponent],
  template: `
    <a href="#main-content" class="skip-link" (click)="skipToContent($event)">
      Saltar al contenido principal
    </a>
    <app-nav-bar />
    <main id="main-content" class="app-main" tabindex="-1">
      <router-outlet @routeTransition />
    </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  animations: [
    trigger('routeTransition', [
      transition('* <=> *', [
        query(':enter', [
          style({ opacity: 0, transform: 'translateY(8px)' }),
        ], { optional: true }),
        query(':leave', [
          animate('180ms ease', style({ opacity: 0, transform: 'translateY(-8px)' })),
        ], { optional: true }),
        query(':enter', [
          animate('280ms 50ms ease', style({ opacity: 1, transform: 'translateY(0)' })),
        ], { optional: true }),
      ]),
    ]),
  ],
})
export class App {
  private readonly el = inject(ElementRef);

  skipToContent(event: Event): void {
    event.preventDefault();
    const main = this.el.nativeElement.querySelector('#main-content') as HTMLElement | null;
    main?.focus();
  }
}
