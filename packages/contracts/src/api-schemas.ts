import { z } from 'zod';
import {
  UserRole,
  ListingIntent,
  PropertyType,
  ListingStatus,
  RoomType,
  RealityReviewLevel,
  MessageKind,
  SafetyReportCategory,
} from './enums.js';

// --- Auth ---
export const phoneStartSchema = z.object({
  phone: z.string().regex(/^\+?[0-9]{10,15}$/, 'Valid phone number required'),
});

export const otpVerifySchema = z.object({
  phone: z.string().regex(/^\+?[0-9]{10,15}$/),
  code: z.string().length(6),
});

export const refreshSchema = z.object({
  refreshToken: z.string().min(1),
});

// --- User ---
export const createUserSchema = z.object({
  phone: z.string().regex(/^\+?[0-9]{10,15}$/),
  displayName: z.string().trim().min(2).max(80),
  primaryRole: z.nativeEnum(UserRole),
});

// --- Listing ---
export const createListingSchema = z.object({
  title: z.string().trim().min(5).max(200),
  intent: z.nativeEnum(ListingIntent),
  propertyType: z.nativeEnum(PropertyType),
  publicArea: z.string().trim().min(2).max(100),
  exactAddress: z.string().trim().min(5).max(500),
  mapsLink: z.string().url().optional(),
  approximateLat: z.number().min(20).max(27).optional(),
  approximateLng: z.number().min(88).max(93).optional(),
  priceAmount: z.number().int().min(0),
  currency: z.literal('BDT').default('BDT'),
  rooms: z.array(z.object({
    roomType: z.nativeEnum(RoomType),
    displayName: z.string().trim().min(1).max(60),
    ordinal: z.number().int().min(0),
  })).min(1).max(20),
});

export const updateListingSchema = createListingSchema.partial().omit({ rooms: true });

export const listingQuerySchema = z.object({
  intent: z.nativeEnum(ListingIntent).optional(),
  propertyType: z.nativeEnum(PropertyType).optional(),
  minPrice: z.coerce.number().int().min(0).optional(),
  maxPrice: z.coerce.number().int().min(0).optional(),
  area: z.string().optional(),
  status: z.nativeEnum(ListingStatus).optional(),
  realityLevel: z.nativeEnum(RealityReviewLevel).optional(),
  page: z.coerce.number().int().min(1).default(1),
  limit: z.coerce.number().int().min(1).max(50).default(20),
});

// --- Identity ---
export const startIdentitySchema = z.object({
  consentVersion: z.string().min(1),
});

// --- Authority ---
export const uploadAuthorityEvidenceSchema = z.object({
  listingId: z.string().uuid(),
  authorityRole: z.enum(['OWNER', 'AUTHORISED_AGENT', 'BUILDING_REPRESENTATIVE']),
  notes: z.string().trim().max(500).optional(),
});

// --- Capture ---
export const createCaptureSessionSchema = z.object({
  listingId: z.string().uuid(),
  routeVersion: z.number().int().min(1),
});

export const createCheckpointSchema = z.object({
  sessionId: z.string().uuid(),
  roomId: z.string().uuid(),
  checkpointKey: z.string().min(1).max(64),
  holdDurationMs: z.number().int().min(0).max(10_000),
  orientation: z.object({
    headingDeg: z.number().finite().optional(),
    pitchDeg: z.number().finite().optional(),
    rollDeg: z.number().finite().optional(),
  }).optional(),
  localQuality: z.object({
    stabilityScore: z.number().int().min(0).max(100).optional(),
    brightnessScore: z.number().int().min(0).max(100).optional(),
  }).optional(),
  mediaUploadId: z.string().uuid(),
});

export const confirmMediaUploadSchema = z.object({
  storageKey: z.string().min(1),
  sha256: z.string().length(64),
  mimeType: z.string().min(1),
  width: z.number().int().positive().optional(),
  height: z.number().int().positive().optional(),
});

// --- Messaging ---
export const sendMessageSchema = z.object({
  kind: z.nativeEnum(MessageKind).default('TEXT'),
  body: z.string().trim().min(1).max(2000).optional(),
  relatedEntityType: z.string().optional(),
  relatedEntityId: z.string().uuid().optional(),
});

// --- Offers ---
export const createOfferSchema = z.object({
  conversationId: z.string().uuid(),
  terms: z.object({
    offerType: z.enum(['RENT', 'SALE']),
    priceAmount: z.number().int().min(0),
    advanceAmount: z.number().int().min(0).optional(),
    moveInDate: z.string().datetime().optional(),
    utilitiesIncluded: z.array(z.string()).optional(),
    furnitureIncluded: z.array(z.string()).optional(),
    conditionNote: z.string().trim().max(500).optional(),
    expiresAt: z.string().datetime().optional(),
  }),
  note: z.string().trim().max(500).optional(),
});

export const counterOfferSchema = z.object({
  terms: z.object({
    priceAmount: z.number().int().min(0),
    advanceAmount: z.number().int().min(0).optional(),
    moveInDate: z.string().datetime().optional(),
    utilitiesIncluded: z.array(z.string()).optional(),
    furnitureIncluded: z.array(z.string()).optional(),
    conditionNote: z.string().trim().max(500).optional(),
    expiresAt: z.string().datetime().optional(),
  }),
  note: z.string().trim().max(500).optional(),
});

// --- Viewings ---
export const requestViewingSchema = z.object({
  preferredSlotId: z.string().uuid(),
  attendeeCount: z.number().int().min(1).max(3),
  companionName: z.string().trim().min(2).max(80).optional(),
  note: z.string().trim().max(280).optional(),
  acceptedSafetyTerms: z.literal(true),
});

export const proposeSlotSchema = z.object({
  startsAt: z.string().datetime(),
  endsAt: z.string().datetime(),
});

export const createSlotSchema = z.object({
  startsAt: z.string().datetime(),
  endsAt: z.string().datetime(),
  capacity: z.number().int().min(1).max(10).default(1),
});

// --- Safety ---
export const safetyReportSchema = z.object({
  viewingId: z.string().uuid().optional(),
  listingId: z.string().uuid().optional(),
  category: z.nativeEnum(SafetyReportCategory),
  description: z.string().trim().min(10).max(2000),
});

// --- Response DTOs ---
export const paginationSchema = z.object({
  page: z.number().int().min(1),
  limit: z.number().int().min(1),
  total: z.number().int().min(0),
});

export function successResponse<T extends z.ZodType>(dataSchema: T) {
  return z.object({
    data: dataSchema,
    meta: paginationSchema.partial().optional(),
  });
}

export const errorResponseSchema = z.object({
  error: z.object({
    code: z.string(),
    message: z.string(),
    requestId: z.string().optional(),
  }),
});
