import type { ListingIntent, PropertyType, RealityReviewLevel, RoomType } from './enums.js';

// --- Public Listing Card ---
export type PublicListingCard = {
  id: string;
  slug: string;
  title: string;
  intent: ListingIntent;
  propertyType: PropertyType;
  status: string;
  publicArea: string;
  approximateLat: number | null;
  approximateLng: number | null;
  priceAmount: number;
  currency: string;
  livePulseAt: string | null;
  livePulseExpiresAt: string | null;
  authorityStatus: string;
  realityReviewLevel: RealityReviewLevel | null;
  realityPublishedAt: string | null;
};

// --- Public Reality Passport ---
export type PublicRealityPassport = {
  listing: {
    slug: string;
    title: string;
    publicArea: string;
    intent: ListingIntent;
    propertyType: PropertyType;
    priceAmount: number;
    currency: 'BDT';
    livePulse: {
      status: 'AVAILABLE' | 'UNCONFIRMED' | 'HELD';
      confirmedAt?: string;
    };
  };
  reality: {
    reviewLevel: RealityReviewLevel;
    capturedAt?: string;
    coverageScore: number;
    missingRoomLabels: string[];
    sourceSummary: 'HOLD_TO_CAPTURE' | 'IMPORTED_PANORAMA' | 'PRO_SPATIAL_SCAN' | 'MIXED';
  };
  rooms: Array<{
    id: string;
    roomType: RoomType;
    displayName: string;
    previewUrl: string;
    panoramaUrl?: string;
    sourceType: string;
  }>;
  edges: Array<{
    fromRoomId: string;
    toRoomId: string;
    doorwayLabel: string;
  }>;
};

// --- User Profile ---
export type UserProfile = {
  id: string;
  displayName: string;
  avatarUrl: string | null;
  primaryRole: string;
  identityStatus: string;
};

// --- Checkpoint Decision ---
export type CheckpointDecision = {
  checkpointId: string;
  status: 'ACCEPTED' | 'RETAKE_SUGGESTED' | 'REJECTED';
  reasonCode?:
    | 'HOLD_TOO_SHORT'
    | 'MOTION_TOO_HIGH'
    | 'TOO_DARK'
    | 'BLURRY'
    | 'DUPLICATE_MEDIA'
    | 'INVALID_MEDIA';
  userMessage: string;
  roomAcceptedCount: number;
  roomRequiredCount: number;
  sessionCoverageScore: number;
};

// --- Viewing Pass ---
export type ViewingPassView = {
  viewingId: string;
  passToken: string;
  timeWindow: string;
  date: string;
  expiresAt: string;
  addressUnlockAt: string;
};
