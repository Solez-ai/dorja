# Hold-to-Capture Contract

Load this reference when implementing database migrations, API contracts, capture worker logic, or browser-tour DTOs.

## Checkpoint template

```ts
export type RouteCheckpoint = {
  key: string;
  roomType: string;
  instruction: string;
  expectedDirection: 'MAIN_WALL' | 'OPPOSITE_WALL' | 'WINDOW_SIDE' | 'DOORWAY' | 'LOOK_BACK' | 'OUTWARD_VIEW' | 'EDGE_VIEW';
  minimumHoldMs: number;
  order: number;
};

export const standardRoomCheckpoints: RouteCheckpoint[] = [
  { key: 'centre-main', roomType: 'LIVING_ROOM', instruction: 'Stand near the centre. Face the main wall.', expectedDirection: 'MAIN_WALL', minimumHoldMs: 1200, order: 1 },
  { key: 'centre-opposite', roomType: 'LIVING_ROOM', instruction: 'Turn around and face the opposite wall.', expectedDirection: 'OPPOSITE_WALL', minimumHoldMs: 1200, order: 2 },
  { key: 'window-side', roomType: 'LIVING_ROOM', instruction: 'Face the window or balcony side.', expectedDirection: 'WINDOW_SIDE', minimumHoldMs: 1200, order: 3 },
  { key: 'doorway', roomType: 'LIVING_ROOM', instruction: 'Face the doorway.', expectedDirection: 'DOORWAY', minimumHoldMs: 1200, order: 4 },
  { key: 'doorway-back', roomType: 'LIVING_ROOM', instruction: 'Stand in the doorway and look back into the room.', expectedDirection: 'LOOK_BACK', minimumHoldMs: 1200, order: 5 },
];
```

Kitchen/bathroom routes use four checkpoints. Balcony routes use entrance, outward view, and edge view. Templates live in database JSON so they can change without a mobile binary release.

## Client payload

```ts
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
```

Never trust local scores as final. The server recalculates/validates its own media quality when possible.

## Server output

```ts
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
```

## Quality thresholds

Keep thresholds configurable. Start conservatively and tune using permissioned real captures. Never hardcode “accuracy” claims.

```text
minimum hold duration: 1200 ms
target hold duration: 1500–2000 ms
minimum local stability hint: 60 / 100
route completeness: accepted checkpoints / required checkpoints
```

## Public Reality Passport DTO

```ts
export type PublicRealityPassport = {
  listing: {
    slug: string;
    title: string;
    publicArea: string;
    intent: 'RENT' | 'SALE';
    priceAmount: number;
    currency: 'BDT';
    livePulse: { status: 'AVAILABLE' | 'UNCONFIRMED' | 'HELD'; confirmedAt?: string };
  };
  reality: {
    reviewLevel: 'INCOMPLETE' | 'SELLER_CAPTURED' | 'AGENT_VERIFIED' | 'EXPIRED';
    capturedAt?: string;
    coverageScore: number;
    missingRoomLabels: string[];
    sourceSummary: 'HOLD_TO_CAPTURE' | 'IMPORTED_PANORAMA' | 'PRO_SPATIAL_SCAN' | 'MIXED';
  };
  rooms: Array<{
    id: string;
    roomType: string;
    displayName: string;
    previewUrl: string;
    panoramaUrl?: string;
    sourceType: string;
  }>;
  edges: Array<{ fromRoomId: string; toRoomId: string; doorwayLabel: string }>;
};
```

The DTO must not contain `exactAddress`, raw latitude/longitude, phone number, user identity fields, evidence paths, raw checkpoint sensor values, or unprocessed image locations.
