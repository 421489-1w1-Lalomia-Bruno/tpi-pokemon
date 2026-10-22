/**
 * Convierte el campo `effect` de TrainerCardModel (JSON array de strings)
 * en texto legible.
 *
 * Ejemplo: '["Heal 30 damage from 1 of your Pokemon."]'
 *       → 'Heal 30 damage from 1 of your Pokemon.'
 */
export function parseTrainerEffect(effect: string | null | undefined): string {
  if (!effect) return 'Sin descripción';
  try {
    const parsed = JSON.parse(effect);
    if (Array.isArray(parsed)) {
      return parsed.join('. ').trim() || 'Sin descripción';
    }
    return String(parsed);
  } catch {
    return effect;
  }
}

/**
 * Traducción al español de los efectos de las cartas Entrenador del set `xy1`
 * (el único set sincronizado por defecto). Keyed por nombre de carta porque el
 * nombre es estable, a diferencia del texto en inglés que viene de la API.
 */
const TRAINER_EFFECT_ES: Record<string, string> = {
  'Cassius': 'Baraja 1 de tus Pokémon y todas las cartas unidas a él dentro de tu mazo.',
  'Evosoda':
    'Busca en tu mazo una carta que evolucione a 1 de tus Pokémon y colócala sobre ese Pokémon (cuenta como evolucionar). Luego baraja tu mazo. No puedes usarla en tu primer turno ni sobre un Pokémon jugado este turno.',
  'Fairy Garden':
    'Cada Pokémon con Energía Hada unida (tuyo o de tu rival) no tiene Coste de Retirada. Permanece en juego; descártala si entra otro Estadio.',
  'Great Ball':
    'Mira las 7 cartas superiores de tu mazo. Puedes revelar un Pokémon de entre ellas y ponerlo en tu mano. Baraja el resto en tu mazo.',
  'Hard Charm':
    'Únela a 1 de tus Pokémon que no tenga ya una herramienta. El daño que ese Pokémon reciba de los ataques del rival se reduce en 20 (tras aplicar Debilidad y Resistencia).',
  'Max Revive': 'Pon un Pokémon de tu pila de descarte encima de tu mazo.',
  'Muscle Band':
    'Únela a 1 de tus Pokémon que no tenga ya una herramienta. Los ataques de ese Pokémon hacen 20 puntos de daño más al Pokémon Activo del rival (antes de aplicar Debilidad y Resistencia).',
  'Professor Sycamore': 'Descarta tu mano y roba 7 cartas.',
  "Professor's Letter":
    'Busca en tu mazo hasta 2 cartas de Energía básica, revélalas y ponlas en tu mano. Luego baraja tu mazo.',
  'Red Card': 'Tu rival baraja su mano dentro de su mazo y roba 4 cartas.',
  'Roller Skates': 'Lanza una moneda. Si sale cara, roba 3 cartas.',
  'Shadow Circle':
    'Cada Pokémon con Energía Oscura unida (tuyo o del rival) no tiene Debilidad. Permanece en juego; descártala si entra otro Estadio.',
  'Shauna': 'Baraja tu mano dentro de tu mazo. Luego roba 5 cartas.',
  'Super Potion':
    'Cura 60 puntos de daño de 1 de tus Pokémon. Si lo haces, descarta una Energía unida a ese Pokémon.',
  'Team Flare Grunt': 'Descarta una Energía unida al Pokémon Activo de tu rival.',
};

/**
 * Devuelve el efecto de una carta Entrenador en español. Si la carta no está
 * en el mapa de traducción, cae al texto original parseado (inglés).
 */
export function trainerEffectEs(
  name: string | null | undefined,
  effect: string | null | undefined,
): string {
  if (name && TRAINER_EFFECT_ES[name]) {
    return TRAINER_EFFECT_ES[name];
  }
  return parseTrainerEffect(effect);
}
