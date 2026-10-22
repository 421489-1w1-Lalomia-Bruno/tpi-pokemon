export const ICONS = {
  'energy-fire':      'shared/icons/energy-fire.svg',
  'energy-water':     'shared/icons/energy-water.svg',
  'energy-grass':     'shared/icons/energy-grass.svg',
  'energy-lightning': 'shared/icons/energy-lightning.svg',
  'energy-psychic':   'shared/icons/energy-psychic.svg',
  'energy-fighting':  'shared/icons/energy-fighting.svg',
  'energy-darkness':  'shared/icons/energy-darkness.svg',
  'energy-metal':     'shared/icons/energy-metal.svg',
  'energy-fairy':     'shared/icons/energy-fairy.svg',
  'energy-dragon':    'shared/icons/energy-dragon.svg',
  'energy-colorless': 'shared/icons/energy-colorless.svg',
  'status-asleep':    'shared/icons/status-asleep.svg',
  'status-burned':    'shared/icons/status-burned.svg',
  'status-confused':  'shared/icons/status-confused.svg',
  'status-paralyzed': 'shared/icons/status-paralyzed.svg',
  'status-poisoned':  'shared/icons/status-poisoned.svg',
  'action-attack':    'shared/icons/action-attack.svg',
  'action-evolve':    'shared/icons/action-evolve.svg',
  'action-retreat':   'shared/icons/action-retreat.svg',
  'action-energy':    'shared/icons/action-energy.svg',
  'action-pass':      'shared/icons/action-pass.svg',
  'ui-close':         'shared/icons/ui-close.svg',
  'ui-pokeball':      'shared/icons/ui-pokeball.svg',
  'ui-star':          'shared/icons/ui-star.svg',
  'ui-cross-circle':  'shared/icons/ui-cross-circle.svg',
  'ui-draw':          'shared/icons/ui-draw.svg',
  'ui-check':         'shared/icons/ui-check.svg',
  'ui-info':          'shared/icons/ui-info.svg',
  'nav-pokedex':      'shared/icons/nav-pokedex.svg',
  'nav-deck':         'shared/icons/nav-deck.svg',
  'nav-lobby':        'shared/icons/nav-lobby.svg',
} as const;

export type IconName = keyof typeof ICONS;

export function iconPath(name: IconName): string {
  return ICONS[name];
}
