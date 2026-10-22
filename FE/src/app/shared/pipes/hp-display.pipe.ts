import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'hpDisplay' })
export class HpDisplayPipe implements PipeTransform {
  transform(value: unknown): unknown { return value; }
}
