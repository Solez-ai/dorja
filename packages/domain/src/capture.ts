import type { RoomType } from '@dorja/contracts';

export type RouteCheckpoint = {
  key: string;
  roomType: RoomType;
  instruction: string;
  expectedDirection: 'MAIN_WALL' | 'OPPOSITE_WALL' | 'WINDOW_SIDE' | 'DOORWAY' | 'LOOK_BACK' | 'OUTWARD_VIEW' | 'EDGE_VIEW';
  minimumHoldMs: number;
  order: number;
};

// Standard room: 5 checkpoints
export const standardRoomCheckpoints: RouteCheckpoint[] = [
  { key: 'centre-main', roomType: 'LIVING_ROOM', instruction: 'Stand near the centre. Face the main wall.', expectedDirection: 'MAIN_WALL', minimumHoldMs: 1200, order: 1 },
  { key: 'centre-opposite', roomType: 'LIVING_ROOM', instruction: 'Turn around and face the opposite wall.', expectedDirection: 'OPPOSITE_WALL', minimumHoldMs: 1200, order: 2 },
  { key: 'window-side', roomType: 'LIVING_ROOM', instruction: 'Face the window or balcony side.', expectedDirection: 'WINDOW_SIDE', minimumHoldMs: 1200, order: 3 },
  { key: 'doorway', roomType: 'LIVING_ROOM', instruction: 'Face the doorway.', expectedDirection: 'DOORWAY', minimumHoldMs: 1200, order: 4 },
  { key: 'doorway-back', roomType: 'LIVING_ROOM', instruction: 'Stand in the doorway and look back into the room.', expectedDirection: 'LOOK_BACK', minimumHoldMs: 1200, order: 5 },
];

// Kitchen/Bathroom: 4 checkpoints
export const kitchenBathroomCheckpoints: RouteCheckpoint[] = [
  { key: 'entrance', roomType: 'KITCHEN', instruction: 'Stand at the entrance and capture the room overview.', expectedDirection: 'MAIN_WALL', minimumHoldMs: 1200, order: 1 },
  { key: 'main-counter', roomType: 'KITCHEN', instruction: 'Face the main counter or wall.', expectedDirection: 'OPPOSITE_WALL', minimumHoldMs: 1200, order: 2 },
  { key: 'utility-side', roomType: 'KITCHEN', instruction: 'Face the utility or fixture side.', expectedDirection: 'DOORWAY', minimumHoldMs: 1200, order: 3 },
  { key: 'doorway-exit', roomType: 'KITCHEN', instruction: 'Stand at the doorway and look back.', expectedDirection: 'LOOK_BACK', minimumHoldMs: 1200, order: 4 },
];

// Balcony: 3 checkpoints
export const balconyCheckpoints: RouteCheckpoint[] = [
  { key: 'entrance', roomType: 'BALCONY', instruction: 'Stand at the balcony entrance.', expectedDirection: 'MAIN_WALL', minimumHoldMs: 1200, order: 1 },
  { key: 'outward-view', roomType: 'BALCONY', instruction: 'Face outward and capture the view.', expectedDirection: 'OUTWARD_VIEW', minimumHoldMs: 1200, order: 2 },
  { key: 'edge-view', roomType: 'BALCONY', instruction: 'Move to the left or right edge.', expectedDirection: 'EDGE_VIEW', minimumHoldMs: 1200, order: 3 },
];

// Bedroom: same as standard (5 checkpoints) but different room type label
export const bedroomCheckpoints: RouteCheckpoint[] = standardRoomCheckpoints.map(cp => ({
  ...cp,
  roomType: 'BEDROOM' as RoomType,
}));

// Dining room: same as standard (5 checkpoints)
export const diningRoomCheckpoints: RouteCheckpoint[] = standardRoomCheckpoints.map(cp => ({
  ...cp,
  roomType: 'DINING_ROOM' as RoomType,
}));

export function getCheckpointsForRoomType(roomType: RoomType): RouteCheckpoint[] {
  switch (roomType) {
    case 'KITCHEN':
    case 'BATHROOM':
      return kitchenBathroomCheckpoints.map(cp => ({ ...cp, roomType }));
    case 'BALCONY':
      return balconyCheckpoints;
    case 'BEDROOM':
      return bedroomCheckpoints;
    case 'DINING_ROOM':
      return diningRoomCheckpoints;
    case 'LIVING_ROOM':
    default:
      return standardRoomCheckpoints;
  }
}

/**
 * Capture confidence score calculation.
 * captureConfidence =
 *   40% route checkpoint coverage +
 *   25% accepted quality checks +
 *   20% viewpoint diversity +
 *   15% doorway connectivity
 */
export function calculateCaptureConfidence(params: {
  routeCoverage: number; // 0-1
  acceptedQualityRatio: number; // 0-1
  viewpointDiversity: number; // 0-1
  doorwayConnectivity: number; // 0-1
}): number {
  return Math.round(
    params.routeCoverage * 0.4 +
    params.acceptedQualityRatio * 0.25 +
    params.viewpointDiversity * 0.2 +
    params.doorwayConnectivity * 0.15
  ) * 100;
}

// Quality thresholds (configurable, start conservative)
export const CAPTURE_QUALITY = {
  MINIMUM_HOLD_MS: 1200,
  TARGET_HOLD_MS_MIN: 1500,
  TARGET_HOLD_MS_MAX: 2000,
  MINIMUM_STABILITY_HINT: 60,
  BLUR_THRESHOLD: 50,
  MAX_FILE_SIZE_BYTES: 20 * 1024 * 1024, // 20MB
  ALLOWED_MIME_TYPES: ['image/jpeg', 'image/png', 'image/heic', 'image/heif', 'video/mp4'],
} as const;
