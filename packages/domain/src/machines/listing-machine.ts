import type { ListingStatus } from '@dorja/contracts';

type ListingTransition = {
  from: ListingStatus;
  to: ListingStatus;
  action: string;
};

/**
 * Listing state machine.
 *
 * DRAFT → AUTHORITY_REVIEW_PENDING → ACTIVE
 * ACTIVE → VIEWING_HELD → ACTIVE (resume)
 * ACTIVE/VIEWING_HELD → UNCONFIRMED (pulse expired)
 * UNCONFIRMED → ACTIVE (reconfirmed)
 * ACTIVE → RENTED_OR_SOLD
 * ACTIVE → PAUSED → ACTIVE
 * ACTIVE/VIEWING_HELD/UNCONFIRMED → RESTRICTED
 * any → ARCHIVED
 */
const transitions: ListingTransition[] = [
  // Creation flow
  { from: 'DRAFT', to: 'AUTHORITY_REVIEW_PENDING', action: 'submit_for_review' },
  { from: 'AUTHORITY_REVIEW_PENDING', to: 'ACTIVE', action: 'authority_approved' },
  { from: 'AUTHORITY_REVIEW_PENDING', to: 'DRAFT', action: 'authority_rejected' },

  // Availability
  { from: 'ACTIVE', to: 'VIEWING_HELD', action: 'viewing_scheduled' },
  { from: 'VIEWING_HELD', to: 'ACTIVE', action: 'viewing_completed' },

  // Live pulse
  { from: 'ACTIVE', to: 'UNCONFIRMED', action: 'pulse_expired' },
  { from: 'VIEWING_HELD', to: 'UNCONFIRMED', action: 'pulse_expired' },
  { from: 'UNCONFIRMED', to: 'ACTIVE', action: 'pulse_reconfirmed' },

  // Closure
  { from: 'ACTIVE', to: 'RENTED_OR_SOLD', action: 'mark_closed' },
  { from: 'VIEWING_HELD', to: 'RENTED_OR_SOLD', action: 'mark_closed' },
  { from: 'UNCONFIRMED', to: 'RENTED_OR_SOLD', action: 'mark_closed' },

  // Pause
  { from: 'ACTIVE', to: 'PAUSED', action: 'pause' },
  { from: 'PAUSED', to: 'ACTIVE', action: 'unpause' },

  // Safety restriction
  { from: 'ACTIVE', to: 'RESTRICTED', action: 'restrict' },
  { from: 'VIEWING_HELD', to: 'RESTRICTED', action: 'restrict' },
  { from: 'UNCONFIRMED', to: 'RESTRICTED', action: 'restrict' },
  { from: 'RESTRICTED', to: 'ACTIVE', action: 'unrestrict' },

  // Archive (from any active-like state)
  { from: 'DRAFT', to: 'ARCHIVED', action: 'archive' },
  { from: 'ACTIVE', to: 'ARCHIVED', action: 'archive' },
  { from: 'VIEWING_HELD', to: 'ARCHIVED', action: 'archive' },
  { from: 'UNCONFIRMED', to: 'ARCHIVED', action: 'archive' },
  { from: 'PAUSED', to: 'ARCHIVED', action: 'archive' },
  { from: 'RENTED_OR_SOLD', to: 'ARCHIVED', action: 'archive' },
];

export function canTransition(
  current: ListingStatus,
  action: string,
): { allowed: boolean; next?: ListingStatus } {
  const transition = transitions.find(
    (t) => t.from === current && t.action === action,
  );
  if (!transition) return { allowed: false };
  return { allowed: true, next: transition.to };
}

export function getPublicStatus(status: ListingStatus): string {
  switch (status) {
    case 'ACTIVE':
      return 'AVAILABLE';
    case 'VIEWING_HELD':
      return 'AVAILABLE';
    case 'UNCONFIRMED':
      return 'UNCONFIRMED';
    default:
      return status;
  }
}

export function isPubliclyVisible(status: ListingStatus): boolean {
  return ['ACTIVE', 'VIEWING_HELD', 'UNCONFIRMED'].includes(status);
}
