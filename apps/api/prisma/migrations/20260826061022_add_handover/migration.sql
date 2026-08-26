-- CreateEnum
CREATE TYPE "HandoverPassportStatus" AS ENUM ('PREPARING', 'ACTIVE', 'HANDOVER_REVIEW', 'CLOSED');

-- CreateEnum
CREATE TYPE "HandoverParticipantRole" AS ENUM ('BUYER', 'DEVELOPER_REPRESENTATIVE', 'OWNER_REPRESENTATIVE', 'VIEWER');

-- CreateEnum
CREATE TYPE "PromiseCategory" AS ENUM ('HANDOVER_DATE', 'PAYMENT_TERM', 'PRICE_OR_FEE', 'UNIT_SIZE_OR_LAYOUT', 'PARKING', 'FITMENT_OR_MATERIAL', 'UTILITY_OR_SERVICE', 'REGISTRATION_OR_DOCUMENT', 'OTHER');

-- CreateEnum
CREATE TYPE "PromiseStatus" AS ENUM ('DRAFT', 'PENDING_ACKNOWLEDGEMENT', 'ACKNOWLEDGED', 'EVIDENCE_SUBMITTED', 'CHANGE_PROPOSED', 'REMEDY_OPEN', 'REMEDY_IN_PROGRESS', 'RESOLVED', 'CONTESTED', 'ARCHIVED');

-- CreateEnum
CREATE TYPE "HandoverEvidenceType" AS ENUM ('PHOTO', 'DOCUMENT', 'RECEIPT', 'PAYMENT_REFERENCE', 'MESSAGE_NOTE', 'EXISTING_DORJA_CAPTURE_REFERENCE', 'SITE_UPDATE', 'OTHER');

-- CreateEnum
CREATE TYPE "RemedyPriority" AS ENUM ('LOW', 'NORMAL', 'HIGH');

-- CreateEnum
CREATE TYPE "RemedyStatus" AS ENUM ('OPEN', 'ACKNOWLEDGED', 'REMEDY_PROPOSED', 'IN_PROGRESS', 'READY_FOR_REVIEW', 'RESOLVED', 'CONTESTED', 'CLOSED');

-- CreateEnum
CREATE TYPE "HandoverEventType" AS ENUM ('PASSPORT_CREATED', 'PROMISE_CREATED', 'PROMISE_ACKNOWLEDGED', 'PROMISE_CHANGE_PROPOSED', 'PROMISE_CONTESTED', 'EVIDENCE_ADDED', 'REMEDY_OPENED', 'REMEDY_DATE_PROPOSED', 'REMEDY_DATE_ACCEPTED', 'REMEDY_READY_FOR_REVIEW', 'REMEDY_RESOLVED', 'REMEDY_CONTESTED', 'EVIDENCE_PACK_EXPORTED');

-- CreateTable
CREATE TABLE "HandoverPassport" (
    "id" TEXT NOT NULL,
    "listingId" TEXT NOT NULL,
    "status" "HandoverPassportStatus" NOT NULL DEFAULT 'PREPARING',
    "agreementDate" TIMESTAMP(3),
    "latestActivityAt" TIMESTAMP(3),
    "promiseResolvedCount" INTEGER NOT NULL DEFAULT 0,
    "promiseTotalCount" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "HandoverPassport_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "HandoverParticipant" (
    "id" TEXT NOT NULL,
    "passportId" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "role" "HandoverParticipantRole" NOT NULL DEFAULT 'VIEWER',
    "invitedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "acceptedAt" TIMESTAMP(3),

    CONSTRAINT "HandoverParticipant_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Promise" (
    "id" TEXT NOT NULL,
    "passportId" TEXT NOT NULL,
    "category" "PromiseCategory" NOT NULL,
    "title" TEXT NOT NULL,
    "originalPromiseText" TEXT NOT NULL,
    "sourceReferenceLabel" TEXT,
    "sourceDocumentUrl" TEXT,
    "promisedDate" TIMESTAMP(3),
    "promisedAmount" INTEGER,
    "currency" TEXT NOT NULL DEFAULT 'BDT',
    "originallyCreatedByUserId" TEXT NOT NULL,
    "currentStatus" "PromiseStatus" NOT NULL DEFAULT 'DRAFT',
    "acknowledgementStatus" TEXT NOT NULL DEFAULT 'PENDING_ACKNOWLEDGEMENT',
    "acknowledgedByUserId" TEXT,
    "acknowledgedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Promise_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PromiseRevision" (
    "id" TEXT NOT NULL,
    "promiseId" TEXT NOT NULL,
    "revisionNumber" INTEGER NOT NULL,
    "proposedText" TEXT,
    "proposedDate" TIMESTAMP(3),
    "proposedAmount" INTEGER,
    "proposerUserId" TEXT NOT NULL,
    "note" TEXT,
    "state" TEXT NOT NULL DEFAULT 'PROPOSED',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "PromiseRevision_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "HandoverEvidence" (
    "id" TEXT NOT NULL,
    "passportId" TEXT NOT NULL,
    "promiseId" TEXT,
    "remedyId" TEXT,
    "submitterUserId" TEXT NOT NULL,
    "evidenceType" "HandoverEvidenceType" NOT NULL,
    "label" TEXT NOT NULL,
    "storageKey" TEXT,
    "visibleNote" TEXT,
    "privateNote" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "HandoverEvidence_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "RemedyIssue" (
    "id" TEXT NOT NULL,
    "passportId" TEXT NOT NULL,
    "linkedPromiseId" TEXT,
    "title" TEXT NOT NULL,
    "description" TEXT,
    "createdByUserId" TEXT NOT NULL,
    "priority" "RemedyPriority" NOT NULL DEFAULT 'NORMAL',
    "status" "RemedyStatus" NOT NULL DEFAULT 'OPEN',
    "proposedCompletionAt" TIMESTAMP(3),
    "acceptedCompletionAt" TIMESTAMP(3),
    "closedAt" TIMESTAMP(3),
    "lastActorUserId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "RemedyIssue_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "HandoverEvent" (
    "id" TEXT NOT NULL,
    "passportId" TEXT NOT NULL,
    "actorUserId" TEXT,
    "eventType" "HandoverEventType" NOT NULL,
    "entityType" TEXT,
    "entityId" TEXT,
    "publicMetadata" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "HandoverEvent_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "HandoverPassport_listingId_idx" ON "HandoverPassport"("listingId");

-- CreateIndex
CREATE INDEX "HandoverParticipant_passportId_idx" ON "HandoverParticipant"("passportId");

-- CreateIndex
CREATE INDEX "HandoverParticipant_userId_idx" ON "HandoverParticipant"("userId");

-- CreateIndex
CREATE UNIQUE INDEX "HandoverParticipant_passportId_userId_key" ON "HandoverParticipant"("passportId", "userId");

-- CreateIndex
CREATE INDEX "Promise_passportId_currentStatus_idx" ON "Promise"("passportId", "currentStatus");

-- CreateIndex
CREATE INDEX "Promise_passportId_createdAt_idx" ON "Promise"("passportId", "createdAt");

-- CreateIndex
CREATE INDEX "PromiseRevision_promiseId_createdAt_idx" ON "PromiseRevision"("promiseId", "createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "PromiseRevision_promiseId_revisionNumber_key" ON "PromiseRevision"("promiseId", "revisionNumber");

-- CreateIndex
CREATE INDEX "HandoverEvidence_passportId_createdAt_idx" ON "HandoverEvidence"("passportId", "createdAt");

-- CreateIndex
CREATE INDEX "HandoverEvidence_promiseId_idx" ON "HandoverEvidence"("promiseId");

-- CreateIndex
CREATE INDEX "HandoverEvidence_remedyId_idx" ON "HandoverEvidence"("remedyId");

-- CreateIndex
CREATE INDEX "RemedyIssue_passportId_status_idx" ON "RemedyIssue"("passportId", "status");

-- CreateIndex
CREATE INDEX "RemedyIssue_linkedPromiseId_idx" ON "RemedyIssue"("linkedPromiseId");

-- CreateIndex
CREATE INDEX "HandoverEvent_passportId_createdAt_idx" ON "HandoverEvent"("passportId", "createdAt");

-- CreateIndex
CREATE INDEX "HandoverEvent_eventType_createdAt_idx" ON "HandoverEvent"("eventType", "createdAt");

-- AddForeignKey
ALTER TABLE "HandoverPassport" ADD CONSTRAINT "HandoverPassport_listingId_fkey" FOREIGN KEY ("listingId") REFERENCES "Listing"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "HandoverParticipant" ADD CONSTRAINT "HandoverParticipant_passportId_fkey" FOREIGN KEY ("passportId") REFERENCES "HandoverPassport"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Promise" ADD CONSTRAINT "Promise_passportId_fkey" FOREIGN KEY ("passportId") REFERENCES "HandoverPassport"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PromiseRevision" ADD CONSTRAINT "PromiseRevision_promiseId_fkey" FOREIGN KEY ("promiseId") REFERENCES "Promise"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "HandoverEvidence" ADD CONSTRAINT "HandoverEvidence_passportId_fkey" FOREIGN KEY ("passportId") REFERENCES "HandoverPassport"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "HandoverEvidence" ADD CONSTRAINT "HandoverEvidence_promiseId_fkey" FOREIGN KEY ("promiseId") REFERENCES "Promise"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "HandoverEvidence" ADD CONSTRAINT "HandoverEvidence_remedyId_fkey" FOREIGN KEY ("remedyId") REFERENCES "RemedyIssue"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RemedyIssue" ADD CONSTRAINT "RemedyIssue_passportId_fkey" FOREIGN KEY ("passportId") REFERENCES "HandoverPassport"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "RemedyIssue" ADD CONSTRAINT "RemedyIssue_linkedPromiseId_fkey" FOREIGN KEY ("linkedPromiseId") REFERENCES "Promise"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "HandoverEvent" ADD CONSTRAINT "HandoverEvent_passportId_fkey" FOREIGN KEY ("passportId") REFERENCES "HandoverPassport"("id") ON DELETE CASCADE ON UPDATE CASCADE;
