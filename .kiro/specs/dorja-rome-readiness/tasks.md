# Implementation Plan: Dorja Rome Readiness (FIRSO 2026)

## Overview

This document outlines the complete implementation roadmap for Dorja Homestation's FIRSO 2026 competition submission. The plan spans 6 weeks across 4 phases: Scanner UX/UI redesign, feature completion, testing & documentation, and final QA & submission.

**Target Team:** 1–2 Kotlin/Compose developers  
**Timeline:** 6 weeks to Rome submission  
**Total Estimated Effort:** ~240 hours (~30 hours per week)  
**Constraint:** No local Gradle builds; all CI through GitHub Actions  
**Success Criteria:**
- Scanner functional on Android 11+ devices with proper preview scaling and AR guidance
- All features (AI assistant, calculator, passport, chat, relocation) complete and polished
- Comprehensive device compatibility testing (5+ manufacturers)
- Zero critical crashes on reference device matrix
- WCAG 2.1 AA accessibility compliance verified

---

## Phase 1: Scanner UX/UI Redesign (Weeks 1–2) — ~80 hours

### Objective
Overhaul scanner preview rendering, add AR guidance ring with haptic feedback, implement frame rail carousel, and build zoom management system to meet competition UX standards.

---

### Core Scanner Architecture & Setup

- [x] **T1.1 Set up scanner module structure and core data models**
  - Create `/app/src/main/java/com/example/scanner/` package structure
  - Define Kotlin data classes: `FrameData`, `ScanGeometry.ScanTarget`, `Orientation`, `CaptureState`, `AlignLevel`, `SensorMode`, `ZoomCapabilities`
  - Define enums: `CaptureState` (WAITING_FOR_INITIAL_ANCHOR, CAPTURING_INITIAL_FRAME, MOVING_TO_TARGET, STABILIZING, COMPLETED, ERROR), `AlignLevel` (RED, YELLOW, GREEN), `SensorMode` (GYRO_PRIMARY, ACCEL_MAG_FALLBACK, UNAVAILABLE)
  - Add Room entities: `FrameCacheEntity`, `ScanSessionEntity` for persistence
  - Write basic unit tests for data class serialization
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** S (4 hours)
  - **Testing:** Unit tests for enum behavior, data class equality
  - **Notes:** These form the foundation for all scanner components; get them right early

- [x] **T1.2 Implement CameraManager with zoom range detection and adaptation**
  - Create `CameraManager.kt` class to handle CameraX initialization, binding, unbinding
  - Implement `detectZoomCapabilities()`: Query Camera2 metadata for `minZoomRatio`, `maxZoomRatio`, lens types (ultra-wide, standard, telephoto)
  - Implement zoom constraint logic: Default to 0.8x if ultra-wide available, 1.0x if standard, fall back to minimum
  - Handle zoom failures gracefully (metadata unavailable → use safe defaults 1.0x–2.0x)
  - Implement lens type detection (focal length mapping to ultra-wide <24mm, standard 24–35mm, telephoto >50mm)
  - Add error recovery: Log failures, continue with defaults
  - Ensure thread-safe camera binding/unbinding (wrap in Dispatchers.Main context)
  - **Priority:** Required
  - **Dependencies:** T1.1
  - **Effort:** M (8 hours)
  - **Testing:** Unit tests for zoom constraint logic, mocked Camera2 metadata queries
  - **Requirements:** 1.3, 4.1, 4.2, 4.3, 4.4
  - **Notes:** Camera binding is async; handle cancellation properly. Test on multiple camera stacks (Pixel has Tensor, Samsung has Exynos, etc.)

- [x] **T1.3 Build CameraPreview Composable with native aspect ratio and unzoomed display**
  - Create `CameraPreview.kt` Composable that wraps CameraX PreviewView in an AndroidView
  - Implement explicit aspect ratio matching: Extract camera sensor aspect ratio from CameraX session and apply to PreviewView via `setScaleType(FILL_CENTER)` or `FIT_START`
  - Ensure preview displays at 1:1 device pixels to camera pixels (no artificial zoom)
  - Handle foldable/tablet screen geometries: Detect aspect ratio, apply proportional scaling
  - Handle notch/punch-hole content insets via `WindowInsets` API
  - Implement smooth zoom transitions (<100ms latency between zoom slider change and preview update)
  - Add contentDescription for accessibility ("Camera preview, unzoomed")
  - **Priority:** Required
  - **Dependencies:** T1.2
  - **Effort:** M (10 hours)
  - **Testing:** Manual on Pixel 5, Samsung S21, OnePlus 9; automated Android instrumentation tests (Firebase Test Lab)
  - **Requirements:** 1.1, 1.2, 1.5, 6.1–6.5
  - **Correctness Properties:** Property 1 (Preview Aspect Ratio Preservation), Property 13 (Responsive Layout Stability)
  - **Notes:** AndroidView recomposition can be expensive; use `remember` to cache the PreviewView instance. CameraX aspect ratio API changed in newer versions; document fallback.

- [x] **T1.4 Implement SensorFusionManager for gyroscope and accelerometer+magnetometer fallback**
  - Create `SensorFusionManager.kt` to abstract sensor reading (gyro preferred, accel+mag fallback)
  - Implement `detectAvailableSensors()`: Check for gyroscope, accelerometer, magnetometer availability via SensorManager
  - Implement primary mode (gyro + accel fusion) when gyro available: Complementary filter alpha=0.25 for stability
  - Implement fallback mode (accel + mag fusion): Quaternion from accelerometer and magnetometer (using compass heading, pitch from accel)
  - Implement Euler angle validation: Ensure heading ∈ [0, 360), pitch ∈ [-90, 90], roll ∈ [-180, 180] (clamp if invalid)
  - Handle sensor failures: Log, continue in fallback mode, set `SensorMode.UNAVAILABLE` if both fail
  - Implement low-pass smoothing to reduce jitter (exponential moving average, alpha=0.25)
  - Add magnetometer interference detection: Variance > threshold → display warning UI
  - **Priority:** Required
  - **Dependencies:** T1.1
  - **Effort:** L (14 hours)
  - **Testing:** Unit tests with property-based testing (fast-check) on 1000+ random sensor vectors; instrumentation tests on reference devices (Pixel 5, OnePlus 9 with gyro; Xiaomi Mi 11, Motorola G20 without)
  - **Requirements:** 2.6, 5.1–5.7
  - **Correctness Properties:** Property 5 (Sensor Fusion Consistency), Property 6 (Sensor Fallback Correctness)
  - **Notes:** Sensor fusion is complex; use published algorithms (complementary filter, quaternion SLERP). Test on actual devices with varying sensor quality. Magnetometer needs calibration figure-8 gesture from user.

- [x] **T1.5 Implement ZoomSlider Composable with range adaptation**
  - Create `ZoomSlider.kt` Composable with `Slider` composable from Material 3
  - Bind slider to `CameraManager.getZoomCapabilities()` to set min/max values
  - Implement real-time zoom updates: On slider change, call `CameraX.setCameraControl().setZoomRatio(selectedZoom)`
  - Add visual feedback: Display current zoom ratio (e.g., "1.0x") above or below slider
  - Implement snap-to-nearest-supported-value if user tries value outside range (display toast notification)
  - Handle edge cases: Devices with fixed zoom (min = max) → disable slider, show label "Fixed zoom"
  - Add accessibility: contentDescription with current zoom value, step size, min/max range (e.g., "Zoom slider, current value 1.0x, range 0.5x to 4.0x")
  - **Priority:** Required
  - **Dependencies:** T1.2, T1.3
  - **Effort:** S (4 hours)
  - **Testing:** Unit tests for zoom constraint logic; manual testing on 3+ devices with different zoom ranges
  - **Requirements:** 1.2, 1.3, 4.2, 4.5
  - **Correctness Properties:** Property 2 (Zoom Slider Constraints)
  - **Notes:** Zoom slider should be visually prominent but not distract from preview. Use Material 3 slider styling for consistency.

---

### AR Guidance Ring & Capture Flow

- [ ] **T1.6 Implement ScanGeometry module for rotation matrix and frame overlap calculations**
  - Create `ScanGeometry.kt` with static/companion object functions
  - Implement `quaternionToRotationMatrix(q: Quaternion): FloatArray` — Convert quaternion to 3×3 rotation matrix (9 elements)
  - Implement `calculateFrameOverlap(h1: Float, h2: Float, heading_delta: Float): Float` — Symmetric overlap calculation (0–1 range), verify symmetry property
  - Implement `generateScanTargets(anchorHeading: Float, shotCount: Int): List<ScanTarget>` — Evenly-spaced targets around 360°; immutable once generated
  - Implement `calculatePanoramaDimensions(frameCount: Int, frameAspectRatio: Float): Pair<Int, Int>` — Ensure 2:1 output aspect (e.g., 4096×2048), power-of-2 sizes
  - Implement `validatePanoramaOutput(pixels: ByteArray, width: Int, height: Int): Boolean` — Check for NaN pixels, 2:1 aspect (±0.01), file size <50MB
  - Implement quaternion SLERP (spherical linear interpolation) for smooth heading interpolation between frames
  - Add Euler angle validation helper: `isValidEulerAngles(heading: Float, pitch: Float, roll: Float): Boolean`
  - **Priority:** Required
  - **Dependencies:** T1.1
  - **Effort:** M (10 hours)
  - **Testing:** Property-based tests with fast-check: 1000+ random quaternions, 1000+ random heading pairs; verify symmetry and range properties
  - **Requirements:** 2.1, 3.2, 3.5
  - **Correctness Properties:** Property 3 (Frame Overlap Calculation), Property 4 (Stitching Output Validation), Property 7 (Target Generation Invariant)
  - **Notes:** Rotation math is error-prone; use quaternion library if available (e.g., Quaternion from Apache Commons). Add inline documentation for matrix operations.

- [ ] **T1.7 Build ARGuidanceRing Composable with color states and animations**
  - Create `ARGuidanceRing.kt` Composable rendering a filled circle with stroke on Canvas
  - Implement state machine: RED (poor overlap) → YELLOW (align needed) → GREEN (ready to capture)
  - Define visual specs: Diameter 120–180dp proportional to screen size, 3px stroke, 0.3 alpha fill
  - Implement color transitions: RED (#E53935), YELLOW (#FFC107), GREEN (#4CAF50)
  - Animate to new position on screen (slide to edge) when 30-degree rotation detected (spring animation, 400ms)
  - Pulse animation: On YELLOW state, linear pulse 800ms (scale 1.0 → 1.1 → 1.0)
  - Glow animation: On GREEN state, fade glow 1s (opacity 0 → 1 → 0 repeat)
  - Add text label below ring when in YELLOW state: "Rotate 30° for next shot" or "Hold steady"
  - Add fallback indicator: When SensorMode.ACCEL_MAG_FALLBACK, display "Accel mode — hold steady for 2 sec"
  - Add contentDescription: "Guidance ring, state [RED/YELLOW/GREEN], current position [heading degrees]"
  - **Priority:** Required
  - **Dependencies:** T1.1, T1.4
  - **Effort:** M (8 hours)
  - **Testing:** Manual animation review on multiple devices; accessibility testing with TalkBack; screenshot regression tests
  - **Requirements:** 2.1–2.6
  - **Notes:** Animations should feel responsive, not sluggish. Use Compose's `animateValueAsState()` for smooth transitions. Test on 90fps devices (high-refresh).

- [ ] **T1.8 Implement haptic feedback system with pattern utilities**
  - Create `HapticFeedback.kt` utility object with pattern functions
  - Implement `hapticSuccess()`: Dual pulse 50ms + 100ms pause + 50ms (SUCCESS pattern)
  - Implement `hapticCapture()`: Single pulse 100ms, medium strength (HEAVY_CLICK on Android 12+, vibrate(100) on earlier)
  - Implement `hapticTap()`: Light pulse 30ms (LIGHT_CLICK on Android 12+)
  - Implement `hapticWarning()`: Triple pulse 50ms each, 50ms gaps (WARNING pattern)
  - Wrap all calls in try-catch: If Vibrator unavailable or permission denied, fail silently (no crash)
  - Handle low-battery/vibration-disabled states: Check device settings before vibrating
  - Abstract platform differences: Use `VibrationEffect.createPredefined()` on Android 12+, fallback to `Vibrator.vibrate()` on earlier
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** S (3 hours)
  - **Testing:** Manual testing on Pixel 5, Samsung S21; verify pattern timing matches spec
  - **Requirements:** 2.2, 2.3, 19.1–19.6
  - **Notes:** Haptic library `androidx.compose.foundation.hapticFeedback` can simplify this; use if available.

- [ ] **T1.9 Build FramePreviewRail Composable with thumbnail carousel**
  - Create `FramePreviewRail.kt` Composable using LazyRow for horizontal scrolling
  - Display captured frame thumbnails: 80px minimum height, 2px border, rounded 4dp corners
  - Implement auto-scroll: Keep current/latest frame visible in viewport
  - Implement tap-to-highlight: Tap thumbnail to select, highlight with border
  - Implement long-press menu: Delete frame, retake frame, duplicate frame (if supported)
  - Add "Add Frame" button (+ icon) at end of rail for manual triggering
  - Implement frame count indicator: "Frame 3 of 12" displayed above rail
  - Add contentDescription per thumbnail: "Frame 3, captured at 14:22:05, coverage east wall"
  - Handle edge cases: Empty rail, single frame, all 12 frames captured
  - **Priority:** Required
  - **Dependencies:** T1.1
  - **Effort:** M (6 hours)
  - **Testing:** Manual UI review; accessibility testing with TalkBack; screenshot regression tests
  - **Requirements:** 1.6, 2.1
  - **Notes:** Thumbnail generation from JPEG cache is I/O intensive; use coroutines to load off main thread. Consider caching BitmapFactory-decoded thumbnails in memory.

- [ ] **T1.10 Implement CaptureButton with shutter feedback and state management**
  - Create `CaptureButton.kt` Composable with Material 3 Button or FAB styling
  - Implement capture trigger logic: On click, call `ScannerViewModel.captureFrame()`
  - Add visual feedback: Button scale animation on press (0.95 scale, 100ms)
  - Add ripple effect (Material 3 default)
  - Emit haptic feedback on tap: Call `HapticFeedback.hapticTap()`
  - Implement button state: Enabled/disabled based on `CaptureState` (disabled during stitching, initial anchor)
  - Add tooltip or label: "Capture frame" with accessibility description
  - Implement long-hold behavior: Long-press to manual trigger next scan target (for advanced users)
  - **Priority:** Required
  - **Dependencies:** T1.8, T1.1
  - **Effort:** S (3 hours)
  - **Testing:** Manual testing; accessibility testing
  - **Requirements:** 3.1
  - **Notes:** Button should feel responsive. Use Material 3's `FilledTonalButton` or `ExtendedFloatingActionButton` for visibility.

---

### Stitching Pipeline & Error Recovery

- [ ] **T1.11 Implement StitchingEngine with timeout handling and memory management**
  - Create `StitchingEngine.kt` class handling 6–12 frame stitching on background thread
  - Implement frame loading pipeline: Load JPEG frames from cache, decode to Bitmap (background thread)
  - Implement coarse alignment: Use ScanGeometry overlap calculations to determine frame placement
  - Implement frame blending: Feather overlapping edges to hide seams (box filter or Gaussian blur on seams)
  - Implement Bitmap pooling: Reuse Bitmap objects via `Bitmap.createBitmap()` pooling to minimize GC pauses
  - Implement phase progression: PREPARING → PROCESSING → VALIDATING → CACHING, emit progress updates every 500ms
  - Implement timeout logic:
    - < 8s: Silent progress (show %, no warning)
    - 8s–30s: Display warning "Large image; this may take a minute…"
    - > 30s: Show cancel CTA "Try recapturing with 8 frames instead of 12"
    - > 60s: Still allow user to cancel, but continue in background
  - Implement error handling: Catch OOM, corrupted frame data, math errors; log and return `Result.Error(message)`
  - Implement output validation: Call `ScanGeometry.validatePanoramaOutput()` before caching
  - Implement caching: Save panorama to app cache directory with metadata file (frame count, timestamp, device info)
  - **Priority:** Required
  - **Dependencies:** T1.1, T1.6
  - **Effort:** L (14 hours)
  - **Testing:** Integration tests with mocked frames; stress test with OOM simulation; instrumentation tests on Firebase Test Lab
  - **Requirements:** 3.2–3.8, 13.1–13.8
  - **Correctness Properties:** Property 4 (Stitching Output Validation)
  - **Notes:** Stitching is computationally expensive; profile on budget devices (Motorola G20) to establish realistic timeouts. Use Canvas or native graphics library if available.

- [ ] **T1.12 Build FrameCaptureManager handling camera binding and frame I/O**
  - Create `FrameCaptureManager.kt` for CameraX image capture pipeline
  - Implement image capture: `captureImage()` → JPEG format, quality ≥ 95%, save to app cache
  - Implement frame naming: `/cache/scan_<sessionId>_frame_<index>_<timestamp>.jpg`
  - Implement frame metadata storage: Timestamp, heading, pitch, roll from sensor fusion
  - Implement progress reporting: Update UI every 500ms with frame count, estimated stitching time
  - Implement cancellation: Clear frame buffers on user cancel, reset frame counter to 0 within 1 second
  - Implement error recovery: Camera binding failure → display recovery UI (see T1.14)
  - Implement frame buffer management: Keep frames in memory until stitching complete, then release
  - **Priority:** Required
  - **Dependencies:** T1.2, T1.3, T1.4
  - **Effort:** M (8 hours)
  - **Testing:** Integration tests on real devices; mock camera failures to test recovery
  - **Requirements:** 3.1, 3.3, 3.8, 14.1–14.4
  - **Notes:** CameraX ImageCapture use case is async; handle callbacks properly. Test on devices with file I/O constraints (USB 2.0 write speed).

- [ ] **T1.13 Implement ScannerViewModel state management with Flow/StateFlow**
  - Create `ScannerViewModel.kt` extending ViewModel, integrating all scanner components
  - Define state flows:
    - `_captureState: MutableStateFlow<CaptureState>`
    - `_scanTargets: MutableStateFlow<List<ScanGeometry.ScanTarget>>`
    - `_capturedFrames: MutableStateFlow<List<FrameData>>`
    - `_stitchingProgress: MutableStateFlow<StitchingProgress?>`
    - `_orientation: MutableStateFlow<Orientation>`
    - `_cameraCapabilities: MutableStateFlow<CameraCapabilities?>`
    - `_alignLevel: MutableStateFlow<AlignLevel>`
    - `_sensorMode: MutableStateFlow<SensorMode>`
    - `_errorMessage: MutableStateFlow<String?>`
  - Implement state machine transitions: WAITING_FOR_ANCHOR → CAPTURING_INITIAL_FRAME → MOVING_TO_TARGET → STABILIZING → COMPLETED
  - Implement `startScan()`: Confirm anchor orientation, generate targets, start capture flow
  - Implement `captureFrame()`: Call FrameCaptureManager, update capturedFrames, check if stitching should begin
  - Implement `stitch()`: Call StitchingEngine, emit progress updates, handle errors
  - Implement `cancelScan()`: Clear state, release resources, reset to initial state
  - Implement session persistence: Save/restore scan session from Room DB on app background/foreground
  - **Priority:** Required
  - **Dependencies:** T1.1, T1.2, T1.4, T1.6, T1.11, T1.12
  - **Effort:** M (9 hours)
  - **Testing:** Unit tests for state transitions; integration tests with all scanner components
  - **Requirements:** All scanner requirements (1–6, 13)
  - **Notes:** Use `viewModelScope.launch()` for all async operations. Test state recovery on app restart.

- [ ] **T1.14 Build error recovery UI components (CameraBindingError, StitchingError, etc.)**
  - Create `ErrorRecoveryUI.kt` with Composables for each error type:
    - `CameraBindingErrorScreen()`: "Camera unavailable" with retry/settings/back buttons
    - `StitchingErrorScreen()`: "Stitching failed" with retry/recapture buttons, estimated time
    - `PermissionDeniedScreen()`: "Camera permission required" with grant/settings buttons
    - `CameraHardwareUnavailableScreen()`: "Device has no camera" with back button
    - `LowMemoryWarningDialog()`: "Device low on memory; try 8 frames instead of 12"
    - `SensorUnavailableScreen()`: "This device cannot capture scans; gyroscope/accelerometer required"
  - Implement retry logic: On "Retry", dismiss error and attempt corrective action
  - Implement timeout recovery: After 8s, show warning; after 30s, show cancel CTA
  - Add consistent styling: Material 3 error colors (#D32F2F), clear messaging, visible action buttons
  - Add accessibility: All error messages readable by TalkBack with clear next steps
  - **Priority:** Required
  - **Dependencies:** T1.1, T1.8
  - **Effort:** M (7 hours)
  - **Testing:** Manual UI review; simulate error conditions (disable camera, fill storage, etc.); accessibility testing
  - **Requirements:** 14.1–14.4, 20.1–20.5
  - **Notes:** Error messages should be user-friendly, not technical jargon. Test on actual failure scenarios.

- [ ] **T1.15 Integrate all scanner components into RoomScannerScreen Composable**
  - Create `RoomScannerScreen.kt` main screen Composable combining all components:
    - CameraPreview (T1.3) as background/fullscreen
    - ARGuidanceOverlay (T1.7) centered on preview
    - ZoomSlider (T1.5) on left/bottom side
    - FramePreviewRail (T1.9) at bottom
    - CaptureButton (T1.10) at bottom-center
    - ProgressIndicator (from T1.11) during stitching
    - ErrorRecoveryUI (T1.14) when errors occur
  - Implement layout responsiveness: Adjust component positions for foldable/tablet/notch
  - Implement lifecycle management: Call SensorFusionManager.start() on screen entry, stop on exit
  - Implement state binding: Collect from ScannerViewModel StateFlows, recompose on updates
  - Implement navigation: On successful stitching, navigate to PanoramaViewerScreen (stub for now)
  - **Priority:** Required
  - **Dependencies:** T1.3, T1.5, T1.7, T1.9, T1.10, T1.13, T1.14
  - **Effort:** M (8 hours)
  - **Testing:** Manual UI integration testing on 3+ devices; screenshot regression tests
  - **Requirements:** 1.1–1.6, 2.1–2.6, 3.1–3.8, 6.1–6.6
  - **Notes:** Use `remember { viewModel() }` to get ScannerViewModel. Test orientation changes (portrait ↔ landscape) without data loss.

---

## Phase 2: Feature Completion (Week 3) — ~60 hours

### Objective
Complete AI assistant graceful disable, financial calculator, handover passport, chat system, and relocation mode to ensure all features are polished and competition-ready.

---

### AI Assistant & Settings

- [ ] **T2.1 Implement graceful AI feature flag with Cactus SDK try-catch loading**
  - Create `AIAssistant.kt` wrapper class with safe class loading
  - Wrap Cactus SDK class load in try-catch: If available, load and enable; if unavailable (commented-out or removed), set `isAvailable = false`
  - Implement feature flag logic: Check `isAvailable` before calling AI model
  - Add Settings toggle: "Enable AI Assistance" (only show if available, default ON)
  - Implement graceful fallback: If model load fails at runtime, log error and continue with AI disabled
  - Update Listing Creation flow: Show AI suggestions if `isAvailable && userToggle`, otherwise skip AI section
  - Test both scenarios: With Cactus SDK commented-out, with SDK available
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** S (4 hours)
  - **Testing:** Unit tests with mock SDK present/absent; manual testing with commented SDK
  - **Requirements:** 7.1–7.6
  - **Correctness Properties:** Property 9 (AI Feature Graceful Disable)
  - **Notes:** Don't crash if SDK is missing; this is a critical requirement for Rome.

- [ ] **T2.2 Update build.gradle.kts and remove Android 7–10 support**
  - Update `minSdk = 30` (Android 11) in app/build.gradle.kts
  - Verify `targetSdk = 36` (Android 14)
  - Remove any Android 7–10 compatibility shims/fallbacks from codebase
  - Update AndroidManifest.xml to reflect minSdk
  - Update README.md: "Dorja requires Android 11 or later (API 30+)"
  - Add CI/CD check to enforce minSdk in build validation
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** S (2 hours)
  - **Testing:** Build APK via CI, verify target devices are Android 11+
  - **Requirements:** 12.1–12.7
  - **Notes:** This is a one-way change; no rollback needed.

---

### Financial Calculator

- [ ] **T2.3 Implement FinancialCalculator business logic with real-time amortization**
  - Create `FinancialCalculator.kt` class with computation functions
  - Implement `calculateMonthlyPayment(principal, monthlyRate, numPayments)`: Standard amortization formula P * [r(1+r)^n] / [(1+r)^n - 1]
  - Implement `calculateTotalInterest(monthlyPayment, numPayments, principal): Double`
  - Implement `calculateROI(rentIncome, propertyValue, years): Double`
  - Implement `calculateCashOnCashReturn(downPayment, annualCashFlow): Double`
  - Implement input validation: Check for negative values, interest rate > 100%, loan term <= 0
  - Ensure computation latency < 100ms on budget devices (use Double precision, not BigDecimal)
  - Add error handling: Catch division by zero, NaN results
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** S (3 hours)
  - **Testing:** Property-based tests: 1000+ random valid inputs verify formula correctness
  - **Requirements:** 8.2
  - **Correctness Properties:** Property 10 (Financial Calculator Correctness)
  - **Notes:** Use IEEE 754 Double; acceptable precision loss for financial app. Verify against online calculators.

- [ ] **T2.4 Build CalculatorScreen Composable with input fields and real-time results**
  - Create `CalculatorScreen.kt` Composable with Material 3 TextField components
  - Input fields: Property Price, Down Payment, Interest Rate, Loan Term (years), Monthly Rent
  - Implement real-time computation: On each text change, call `FinancialCalculator`, update results
  - Display results: Monthly Payment, Total Interest, ROI%, Cash-on-Cash Return in bold, clear formatting
  - Implement input validation UI: Inline error messages below each invalid field (red text)
  - Disable Calculate button when any input invalid
  - Implement "Save Calculation" button: Call CalculatorViewModel to persist to Room DB
  - Display calculation history: Sortable list of past calculations (date, property name, monthly payment)
  - Implement "Load" action: Restore calculation values to current form
  - Implement "Delete" action: Remove from history
  - Implement detailed breakdown: Tap a result to show amortization table, ROI chart, cash-flow timeline
  - Handle locale: Currency symbol (৳, €, $), number formatting per locale
  - **Priority:** Required
  - **Dependencies:** T2.3
  - **Effort:** L (10 hours)
  - **Testing:** Manual UI testing; input validation testing; history CRUD testing
  - **Requirements:** 8.1–8.8
  - **Notes:** Use Material 3 `TextField` for styling. Implement calculation history with Room DAO. Chart can be simple (text table or bar chart via third-party library).

- [ ] **T2.5 Add CalculatorViewModel and Room DAO for calculation history**
  - Create `CalculatorViewModel.kt` extending ViewModel
  - Define state flows: `_inputs`, `_results`, `_history`, `_selectedCalculation`
  - Implement `saveCalculation()`: Create `FinancialCalculation` entity, insert via DAO
  - Implement `loadCalculation(id)`: Restore values to inputs
  - Implement `deleteCalculation(id)`: Remove from DB
  - Create `CalculationDao.kt` in Room database with insert, query, delete operations
  - Create `FinancialCalculation` Room entity (Requirement 9.3 from design doc)
  - **Priority:** Required
  - **Dependencies:** T2.4
  - **Effort:** S (4 hours)
  - **Testing:** Unit tests for DAO operations; integration tests with Room DB
  - **Requirements:** 8.4, 8.5
  - **Notes:** Use `@Entity`, `@Dao`, `@Database` annotations. Implement proper migrations if schema changes.

---

### Handover Passport

- [ ] **T2.6 Design and implement HandoverPassport data models with encryption**
  - Create `HandoverPassport.kt` Room entity: id, propertyId, sellerId, buyerId, createdAt, status
  - Create `Promise.kt` Room entity: id, passportId, description, targetDate, status (PENDING/IN_PROGRESS/COMPLETED/OVERDUE/FAILED), createdAt
  - Create `VaultDocument.kt` Room entity: id, passportId, encryptedBytes, sha256Hash, uploadedBy, uploadedAt
  - Create `DigitalSignature.kt` Room entity: id, promiseId, signedBy, signatureData, timestamp
  - Implement encryption: Use Android KeyStore to generate/manage encryption keys
  - Implement SHA-256 hashing: On document upload, compute and store hash for tampering detection
  - Create Room DAOs: `PassportDao`, `PromiseDao`, `VaultDocumentDao`, `SignatureDao`
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** M (8 hours)
  - **Testing:** Unit tests for encryption/decryption round-trips; unit tests for hash consistency
  - **Requirements:** 9.1–9.9
  - **Correctness Properties:** Property 11 (Handover Passport Immutability)
  - **Notes:** Use `AndroidKeyStore` for secure key management. Test hash verification after document tampering simulation.

- [ ] **T2.7 Implement PassportManager business logic for promise tracking and document vault**
  - Create `PassportManager.kt` class
  - Implement `createPassport(propertyId, sellerId, buyerId)`: Generate UUID, create entity, insert via DAO
  - Implement `addPromise(passportId, description, targetDate)`: Create Promise entity, insert
  - Implement `updatePromiseStatus(promiseId, newStatus)`: Update status, log timestamp
  - Implement `markOverduePromises()`: Query promises with targetDate < now and status != COMPLETED, mark as OVERDUE
  - Implement `uploadDocument(passportId, file, uploadedBy)`: Encrypt file, compute hash, store in vault
  - Implement `addSignature(promiseId, signedBy)`: Record digital signature with timestamp
  - Implement `exportPassportPDF(passportId)`: Generate PDF with all promises, documents, signatures (use Android PDF library, e.g., iText)
  - Implement `verifyDocumentIntegrity(docId)`: Re-compute hash, compare with stored hash
  - **Priority:** Required
  - **Dependencies:** T2.6
  - **Effort:** L (12 hours)
  - **Testing:** Unit tests for all CRUD operations; integration tests with Room; mock PDF export
  - **Requirements:** 9.1–9.9
  - **Notes:** PDF export is complex; consider using a third-party library. Hash verification is critical for tampering detection.

- [ ] **T2.8 Build HandoverPassportScreen Composable with promises and document display**
  - Create `HandoverPassportScreen.kt` Composable
  - Display passport header: Property ID, buyer/seller info, created date
  - Display promises section: Scrollable list of promises with status badges (PENDING=yellow, COMPLETED=green, OVERDUE=red)
  - Implement "Add Promise" button: Show dialog for entering description, target date
  - Implement promise detail tap: Show promise status history, documents attached, signatures
  - Implement document vault section: List uploaded documents with upload date, uploader, file size, hash (first 16 chars)
  - Implement "Upload Document" button: File picker, encrypt and store, display confirmation
  - Implement "Sign Off" action: Record signature for completed milestones
  - Implement "Export PDF" button: Generate and share/save PDF
  - Implement immutability UI: Show "Immutable record" badge for all documents/promises
  - Add accessibility: All elements readable by TalkBack with clear descriptions
  - **Priority:** Required
  - **Dependencies:** T2.7
  - **Effort:** M (10 hours)
  - **Testing:** Manual UI testing; file upload simulation; PDF export verification
  - **Requirements:** 9.1–9.9
  - **Notes:** Use Material 3 components. Implement file picker via `androidx.activity.result.ActivityResultContracts.GetContent()`.

---

### Chat System

- [ ] **T2.9 Design and implement ChatMessage data models and rich text support**
  - Create `ChatMessage.kt` Room entity: id, threadId, senderId, content, richTextFormat (Markdown), mediaAttachments (list of file IDs), deliveryStatus, createdAt, seenAt
  - Create `ChatAttachment.kt` Room entity: id, messageId, filePath, mimeType, fileSize, uploadedAt
  - Create `ChatThread.kt` Room entity: id, property Id, sellerId, buyerId, createdAt, lastMessageAt
  - Implement rich text parsing: Support **bold**, *italic*, [link](url) in Markdown
  - Implement content rendering: Convert Markdown to Spanned or HTML for display
  - Implement delivery status state machine: SENDING → SENT → SEEN (or SENDING → ERROR)
  - Create Room DAOs: `ChatMessageDao`, `ChatThreadDao`, `ChatAttachmentDao`
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** M (7 hours)
  - **Testing:** Unit tests for rich text parsing; unit tests for delivery state transitions
  - **Requirements:** 10.2, 10.5, 10.8
  - **Notes:** Use Markwon or similar library for Markdown rendering. Implement proper entity relations with @Relation annotations.

- [ ] **T2.10 Implement ChatService with message persistence and delivery tracking**
  - Create `ChatService.kt` class
  - Implement `sendMessage(threadId, senderId, content, attachments)`: Create message with SENDING status, persist to DB, simulate send (mark as SENT after 1s, SEEN after 5s for demo)
  - Implement `updateDeliveryStatus(messageId, status)`: Update message status, emit notification
  - Implement `attachImage(file)`: Compress to max 5MB total per message, store in vault, add to attachments list
  - Implement message query methods: `getMessages(threadId)`, `getThreads(userId)`
  - Implement typing indicator: `setUserTyping(threadId, userId)`, broadcast to recipients (demo via local state)
  - Implement notification: `notifyNewMessage(message)` — show notification with sender name, message preview
  - **Priority:** Required
  - **Dependencies:** T2.9
  - **Effort:** M (8 hours)
  - **Testing:** Unit tests for message CRUD; integration tests for delivery status updates; manual notification testing
  - **Requirements:** 10.1–10.8
  - **Correctness Properties:** Property 12 (Chat Message Delivery Status)
  - **Notes:** For demo, implement local-only chat (no backend). Real implementation would sync with backend API.

- [ ] **T2.11 Build ChatScreen Composable with message list and input field**
  - Create `ChatScreen.kt` Composable
  - Display message history: Scrollable column of messages with sender, timestamp, content (rendered Markdown), status icon
  - Implement message bubbles: Sent messages (right-aligned, blue), received messages (left-aligned, gray)
  - Implement media preview: Tap image to enlarge, show full-size in modal
  - Implement rich text formatting: Bold, italic, links rendered and clickable
  - Implement input field: Material 3 TextField with RTL support (for Bangla)
  - Implement image picker: "+" button opens file picker, shows thumbnail before send
  - Implement send button: Only enabled when message non-empty, emit haptic on send
  - Implement typing indicator: Show "User is typing..." below message list when recipient typing (demo)
  - Implement delivery status indicators: Hourglass (SENDING), checkmark (SENT), double checkmark (SEEN)
  - Add accessibility: All messages readable by TalkBack with sender, time, status announced
  - **Priority:** Required
  - **Dependencies:** T2.10
  - **Effort:** M (9 hours)
  - **Testing:** Manual UI testing; rich text rendering verification; accessibility testing with TalkBack
  - **Requirements:** 10.1–10.8
  - **Notes:** Use `LazyColumn` for efficient message list scrolling. Implement auto-scroll to latest message.

- [ ] **T2.12 Add ChatViewModel and notification integration**
  - Create `ChatViewModel.kt` extending ViewModel
  - Define state flows: `_messages`, `_threads`, `_userIsTyping`, `_newMessageNotification`
  - Implement `sendMessage()`, `attachImage()`, `setUserTyping()`
  - Implement notification firing: On new message, fire system notification via `NotificationManager`
  - Create notification channel (Android 8+)
  - Implement sound and vibration: Play notification sound (unless muted), haptic pulse
  - **Priority:** Required
  - **Dependencies:** T2.11
  - **Effort:** S (4 hours)
  - **Testing:** Unit tests for notification firing; manual notification testing on device
  - **Requirements:** 10.6
  - **Notes:** Use `androidx.core.app.NotificationCompat` for compatibility. Test notification permissions on Android 13+.

---

### Relocation Mode

- [ ] **T2.13 Create relocation data models and country registry**
  - Create `RelocationCountry.kt` data class: name, code (ISO 3166), visaTypes, propertyRegulations, taxInfo, marketData, languageGuides
  - Create `VisaType.kt` data class: type, duration, requirements, restrictions, restrictions_for_foreigners
  - Create `MarketData.kt` data class: medianPrice, priceTrends (list), avgRentalYield, popularAreas (list)
  - Create `PropertyRegulation.kt` data class: foreignerBuyingAllowed, registrationProcess, requiredDocs, timeline
  - Create `TaxInfo.kt` data class: propertyTax, rentalIncomeTax, capitalGainsTax, rules
  - Create `LanguageGuide.kt` data class: language, commonPhrases (map), links to resources
  - Create `RelocationCountryDao.kt` in Room DB
  - Pre-populate countries: Bangladesh, India, Nepal, Bhutan, Germany, France, Poland, Turkey (mock data for demo)
  - **Priority:** Required
  - **Dependencies:** None
  - **Effort:** M (6 hours)
  - **Testing:** Unit tests for data model serialization; DAO insert/query tests
  - **Requirements:** 11.1–11.7
  - **Notes:** Start with mock data; real app would sync from backend API. Include links to official government resources.

- [ ] **T2.14 Implement RelocationMode toggle and country selector UI**
  - Create `RelocationModeScreen.kt` Composable
  - Add toggle in Settings: "Enable Relocation Mode" (default OFF)
  - Display country selector: Dropdown/list of supported countries
  - On country selection, fetch and display:
    - Market overview (median price, rental yield, price trends)
    - Visa info (types, duration, requirements)
    - Property regulations (buying restrictions, registration process)
    - Language guides (common phrases)
    - Tax information
  - Implement relocation-specific property filters: Visa category, distance from expat hubs, furnished/unfurnished, proximity to intl schools
  - Implement relocation guidance links: Show step-by-step guides with links to official portals
  - Display "Partial Data" badge if country data incomplete
  - **Priority:** Optional (nice-to-have for Rome)
  - **Dependencies:** T2.13
  - **Effort:** M (8 hours)
  - **Testing:** Manual UI testing; country selector functionality; data loading verification
  - **Requirements:** 11.1–11.7
  - **Notes:** Use Material 3 dropdown. For MVP, relocation mode can be simplified to just country selection + market info.

---

## Phase 3: Testing & Documentation (Weeks 4–5) — ~60 hours

### Objective
Implement comprehensive unit and integration tests, verify device compatibility, and document accessibility/localization.

---

### Unit Tests for Scanner & Geometry

- [ ] **T3.1 Write property-based tests for ScanGeometry using fast-check**
  - Create `app/src/test/java/com/example/scanner/ScanGeometryPropertyTest.kt`
  - Test `quaternionToRotationMatrix()`: Generate 1000+ random quaternions, verify output is valid 3×3 orthogonal matrix (det = 1, rows/cols orthogonal)
  - Test `calculateFrameOverlap()`: Generate 1000+ random heading pairs, verify symmetry property (overlap(h1, h2) == overlap(h2, h1)), range ∈ [0, 1]
  - Test `generateScanTargets()`: Generate targets for shot counts 6, 10, 12; verify target count matches, headings evenly spaced (360/count ±0.5°), all targets in valid range [0, 360)
  - Test `calculatePanoramaDimensions()`: Verify output aspect ratio 2:1 (±0.01), width/height are power-of-2
  - Test `validatePanoramaOutput()`: Generate valid panoramas, verify validation passes; generate invalid ones (wrong aspect, NaN, >50MB), verify rejection
  - Test edge cases: Zero rotation, 180° rotation, gimbal lock (pitch ±90°)
  - Implement custom Arbitraries for quaternions, Orientations, Angles
  - Run 1000+ iterations per property
  - **Priority:** Required
  - **Dependencies:** T1.6
  - **Effort:** M (9 hours)
  - **Testing:** Run tests locally via GitHub Actions CI (`./gradlew test`)
  - **Requirements:** 15.1–15.5
  - **Correctness Properties:** Property 1–7 (Preview, Zoom, Overlap, Output, Sensor fusion, Fallback, Target)
  - **Notes:** Install fast-check library in build.gradle.kts. Document Arbitrary implementations for future test additions.

- [ ] **T3.2 Write unit tests for FinancialCalculator and SensorFusion**
  - Create `app/src/test/java/com/example/scanner/FinancialCalculatorTest.kt`
  - Test amortization formula: Fixed scenarios (standard mortgage: $300k, 20% down, 6% rate, 30 years; verify monthly payment ≈ $1,079)
  - Test edge cases: Zero interest (payment = principal / months), large principal (>$1M), short term (5 years)
  - Test input validation: Negative price → error, interest > 100% → error, term <= 0 → error
  - Create `app/src/test/java/com/example/scanner/SensorFusionTest.kt`
  - Test gyro fusion: Generate rotation sequences, verify heading/pitch/roll smoothness (no sudden jumps)
  - Test accel+mag fallback: Verify Euler angles always valid (ranges correct)
  - Test sensor mode transitions: Gyro available → Gyro unavailable, verify fallback activation
  - Test magnetometer interference detection: Generate high-variance magnetic field samples, verify warning triggered
  - **Priority:** Required
  - **Dependencies:** T2.3, T1.4
  - **Effort:** M (7 hours)
  - **Testing:** Run via CI; verify coverage >= 80% for both classes
  - **Requirements:** 8.2, 5.2–5.7
  - **Correctness Properties:** Property 5, 6, 10
  - **Notes:** Use mock objects for SensorManager. Document test scenarios and expected outcomes.

- [ ] **T3.3 Write integration tests for Scanner flow on Firebase Test Lab**
  - Create `app/src/androidTest/java/com/example/scanner/ScannerFlowIntegrationTest.kt`
  - Test scenario 1: Camera binding → preview starts → frame captured → frame cached
  - Test scenario 2: 12 frames captured → stitching begins → progress updates → panorama generated
  - Test scenario 3: Camera binding failure → error UI displayed → retry succeeds
  - Test scenario 4: Stitching timeout (8s) → warning displayed, stitching continues
  - Test scenario 5: Permission denied → recovery UI with grant permission button
  - Test scenario 6: Orientation change (portrait → landscape) → no data loss, UI re-layouts
  - Use `ActivityScenario` to launch ScannerScreen, interact with UI, assert state changes
  - Implement test helpers: Mock frame capture, simulate orientation change
  - Target runtime: < 5 min per test
  - Run on Firebase Test Lab with Pixel 5, Samsung S21, OnePlus 9
  - **Priority:** Required
  - **Dependencies:** T1.15, T2.2
  - **Effort:** L (12 hours)
  - **Testing:** Run via CI on Firebase Test Lab; verify all scenarios pass on all devices
  - **Requirements:** 16.1–16.5
  - **Notes:** Firebase Test Lab requires GCP credentials; set up in GitHub Actions secrets. Document test matrix in CI config.

- [ ] **T3.4 Write accessibility tests (TalkBack, contrast, touch targets)**
  - Create accessibility test file: `app/src/androidTest/java/com/example/accessibility/AccessibilityTest.kt`
  - Test all touch targets: Measure all interactive elements (buttons, sliders, text fields), verify >= 48×48 dp
  - Test contrast ratios: Sample colors from UI, compute contrast ratios, verify >= 4.5:1 for normal text, >= 3:1 for large
  - Test text scaling: Set system text size to max, verify layout doesn't break, all text visible
  - Test TalkBack: Enable TalkBack, navigate through ScannerScreen via swipes, verify all elements announced with descriptive labels
  - Test high contrast mode: Enable high contrast system setting, verify UI colors adjust
  - Test reduced motion: Enable reduced motion, verify animations disabled/simplified
  - Document findings in `.kiro/accessibility-report.md`
  - **Priority:** Required
  - **Dependencies:** T1.15, T2.8, T2.11
  - **Effort:** L (10 hours)
  - **Testing:** Manual + automated; use `AccessibilityChecker` from AndroidX libraries
  - **Requirements:** 21.1–21.8
  - **Notes:** Full WCAG compliance testing requires assistive technology experts; these tests verify baseline. Document manual testing steps for judges.

---

### Device Compatibility Testing

- [ ] **T3.5 Execute device compatibility testing on reference matrix**
  - Test on 5+ reference devices: Pixel 5 (Android 14), Samsung S21 (Android 13), OnePlus 9 (Android 12), Xiaomi Mi 11 (Android 12, no gyro), Motorola G20 (Android 11, no gyro)
  - For each device, test:
    - Camera preview: Aspect ratio correct, unzoomed display, no lag
    - Zoom: Slider range detected correctly, zoom applied smoothly
    - Frame capture: 12 frames captured in <120s, thumbnails generated
    - Stitching: Panorama generated, dimensions 4096×2048, stitching < 30s
    - Gyroscope/accel: Alignment detection works (gyro on P5/S21, accel+mag fallback on Xiaomi/Motorola)
    - AR guidance ring: Animates smoothly, haptics fire correctly
    - Error recovery: Camera binding errors handled gracefully
    - Stability: No crashes during 30 min continuous use
  - Document results: `.kiro/device-compat-matrix.md` with pass/fail/partial status, known issues
  - **Priority:** Required
  - **Dependencies:** T1.15, T3.3
  - **Effort:** L (15 hours)
  - **Testing:** Manual hands-on testing; document with photos/videos
  - **Requirements:** 17.1–17.4
  - **Notes:** Borrow devices from community if needed. Document device screen sizes, camera specs, sensor config for judges.

- [ ] **T3.6 Document device compatibility matrix and create .md file**
  - Create `.kiro/device-compat-matrix.md` with table format
  - Columns: Device model, Android version, Screen size, Camera zoom range, Gyroscope, Test result, Known issues, Tested date
  - Rows: Include all 5+ reference devices
  - Example: "Pixel 5 | 14 | 6 in | 0.5x–4.0x | ✓ | ✅ Pass | None | Jan 2025"
  - Add notes section: Summary of major findings, performance metrics (cold start, stitching time), known limitations
  - Add supported locale section: Languages tested (Bangla, English)
  - Link to release notes for Rome submission
  - **Priority:** Required
  - **Dependencies:** T3.5
  - **Effort:** S (2 hours)
  - **Testing:** Verify matrix accuracy, share with team
  - **Requirements:** 17.1–17.5
  - **Notes:** This document is for judges; make it clear and professional.

---

### Localization

- [ ] **T3.7 Implement Bangla and English string resources**
  - Create `res/values/strings.xml` (English) and `res/values-bn/strings.xml` (Bangla)
  - Extract all user-facing strings from Composables: UI labels, button texts, error messages, guidance text
  - Translate strings to Bangla (hire native speaker for accuracy)
  - Verify all strings covered: Scanner guidance ("Rotate 30° for next shot" → "পরবর্তী শটের জন্য ৩০° ঘোরান"), error messages, settings labels, chat UI, handover passport, calculator, relocation
  - Implement currency/number formatting per locale: Use `Locale` to format price (৳ 10,00,000 in Bangla, €1.000.000 in German)
  - Implement date/time formatting per locale: DD/MM/YYYY in Bangla/Europe, MM/DD/YYYY in US
  - Add language selector in Settings: Dropdown to switch between Bangla/English (restart app to apply)
  - Test string resources: Verify no missing string references (compilation check)
  - **Priority:** Required
  - **Dependencies:** T1.15, T2.8, T2.11, T2.14
  - **Effort:** M (8 hours)
  - **Testing:** Manual testing on device in Bangla mode; verify currency formatting, date formatting
  - **Requirements:** 22.1–22.8
  - **Notes:** Involve native Bangla speakers for quality. Consider RTL text support (Bangla can be RTL context).

---

### Documentation

- [ ] **T3.8 Create Rome submission readiness checklist**
  - Create `.kiro/rome-submission-checklist.md` with sections:
    - **Code Quality**: All requirements implemented (25/25), no TODOs, no crashes in manual QA
    - **Testing**: Unit tests >= 80% coverage, integration tests pass on Firebase Test Lab, device matrix tested
    - **Device Compatibility**: Matrix populated with 5+ devices, all tested, known issues documented
    - **Localization**: Bangla + English complete, proofread, all UI strings translated
    - **Performance**: APK size < 100MB (release), cold start < 3s, stitching < 10s (Pixel 5)
    - **Documentation**: README updated, setup instructions clear, release notes prepared
    - **Security**: No hardcoded secrets, data encrypted at rest (Android KeyStore), network TLS 1.2+
    - **Accessibility**: Contrast verified, touch targets >= 48dp, TalkBack tested, text scaling works
    - **APK Building**: Debug + release APKs generated, signed, uploaded to GitHub Releases
    - **Submission**: Rome organizers notified, judges given APK link, technical support contact provided
  - Each section includes sub-items with verification status (☐ Not started, ◐ In progress, ☑ Complete)
  - Add owner field: Who is responsible for each section
  - Add deadline: Expected completion date
  - **Priority:** Required
  - **Dependencies:** T3.7
  - **Effort:** S (3 hours)
  - **Testing:** Review checklist weekly; update status
  - **Requirements:** 23.1–23.3
  - **Notes:** This is the master gate before Rome; must be 100% complete.

- [ ] **T3.9 Create Privacy & Security audit report**
  - Create `.kiro/privacy-security-audit.md`
  - Section 1: Data at Rest
    - Verify all sensitive data encrypted via Android KeyStore (panoramas, documents, chat, promises)
    - List all data types and encryption methods
  - Section 2: Data in Transit
    - Verify all network calls (if any) use TLS 1.2+
    - Check certificate validation, pinning recommendations
  - Section 3: Permissions
    - Audit all AndroidManifest.xml permissions: Camera, location, read/write storage, etc.
    - Verify each permission requested only when feature accessed (not at startup)
    - Verify no overly-broad permissions
  - Section 4: Crash Logs & Diagnostics
    - Verify crash logs don't contain PII
    - Remove any sensitive data from error messages
  - Section 5: Third-party Dependencies
    - List all gradle dependencies (including transitive)
    - Run `./gradlew dependencyCheck` or similar to scan for known vulnerabilities (or use CI plugin)
    - Document safe/verified libraries
  - Section 6: Data Deletion
    - Verify delete account flow clears all local data (panoramas, documents, messages)
    - Verify request to backend for remote deletion (if backend exists)
  - Document all findings and remediation steps
  - **Priority:** Required
  - **Dependencies:** All feature tasks (T1–T2)
  - **Effort:** M (7 hours)
  - **Testing:** Manual audit; dependency scan via CI
  - **Requirements:** 25.1–25.5
  - **Notes:** Privacy is critical for competition; be thorough.

- [ ] **T3.10 Update README and prepare release notes for Rome**
  - Update `README.md` main sections:
    - Feature overview: Scanner, AI assistant, calculator, handover passport, chat, relocation
    - Requirements: Android 11+, camera, internet (for some features)
    - Build & test instructions (reference CI, no local Gradle)
    - Device compatibility: Link to device matrix
    - Localization: Supported languages (Bangla, English)
    - Performance metrics: Cold start time, stitching time, APK size
    - Accessibility: WCAG 2.1 AA compliance statement, TalkBack support
    - Privacy: Data encrypted at rest, no remote servers required for core features
    - License: AGPL v3 or chosen license
  - Create `RELEASE_NOTES_ROME_2026.md`:
    - Version: 1.0.0-rome
    - Release date: April 2026
    - Major features: Scanner overhaul, feature completion, device compatibility testing
    - Known limitations: AI optional, relocation mode simplified, chat local-only (for demo)
    - Testing results: Device matrix (link), performance metrics
    - Known issues: None critical (if any, document)
  - **Priority:** Required
  - **Dependencies:** T3.10
  - **Effort:** S (3 hours)
  - **Testing:** Manual review; ensure links work
  - **Requirements:** 23.1
  - **Notes:** Write for both developers and judges. Keep language clear and professional.

---

## Phase 4: Final QA & Submission (Week 6) — ~40 hours

### Objective
Execute final QA pass, performance profiling, and prepare submission to Rome with all documentation complete.

---

### Final QA & Performance

- [ ] **T4.1 Execute comprehensive final QA test plan**
  - Create `QA_TEST_PLAN.md` in project root
  - Test on 3 reference devices (Pixel 5, Samsung S21, OnePlus 9) for 30 min continuous use
  - Test scenarios:
    - Launch app → ScannerScreen loads within 3s, preview shows without lag
    - Capture 12 frames → frames thumbnails appear, stitching begins, progress updates
    - Stitching completes → panorama displays in viewer, export works
    - Switch screens: Scanner → Listing → Calculator → Handover → Chat → Relocation, verify smooth transitions
    - Rotate device: Portrait ↔ landscape, verify layout adapts without data loss
    - Background/foreground: Minimize app, wait 5s, resume, verify state preserved
    - Enable/disable features: Toggle AI (if available), test with/without
    - Error scenarios: Disconnect camera (ADB), fill storage, low battery; verify recovery
    - Accessibility: Enable TalkBack, navigate all screens
  - Document results: Pass/fail for each scenario, screenshots/videos of issues
  - Log any crashes (check `adb logcat` for ANRs, exceptions)
  - Document baseline metrics: Cold start time, stitching time, memory usage, battery drain (1-hour scan session)
  - **Priority:** Required
  - **Dependencies:** T1.15, T2.1–T2.14, T3.1–T3.7
  - **Effort:** L (12 hours)
  - **Testing:** Manual hands-on; document findings in `.kiro/qa-final-results.md`
  - **Requirements:** 24.1–24.4
  - **Notes:** Document environment: Device OS version, screen brightness, WiFi/cellular signal strength. Retest any failing scenarios before submission.

- [ ] **T4.2 Measure and optimize performance metrics**
  - Cold start time: Measure from tap app icon to ScannerScreen preview ready (<3s target)
    - Use `adb shell am start-profiling` or Android Profiler
    - Optimize if needed: Lazy-load features, reduce init overhead
  - Stitching time: Measure 12-frame stitching on Pixel 5, Samsung S21 (<10s target)
    - Profile with Android Profiler to identify bottlenecks (CPU, memory, I/O)
    - Optimize: Bitmap pooling, thread efficiency, algorithm improvements
  - Memory usage: Monitor during scan, should stay <500MB peak <1GB
    - Use Android Profiler to detect memory leaks (check GC frequency)
    - Fix leaks: Clear listeners, cancel coroutines on screen exit
  - APK size: Release APK should be < 100MB
    - Use `bundletool` to analyze APK size by module
    - Strip unused resources, compress assets if needed
  - Battery drain: Monitor during 1-hour continuous scan
    - Expected: ~1–2% battery per scan (12 frames + stitching)
    - If excessive, profile CPU/wake-lock usage
  - Frame rate: Preview should run at 60 FPS (or 90 FPS on high-refresh devices)
    - Use Choreographer.getInstance() to measure frame timing
    - Profile GPU/rendering if needed
  - Create performance report: `.kiro/performance-report.md` with baseline metrics and optimization summary
  - **Priority:** Required
  - **Dependencies:** All feature tasks (T1–T3)
  - **Effort:** L (12 hours)
  - **Testing:** Use Android Profiler, Systrace, benchmark tools; document measurements
  - **Requirements:** 24.2–24.4
  - **Notes:** Performance profiling is time-consuming; prioritize high-impact optimizations (stitching, startup).

- [ ] **T4.3 Build release APK and upload to GitHub Releases**
  - Configure release build: Update version code/name (e.g., 1.0.0-rome)
  - Configure signing: Add keystore (or use CI signing)
  - Build release APK via CI: Push tag to main branch, GitHub Actions builds + signs APK
  - Create GitHub Release: Tag version (v1.0.0-rome), upload APK, write release notes (from T3.10)
  - Create debug APK: Also build for judges if needed (with all features unlocked)
  - Verify APK: Download, install on device, verify basic functionality (scan, chat, etc.)
  - Test on fresh Android 11 device: Simulate first-time user experience
  - Measure APK size: Document final size in release notes
  - **Priority:** Required
  - **Dependencies:** T3.10, T4.1, T4.2
  - **Effort:** S (4 hours)
  - **Testing:** Manual install + smoke test
  - **Requirements:** 12.1–12.7, 23.2, 24.1
  - **Notes:** Ensure signing key is secure; don't commit to repo. GitHub Actions should handle signing in secrets.

- [ ] **T4.4 Verify CI/CD pipeline and gate checks**
  - Verify GitHub Actions workflows execute on every push:
    - Build workflow: `gradle build`, runs on Ubuntu latest with Android 14 emulator
    - Test workflow: `gradle test`, runs unit tests, generates coverage report
    - Lint workflow: `gradle lint`, checks code style, security warnings
    - Device test workflow: Runs integration tests on Firebase Test Lab (Pixel 5, S21, OnePlus 9)
  - Verify all checks pass: Build ✓, Tests ✓, Coverage >= 80%, Lint ✓
  - Verify release build: Tag push triggers release APK build + upload
  - Document CI/CD setup: Create `.github/workflows/README.md` explaining pipelines, how to debug failures
  - Ensure no manual workarounds needed: All builds via CI, no local Gradle as per AGENTS.md constraint
  - **Priority:** Required
  - **Dependencies:** T1.15, T3.1–T3.4
  - **Effort:** S (4 hours)
  - **Testing:** Trigger manual run of workflows; verify results
  - **Requirements:** AGENTS.md constraint (no local Gradle)
  - **Notes:** CI/CD is critical for competition; ensure it's reliable before Rome.

---

### Pre-Submission Gate

- [ ] **T4.5 Complete and verify Rome submission readiness checklist**
  - Go through `.kiro/rome-submission-checklist.md` item by item
  - Verify all requirements implemented: 25/25 requirements have implementation code, unit tests, manual testing
  - Verify all features complete: Scanner ✓, AI (graceful) ✓, Calculator ✓, Passport ✓, Chat ✓, Relocation ✓
  - Verify testing complete: Unit >= 80% ✓, Integration on Firebase ✓, Device matrix ✓, Accessibility ✓
  - Verify device compatibility: Matrix complete, 5+ devices tested, known issues documented
  - Verify localization: Bangla + English complete, proofread
  - Verify performance: Cold start < 3s, stitching < 10s, APK < 100MB
  - Verify documentation: README, release notes, device matrix, privacy audit, performance report
  - Verify security: No hardcoded secrets, encryption enabled, permissions audited
  - Verify accessibility: Contrast, touch targets, TalkBack, text scaling
  - Verify APKs built: Release + debug APKs on GitHub Releases
  - Sign off: Mark all items complete ✓
  - **Priority:** Required
  - **Dependencies:** T4.1–T4.4
  - **Effort:** S (3 hours)
  - **Testing:** Self-review; have team member do secondary review
  - **Requirements:** 23.1–23.3
  - **Notes:** This is the go/no-go gate; if any item fails, fix before Rome submission.

- [ ] **T4.6 Prepare submission package and technical documentation for judges**
  - Create submission folder: `submission-rome-2026/`
  - Include:
    - Release APK (from GitHub Releases)
    - Debug APK (if needed)
    - README.md with feature overview and build instructions
    - RELEASE_NOTES.md
    - `.kiro/device-compat-matrix.md` (device testing results)
    - `.kiro/performance-report.md` (metrics)
    - `.kiro/accessibility-report.md` (WCAG compliance)
    - `.kiro/privacy-security-audit.md`
    - Source code link (GitHub repo)
  - Write cover letter: Brief intro to Dorja, highlights of Rome submission (scanner UX, feature completeness, device compatibility)
  - Prepare support contact: Email/name for judges to reach out with questions
  - Test all documentation links: Verify no broken links, all documents readable
  - **Priority:** Required
  - **Dependencies:** T3.8–T3.10, T4.3, T4.5
  - **Effort:** S (3 hours)
  - **Testing:** Manual review; dry-run unzipping and reading submission package
  - **Requirements:** 23.1–23.3
  - **Notes:** Professional presentation matters for judges; make documentation clear and accessible.

- [ ] **T4.7 Communicate Rome submission to organizers and team**
  - Send submission package to Rome organizers (FIRSO 2026 committee)
  - Include APK download link, GitHub repo link, documentation
  - Provide technical support contact info: Email, phone (if available)
  - Notify team: Submission complete, awaiting Rome event
  - Set up contingency: Identify backup contact if primary unavailable
  - Create post-Rome plan: Plan for bug fixes, post-mortem, documentation
  - **Priority:** Required
  - **Dependencies:** T4.6
  - **Effort:** XS (1 hour)
  - **Testing:** Verify email received by organizers
  - **Requirements:** 23.1–23.3
  - **Notes:** Keep records of all communications with organizers.

---

## Notes & Constraints

### No Local Gradle Builds
Per AGENTS.md, all builds happen via GitHub Actions CI. Tasks should assume:
- Developers do NOT run `gradle build`, `gradlew`, etc. locally
- All testing/verification via CI
- If changes need compile check, push to branch and wait for CI results
- Read `ci-logs` branch (`ci-errors.txt`) or Actions run logs for failures

### Device Testing Matrix (Reference Devices)
All device-dependent tasks target these 5+ manufacturers:
- **Pixel 5** (Google, Android 14, Tensor chip, gyroscope ✓, 0.5x–4.0x zoom)
- **Samsung Galaxy S21** (Samsung, Android 13, Exynos, gyroscope ✓, 0.5x–3.0x zoom)
- **OnePlus 9** (OnePlus, Android 12, Snapdragon, gyroscope ✓, 1.0x–2.0x zoom)
- **Xiaomi Mi 11** (Xiaomi, Android 12, Snapdragon, NO gyroscope, 0.8x–5.0x zoom)
- **Motorola G20** (Motorola, Android 11, MediaTek, NO gyroscope, 1.0x–2.0x zoom)

### Property-Based Testing
For tasks involving ScanGeometry, FinancialCalculator, SensorFusion, use fast-check library to generate 1000+ test cases per property.

### Effort Estimates Breakdown
- **XS** (1–2 hours): Documentation, small utility functions
- **S** (2–8 hours): Single UI component, simple business logic
- **M** (8–16 hours): Complex UI component, moderate business logic
- **L** (16–32 hours): Major component integration, heavy computation (e.g., stitching)
- **XL** (32+ hours): Not used in this plan; split into multiple L tasks

**Total Phase Effort:**
- Phase 1: ~80 hours
- Phase 2: ~60 hours
- Phase 3: ~60 hours
- Phase 4: ~40 hours
- **Total: ~240 hours (~30 hours/week for 6 developers weeks)**

---

## Task Dependency Graph

The following dependency graph shows execution waves for parallel task scheduling. Each wave represents a set of independent tasks that can run concurrently; later waves depend on completion of earlier waves.

```json
{
  "waves": [
    {
      "id": 0,
      "description": "Foundation: Data models, core utilities, Android version update",
      "tasks": ["T1.1", "T2.2", "T2.3", "T2.13", "T1.8"]
    },
    {
      "id": 1,
      "description": "Camera & Sensor Infrastructure",
      "tasks": ["T1.2", "T1.4", "T1.6"]
    },
    {
      "id": 2,
      "description": "Camera Preview & Zoom UI",
      "tasks": ["T1.3", "T1.5"]
    },
    {
      "id": 3,
      "description": "AR Guidance Ring & Frame Rail",
      "tasks": ["T1.7", "T1.9"]
    },
    {
      "id": 4,
      "description": "Capture Control & Stitching Engine",
      "tasks": ["T1.10", "T1.11", "T1.12"]
    },
    {
      "id": 5,
      "description": "Error Recovery UI & Scanner ViewModel",
      "tasks": ["T1.13", "T1.14"]
    },
    {
      "id": 6,
      "description": "Scanner Screen Integration",
      "tasks": ["T1.15"]
    },
    {
      "id": 7,
      "description": "Feature Completion: AI, Calculator, Passport, Chat",
      "tasks": ["T2.1", "T2.4", "T2.5", "T2.6", "T2.7", "T2.9", "T2.10"]
    },
    {
      "id": 8,
      "description": "Feature UI: Handover Passport, Chat, Relocation",
      "tasks": ["T2.8", "T2.11", "T2.12", "T2.14"]
    },
    {
      "id": 9,
      "description": "Testing: Unit tests & Accessibility",
      "tasks": ["T3.1", "T3.2", "T3.4", "T3.7"]
    },
    {
      "id": 10,
      "description": "Integration Testing & Device Compatibility",
      "tasks": ["T3.3", "T3.5"]
    },
    {
      "id": 11,
      "description": "Documentation & Security Audit",
      "tasks": ["T3.6", "T3.8", "T3.9", "T3.10"]
    },
    {
      "id": 12,
      "description": "Final QA & Performance Profiling",
      "tasks": ["T4.1", "T4.2"]
    },
    {
      "id": 13,
      "description": "APK Build & CI/CD Verification",
      "tasks": ["T4.3", "T4.4"]
    },
    {
      "id": 14,
      "description": "Submission Gate & Final Checklist",
      "tasks": ["T4.5", "T4.6", "T4.7"]
    }
  ]
}
```

---

## Success Criteria (Rome Submission)

✓ **Code Quality**: All 25 requirements implemented, no TODOs, zero crashes in final QA  
✓ **Testing**: Unit test coverage >= 80%, integration tests pass on Firebase Test Lab, device matrix complete  
✓ **Device Compatibility**: Tested on 5+ manufacturers (Pixel, Samsung, OnePlus, Xiaomi, Motorola), Android 11–14  
✓ **Performance**: Cold start < 3s, stitching < 10s (Pixel 5), APK < 100MB  
✓ **Accessibility**: WCAG 2.1 AA compliance verified (contrast, touch targets, TalkBack, text scaling)  
✓ **Localization**: Bangla + English complete, proofread, all UI strings translated  
✓ **Documentation**: README, release notes, device matrix, privacy audit, performance report complete  
✓ **Security**: No hardcoded secrets, data encrypted at rest, network TLS 1.2+  
✓ **Submission**: Release APK on GitHub, judges notified, Rome organizers have all documentation  

---

## Document Metadata

- **Author**: Kiro (Spec Agent)
- **Date Created**: 2025-01-XX
- **Last Updated**: 2025-01-XX
- **Spec Type**: Feature (Requirements-First Workflow)
- **Feature Name**: dorja-rome-readiness
- **Target Deadline**: Rome FIRSO 2026 (April 2026)
- **Priority**: Critical (Competition submission)
- **Effort Estimate**: ~240 hours total (~30 hours/week for 6 developers weeks)
