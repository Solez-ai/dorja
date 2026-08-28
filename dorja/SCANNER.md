# 3D Panorama Scanner — Architecture & Rebuild Guide

> **Status:** We need to add it

---

## What It Was

The 3D Panorama Scanner was a room-scanning feature that let hosts capture 360° panoramas of individual rooms in their property. It was displayed to buyers as an interactive cylindrical panorama viewer.

---

## How It Worked

### Host Flow (Capture)

1. **Room Selection** — Host picks a listing → sees a list of rooms → taps "Scan 3D" on a room
2. **Pre-Capture Screen** — Full-screen camera preview with:
   - Cylindrical dot-grid overlay (cyan dots curving inward on all 4 edges, giving a wide-angle scope feel)
   - Center instruction card: phone icon with green checkmark badge + "Hold your phone upright in portrait mode"
   - "PRESS TO START" label with down-caret icon above a green circular shutter button
   - Bottom bar: settings/layers icon (left), gyro status toggle (right)
3. **Active Scanning** — Gyroscope-driven capture phase:
   - 12 target angles spaced 30° apart (0°, 30°, 60° ... 330°) WHICH TH SOFTARE INETRACTIVELY SHPOED TO TE SER COPIYING THEIR PHOENS PANAROMA UI
   - Compass ring with 12 angle nodes rendered on camera preview
   - Real gyroscope integration via `TYPE_ROTATION_VECTOR` sensor
   - Auto-capture when device aligns within ±20° of each target angle (350ms hold time for stability) MUST BE VISUALLY SHOW CIONNECT WITH A CROSSHAIR THAT ACTUALLY WORKS
   - Progress strip: green = captured, blue = current target, gray = pending
   - Alignment banner: "ALIGNED — CAPTURING" or "Rotate to angle X"
   - Red stop button + "PRESS STOP WHEN DONE"
4. **Result Screen** — Scan summary (coverage, angles, room name) → "Save 3D Scan to [Room]"

### Buyer Flow (Viewing)

1. Navigate to listing → "See 3D Scans" button → Tour Viewer
2. **Cylindrical Panorama Viewer:**
   - Frames displayed as a horizontal panorama strip filling screen height
   - Frames duplicated for seamless wrap-around (scroll past end → loops back)
   - Gyroscope auto-panning: rotate phone to look around
   - Drag-to-pan with touch override
   - Gradient overlays at top/bottom for depth
   - Badge: "360° REAL PANORAMA • N FRAMES STITCHED"
   - Room selector tabs when multiple rooms have scans

---

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Camera Preview | CameraX `Preview` use case |
| Frame Capture | CameraX `ImageCapture` use case (JPEG format) |
| Gyroscope | Android `SensorManager` with `TYPE_ROTATION_VECTOR` |
| Panorama Rendering | Jetpack Compose `Canvas` with horizontal scroll |
| Data Storage | Room DB field `panoramaData: String?` (JSON) |
| Navigation | `room_scanner/{listingId}` route in `DorjaNavHost` |

---

## Data Model

### Room entity fields (existing in Room DB)

```kotlin
data class Room(
    val id: String,
    val listingId: String,
    val displayName: String,
    val has3DScan: Boolean = false,      // Still exists in DB
    val panoramaData: String? = null      // JSON string, still exists in DB
)
```

### Panorama JSON format

```json
{
  "frames": [
    "/data/.../pano_0_1693123456.jpg",
    "/data/.../pano_30_1693123457.jpg",
    "/data/.../pano_60_1693123458.jpg"
  ],
  "angleCount": 12,
  "roomId": "r_abc123",
  "timestamp": 1693123456000
}
```

Each frame file is a JPEG captured at the corresponding angle. The viewer stitches them left-to-right into a panoramic strip.

---

## Files That Existed

| File | Purpose |
|------|---------|
| `ui/scanner/RoomScannerScreen.kt` | **Deleted** — Full scanner UI (4 phases: ROOM_SELECT → PRE_CAPTURE → SCANNING → RESULT) |
| `ui/tour/TourViewerScreen.kt` | **Simplified** — Was panoramic viewer, now shows "Coming Soon" placeholder |
| `ui/listing/CreateListingScreen.kt` | **Cleaned** — Removed 3D Scan BentoCard section (scan buttons, room scan list) |
| `ui/detail/PropertyDetailScreen.kt` | **Kept** — Still has "See 3D Scans" button linking to tour viewer |
| `ui/seller/HostListingsScreen.kt` | **Cleaned** — Removed "Scan 3D Rooms" button |
| `ui/navigation/DorjaNavHost.kt` | **Cleaned** — Removed `room_scanner/{listingId}` route |
| `ui/capture/CaptureScreen.kt` | **Cleaned** — Removed "3D Room Scanner (ARCore)" action card |
| `gl/Room3DRenderer.kt` | **Still exists** — OpenGL ES 2.0 renderer (was fallback, never used for real panoramas) |

---

## What's Needed
### Core Requirements

1. **Real camera frame capture** — Use CameraX `ImageCapture` to take actual JPEG photos at each of the 12 angles
2. **Gyroscope alignment** — `TYPE_ROTATION_VECTOR` sensor to detect when the phone is pointed at each target angle
3. **Cylindrical panorama stitching** — Arrange captured frames left-to-right in a scrollable/gyro-driven strip
4. **No fake data** — Every panorama must contain real captured photos, not simulated strings

### Rebuild Checklist

- [ ] Recreate `RoomScannerScreen.kt` with CameraX + ImageCapture
- [ ] Add `TYPE_ROTATION_VECTOR` sensor listener for angle detection
- [ ] Implement 4-phase UI: Room Select → Pre-Capture → Scanning → Result
- [ ] Save captured frames to cache as `pano_{angle}_{timestamp}.jpg`
- [ ] Store file paths in `room.panoramaData` as JSON
- [ ] Rebuild `TourViewerScreen.kt` as cylindrical panorama viewer
- [ ] Add gyroscope-driven panning + drag-to-pan in viewer
- [ ] Re-add scanner route in `DorjaNavHost`
- [ ] Re-add scan buttons in `CreateListingScreen` and `HostListingsScreen`
- [ ] Add back `CaptureScreen` 3D scanner action card

### UI Design Notes

- **Dot-grid overlay:** 80+ cyan dots on top/bottom edges curving inward (cylindrical scope effect), plus side dots
- **Shutter button:** Green circle with white inner circle during pre-capture; morphs to red stop button during scanning
- **Progress indicator:** Horizontal strip below camera, colored green (captured) / blue (current) / gray (pending)
- **Alignment banner:** Translucent overlay at center: "ALIGNED — CAPTURING" (cyan) or "Rotate to angle X°" (gray)
- **Gyro toggle:** Right side of bottom bar, clickable ON/OFF

### Camera Configuration

```
ImageCapture.Builder()
    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
    .setTargetRotation(Surface.ROTATION_0)
    .build()
```

Orientation: portrait (ROTATION_0). Frame size determined by device camera resolution.

---

## Dependencies Used

```gradle
// CameraX
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// Compose (already in project)
implementation("androidx.compose.foundation:foundation:...")
implementation("androidx.compose.material:material-icons-extended:...")
```

---

## Key Architecture Decisions

1. **Why cylindrical, not spherical?** Mobile phones scan rooms by rotating horizontally. Vertical tilt is minimal. A cylindrical panorama (horizontal strip) is natural for room scanning.

2. **Why 12 angles (30° spacing)?** Provides enough overlap between frames for smooth panning while keeping capture time under 2 minutes. 30° is the standard for consumer panorama apps.

3. **Why 350ms hold time?** Balances stability (prevents accidental captures during quick movements) with usability (feels responsive).

4. **Why frames saved as separate JPEGs?** Allows the viewer to load frames on-demand during scrolling, rather than stitching into a single massive bitmap which would OOM on large panoramas.

The pohones Macro-est lense mustbe us er(.08 or .5x)
