import type { UserRole, IdentityStatus } from '@dorja/contracts';

type PermissionCheck = {
  allowed: boolean;
  reason?: string;
};

/**
 * Permission rules (spec §3.2):
 * All permission checks must happen server-side.
 * 1. Only OWNER, AGENT, or REPRESENTATIVE with listing authority may edit listing
 * 2. Only assigned lister/representative may create capture sessions
 * 3. Only IDENTITY_CONFIRMED seeker may request SafeView
 * 4. Listing must have LISTING_AUTHORITY_REVIEWED before public publishing
 * 5. Exact address returned only to confirmed appointment participants during reveal window
 * 6. Viewing pass is server-issued, single-use, time-limited
 */
export function canEditListing(
  role: UserRole,
  hasAuthority: boolean,
): PermissionCheck {
  const allowedRoles: UserRole[] = ['OWNER', 'AGENT', 'REPRESENTATIVE'];
  if (!allowedRoles.includes(role)) {
    return { allowed: false, reason: 'Only property owners, agents, or representatives can edit listings.' };
  }
  if (!hasAuthority) {
    return { allowed: false, reason: 'Listing authority has not been verified.' };
  }
  return { allowed: true };
}

export function canCreateCaptureSession(
  role: UserRole,
  isAssignedLister: boolean,
): PermissionCheck {
  const allowedRoles: UserRole[] = ['OWNER', 'AGENT', 'REPRESENTATIVE'];
  if (!allowedRoles.includes(role)) {
    return { allowed: false, reason: 'Only the assigned lister or representative can create capture sessions.' };
  }
  if (!isAssignedLister) {
    return { allowed: false, reason: 'You are not the assigned lister for this property.' };
  }
  return { allowed: true };
}

export function canRequestViewing(
  role: UserRole,
  identityStatus: IdentityStatus,
): PermissionCheck {
  if (role !== 'SEEKER') {
    return { allowed: false, reason: 'Only seekers can request viewings.' };
  }
  if (identityStatus !== 'IDENTITY_CONFIRMED') {
    return { allowed: false, reason: 'Identity confirmation is required before requesting a private viewing.' };
  }
  return { allowed: true };
}

export function canPublishListing(
  authorityReviewed: boolean,
): PermissionCheck {
  if (!authorityReviewed) {
    return { allowed: false, reason: 'Listing must have authority reviewed before public publishing.' };
  }
  return { allowed: true };
}

export function canRevealAddress(
  identityStatus: IdentityStatus,
  isConfirmedParticipant: boolean,
  withinRevealWindow: boolean,
): PermissionCheck {
  if (identityStatus !== 'IDENTITY_CONFIRMED') {
    return { allowed: false, reason: 'Identity confirmation required for address access.' };
  }
  if (!isConfirmedParticipant) {
    return { allowed: false, reason: 'Only confirmed appointment participants can view the exact address.' };
  }
  if (!withinRevealWindow) {
    return { allowed: false, reason: 'Address is only available during the configured reveal window.' };
  }
  return { allowed: true };
}

export function canScanPass(
  scannerRole: UserRole,
  isListingHost: boolean,
): PermissionCheck {
  const allowedRoles: UserRole[] = ['OWNER', 'AGENT', 'REPRESENTATIVE', 'GUARD'];
  if (!allowedRoles.includes(scannerRole)) {
    return { allowed: false, reason: 'Only the host, representative, or building guard can scan passes.' };
  }
  if (!isListingHost) {
    return { allowed: false, reason: 'Scanner is not authorised for this listing.' };
  }
  return { allowed: true };
}

export function canAccessReviewerEndpoint(role: UserRole): PermissionCheck {
  if (role !== 'REVIEWER' && role !== 'ADMIN') {
    return { allowed: false, reason: 'Only reviewers and admins can access this endpoint.' };
  }
  return { allowed: true };
}
