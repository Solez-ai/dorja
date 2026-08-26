import type { ViewingStatus } from '@dorja/contracts';

/**
 * Viewing state machine (spec §10.4):
 *
 * REQUESTED → PROPOSED → CONFIRMED → CHECKED_IN → COMPLETED
 * REQUESTED → CANCELLED → EXPIRED
 * CONFIRMED → CANCELLED → EXPIRED → SAFETY_FOLLOW_UP
 */
type ViewingTransition = {
  from: ViewingStatus;
  to: ViewingStatus;
  action: string;
};

const transitions: ViewingTransition[] = [
  { from: 'REQUESTED', to: 'PROPOSED', action: 'propose' },
  { from: 'REQUESTED', to: 'CONFIRMED', action: 'confirm' },
  { from: 'REQUESTED', to: 'CANCELLED', action: 'cancel' },
  { from: 'REQUESTED', to: 'EXPIRED', action: 'expire' },

  { from: 'PROPOSED', to: 'CONFIRMED', action: 'confirm' },
  { from: 'PROPOSED', to: 'CANCELLED', action: 'cancel' },
  { from: 'PROPOSED', to: 'EXPIRED', action: 'expire' },

  { from: 'CONFIRMED', to: 'CHECKED_IN', action: 'check_in' },
  { from: 'CONFIRMED', to: 'CANCELLED', action: 'cancel' },
  { from: 'CONFIRMED', to: 'EXPIRED', action: 'expire' },
  { from: 'CONFIRMED', to: 'SAFETY_FOLLOW_UP', action: 'safety_flag' },

  { from: 'CHECKED_IN', to: 'COMPLETED', action: 'check_out' },
  { from: 'CHECKED_IN', to: 'SAFETY_FOLLOW_UP', action: 'safety_flag' },

  { from: 'SAFETY_FOLLOW_UP', to: 'COMPLETED', action: 'resolve' },
  { from: 'SAFETY_FOLLOW_UP', to: 'CANCELLED', action: 'cancel' },
];

export function canTransitionViewing(
  current: ViewingStatus,
  action: string,
): { allowed: boolean; next?: ViewingStatus } {
  const transition = transitions.find(
    (t) => t.from === current && t.action === action,
  );
  if (!transition) return { allowed: false };
  return { allowed: true, next: transition.to };
}

export function isViewingActive(status: ViewingStatus): boolean {
  return ['REQUESTED', 'PROPOSED', 'CONFIRMED', 'CHECKED_IN'].includes(status);
}
