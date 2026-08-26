-- CreateEnum
CREATE TYPE "UserRole" AS ENUM ('SEEKER', 'OWNER', 'AGENT', 'REPRESENTATIVE', 'GUARD', 'REVIEWER', 'ADMIN');

-- CreateEnum
CREATE TYPE "IdentityStatus" AS ENUM ('UNVERIFIED', 'PHONE_CONFIRMED', 'IDENTITY_PENDING', 'IDENTITY_CONFIRMED', 'IDENTITY_REJECTED', 'IDENTITY_EXPIRED');

-- CreateEnum
CREATE TYPE "ListingIntent" AS ENUM ('RENT', 'SALE');

-- CreateEnum
CREATE TYPE "PropertyType" AS ENUM ('APARTMENT', 'HOUSE', 'ROOM', 'SUBLET', 'HOSTEL_SEAT', 'OFFICE', 'SHOP', 'LAND');

-- CreateEnum
CREATE TYPE "ListingStatus" AS ENUM ('DRAFT', 'AUTHORITY_REVIEW_PENDING', 'ACTIVE', 'VIEWING_HELD', 'UNCONFIRMED', 'RENTED_OR_SOLD', 'PAUSED', 'RESTRICTED', 'ARCHIVED');

-- CreateEnum
CREATE TYPE "AuthorityStatus" AS ENUM ('NOT_STARTED', 'PENDING', 'REVIEWED', 'REJECTED', 'EXPIRED');

-- CreateEnum
CREATE TYPE "AuthorityRole" AS ENUM ('OWNER', 'AUTHORISED_AGENT', 'BUILDING_REPRESENTATIVE');

-- CreateEnum
CREATE TYPE "CaptureStatus" AS ENUM ('IN_PROGRESS', 'UPLOADING', 'PROCESSING', 'NEEDS_RETAKE', 'READY_FOR_REVIEW', 'PUBLISHED_SELLER_CAPTURED', 'PUBLISHED_AGENT_VERIFIED', 'EXPIRED', 'ABANDONED');

-- CreateEnum
CREATE TYPE "RoomType" AS ENUM ('ENTRY', 'LIVING_ROOM', 'DINING_ROOM', 'BEDROOM', 'KITCHEN', 'BATHROOM', 'BALCONY', 'UTILITY', 'PARKING', 'OTHER');

-- CreateEnum
CREATE TYPE "RealityReviewLevel" AS ENUM ('INCOMPLETE', 'SELLER_CAPTURED', 'AGENT_VERIFIED', 'EXPIRED');

-- CreateEnum
CREATE TYPE "MediaQualityStatus" AS ENUM ('PENDING', 'ACCEPTED', 'RETAKE_SUGGESTED', 'REJECTED');

-- CreateEnum
CREATE TYPE "MediaSourceType" AS ENUM ('HOLD_TO_CAPTURE', 'GUIDED_SEQUENCE', 'IMPORTED_PANORAMA', 'PRO_SPATIAL_SCAN', 'PROOF_REQUEST');

-- CreateEnum
CREATE TYPE "VerificationProviderType" AS ENUM ('MANUAL_REVIEW', 'AUTHORISED_NID_PROVIDER', 'DISABLED');

-- CreateEnum
CREATE TYPE "VerificationReviewStatus" AS ENUM ('NOT_STARTED', 'PENDING', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'CANCELLED');

-- CreateEnum
CREATE TYPE "EvidenceKind" AS ENUM ('IDENTITY_DOCUMENT', 'SELFIE_OR_LIVENESS_RESULT', 'PROPERTY_AUTHORITY_DOCUMENT', 'OWNER_AUTHORISATION', 'AGENCY_BUSINESS_DOCUMENT', 'BUILDING_MANAGER_CONFIRMATION', 'OPTIONAL_PCC');

-- CreateEnum
CREATE TYPE "EvidenceReviewStatus" AS ENUM ('UPLOADED', 'UNDER_REVIEW', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'RETENTION_PURGED');

-- CreateEnum
CREATE TYPE "ConversationStatus" AS ENUM ('ACTIVE', 'BLOCKED', 'ARCHIVED');

-- CreateEnum
CREATE TYPE "MessageKind" AS ENUM ('TEXT', 'SYSTEM_NOTICE', 'PROOF_REQUEST', 'PROOF_RESPONSE', 'OFFER_CARD', 'VIEWING_CARD');

-- CreateEnum
CREATE TYPE "OfferStatus" AS ENUM ('DRAFT', 'SENT', 'COUNTERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'WITHDRAWN');

-- CreateEnum
CREATE TYPE "ViewingStatus" AS ENUM ('REQUESTED', 'PROPOSED', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'CHECKED_IN', 'COMPLETED', 'SAFETY_FOLLOW_UP');

-- CreateEnum
CREATE TYPE "ViewingPassStatus" AS ENUM ('ISSUED', 'VIEWED', 'CHECKED_IN', 'INVALIDATED', 'EXPIRED');

-- CreateEnum
CREATE TYPE "SafetyEventType" AS ENUM ('ADDRESS_REVEALED', 'PASS_VIEWED', 'PASS_SCANNED', 'SEEKER_CHECKED_IN', 'HOST_CHECKED_IN', 'SEEKER_CHECKED_OUT', 'HOST_CHECKED_OUT', 'CHECKOUT_REMINDER_SENT', 'TRUSTED_CONTACT_ALERTED', 'SAFETY_CONCERN_REPORTED', 'APPOINTMENT_CANCELLED');

-- CreateEnum
CREATE TYPE "SafetyReportCategory" AS ENUM ('SUSPICIOUS_BEHAVIOUR', 'FALSE_LISTING', 'HARASSMENT', 'ADDRESS_MISUSE', 'NO_SHOW', 'IMMEDIATE_DANGER', 'OTHER');

-- CreateEnum
CREATE TYPE "SafetyReportStatus" AS ENUM ('OPEN', 'TRIAGED', 'ACTION_TAKEN', 'CLOSED');

-- CreateEnum
CREATE TYPE "NotificationChannel" AS ENUM ('IN_APP', 'PUSH', 'SMS', 'CONSOLE');

-- CreateEnum
CREATE TYPE "NotificationStatus" AS ENUM ('PENDING', 'SENT', 'FAILED', 'CANCELLED');

-- CreateEnum
CREATE TYPE "AuditActorType" AS ENUM ('USER', 'SYSTEM', 'REVIEWER', 'ADMIN');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "phoneHash" TEXT NOT NULL,
    "phoneLast4" TEXT NOT NULL,
    "displayName" TEXT NOT NULL,
    "avatarUrl" TEXT,
    "primaryRole" "UserRole" NOT NULL,
    "identityStatus" "IdentityStatus" NOT NULL DEFAULT 'UNVERIFIED',
    "identityVerifiedAt" TIMESTAMP(3),
    "identityExpiresAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Listing" (
    "id" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "ownerId" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "intent" "ListingIntent" NOT NULL,
    "propertyType" "PropertyType" NOT NULL,
    "status" "ListingStatus" NOT NULL DEFAULT 'DRAFT',
    "publicArea" TEXT NOT NULL,
    "exactAddressEncrypted" BYTEA,
    "exactLatEncrypted" BYTEA,
    "exactLngEncrypted" BYTEA,
    "approximateLat" DECIMAL(9,6),
    "approximateLng" DECIMAL(9,6),
    "mapsLink" TEXT,
    "priceAmount" INTEGER NOT NULL,
    "currency" TEXT NOT NULL DEFAULT 'BDT',
    "livePulseAt" TIMESTAMP(3),
    "livePulseExpiresAt" TIMESTAMP(3),
    "authorityStatus" "AuthorityStatus" NOT NULL DEFAULT 'NOT_STARTED',
    "authorityExpiresAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Listing_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CaptureSession" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "capturedByUserId" TEXT NOT NULL,
    "routeVersion" INTEGER NOT NULL,
    "status" "CaptureStatus" NOT NULL DEFAULT 'IN_PROGRESS',
    "coverageScore" INTEGER NOT NULL DEFAULT 0,
    "startedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "submittedAt" TIMESTAMP(3),
    "captureTimestamp" TIMESTAMP(3),
    "metadataJson" JSONB,

    CONSTRAINT "CaptureSession_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Room" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "captureSessionId" TEXT,
    "roomType" "RoomType" NOT NULL,
    "displayName" TEXT NOT NULL,
    "ordinal" INTEGER NOT NULL,

    CONSTRAINT "Room_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "MediaAsset" (
    "id" TEXT NOT NULL,
    "captureSessionId" TEXT NOT NULL,
    "storageKey" TEXT NOT NULL,
    "sha256" TEXT NOT NULL,
    "mimeType" TEXT NOT NULL,
    "width" INTEGER,
    "height" INTEGER,
    "capturedAt" TIMESTAMP(3),
    "qualityStatus" "MediaQualityStatus" NOT NULL DEFAULT 'PENDING',
    "sourceType" "MediaSourceType" NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "MediaAsset_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "RealityPassport" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "captureSessionId" TEXT NOT NULL,
    "reviewLevel" "RealityReviewLevel" NOT NULL DEFAULT 'INCOMPLETE',
    "coverageScore" INTEGER NOT NULL DEFAULT 0,
    "publishedAt" TIMESTAMP(3),
    "expiresAt" TIMESTAMP(3),
    "publicStatus" TEXT NOT NULL DEFAULT 'INCOMPLETE',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "RealityPassport_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "IdentityVerification" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "providerType" "VerificationProviderType" NOT NULL,
    "status" "VerificationReviewStatus" NOT NULL DEFAULT 'NOT_STARTED',
    "consentVersion" TEXT NOT NULL,
    "consentedAt" TIMESTAMP(3) NOT NULL,
    "providerReferenceEnc" BYTEA,
    "verifiedNameEnc" BYTEA,
    "portraitAssetId" TEXT,
    "verificationDataEnc" BYTEA,
    "reviewerId" TEXT,
    "reviewerNoteEnc" BYTEA,
    "verifiedAt" TIMESTAMP(3),
    "expiresAt" TIMESTAMP(3),
    "rejectedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "IdentityVerification_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SensitiveBlob" (
    "id" TEXT NOT NULL,
    "subjectType" TEXT NOT NULL,
    "subjectId" TEXT NOT NULL,
    "fieldName" TEXT NOT NULL,
    "keyVersion" INTEGER NOT NULL,
    "encryptedDek" BYTEA NOT NULL,
    "iv" BYTEA NOT NULL,
    "ciphertext" BYTEA NOT NULL,
    "authTag" BYTEA NOT NULL,
    "aadVersion" INTEGER NOT NULL DEFAULT 1,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "rotatedAt" TIMESTAMP(3),
    "purgeAfter" TIMESTAMP(3),
    "purgedAt" TIMESTAMP(3),

    CONSTRAINT "SensitiveBlob_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PrivateEvidence" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "listingId" TEXT,
    "kind" "EvidenceKind" NOT NULL,
    "status" "EvidenceReviewStatus" NOT NULL DEFAULT 'UPLOADED',
    "storageKeyEncrypted" BYTEA NOT NULL,
    "fileSha256" TEXT NOT NULL,
    "fileMimeType" TEXT NOT NULL,
    "fileSizeBytes" INTEGER NOT NULL,
    "uploadExpiresAt" TIMESTAMP(3),
    "reviewedByUserId" TEXT,
    "reviewedAt" TIMESTAMP(3),
    "reviewNoteEnc" BYTEA,
    "retentionUntil" TIMESTAMP(3) NOT NULL,
    "purgedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "PrivateEvidence_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AuthorityReview" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "applicantUserId" TEXT NOT NULL,
    "authorityRole" "AuthorityRole" NOT NULL,
    "status" "AuthorityStatus" NOT NULL DEFAULT 'PENDING',
    "reviewerId" TEXT,
    "reviewedAt" TIMESTAMP(3),
    "expiresAt" TIMESTAMP(3),
    "privateSummaryEnc" BYTEA,
    "publicLabel" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "AuthorityReview_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TrustedContact" (
    "id" TEXT NOT NULL,
    "ownerUserId" TEXT NOT NULL,
    "displayNameEnc" BYTEA NOT NULL,
    "phoneEncrypted" BYTEA NOT NULL,
    "phoneLast4" TEXT NOT NULL,
    "relationshipEnc" BYTEA,
    "verifiedAt" TIMESTAMP(3),
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "TrustedContact_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AvailabilitySlot" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "hostUserId" TEXT NOT NULL,
    "startsAt" TIMESTAMP(3) NOT NULL,
    "endsAt" TIMESTAMP(3) NOT NULL,
    "capacity" INTEGER NOT NULL DEFAULT 1,
    "confirmedCount" INTEGER NOT NULL DEFAULT 0,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "AvailabilitySlot_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Conversation" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "seekerUserId" TEXT NOT NULL,
    "hostUserId" TEXT NOT NULL,
    "status" "ConversationStatus" NOT NULL DEFAULT 'ACTIVE',
    "lastMessageAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Conversation_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Message" (
    "id" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "senderUserId" TEXT NOT NULL,
    "kind" "MessageKind" NOT NULL,
    "bodyEncrypted" BYTEA,
    "safePreview" TEXT,
    "relatedEntityType" TEXT,
    "relatedEntityId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "editedAt" TIMESTAMP(3),
    "deletedAt" TIMESTAMP(3),

    CONSTRAINT "Message_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Offer" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "senderUserId" TEXT NOT NULL,
    "recipientUserId" TEXT NOT NULL,
    "currentVersion" INTEGER NOT NULL DEFAULT 1,
    "status" "OfferStatus" NOT NULL DEFAULT 'DRAFT',
    "expiresAt" TIMESTAMP(3),
    "acceptedAt" TIMESTAMP(3),
    "declinedAt" TIMESTAMP(3),
    "withdrawnAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Offer_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "OfferVersion" (
    "id" TEXT NOT NULL,
    "offerId" TEXT NOT NULL,
    "version" INTEGER NOT NULL,
    "createdByUserId" TEXT NOT NULL,
    "termsJson" JSONB NOT NULL,
    "noteEncrypted" BYTEA,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "OfferVersion_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Viewing" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "seekerId" TEXT NOT NULL,
    "hostId" TEXT NOT NULL,
    "status" "ViewingStatus" NOT NULL DEFAULT 'REQUESTED',
    "startsAt" TIMESTAMP(3),
    "endsAt" TIMESTAMP(3),
    "attendeeCount" INTEGER NOT NULL DEFAULT 1,
    "companionName" TEXT,
    "addressRevealAt" TIMESTAMP(3),
    "exactAddressViewedAt" TIMESTAMP(3),
    "seekerCheckedInAt" TIMESTAMP(3),
    "hostCheckedInAt" TIMESTAMP(3),
    "seekerCheckedOutAt" TIMESTAMP(3),
    "hostCheckedOutAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Viewing_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ViewingPass" (
    "id" TEXT NOT NULL,
    "viewingId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "tokenVersion" INTEGER NOT NULL DEFAULT 1,
    "status" "ViewingPassStatus" NOT NULL DEFAULT 'ISSUED',
    "issuedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "viewedAt" TIMESTAMP(3),
    "checkedInAt" TIMESTAMP(3),
    "invalidatedAt" TIMESTAMP(3),
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "scanCount" INTEGER NOT NULL DEFAULT 0,
    "lastScannedByUserId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "ViewingPass_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SafetyEvent" (
    "id" TEXT NOT NULL,
    "viewingId" TEXT NOT NULL,
    "actorUserId" TEXT,
    "eventType" "SafetyEventType" NOT NULL,
    "occurredAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "metadataEncrypted" BYTEA,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "SafetyEvent_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SafetyReport" (
    "id" TEXT NOT NULL,
    "viewingId" TEXT,
    "listingId" TEXT,
    "reporterUserId" TEXT NOT NULL,
    "reportedUserId" TEXT,
    "category" "SafetyReportCategory" NOT NULL,
    "status" "SafetyReportStatus" NOT NULL DEFAULT 'OPEN',
    "descriptionEnc" BYTEA NOT NULL,
    "evidenceAssetIdsEnc" BYTEA,
    "triagedByUserId" TEXT,
    "triagedAt" TIMESTAMP(3),
    "resolutionEnc" BYTEA,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "SafetyReport_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Notification" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "channel" "NotificationChannel" NOT NULL,
    "type" TEXT NOT NULL,
    "payloadJson" JSONB NOT NULL,
    "status" "NotificationStatus" NOT NULL DEFAULT 'PENDING',
    "scheduledFor" TIMESTAMP(3),
    "sentAt" TIMESTAMP(3),
    "failureReason" TEXT,
    "dedupeKey" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "Notification_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AuditLog" (
    "id" TEXT NOT NULL,
    "actorType" "AuditActorType" NOT NULL,
    "actorUserId" TEXT,
    "action" TEXT NOT NULL,
    "subjectType" TEXT NOT NULL,
    "subjectId" TEXT NOT NULL,
    "requestId" TEXT,
    "ipHash" TEXT,
    "userAgentHash" TEXT,
    "publicMetadataJson" JSONB,
    "eventHash" TEXT NOT NULL,
    "previousEventHash" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AuditLog_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CaptureRouteTemplate" (
    "id" TEXT NOT NULL,
    "version" INTEGER NOT NULL,
    "propertyType" "PropertyType" NOT NULL,
    "displayName" TEXT NOT NULL,
    "templateJson" JSONB NOT NULL,
    "isActive" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "CaptureRouteTemplate_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CaptureRoomProgress" (
    "id" TEXT NOT NULL,
    "captureSessionId" TEXT NOT NULL,
    "roomId" TEXT NOT NULL,
    "ordinal" INTEGER NOT NULL,
    "requiredCheckpointCount" INTEGER NOT NULL,
    "acceptedCheckpointCount" INTEGER NOT NULL DEFAULT 0,
    "status" TEXT NOT NULL DEFAULT 'NOT_STARTED',
    "startedAt" TIMESTAMP(3),
    "completedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "CaptureRoomProgress_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "SpatialCheckpoint" (
    "id" TEXT NOT NULL,
    "captureSessionId" TEXT NOT NULL,
    "roomId" TEXT NOT NULL,
    "checkpointKey" TEXT NOT NULL,
    "ordinal" INTEGER NOT NULL,
    "expectedDirection" TEXT NOT NULL,
    "holdDurationMs" INTEGER NOT NULL,
    "startedAt" TIMESTAMP(3),
    "completedAt" TIMESTAMP(3),
    "deviceHeadingDeg" DECIMAL(7,3),
    "devicePitchDeg" DECIMAL(7,3),
    "deviceRollDeg" DECIMAL(7,3),
    "stabilityScore" INTEGER,
    "brightnessScore" INTEGER,
    "blurScore" INTEGER,
    "coverageStatus" TEXT NOT NULL DEFAULT 'PENDING',
    "primaryMediaAssetId" TEXT,
    "burstAssetIdsJson" JSONB,
    "rejectionReason" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "SpatialCheckpoint_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TourNode" (
    "id" TEXT NOT NULL,
    "roomId" TEXT NOT NULL,
    "panoramaAssetId" TEXT,
    "previewAssetId" TEXT NOT NULL,
    "mapX" DECIMAL(8,4),
    "mapY" DECIMAL(8,4),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "TourNode_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "TourEdge" (
    "id" TEXT NOT NULL,
    "fromNodeId" TEXT NOT NULL,
    "toNodeId" TEXT NOT NULL,
    "doorwayLabel" TEXT NOT NULL,
    "hotspotYaw" DECIMAL(8,4),
    "hotspotPitch" DECIMAL(8,4),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "TourEdge_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_phoneHash_key" ON "User"("phoneHash");

-- CreateIndex
CREATE UNIQUE INDEX "Listing_slug_key" ON "Listing"("slug");

-- CreateIndex
CREATE UNIQUE INDEX "MediaAsset_storageKey_key" ON "MediaAsset"("storageKey");

-- CreateIndex
CREATE INDEX "IdentityVerification_userId_status_idx" ON "IdentityVerification"("userId", "status");

-- CreateIndex
CREATE INDEX "IdentityVerification_status_expiresAt_idx" ON "IdentityVerification"("status", "expiresAt");

-- CreateIndex
CREATE INDEX "SensitiveBlob_purgeAfter_idx" ON "SensitiveBlob"("purgeAfter");

-- CreateIndex
CREATE UNIQUE INDEX "SensitiveBlob_subjectType_subjectId_fieldName_key" ON "SensitiveBlob"("subjectType", "subjectId", "fieldName");

-- CreateIndex
CREATE INDEX "PrivateEvidence_userId_kind_status_idx" ON "PrivateEvidence"("userId", "kind", "status");

-- CreateIndex
CREATE INDEX "PrivateEvidence_listingId_kind_status_idx" ON "PrivateEvidence"("listingId", "kind", "status");

-- CreateIndex
CREATE INDEX "PrivateEvidence_retentionUntil_idx" ON "PrivateEvidence"("retentionUntil");

-- CreateIndex
CREATE INDEX "AuthorityReview_listingId_status_idx" ON "AuthorityReview"("listingId", "status");

-- CreateIndex
CREATE INDEX "AuthorityReview_applicantUserId_status_idx" ON "AuthorityReview"("applicantUserId", "status");

-- CreateIndex
CREATE INDEX "TrustedContact_ownerUserId_isActive_idx" ON "TrustedContact"("ownerUserId", "isActive");

-- CreateIndex
CREATE INDEX "AvailabilitySlot_listingId_startsAt_isActive_idx" ON "AvailabilitySlot"("listingId", "startsAt", "isActive");

-- CreateIndex
CREATE INDEX "AvailabilitySlot_hostUserId_startsAt_isActive_idx" ON "AvailabilitySlot"("hostUserId", "startsAt", "isActive");

-- CreateIndex
CREATE INDEX "Conversation_seekerUserId_lastMessageAt_idx" ON "Conversation"("seekerUserId", "lastMessageAt");

-- CreateIndex
CREATE INDEX "Conversation_hostUserId_lastMessageAt_idx" ON "Conversation"("hostUserId", "lastMessageAt");

-- CreateIndex
CREATE UNIQUE INDEX "Conversation_listingId_seekerUserId_hostUserId_key" ON "Conversation"("listingId", "seekerUserId", "hostUserId");

-- CreateIndex
CREATE INDEX "Message_conversationId_createdAt_idx" ON "Message"("conversationId", "createdAt");

-- CreateIndex
CREATE INDEX "Offer_listingId_status_idx" ON "Offer"("listingId", "status");

-- CreateIndex
CREATE INDEX "Offer_recipientUserId_status_idx" ON "Offer"("recipientUserId", "status");

-- CreateIndex
CREATE INDEX "Offer_expiresAt_status_idx" ON "Offer"("expiresAt", "status");

-- CreateIndex
CREATE UNIQUE INDEX "OfferVersion_offerId_version_key" ON "OfferVersion"("offerId", "version");

-- CreateIndex
CREATE INDEX "Viewing_listingId_status_idx" ON "Viewing"("listingId", "status");

-- CreateIndex
CREATE INDEX "Viewing_seekerId_status_idx" ON "Viewing"("seekerId", "status");

-- CreateIndex
CREATE INDEX "Viewing_hostId_status_idx" ON "Viewing"("hostId", "status");

-- CreateIndex
CREATE UNIQUE INDEX "ViewingPass_viewingId_key" ON "ViewingPass"("viewingId");

-- CreateIndex
CREATE UNIQUE INDEX "ViewingPass_tokenHash_key" ON "ViewingPass"("tokenHash");

-- CreateIndex
CREATE INDEX "ViewingPass_expiresAt_status_idx" ON "ViewingPass"("expiresAt", "status");

-- CreateIndex
CREATE INDEX "SafetyEvent_viewingId_occurredAt_idx" ON "SafetyEvent"("viewingId", "occurredAt");

-- CreateIndex
CREATE INDEX "SafetyEvent_eventType_occurredAt_idx" ON "SafetyEvent"("eventType", "occurredAt");

-- CreateIndex
CREATE INDEX "SafetyReport_status_createdAt_idx" ON "SafetyReport"("status", "createdAt");

-- CreateIndex
CREATE INDEX "SafetyReport_reporterUserId_createdAt_idx" ON "SafetyReport"("reporterUserId", "createdAt");

-- CreateIndex
CREATE INDEX "Notification_status_scheduledFor_idx" ON "Notification"("status", "scheduledFor");

-- CreateIndex
CREATE UNIQUE INDEX "Notification_userId_dedupeKey_key" ON "Notification"("userId", "dedupeKey");

-- CreateIndex
CREATE UNIQUE INDEX "AuditLog_eventHash_key" ON "AuditLog"("eventHash");

-- CreateIndex
CREATE INDEX "AuditLog_subjectType_subjectId_createdAt_idx" ON "AuditLog"("subjectType", "subjectId", "createdAt");

-- CreateIndex
CREATE INDEX "AuditLog_actorUserId_createdAt_idx" ON "AuditLog"("actorUserId", "createdAt");

-- CreateIndex
CREATE INDEX "AuditLog_action_createdAt_idx" ON "AuditLog"("action", "createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "CaptureRouteTemplate_version_key" ON "CaptureRouteTemplate"("version");

-- CreateIndex
CREATE UNIQUE INDEX "CaptureRoomProgress_captureSessionId_roomId_key" ON "CaptureRoomProgress"("captureSessionId", "roomId");

-- CreateIndex
CREATE INDEX "SpatialCheckpoint_captureSessionId_ordinal_idx" ON "SpatialCheckpoint"("captureSessionId", "ordinal");

-- CreateIndex
CREATE UNIQUE INDEX "SpatialCheckpoint_captureSessionId_roomId_checkpointKey_key" ON "SpatialCheckpoint"("captureSessionId", "roomId", "checkpointKey");

-- CreateIndex
CREATE UNIQUE INDEX "TourNode_roomId_key" ON "TourNode"("roomId");

-- CreateIndex
CREATE UNIQUE INDEX "TourEdge_fromNodeId_toNodeId_key" ON "TourEdge"("fromNodeId", "toNodeId");

-- AddForeignKey
ALTER TABLE "Listing" ADD CONSTRAINT "Listing_ownerId_fkey" FOREIGN KEY ("ownerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CaptureSession" ADD CONSTRAINT "CaptureSession_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CaptureSession" ADD CONSTRAINT "CaptureSession_capturedByUserId_fkey" FOREIGN KEY ("capturedByUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Room" ADD CONSTRAINT "Room_captureSessionId_fkey" FOREIGN KEY ("captureSessionId") REFERENCES "CaptureSession"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Room" ADD CONSTRAINT "Room_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MediaAsset" ADD CONSTRAINT "MediaAsset_captureSessionId_fkey" FOREIGN KEY ("captureSessionId") REFERENCES "CaptureSession"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RealityPassport" ADD CONSTRAINT "RealityPassport_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RealityPassport" ADD CONSTRAINT "RealityPassport_captureSessionId_fkey" FOREIGN KEY ("captureSessionId") REFERENCES "CaptureSession"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "IdentityVerification" ADD CONSTRAINT "IdentityVerification_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PrivateEvidence" ADD CONSTRAINT "PrivateEvidence_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AuthorityReview" ADD CONSTRAINT "AuthorityReview_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TrustedContact" ADD CONSTRAINT "TrustedContact_ownerUserId_fkey" FOREIGN KEY ("ownerUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AvailabilitySlot" ADD CONSTRAINT "AvailabilitySlot_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Conversation" ADD CONSTRAINT "Conversation_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Conversation" ADD CONSTRAINT "Conversation_seekerUserId_fkey" FOREIGN KEY ("seekerUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Conversation" ADD CONSTRAINT "Conversation_hostUserId_fkey" FOREIGN KEY ("hostUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Message" ADD CONSTRAINT "Message_conversationId_fkey" FOREIGN KEY ("conversationId") REFERENCES "Conversation"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Message" ADD CONSTRAINT "Message_senderUserId_fkey" FOREIGN KEY ("senderUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Offer" ADD CONSTRAINT "Offer_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Offer" ADD CONSTRAINT "Offer_senderUserId_fkey" FOREIGN KEY ("senderUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Offer" ADD CONSTRAINT "Offer_recipientUserId_fkey" FOREIGN KEY ("recipientUserId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "OfferVersion" ADD CONSTRAINT "OfferVersion_offerId_fkey" FOREIGN KEY ("offerId") REFERENCES "Offer"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Viewing" ADD CONSTRAINT "Viewing_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Viewing" ADD CONSTRAINT "Viewing_seekerId_fkey" FOREIGN KEY ("seekerId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Viewing" ADD CONSTRAINT "Viewing_hostId_fkey" FOREIGN KEY ("hostId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ViewingPass" ADD CONSTRAINT "ViewingPass_viewingId_fkey" FOREIGN KEY ("viewingId") REFERENCES "Viewing"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "SafetyEvent" ADD CONSTRAINT "SafetyEvent_viewingId_fkey" FOREIGN KEY ("viewingId") REFERENCES "Viewing"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "SafetyReport" ADD CONSTRAINT "SafetyReport_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AuditLog" ADD CONSTRAINT "AuditLog_actorUserId_fkey" FOREIGN KEY ("actorUserId") REFERENCES "User"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CaptureRoomProgress" ADD CONSTRAINT "CaptureRoomProgress_captureSessionId_fkey" FOREIGN KEY ("captureSessionId") REFERENCES "CaptureSession"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "SpatialCheckpoint" ADD CONSTRAINT "SpatialCheckpoint_captureSessionId_fkey" FOREIGN KEY ("captureSessionId") REFERENCES "CaptureSession"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TourNode" ADD CONSTRAINT "TourNode_roomId_fkey" FOREIGN KEY ("roomId") REFERENCES "Room"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TourEdge" ADD CONSTRAINT "TourEdge_fromNodeId_fkey" FOREIGN KEY ("fromNodeId") REFERENCES "TourNode"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "TourEdge" ADD CONSTRAINT "TourEdge_toNodeId_fkey" FOREIGN KEY ("toNodeId") REFERENCES "TourNode"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
