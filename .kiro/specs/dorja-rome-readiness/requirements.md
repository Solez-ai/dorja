# Requirements Document

## Requirements: Dorja Rome Readiness (FIRSO 2026)

## Introduction

Dorja Homestation is a privacy-first real estate platform for Bangladesh featuring industry-leading 3D room scanning, trust verification, safe viewings, and digital property handover. The app is heading to FIRSO 2026 in Rome (Fibonacci International Robot Olympiad) and must meet competition-grade standards: strong UX/UI, Android 11+ support, robust device compatibility, and polished feature completeness.

This specification addresses critical audit findings, incomplete features, device compatibility gaps, and polish requirements to ensure Dorja is **competition-ready and production-grade** by Rome.

### Scope
- Scanner UX/UI redesign and optimization
- Feature completion (AI assistant, calculator, handover passport, chat, relocation)
- Device compatibility and performance hardening
- Testing, documentation, and accessibility
- Rome submission and judging readiness

### Success Criteria
- Scanner works smoothly on Android 11+ phones (Samsung, Google Pixel, OnePlus, etc.)
- Preview and capture experience matches industry standards (e.g., Google Camera, Zillow 3D)
- All features functionally complete and polished
- Comprehensive device compatibility testing documented
- Zero critical crashes on target device matrix

---

## Glossary

- **Scanner**: The 3D room capture system combining CameraX, gyroscope/accelerometer, and equirectangular stitching
- **Panorama / Equirectangular Image**: A 2:1 aspect-ratio 360-degree spherical image (e.g., 4096×2048px) displaying full-room coverage
- **Gyroscope Fallback**: Accel+mag fusion when gyroscope unavailable (fallback sensor stack)
- **AR Guidance Ring**: Visual target indicator helping user align camera during capture
- **Zoom Ratio**: Camera lens focal-length multiplier (0.5x = ultra-wide, 1.0x = standard, 2.0x+ = telephoto)
- **Frame Preview Rail**: Carousel of captured frame thumbnails during multi-frame capture
- **Stitching**: Algorithm combining overlapping frames into a single panorama
- **Anchor**: Reference pose (rotational position) at start of scan for alignment
- **Handover Passport**: Digital record tracking property promises, documents, and milestones from listing to key handover
- **Cactus SDK**: Commented-out on-device AI assistant (rule-based, not ML-based)
- **SafeView Pass**: QR-based viewing appointment with address verification and emergency contact integration
- **EARS Pattern**: Structured requirement format (Event-driven, Ubiquitous, State-driven, Unwanted event, Optional feature, Complex)
- **Property-Based Testing (PBT)**: Generative testing that verifies correctness across broad input domains
- **Device Matrix**: Set of target phones covering Android 11+ with varying screen sizes, camera stacks, and sensor configurations
- **WCAG Compliance**: Web Content Accessibility Guidelines; focus here is WCAG 2.1 Level AA for mobile (color contrast, touch targets, text scaling)
- **Localization**: Language and regional adaptation (Bangla, English, European languages)

---

## Requirements

### Requirement 1: Scanner Preview & Capture UX — Fix Zoom and Display Scale

**User Story:** As a property seller or enumerator, I want a clear, unzoomed camera preview that shows the room at a natural scale, so that I can easily frame shots and capture accurate 360-degree scans without frustration.

#### Acceptance Criteria

1. WHEN the user opens the Scanner, THE PreviewRenderer SHALL display the camera feed at **native device aspect ratio** (not super-zoomed or stretched) at a scale of 1:1 device pixels to camera pixels, so that the user sees a natural, unzoomed live preview.
2. WHEN the user adjusts the zoom slider (0.5x, 0.8x, 1.0x), THE zoom SHALL apply smoothly and THE preview SHALL re-render immediately without lag or black frames.
3. WHEN the device has multiple camera lenses (ultra-wide, standard, telephoto), THE Scanner SHALL default to **0.8x zoom on the standard lens** (e.g., Sony IMX sensor at f/1.8), and THE app SHALL detect min/max zoom range at startup and limit the slider to available ratios.
4. WHEN the device lacks a standard lens (only ultra-wide or telephoto available), THE Scanner SHALL gracefully fall back to the widest available lens and log the fallback.
5. WHEN the user is on a foldable or ultra-tall device (aspect ratio > 2.5:1), THE preview SHALL scale proportionally to fit the screen without pillarboxing or letterboxing.
6. THE Frame Preview Rail (carousel of captured frames) SHALL display thumbnails at **no smaller than 80px height** so that the user can see frame quality and coverage before finalizing the scan.

---

### Requirement 2: AR Guidance and Alignment Feedback

**User Story:** As a user capturing a room scan, I want clear visual and haptic feedback on how well I'm aligning the camera with the AR target, so that I can capture high-quality overlapping frames and know when I've achieved proper coverage.

#### Acceptance Criteria

1. WHEN the user is in capture mode, THE Guidance Ring (AR target indicator) SHALL be **rendered at 120-180px diameter** (proportional to screen size) at the center of the preview, using a **high-contrast color** (e.g., bright cyan or lime green) with a 3px stroke and semi-transparent fill (alpha 0.3).
2. WHEN the accelerometer/gyroscope detects motion toward the next capture point (30-degree rotation), THE Guidance Ring SHALL animate to a new position on screen (e.g., slide to the edge), and the app SHALL emit a **haptic pulse** (medium strength, 100ms duration) to confirm alignment target achieved.
3. WHEN the gyroscope data suggests frame overlap is optimal (overlap threshold met), THE Guidance Ring SHALL change color to **green** and emit a **success haptic pattern** (dual pulse: 50ms + pause + 50ms).
4. WHEN the gyroscope data suggests overlap is poor or missing, THE Guidance Ring SHALL remain **yellow** and display a **text label** below ("Rotate 30° for next shot" or "Hold steady"), and the app SHALL NOT auto-trigger capture until overlap improves.
5. WHEN the user deviates >10 degrees from the planned capture sequence, THE app SHALL emit a **warning haptic pattern** (three short pulses) and display a toast warning ("Recalibrating alignment...").
6. IF the gyroscope is unavailable or unreliable, THE Guidance Ring SHALL display a **fallback mode indicator** ("Accel mode — hold steady for 2 sec") and use accelerometer + magnetometer fusion instead, without degrading user experience.

---

### Requirement 3: Frame Capture and Stitching with Timeout & Error Recovery

**User Story:** As a user, I want the scanner to capture frames reliably, stitch them without crashing or stalling, and recover gracefully if something goes wrong, so that I can complete scans on budget devices and poor network conditions.

#### Acceptance Criteria

1. WHEN the user initiates a frame capture, THE CameraX pipeline SHALL capture an image in **JPEG format** (quality ≥ 95%) and display it in the Frame Preview Rail within **500ms**.
2. WHEN 12 frames are ready for stitching, THE Stitching Engine SHALL begin processing on a background thread and display a **progress indicator** ("Stitching 12 frames... 25%, 50%, 75%, 100%") that updates at least every 500ms.
3. WHEN stitching takes longer than **8 seconds** (timeout threshold for budget devices), THE app SHALL display a warning ("Large image; this may take a minute...") but SHALL continue processing unless the user cancels.
4. WHEN stitching exceeds **30 seconds**, THE app SHALL display a **cancel option** and a fallback suggestion ("Try recapturing with 8 frames instead of 12").
5. WHEN stitching completes successfully, THE panorama SHALL be **validated** (check dimensions are 2:1, no NaN pixels, byte size < 50MB), and THE result SHALL be cached locally before moving to the preview screen.
6. IF stitching fails (OOM, corrupted frames, or math error), THE app SHALL log the error, display a **user-friendly error message** ("Failed to stitch frames. Please try again or recapture."), and offer a **Retry** or **Capture Again** button.
7. IF frame capture fails (camera is locked, permission denied at runtime), THE app SHALL immediately display a **recovery flow** ("Camera unavailable. Check permissions or restart the app.") with a **Grant Permissions** button.
8. WHEN the user cancels a scan in progress, THE app SHALL clear frame buffers, reset the frame counter to 0, and return to the idle preview state within **1 second**.

---

### Requirement 4: Device Zoom Range Detection and Adaptation

**User Story:** As a developer, I want the app to automatically detect and adapt to each device's camera zoom capabilities, so that the zoom slider and default zoom are appropriate for every device and camera stack.

#### Acceptance Criteria

1. WHEN the app initializes CameraX on a new device, THE Camera Manager SHALL query the Camera2 metadata for **minZoomRatio** and **maxZoomRatio** (or fallback to default 1.0x – 8.0x if metadata unavailable).
2. WHEN zoom metadata is available, THE Zoom Slider SHALL be **constrained to the detected range** and display only achievable zoom levels (e.g., 0.5x – 4.0x on some devices, 1.0x – 2.0x on others).
3. WHEN the app detects an ultra-wide lens (equiv. focal length < 24mm), THE default zoom SHALL be set to **0.8x**; WHEN a standard lens (equiv. focal length 28–35mm) is available, THE default SHALL be **1.0x**.
4. IF zoom metadata fails to load, THE app SHALL log a warning and use safe defaults (1.0x – 2.0x), and THE user SHALL be able to override via the slider.
5. WHEN the user selects a zoom level outside the camera's supported range, THE app SHALL snap to the nearest supported value and display a subtle notification ("Using maximum zoom for this device").

---

### Requirement 5: Gyroscope Fallback and Sensor Fusion

**User Story:** As a user on a device without a gyroscope (e.g., some budget Android phones), I want the scanner to still work using accelerometer and magnetometer fusion, so that I can capture room scans on any Android 11+ phone.

#### Acceptance Criteria

1. WHEN the app starts, THE Sensor Manager SHALL detect available sensors (gyroscope, accelerometer, magnetometer) and log the configuration.
2. IF gyroscope is available, THE app SHALL use **gyroscope + accelerometer fusion** for primary orientation tracking (hybrid approach for stability).
3. IF gyroscope is unavailable, THE app SHALL fall back to **accelerometer + magnetometer fusion** (9-axis compass-based rotation) and display a **fallback indicator** on the Guidance Ring ("Accel mode").
4. WHEN in accel-only mode, THE Guidance Ring SHALL require the user to **hold the phone steady for 2 seconds** at each capture point before auto-triggering, to allow compass calibration.
5. WHEN the magnetometer is affected by external interference (e.g., nearby metal), THE app SHALL detect unstable heading values (variance > threshold) and display a warning ("Move away from metal objects").
6. IF all three sensors fail or are unavailable, THE app SHALL display an error message ("This device cannot capture scans; gyroscope and accelerometer required") and disable the Scanner screen.
7. WHEN the user transitions between gyro mode and accel mode (e.g., gyro stops reporting), THE app SHALL log the transition, display a toast notification, but SHALL continue scanning without interruption.

---

### Requirement 6: Screen Size Adaptation (Foldables, Tablets, Notches)

**User Story:** As a user on a foldable or tablet, I want the scanner UI to adapt gracefully to unusual screen geometries, so that the preview, buttons, and guidance ring remain visible and touchable.

#### Acceptance Criteria

1. WHEN the app runs on a foldable device in folded mode (aspect ratio ~19.5:9, narrow), THE preview SHALL maintain its aspect ratio and THE controls (zoom, capture, frames rail) SHALL stack vertically without overlapping the preview.
2. WHEN the app runs on a foldable in unfolded mode (large square display), THE preview SHALL scale to fill available space and THE frame preview rail SHALL move to the side (landscape orientation) rather than bottom.
3. WHEN the app runs on a tablet (>= 7 inches, landscape), THE preview SHALL use landscape orientation by default, and THE zoom slider and capture button SHALL appear on the side panel (not bottom).
4. WHEN the device has a notch or punch-hole (content inset), THE Guidance Ring, frame preview rail, and buttons SHALL respect the inset and not be hidden behind the notch.
5. WHEN the display has unusual aspect ratios (ultra-wide, ultra-tall, or square), THE preview SHALL fill available space proportionally, and the scanner SHALL adapt frame capture geometry accordingly (no distortion or cropping).
6. WHEN the device is rotated from portrait to landscape, THE preview AND all UI elements SHALL re-layout within **300ms** without losing captured frames or resetting the scan.

---

### Requirement 7: AI Assistant Restoration or Graceful Disable

**User Story:** As a user, I want smart assistance with property descriptions and AI-powered feature detection (or I want the app to work perfectly fine without it), so that the app is complete and never broken by missing dependencies.

#### Acceptance Criteria

1. WHEN the Cactus SDK is available (dependency restored in build.gradle.kts), THE app SHALL load the AI model at startup and display an **AI Assistant badge** in the Listing Creation flow.
2. WHEN the user opens Listing Creation and Cactus is available, THE app SHALL offer **AI-powered room suggestions** ("This looks like a bedroom with hardwood floor; suggested tags: bedroom, flooring, natural light").
3. IF Cactus SDK is not available (commented out or removed), THE app SHALL gracefully disable the AI features WITHOUT crashing, and the Listing Creation flow SHALL work identically but without AI suggestions.
4. WHEN the user toggles AI assistance OFF in Settings, THE app SHALL respect the preference and skip AI processing on subsequent scans.
5. IF the AI model fails to load at startup (corrupt, incompatible, or missing), THE app SHALL log the error and continue with AI disabled (not a blocker).
6. THE app SHALL NOT crash or display an error message to the user if Cactus is unavailable; the experience SHALL be seamless with or without it.

---

### Requirement 8: Financial Calculator Refinement

**User Story:** As a user, I want the Financial Calculator to be polished, accurate, and helpful for property investment decisions, so that I trust its outputs and use it regularly.

#### Acceptance Criteria

1. WHEN the user opens the Financial Calculator, THE app SHALL display input fields for **Property Price**, **Down Payment**, **Interest Rate**, **Loan Term (years)**, and **Monthly Rent** (for ROI calculation).
2. WHEN the user modifies an input, THE Calculator SHALL re-compute and display results in **real-time** (<100ms latency): Monthly Payment, Total Interest, ROI%, and Cash-on-Cash Return.
3. WHEN the user enters invalid input (negative price, interest rate > 100%), THE app SHALL display an inline validation error and disable the Calculate button until the input is corrected.
4. WHEN the user clicks **"Save Calculation"**, THE app SHALL store the calculation in a local history database with a timestamp and property name (if applicable), and display a toast confirmation.
5. WHEN the user views the Calculation History, THE app SHALL display a sortable, filterable list of past calculations with the ability to **Load** (restore to current calculator) or **Delete** each entry.
6. WHEN the user taps on a result, THE app SHALL display a **detailed breakdown** (amortization table summary, ROI chart, cash-flow timeline).
7. THE calculator interface SHALL be **responsive** on phones (portrait), tablets (landscape), and foldables, with input fields visible and results non-overlapping.
8. WHEN the app runs on a device with different locale settings (e.g., Bangladesh, Europe), THE Calculator SHALL display currency symbols and number formats appropriately (e.g., ৳, €, $).

---

### Requirement 9: Handover Passport — Full Promise Tracking and Document Vault

**User Story:** As a buyer, I want to track every promise the seller made (renovation, legal clearance, transfer date), upload and verify documents, and have a digital record that survives the transaction, so that I have legal recourse if promises are broken.

#### Acceptance Criteria

1. WHEN a Property is created on the platform, THE system SHALL automatically generate a **Handover Passport** (unique ID, creation date, property reference, seller/buyer records).
2. WHEN the seller is creating a listing, THE app SHALL display a **Promises section** where the seller can add promises (e.g., "Renovated by Jan 31, 2025", "Legal clearance from authorities", "Transfer within 30 days of payment").
3. WHEN the seller adds a promise, THE app SHALL store it with **timestamp, description, target completion date, and status** (pending, in-progress, completed, overdue, failed).
4. WHEN the buyer views the property, THE app SHALL display ALL seller promises in the Handover Passport, clearly visible and **immutable** (buyer cannot edit, only view and acknowledge).
5. WHEN either party uploads a document (deed, mutation, clearance certificate, renovation photos), THE app SHALL:
   - Store the document in an **encrypted vault** (property-scoped storage, encrypted at rest)
   - Generate a **document hash** (SHA-256) for tampering detection
   - Display the document with metadata (name, date uploaded, uploader role, hash)
   - Allow both parties to view but NOT delete or modify
6. WHEN a seller updates a promise status (e.g., "renovation complete"), THE app SHALL display the status change in the Handover Passport timeline with timestamp, and SHALL notify the buyer.
7. WHEN a promise target date passes, THE app SHALL automatically mark it **"Overdue"** (if status is not "completed") and send a notification to the buyer.
8. WHEN either party clicks "Sign Off" on a milestone (e.g., "Renovation complete"), THE app SHALL record a **digital signature** (timestamp + party ID) and display it immutably in the passport.
9. THE Handover Passport SHALL be **exportable as PDF** for legal record-keeping, including all promises, documents, and signatures.

---

### Requirement 10: Chat System — Rich Text and Media Support

**User Story:** As a buyer or seller, I want to exchange messages with rich formatting, images, and document links within the app, so that I can communicate clearly without switching to external apps.

#### Acceptance Criteria

1. WHEN the user opens a chat thread, THE app SHALL display the message history with **sender identity, timestamp, and message content** clearly visible.
2. WHEN the user types a message, THE app SHALL support **bold, italic, and link** formatting (using Markdown or a rich-text editor), and SHALL render the formatted message in the chat.
3. WHEN the user selects "Attach Image", THE app SHALL open the photo picker, allow the user to select one or more images, and display thumbnails in the message input field.
4. WHEN the user sends a message with images, THE app SHALL:
   - Compress images to a reasonable size (max 5MB total per message)
   - Upload to encrypted storage
   - Display images inline in the chat thread as non-removable records
   - Show a **progress indicator** ("Uploading image 1 of 3...") during upload
5. WHEN a message contains a link or document reference (e.g., from the Handover Passport), THE app SHALL display the link as an interactive card with a preview and **"View Document"** button.
6. WHEN the user receives a new message, THE app SHALL display a **notification** (if app is in background) and play a **sound** (unless muted in Settings).
7. WHEN the user is typing, THE app SHALL display "User is typing..." on the recipient's screen, and SHALL update the status within **1 second** of the sender stopping.
8. WHEN a message is sent, THE app SHALL display a **delivery status** (Sending → Sent → Seen) with visual indicators (hourglass, checkmark, double-checkmark).

---

### Requirement 11: Relocation Mode — Comprehensive Country and Market Data

**User Story:** As a user relocating to a new country, I want access to property market data, visa/residency information, and relocation guides for my destination, so that I can make informed property decisions in an unfamiliar market.

#### Acceptance Criteria

1. WHEN the user enables Relocation Mode in Settings, THE app SHALL display a **Country Selector** with supported destinations (Bangladesh, India, Nepal, Bhutan, selected European markets).
2. WHEN the user selects a destination country, THE app SHALL load **country-specific data** including:
   - Market overview (median price, price trends, avg. rental yield)
   - Visa and residency requirements (expat visa types, duration, requirements)
   - Property regulations (foreigners' buying restrictions, registration process)
   - Language guides (basic phrases for property viewing in local language)
   - Tax information (property tax, rental income tax, capital gains)
3. WHEN the user searches for properties in relocation mode, THE app SHALL apply **country-specific filters** (visa category, distance from expat hubs, furnished/unfurnished, proximity to international schools).
4. WHEN the user views a relocation property, THE app SHALL display **relocation-specific information** (expat-friendly rating, proximity to embassies, international schools, expat community size).
5. WHEN the user clicks on relocation guidance (e.g., "How to register property in India"), THE app SHALL display a **step-by-step guide** with links to official government portals and verified third-party resources.
6. WHEN the app receives an update to country data (new tax laws, visa changes), THE app SHALL download and cache the update, and display a **"Data Updated"** notification.
7. IF relocation data for a destination is incomplete or outdated, THE app SHALL display a **"Partial Data"** badge and recommend contacting official sources.

---

### Requirement 12: Android 11+ Target and Minimum Version Clarity

**User Story:** As a developer, I want clarity on Android version support so that I know which APIs are available, which legacy code can be removed, and which compatibility measures are required.

#### Acceptance Criteria

1. THE app's **minSdk SHALL be updated to 30** (Android 11), effective immediately for FIRSO 2026 submission. Legacy Android 7–10 support is removed.
2. THE app's **targetSdk SHALL remain 36** (Android 14), ensuring compatibility with the latest Android OS and access to modern APIs.
3. ALL app code SHALL use Android 11+ APIs directly (no shims or compatibility fallbacks for Android 10 or earlier).
4. THE app's **runtime permission strategy** SHALL align with Android 11+ (no ACCESS_FINE_LOCATION on app startup; request only when needed).
5. WHEN the app is installed on an Android 11 device, ALL permissions SHALL be requested at runtime (no manifest-only permissions).
6. THE app's build.gradle.kts SHALL reflect minSdk = 30 and targetSdk = 36, and CI/CD SHALL enforce this.
7. THE documentation (README.md, release notes) SHALL state: "Dorja requires Android 11 or later (API 30+)" and recommend minimum phones from 2019 onwards.

---

### Requirement 13: Stitching Performance and Memory Management

**User Story:** As a developer, I want stitching to be efficient and memory-safe on budget devices, so that the app never crashes due to OOM and completes scans in reasonable time even on mid-range phones.

#### Acceptance Criteria

1. THE Stitching Engine SHALL process frames in a background thread (not main thread) and report progress without blocking UI.
2. WHEN stitching begins, THE app SHALL estimate required memory based on frame count and resolution, and display a warning if memory is below safe threshold (e.g., <300MB free).
3. WHEN stitching is in progress, THE app SHALL NOT allow the user to capture additional frames or navigate away (button disabled, navigation blocked).
4. THE Stitching Engine SHALL use **bitmap pooling** or similar memory-efficient techniques to avoid garbage collection pauses during frame processing.
5. WHEN a frame is stitched successfully, THE memory for that frame SHALL be released immediately (not held until the full panorama is complete).
6. WHEN stitching completes, THE panorama SHALL be **downsampled to a max resolution of 4096×2048px** (if source is higher), to limit file size and memory footprint.
7. WHEN the resulting panorama exceeds **50MB**, THE app SHALL apply **JPEG compression** (increase quality threshold) to bring it below 50MB, and SHALL log the compression ratio.
8. IF stitching runs out of memory (OOM exception), THE app SHALL gracefully catch the exception, clear all frame buffers, display an error, and allow the user to retry with fewer frames.

---

### Requirement 14: Camera Binding Failure Recovery

**User Story:** As a user, I want the scanner to recover gracefully if the camera fails to bind (e.g., another app is using the camera), so that I get a helpful error message and can retry without crashing.

#### Acceptance Criteria

1. WHEN the user opens the Scanner, THE app SHALL attempt to bind the camera via CameraX, and SHALL handle the following error cases:
   - **Camera already in use**: Display "Camera is in use by another app. Close it and try again."
   - **Permission denied**: Display "Camera permission denied. Go to Settings > Permissions to enable."
   - **Camera hardware not available**: Display "Device has no camera."
2. WHEN a camera binding error occurs, THE Scanner screen SHALL display a **full-screen error state** (not a crash dialog) with an **"OK"** or **"Retry"** button.
3. WHEN the user taps **Retry**, THE app SHALL attempt to rebind the camera, and if successful, SHALL return to the normal preview state.
4. WHEN the camera is successfully bound, THE app SHALL start the preview stream within **500ms** and display a smooth, flicker-free live feed.
5. IF the preview stream fails mid-scan (camera disconnected), THE app SHALL pause the preview, log the error, display a recovery message ("Camera disconnected; retrying..."), and attempt to rebind within **3 seconds**.

---

### Requirement 15: Testing Strategy — Unit Tests for ScanGeometry

**User Story:** As a developer, I want comprehensive unit tests for the ScanGeometry and stitching logic, so that alignment calculations, frame overlap, and panorama generation are verifiable and regression-free.

#### Acceptance Criteria

1. A new module `app/src/test/java/com/example/scanner/ScanGeometryTest.kt` SHALL exist with unit tests covering:
   - **Rotation matrix calculations**: Verify that quaternion-to-matrix conversion is accurate
   - **Frame overlap detection**: Verify that overlap threshold logic correctly identifies when two frames have sufficient overlap
   - **Panorama dimension calculation**: Verify that output panorama dimensions (2:1 aspect, pixel resolution) are calculated correctly
   - **Edge cases**: Zero rotation, 180-degree rotation, gimbal lock conditions
2. EACH test SHALL use property-based testing (fast-check or similar) to verify correctness across 1000+ randomly generated rotation matrices and frame configurations.
3. WHEN tests are run (`./gradlew test`), ALL tests SHALL pass, and code coverage for ScanGeometry and stitching logic SHALL be >= 80%.
4. WHEN a test fails, THE error message SHALL clearly indicate the failing input case (e.g., "rotation=45°, overlap=0.8; expected 4096×2048 panorama but got 4096×1024").
5. A new test `StitchingIntegrationTest.kt` SHALL verify end-to-end stitching (capture 12 frames from reference device, stitch, verify panorama dimensions and quality).

---

### Requirement 16: Integration Tests for Scanner Flow

**User Story:** As a QA engineer, I want integration tests that simulate a real user capturing a room, so that I can verify the entire scanner flow works end-to-end on multiple devices.

#### Acceptance Criteria

1. A new file `app/src/androidTest/java/com/example/scanner/ScannerFlowTest.kt` SHALL exist with instrumentation tests covering:
   - **Camera binding**: Verify camera initializes and preview starts
   - **Frame capture**: Simulate 12 frame captures (using mocked CameraX output) and verify each frame is saved
   - **Stitching**: Verify stitching completes and produces a valid panorama
   - **Error recovery**: Simulate camera binding failure and verify recovery flow works
2. TESTS SHALL run on Firebase Test Lab (or local Android emulator) against representative device configurations (Pixel 5, Samsung S21, OnePlus 9).
3. WHEN a test runs, IT SHALL take <5 minutes to complete (not a long-running stress test).
4. WHEN a test fails, LOGS SHALL include:
   - Device model, Android version, camera capabilities
   - Timestamp and frame count at failure point
   - Error stack trace
5. TESTS SHALL be part of the CI/CD pipeline (run on every commit to main).

---

### Requirement 17: Device Compatibility Matrix and Documentation

**User Story:** As a QA engineer and as competition judges, I want a clear, published list of tested devices, Android versions, and known limitations, so that I know what to expect and can plan accordingly.

#### Acceptance Criteria

1. A new file `.kiro/device-compat-matrix.md` SHALL list all tested devices with the following columns:
   - Device model (e.g., Pixel 5, Samsung Galaxy S21, OnePlus 9)
   - Android version (e.g., 11, 12, 13, 14)
   - Camera capabilities (zoom range, gyroscope, sensor type)
   - Scanner test result (✓ Pass, ⚠ Partial, ✗ Fail)
   - Known issues (if any)
   - Tested date
2. THE matrix SHALL include devices from at least 5 manufacturers (Samsung, Google, OnePlus, Xiaomi, Motorola) covering Android 11–14.
3. FOR each device, THE following features SHALL be tested:
   - Camera preview at 0.5x, 0.8x, 1.0x zoom
   - Frame capture (12 frames, <2min total time)
   - Stitching (< 10 seconds on device, < 30 seconds acceptable)
   - Gyroscope fallback (if device has no gyro)
   - App stability (no crashes, no ANR warnings)
4. WHEN a new device is tested, THE matrix SHALL be updated within 24 hours.
5. THE matrix SHALL be published on the repo README or a dedicated document, and shared with competition organizers before Rome.

---

### Requirement 18: Animation and Transition Polish

**User Story:** As a user, I want smooth, thoughtful animations between screens and UI states, so that the app feels polished and modern (industry-grade).

#### Acceptance Criteria

1. WHEN transitioning from the Scanner preview to the Panorama Viewer, THE transition SHALL be a **smooth fade-and-scale animation** (250ms duration, spring easing) that makes the panorama appear to expand from the preview.
2. WHEN transitioning from one screen to another (e.g., Scanner → Listing Creation), THE navigation SHALL include a **slide-in animation** (200ms, entrance from bottom or side depending on navigation flow).
3. WHEN the user interacts with a button (e.g., "Capture" button), THE button SHALL display a **ripple effect** or **scale animation** (brief 100ms scale to 0.95, then back to 1.0) to confirm the tap.
4. WHEN the Guidance Ring moves or changes color during capture, THE animation SHALL be **smooth and continuous** (0ms to 300ms, depending on magnitude of change), not instant.
5. WHEN the app displays a loading state (stitching, uploading), THE progress indicator SHALL animate smoothly, not freeze or stutter.
6. WHEN an error message is dismissed or a success message fades out, THE animation SHALL be **gentle and non-jarring** (fade-out over 300ms).
7. THE app SHALL use Jetpack Compose's **Animation API** or **GSAP-equivalent** Kotlin coroutines animations (no third-party animation libraries unless already integrated).

---

### Requirement 19: Haptic Feedback Consistency and Completeness

**User Story:** As a user, I want consistent haptic feedback (vibrations) for key interactions (capture, alignment, errors), so that I receive tactile confirmation without looking at the screen.

#### Acceptance Criteria

1. WHEN the Guidance Ring detects proper frame overlap and is ready for capture, THE app SHALL emit a **haptic pulse** (medium strength, 100ms, VibrationEffect.EFFECT_HEAVY_CLICK on Android 12+, Vibrator.vibrate(100) on earlier versions).
2. WHEN the user successfully captures a frame, THE app SHALL emit a **success pattern**: 50ms vibration + 100ms pause + 50ms vibration (dual pulse).
3. WHEN the user taps the "Capture" button, THE app SHALL emit a **light haptic feedback** (30ms pulse, VibrationEffect.EFFECT_LIGHT_CLICK).
4. WHEN an error occurs (camera failure, stitching failure), THE app SHALL emit a **warning pattern**: three short pulses (50ms each, 50ms gaps).
5. WHEN the user is in a low-battery state or the device has haptic feedback disabled (via Settings), THE app SHALL NOT crash or display an error; haptics simply won't fire.
6. WHEN the app emits a haptic, THE effect SHALL be **brief and subtle** (not obnoxious or distracting).

---

### Requirement 20: Error State and Recovery Flows — Comprehensive Coverage

**User Story:** As a user, I want clear, actionable error messages and recovery paths for any failure (camera, stitching, permissions, network), so that I'm never stuck.

#### Acceptance Criteria

1. FOR each of the following error scenarios, THE app SHALL display a **user-friendly error screen** (not a system dialog or crash):
   - Camera permission denied
   - Camera hardware unavailable
   - Camera already in use
   - Stitching failure (OOM, corrupted frames)
   - File I/O failure (disk full, write permission denied)
   - Network timeout (uploading panorama)
   - Device low on memory (< 100MB free)
2. EACH error screen SHALL display:
   - **Title**: Clear, concise description (e.g., "Camera Not Available")
   - **Body**: Brief explanation of what happened and why
   - **Action buttons**: 1–2 buttons (e.g., "Grant Permission", "Retry", "Go Back")
3. WHEN the user taps an action button, THE app SHALL attempt the corrective action:
   - "Grant Permission" → Opens Settings or permission dialog
   - "Retry" → Retries the failed operation (e.g., rebind camera, re-stitch frames)
   - "Go Back" → Returns to previous screen and clears error state
4. WHEN a corrective action succeeds, THE app SHALL return to normal operation without displaying any further messages.
5. WHEN a corrective action fails again, THE app SHALL display a secondary error message with additional detail and an option to contact support or view logs.

---

### Requirement 21: Accessibility Review (WCAG 2.1 Level AA, Mobile Focus)

**User Story:** As an accessibility-conscious user or as a competition judge, I want the app to follow accessibility standards so that it's usable by everyone, including those with visual or motor impairments.

#### Acceptance Criteria

1. ALL interactive elements (buttons, sliders, checkboxes) SHALL have a **minimum touch target size of 48×48 dp** (per Material Design and WCAG guidelines).
2. ALL text SHALL have a **minimum font size of 14sp** (body text; headers may be larger).
3. ALL text and background colors SHALL meet **4.5:1 contrast ratio** (WCAG AA level) for normal text and **3:1** for large text (18sp+ bold or 24sp+).
4. THE app SHALL support **text scaling** (user can increase text size in Settings via Android system settings without breaking layout).
5. WHEN the user enables **TalkBack** (Android screen reader), THE following SHALL be true:
   - All UI elements SHALL have descriptive **content descriptions** (e.g., button "Capture frame" not just "Button")
   - Form fields SHALL be labeled (e.g., "Zoom slider, current value 1.0x, range 0.5x to 4.0x")
   - Errors and status updates SHALL be announced
6. THE app SHALL support **high contrast mode** (user enables high contrast in Accessibility settings; app colors adjust for better visibility).
7. THE app SHALL support **reduced motion** (user enables this in Settings; animations are disabled or simplified).
8. ALL guidance text in the Scanner SHALL be available in **Bangla and English**, with clear visual indicators of which language is active.

---

### Requirement 22: Localization — Bangla, English, and European Languages

**User Story:** As a user in Bangladesh or Europe, I want the app fully translated and culturally adapted to my language and region, so that I can use it naturally and trust the content.

#### Acceptance Criteria

1. THE app SHALL support the following **languages**:
   - Bangla (bn-BD, fully translated and proofread)
   - English (en-US, en-GB)
   - For European markets: German (de), French (fr), Polish (pl), Turkish (tr) — deferred to Phase 3 (Stage 3) unless Rome requires it
2. WHEN the user opens Settings, THEY SHALL be able to select their preferred language, and THE app SHALL immediately **re-render all UI in that language** (including the Scanner, Listing Creation, Chat, Handover Passport).
3. ALL text strings (UI labels, error messages, guidance text, currency symbols) SHALL be stored in **strings.xml** resource files (one per language), not hardcoded.
4. WHEN the app detects the device's system language (e.g., Bangla in Bangladesh), IT SHALL default to that language on first launch.
5. THE Handover Passport, Chat, and all transactional content SHALL be **available in the user's chosen language**.
6. CURRENCY SHALL be displayed according to locale:
   - Bangladesh: Taka (৳) for prices, formatted as ৳ 10,00,000 (lakh format)
   - Europe: EUR (€) for prices, formatted per locale (e.g., 1.000.000 € in Germany)
7. DATE AND TIME FORMAT SHALL respect locale (e.g., DD/MM/YYYY in Bangladesh and Europe, MM/DD/YYYY in US).
8. ALL guidance text and error messages in the Scanner SHALL be **clear and unambiguous in each language** (user testing with native speakers in each market).

---

### Requirement 23: Rome Submission Readiness Checklist

**User Story:** As a project manager, I want a clear, verifiable checklist of all pre-submission tasks so that nothing is missed before Rome.

#### Acceptance Criteria

1. A file `.kiro/rome-submission-checklist.md` SHALL exist and include the following sections:
   - **Code Quality**: All requirements implemented, no TODOs left, no crashes in manual QA
   - **Testing**: Unit tests pass, integration tests pass on Firebase Test Lab, no regressions
   - **Device Compatibility**: Matrix populated, devices tested, known issues documented
   - **Localization**: Bangla and English translations complete and proofread, no missing strings
   - **Performance**: APK size < 120MB, cold start < 3 seconds, stitching < 10 seconds on target devices
   - **Documentation**: README updated, setup instructions clear, release notes prepared
   - **Security**: No hardcoded secrets, Firebase security rules set, data encryption enabled
   - **Accessibility**: Contrast, touch targets, text scaling, TalkBack tested
   - **APK Building**: Debug and release APKs generated, signed correctly, uploaded to GitHub Releases
2. BEFORE submitting to Rome, THE project manager SHALL verify that **all checklist items are ✓ Complete**.
3. IF any item is incomplete, THE project manager SHALL escalate to the relevant team and set a new deadline.

---

### Requirement 24: Final QA and Performance Profiling

**User Story:** As a QA lead, I want a comprehensive final QA pass and performance profiling to ensure the app meets competition standards.

#### Acceptance Criteria

1. A **Final QA Test Plan** SHALL be executed on at least 3 representative devices (e.g., Pixel 5, Samsung S21, OnePlus 9) covering:
   - App stability (no crashes for 30 min of continuous use)
   - All core features (scan, explore, chat, handover, calculator)
   - Network resilience (scan upload with intermittent network)
   - Edge cases (device rotation, app background/foreground transitions, low battery)
2. DURING profiling, THE following metrics SHALL be measured:
   - **APK size**: Target < 120MB (debug), < 100MB (release)
   - **Cold start time**: Target < 3 seconds
   - **Stitching time**: Target < 10 seconds (on Pixel 5-class device)
   - **Memory usage**: Target < 500MB during scan, < 1GB peak
   - **Battery drain**: Monitor during 1-hour continuous scan session
   - **Frame rate**: 60 FPS (or 90 FPS on high-refresh devices) during preview
3. IF any metric exceeds target, THE team SHALL investigate and optimize before Rome.
4. A **Performance Report** SHALL be generated, documenting baseline metrics and any bottlenecks discovered.

---

### Requirement 25: Privacy Audit and Security Hardening

**User Story:** As a privacy-conscious user and as competition judges, I want assurance that my data is secure, encrypted, and handled according to privacy standards.

#### Acceptance Criteria

1. A **Privacy & Security Audit** SHALL be conducted covering:
   - **Data at rest**: Verify all sensitive data (panoramas, documents, chat messages) is encrypted using Android KeyStore
   - **Data in transit**: Verify all network calls use TLS 1.2+, certificate pinning recommended
   - **Permissions**: Verify all Android permissions are requested only when needed (no overly-broad permissions)
   - **Crash logs**: Verify crash logs do not contain personally identifiable information
   - **Third-party dependencies**: Verify no malicious or compromised libraries in dependency tree
2. WHEN a user deletes account or property, THE app SHALL delete all associated data (panoramas, documents, messages) from local storage and request remote deletion from backend (if backend exists).
3. THE app SHALL display a **Privacy Policy** screen in Settings, clearly stating what data is collected, how it's used, and how long it's retained.
4. IF the app stores data on-device only (no remote backend), THIS SHALL be clearly stated in the Privacy Policy ("All your data stays on your phone").
5. A **Security Report** SHALL be generated, documenting all findings and remediation steps taken.

---

## Non-Functional Requirements

### Performance

1. **Scanner Preview**: Live preview shall run at 60 FPS (30 FPS acceptable on budget devices) with <100ms latency between sensor input and visual feedback.
2. **Frame Capture**: Capturing a single frame shall complete in < 200ms.
3. **Stitching**: 12-frame stitching shall complete in < 10 seconds on Pixel 5-class devices, < 30 seconds on budget devices.
4. **Cold Start**: App launch from home screen to Scanner preview ready shall take < 3 seconds.
5. **Memory**: App shall not exceed 500MB RAM during scan, 1GB peak.
6. **APK Size**: Release APK shall be < 100MB.

### Reliability

1. **Uptime**: App shall not crash during normal use (target: 99%+ crash-free sessions).
2. **Error Recovery**: All error states shall have recovery paths (no dead ends).
3. **Sensor Fallback**: If gyroscope unavailable, app shall fall back to accel+mag without user intervention.

### Usability

1. **Discoverability**: All major features (scan, explore, chat, handover) shall be accessible within 2 taps from main screen.
2. **Guidance**: User guidance for scanner shall be clear, visual, and not require reading a manual.
3. **Feedback**: All user actions shall receive visual or haptic feedback within 200ms.

### Security

1. **Data Encryption**: All sensitive data (panoramas, documents, messages) shall be encrypted at rest using Android KeyStore.
2. **Network Security**: All network calls shall use TLS 1.2+ with certificate validation.
3. **Permission Minimization**: Only request permissions when feature is accessed, not at startup.

### Accessibility

1. **Touch Targets**: All interactive elements shall have minimum 48×48 dp size.
2. **Text Contrast**: All text shall have 4.5:1 contrast ratio (WCAG AA level).
3. **Screen Reader**: All UI elements shall have descriptive labels for TalkBack.

### Maintainability

1. **Code Documentation**: All scanner and stitching functions shall have clear comments explaining algorithm and parameters.
2. **Error Logging**: All errors shall be logged with context (device info, Android version, camera capabilities).
3. **Version Control**: All changes shall be committed to git with clear commit messages.

---

## Dependencies and Assumptions

### Dependencies
- **CameraX** (androidx.camera): Stable, maintained by Android team
- **Jetpack Compose**: Stable for Material 3 UI
- **Room**: SQLite abstraction, maintained by Android team
- **Kotlin Coroutines**: Async/threading abstraction
- **ZXing Core**: QR code generation and parsing
- **Coil**: Image loading and caching
- **Firebase AppCheck** (optional): Security measure against bot traffic
- **Cactus SDK** (optional, commented out): AI assistant (see Requirement 7 for restore path)

### Assumptions
1. Target devices have Android 11+ (minSdk = 30); Android 7–10 support is removed.
2. All target devices have a camera; app requires camera hardware (cannot run on non-camera devices).
3. All target devices have a SIM card or are on WiFi; app requires internet for some features (upload, verification).
4. Judges will test on actual physical devices, not emulators, during competition.
5. App source code and APK will be provided to judges before Rome; no remote API backend is required for core features (scan, stitching, local handover).

---

## Glossary (Extended)

- **Equirectangular**: A map projection where a sphere is unwrapped into a 2D image at 2:1 aspect ratio; used for 360-degree panoramas.
- **Stitching**: The algorithmic process of aligning and blending overlapping images into a seamless panorama.
- **Gyroscope Calibration**: The initial alignment of gyroscope to device orientation; usually automatic but may require holding device steady.
- **Frame Overlap**: The percentage of pixels shared between two consecutive frames; typically 20–40% for good stitching results.
- **CameraX**: Android Jetpack library providing a unified API for camera access across devices.
- **Room Database**: SQLite abstraction layer for local storage.
- **Material Design 3**: Latest Google design system with dynamic color and accessibility features.
- **Firebase Test Lab**: Google Cloud service for running instrumentation tests on real devices.
- **APK (Android Package Kit)**: Compiled Android app binary; similar to .exe on Windows or .dmg on Mac.
- **Proguard**: Code obfuscation and optimization tool for Android.

---

## Success Metrics

1. **Scanner works smoothly on Android 11+ devices**: ✓ Pass if 5+ devices tested, no critical issues
2. **All features functionally complete**: ✓ Pass if all requirements have implementation code and pass unit/integration tests
3. **Comprehensive testing**: ✓ Pass if unit test coverage >= 80%, integration tests pass on Firebase Test Lab
4. **Device compatibility matrix published**: ✓ Pass if matrix includes >= 5 manufacturers, all columns populated
5. **Rome submission approved**: ✓ Pass if judges approve app at competition and report no blockers

---

## Document Metadata

- **Author**: Kiro (Specification Agent)
- **Date Created**: 2025-01-XX
- **Last Updated**: 2025-01-XX
- **Spec Type**: Feature (Requirements-First Workflow)
- **Feature Name**: dorja-rome-readiness
- **Target Deadline**: Rome FIRSO 2026 (April 2026)
- **Priority**: Critical (Competition submission)
