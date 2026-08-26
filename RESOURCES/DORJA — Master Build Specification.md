# DORJA — Claude Code Master Build Specification

> **Audience:** Claude Code or another autonomous coding agent.
>
> **Goal:** Build a competition-ready, Bangladesh-first property discovery platform that transforms a physical **TO-LET / FOR SALE** sign into a live, accountable, interactive property record.
>
> **Critical positioning:** This is **not** a generic property marketplace and not a generic 3D-tour clone. It is a **physical-to-digital property signal network**: live availability, structured 3D-ready evidence, protected negotiation, and accountable appointments.

---

## 0. Agent Instructions — Read This Before Writing Code

Build a working monorepo with a React Native Expo mobile app, a browser web app, and a locally hostable backend.

The project must be demonstrable locally without paid AI credits, without an Android Studio workflow, and without pretending to have unrestricted access to government data.

Use `pnpm` workspaces. Use TypeScript strictly. Use a local Docker stack for PostgreSQL, Redis, and MinIO-compatible storage. Use Expo Application Services for Android builds when native development builds are required; do not require the team to open Android Studio or edit Gradle manually.

Do not invent customer reviews, ratings, testimonials, fake transaction history, or fake verification claims. Sample properties must be marked **Demo Listing** in development data. Never state that a person has a criminal record, is criminally safe, owns a property, or has legal title unless the product state explicitly supports only the limited wording defined below.

Do not claim that ordinary phones create survey-grade 3D models. The default capture is a **guided, navigable room-tour capture** with provenance. LiDAR/advanced spatial scans are optional enhancements on supported devices.

The exact address, phone number, raw NID, raw identity document, and raw property document must never be visible publicly. The system must not implement a universal “background check” feature. It must implement accountable identity/authority verification and an auditable viewing appointment protocol.

Use the future user-provided logo from:

```text
/Assets/logo.png
```

Treat this image as a transparent PNG mark. Do not create a substitute logo. If it is absent, render a typographic fallback reading `DORJA` in the documented display font and show a development warning in the console only.

---

## 1. Product Definition

### 1.1 Working name

**DORJA** is the working product name. “Dorja” means “door” in Bangla. The team may rename it later, but the initial implementation should use `DORJA` consistently in user-facing strings and app identifiers.

### 1.2 One-line positioning

**DORJA turns a street-side property sign into a live, explorable, negotiable, and appointment-safe property record.**

### 1.3 The problem

Property discovery in Bangladesh is fragmented across physical TO-LET signs, brokers, informal calls, social posts, and listing portals. A buyer can waste time contacting properties that are gone, receive flattering but incomplete images, negotiate verbally with no clear record, or enter a private viewing without a traceable appointment. Owners and caretakers also face safety risk when anonymous strangers request access to a private property.

### 1.4 The solution

DORJA combines six connected systems:

1. **Sign-to-Space:** QR/property IDs on a physical sign open the right digital property record.
2. **Live Pulse:** a lister reconfirms that a property is available; stale listings become visibly unconfirmed rather than silently remaining online.
3. **Reality Passport:** guided phone capture produces a browser-navigable room tour with capture date, coverage, review level, and condition notes.
4. **Twin View:** a buyer compares equivalent rooms across shortlisted properties.
5. **Offer Room:** structured rent/sale counter-offers replace confused, unrecorded negotiation calls.
6. **SafeView:** protected messaging, address release only after appointment confirmation, one-time viewing passes, check-in/out, and safety reporting.

### 1.5 Not in scope for the FIRSO MVP

The MVP must not attempt to do the following:

- nationwide inventory ingestion;
- automated property title verification;
- universal criminal-record screening;
- automatic police dispatch;
- survey-grade measurements on every Android phone;
- fully automatic Matterport-quality mesh reconstruction;
- financial escrow, deposit collection, or mortgage approval;
- a public social feed, ratings, or reviews;
- hidden ranking algorithms that discriminate by religion, gender, income, family status, or location.

---

## 2. Brand System

### 2.1 Design movement

Use **Civic Wayfinding × Dhaka Print Ephemera**. The interface should feel like a trustworthy public service and a refined property tool, not a glossy luxury broker site, neon startup dashboard, or generic SaaS template.

The visual language should use strong directional labels, map-like spatial lines, stamped status labels, clipped corners, and paper-like neutrals. The product needs to feel practical enough for a caretaker with a budget Android phone and polished enough to impress a FIRSO judge.

### 2.2 Brand personality

| Trait | Meaning in UI and copy |
|---|---|
| Accountable | Show dates, status, coverage, and limits clearly. |
| Direct | Use short, unambiguous actions. Avoid marketing filler. |
| Neighbourly | Use calm, accessible Bangla/English microcopy. |

### 2.3 Brand voice

Use clear, specific language.

Good:

> “Available — reconfirmed 2 hours ago.”

> “Your viewing address unlocks after both sides confirm.”

Bad:

> “Find your dream home today.”

> “Experience the future of real estate.”

### 2.4 Core colour palette

The signature colour is **Jol Teal**. It should be unmistakably DORJA and used for verified action, active navigation, capture guidance, and trusted controls.

| Token | Hex | Use |
|---|---:|---|
| `ink-950` | `#0B1F33` | Main text, dark navigation, high-contrast headers. |
| `ink-800` | `#17324D` | Secondary dark surfaces. |
| `jol-700` | `#006B68` | Deep teal hover and active state. |
| `jol-600` | `#007C78` | **Signature DORJA colour.** Primary buttons, routes, trusted status. |
| `jol-100` | `#D7F1EE` | Soft teal backgrounds and selected cards. |
| `paper-50` | `#FBF8F2` | Primary warm page background. |
| `paper-100` | `#F2EDE3` | Card background, route panels. |
| `sand-300` | `#D9CCB9` | Dividers, map outlines, disabled states. |
| `amber-500` | `#E79C2E` | Pending appointment, route checkpoint, notice. |
| `amber-100` | `#FCE8BE` | Soft warning background. |
| `leaf-600` | `#267450` | Confirmed availability and successful check-in. |
| `red-600` | `#B83D37` | Critical error, blocked/restricted state, safety concern. |
| `red-100` | `#F8DDD9` | Soft critical background. |
| `sky-500` | `#3D86B9` | Information and map/location accents only. |

Do not use a purple gradient. Do not use an all-white page with interchangeable rounded cards. Do not use Inter as the primary font.

### 2.5 Typography

| Context | Font | Weight | Rule |
|---|---|---:|---|
| English display | `Space Grotesk` | 600–700 | Used for short headings and numeric price blocks. |
| English body | `IBM Plex Sans` | 400–600 | Used for forms, tables, and interface body text. |
| Bangla | `Hind Siliguri` | 400–700 | Used for all Bangla labels and sentences. |
| Numeric/time | `IBM Plex Mono` | 500–600 | Used for confirmation timestamps, status IDs, and capture data. |

Use `font-display: swap` on the web. Provide appropriate local/system fallbacks for mobile.

### 2.6 Logo usage

Use `/Assets/logo.png` in the following locations:

- mobile launch/header mark at 40–48 px visual width;
- website header at 44–56 px visual width;
- QR sign card at 56–72 px visual width;
- app icon/splash only after an appropriate icon crop is supplied;
- never shrink it into a 12 px decorative glyph.

### 2.7 Shape and motion

Use a `10 px` corner radius for ordinary cards and `2 px` radius for stamped status labels. Use one clipped-corner motif in the top-right of capture route panels and safety pass cards.

Motion must be subtle. Buttons scale to `0.97` on press. Panels enter with 180–240 ms opacity/translation transitions. Respect reduced-motion settings. Do not animate a heavy 3D tour while the user is navigating forms.

---

## 3. User Roles and Permissions

### 3.1 Roles

| Role | Main job | Can create listing | Can capture | Can make offers | Can schedule viewing | Can verify check-in |
|---|---|---:|---:|---:|---:|---:|
| Guest | Browse public properties | No | No | No | No | No |
| Seeker | Buyer or renter | No | No | Yes | Yes | No |
| Owner | Property owner | Yes | Yes | Receives/creates counter-offers | Yes | Yes |
| Agent | Owner-authorised marketing agent | Yes, with authority link | Yes | Receives/creates counter-offers | Yes | Yes |
| Representative | Caretaker/building representative | Only invited listings | Yes | No, unless permission granted | Can host | Yes |
| Guard | Building gate/host scanner | No | No | No | No | Yes |
| Reviewer | Platform verification staff | No | No | No | No | No |
| Admin | System operator | Yes | No | No | No | No |

### 3.2 Permission rules

All permission checks must happen server-side. The client may hide a UI control, but hiding is never sufficient authorisation.

1. Only an `OWNER`, `AGENT`, or `REPRESENTATIVE` with a listing authority relation may edit a listing.
2. Only the assigned lister/representative may create capture sessions for a listing.
3. Only an `IDENTITY_CONFIRMED` seeker may request a SafeView appointment.
4. A property must have `LISTING_AUTHORITY_REVIEWED` before public publishing.
5. Exact address data is returned only to confirmed appointment participants during the configured reveal window.
6. A viewing pass is server-issued, single-use, and time-limited.

---

## 4. Technology Architecture

### 4.1 Required stack

| Area | Technology | Reason |
|---|---|---|
| Package manager | pnpm workspaces | Fast, consistent monorepo management. |
| Mobile | Expo SDK + React Native + Expo Router + TypeScript | No manual Android Studio workflow for normal development. |
| Web | Next.js App Router + TypeScript | Strong browser experience for discovery and tours. |
| API | Fastify + TypeScript + Zod + OpenAPI | Fast local API, schema-first validation. |
| Database | PostgreSQL + Prisma | Reliable relational data for workflow state. |
| Realtime | Socket.IO | Messaging, offer updates, booking changes. |
| Queue | BullMQ + Redis | Media processing, reminders, expiry jobs. |
| Local storage | MinIO | S3-compatible media storage local to the team. |
| Web 3D/panorama | Marzipano or Pannellum | Mature browser panorama navigation without inventing a renderer. |
| QR | `qrcode` and `expo-camera` | Sign-to-Space and one-time viewing passes. |
| Maps | Google Maps deep links in MVP; provider adapter for a Map SDK later | Avoid hard dependency on paid keys while keeping interactive map links. |
| Tests | Vitest, Supertest, Playwright | Unit, API, and web end-to-end coverage. |
| Formatting | Biome or ESLint + Prettier | Consistent agent-generated code. |

### 4.2 Why Expo is required

The team does not want to manage Android Studio or Gradle. Use Expo managed workflow and EAS cloud builds:

```bash
pnpm exec eas build --platform android --profile development
```

The app still has native Android code under the hood, but cloud builds avoid manually using Android Studio. Start in Expo Go for JavaScript-only capture flow proofing. If a native AR module is later required, create an Expo development build through EAS. Do not migrate to a bare Android project for the FIRSO MVP. [1] [2]

### 4.3 Repository layout

```text
dorja/
├── Assets/
│   └── logo.png                    # User supplies this. Do not replace it.
├── apps/
│   ├── mobile/                     # Expo React Native app
│   │   ├── app/
│   │   │   ├── (auth)/
│   │   │   ├── (tabs)/
│   │   │   ├── capture/
│   │   │   ├── listing/
│   │   │   ├── safeview/
│   │   │   └── _layout.tsx
│   │   ├── components/
│   │   ├── features/
│   │   ├── lib/
│   │   ├── app.config.ts
│   │   └── eas.json
│   ├── web/                        # Next.js app
│   │   ├── app/
│   │   │   ├── page.tsx
│   │   │   ├── explore/
│   │   │   ├── properties/[slug]/
│   │   │   ├── compare/
│   │   │   ├── inbox/
│   │   │   └── safeview/
│   │   ├── components/
│   │   └── public/
│   └── api/                        # Fastify backend
│       ├── src/
│       │   ├── modules/
│       │   ├── plugins/
│       │   ├── jobs/
│       │   └── server.ts
│       └── prisma/
├── packages/
│   ├── contracts/                  # Zod API contracts and shared types
│   ├── domain/                     # State machines, permissions, policies
│   ├── ui-tokens/                  # Colours, typography, spacing
│   └── config/                     # ESLint/TS configs
├── infra/
│   ├── docker-compose.yml
│   └── minio/
├── docs/
│   ├── SECURITY.md
│   ├── DEMO_RUNBOOK.md
│   └── API.md
├── pnpm-workspace.yaml
├── package.json
└── README.md
```

### 4.4 Root package scripts

Implement equivalent commands:

```json
{
  "scripts": {
    "dev": "concurrently -n api,web,mobile \"pnpm --filter @dorja/api dev\" \"pnpm --filter @dorja/web dev\" \"pnpm --filter @dorja/mobile start\"",
    "dev:infra": "docker compose -f infra/docker-compose.yml up -d",
    "dev:infra:down": "docker compose -f infra/docker-compose.yml down",
    "db:migrate": "pnpm --filter @dorja/api prisma:migrate",
    "db:seed": "pnpm --filter @dorja/api seed",
    "test": "pnpm -r test",
    "lint": "pnpm -r lint",
    "typecheck": "pnpm -r typecheck",
    "build": "pnpm -r build"
  }
}
```

### 4.5 Environment variables

Create `.env.example` and never commit actual secrets.

```dotenv
NODE_ENV=development
PORT=4000
DATABASE_URL=postgresql://dorja:dorja@localhost:5432/dorja
REDIS_URL=redis://localhost:6379
MINIO_ENDPOINT=localhost
MINIO_PORT=9000
MINIO_ACCESS_KEY=change-me
MINIO_SECRET_KEY=change-me
MINIO_BUCKET=dorja-media
JWT_ACCESS_SECRET=replace-with-long-random-secret
JWT_REFRESH_SECRET=replace-with-long-random-secret
WEB_ORIGIN=http://localhost:3000
EXPO_PUBLIC_API_URL=http://YOUR_LAN_IP:4000
MAPS_MODE=deep-link
IDENTITY_PROVIDER=manual-review
IDENTITY_PROVIDER_BASE_URL=
IDENTITY_PROVIDER_CLIENT_ID=
IDENTITY_PROVIDER_CLIENT_SECRET=
SMS_PROVIDER=console
SAFETY_ALERT_PROVIDER=console
```

Do not put an NID API key, government credential, or production SMS secret in a mobile bundle.

---

## 5. Data Model

### 5.1 Global conventions

Use UUID primary keys. Store all instants in UTC. Convert user-visible dates and appointment time slots to `Asia/Dhaka`. Use `createdAt`, `updatedAt`, and `deletedAt` where appropriate.

Store sensitive values encrypted at rest or as provider references. Store a hash of one-time security tokens, never a raw token after issuing it.

### 5.2 Core enums

```ts
export enum UserRole {
  SEEKER = 'SEEKER',
  OWNER = 'OWNER',
  AGENT = 'AGENT',
  REPRESENTATIVE = 'REPRESENTATIVE',
  GUARD = 'GUARD',
  REVIEWER = 'REVIEWER',
  ADMIN = 'ADMIN',
}

export enum IdentityStatus {
  UNVERIFIED = 'UNVERIFIED',
  PHONE_CONFIRMED = 'PHONE_CONFIRMED',
  IDENTITY_PENDING = 'IDENTITY_PENDING',
  IDENTITY_CONFIRMED = 'IDENTITY_CONFIRMED',
  IDENTITY_REJECTED = 'IDENTITY_REJECTED',
  IDENTITY_EXPIRED = 'IDENTITY_EXPIRED',
}

export enum ListingStatus {
  DRAFT = 'DRAFT',
  AUTHORITY_REVIEW_PENDING = 'AUTHORITY_REVIEW_PENDING',
  ACTIVE = 'ACTIVE',
  VIEWING_HELD = 'VIEWING_HELD',
  UNCONFIRMED = 'UNCONFIRMED',
  RENTED_OR_SOLD = 'RENTED_OR_SOLD',
  PAUSED = 'PAUSED',
  RESTRICTED = 'RESTRICTED',
  ARCHIVED = 'ARCHIVED',
}

export enum RealityReviewLevel {
  INCOMPLETE = 'INCOMPLETE',
  SELLER_CAPTURED = 'SELLER_CAPTURED',
  AGENT_VERIFIED = 'AGENT_VERIFIED',
  EXPIRED = 'EXPIRED',
}

export enum OfferStatus {
  DRAFT = 'DRAFT',
  SENT = 'SENT',
  COUNTERED = 'COUNTERED',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  EXPIRED = 'EXPIRED',
  WITHDRAWN = 'WITHDRAWN',
}

export enum ViewingStatus {
  REQUESTED = 'REQUESTED',
  PROPOSED = 'PROPOSED',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
  CHECKED_IN = 'CHECKED_IN',
  COMPLETED = 'COMPLETED',
  SAFETY_FOLLOW_UP = 'SAFETY_FOLLOW_UP',
}
```

### 5.3 Essential Prisma entities

Implement models equivalent to the following. Use field names consistently between database, API, and client.

```prisma
model User {
  id                 String            @id @default(uuid())
  phoneHash          String            @unique
  phoneLast4         String
  displayName        String
  avatarUrl          String?
  primaryRole        UserRole
  identityStatus     IdentityStatus    @default(UNVERIFIED)
  identityVerifiedAt DateTime?
  identityExpiresAt  DateTime?
  createdAt          DateTime          @default(now())
  updatedAt          DateTime          @updatedAt
  listings           Listing[]         @relation("ListingOwner")
  messages           Message[]
  offersSent         Offer[]           @relation("OfferSender")
  offersReceived     Offer[]           @relation("OfferRecipient")
  visitsAsSeeker     Viewing[]         @relation("ViewingSeeker")
  visitsAsHost       Viewing[]         @relation("ViewingHost")
}

model Listing {
  id                    String        @id @default(uuid())
  slug                  String        @unique
  ownerId               String
  owner                 User          @relation("ListingOwner", fields: [ownerId], references: [id])
  title                 String
  intent                ListingIntent
  propertyType          PropertyType
  status                ListingStatus @default(DRAFT)
  publicArea            String
  exactAddressEncrypted Bytes?
  exactLatEncrypted     Bytes?
  exactLngEncrypted     Bytes?
  approximateLat        Decimal?      @db.Decimal(9, 6)
  approximateLng        Decimal?      @db.Decimal(9, 6)
  mapsLink              String?
  priceAmount           Int
  currency              String        @default("BDT")
  livePulseAt           DateTime?
  livePulseExpiresAt    DateTime?
  authorityStatus       AuthorityStatus @default(PENDING)
  authorityExpiresAt    DateTime?
  createdAt             DateTime      @default(now())
  updatedAt             DateTime      @updatedAt
  rooms                 Room[]
  captureSessions       CaptureSession[]
  realityPassports      RealityPassport[]
  offers                Offer[]
  viewings              Viewing[]
}

model CaptureSession {
  id                 String        @id @default(uuid())
  listingId          String
  listing            Listing       @relation(fields: [listingId], references: [id])
  capturedByUserId   String
  routeVersion       Int
  status             CaptureStatus @default(IN_PROGRESS)
  coverageScore      Int           @default(0)
  startedAt          DateTime      @default(now())
  submittedAt        DateTime?
  captureTimestamp   DateTime?
  metadataJson       Json?
  rooms              Room[]
  mediaAssets        MediaAsset[]
}

model Room {
  id               String          @id @default(uuid())
  listingId        String
  captureSessionId String?
  roomType         RoomType
  displayName      String
  ordinal          Int
  captureSession   CaptureSession? @relation(fields: [captureSessionId], references: [id])
  listing          Listing         @relation(fields: [listingId], references: [id])
  tourNode         TourNode?
}

model MediaAsset {
  id               String        @id @default(uuid())
  captureSessionId String
  captureSession   CaptureSession @relation(fields: [captureSessionId], references: [id])
  storageKey       String        @unique
  sha256           String
  mimeType         String
  width            Int?
  height           Int?
  capturedAt       DateTime?
  qualityStatus    MediaQualityStatus @default(PENDING)
  sourceType       MediaSourceType
  createdAt        DateTime      @default(now())
}

model Viewing {
  id                  String        @id @default(uuid())
  listingId           String
  listing             Listing       @relation(fields: [listingId], references: [id])
  seekerId            String
  seeker              User          @relation("ViewingSeeker", fields: [seekerId], references: [id])
  hostId              String
  host                User          @relation("ViewingHost", fields: [hostId], references: [id])
  status              ViewingStatus @default(REQUESTED)
  startsAt            DateTime?
  endsAt              DateTime?
  attendeeCount       Int           @default(1)
  companionName       String?
  addressRevealAt     DateTime?
  exactAddressViewedAt DateTime?
  seekerCheckedInAt   DateTime?
  hostCheckedInAt     DateTime?
  seekerCheckedOutAt  DateTime?
  hostCheckedOutAt    DateTime?
  createdAt           DateTime      @default(now())
  updatedAt           DateTime      @updatedAt
  pass                ViewingPass?
  safetyEvents        SafetyEvent[]
}
```

Complete the schema with models for `AuthorityReview`, `IdentityVerification`, `Conversation`, `Message`, `Offer`, `OfferTerm`, `AvailabilitySlot`, `ViewingPass`, `SafetyEvent`, `SafetyReport`, `Notification`, `AuditLog`, `TourNode`, and `TourEdge`.

### 5.4 Data minimisation rules

1. Do not store raw NID values in `User`.
2. Do not expose identity document images to a property counterpart.
3. Do not place exact address in listing search API responses.
4. Do not log raw viewing-pass values.
5. Do not include sensitive values in websocket events.
6. Do not return safety reports to the reported user while an investigation is active.

### 5.5 Full database implementation contract

The system must use one local PostgreSQL database for transactional state. Prisma is the application ORM, but the schema must also include manually written SQL migrations for extensions, partial indexes, read-only public views, and row-level restrictions where appropriate.

Enable these PostgreSQL extensions in the first migration:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

Use `citext` for normalised text where case-insensitive matching is required. Use `pg_trgm` only for public approximate-area/property search. Never create a trigram index over encrypted address, identity, or safety fields.

#### 5.5.1 Required Prisma enums

Add all missing enums explicitly. Do not keep crucial workflow states as unconstrained strings.

```prisma
enum ListingIntent {
  RENT
  SALE
}

enum PropertyType {
  APARTMENT
  HOUSE
  ROOM
  SUBLET
  HOSTEL_SEAT
  OFFICE
  SHOP
  LAND
}

enum AuthorityStatus {
  NOT_STARTED
  PENDING
  REVIEWED
  REJECTED
  EXPIRED
}

enum AuthorityRole {
  OWNER
  AUTHORISED_AGENT
  BUILDING_REPRESENTATIVE
}

enum CaptureStatus {
  IN_PROGRESS
  UPLOADING
  PROCESSING
  NEEDS_RETAKE
  READY_FOR_REVIEW
  PUBLISHED_SELLER_CAPTURED
  PUBLISHED_AGENT_VERIFIED
  EXPIRED
  ABANDONED
}

enum RoomType {
  ENTRY
  LIVING_ROOM
  DINING_ROOM
  BEDROOM
  KITCHEN
  BATHROOM
  BALCONY
  UTILITY
  PARKING
  OTHER
}

enum MediaQualityStatus {
  PENDING
  ACCEPTED
  RETAKE_SUGGESTED
  REJECTED
}

enum MediaSourceType {
  HOLD_TO_CAPTURE
  GUIDED_SEQUENCE
  IMPORTED_PANORAMA
  PRO_SPATIAL_SCAN
  PROOF_REQUEST
}

enum VerificationProviderType {
  MANUAL_REVIEW
  AUTHORISED_NID_PROVIDER
  DISABLED
}

enum VerificationReviewStatus {
  NOT_STARTED
  PENDING
  CONFIRMED
  REJECTED
  EXPIRED
  CANCELLED
}

enum EvidenceKind {
  IDENTITY_DOCUMENT
  SELFIE_OR_LIVENESS_RESULT
  PROPERTY_AUTHORITY_DOCUMENT
  OWNER_AUTHORISATION
  AGENCY_BUSINESS_DOCUMENT
  BUILDING_MANAGER_CONFIRMATION
  OPTIONAL_PCC
}

enum EvidenceReviewStatus {
  UPLOADED
  UNDER_REVIEW
  ACCEPTED
  REJECTED
  EXPIRED
  RETENTION_PURGED
}

enum ConversationStatus {
  ACTIVE
  BLOCKED
  ARCHIVED
}

enum MessageKind {
  TEXT
  SYSTEM_NOTICE
  PROOF_REQUEST
  PROOF_RESPONSE
  OFFER_CARD
  VIEWING_CARD
}

enum NotificationChannel {
  IN_APP
  PUSH
  SMS
  CONSOLE
}

enum NotificationStatus {
  PENDING
  SENT
  FAILED
  CANCELLED
}

enum SafetyEventType {
  ADDRESS_REVEALED
  PASS_VIEWED
  PASS_SCANNED
  SEEKER_CHECKED_IN
  HOST_CHECKED_IN
  SEEKER_CHECKED_OUT
  HOST_CHECKED_OUT
  CHECKOUT_REMINDER_SENT
  TRUSTED_CONTACT_ALERTED
  SAFETY_CONCERN_REPORTED
  APPOINTMENT_CANCELLED
}

enum SafetyReportCategory {
  SUSPICIOUS_BEHAVIOUR
  FALSE_LISTING
  HARASSMENT
  ADDRESS_MISUSE
  NO_SHOW
  IMMEDIATE_DANGER
  OTHER
}

enum SafetyReportStatus {
  OPEN
  TRIAGED
  ACTION_TAKEN
  CLOSED
}

enum ViewingPassStatus {
  ISSUED
  VIEWED
  CHECKED_IN
  INVALIDATED
  EXPIRED
}

enum AuditActorType {
  USER
  SYSTEM
  REVIEWER
  ADMIN
}
```

#### 5.5.2 Full sensitive-data models

The following models are mandatory. They isolate personally identifiable information from public product rows and let the application purge or rotate sensitive data without destroying the listing workflow.

```prisma
model IdentityVerification {
  id                   String                   @id @default(uuid())
  userId               String
  user                 User                     @relation(fields: [userId], references: [id], onDelete: Cascade)
  providerType         VerificationProviderType
  status               VerificationReviewStatus @default(NOT_STARTED)
  consentVersion       String
  consentedAt          DateTime
  providerReferenceEnc Bytes?
  verifiedNameEnc      Bytes?
  portraitAssetId      String?
  verificationDataEnc  Bytes?
  reviewerId           String?
  reviewerNoteEnc      Bytes?
  verifiedAt           DateTime?
  expiresAt            DateTime?
  rejectedAt           DateTime?
  createdAt            DateTime                 @default(now())
  updatedAt            DateTime                 @updatedAt

  @@index([userId, status])
  @@index([status, expiresAt])
}

model SensitiveBlob {
  id                  String   @id @default(uuid())
  subjectType         String
  subjectId           String
  fieldName           String
  keyVersion          Int
  encryptedDek        Bytes
  iv                  Bytes
  ciphertext          Bytes
  authTag             Bytes
  aadVersion          Int      @default(1)
  createdAt           DateTime @default(now())
  rotatedAt           DateTime?
  purgeAfter          DateTime?
  purgedAt            DateTime?

  @@unique([subjectType, subjectId, fieldName])
  @@index([purgeAfter])
}

model PrivateEvidence {
  id                  String               @id @default(uuid())
  userId              String
  listingId           String?
  kind                EvidenceKind
  status              EvidenceReviewStatus @default(UPLOADED)
  storageKeyEncrypted Bytes
  fileSha256          String
  fileMimeType        String
  fileSizeBytes       Int
  uploadExpiresAt     DateTime?
  reviewedByUserId    String?
  reviewedAt          DateTime?
  reviewNoteEnc       Bytes?
  retentionUntil      DateTime
  purgedAt            DateTime?
  createdAt           DateTime             @default(now())
  updatedAt           DateTime             @updatedAt

  @@index([userId, kind, status])
  @@index([listingId, kind, status])
  @@index([retentionUntil])
}

model AuthorityReview {
  id                  String          @id @default(uuid())
  listingId           String
  listing             Listing         @relation(fields: [listingId], references: [id], onDelete: Cascade)
  applicantUserId     String
  authorityRole       AuthorityRole
  status              AuthorityStatus @default(PENDING)
  reviewerId          String?
  reviewedAt          DateTime?
  expiresAt           DateTime?
  privateSummaryEnc   Bytes?
  publicLabel         String
  createdAt           DateTime        @default(now())
  updatedAt           DateTime        @updatedAt

  @@index([listingId, status])
  @@index([applicantUserId, status])
}

model TrustedContact {
  id                  String   @id @default(uuid())
  ownerUserId         String
  displayNameEnc      Bytes
  phoneEncrypted      Bytes
  phoneLast4          String
  relationshipEnc     Bytes?
  verifiedAt          DateTime?
  isActive            Boolean  @default(true)
  createdAt           DateTime @default(now())
  updatedAt           DateTime @updatedAt

  @@index([ownerUserId, isActive])
}
```

`SensitiveBlob` is the only generic encrypted column store. Do not use it for searchable/filterable public business fields. Its purpose is exact address, provider references, trusted-contact phone numbers, internal reviewer notes, and encrypted incident payloads.

#### 5.5.3 Complete transactional workflow models

```prisma
model AvailabilitySlot {
  id                  String   @id @default(uuid())
  listingId           String
  hostUserId          String
  startsAt            DateTime
  endsAt              DateTime
  capacity            Int      @default(1)
  confirmedCount      Int      @default(0)
  isActive            Boolean  @default(true)
  createdAt           DateTime @default(now())
  updatedAt           DateTime @updatedAt

  @@index([listingId, startsAt, isActive])
  @@index([hostUserId, startsAt, isActive])
}

model Conversation {
  id                  String             @id @default(uuid())
  listingId           String
  seekerUserId        String
  hostUserId          String
  status              ConversationStatus @default(ACTIVE)
  lastMessageAt       DateTime?
  createdAt           DateTime           @default(now())
  updatedAt           DateTime           @updatedAt
  messages            Message[]

  @@unique([listingId, seekerUserId, hostUserId])
  @@index([seekerUserId, lastMessageAt])
  @@index([hostUserId, lastMessageAt])
}

model Message {
  id                  String      @id @default(uuid())
  conversationId      String
  conversation        Conversation @relation(fields: [conversationId], references: [id], onDelete: Cascade)
  senderUserId        String
  kind                MessageKind
  bodyEncrypted       Bytes?
  safePreview         String?
  relatedEntityType   String?
  relatedEntityId     String?
  createdAt           DateTime    @default(now())
  editedAt            DateTime?
  deletedAt           DateTime?

  @@index([conversationId, createdAt])
}

model Offer {
  id                  String      @id @default(uuid())
  listingId           String
  conversationId      String
  senderUserId        String
  recipientUserId     String
  currentVersion      Int         @default(1)
  status              OfferStatus @default(DRAFT)
  expiresAt           DateTime?
  acceptedAt          DateTime?
  declinedAt          DateTime?
  withdrawnAt         DateTime?
  createdAt           DateTime    @default(now())
  updatedAt           DateTime    @updatedAt
  versions            OfferVersion[]

  @@index([listingId, status])
  @@index([recipientUserId, status])
  @@index([expiresAt, status])
}

model OfferVersion {
  id                  String   @id @default(uuid())
  offerId             String
  offer               Offer    @relation(fields: [offerId], references: [id], onDelete: Cascade)
  version             Int
  createdByUserId     String
  termsJson           Json
  noteEncrypted       Bytes?
  createdAt           DateTime @default(now())

  @@unique([offerId, version])
}

model ViewingPass {
  id                  String            @id @default(uuid())
  viewingId           String            @unique
  viewing             Viewing           @relation(fields: [viewingId], references: [id], onDelete: Cascade)
  tokenHash           String            @unique
  tokenVersion        Int               @default(1)
  status              ViewingPassStatus @default(ISSUED)
  issuedAt            DateTime          @default(now())
  viewedAt            DateTime?
  checkedInAt         DateTime?
  invalidatedAt       DateTime?
  expiresAt           DateTime
  scanCount           Int               @default(0)
  lastScannedByUserId String?
  createdAt           DateTime          @default(now())
  updatedAt           DateTime          @updatedAt

  @@index([expiresAt, status])
}

model SafetyEvent {
  id                  String          @id @default(uuid())
  viewingId           String
  viewing             Viewing         @relation(fields: [viewingId], references: [id], onDelete: Cascade)
  actorUserId         String?
  eventType           SafetyEventType
  occurredAt          DateTime        @default(now())
  metadataEncrypted   Bytes?
  createdAt           DateTime        @default(now())

  @@index([viewingId, occurredAt])
  @@index([eventType, occurredAt])
}

model SafetyReport {
  id                  String             @id @default(uuid())
  viewingId           String?
  listingId           String?
  reporterUserId      String
  reportedUserId      String?
  category            SafetyReportCategory
  status              SafetyReportStatus @default(OPEN)
  descriptionEnc      Bytes
  evidenceAssetIdsEnc Bytes?
  triagedByUserId     String?
  triagedAt           DateTime?
  resolutionEnc       Bytes?
  createdAt           DateTime           @default(now())
  updatedAt           DateTime           @updatedAt

  @@index([status, createdAt])
  @@index([reporterUserId, createdAt])
}

model Notification {
  id                  String             @id @default(uuid())
  userId              String
  channel             NotificationChannel
  type                String
  payloadJson         Json
  status              NotificationStatus @default(PENDING)
  scheduledFor        DateTime?
  sentAt              DateTime?
  failureReason       String?
  dedupeKey           String?
  createdAt           DateTime           @default(now())

  @@unique([userId, dedupeKey])
  @@index([status, scheduledFor])
}

model AuditLog {
  id                  String         @id @default(uuid())
  actorType           AuditActorType
  actorUserId         String?
  action              String
  subjectType         String
  subjectId           String
  requestId           String?
  ipHash              String?
  userAgentHash       String?
  publicMetadataJson  Json?
  eventHash           String         @unique
  previousEventHash   String?
  createdAt           DateTime       @default(now())

  @@index([subjectType, subjectId, createdAt])
  @@index([actorUserId, createdAt])
  @@index([action, createdAt])
}
```

#### 5.5.4 Capture checkpoint database models

The mobile app needs durable records for each guided hold. Do not store only a final video and lose the route evidence.

```prisma
model CaptureRouteTemplate {
  id                  String   @id @default(uuid())
  version             Int      @unique
  propertyType        PropertyType
  displayName         String
  templateJson        Json
  isActive            Boolean  @default(true)
  createdAt           DateTime @default(now())
}

model CaptureRoomProgress {
  id                  String   @id @default(uuid())
  captureSessionId    String
  roomId              String
  ordinal             Int
  requiredCheckpointCount Int
  acceptedCheckpointCount Int @default(0)
  status              String   @default("NOT_STARTED")
  startedAt           DateTime?
  completedAt         DateTime?
  createdAt           DateTime @default(now())
  updatedAt           DateTime @updatedAt

  @@unique([captureSessionId, roomId])
}

model SpatialCheckpoint {
  id                  String   @id @default(uuid())
  captureSessionId    String
  roomId              String
  checkpointKey       String
  ordinal             Int
  expectedDirection   String
  holdDurationMs      Int
  startedAt           DateTime?
  completedAt         DateTime?
  deviceHeadingDeg    Decimal? @db.Decimal(7, 3)
  devicePitchDeg      Decimal? @db.Decimal(7, 3)
  deviceRollDeg       Decimal? @db.Decimal(7, 3)
  stabilityScore      Int?
  brightnessScore     Int?
  blurScore           Int?
  coverageStatus      String   @default("PENDING")
  primaryMediaAssetId String?
  burstAssetIdsJson   Json?
  rejectionReason     String?
  createdAt           DateTime @default(now())
  updatedAt           DateTime @updatedAt

  @@unique([captureSessionId, roomId, checkpointKey])
  @@index([captureSessionId, ordinal])
}

model TourNode {
  id                  String   @id @default(uuid())
  roomId              String   @unique
  panoramaAssetId     String?
  previewAssetId      String
  mapX                Decimal? @db.Decimal(8, 4)
  mapY                Decimal? @db.Decimal(8, 4)
  createdAt           DateTime @default(now())
  updatedAt           DateTime @updatedAt
}

model TourEdge {
  id                  String   @id @default(uuid())
  fromNodeId          String
  toNodeId            String
  doorwayLabel        String
  hotspotYaw          Decimal? @db.Decimal(8, 4)
  hotspotPitch        Decimal? @db.Decimal(8, 4)
  createdAt           DateTime @default(now())

  @@unique([fromNodeId, toNodeId])
}
```

### 5.6 Encryption and local key-management implementation

#### 5.6.1 Threat model

The local backend must protect against accidental data leakage, database backups being copied, ordinary developers browsing raw records, and public API overexposure. It cannot by itself protect against a fully compromised host operating system or a malicious privileged administrator. State this limitation in internal documentation.

#### 5.6.2 Envelope encryption strategy

Use AES-256-GCM envelope encryption for fields that should never be in plaintext in PostgreSQL.

```text
Root Key / KEK
  → encrypts one unique Data Encryption Key (DEK) per sensitive field blob
  → DEK encrypts the field payload with AES-256-GCM
  → database stores encrypted DEK, IV, authentication tag, ciphertext, key version
```

Set this local-development key only through environment configuration:

```dotenv
FIELD_ENCRYPTION_ROOT_KEY_BASE64=replace-with-32-byte-base64-key
FIELD_ENCRYPTION_KEY_VERSION=1
AUDIT_LOG_HMAC_KEY_BASE64=replace-with-separate-32-byte-base64-key
```

Production must obtain the root key from a managed secret store/KMS. For the FIRSO local demo, use a `.env` file outside version control and rotate it before any public deployment.

#### 5.6.3 Fields that must be encrypted

| Entity | Encrypted field | Reason |
|---|---|---|
| `Listing` | exact address, exact latitude, exact longitude | Public discovery must not reveal entry-level location. |
| `IdentityVerification` | provider reference, verified name, provider result | Identity-provider result is sensitive. |
| `PrivateEvidence` | storage object key and reviewer note | Prevent staff/client access to raw document references. |
| `TrustedContact` | name, phone, relationship | Protect emergency contact data. |
| `Message` | full message text | Permit a limited safe preview without storing public plaintext. |
| `OfferVersion` | optional personal note | Terms can stay structured; sensitive note remains private. |
| `SafetyEvent` | event payload | Safety metadata is highly sensitive. |
| `SafetyReport` | description, evidence references, resolution | Prevent retaliation and data exposure. |

Do not encrypt numeric public price, listing category, approximate area, verification status, timestamps, or workflow states because those are required for normal filtering and transparent user display.

#### 5.6.4 Encryption service code shape

Use Node `crypto`. Do not invent cryptography. Keep this service server-only.

```ts
import crypto from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';

export type EncryptedPayload = {
  keyVersion: number;
  encryptedDek: Buffer;
  iv: Buffer;
  ciphertext: Buffer;
  authTag: Buffer;
};

export class FieldEncryptionService {
  constructor(
    private readonly rootKey: Buffer,
    private readonly keyVersion: number,
  ) {
    if (rootKey.byteLength !== 32) throw new Error('FIELD_ENCRYPTION_ROOT_KEY must be 32 bytes');
  }

  encrypt(plaintext: Buffer, aad: string): EncryptedPayload {
    const dek = crypto.randomBytes(32);
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv(ALGORITHM, dek, iv);
    cipher.setAAD(Buffer.from(aad, 'utf8'));
    const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
    const authTag = cipher.getAuthTag();

    const kekIv = crypto.randomBytes(12);
    const kekCipher = crypto.createCipheriv(ALGORITHM, this.rootKey, kekIv);
    kekCipher.setAAD(Buffer.from(`dek:${aad}`, 'utf8'));
    const encryptedDekBody = Buffer.concat([kekCipher.update(dek), kekCipher.final()]);
    const encryptedDek = Buffer.concat([kekIv, kekCipher.getAuthTag(), encryptedDekBody]);

    return { keyVersion: this.keyVersion, encryptedDek, iv, ciphertext, authTag };
  }

  decrypt(payload: EncryptedPayload, aad: string): Buffer {
    const kekIv = payload.encryptedDek.subarray(0, 12);
    const kekAuthTag = payload.encryptedDek.subarray(12, 28);
    const encryptedDekBody = payload.encryptedDek.subarray(28);
    const kekDecipher = crypto.createDecipheriv(ALGORITHM, this.rootKey, kekIv);
    kekDecipher.setAAD(Buffer.from(`dek:${aad}`, 'utf8'));
    kekDecipher.setAuthTag(kekAuthTag);
    const dek = Buffer.concat([kekDecipher.update(encryptedDekBody), kekDecipher.final()]);

    const decipher = crypto.createDecipheriv(ALGORITHM, dek, payload.iv);
    decipher.setAAD(Buffer.from(aad, 'utf8'));
    decipher.setAuthTag(payload.authTag);
    return Buffer.concat([decipher.update(payload.ciphertext), decipher.final()]);
  }
}
```

The additional authenticated data must use a stable, non-secret binding:

```text
dorja:v1:{subjectType}:{subjectId}:{fieldName}
```

This prevents ciphertext from one field being moved to another field without detection.

#### 5.6.5 Passwords, OTPs, and tokens

1. If password login is added later, use Argon2id. Do not use SHA-256 for passwords.
2. Store OTPs as HMAC hashes with short expiry; do not store plain OTP values.
3. Generate viewing-pass tokens using `crypto.randomBytes(32)` or higher entropy.
4. Store only SHA-256 or HMAC hash of viewing-pass raw token.
5. Store refresh tokens hashed and rotate them on each refresh.
6. Use a distinct secret for audit-log HMAC; do not reuse the encryption root key.

### 5.7 Database access model and public-safe views

The API must use a single least-privilege `dorja_app` database role. The mobile and web clients must never connect directly to PostgreSQL. Create a separate `dorja_migrator` role for migrations.

Create a database view or API query DTO for public listings. This view must never select encrypted address, trusted contact, identity, authority evidence, safety report, or raw media private metadata.

```sql
CREATE VIEW public_listing_cards AS
SELECT
  l.id,
  l.slug,
  l.title,
  l.intent,
  l.property_type,
  l.status,
  l.public_area,
  l.approximate_lat,
  l.approximate_lng,
  l.price_amount,
  l.currency,
  l.live_pulse_at,
  l.live_pulse_expires_at,
  l.authority_status,
  rp.review_level AS reality_review_level,
  rp.published_at AS reality_published_at
FROM listings l
LEFT JOIN LATERAL (
  SELECT * FROM reality_passports
  WHERE listing_id = l.id
  ORDER BY published_at DESC
  LIMIT 1
) rp ON TRUE
WHERE l.status IN ('ACTIVE', 'VIEWING_HELD', 'UNCONFIRMED');
```

If row-level security is enabled, use it as defense in depth, not as a replacement for API authorisation. Keep all exact-address retrieval in a dedicated service method that validates confirmed appointment participation and reveal time.

### 5.8 Retention and purge schedule

Implement a scheduled daily purge job. The following is a product default that legal counsel must review before commercial launch.

| Data | Retention default | Purge action |
|---|---|---|
| OTP challenge | 10 minutes | Delete/hash expires. |
| Unfinished capture local cache | 7 days | Delete local/server draft media if unsubmitted. |
| Rejected identity evidence | 30 days | Delete private object and mark evidence purged. |
| Accepted identity evidence | 180 days after verification expiry | Delete private object; retain minimal audit status. |
| Authority evidence | 180 days after authority expiry | Delete private object; retain authority outcome. |
| Exact address encrypted blob | While active listing + 90 days after archive | Purge or anonymise. |
| Safety report payload | 365 days after closure | Purge encrypted payload; retain minimum audit. |
| Appointment record | 24 months | Anonymise PII, retain aggregate operational event. |
| Audit log | 24 months minimum | Retain tamper-evident record unless lawful deletion required. |
| Media from active passport | While listing is active | Delete or archive at listing owner request subject to evidence policy. |

Every purge job must create an `AuditLog` entry but must not duplicate the sensitive content being removed.

### 5.9 Tamper-evident audit implementation

Audit logs are not a blockchain. Use an HMAC hash chain to make unexpected alteration detectable in the local database.

```ts
function computeAuditHash(input: {
  previousEventHash: string | null;
  actorType: string;
  actorUserId: string | null;
  action: string;
  subjectType: string;
  subjectId: string;
  occurredAtIso: string;
  publicMetadataJson: unknown;
}): string {
  const canonical = JSON.stringify(input);
  const auditKey = Buffer.from(process.env.AUDIT_LOG_HMAC_KEY_BASE64!, 'base64');
  return crypto
    .createHmac('sha256', auditKey)
    .update(canonical)
    .digest('hex');
}
```

Use only non-sensitive metadata in the audit payload. Example: `{"role":"SEEKER","reason":"appointment_confirmed"}`. Never insert phone numbers, address, messages, NID values, or report narrative in audit JSON.

---

## 6. Identity, Authority, and Safety Verification

### 6.1 Correct product wording

Never call the feature “mandatory criminal background check.” Use:

> **Identity confirmation and SafeView accountability.**

Why: A national identity verification confirms identity matching, not whether somebody is safe. A Police Clearance Certificate is a separate official citizen process; it is not a public real-time API that a startup should query for every viewer. The official police-clearance portal exists for application workflow and institutional systems may have access restrictions. [5]

### 6.2 Recommended provider strategy

Implement an adapter pattern immediately. Start with manual review in the competition demo. Enable a government/authorised identity provider only after the business has a legitimate contract, consent process, security review, and credentials.

```ts
export interface IdentityProvider {
  name: string;
  startVerification(input: {
    userId: string;
    consentVersion: string;
    redirectUrl?: string;
  }): Promise<{ providerSessionId: string; redirectUrl?: string }>;

  getVerification(providerSessionId: string): Promise<{
    status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'EXPIRED';
    verifiedName?: string;
    portraitUrl?: string;
    providerReference: string;
    expiresAt?: Date;
  }>;
}
```

Implement these providers:

```ts
class ManualReviewIdentityProvider implements IdentityProvider {}
class AuthorisedNidProvider implements IdentityProvider {}
class DisabledIdentityProvider implements IdentityProvider {}
```

`AuthorisedNidProvider` must throw a clear configuration error unless actual approved credentials and a signed integration arrangement are present. Never reverse engineer, scrape, buy random NID APIs, or use unverified API resellers.

### 6.3 NID integration recommendation

If the company becomes eligible to integrate with an authorised Bangladesh NID/Porichoy channel, integrate it **only** through the official/contracted path. Keep its API base URL, credentials, and field mapping server-only behind `IdentityProvider`.

For the FIRSO demo use:

```text
phone OTP simulation
  → explicit user consent screen
  → redacted identity-document demo upload
  → reviewer approves or rejects
  → account shows “Identity confirmed — demo review”
```

The product badge must say **Identity confirmed**, not “Government background checked.”

### 6.4 Police Clearance Certificate policy

Do not make PCC mandatory for all users. It would be disproportionate and impractical. Offer only an optional user-provided assurance field in a future release:

```text
PCC submitted by user — document validity not independently guaranteed
```

Do not expose the certificate file to counterparties. Do not transform it into a “safe person” score. Do not interpret absence of a PCC as suspicion.

### 6.5 Property authority workflow

| Lister type | Required proof state | Public label |
|---|---|---|
| Owner | Identity confirmation + reviewed property evidence | `Owner authority reviewed` |
| Agent | Identity confirmation + agency detail + owner authorisation link | `Authorised agent reviewed` |
| Caretaker/representative | Identity confirmation + owner/manager invitation | `Building representative reviewed` |

The label is property-specific and expires. It must never say “title verified” unless a future legally qualified title process supports that claim.

---

## 7. The Reality Passport and Capture Pipeline

### 7.1 Truthful capture promise

Use this exact framing in the app and pitch:

> “DORJA guides an owner through a repeatable capture route and publishes a navigable record of what was captured, when it was captured, and what remains unseen.”

### 7.2 Capture modes

| Mode | Device | Output | Status |
|---|---|---|---|
| Hold-to-capture spatial checkpoint | Any reasonable Android/iPhone | Guided steady hold, short capture burst, room checkpoint evidence, and door link | **Core MVP** |
| Guided room capture | Any reasonable Android/iPhone | 6–8 images around a room plus door link | Compatibility fallback |
| Imported panorama | Panorama-capable device or existing panorama | Equirectangular panorama for free-look tour | Enhanced MVP |
| Pro spatial scan | Supported LiDAR/advanced device | Optional spatial/floor-plan data | Future enhancement |

Apple RoomPlan is a supported-device LiDAR workflow for room plans; do not depend on it for Android coverage. [4]

### 7.3 Mobile capture screens

#### Screen: `CaptureStartScreen`

Top header: back control, DORJA logo, listing title.

Primary content: a linear route showing room chips in physical order.

Bottom: a large teal `Start hold-to-capture route` button, a secondary `Guided photos` fallback button, and `Import 360 panorama` tertiary button.

Show a 3-point preflight:

1. “Open curtains or switch on lights.”
2. “Stand near the centre of each room.”
3. “Keep faces, personal documents, and valuables out of frame.”

#### Screen: `CaptureRoomScreen`

This is full-screen camera mode.

Overlay elements:

- top-left: `Living room · 3 of 8` in a compact dark translucent panel;
- top-right: torch icon, help icon, exit button;
- centre: a teal horizon guide and a circular target;
- edge ring: eight small angle dots; captured dots become teal;
- bottom: large circular shutter, `Retake` state, and route progress;
- below shutter: `Move slowly. Keep this room in one position.`

The UI must use `expo-camera` for preview, camera permission, photo capture, and QR scanning. It must not depend on a full native AR module for its first demo. Expo Camera supports preview, photos/video, and barcode scanning in Expo-supported apps. [3]

#### Screen: `HoldToCaptureCheckpointScreen`

This screen is the preferred MVP capture mechanic. It is deliberately inspired by spatial-capture experiences, but it avoids claiming inaccessible full AR reconstruction.

The user sees the live camera. The app tells them where to stand and which direction to face. They press and **hold** the large Jol Teal capture control for 1.5–2.0 seconds. During the hold, the app gathers a short frame burst, device orientation readings, light/blur hints, and a primary still image. When the phone remains reasonably stable, the ring completes and the checkpoint locks in.

Visual design:

```text
┌───────────────────────────────────────┐
│ Living room · Centre · 2 / 5     [?]  │
│                                       │
│        ─── horizon guide ───          │
│              ◉                        │
│                                       │
│  Stand here. Face the main wall.      │
│  Hold steady until the ring completes.│
│                                       │
│       [      HOLD TO CAPTURE      ]   │
│       ○ ○ ● ○ ○    Route 2 / 5         │
└───────────────────────────────────────┘
```

During press:

```text
0–350 ms   → require stable camera and acceptable brightness
350–1200 ms → capture a small timed burst / preview frames
1200–1800 ms → select sharpest primary frame and persist checkpoint
success     → haptic success + teal checkpoint dot + “Next: doorway”
failure     → short amber explanation + “Hold more steadily”
```

Use `expo-haptics` for light start feedback and success/error notification feedback. Use `expo-sensors` device motion readings only as a stability hint. The route itself provides the spatial order; do not call the resulting inferred positions real-world centimetre measurements.

### 7.3A Hold-to-Capture Spatial Checkpoint protocol

#### Goal

Make property capture feel intelligent and guided without forcing a user to understand panorama stitching, AR anchors, LiDAR, or 3D modelling.

#### What the system actually analyses

The MVP may analyse these deterministic capture-quality signals:

| Signal | Source | Use |
|---|---|---|
| Hold duration | Press timer | Reject accidental taps. |
| Motion stability | Gyroscope/accelerometer variance while holding | Prompt user to hold steady. |
| Orientation | Device heading/pitch/roll | Record viewpoint label, not survey geometry. |
| Brightness | Camera frame luminance estimate | Prompt for light/torch when too dark. |
| Blur | Local or server Laplacian-variance heuristic | Suggest retake when frame is visibly blurred. |
| Coverage | Route checkpoint completion | Show exactly which room views are missing. |
| Duplicate hint | Perceptual hash/thumbnail comparison | Prevent same photo satisfying multiple checkpoints. |

The system must not claim to identify wall dimensions, property defects, title status, structural safety, or all objects in the room from these signals.

#### Default checkpoint template

For an ordinary rectangular room use five checkpoints:

```text
1. Centre facing main wall
2. Centre facing opposite wall
3. Centre facing window/balcony side
4. Centre facing doorway
5. Doorway looking back into room
```

For kitchen/bathroom use four checkpoints:

```text
1. Entrance overview
2. Main counter/wall
3. Utility/fixture side
4. Doorway exit
```

For balcony use three checkpoints:

```text
1. Balcony entrance
2. Outward view
3. Left/right edge view
```

The template must be data-driven through `CaptureRouteTemplate.templateJson`, so the team can change it without an app release.

#### Honest output names

Use these names in the product:

```text
Spatial Checkpoint
Room Route
Captured View
Coverage Map
Navigable Room Record
```

Do not use these names for the default Android flow:

```text
3D scan
Digital twin
Exact room model
Survey measurement
AR measurement
```

The web experience may look highly immersive through a room graph, preview transitions, and optional panorama. Its product label must still accurately describe the capture source.

#### Expo-compatible implementation outline

```tsx
import { CameraView, useCameraPermissions } from 'expo-camera';
import * as Haptics from 'expo-haptics';
import { DeviceMotion } from 'expo-sensors';

type CheckpointState = 'IDLE' | 'HOLDING' | 'CAPTURING' | 'ACCEPTED' | 'RETAKE';

async function beginHoldCapture() {
  setCheckpointState('HOLDING');
  await Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
  startMotionWindow();
  startHoldTimer();
}

async function completeHoldCapture() {
  const stability = calculateStabilityScore(motionSamplesRef.current);
  if (stability < 60) {
    setCheckpointState('RETAKE');
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    return;
  }

  setCheckpointState('CAPTURING');
  const photo = await cameraRef.current?.takePictureAsync({
    quality: 0.75,
    exif: true,
    skipProcessing: false,
  });

  if (!photo) throw new Error('Capture returned no image');
  await captureApi.createCheckpoint({
    sessionId,
    roomId,
    checkpointKey,
    holdDurationMs: holdDuration(),
    deviceMotionSummary: summarizeMotion(motionSamplesRef.current),
    localUri: photo.uri,
  });

  setCheckpointState('ACCEPTED');
  await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
}
```

The application should avoid trying to keep the camera preview active on multiple mounted screens. Unmount capture screen when it loses focus.

#### Screen: `CaptureReviewScreen`

Show captured thumbnails in a 2-column mosaic. Display:

```text
Living room
Coverage: 7 / 8 points
Quality: 6 accepted · 1 retake suggested
```

The lister can retake only the failed point, mark a point unavailable with a reason, or continue.

#### Screen: `CapturePublishScreen`

Show a vertical `Reality Passport` summary.

```text
Seller-captured
14 Aug 2026 · 16:42
Rooms: 6 / 7 complete
Balcony: not captured
Listing authority: reviewed
```

Buttons:

- `Submit for review`
- `Save as draft`
- `Preview web tour`

### 7.4 Capture state machine

```text
IN_PROGRESS
  → UPLOADING
  → PROCESSING
  → NEEDS_RETAKE
  → READY_FOR_REVIEW
  → PUBLISHED_SELLER_CAPTURED
  → PUBLISHED_AGENT_VERIFIED
  → EXPIRED
```

### 7.5 Processing jobs

Each uploaded file creates a deterministic background job.

```text
media.uploaded
  → validate mime type and file size
  → calculate SHA-256
  → read dimensions / EXIF when available
  → run blur threshold
  → detect duplicate hash / near duplicate
  → assign capture point
  → create preview derivative
  → create panorama tiles if panorama source
  → update route coverage
  → notify uploader
```

Do not label a blur threshold, coverage score, or image checker as AI inspection. It is a media-quality check only.

### 7.5A Hold-to-capture processing pipeline

For each accepted checkpoint, persist immediately, then process asynchronously.

```text
mobile hold begins
  → local motion samples collected
  → primary still captured
  → checkpoint row created as PENDING
  → signed upload URL requested
  → media uploaded to private storage
  → client confirms upload metadata
  → server validates file and computes content hash
  → server calculates basic quality metrics
  → checkpoint becomes ACCEPTED or RETAKE_SUGGESTED
  → room progress recalculated
  → capture-session coverage recalculated
  → Reality Passport preview updated
```

Persist the checkpoint before heavy processing so a crashed or disconnected app can resume. The client should queue unsent capture records in encrypted local storage and retry when connectivity returns.

#### Checkpoint quality policy

| Condition | Status | Client instruction |
|---|---|---|
| Hold under 1.2 seconds | `RETAKE_SUGGESTED` | “Hold until the teal ring completes.” |
| Stability score below threshold | `RETAKE_SUGGESTED` | “Keep the phone still while holding.” |
| Very low brightness | `RETAKE_SUGGESTED` | “Turn on a light or use the torch.” |
| Severe blur | `RETAKE_SUGGESTED` | “Retake this view more slowly.” |
| Same media hash used twice | `REJECTED` | “This view was already used in this room.” |
| Accepted checkpoint | `ACCEPTED` | “Captured. Next: face the doorway.” |

Do not silently discard a checkpoint. Tell the lister why it was rejected in plain language and allow a retry.

#### Spatial-confidence display

The system may calculate a `captureConfidence` score purely as a coverage/quality indicator:

```text
captureConfidence =
  40% route checkpoint coverage +
  25% accepted quality checks +
  20% viewpoint diversity +
  15% doorway connectivity
```

Display it only as:

```text
Capture completeness: 82%
```

Do not label it “3D accuracy,” “truth score,” “home quality,” or “safety score.”

### 7.5B Optional enhanced spatial processing

After the core capture flow works, the local backend may add optional workers:

1. Panorama stitching from intentionally overlapping images.
2. Image-tile generation for a panorama viewer.
3. Doorway-link suggestions derived from the route sequence.
4. On compatible devices, a custom Expo development build that passes optional depth/AR metadata to the server.

Each enhanced output must include `sourceType` and `processingVersion`. Never overwrite the raw source capture. A user must be able to see whether a room is a standard hold-to-capture record, imported panorama, or Pro Spatial Scan.

### 7.6 Tour graph

Represent a home as a graph, not a fake 3D mesh.

```ts
type TourNode = {
  id: string;
  roomId: string;
  roomType: 'LIVING_ROOM' | 'BEDROOM' | 'KITCHEN' | 'BATHROOM' | 'BALCONY' | 'OTHER';
  panoramaAssetUrl?: string;
  previewImageUrl: string;
  x: number;
  y: number;
};

type TourEdge = {
  id: string;
  fromNodeId: string;
  toNodeId: string;
  label: string;
  hotspotYaw?: number;
  hotspotPitch?: number;
};
```

### 7.7 Reality Passport labels

Show one and only one current state:

| Label | Meaning |
|---|---|
| `Incomplete capture` | Required rooms/checkpoints are missing. |
| `Seller-captured` | The lister submitted the capture route. |
| `Agent-verified` | An authorised representative reviewed the on-site capture according to a documented policy. |
| `Capture expired` | Capture date exceeded freshness policy or lister did not reconfirm. |

---

## 8. Web Application — Exact Interface

### 8.1 Overall web layout

Desktop uses an asymmetric layout. Use a 72 px dark ink vertical rail on the left. The body uses warm paper background. The active item is marked by a Jol Teal vertical line and a small text label.

Use a responsive top bar on tablet/mobile web. Do not make the desktop site look like a generic dashboard with six identical cards.

### 8.2 Navigation rail

Items:

```text
Explore
Shortlist
Compare
Inbox
Visits
My Listings
```

Top: logo.

Bottom: language toggle `বাংলা / EN`, account avatar, and `Safety guide` link.

### 8.3 Explore screen

Layout:

```text
┌───────┬───────────────────────┬─────────────────────────────────────┐
│ Rail  │ Filter ledger          │ Property map/list canvas            │
│       │ Area                   │  map or visual area field           │
│       │ Rent/Sale              │                                     │
│       │ Budget                 │  listing cards appear as stacked     │
│       │ Bedrooms               │  paper tickets, not rounded tiles    │
│       │ Live status            │                                     │
│       │ Reality status         │                                     │
│       │ [Reset]                │                                     │
└───────┴───────────────────────┴─────────────────────────────────────┘
```

Default sort: **Live availability first**, then newest reconfirmed time.

Listing ticket contents:

- availability stamp: `AVAILABLE · CONFIRMED 2H AGO`;
- property type and approximate area;
- price block in mono font;
- 3 image strip or room-tour preview;
- capture badge such as `SELLER-CAPTURED · 6/7 ROOMS`;
- `Open Passport` primary text action;
- bookmark icon.

Public listing cards must not display exact address or direct phone number.

### 8.4 Property Passport screen

Desktop layout:

```text
┌─────────────────────┬──────────────────────────────┬─────────────────┐
│ Evidence ledger     │ Interactive Tour Canvas       │ Decision panel  │
│                     │                              │                 │
│ Live Pulse          │  panorama / room graph       │ Price           │
│ Reality status      │  hotspot navigation           │ Compare         │
│ Captured date       │                              │ Ask question    │
│ Missing rooms       │                              │ Request visit   │
│ Authority label     │                              │                 │
└─────────────────────┴──────────────────────────────┴─────────────────┘
```

The centre tour canvas must dominate the page. Place room nodes/floor-plan selector below it, not above it.

Left evidence ledger copy example:

```text
LIVE PULSE
Available — reconfirmed 2 hours ago

REALITY PASSPORT
Seller-captured · 14 Aug 2026
Coverage: 6 of 7 rooms
Not captured: Balcony

LISTING AUTHORITY
Owner authority reviewed
This is not title certification.
```

Right decision panel actions:

```text
Add to Twin View
Ask in protected chat
Send structured offer
Request a SafeView
```

### 8.5 Twin View screen

The compare screen is the signature visual interaction.

Top: room type control: `Living room | Kitchen | Bedroom | Balcony`.

Centre: two equal panorama canvases side by side. Each canvas has a north/light/route mini label. On a mobile browser, stack vertically with a sticky `A / B` switcher.

Bottom ledger:

| Field | Property A | Property B |
|---|---|---|
| Live Pulse | Confirmed 2h ago | Confirmed 5h ago |
| Capture date | 14 Aug 2026 | 12 Aug 2026 |
| Room captured | Yes | Yes |
| Balcony | Not captured | Captured |
| Ask for proof | Button | Button |

Do not create a hidden AI recommendation score. Let buyers inspect actual evidence.

### 8.6 Inbox screen

Use a two-pane messaging view on desktop and a full-screen thread on mobile.

Conversation header includes property thumbnail, property title, and safe status. Display a safety banner until an appointment is confirmed:

> “Keep your phone number, money, and exact address private until SafeView confirms the visit.”

Messages can be plain text, selected quick questions, proof requests, offer cards, or appointment cards.

### 8.7 Visits screen

Use a chronological route layout, not a grid.

Each appointment displays:

```text
TODAY · 4:30–5:00 PM
Mirpur 11 · exact address unlocks in 43 min
Buyer: Identity confirmed
Host: Owner authority reviewed
[Open Viewing Pass]
```

---

## 9. Mobile Application — Exact Interface

### 9.1 Mobile tab structure

Use five tabs:

```text
Explore | Inbox | Visits | Capture | Account
```

The centre `Capture` item uses the signature Jol Teal mark and opens a role-aware action sheet.

```text
Capture a listing
Scan a DORJA property code
Check a viewing pass
```

### 9.2 Mobile Explore

Top is a compact search bar: `Search area, road, or property ID`.

Below are horizontally scrollable filter chips:

```text
For rent · For sale · Family · Bachelor · Available now · 3D tour · Verified authority
```

Cards should use full-width 4:5 image/tour preview with an overlaid Live Pulse stamp at the top. The lower paper panel contains price, approximate area, property type, and capture status.

### 9.3 Mobile property detail

Use a full-width tour preview at top. The screen below it has a physically printed-looking evidence strip.

```text
AVAILABLE
Reconfirmed 2h ago

SELLER-CAPTURED
14 Aug 2026 · 6/7 rooms
```

Bottom persistent action bar:

```text
[Compare] [Ask] [Request SafeView]
```

### 9.4 Mobile Offer Room

Do not let price negotiation happen only as loose chat text.

Use a structured card with editable fields:

```text
Offer type: Rent / Sale
Monthly rent / Proposed price: ৳ [input]
Advance / booking amount: ৳ [input]
Move-in / completion date: [date]
Utilities included: [chips]
Furniture included: [chips]
Condition note: [optional text]
Offer expires: [date/time]
```

Buttons:

```text
Save draft
Send offer
Counter offer
Accept terms
Decline
```

Every sent offer produces an immutable `OfferVersion`. Acceptance locks the offer record but does **not** create a legally binding sale agreement. Display this warning before acceptance.

### 9.5 Mobile SafeView appointment flow

#### Request screen

Inputs:

- preferred slot;
- attendee count;
- companion name optional;
- one short note;
- checkbox: `I understand that exact location is protected until appointment confirmation.`

#### Host confirmation screen

Show buyer verification label, attendee count, selected/proposed time, property preview, and `Accept time` / `Propose another time` controls.

#### Confirmed pass screen

Use a dark Ink background with a large white/teal dynamic QR. Do not show raw NID data.

```text
SAFEVIEW PASS
4:30–5:00 PM · 14 Aug
Expires in 41 min
Property entry unlocks at 4:00 PM
```

Show `Call trusted contact` and `Safety guide` controls. Do not label a button “police alert” unless there is a formally supported emergency integration.

### 9.6 Bangladesh accessibility rules

1. Support Bangla and English from the same translation key set.
2. Use no text smaller than 14 sp in primary flows.
3. Primary action touch targets must be at least 48 dp.
4. Do not rely on colour alone for availability/verification.
5. Offer a Lite Tour: thumbnails first, then on-demand high-resolution panorama tiles.
6. Queue uploads if connectivity drops; show clear recovery action.
7. Use `Asia/Dhaka` throughout appointment displays.

---

## 10. Messaging, Negotiation, and Scheduling

### 10.1 Conversation creation

Create one conversation per `(listing, seeker, host)` tuple. Do not create duplicate threads.

```ts
type ConversationKind = 'PROPERTY';
type MessageKind =
  | 'TEXT'
  | 'PROOF_REQUEST'
  | 'OFFER_CARD'
  | 'VIEWING_CARD'
  | 'SYSTEM_NOTICE';
```

### 10.2 Protected messaging rules

Before a SafeView appointment is confirmed:

- detect and mask likely phone numbers in message display;
- block sending exact address-like content when possible, but do not pretend regex is perfect;
- display contextual safety reminder;
- disallow uploading raw identity documents into ordinary chat;
- allow proof requests such as “Please show balcony at 6 PM”;
- allow seller to answer with a time-stamped proof clip attached to the listing passport.

After appointment confirmation:

- exact address release happens through a dedicated appointment field, not through chat text;
- only participants can view it during the reveal window;
- chat still never exposes raw NID or authority documents.

### 10.3 Negotiation state machine

```text
DRAFT
  → SENT
  → ACCEPTED
  → CLOSED

SENT
  → COUNTERED
  → DECLINED
  → WITHDRAWN
  → EXPIRED

COUNTERED
  → ACCEPTED
  → DECLINED
  → COUNTERED
  → EXPIRED
```

Rule: only the recipient can counter a live offer. The sender may withdraw until accepted. A newly countered offer invalidates the previous version.

### 10.4 Booking state machine

```text
REQUESTED
  → PROPOSED
  → CONFIRMED
  → CHECKED_IN
  → COMPLETED

REQUESTED
  → CANCELLED
  → EXPIRED

CONFIRMED
  → CANCELLED
  → EXPIRED
  → SAFETY_FOLLOW_UP
```

### 10.5 Slot rules

1. Slots belong to a listing and host.
2. Slots have `startsAt`, `endsAt`, `capacity`, `isActive`.
3. The backend checks conflicts transactionally before confirming.
4. Default maximum appointment duration is 30 minutes.
5. Default exact-address reveal window begins 30 minutes before the slot.
6. Appointment pass expires 15 minutes after the scheduled end unless extended by host.
7. A cancelled slot must revoke address access and invalidate its pass immediately.

### 10.6 Safety events

| Event | Trigger | Action |
|---|---|---|
| `ADDRESS_REVEALED` | Appointment enters reveal window | Audit event only. |
| `PASS_VIEWED` | Participant opens pass | Audit event only. |
| `CHECKED_IN` | Host/guard scans pass | Update visit state and notify participant. |
| `CHECKOUT_REMINDER` | End time passes without check-out | Push/in-app reminder to both parties. |
| `SAFETY_CONCERN` | User opens report/concern action | Create private report and alert support workflow. |
| `MISSED_CHECKOUT` | Configured delay after reminder | Notify preselected trusted contacts only if user enabled it. |

Do not automatically message police. The platform must direct users to local emergency services in a real emergency.

### 10.7 SafeView transactional backend implementation

Every high-risk workflow must run inside a database transaction. Do not let the client sequence multiple mutations and hope they remain consistent.

#### 10.7.1 Request viewing transaction

```text
BEGIN SERIALIZABLE
  1. Lock listing row.
  2. Confirm listing is ACTIVE or VIEWING_HELD and Live Pulse is not expired.
  3. Confirm seeker identity status is IDENTITY_CONFIRMED and unexpired.
  4. Confirm listing authority is REVIEWED and unexpired.
  5. Lock requested availability slot.
  6. Confirm slot is active, in future, and has capacity.
  7. Create Viewing(status=REQUESTED).
  8. Create or reuse property conversation.
  9. Write audit event VIEWING_REQUESTED.
COMMIT
AFTER COMMIT
  10. Queue in-app/push notification to host.
  11. Publish viewing.updated websocket event without exact address.
```

#### 10.7.2 Confirm viewing and issue pass transaction

```text
BEGIN SERIALIZABLE
  1. Lock Viewing row and AvailabilitySlot row.
  2. Confirm requester, host, listing, and slot remain eligible.
  3. Confirm exact slot capacity has not been exceeded.
  4. Calculate addressRevealAt = startsAt - 30 minutes.
  5. Generate 32-byte random raw pass token.
  6. Store SHA-256 hash of raw pass token only.
  7. Create ViewingPass(status=ISSUED, expiresAt=endsAt + 15 minutes).
  8. Update Viewing(status=CONFIRMED, startsAt, endsAt, addressRevealAt).
  9. Increment slot confirmedCount.
  10. Write audit event VIEWING_CONFIRMED.
COMMIT
AFTER COMMIT
  11. Queue appointment reminders and pass reveal job.
  12. Send host/seeker confirmation notice.
```

The raw pass token is returned once in the confirmed participant response and encoded into the QR. Do not store or log it afterward. When the participant retrieves the pass later, create a signed, short-lived QR presentation token from the hashed record; never recreate the original raw database token.

#### 10.7.3 Exact-address reveal transaction

```text
BEGIN READ COMMITTED
  1. Load viewing by ID.
  2. Confirm current user is seeker, host, or registered companion/guard participant.
  3. Confirm viewing status is CONFIRMED or CHECKED_IN.
  4. Confirm current server time is within [addressRevealAt, endsAt + 15min].
  5. Decrypt exact listing address only in service memory.
  6. Write SafetyEvent(ADDRESS_REVEALED) and AuditLog.
COMMIT
RETURN one-time response with no-store cache headers.
```

Set response headers:

```http
Cache-Control: no-store, max-age=0
Pragma: no-cache
Referrer-Policy: no-referrer
```

The system cannot technically stop screenshots. The user interface must state that the address is private and should not be forwarded.

#### 10.7.4 Pass scan/check-in transaction

```text
BEGIN SERIALIZABLE
  1. Hash presented token or validate short-lived signed presentation token.
  2. Lock ViewingPass and Viewing rows.
  3. Confirm pass status is ISSUED or VIEWED.
  4. Confirm current time is in check-in window.
  5. Confirm scanner is authorised host, guard, or representative for the listing.
  6. Increment scanCount.
  7. If no check-in exists: set pass CHECKED_IN and create corresponding SafetyEvent.
  8. If already checked in: return idempotent success; do not create second event.
  9. Write AuditLog.
COMMIT
AFTER COMMIT
  10. Notify both appointment parties of check-in.
```

#### 10.7.5 Cancel viewing transaction

```text
BEGIN SERIALIZABLE
  1. Lock viewing and pass.
  2. Confirm actor is host or seeker.
  3. Update viewing status CANCELLED.
  4. Update pass status INVALIDATED if it exists.
  5. Decrement slot confirmedCount only once.
  6. Create SafetyEvent(APPOINTMENT_CANCELLED).
  7. Write AuditLog.
COMMIT
AFTER COMMIT
  8. Send cancellation notification.
```

### 10.8 Safety follow-up implementation

#### 10.8.1 Missed check-out job

Run a queue worker every five minutes.

```text
Find viewings where:
  status is CONFIRMED or CHECKED_IN
  end time + 10 minutes is past
  one or both checkout timestamps are null
  no active CHECKOUT_REMINDER_SENT event exists

For each viewing:
  create CHECKOUT_REMINDER_SENT event
  queue reminder to both parties

If end time + 30 minutes is past and the user has opted in:
  create TRUSTED_CONTACT_ALERTED event
  queue a limited alert to selected trusted contact
```

The trusted-contact alert must say only:

```text
DORJA safety check: [Name] has not completed check-out from a scheduled property viewing. Please contact them directly.
```

It must not include a full address unless the user has explicitly consented to that level of sharing in a future feature. It must not accuse another participant of a crime.

#### 10.8.2 Safety concern reporting

The `Safety Concern` button must be reachable from a confirmed viewing, a completed viewing, and a property conversation. It opens a private form:

```text
What happened?
[Suspicious behaviour]
[False listing]
[Harassment]
[Address misuse]
[No-show]
[Immediate danger]
[Other]

Describe what happened [encrypted private text]
Attach evidence [optional]
[Submit private report]
```

If user selects `Immediate danger`, show this first:

> “If you are in immediate danger, move to a safer place and contact local emergency services now. DORJA cannot dispatch emergency responders.”

Then permit report submission. Do not show public reports, public strike counts, or public accusations on profile pages.

#### 10.8.3 Internal triage queue

Reviewer/admin sees only:

```text
Report ID
Category
Created time
Associated listing/viewing ID
Status
```

The detailed report description is decrypted only after reviewer action records a `REPORT_DETAIL_OPENED` audit event. Platform actions are limited to documented rules:

```text
Restrict listing temporarily
Suspend messaging temporarily
Request additional information
Mark appointment/capture as under review
Close report with encrypted internal resolution
```

Do not state that a user committed a crime. Use wording such as `Account restricted while a safety concern is reviewed.`

---

## 11. API Design

### 11.1 API conventions

Base URL:

```text
/v1
```

Use bearer access tokens and rotation-capable refresh tokens. Use Zod schemas as a shared contract package. Generate OpenAPI documentation from Fastify route schemas.

Success response:

```json
{
  "data": {},
  "meta": {}
}
```

Error response:

```json
{
  "error": {
    "code": "VIEWING_IDENTITY_REQUIRED",
    "message": "Identity confirmation is required before requesting a private viewing.",
    "requestId": "..."
  }
}
```

### 11.2 Authentication endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/auth/otp/start` | Start phone OTP. Use console transport in demo. |
| `POST` | `/v1/auth/otp/verify` | Verify OTP and issue session. |
| `POST` | `/v1/auth/refresh` | Rotate refresh token. |
| `POST` | `/v1/auth/logout` | Revoke current session. |
| `GET` | `/v1/me` | Current user and permissions. |

### 11.3 Identity and authority endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/identity/start` | Start provider/manual review flow after consent. |
| `GET` | `/v1/identity/status` | Return only current user’s verification status. |
| `POST` | `/v1/listings/:listingId/authority/evidence` | Upload authority evidence to private review storage. |
| `POST` | `/v1/reviewer/authority/:reviewId/approve` | Reviewer action. |
| `POST` | `/v1/reviewer/authority/:reviewId/reject` | Reviewer action with reason. |

### 11.4 Listing endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/v1/listings` | Public discovery; returns approximate data only. |
| `POST` | `/v1/listings` | Create draft listing. |
| `GET` | `/v1/listings/:slug` | Public Reality Passport view. |
| `PATCH` | `/v1/listings/:listingId` | Lister updates draft/active listing. |
| `POST` | `/v1/listings/:listingId/live-pulse` | Reconfirm availability. |
| `POST` | `/v1/listings/:listingId/mark-closed` | Mark rented/sold. |
| `POST` | `/v1/listings/:listingId/qr` | Generate a property QR SVG/PNG. |

### 11.5 Capture endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/listings/:listingId/capture-sessions` | Create capture session. |
| `POST` | `/v1/capture-sessions/:id/upload-url` | Get signed MinIO upload URL. |
| `POST` | `/v1/capture-sessions/:id/media` | Confirm uploaded asset metadata. |
| `POST` | `/v1/capture-sessions/:id/rooms` | Add/modify room capture metadata. |
| `POST` | `/v1/capture-sessions/:id/submit` | Run processing/review path. |
| `GET` | `/v1/capture-sessions/:id` | Lister capture progress. |
| `POST` | `/v1/capture-sessions/:id/publish` | Publish permitted reviewed capture. |

### 11.6 Messaging endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/listings/:listingId/conversations` | Open/reuse property conversation. |
| `GET` | `/v1/conversations` | User conversation list. |
| `GET` | `/v1/conversations/:id/messages` | Paginated messages. |
| `POST` | `/v1/conversations/:id/messages` | Send allowed message kind. |
| `POST` | `/v1/conversations/:id/proof-requests` | Request targeted evidence. |

### 11.7 Offer endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/v1/listings/:listingId/offers` | Create structured offer. |
| `GET` | `/v1/offers/:id` | Offer detail with versions. |
| `POST` | `/v1/offers/:id/counter` | Counter current offer. |
| `POST` | `/v1/offers/:id/accept` | Accept with non-binding acknowledgement. |
| `POST` | `/v1/offers/:id/decline` | Decline. |
| `POST` | `/v1/offers/:id/withdraw` | Withdraw if sender has permission. |

### 11.8 Viewing endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/v1/listings/:listingId/slots` | Return available slots. |
| `POST` | `/v1/listings/:listingId/viewings` | Request a SafeView. |
| `POST` | `/v1/viewings/:id/propose` | Host proposes slot. |
| `POST` | `/v1/viewings/:id/confirm` | Both conditions satisfied; create pass. |
| `POST` | `/v1/viewings/:id/cancel` | Cancel and revoke address/pass. |
| `GET` | `/v1/viewings/:id/pass` | Participant retrieves time-bound pass. |
| `POST` | `/v1/viewings/:id/check-in` | Host/guard scanner confirmation. |
| `POST` | `/v1/viewings/:id/check-out` | Participant check-out. |
| `POST` | `/v1/viewings/:id/safety-events` | Private safety concern or event. |

### 11.9 Representative route sample

```ts
const requestViewingBody = z.object({
  preferredSlotId: z.string().uuid(),
  attendeeCount: z.number().int().min(1).max(3),
  companionName: z.string().trim().min(2).max(80).optional(),
  note: z.string().trim().max(280).optional(),
  acceptedSafetyTerms: z.literal(true),
});

fastify.post('/v1/listings/:listingId/viewings', {
  preHandler: [requireAuth],
  schema: { body: requestViewingBody },
}, async (request, reply) => {
  const user = request.user;
  if (user.identityStatus !== 'IDENTITY_CONFIRMED') {
    return reply.code(403).send({
      error: {
        code: 'VIEWING_IDENTITY_REQUIRED',
        message: 'Identity confirmation is required before requesting a private viewing.',
      },
    });
  }

  const viewing = await viewingService.request({
    listingId: request.params.listingId,
    seekerId: user.id,
    ...request.body,
  });

  return reply.code(201).send({ data: viewing });
});
```

---

## 12. WebSocket Events

Use rooms keyed by user and conversation. Never broadcast private listing details globally.

```text
conversation.message.created
offer.created
offer.updated
listing.live-pulse.updated
capture-session.updated
viewing.updated
viewing.pass.issued
viewing.checked-in
viewing.checkout-reminder
notification.created
```

Payloads must use public-safe DTOs. Exact address must never appear in a broadcast payload.

---

## 13. Local Infrastructure

### 13.1 Docker Compose requirements

Create `infra/docker-compose.yml` with:

```text
postgres  → port 5432
redis     → port 6379
minio     → ports 9000 and 9001
```

Use persistent named volumes. Create a bucket bootstrap job for `dorja-media` with private policy. Media access should use short-lived signed URLs.

### 13.2 Local-network device testing

The Expo mobile app must reach the API from a physical Android device. Do not hardcode `localhost` in the mobile app. Use a development environment variable such as:

```dotenv
EXPO_PUBLIC_API_URL=http://192.168.0.10:4000
```

Document that the device and development computer need to be on the same Wi-Fi network. Use a safe temporary tunnel only if necessary; do not expose a production database publicly for a demo.

### 13.3 Background jobs

Implement queues:

```text
media-processing
capture-expiry
live-pulse-expiry
offer-expiry
viewing-reminders
viewing-pass-expiry
safety-follow-up
notification-delivery
```

### 13.4 Local backend service boundaries

The backend must be organised as domain modules. Do not place all business logic in HTTP route handlers.

```text
src/modules/
├── auth/
│   ├── auth.service.ts
│   ├── otp.service.ts
│   ├── session.service.ts
│   └── auth.routes.ts
├── identity/
│   ├── identity-provider.ts
│   ├── manual-review.provider.ts
│   ├── identity.service.ts
│   └── identity.routes.ts
├── listings/
│   ├── listing.service.ts
│   ├── live-pulse.service.ts
│   ├── authority.service.ts
│   └── listing.routes.ts
├── capture/
│   ├── capture-session.service.ts
│   ├── checkpoint.service.ts
│   ├── media.service.ts
│   ├── capture-processor.ts
│   └── capture.routes.ts
├── messaging/
│   ├── conversation.service.ts
│   ├── redaction.service.ts
│   └── message.routes.ts
├── offers/
│   ├── offer.service.ts
│   ├── offer-state-machine.ts
│   └── offer.routes.ts
├── viewings/
│   ├── viewing.service.ts
│   ├── viewing-pass.service.ts
│   ├── address-reveal.service.ts
│   ├── safety.service.ts
│   └── viewing.routes.ts
├── security/
│   ├── field-encryption.service.ts
│   ├── audit.service.ts
│   ├── signed-url.service.ts
│   └── security-policy.ts
├── notifications/
│   ├── notification.service.ts
│   ├── console.provider.ts
│   └── notification.worker.ts
└── jobs/
    ├── queues.ts
    ├── live-pulse.worker.ts
    ├── retention.worker.ts
    ├── viewing-reminder.worker.ts
    └── media.worker.ts
```

Each service must receive dependencies via constructor/factory injection. This permits `ManualReviewIdentityProvider`, console notification transport, and local storage adapters in development while keeping a clean route to production providers.

### 13.5 Database transaction helper

Use one wrapper that provides request ID, actor context, transaction, and audit logging. A service must not directly call `prisma.$transaction` without preserving these values.

```ts
export async function withDomainTransaction<T>(
  ctx: RequestContext,
  work: (tx: Prisma.TransactionClient, audit: AuditWriter) => Promise<T>,
): Promise<T> {
  return prisma.$transaction(async (tx) => {
    const audit = new AuditWriter(tx, ctx);
    const result = await work(tx, audit);
    return result;
  }, { isolationLevel: Prisma.TransactionIsolationLevel.Serializable });
}
```

Use serializable transactions for pass issuance, viewing confirmation, check-in, cancellation, offer acceptance, and slot capacity changes. Use ordinary read committed transactions for public search and non-critical reads.

---

## 14. Safety and Privacy Rules

### 14.1 Protected information matrix

| Information | Public | Conversation before confirmed viewing | Confirmed participants | Reviewer/Admin |
|---|---:|---:|---:|---:|
| Approximate area | Yes | Yes | Yes | Yes |
| Exact property address | No | No | Time-window only | Yes, restricted |
| Phone number | No | No | No by default | Restricted |
| Identity status badge | Yes, limited | Yes | Yes | Yes |
| NID/document image | No | No | No | Restricted |
| Property authority evidence | No | No | No | Restricted |
| Viewing schedule | No | Participants only | Yes | Yes |
| Safety report | No | No | Reporter/support only | Restricted |

### 14.2 Audit log events

Record an append-only audit log for:

```text
IDENTITY_CONSENT_GRANTED
IDENTITY_STATUS_CHANGED
AUTHORITY_EVIDENCE_UPLOADED
AUTHORITY_STATUS_CHANGED
EXACT_ADDRESS_REVEALED
VIEWING_PASS_ISSUED
VIEWING_PASS_SCANNED
VIEWING_CHECKED_OUT
SAFETY_CONCERN_CREATED
LISTING_RESTRICTED
```

### 14.3 Safety copy

Show these rules during appointment confirmation:

> Meet during the confirmed time window.
>
> Bring a companion where possible.
>
> Do not share money, raw identity documents, or passwords during a viewing.
>
> Use the DORJA Viewing Pass at arrival.
>
> In an emergency, contact local emergency services directly.

### 14.4 Abuse control

Rate limit OTP requests, login attempts, message sends, proof requests, QR scan attempts, and viewing requests. Add account/IP/device telemetry only with transparent consent and privacy documentation.

---

## 15. Maps and Location

### 15.1 MVP

Allow lister to paste a Google Maps link. Server validates host against an allow-list and stores the normalised URL. Display an approximate location card publicly.

Public copy:

```text
Approximate area: Mirpur 11
Exact entry details unlock after a confirmed SafeView.
```

The mobile app may use `Linking.openURL()` to open Google Maps as an external interactive map in the MVP. The website may show a static/approximate location panel and an external `Open area in Maps` action.

### 15.2 Later provider adapter

Use this interface so a map provider can be changed later:

```ts
interface MapProvider {
  normalizeSharedLink(input: string): Promise<NormalizedLocation>;
  publicPreview(location: ApproximateLocation): PublicMapPreview;
  directionsLink(exactLocation: ExactLocation): string;
}
```

Do not bake Google keys into the mobile app source. Use server proxying/configuration if a provider SDK is selected later.

---

## 16. Demo Data and FIRSO Demonstration

### 16.1 Demo data rules

Use two permissioned, real or clearly fictional **Demo Listing** properties. Do not use real user identity documents. Do not include customer reviews or invented testimonials.

Create:

```text
Demo Listing A: Family apartment in Mirpur
Demo Listing B: Family apartment in Uttara
```

Each demo listing needs:

- property QR poster;
- public approximate area;
- 5–7 room nodes;
- a real capture date;
- one deliberately missing room on one listing to demonstrate transparency;
- one live availability status;
- one offer example;
- one viewing request and pass.

### 16.2 Ninety-second judge flow

1. Judge scans a printed DORJA QR poster.
2. Browser opens the property Reality Passport.
3. Judge sees `Available — reconfirmed 2h ago` and a room-tour canvas.
4. Judge presses `Twin View`, comparing the two balconies.
5. Judge requests `Show the balcony at 6 PM` in protected chat.
6. Judge requests a SafeView and sees identity confirmation requirement.
7. Demo seeker becomes identity-confirmed through a clearly labelled simulation.
8. Seller accepts a time slot; QR Viewing Pass appears.
9. A second mobile device plays the building guard and scans the pass.
10. Both check out; visit becomes complete.

### 16.3 Pitch language

Use this exact line:

> “Property portals digitise advertisements. DORJA digitises the signal outside the building and keeps that signal accountable until the viewing is complete.”

Do not say:

> “We have solved crime.”

Do not say:

> “Every phone makes perfect 3D.”

---

## 17. Implementation Milestones

### Milestone 0 — Bootstrap

Deliver:

- monorepo;
- Docker services;
- Prisma schema;
- API health endpoint;
- mobile/web app shells;
- shared colour tokens;
- logo integration with fallback;
- lint, typecheck, and test commands.

Acceptance:

```text
pnpm dev:infra starts all infrastructure.
pnpm typecheck passes.
Web opens at localhost:3000.
API opens at localhost:4000/health.
Expo starts without errors.
```

### Milestone 1 — Public property passport

Deliver:

- listing CRUD;
- public explore page;
- Live Pulse;
- property page;
- room graph and placeholder/media tour;
- hold-to-capture checkpoint preview from seeded media;
- Twin View;
- QR generation.

Acceptance:

```text
Guest can scan/open a QR link.
Guest sees approximate area, not exact address.
Guest can compare equivalent rooms in two properties.
```

### Milestone 2 — Mobile capture

Deliver:

- capture route;
- camera permission;
- guided hold-to-capture spatial checkpoints;
- upload queue;
- review screen;
- local MinIO media storage;
- Reality Passport publication.

Acceptance:

```text
Linter can capture two rooms on Android.
Interrupted upload resumes/retries.
Web page reflects submitted capture coverage and timestamp.
Each checkpoint records a hold duration and accepted/retake status.
```

### Milestone 3 — Safe transactions

Deliver:

- OTP flow;
- manual identity review demo;
- authority review;
- protected conversation;
- structured offers;
- slots and viewings;
- time-limited pass;
- check-in/out.
- encrypted sensitive records, append-only audit events, and retention jobs.

Acceptance:

```text
Unverified seeker cannot request a viewing.
Exact address does not appear before confirmed reveal window.
Cancelled visit invalidates pass.
Guard view accepts pass exactly once.
```

### Milestone 4 — Safety and demo polish

Deliver:

- concern reporting;
- trusted-contact simulation;
- event audit log;
- Bangla/English copy;
- Lite Tour mode;
- demo runbook;
- no console errors.

Acceptance:

```text
The 90-second demo can run from a QR poster to a completed SafeView check-out.
All status labels use precise, non-misleading language.
No raw documents, phone numbers, or exact addresses leak through public API calls.
```

---

## 18. Required Tests

### 18.1 Unit tests

Test:

- all offer state transitions;
- all viewing state transitions;
- access control checks;
- exact-address reveal calculation;
- pass token hashing and single-use invalidation;
- listing Live Pulse expiry;
- capture coverage calculations;
- role/authority permissions;
- message redaction helper;
- provider adapter failure modes.

### 18.2 API tests

Test:

- guest cannot fetch exact address;
- unverified user gets `403 VIEWING_IDENTITY_REQUIRED`;
- no duplicate conversation for same participant/listing;
- conflicting time slot cannot confirm;
- offer counter invalidates prior offer version;
- cancel revokes pass;
- expired pass rejects check-in;
- viewer cannot access a different property pass;
- reviewer-only endpoints reject ordinary users.

### 18.3 Web end-to-end tests

Test:

- explore search/filter;
- passport open;
- Twin View room selector;
- protected chat safety banner;
- offer card rendering;
- appointment confirmation;
- address remains hidden until reveal condition.

### 18.4 Mobile test checklist

Test on physical Android device:

- camera permission denied/regranted;
- camera session opens once;
- capture route and retake;
- QR scan opens listing;
- network interruption queues upload;
- appointment pass screen;
- check-in scan;
- large text and Bangla labels do not clip.

---

## 19. Code Quality Rules

1. Use feature folders and composition over giant components.
2. Never call setState during render.
3. Use `FlatList` for long mobile lists.
4. Use server-side authorisation for every private endpoint.
5. Keep API DTOs separate from ORM records.
6. Use `zod` validation at all request boundaries.
7. Use database transactions for slot confirmation, offer acceptance, and pass issuance.
8. Use named domain errors, not unstructured strings.
9. Add loading, empty, and error states to every asynchronous screen.
10. Avoid fake data in production interfaces; demo seed content must be clearly marked.
11. Keep sensitive environment variables server-only.
12. Never use user-provided filenames directly as storage keys.
13. Never log raw OTPs, tokens, NIDs, property documents, exact addresses, or safety report contents.
14. Do not use `any` in new TypeScript code.
15. Keep all buyer-facing verification labels tied to actual system states.

---

## 20. Definition of Done

The first complete version is done only when all of the following are true:

- A real printed QR opens a public listing on web/mobile.
- The public listing has Live Pulse, Reality Passport, room navigation, and Twin View.
- Mobile app can guide and upload room capture using Expo Camera.
- Capture is attached to a listing and a lister identity record.
- Web/mobile support protected property messaging.
- Offer Room supports structured offer/counter/accept/decline workflow.
- SafeView requires identity confirmation, time selection, and both-party confirmation.
- Exact address is protected until confirmed appointment reveal time.
- One-time QR pass can be checked in once by a guard/host account.
- Check-out events complete the appointment.
- Safety concern flow exists with honest limitations.
- No screen claims title verification, criminal-background clearance, automatic police response, or universal phone-generated 3D mesh.
- Brand colours, typography, logo path, and UI rules from this document are implemented consistently.
- `pnpm lint`, `pnpm typecheck`, `pnpm test`, and `pnpm build` pass.

---

## 21. Reference Sources

[1] Expo, “EAS Build.” https://docs.expo.dev/build/introduction/

[2] Expo, “Introduction to development builds.” https://docs.expo.dev/develop/development-builds/introduction/

[3] Expo, “Expo Camera.” https://docs.expo.dev/versions/latest/sdk/camera/

[4] Apple, “Introducing RoomPlan.” https://developer.apple.com/augmented-reality/roomplan/

[5] Bangladesh Police, “Police Clearance Certificate” portal. https://pcc.police.gov.bd/ords/f?p=501

[6] BBC News Bangla, “ভাড়াটে সেজে অভিনব উপায়ে ঢাকায় বাড়ি লুট,” 29 October 2018. https://www.bbc.com/bengali/news-46014488

---

## 22. Final Agent Prompt

Use this document as the implementation contract.

Build in milestone order. Do not skip security and workflow state checks to make a pretty demo. Do not replace the DORJA design system with a generic Tailwind dashboard. Do not request Android Studio setup. Do not introduce payment, title verification, or criminal-record integrations unless the required lawful provider credentials and agreements have been explicitly supplied.

When blocked by a missing integration, implement the provider adapter, visible product state, local demo path, and clear configuration error. Preserve the route to a real integration without faking it.

The best demo is not a large list of cards. It is one complete journey:

```text
Physical QR sign
→ live property record
→ Reality Passport
→ Twin View
→ protected question
→ formal offer
→ identity-confirmed SafeView
→ one-time pass
→ check-in and check-out
```

Build that journey exceptionally well.
