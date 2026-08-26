import type { OfferStatus } from '@dorja/contracts';

/**
 * Offer state machine (spec §10.3):
 *
 * DRAFT → SENT → ACCEPTED → CLOSED
 * SENT → COUNTERED → DECLINED → WITHDRAWN → EXPIRED
 * COUNTERED → ACCEPTED → DECLINED → COUNTERED → EXPIRED
 *
 * Rules:
 * - Only the recipient can counter a live offer
 * - Sender may withdraw until accepted
 * - A newly countered offer invalidates the previous version
 */
type OfferTransition = {
  from: OfferStatus;
  to: OfferStatus;
  action: string;
  actor: 'sender' | 'recipient' | 'system';
};

const transitions: OfferTransition[] = [
  { from: 'DRAFT', to: 'SENT', action: 'send', actor: 'sender' },
  { from: 'SENT', to: 'ACCEPTED', action: 'accept', actor: 'recipient' },
  { from: 'SENT', to: 'COUNTERED', action: 'counter', actor: 'recipient' },
  { from: 'SENT', to: 'DECLINED', action: 'decline', actor: 'recipient' },
  { from: 'SENT', to: 'WITHDRAWN', action: 'withdraw', actor: 'sender' },
  { from: 'SENT', to: 'EXPIRED', action: 'expire', actor: 'system' },
  { from: 'COUNTERED', to: 'ACCEPTED', action: 'accept', actor: 'recipient' },
  { from: 'COUNTERED', to: 'DECLINED', action: 'decline', actor: 'recipient' },
  { from: 'COUNTERED', to: 'EXPIRED', action: 'expire', actor: 'system' },
];

export function canTransitionOffer(
  current: OfferStatus,
  action: string,
  actorRole: 'sender' | 'recipient' | 'system',
): { allowed: boolean; next?: OfferStatus } {
  const transition = transitions.find(
    (t) => t.from === current && t.action === action && t.actor === actorRole,
  );
  if (!transition) return { allowed: false };
  return { allowed: true, next: transition.to };
}

export function isOfferLive(status: OfferStatus): boolean {
  return ['SENT', 'COUNTERED'].includes(status);
}
