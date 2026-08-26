// --- User & Identity ---
export const UserRole = {
  SEEKER: 'SEEKER',
  OWNER: 'OWNER',
  AGENT: 'AGENT',
  REPRESENTATIVE: 'REPRESENTATIVE',
  GUARD: 'GUARD',
  REVIEWER: 'REVIEWER',
  ADMIN: 'ADMIN',
} as const;
export type UserRole = (typeof UserRole)[keyof typeof UserRole];

export const IdentityStatus = {
  UNVERIFIED: 'UNVERIFIED',
  PHONE_CONFIRMED: 'PHONE_CONFIRMED',
  IDENTITY_PENDING: 'IDENTITY_PENDING',
  IDENTITY_CONFIRMED: 'IDENTITY_CONFIRMED',
  IDENTITY_REJECTED: 'IDENTITY_REJECTED',
  IDENTITY_EXPIRED: 'IDENTITY_EXPIRED',
} as const;
export type IdentityStatus = (typeof IdentityStatus)[keyof typeof IdentityStatus];

// --- Listing ---
export const ListingIntent = {
  RENT: 'RENT',
  SALE: 'SALE',
} as const;
export type ListingIntent = (typeof ListingIntent)[keyof typeof ListingIntent];

export const PropertyType = {
  APARTMENT: 'APARTMENT',
  HOUSE: 'HOUSE',
  ROOM: 'ROOM',
  SUBLET: 'SUBLET',
  HOSTEL_SEAT: 'HOSTEL_SEAT',
  OFFICE: 'OFFICE',
  SHOP: 'SHOP',
  LAND: 'LAND',
} as const;
export type PropertyType = (typeof PropertyType)[keyof typeof PropertyType];

export const ListingStatus = {
  DRAFT: 'DRAFT',
  AUTHORITY_REVIEW_PENDING: 'AUTHORITY_REVIEW_PENDING',
  ACTIVE: 'ACTIVE',
  VIEWING_HELD: 'VIEWING_HELD',
  UNCONFIRMED: 'UNCONFIRMED',
  RENTED_OR_SOLD: 'RENTED_OR_SOLD',
  PAUSED: 'PAUSED',
  RESTRICTED: 'RESTRICTED',
  ARCHIVED: 'ARCHIVED',
} as const;
export type ListingStatus = (typeof ListingStatus)[keyof typeof ListingStatus];

export const AuthorityStatus = {
  NOT_STARTED: 'NOT_STARTED',
  PENDING: 'PENDING',
  REVIEWED: 'REVIEWED',
  REJECTED: 'REJECTED',
  EXPIRED: 'EXPIRED',
} as const;
export type AuthorityStatus = (typeof AuthorityStatus)[keyof typeof AuthorityStatus];

export const AuthorityRole = {
  OWNER: 'OWNER',
  AUTHORISED_AGENT: 'AUTHORISED_AGENT',
  BUILDING_REPRESENTATIVE: 'BUILDING_REPRESENTATIVE',
} as const;
export type AuthorityRole = (typeof AuthorityRole)[keyof typeof AuthorityRole];

// --- Capture ---
export const CaptureStatus = {
  IN_PROGRESS: 'IN_PROGRESS',
  UPLOADING: 'UPLOADING',
  PROCESSING: 'PROCESSING',
  NEEDS_RETAKE: 'NEEDS_RETAKE',
  READY_FOR_REVIEW: 'READY_FOR_REVIEW',
  PUBLISHED_SELLER_CAPTURED: 'PUBLISHED_SELLER_CAPTURED',
  PUBLISHED_AGENT_VERIFIED: 'PUBLISHED_AGENT_VERIFIED',
  EXPIRED: 'EXPIRED',
  ABANDONED: 'ABANDONED',
} as const;
export type CaptureStatus = (typeof CaptureStatus)[keyof typeof CaptureStatus];

export const RoomType = {
  ENTRY: 'ENTRY',
  LIVING_ROOM: 'LIVING_ROOM',
  DINING_ROOM: 'DINING_ROOM',
  BEDROOM: 'BEDROOM',
  KITCHEN: 'KITCHEN',
  BATHROOM: 'BATHROOM',
  BALCONY: 'BALCONY',
  UTILITY: 'UTILITY',
  PARKING: 'PARKING',
  OTHER: 'OTHER',
} as const;
export type RoomType = (typeof RoomType)[keyof typeof RoomType];

export const RealityReviewLevel = {
  INCOMPLETE: 'INCOMPLETE',
  SELLER_CAPTURED: 'SELLER_CAPTURED',
  AGENT_VERIFIED: 'AGENT_VERIFIED',
  EXPIRED: 'EXPIRED',
} as const;
export type RealityReviewLevel = (typeof RealityReviewLevel)[keyof typeof RealityReviewLevel];

export const MediaQualityStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  RETAKE_SUGGESTED: 'RETAKE_SUGGESTED',
  REJECTED: 'REJECTED',
} as const;
export type MediaQualityStatus = (typeof MediaQualityStatus)[keyof typeof MediaQualityStatus];

export const MediaSourceType = {
  HOLD_TO_CAPTURE: 'HOLD_TO_CAPTURE',
  GUIDED_SEQUENCE: 'GUIDED_SEQUENCE',
  IMPORTED_PANORAMA: 'IMPORTED_PANORAMA',
  PRO_SPATIAL_SCAN: 'PRO_SPATIAL_SCAN',
  PROOF_REQUEST: 'PROOF_REQUEST',
} as const;
export type MediaSourceType = (typeof MediaSourceType)[keyof typeof MediaSourceType];

// --- Verification ---
export const VerificationProviderType = {
  MANUAL_REVIEW: 'MANUAL_REVIEW',
  AUTHORISED_NID_PROVIDER: 'AUTHORISED_NID_PROVIDER',
  DISABLED: 'DISABLED',
} as const;
export type VerificationProviderType = (typeof VerificationProviderType)[keyof typeof VerificationProviderType];

export const VerificationReviewStatus = {
  NOT_STARTED: 'NOT_STARTED',
  PENDING: 'PENDING',
  CONFIRMED: 'CONFIRMED',
  REJECTED: 'REJECTED',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED',
} as const;
export type VerificationReviewStatus = (typeof VerificationReviewStatus)[keyof typeof VerificationReviewStatus];

export const EvidenceKind = {
  IDENTITY_DOCUMENT: 'IDENTITY_DOCUMENT',
  SELFIE_OR_LIVENESS_RESULT: 'SELFIE_OR_LIVENESS_RESULT',
  PROPERTY_AUTHORITY_DOCUMENT: 'PROPERTY_AUTHORITY_DOCUMENT',
  OWNER_AUTHORISATION: 'OWNER_AUTHORISATION',
  AGENCY_BUSINESS_DOCUMENT: 'AGENCY_BUSINESS_DOCUMENT',
  BUILDING_MANAGER_CONFIRMATION: 'BUILDING_MANAGER_CONFIRMATION',
  OPTIONAL_PCC: 'OPTIONAL_PCC',
} as const;
export type EvidenceKind = (typeof EvidenceKind)[keyof typeof EvidenceKind];

export const EvidenceReviewStatus = {
  UPLOADED: 'UPLOADED',
  UNDER_REVIEW: 'UNDER_REVIEW',
  ACCEPTED: 'ACCEPTED',
  REJECTED: 'REJECTED',
  EXPIRED: 'EXPIRED',
  RETENTION_PURGED: 'RETENTION_PURGED',
} as const;
export type EvidenceReviewStatus = (typeof EvidenceReviewStatus)[keyof typeof EvidenceReviewStatus];

// --- Offers ---
export const OfferStatus = {
  DRAFT: 'DRAFT',
  SENT: 'SENT',
  COUNTERED: 'COUNTERED',
  ACCEPTED: 'ACCEPTED',
  DECLINED: 'DECLINED',
  EXPIRED: 'EXPIRED',
  WITHDRAWN: 'WITHDRAWN',
} as const;
export type OfferStatus = (typeof OfferStatus)[keyof typeof OfferStatus];

// --- Viewing ---
export const ViewingStatus = {
  REQUESTED: 'REQUESTED',
  PROPOSED: 'PROPOSED',
  CONFIRMED: 'CONFIRMED',
  CANCELLED: 'CANCELLED',
  EXPIRED: 'EXPIRED',
  CHECKED_IN: 'CHECKED_IN',
  COMPLETED: 'COMPLETED',
  SAFETY_FOLLOW_UP: 'SAFETY_FOLLOW_UP',
} as const;
export type ViewingStatus = (typeof ViewingStatus)[keyof typeof ViewingStatus];

export const ViewingPassStatus = {
  ISSUED: 'ISSUED',
  VIEWED: 'VIEWED',
  CHECKED_IN: 'CHECKED_IN',
  INVALIDATED: 'INVALIDATED',
  EXPIRED: 'EXPIRED',
} as const;
export type ViewingPassStatus = (typeof ViewingPassStatus)[keyof typeof ViewingPassStatus];

// --- Messaging ---
export const ConversationStatus = {
  ACTIVE: 'ACTIVE',
  BLOCKED: 'BLOCKED',
  ARCHIVED: 'ARCHIVED',
} as const;
export type ConversationStatus = (typeof ConversationStatus)[keyof typeof ConversationStatus];

export const MessageKind = {
  TEXT: 'TEXT',
  SYSTEM_NOTICE: 'SYSTEM_NOTICE',
  PROOF_REQUEST: 'PROOF_REQUEST',
  PROOF_RESPONSE: 'PROOF_RESPONSE',
  OFFER_CARD: 'OFFER_CARD',
  VIEWING_CARD: 'VIEWING_CARD',
} as const;
export type MessageKind = (typeof MessageKind)[keyof typeof MessageKind];

// --- Safety ---
export const SafetyEventType = {
  ADDRESS_REVEALED: 'ADDRESS_REVEALED',
  PASS_VIEWED: 'PASS_VIEWED',
  PASS_SCANNED: 'PASS_SCANNED',
  SEEKER_CHECKED_IN: 'SEEKER_CHECKED_IN',
  HOST_CHECKED_IN: 'HOST_CHECKED_IN',
  SEEKER_CHECKED_OUT: 'SEEKER_CHECKED_OUT',
  HOST_CHECKED_OUT: 'HOST_CHECKED_OUT',
  CHECKOUT_REMINDER_SENT: 'CHECKOUT_REMINDER_SENT',
  TRUSTED_CONTACT_ALERTED: 'TRUSTED_CONTACT_ALERTED',
  SAFETY_CONCERN_REPORTED: 'SAFETY_CONCERN_REPORTED',
  APPOINTMENT_CANCELLED: 'APPOINTMENT_CANCELLED',
} as const;
export type SafetyEventType = (typeof SafetyEventType)[keyof typeof SafetyEventType];

export const SafetyReportCategory = {
  SUSPICIOUS_BEHAVIOUR: 'SUSPICIOUS_BEHAVIOUR',
  FALSE_LISTING: 'FALSE_LISTING',
  HARASSMENT: 'HARASSMENT',
  ADDRESS_MISUSE: 'ADDRESS_MISUSE',
  NO_SHOW: 'NO_SHOW',
  IMMEDIATE_DANGER: 'IMMEDIATE_DANGER',
  OTHER: 'OTHER',
} as const;
export type SafetyReportCategory = (typeof SafetyReportCategory)[keyof typeof SafetyReportCategory];

export const SafetyReportStatus = {
  OPEN: 'OPEN',
  TRIAGED: 'TRIAGED',
  ACTION_TAKEN: 'ACTION_TAKEN',
  CLOSED: 'CLOSED',
} as const;
export type SafetyReportStatus = (typeof SafetyReportStatus)[keyof typeof SafetyReportStatus];

// --- Notifications ---
export const NotificationChannel = {
  IN_APP: 'IN_APP',
  PUSH: 'PUSH',
  SMS: 'SMS',
  CONSOLE: 'CONSOLE',
} as const;
export type NotificationChannel = (typeof NotificationChannel)[keyof typeof NotificationChannel];

export const NotificationStatus = {
  PENDING: 'PENDING',
  SENT: 'SENT',
  FAILED: 'FAILED',
  CANCELLED: 'CANCELLED',
} as const;
export type NotificationStatus = (typeof NotificationStatus)[keyof typeof NotificationStatus];

// --- Audit ---
export const AuditActorType = {
  USER: 'USER',
  SYSTEM: 'SYSTEM',
  REVIEWER: 'REVIEWER',
  ADMIN: 'ADMIN',
} as const;
export type AuditActorType = (typeof AuditActorType)[keyof typeof AuditActorType];
