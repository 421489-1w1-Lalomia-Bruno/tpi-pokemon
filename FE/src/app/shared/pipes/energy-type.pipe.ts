import { Pipe, PipeTransform } from '@angular/core';

const ENERGY_CSS: Record<string, string> = {
  FIRE: 'fire',
  WATER: 'water',
  GRASS: 'grass',
  LIGHTNING: 'lightning',
  PSYCHIC: 'psychic',
  FIGHTING: 'fighting',
  DARKNESS: 'darkness',
  METAL: 'metal',
  FAIRY: 'fairy',
  DRAGON: 'dragon',
  COLORLESS: 'colorless',
};

const ENERGY_LABEL: Record<string, string> = {
  FIRE: 'Fuego',
  WATER: 'Agua',
  GRASS: 'Planta',
  LIGHTNING: 'Rayo',
  PSYCHIC: 'Psíquico',
  FIGHTING: 'Lucha',
  DARKNESS: 'Oscuridad',
  METAL: 'Metal',
  FAIRY: 'Hada',
  DRAGON: 'Dragón',
  COLORLESS: 'Incoloro',
};

@Pipe({ name: 'energyType' })
export class EnergyTypePipe implements PipeTransform {
  transform(value: unknown, mode?: 'cssClass' | 'label'): string {
    const key = String(value ?? '');
    if (mode === 'label') return ENERGY_LABEL[key] ?? key;
    return ENERGY_CSS[key] ?? 'colorless';
  }
}
