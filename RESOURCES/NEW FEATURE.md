# Claude Code Prompt — Add DORJA Handover to the Existing Full DORJA App

Copy the entire prompt below into Claude Code from the root directory of the **already-built DORJA application**.

---

```text
You are extending an existing, already-built application named DORJA.

## Mission

Add a new, polished, production-shaped module named **DORJA Handover** to the existing app. This module is inspired by the SHARTO concept: a buyer-owned **Promise-to-Proof Passport** for a specific apartment/unit from booking through handover and remedy tracking.

The full DORJA app is ALREADY BUILT. Do not replace it, rebuild it, downgrade it, remove existing capabilities, or create a separate parallel application. Preserve all existing DORJA features, including property discovery, listing pages, 3D/Reality Passport or capture flows, safe viewing, messaging, offers, scheduling, account systems, backend conventions, navigation, current data model, and design system.

Your job is to add the handover module as a natural DORJA capability attached to an existing property/listing/unit.

## First: inspect before editing

Before changing code, inspect the repository carefully and report a short implementation plan. Identify:

1. The framework(s), package manager, routes, data layer/ORM, auth mechanism, storage mechanism, websocket/realtime mechanism, and testing setup already used.
2. Existing property/listing/unit models and how a listing detail page is rendered.
3. Existing user roles and permission helpers.
4. Existing document/media upload mechanisms.
5. Existing notification, messaging, activity/audit-log, QR, PDF/export, and calendar components/services, if any.
6. Existing visual tokens, fonts, icons, component primitives, and mobile/web layout conventions.

Use existing patterns and components wherever they exist. Do not introduce a second router, second ORM, second authentication system, duplicate upload service, duplicate notification service, or an unrelated design system.

After inspection, implement directly. Do not stop merely because a feature is not present; use the app’s closest established pattern.

## Hard preservation rules

- Do not alter, delete, or regress an existing DORJA route or feature.
- Do not redesign unrelated screens.
- Do not replace the current DORJA logo, colours, typography, navigation, or identity.
- Do not refactor the entire app just to add this feature.
- Do not add a public marketplace, AR/3D reconstruction engine, government integration, title verification, criminal-record check, payment escrow, legal-advice flow, or AI features for this module.
- Do not claim legal verification, construction safety certification, title verification, enforceable arbitration, or automatic dispute resolution.
- Do not invent user reviews, ratings, testimonials, or fake public activity.
- Do not create an isolated “SHARTO” app. The user-facing name is **DORJA Handover**.

## Product definition

DORJA Handover answers one question for a buyer:

> “For this exact apartment, what was promised, what evidence exists, what changed, and what still needs to be resolved?”

It is a structured record associated with one existing DORJA property/listing. It turns scattered clauses, payment receipts, photos, developer messages, and handover defects into a neutral chronological unit passport.

The module’s core interaction is:

```text
Promise → Proof → Remedy → Handover Evidence Pack
```

The product records who submitted evidence and when. It does not decide who is legally right.

## Where it belongs in the existing app

### Property/listing detail

On every eligible existing DORJA property detail page, add a new contextual section or tab:

```text
Handover Passport
```

Eligibility should initially support listings/units marked for sale, under construction, booked, or handover-ready. Reuse existing property intent/status fields if they already exist. If needed, add a small explicit eligibility field without breaking current listings.

On non-eligible properties, do not clutter the page. Show no module or a small unobtrusive `Handover available for purchase units` explanation only to authorised listers.

### Navigation

Add a contextual `Handover` item under the existing property detail navigation and an aggregated `My Handover` / `Handover Passports` item only if the existing information architecture has an appropriate buyer/owner dashboard area.

Do not add a new top-level navigation system if the app does not need it.

### Existing 3D / Reality Passport integration

If the listing already has DORJA’s Reality Passport, media, 3D tour, or capture evidence, surface those assets inside the Handover Passport as evidence links:

```text
Reality capture recorded on [date]
Open captured room view
```

Do not modify the existing tour renderer. The handover module consumes existing capture/media references when present.

## Roles and capability model

Map these capabilities to the app’s existing roles. Do not create duplicate user identities.

| Capability | Buyer | Developer/owner/authorised agent | DORJA reviewer/admin |
|---|---:|---:|---:|
| View passport linked to their unit | Yes | Yes | Yes |
| Create original promise | Draft/request only unless authorised | Yes | Yes |
| Acknowledge promise | Yes | Yes | Yes |
| Upload proof | Yes | Yes | Yes |
| Create remedy issue | Yes | Yes | Yes |
| Propose remedy date | No | Yes | Yes |
| Accept/contest remedy completion | Yes | Yes | Yes |
| Export neutral evidence pack | Yes | Yes | Yes |
| View raw private verification documents | No | No | Only existing authorised reviewer flow |

Use existing property-authority verification rules. A developer/agent must be associated with the current property/listing before adding or answering promises.

## Exact feature set to build now

### 1. Handover Passport header

Build a unit-level dashboard header attached to the listing/property.

Show:

```text
Handover Passport
[existing property title / unit identity]
Passport status: Preparing | Active | Handover review | Closed
Original agreement date
Latest activity timestamp
Promise completion: X of Y resolved
```

Use the current DORJA visual system. Follow its existing responsive patterns. If no handover design tokens exist, use:

- Jol Teal `#007C78` for active/acknowledged actions;
- Ink `#0B1F33` for major text and structured ledger surfaces;
- Paper `#FBF8F2` for neutral surfaces;
- Amber `#E79C2E` for pending/attention state;
- Leaf `#267450` for completed/accepted state;
- Safety Red `#B83D37` only for blocked/overdue/important concern state.

Do not use purple gradients, generic SaaS cards, a dashboard wall of identical rounded rectangles, or random redesigns that conflict with existing DORJA screens.

### 2. Promise Line — the signature interaction

Build a horizontal/vertical responsive timeline called **Promise Line**.

Each promise is a structured, chronological card. It must never be only a chat message.

Required promise fields:

```text
id
passportId
category
title
originalPromiseText
sourceReferenceLabel
sourceDocument/media reference optional
promisedDate optional
promisedAmount optional, BDT by default
originallyCreatedByUserId
createdAt
acknowledgementStatus
currentStatus
```

Use these categories:

```text
HANDOVER_DATE
PAYMENT_TERM
PRICE_OR_FEE
UNIT_SIZE_OR_LAYOUT
PARKING
FITMENT_OR_MATERIAL
UTILITY_OR_SERVICE
REGISTRATION_OR_DOCUMENT
OTHER
```

Use these states:

```text
DRAFT
PENDING_ACKNOWLEDGEMENT
ACKNOWLEDGED
EVIDENCE_SUBMITTED
CHANGE_PROPOSED
REMEDY_OPEN
REMEDY_IN_PROGRESS
RESOLVED
CONTESTED
ARCHIVED
```

Promise card UI must show:

```text
Category stamp
Original promise
Source reference
Promised date / amount where applicable
Current state
Who last acted, and when
Evidence count
Open remedy count
Actions allowed for the current role
```

Suggested buyer-facing copy:

```text
PROMISED
Handover by 30 June 2026

CURRENT STATE
Developer proposed a revised date

NEXT STEP
Review the proposed change by 18 August 2026
```

### 3. Promise creation and acknowledgement

Implement a guided form for adding a promise. Do not parse legal PDFs with AI. The user enters a concise promise manually and may attach supporting document/photo evidence.

Form fields:

```text
Promise category
Short title
What was promised? [text]
Source reference [e.g. Booking agreement, clause 7]
Promised date [optional]
Promised amount [optional]
Attach existing/new evidence [optional]
```

After a promise is submitted:

1. It enters `PENDING_ACKNOWLEDGEMENT`.
2. The counterpart receives an existing-app notification.
3. The counterpart can `Acknowledge`, `Propose change`, or `Mark contested` with a required short note.
4. The original text is immutable after acknowledgement. Any change creates a linked `PromiseRevision`; never overwrite historic text.

### 4. Proof Desk

Create an evidence panel on each promise.

Supported evidence types:

```text
PHOTO
DOCUMENT
RECEIPT
PAYMENT_REFERENCE
MESSAGE_NOTE
EXISTING_DORJA_CAPTURE_REFERENCE
SITE_UPDATE
OTHER
```

For each proof item display:

```text
Type
Submitted by
Submitted at
Short visible label
Optional private note where permission allows
File/view action
```

Do not use evidence labels such as “verified truth,” “legal proof,” or “fraud confirmed.” Use neutral labels such as `Submitted by buyer`, `Submitted by developer`, or `Existing DORJA capture`.

Reuse existing media/document storage and signed-URL rules. Reuse existing document viewers if present. Private documents must never be publicly retrievable through a listing URL.

### 5. Remedy Clock

Implement an issue/remedy workflow connected to a promise or directly to the handover passport.

Example:

```text
Issue: Bathroom wall tile is cracked
Linked promise: Fittings and materials
Reported by: Buyer
Reported at: [timestamp]
Developer response: Repair proposed
Proposed remedy date: [date]
Current status: Remedy in progress
```

Required remedy fields:

```text
id
passportId
linkedPromiseId optional
title
description
createdByUserId
priority: LOW | NORMAL | HIGH
status: OPEN | ACKNOWLEDGED | REMEDY_PROPOSED | IN_PROGRESS | READY_FOR_REVIEW | RESOLVED | CONTESTED | CLOSED
proposedCompletionAt optional
acceptedCompletionAt optional
closedAt optional
```

Only developer/owner/authorised representative can propose a remedy deadline. Buyer can accept, request revision, mark unresolved, or contest completion. Every action adds a timeline event.

The Remedy Clock should show days remaining only when a proposed completion date exists. It is a visibility tool, not an automatic legal deadline or enforcement mechanism.

### 6. Neutral Handover Evidence Pack

Build an exportable, neutral, chronological dossier for one passport.

The initial implementation may be a print-optimised HTML route with browser `Print / Save as PDF` if the app has no established PDF service. If it already has PDF/export tooling, use that instead.

The exported pack must include:

```text
Passport and unit identifier
Generation timestamp
Explicit neutral disclaimer
Promise Line in chronological order
Original promise text and revisions
Evidence index, showing submitter and timestamps
Remedy history and current state
Links/identifiers for allowed attached files
Handover summary: resolved, open, contested items
```

Use this disclaimer exactly or an equivalent existing legal-copy style:

> This record is a chronological summary of information submitted through DORJA by the participating parties. It does not verify legal title, determine liability, certify construction quality, or replace professional legal or technical advice.

Do not expose raw NID, identity documents, private safety records, access tokens, exact private addresses, or internal reviewer notes in the export.

### 7. QR entry point

If the existing DORJA app already has QR generation/scanning, add an authenticated QR/link entry to an eligible passport.

The QR should resolve to:

```text
/properties/:propertySlug/handover/:passportId
```

Public anonymous access must show no private passport data. Direct QR access should require an existing authenticated session and property/passport participant authorisation.

### 8. Activity and notifications

Reuse the existing notification and activity patterns.

Create a clear activity event for:

```text
PASSPORT_CREATED
PROMISE_CREATED
PROMISE_ACKNOWLEDGED
PROMISE_CHANGE_PROPOSED
PROMISE_CONTESTED
EVIDENCE_ADDED
REMEDY_OPENED
REMEDY_DATE_PROPOSED
REMEDY_DATE_ACCEPTED
REMEDY_READY_FOR_REVIEW
REMEDY_RESOLVED
REMEDY_CONTESTED
EVIDENCE_PACK_EXPORTED
```

Notify relevant participants on meaningful changes only. Do not spam every viewer or send duplicate notifications.

## Data model: integrate, do not duplicate

Inspect the existing models first. Add the minimum new tables/models and relations needed. Use the existing ORM, IDs, timestamps, soft-delete policy, audit mechanism, and migration approach.

Conceptual models to map into the existing schema:

```text
HandoverPassport
  - property/listing/unit relation
  - buyer participant relation
  - developer/owner/agent participant relation
  - status
  - agreement date
  - created/updated timestamps

HandoverParticipant
  - passport relation
  - existing user relation
  - role: BUYER | DEVELOPER_REPRESENTATIVE | OWNER_REPRESENTATIVE | VIEWER
  - invited/accepted timestamps

Promise
  - passport relation
  - category, title, immutable original text
  - source reference
  - optional date/amount
  - current state
  - created by existing user

PromiseRevision
  - promise relation
  - revision number
  - proposed text/date/amount changes
  - proposer, note, state, timestamps

HandoverEvidence
  - passport relation
  - optional promise/remedy relation
  - submitter existing user relation
  - evidence type, label, storage/media reference, visible note
  - created timestamp

RemedyIssue
  - passport relation
  - optional promise relation
  - description, priority, state
  - proposed/accepted completion timestamps
  - reporter and last actor

HandoverEvent
  - passport relation
  - actor existing user relation nullable for system action
  - event type
  - related entity type/id
  - safe public metadata only
  - timestamp
```

Do not duplicate `User`, `Property`, `Listing`, `MediaAsset`, `Document`, `Conversation`, `Notification`, or `AuditLog` when the existing app already has equivalents. Link to them.

## API requirements

Follow existing API style, auth, validation, errors, and naming. If an HTTP API is used, add equivalents of:

```text
GET    /properties/:propertyId/handover-passport
POST   /properties/:propertyId/handover-passports
PATCH  /handover-passports/:passportId

GET    /handover-passports/:passportId/promises
POST   /handover-passports/:passportId/promises
POST   /handover-promises/:promiseId/acknowledge
POST   /handover-promises/:promiseId/revisions
POST   /handover-promises/:promiseId/contest

POST   /handover-passports/:passportId/evidence
GET    /handover-passports/:passportId/timeline

GET    /handover-passports/:passportId/remedies
POST   /handover-passports/:passportId/remedies
POST   /handover-remedies/:remedyId/propose-date
POST   /handover-remedies/:remedyId/accept-date
POST   /handover-remedies/:remedyId/mark-ready
POST   /handover-remedies/:remedyId/resolve
POST   /handover-remedies/:remedyId/contest

GET    /handover-passports/:passportId/evidence-pack
POST   /handover-passports/:passportId/qr
```

Every mutating route must enforce participant and property-authority permissions server-side. Validate input with the project’s established schema library. Use database transactions for revisions, acknowledgement, remedy state changes, and timeline-event creation.

## Security and privacy requirements

1. Preserve existing DORJA role-based access controls.
2. Do not expose a handover passport in public property search/listing responses.
3. Generate evidence downloads through existing authorised signed URLs or equivalent controlled access.
4. Do not put private evidence URLs, personal identity details, internal notes, or raw file paths into websocket events or logs.
5. Do not expose a buyer’s phone number, NID, private address, or safety information through passport export.
6. Record authorised changes in the existing audit/activity system.
7. Use `Cache-Control: no-store` on private passport/export responses if the current platform supports response headers.
8. Keep the original promise immutable; create revisions rather than editing history.
9. Never calculate a “trust score,” “legal win probability,” or “developer reputation score.”

## UI and interaction specification

### Desktop property page integration

Add an `Handover Passport` tab/section adjacent to existing property evidence/details. The core desktop composition should be:

```text
Left:  passport summary and filters
Centre: Promise Line timeline — the dominant visual
Right:  active promise/remedy inspector and contextual actions
```

Avoid a generic table-only interface. Make the Promise Line feel like a legible record of the unit’s story.

### Mobile integration

On mobile, use:

```text
Passport header
Completion / open issues strip
Promise Line as stacked chronology
Promise or remedy detail bottom sheet / full screen
Persistent “Add promise” or “Report issue” action for authorised roles
```

Keep primary actions large and explicit. Support Bangla/English existing translation patterns. Preserve accessibility, keyboard flow, focus states, and reduced motion preferences.

### Visual state language

```text
Teal  = acknowledged / active
Amber = awaiting action / proposed date
Leaf  = resolved / accepted
Red   = contested / overdue / important attention
Ink   = structured historical record
Paper = neutral background
```

Every state must also have a written label; never use colour alone.

## Seed/demo data

Add a clearly labelled development/demo passport connected to an existing demo property, such as:

```text
Flat B-7 · Green Avenue (Demo Unit)
```

Seed three promises:

1. `HANDOVER_DATE` — “Handover by 30 June 2026.”
2. `PARKING` — “One parking space included with the unit.”
3. `FITMENT_OR_MATERIAL` — “Bathroom wall tile finish as stated in booking specification.”

Seed one remedy issue connected to the tile promise:

```text
Bathroom wall tile cracked
Status: REMEDY_PROPOSED
Proposed completion: 7 days after report
```

Mark every seed label visibly as `Demo data`. Do not fabricate customer names, reviews, testimonials, ratings, or legal outcomes.

## Required tests

Add/extend tests using the existing test framework.

### Backend/domain tests

- A buyer participant can read only their own unit passport.
- Non-participant cannot read, export, or upload evidence.
- Non-authorised developer/agent cannot create promises.
- Original promise text cannot be overwritten after acknowledgement.
- A revision creates a new historical record and timeline event.
- Remedy state transition is validated.
- Only authorised representative can propose remedy completion date.
- Evidence export excludes private/identity/internal fields.
- Passport QR/deep link requires authorisation.
- Timeline events are created atomically with relevant mutations.

### UI/end-to-end tests

- Existing property detail remains functional.
- Eligible property opens the Handover Passport.
- Buyer creates a promise and developer acknowledges it.
- Buyer opens remedy issue and developer proposes deadline.
- Timeline shows correct chronological status.
- Evidence Pack is printable/exportable.
- Existing DORJA 3D/Reality Passport, messaging, offers, and SafeView flows continue functioning.

## Completion checklist

Do not report completion until all are true:

- Existing DORJA feature set is preserved and still reachable.
- DORJA Handover is connected to existing properties/listings, users, storage, and notifications.
- Promise Line, proof submission, remedy clock, chronological timeline, export, and QR/deep link all work.
- All copy is precise and does not make legal/technical verification claims.
- Private passport data is never exposed by public listing endpoints.
- Existing design language is followed.
- Database migrations are safe and reversible according to existing project conventions.
- Relevant unit/API/UI tests pass.
- The app builds successfully with the repository’s standard commands.

## Final response format

When finished, respond with:

1. What existing DORJA systems you integrated with.
2. New routes/components/models/migrations added.
3. Feature flow from property detail to Evidence Pack.
4. Security/privacy protections enforced.
5. Tests run and outcomes.
6. Any clearly labelled limitations or follow-up integrations.
```

---

## Use note

Give Claude Code this prompt together with the existing DORJA codebase and the earlier DORJA master specification. This prompt **overrides only the earlier suggestion to defer broader DORJA features**: the full DORJA application remains in place and DORJA Handover is added on top of it.
