import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'timeRemaining', standalone: true })
export class TimeRemainingPipe implements PipeTransform {
  transform(ms: number): string {
    if (ms == null || isNaN(ms) || ms < 0) {
      return '0:00';
    }
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
