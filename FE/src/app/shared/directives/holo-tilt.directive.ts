import { Directive, ElementRef, NgZone, OnDestroy, inject, input } from '@angular/core';

/**
 * Efecto "carta premium" estilo Pokémon TCG Pocket:
 *  - Inclinación 3D (tilt) siguiendo el puntero.
 *  - Expone variables CSS para que el componente dibuje brillos/holo dinámicos:
 *      --mx / --my          posición del puntero dentro de la carta (0–100%)
 *      --rx / --ry          rotación 3D resultante (deg)
 *      --pointer-d          distancia normalizada al centro (0 centro → 1 borde)
 *      --interacting        1 mientras el puntero está encima, 0 al salir
 *
 * Los listeners corren FUERA de la zona de Angular (no disparan change
 * detection en cada mousemove) y actualizan estilos vía requestAnimationFrame.
 * Respeta `prefers-reduced-motion`.
 */
@Directive({
  selector: '[holoTilt]', // eslint-disable-line @angular-eslint/directive-selector
})
export class HoloTiltDirective implements OnDestroy {
  private readonly host = inject(ElementRef<HTMLElement>).nativeElement;
  private readonly zone = inject(NgZone);

  /** Inclinación máxima en grados. */
  readonly maxTilt = input(13, { alias: 'holoTilt' });
  /** Permite desactivar el efecto puntualmente. */
  readonly disabled = input(false, { alias: 'holoTiltDisabled' });

  private rafId = 0;
  private lastEvent: PointerEvent | null = null;
  private readonly reduceMotion =
    typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;

  constructor() {
    if (this.reduceMotion) return;
    this.zone.runOutsideAngular(() => {
      this.host.addEventListener('pointerenter', this.onEnter);
      this.host.addEventListener('pointermove', this.onMove);
      this.host.addEventListener('pointerleave', this.onLeave);
    });
  }

  private readonly onEnter = (): void => {
    if (this.disabled()) return;
    this.host.style.setProperty('--interacting', '1');
    this.host.classList.add('is-interacting');
  };

  private readonly onMove = (event: PointerEvent): void => {
    if (this.disabled()) return;
    this.lastEvent = event;
    if (this.rafId) return;
    this.rafId = requestAnimationFrame(this.apply);
  };

  private readonly apply = (): void => {
    this.rafId = 0;
    const event = this.lastEvent;
    if (!event) return;

    const rect = this.host.getBoundingClientRect();
    const px = clamp((event.clientX - rect.left) / rect.width);
    const py = clamp((event.clientY - rect.top) / rect.height);

    const max = this.maxTilt();
    const ry = (px - 0.5) * 2 * max; // rotateY: izquierda/derecha
    const rx = -(py - 0.5) * 2 * max; // rotateX: arriba/abajo
    const dist = clamp(Math.hypot(px - 0.5, py - 0.5) / 0.7071);

    const s = this.host.style;
    s.setProperty('--mx', `${(px * 100).toFixed(2)}%`);
    s.setProperty('--my', `${(py * 100).toFixed(2)}%`);
    s.setProperty('--rx', `${rx.toFixed(2)}deg`);
    s.setProperty('--ry', `${ry.toFixed(2)}deg`);
    s.setProperty('--pointer-d', dist.toFixed(3));
  };

  private readonly onLeave = (): void => {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
      this.rafId = 0;
    }
    this.lastEvent = null;
    this.host.classList.remove('is-interacting');
    const s = this.host.style;
    s.setProperty('--interacting', '0');
    s.setProperty('--mx', '50%');
    s.setProperty('--my', '50%');
    s.setProperty('--rx', '0deg');
    s.setProperty('--ry', '0deg');
    s.setProperty('--pointer-d', '0');
  };

  ngOnDestroy(): void {
    this.host.removeEventListener('pointerenter', this.onEnter);
    this.host.removeEventListener('pointermove', this.onMove);
    this.host.removeEventListener('pointerleave', this.onLeave);
    if (this.rafId) cancelAnimationFrame(this.rafId);
  }
}

function clamp(value: number, min = 0, max = 1): number {
  return Math.min(Math.max(value, min), max);
}
