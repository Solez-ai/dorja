---
name: dorja-expo-spatial-capture
description: Build or modify DORJA’s Expo/React Native mobile capture, Reality Passport, and browser-tour workflows. Use when implementing guided hold-to-capture spatial checkpoints, Expo Camera capture, room-tour evidence, capture processing, listing provenance, or a web panorama/room-graph handoff without requiring Android Studio or claiming automatic survey-grade 3D.
---

# DORJA Expo Spatial Capture

Build the DORJA capture experience as **guided spatial evidence**, not as a generic camera flow and not as an overclaimed 3D scanner.

## Non-negotiable product contract

Implement these statements as product and engineering constraints.

1. Use **React Native + Expo + TypeScript**. Build Android through EAS; do not require manual Android Studio or Gradle edits.
2. Treat the default capture as **Hold-to-Capture Spatial Checkpoints** on ordinary phones.
3. Describe the browser result as a **Reality Passport**, **Navigable Room Record**, or **Room Tour**.
4. Do not call ordinary phone capture a survey-grade 3D mesh, a legally verified digital twin, structural inspection, or exact measurement tool.
5. Bind each capture session to one listing, one authenticated capturer, route version, capture timestamp, and review status.
6. Keep exact addresses, raw identity data, and private evidence out of public capture/tour responses.
7. Label every public tour with its actual source and freshness: `Seller-captured`, `Agent-verified`, `Incomplete capture`, or `Capture expired`.

Read `references/hold-to-capture-contract.md` before changing capture state, checkpoint quality, API payloads, or the listing-to-tour mapping.

## Choose the correct implementation level

| Need | Implement now | Avoid |
|---|---|---|
| Competition demo on normal Android | Expo Camera + long-press checkpoint + room graph | A custom ARCore engine or cloud mesh reconstruction |
| Browser exploration | Marzipano/Pannellum-style panorama viewer and room nodes | A fake free-roam 3D world when only still images exist |
| Better immersive room view | Import a real equirectangular panorama | Claiming a set of normal photos is a full panorama |
| Advanced supported-device scan | Add a native module only through Expo development build | Making LiDAR a baseline requirement |
| Build Android | `eas build --platform android --profile development` | Android Studio/Gradle workflow unless a separate native debugging task explicitly requires it |

## Implementation workflow

### 1. Establish listing and capture permissions

Before capture begins, verify server-side that:

- the user is the listing owner, authorised agent, or invited representative;
- the listing is not archived/restricted;
- `listingId`, `capturedByUserId`, and route template version are persisted in a new `CaptureSession`;
- public publishing remains blocked until property-authority review reaches the required state.

Never create a capture session from a public listing page without authenticated role checks.

### 2. Use the Hold-to-Capture UX

Show a full-screen `CameraView` with one current checkpoint, a horizon guide, route count, torch/help actions, and a large press-and-hold control.

For each checkpoint:

1. Display a human instruction, for example: `Stand near the centre. Face the main wall.`
2. Begin a hold timer and motion-sample window on press-in.
3. Require at least 1.2 seconds of continuous hold; target 1.5–2.0 seconds.
4. Calculate stability from device-motion variance.
5. Capture a primary still only after the threshold is met.
6. Upload media through a signed URL and persist checkpoint metadata.
7. Receive accepted/retake state from the backend.
8. Show a specific next action, for example: `Captured. Next: face the doorway.`

Use haptics lightly: one light impact at hold start, success notification after accepted capture, error notification only after a failed capture.

### 3. Record deterministic quality signals

Record only the signals the app actually has:

| Signal | Purpose | Do not claim |
|---|---|---|
| Hold duration | Reject accidental taps | Camera understands a room |
| Motion variance | Prompt steady holding | Exact camera position |
| Heading/pitch/roll | Annotate viewpoint direction | Survey geometry |
| Luminance | Prompt for torch/light | Daylight or liveability score |
| Blur heuristic | Suggest retake | Property condition detection |
| Route coverage | Explain missing evidence | Structural completeness |
| Perceptual hash | Avoid duplicate checkpoint media | Identity or authenticity proof |

Never block a user silently. Return `RETAKE_SUGGESTED` with a plain-language explanation and retain the raw attempt only according to the documented retention policy.

### 4. Persist and process capture safely

Use this state sequence:

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

Persist checkpoint rows before slow media processing. Support resumable/retryable upload queue on mobile. The worker validates MIME type, size, hash, dimensions, blur, duplication, and route coverage; it does not run a fictional inspection AI.

### 5. Publish the Reality Passport

Create a public-safe DTO with:

- public listing slug and approximate area;
- room type, display name, preview/panorama signed URL where allowed;
- capture date and route coverage;
- review level and missing-room list;
- room graph nodes and edges;
- no exact address, raw EXIF, raw device metadata, identity data, or private evidence.

Use the room graph for movement between rooms. Only render a free-look panorama control when the room has an actual panorama source.

### 6. Build the web handoff

The web app is a viewing/discovery surface, not a capture surface.

1. Fetch the public Reality Passport by listing slug.
2. Load room preview first for Lite Tour.
3. Load high-resolution panorama tiles only when user opens an eligible panorama.
4. Render room edges as labelled doorway actions.
5. Expose room type tags so Twin View can compare kitchen-to-kitchen and balcony-to-balcony.
6. Keep proof requests, offers, and SafeView appointment actions separate from the tour renderer.

## Required Expo modules and patterns

Use:

```text
expo-camera        Camera preview, still capture, QR scanning
expo-haptics       Hold start/success/error feedback
expo-sensors       Device-motion stability hint
expo-file-system   Local queue metadata and retry support
expo-secure-store  Session/credential storage only
expo-router        Screens and deep links
```

Keep `CameraView` mounted only while the capture screen is focused. Use `useCameraPermissions`; provide a clear permission-recovery screen. Test physical Android device capture on the same LAN as the local backend using `EXPO_PUBLIC_API_URL` with the computer’s LAN IP, never `localhost`.

## Mandatory data contract

Implement or preserve these backend records:

```text
CaptureSession(listingId, capturedByUserId, routeVersion, status, coverageScore, captureTimestamp)
CaptureRoomProgress(captureSessionId, roomId, requiredCheckpointCount, acceptedCheckpointCount, status)
SpatialCheckpoint(captureSessionId, roomId, checkpointKey, holdDurationMs,
                  orientation, stabilityScore, brightnessScore, blurScore,
                  coverageStatus, primaryMediaAssetId)
MediaAsset(captureSessionId, storageKey, sha256, sourceType, qualityStatus)
TourNode(roomId, previewAssetId, panoramaAssetId?)
TourEdge(fromNodeId, toNodeId, doorwayLabel)
RealityPassport(listingId, captureSessionId, reviewLevel, expiresAt, publicStatus)
```

Do not serialise raw local device paths, private MinIO keys, EXIF payloads, or exact property coordinates to the public API.

## DORJA UI rules

Use DORJA visual language:

- Jol Teal `#007C78` for active capture ring, accepted checkpoint, and primary action;
- Ink `#0B1F33` for high-contrast camera HUD panels;
- Paper `#FBF8F2` for non-camera screens;
- Amber `#E79C2E` for retake/pending state;
- Leaf `#267450` for accepted/confirmed state;
- Safety Red `#B83D37` only for blocking error or safety concern;
- English: Space Grotesk/IBM Plex Sans; Bangla: Hind Siliguri;
- preserve large 48 dp minimum touch targets and visible room-progress text.

Do not use neon/cyberpunk effects, purple gradients, tiny controls, or generic all-rounded-card dashboard layout.

## Test checklist

Verify all of these before claiming the feature complete:

- Camera permission denial and recovery work.
- Accidental tap under minimum hold duration does not create accepted checkpoint.
- High motion generates a retake suggestion.
- Valid hold persists a checkpoint and captures a local retry record.
- Network interruption queues then resumes upload.
- Duplicate source media cannot satisfy two checkpoint keys.
- Public property page never exposes exact address/identity/private evidence.
- Room with ordinary checkpoint stills does not display false panorama/free-look claims.
- Real panorama source opens in web viewer.
- Capture source and freshness label appears in web and mobile views.
- Twin View only compares equivalent tagged room types.
- All capture/processing failures give a recoverable next action.

## Failure handling

| Failure | Required behaviour |
|---|---|
| Camera unavailable | Explain device/permission issue and offer retry. |
| Poor light | Offer torch/lighting instruction, not an opaque error. |
| Motion too high | Explain “Hold the phone still” and allow immediate retry. |
| Upload fails | Queue locally, show `Waiting to upload`, retry on connectivity. |
| Native AR library unavailable | Keep hold-to-capture flow working; do not block core capture. |
| Panorama processing fails | Publish room previews/graph only if policy permits; label source accurately. |
| Listing authority expires | Keep capture private; block new public passport publication. |

## Completion standard

Consider the feature complete only when a lister can follow an understandable room route on an ordinary Android phone, create an auditable capture session, upload/retry media to the local backend, and publish a truthfully labelled browser-navigable Reality Passport tied to the right verified listing.
