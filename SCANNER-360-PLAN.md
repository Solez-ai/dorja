# DORJA Scanner 2.0 — Omnidirectional 360° × 180° Spherical Capture

> **Goal:** upgrade the room scan from a single horizontal ring (360° around, fixed height)
> to a full **spherical / omnidirectional** capture — 360° horizontal × 180° vertical
> (zenith → nadir) — so buyers can look **up at the ceiling and down at the floor** inside
> a true sphere view in the tour.
>
> Omnidi = all directions; a full sphere covers 4π steradians of solid angle. Today we only
> capture a band around the equator of that sphere.
>
> **Two hard product requirements:**
> 1. **Every inch of the room, from a good angle.** The scan must cover the entire sphere
>    with no holes — walls, floor, ceiling, every corner — and the viewer must let the
>    buyer zoom in far enough to actually inspect details (switches, cracks, window views).
> 2. **Auto-light.** Phone cameras auto-expose differently when tilted at the ceiling vs
>    the floor vs a window. Without automatic exposure/white-balance normalization the
>    stitched sphere would show ugly brightness bands at every seam. A dedicated auto-light
>    pipeline runs at capture *and* stitch time so joins are invisible.

**Rule of the house:** all builds run via GitHub Actions. Never build with Gradle locally —
every phase below ends with a push and a green CI check.

---

## 0. Current architecture (as of `a1f3c08`)

| Piece | File | Today |
|---|---|---|
| Capture UI | `ui/scanner/RoomScannerScreen.kt` (1004 lines) | `TOTAL_SHOTS = 12` frames in one horizontal ring. Each frame stores **only** `FrameData(path, heading)`. Gyro `pitch` is read for *guidance* only (elevation profile: shots 1/5 tilt −15°, shot 3 +15°) — every frame is still shot from the same vertical band. |
| Stitching | `stitchFramesInternal()` in the same file | Renders into a fixed **4096×2048** 2:1 equirect canvas, but maps frames using **heading only** (`CAMERA_HFOV_DEG = 63°`). Vertical coverage is whatever one ring catches (~±25–30° around the horizon); top/bottom of the canvas are dead zones. |
| Storage | `Room.panoramaData` JSON (`buildJson()`) | `{"stitchedPanorama": "/path", ...}`. `Scan.scanDataJson` exists too. **No Room schema change needed** — the JSON blob is free-form. |
| Viewer | `ui/tour/PanoramaViewerScreen.kt` (334 lines) | Loads bitmap (≤4096 px wide), **cylindrical** per-column sampling, drag pans `panX` only, gyro feeds **yaw only**. Vertical look is impossible. Landscape-locked already. |

**Gap to close:** frames have no `pitch`/row metadata → stitcher can't fill the sphere →
viewer can't tilt. All three layers must move together, but the JSON storage lets us do it
**without a destructive DB migration**.

---

## 1. Capture protocol — the spherical grid

A phone camera covers ≈ 63° horizontal × ≈ 48° vertical (portrait). To cover −90°…+90°
vertically with overlap, sweep the phone in **rings** at these pitch targets
(existing convention: negative pitch = tilted back/up, positive = tilted forward/down):

```
Ring 0  (cap)   pitch  +90°  zenith        — 1 shot straight up
Ring 1          pitch  +70°  high ceiling  — 12 stops around
Ring 2          pitch  +35°  upper walls   — 12 stops
Ring 3          pitch   0°   horizon       — 12 stops   (current behavior)
Ring 4          pitch  −35°  lower walls   — 12 stops
Ring 5          pitch  −70°  floor         — 12 stops
Ring 6  (cap)   pitch  −90°  nadir         — 1 shot straight down (at feet)
                                     TOTAL: 62 shots
```

- Ring spacing (35–40°) guarantees 8–13° vertical overlap with a 48° vFOV; horizontal
  overlap stays the existing 12 × 30° steps (63° hFOV → 33° overlap).
- The two cap shots fill the poles where ring geometry degenerates.

**Two capture modes (user chooses on the scanner intro screen):**

| Mode | Rings | Shots | Vertical coverage | Use |
|---|---|---|---|---|
| **Full Sphere** (default) | 5 rings + 2 caps | 62 | true ±90° | Competition demo, final listings |
| **Quick** | 3 rings (+35/0/−35) | 36 | ~±60° | Impatient users; poles get synthesized fill |

Quick mode still produces a **valid full 2:1 equirect** — the stitcher synthesizes
pole fill (see §3), so the viewer never breaks; the poles are just softer.

**Light discipline at capture time (first half of the auto-light story):**
- Lock AE/AWB via CameraX `cameraControl` while inside a ring, re-locking at the first
  frame of each ring, so frames within one ring share exposure. Between rings the stitcher
  normalizes (§3). Where supported, use `MeteringPoint` on the room's mid-tone wall
  (not the window) before locking, so bright windows don't darken the whole frame.
- The scanner intro explains one rule: *move slowly, let the exposure settle before the
  first tap of each ring.*

**Coverage guarantee — "every inch of the room":**
- The grid above is designed so every direction of the sphere is covered by ≥1 frame
  with 8–13° overlap in both axes (35–40° ring spacing vs 48° vFOV; 30° steps vs 63° hFOV).
- Before the Done button enables, `ScanGeometry` computes a **coverage map** (e.g., a
  72×36 lon/lat occupancy grid) from every frame's (heading, pitch, FOV). Any uncovered
  cell → the UI points at the missing slot ("ceiling ring: 2 shots missing at 120°") and
  blocks finishing. A percentage badge ("Sphere coverage: 100%") doubles as a quality
  signal shown on the listing's tour card.

---

## 2. Phase 1 — Data model & schema v2 (small)

**Files:** `RoomScannerScreen.kt` (FrameData, buildJson), new `scanner/ScanGeometry.kt`.

1. Extend `FrameData` → `(path, heading, pitchDeg, row, col)`. `row`/`col` come from the
   capture grid; `heading`/`pitchDeg` come from the rotation vector at shutter time.
2. Bump scan JSON to **version 2**:
   ```json
   {
     "version": 2,
     "projection": "equirectangular",
     "coverage": "360x180",
     "mode": "full|quick",
     "cameraHfovDeg": 63, "cameraVfovDeg": 48,
     "rows": [ {"pitchDeg": 0, "frames": [{"path": "...", "heading": 12.3, "col": 0}] } ],
     "caps": {"zenith": {"path": "..."}, "nadir": {"path": "..."}},
     "stitchedPanorama": "/path/pano.jpg",
     "capturedAt": 1730000000
   }
   ```
3. **Back-compat:** readers treat missing `version` as v1 → single equator row. The viewer
   clamps vertical panning to the captured band (±~30°) for old scans instead of showing
   empty poles. No Room migration; `panoramaData` is a JSON string.
4. Extract all lon/lat ↔ ray math into pure Kotlin functions in `ScanGeometry.kt`
   (unit-testable, no Android deps): `rayToLonLat()`, `lonLatToRay()`, `projectRayIntoFrame()`.

**Acceptance:** old v1 scans still open in the viewer; new JSON round-trips.

---

## 3. Phase 2 — Stitcher: fill the sphere (large, the core)

**Files:** `stitchFramesInternal()` (+ helpers), `ScanGeometry.kt`.

Replace the heading-only paste with a **direction-based inverse mapper**:

1. For each output pixel of the canvas — **8192×4096** for Full mode (4 px/°, detail
   survives zoom), 4096×2048 for Quick, 2048×1024 low-RAM:
   `(lon, lat) = (x/W·360−180, 90−y/H·180)`.
2. Build the 3D ray for `(lon, lat)`; for every candidate frame (center direction
   `(heading, pitch)` within hFOV/2 + margin horizontally and vFOV/2 + margin vertically),
   project the ray into the frame's pixel via pinhole math and sample.
3. **Blend** overlapping frames with an angular-distance weight (feather ~10° around each
   frame edge). Replace any linear paste with this weight map to kill hard seams.
4. **Auto-Light pipeline (second half of the story — this is what makes seams invisible).**
   Even with capture-time AE locks, rings differ (ceiling lamps vs floor shadows vs
   windows). Before blending, run this normalization chain:
   a. **Gain compensation** — solve a per-frame gain (classic multi-band panorama trick):
      for every pair of overlapping frames, measure the mean-intensity difference in the
      overlap region; solve the least-squares system so all overlaps agree. This is
      global and cheap (matrix over ≤62 frames).
   b. **Luminance histogram matching** — remap each frame's histogram onto the horizon
      ring's reference histogram (per ring, so ceiling stays bright-ish and floor
      stays dark-ish, but transitions between them are smooth).
   c. **White-balance alignment** — match per-channel (R,G,B) means in overlap zones to
      kill the yellow-ceiling / blue-window tint mismatch.
   d. **Vignette & gamma normalization** — remove lens corner darkening; uniform gamma
      across frames so blended edges don't shift tone.
   e. **Multi-band blending** — replace the simple feather weight map with a 3–4 level
      Laplacian pyramid blend: low frequencies blend over wide distances (no brightness
      step across a seam), high frequencies blend narrowly (keeps texture sharp). This is
      the single biggest win for "the stitches look bad" complaints.
   f. **Pole caps** — same pipeline applied radially; the cap shot is gain-compensated
      against the innermost ring before blending.
5. **Pole handling:**
   - Cap shot exists → project the zenith/nadir photo radially (it's an isothermal patch;
     blend with the top/bottom ring).
   - No cap (Quick mode) → synthesize: fill the pole cap by radially stretching + blurring
     the innermost captured ring. Cosmetically fine for ceilings/floors.
6. **Memory & speed:** process canvas row-by-row into an `IntArray`, `setPixels()` once per
   tile, decode frames with `inSampleSize` (target ~800 px height, as today). The 8192
   canvas is processed in horizontal strips and encoded incrementally so the full bitmap
   never needs to exist twice. Run in `Dispatchers.Default`, emit progress for a UI bar
   ("Stitching ring 3/5…").
7. Keep `stitch_debug` frame dumps behind `BuildConfig.DEBUG`.

**Acceptance:** a synthetic test — feed gradient frames at known (heading, pitch) → output
canvas corners contain the zenith/nadir colors; seam weight math unit-tested on
`ScanGeometry` functions.

---

## 4. Phase 3 — Capture UX: rings, not one circle (medium)

**Files:** `RoomScannerScreen.kt` (`CapturingPhase`, overlays), `ScanGeometry.kt` (grid).

1. **Grid state machine:** `currentRow`, `currentCol` replace flat `currentTarget`.
   Finish a ring → haptic + "TILT UP TO NEXT RING" transition → next ring starts at the
   same heading you're facing (no forced rewind).
2. **Vertical progress rail** (right edge): 7 dots (5 rings + 2 caps) mirroring the
   horizontal 12-stop rail; completed rings turn green, current one pulses yellow —
   same visual language as the existing compass.
3. **Lock logic per frame:** heading within ±10° of column target **and** pitch within ±10°
   of row target (reuse existing thresholds + haptic-on-lock).
4. Ring 0/6 (caps): guidance flips to "POINT STRAIGHT UP/DOWN" with a level indicator
   (pitch only, no heading constraint).
5. **Retake:** tapping a captured thumbnail re-arms that specific (row, col) slot —
   critical for 62 shots, one bad frame shouldn't scrap a scan.
6. Intro screen copy: "Capture 5 rings × 12 photos + ceiling & floor. Turn → tap → tilt."
   Mode toggle (Full / Quick) with shot-count labels.
7. Estimated capture time hint (~90–120 s full sphere) to set expectations.

**Acceptance:** user can complete a full-sphere scan without reading any documentation;
pause/resume mid-scan keeps all captured frames.

---

## 5. Phase 4 — Viewer: look up and down (medium)

**Files:** `ui/tour/PanoramaViewerScreen.kt`.

1. **Full spherical mapping.** Replace cylindrical per-column sampling with per-pixel
   ray mapping: screen (x,y) → ray via yaw/pitch/FOV → `(lon, lat)` → sample.
   Implementation note: a per-pixel Kotlin loop per frame is too slow in Compose — use the
   **column-slice approximation** (per-column vertical offset + scale derived from the lat
   mapping; ~5% curvature error at screen edges, 60 fps on mid-range phones). Keep the
   exact ray math in `ScanGeometry.kt` for correctness reference and future GPU path.
2. **Vertical drag** with clamp `±(90 − vFOV/2)`; for **v1 scans** clamp to ±28° (their
   captured band). Horizontal wrap-around stays as-is.
3. **Pinch to zoom** = FOV 100°…25°; double-tap resets / toggles zoom levels.
4. **"See every inch" — hi-res tiled zoom.** A sphere is only useful if details survive
   magnification, so:
   - Full-mode stitcher outputs **8192×4096** (2048×1024 for Quick/low-RAM) — 4× the
     current 4096×2048, ~4 px/°, enough to read a light switch at normal viewing FOV.
   - Viewer loads a **base mip** (4096 or 2048) for panning, then samples **tiles via
     `BitmapRegionDecoder`** when the user zooms past ~60° FOV, decoding only the visible
     lon/lat window at full res. Memory stays flat no matter the zoom level.
   - Zoom-eligible areas: everywhere — the coverage guarantee (§1) means any direction
     the user zooms into has real captured pixels, not synthesized fill.
5. **Gyro:** feed both yaw (existing) and pitch from the rotation vector so tilting the
   phone looks up/down — toggleable like today.
6. **Compass hint** at the poles (small "up/down" chevron vignette) so users understand
   they reached zenith/nadir.
7. Per-room view state remembered while switching room tabs (yaw, pitch, zoom).

**Acceptance:** on a Full Sphere scan the user can see the ceiling lamp and the floor rug,
then zoom to inspect a wall socket and read a window view; drag/gyro/pinch all behave;
v1 scans look exactly as today; memory stays flat while zoomed (tile cache evicts).

---

## 6. Phase 5 — Persistence, polish, perf (small)

1. Store `mode` + `rows` metadata (already in JSON) and show scan coverage in the listing's
   tour card ("Full sphere 360°×180°" badge vs "360°").
2. Stitching progress dialog with cancel; stitched output cached to
   `files/scans/<scanId>/pano.jpg` (same as today).
3. Low-RAM fallback (2048×1024) + instrumentation: log stitch duration/memory per mode.
4. Optional (post-competition): real GPU mesh viewer (OpenGL/`drawMesh`) — noted, not scoped.

---

## 7. Test plan

| Layer | Test |
|---|---|
| `ScanGeometry` math | Pure JVM unit tests: ray↔lon/lat round-trip, frame projection edge cases |
| Stitcher | Synthetic frames (gradients at known angles) → assert pole pixels + seam weights; OOM path |
| Auto-light | Frames with synthetic ±2-stop exposure + color-cast differences → assert post-pipeline overlap luminance delta below threshold; gain-comp solver convergence on synthetic overlap graph |
| Coverage map | Frames with a deliberate hole → coverage grid reports the missing lon/lat cells and blocks Done |
| JSON | v1 read → v2 write round-trip; corrupt JSON → safe fallback |
| Device QA | Full-sphere scan in a real room: ceiling/floor visible, no visible bands; Quick mode; gyro off; low-RAM device; interrupt mid-stitch |
| CI | Every phase pushed → GitHub Actions green (**never local Gradle**); compiler errors via the `ci-logs` branch |

---

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Yaw drift across 62 shots | Re-read rotation vector at every shutter (already done); per-slot retake (§4.5) |
| Exposure/color shift between rings | Per-row luminance equalization (§3.4) |
| OOM during stitch | Row-tile processing, `inSampleSize`, low-RAM downscale, OOM→graceful null (already exists) |
| User fatigue (62 shots) | Quick mode (36), ring transition haptics, time hint, retake instead of restart |
| Mixed lighting (window vs lamp vs shadow corner) | WB alignment + multi-band blending (§3.4); AE lock per ring at capture |
| Over-sharpened zoomed-in view ("every inch" needs detail) | High-res stitch output + tiled zoom (§5); coverage badge proves no blind spots |
| Stitch latency (~62 frames) | Background dispatcher + progress UI; measure, optimize only if >20 s |
| Old scans breaking | v1 JSON read path + viewer clamp (§2.3, §5.2) — verified by test |

---

## 9. Milestones (in order, each ends with CI green)

| # | Phase | Size | Depends on |
|---|---|---|---|
| 1 | ScanGeometry + FrameData + JSON v2 (+ v1 back-compat) | S | — |
| 2 | Ring-based capture UX (Full/Quick modes, retake) | M | 1 |
| 3 | Spherical stitcher + **Auto-Light pipeline** (gain/histogram/WB/multi-band) + coverage validation + hi-res output | L | 1 |
| 4 | Sphere viewer (vertical drag, gyro pitch, pole vignette, hi-res tiled zoom) | M | 1, 3 |
| 5 | Polish: badges, perf instrumentation, low-RAM path | S | 3, 4 |

Phases 2 and 3 can run in parallel after 1; **4 requires 3** (nothing to look at
vertically until the stitcher fills the sphere).
