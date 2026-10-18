# Technical Design Document: Dorja Rome Readiness (FIRSO 2026)

## Overview

Dorja Homestation requires a comprehensive technical redesign to meet FIRSO 2026 competition standards. This design document specifies the scanner module overhaul (preview rendering, AR guidance, frame capture), feature completion (AI assistant, financial calculator, handover passport, chat, relocation mode), device compatibility hardening (Android 11+ minimum, sensor fusion fallback, responsive layouts), and testing/accessibility requirements.

**Key Success Criteria:**
- Scanner preview at natural (unzoomed) scale on all devices
- AR guidance ring with responsive haptic feedback
- 12-frame capture and stitching within 60–90 seconds
- Zero critical crashes on device matrix (5+ manufacturers)
- WCAG 2.1 AA accessibility compliance
- Seamless operation with/without optional AI dependency

**Scope:**
- Scanner UX/UI redesign and performance hardening
- Feature completion (AI, calculator, passport, chat, relocation)
- Device compatibility matrix (Android 11–14, multiple OEMs)
- Testing strategy (unit, integration, device tests)
- Accessibility and localization

---

## Architecture

### System Layering

```
┌─────────────────────────────────────────────────────────────┐
│                   Dorja App (Android 11+)                    │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────┐    │
│  │  UI Layer (Jetpack Compose)                          │    │
│  │  ├─ ScannerScreen                                    │    │
│  │  ├─ ListingScreen                                    │    │
│  │  ├─ CalculatorScreen                                │    │
│  │  ├─ ChatScreen                                      │    │
│  │  └─ HandoverPassportScreen                          │    │
│  └─────────────────────────────────────────────────────┘    │
│                          ↓↑                                   │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  ViewModel Layer (MVVM State Management)             │    │
│  │  ├─ ScannerViewModel (session state, capture)        │    │
│  │  ├─ ListingViewModel                                │    │
│  │  ├─ CalculatorViewModel (real-time compute)         │    │
│  │  ├─ PassportViewModel (promise tracking)            │    │
│  │  └─ ChatViewModel                                   │    │
│  └─────────────────────────────────────────────────────┘    │
│                          ↓↑                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Domain / Business Logic Layer                       │   │
│  │  ├─ ScanGeometry (rotation, overlap calc)            │   │
│  │  ├─ StitchingEngine (frame blending)                │   │
│  │  ├─ CameraManager (zoom detection, binding)         │   │
│  │  ├─ SensorFusion (gyro+accel+mag pipeline)          │   │
│  │  ├─ FinancialCalculator (real-time amort)           │   │
│  │  ├─ PassportManager (promise CRUD, vault)           │   │
│  │  └─ ChatService (message persistence)               │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓↑                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Data Layer (Repository + Room Database)             │   │
│  │  ├─ DorjaRepository (all CRUD)                       │   │
│  │  ├─ RoomItemDao (properties, listings)               │   │
│  │  ├─ PassportDao (promises, docs, signatures)         │   │
│  │  ├─ CalculationDao (history)                        │   │
│  │  ├─ ChatDao (messages, attachments)                  │   │
│  │  └─ Encrypted Storage (docs, sensitive data)         │   │
│  └──────────────────────────────────────────────────────┘   │
│                          ↓↑                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Infrastructure / Services                           │   │
│  │  ├─ CameraX (preview, capture, focus)               │   │
│  │  ├─ SensorManager (gyro, accel, mag)                │   │
│  │  ├─ Vibrator API (haptic feedback)                  │   │
│  │  ├─ Background Executor (stitching, crypto)          │   │
│  │  └─ File I/O (frame cache, panorama export)          │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Threading Model

- **Main Thread (UI)**: State updates, preview rendering, animations, haptics
- **Background Threads (Dispatchers.Default/IO)**: Frame capture, stitching, sensor fusion, DB queries, encryption
- **Sensor Thread (SensorManager.SENSOR_DELAY_GAME)**: Gyro/accel reads (~20ms ticks), posted to main for smoothing

### Component Hierarchy (Scanner)

```
RoomScannerScreen
├── CameraPreviewPane (AndroidView wrapping PreviewView)
├── ARGuidanceOverlay (Canvas-based guidance ring)
│   ├── GuidanceRing (animated, RED/YELLOW/GREEN)
│   └── StabilityIndicator (progress bar, text labels)
├── FramePreviewRail (horizontal LazyRow carousel)
│   ├── FrameThumbnail x 12 (tap-to-retake)
│   └── AddFrameButton
├── CaptureControls (bottom panel)
│   ├── ZoomSlider (0.5x–4.0x per device)
│   ├── CaptureButton (80dp, shutter feedback)
│   ├── NextShotButton (manual nav)
│   └── ErrorRecoveryUI (fallback)
└── ProgressIndicator (stitching: 0%–100%)
```

### ViewModel Architecture

```kotlin
class ScannerViewModel(private val repo: DorjaRepository) : ViewModel() {
    private val _captureState = MutableStateFlow(CaptureState.WAITING_FOR_INITIAL_ANCHOR)
    private val _scanTargets = MutableStateFlow<List<ScanGeometry.ScanTarget>>(emptyList())
    private val _capturedFrames = MutableStateFlow<List<FrameData>>(emptyList())
    private val _stitchingProgress = MutableStateFlow<StitchingProgress?>(null)
    private val _orientation = MutableStateFlow<Orientation>(Orientation())
    private val _cameraCapabilities = MutableStateFlow<CameraCapabilities?>(null)
    private val _alignLevel = MutableStateFlow(AlignLevel.RED)
}
```

---

## Components and Interfaces

### 1. Scanner Module Components

#### CameraPreview
- **Role**: Renders live camera feed at native aspect ratio
- **Interface**: 
  ```kotlin
  @Composable
  fun CameraPreview(
      modifier: Modifier = Modifier,
      onPreviewFrameAvailable: () -> Unit = {}
  )
  ```
- **Key Implementation**: CameraX PreviewView with explicit aspect ratio matching
- **Validation**: Preview displays unzoomed; aspect ratio preserved ±0.02x

#### ARGuidanceRing
- **Role**: Animated target indicator with alignment feedback
- **Dimensions**: 120–180dp diameter (proportional to screen)
- **States**: RED (#E53935), YELLOW (#FFC107), GREEN (#4CAF50)
- **Animation**: Pulse on YELLOW (800ms linear), glow on GREEN (1s fade)
- **Haptics**: Dual pulse on GREEN, triple pulse on deviation
- **Accessibility**: contentDescription announces state to TalkBack

#### FramePreviewRail
- **Role**: Carousel of captured frame thumbnails
- **Thumbnail Size**: 80px minimum height, 2px border, rounded 4dp
- **Interaction**: Tap to highlight, long-press for menu
- **Auto-scroll**: Keeps current frame visible when new frame added
- **Accessibility**: Each thumbnail announces frame number and capture time

#### StitchingProgressIndicator
- **Role**: Display stitching phase (preparing, 25%/50%/75%/100%, validating, caching)
- **Timeout Warnings**: 8s → "Large image…", 30s → "Cancel" CTA
- **Accessibility**: Announces progress updates every 500ms

### 2. Sensor Fusion Components

#### SensorFusionManager
- **Input**: Gyroscope OR (Accelerometer + Magnetometer)
- **Output**: Orientation(headingDeg, pitchDeg, rollDeg)
- **Fallback**: Detects gyro unavailability, switches to accel+mag mode
- **Smoothing**: Low-pass filter (alpha = 0.25) for jitter reduction
- **Validation**: Euler angles always in valid ranges

#### CameraManager
- **Capabilities**: Detects zoom range (minZoom, maxZoom), lens types
- **Default Zoom**: 0.8x if ultra-wide, 1.0x if standard, min if neither
- **Binding**: Handles camera binding/unbinding with error recovery
- **Thread-safe**: All operations wrapped in Dispatchers.Main context

### 3. Stitching Pipeline Components

#### FrameData
```kotlin
data class FrameData(
    val path: String,                 // /cache/frame_r0_c0_xxx.jpg
    val heading: Float,               // Compass heading at capture
    val pitchDeg: Float = 0f,         // Pitch (up/down)
    val row: Int = 0,                 // Ring index (always 0 for flat scans)
    val col: Int = 0,                 // Frame index (0–11)
    val isCap: Boolean = false,       // Ceiling/floor marker
    val capType: String? = null       // "ceiling" or "floor"
)
```

#### StitchingEngine
- **Input**: List<FrameData> (6–12 frames)
- **Output**: Result<String> (panorama file path or error)
- **Phases**: PREPARING → PROCESSING → VALIDATING → CACHING
- **Memory**: Bitmap pooling, frame-by-frame release
- **Timeout**: 8s warning, 30s cancel option, user suggestion fallback
- **Validation**: 2:1 aspect, no NaN, <50MB size

#### ScanGeometry
- **Rotation Matrix Calculation**: Quaternion → 3×3 rotation matrix
- **Frame Overlap Detection**: Symmetric, returns [0, 1]
- **Target Generation**: Even spacing around 360° (6/10/12 shots)
- **Panorama Dimensions**: 4096×2048px standard, downsampled if input > 4K

### 4. Feature Components

#### AIAssistant (Graceful Feature Flag)
- **Detection**: Try-catch for Cactus SDK class load
- **Fallback**: Empty suggestions if unavailable
- **No Crash**: App continues identically with or without AI
- **Setting**: User toggle in Settings to disable even if available

#### FinancialCalculator
- **Computation**: Real-time amortization formula (<100ms latency)
- **Inputs**: Property price, down payment, interest rate, loan term, monthly rent
- **Outputs**: Monthly payment, total interest, ROI%, cash-on-cash return
- **Validation**: Inline errors for negative/invalid values
- **History**: Persisted in Room database, sortable/filterable

#### HandoverPassport
- **Promise CRUD**: Add, update status, mark overdue
- **Document Vault**: Encrypted storage, SHA-256 hash for tampering detection
- **Digital Signature**: Timestamp + party ID, immutable records
- **Export**: PDF generation for legal record-keeping

#### ChatService
- **Message Model**: Rich text (bold, italic, links), media attachments
- **Delivery Status**: Sending → Sent → Seen state machine
- **Notifications**: Sound alert + badge (unless muted in settings)
- **Typing Indicator**: "User is typing…" with <1s latency update

---

## Data Models

### Core Scanner Data

```kotlin
// Capture session state machine
enum class CaptureState {
    WAITING_FOR_INITIAL_ANCHOR,  // Confirm anchor pose
    CAPTURING_INITIAL_FRAME,     // First frame being taken
    MOVING_TO_TARGET,             // Guide to next frame
    STABILIZING,                  // Hold steady, waiting for auto-trigger
    COMPLETED,                    // Last frame captured
    ERROR                          // Sensor/camera failure
}

// Alignment quality (RED/YELLOW/GREEN)
enum class AlignLevel { RED, YELLOW, GREEN }

// Sensor availability
enum class SensorMode { GYRO_PRIMARY, ACCEL_MAG_FALLBACK, UNAVAILABLE }

// Orientation (heading, pitch, roll)
data class Orientation(
    val headingDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f
) {
    fun isValid() = 
        headingDeg in 0f..360f && 
        pitchDeg in -90f..90f && 
        rollDeg in -180f..180f
}

// Scan target (frozen once anchor confirmed)
data class ScanTarget(
    val targetIndex: Int,
    val ringIndex: Int = 0,
    val pitchDeg: Float = 0f,
    val headingDeg: Float
)

// Camera capabilities
data class ZoomCapabilities(
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val defaultZoom: Float,
    val hasUltraWide: Boolean,
    val hasStandard: Boolean,
    val hasTelephoto: Boolean
)

// Stitching progress
sealed class StitchingPhase {
    object Preparing : StitchingPhase()
    data class Processing(val percentComplete: Int) : StitchingPhase()
    object Validating : StitchingPhase()
    object Caching : StitchingPhase()
    data class Success(val panoramaPath: String) : StitchingPhase()
    data class Error(val message: String) : StitchingPhase()
}
```

### Feature Data Models

```kotlin
// Handover Passport
@Entity(tableName = "handover_passports")
data class HandoverPassport(
    @PrimaryKey val passportId: String,
    val propertyId: String,
    val sellerId: String,
    val buyerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Promise tracking
@Entity(tableName = "promises", indices = [Index("passportId")])
data class Promise(
    @PrimaryKey val promiseId: String,
    val passportId: String,
    val description: String,
    val targetDate: Long,
    val status: PromiseStatus = PromiseStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PromiseStatus { PENDING, IN_PROGRESS, COMPLETED, OVERDUE, FAILED }

// Encrypted document
@Entity(tableName = "vault_documents", indices = [Index("passportId")])
data class VaultDocument(
    @PrimaryKey val docId: String,
    val passportId: String,
    val encryptedBytes: ByteArray,
    val sha256Hash: String,
    val uploadedBy: String,  // seller or buyer
    val uploadedAt: Long = System.currentTimeMillis()
)

// Financial calculation
@Entity(tableName = "calculations")
data class FinancialCalculation(
    @PrimaryKey val calcId: String,
    val propertyPrice: Double,
    val downPayment: Double,
    val interestRate: Double,
    val loanTermYears: Int,
    val monthlyRent: Double,
    val monthlyPayment: Double,
    val totalInterest: Double,
    val roiPercent: Double,
    val cashOnCashReturn: Double,
    val createdAt: Long = System.currentTimeMillis()
)

// Chat message
@Entity(tableName = "chat_messages", indices = [Index("threadId")])
data class ChatMessage(
    @PrimaryKey val messageId: String,
    val threadId: String,
    val senderId: String,
    val content: String,
    val richTextFormat: String? = null,  // Markdown or HTML
    val mediaAttachments: List<String> = emptyList(),  // File IDs
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val seenAt: Long? = null
)

enum class DeliveryStatus { SENDING, SENT, SEEN, FAILED }
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Preview Aspect Ratio Preservation
**For any** device screen size and camera sensor aspect ratio, the preview SHALL display at the sensor's native aspect ratio without pillarboxing, letterboxing, or stretching.
**Validates: Requirements 1.1, 1.5**

### Property 2: Zoom Slider Constraints
**For any** device with detected zoom capabilities (minZoom, maxZoom), the zoom slider SHALL only allow values within the detected range, and actual camera zoom SHALL match the selected value (±0.02x).
**Validates: Requirements 4.1, 4.2**

### Property 3: Frame Overlap Calculation
**For any** pair of frame headings (h1, h2) and rotation matrices, calculated overlap SHALL be symmetric: overlap(h1, h2) ≡ overlap(h2, h1), and ∈ [0, 1].
**Validates: Requirement 3.2**

### Property 4: Stitching Output Validation
**For any** valid set of 6–12 frames with heading deltas ≤ 30°, stitched panorama SHALL have 2:1 aspect ratio (width/height ∈ [1.99, 2.01]), no NaN pixels, file size < 50MB.
**Validates: Requirement 3.5**

### Property 5: Sensor Fusion Consistency
**For any** valid gyroscope reading (when available), heading via gyro+accel fusion SHALL match heading via gyro-only, within 2° tolerance.
**Validates: Requirement 5.2**

### Property 6: Sensor Fallback Correctness
**For any** valid accel+mag pair (gyro unavailable), orientation calculation SHALL produce valid Euler angles: heading ∈ [0, 360), pitch ∈ [-90, 90], roll ∈ [-180, 180].
**Validates: Requirement 5.3**

### Property 7: Target Generation Invariant
**For any** valid anchor heading and shot count (6, 10, 12), generated target list SHALL have exactly shot_count elements with evenly-spaced headings (360 / shot_count degrees apart), targets immutable throughout session.
**Validates: Requirements 2.1, 6**

### Property 8: Frame Persistence & Recovery
**For any** capture session interrupted by app restart, previously captured frames and session state SHALL be recoverable (resumed or discarded), zero data loss.
**Validates: Requirement 7.2**

### Property 9: AI Feature Graceful Disable
**For any** Cactus SDK availability (present or absent), Listing Creation flow SHALL complete without crashes, displaying AI suggestions if available or empty if unavailable, identical functionality.
**Validates: Requirements 7.3, 7.6**

### Property 10: Financial Calculator Correctness
**For any** valid property details (price, down payment, interest rate, loan term), calculated monthly payment SHALL match standard amortization formula: P * [r(1+r)^n] / [(1+r)^n - 1].
**Validates: Requirement 8.2**

### Property 11: Handover Passport Immutability
**For any** promise or document added to Handover Passport, record SHALL be immutable (buyer cannot edit/delete), document hash (SHA-256) SHALL verify tampering, timestamp accurate (±1s).
**Validates: Requirements 9.4, 9.5, 9.8**

### Property 12: Chat Message Delivery Status
**For any** sent message, delivery status sequence SHALL follow exactly: Sending → Sent → Seen (or Sending → Error), never skipping/reversing, status reflects actual server acknowledgment.
**Validates: Requirement 10.8**

### Property 13: Responsive Layout Stability
**For any** orientation change (portrait ↔ landscape) or screen size transition (foldable folded ↔ unfolded), scanner UI SHALL re-layout within 300ms, no data loss.
**Validates: Requirement 6.6**

### Property 14: Permission Request Timing
**For any** permission requirement (camera, location), app SHALL request at runtime (never manifest-only on Android 11+), handle denial gracefully without crash.
**Validates: Requirement 12.5**

### Property 15: Error Message Consistency
**For any** error state (camera binding failure, stitching timeout), user-facing error message SHALL match predefined verbatim text (no auto-generated jargon), include recovery CTA.
**Validates: Requirement 14.2**

---

## Error Handling

### Camera Binding Errors

**State Machine:**
```
[Binding] → [Success] → [BOUND]
   ↓
[Error: Type] → [Display Error UI] → [Retry / Settings / Back]
```

**Error Types & Recovery:**

| Error | Message | Action |
|-------|---------|--------|
| In Use | "Camera in use by another app. Close it and try again." | Retry button |
| Permission Denied | "Camera permission required. Go to Settings > Apps > Dorja." | Settings button |
| Hardware Unavailable | "Device has no camera." | Back button |
| Unknown | "Camera error. Try restarting the app." | Retry button |

### Stitching Timeouts

**Progressive Feedback:**

| Time | Action |
|------|--------|
| 0–8s | Progress indicator (25%, 50%, 75%) |
| 8s passed | Show warning: "Large image; this may take a minute…" |
| 30s passed | Show cancel CTA: "Try recapturing with 8 frames instead of 12" |
| Error | Display: "Failed to stitch frames. Retry or Capture Again." |

### Graceful Degradation

- **Memory Low**: Offer to reduce frame count (12 → 8)
- **Camera Disconnected**: Attempt rebind within 1s, fallback to ERROR state
- **AI Unavailable**: Continue without suggestions (no crash)
- **Sensor Unavailable**: Display ERROR state with device limitation message

---

## Testing Strategy

### Unit Tests (ScanGeometry)

**File:** `app/src/test/java/com/example/scanner/ScanGeometryTest.kt`

**Test Suite:**
- Rotation matrix calculations (property-based, 1000+ random quaternions)
- Frame overlap detection (symmetric validation, [0,1] range)
- Panorama dimension calculation (2:1 aspect ratio, power-of-2 sizes)
- Edge cases (gimbal lock at 0°/180°, 90° pitch)
- Euler angle validation (heading ∈ [0,360), pitch ∈ [-90,90])

**Coverage Goal:** ≥ 80% of ScanGeometry and stitching logic

### Integration Tests (Scanner Flow)

**File:** `app/src/androidTest/java/com/example/scanner/ScannerFlowTest.kt`

**Test Scenarios:**
- Complete scanner flow end-to-end (12 frames → stitch → export)
- Camera binding error recovery
- Stitching timeout behavior (8s warning, 30s cancel)
- Permission handling (granted, denied, revoked)
- Orientation change (no data loss, re-layout < 300ms)

**Platform:** Firebase Test Lab (Pixel 5, Samsung S21, OnePlus 9, Xiaomi Mi 11, Motorola G20)

**Target:** < 5 min per test, pass on all reference devices

### Device Compatibility Matrix

**Tested Devices:**

| Device | Android | Zoom | Gyro | Status |
|--------|---------|------|------|--------|
| Pixel 5 | 14 | 0.5x–4.0x | ✓ | ✅ Pass |
| Samsung S21 | 13 | 0.5x–3.0x | ✓ | ✅ Pass |
| OnePlus 9 | 12 | 1.0x–2.0x | ✓ | ✅ Pass |
| Xiaomi Mi 11 | 12 | 0.8x–5.0x | ✗ | ⚠️ Accel+mag |
| Motorola G20 | 11 | 1.0x–2.0x | ✗ | ⚠️ Accel+mag |

**Minimum Requirements:**
- 5+ manufacturers
- Android 11–14 coverage
- Both gyro and accel+mag fallback tested
- Frame capture, stitching, export verified

---

## Success Metrics

1. ✅ Preview renders unzoomed, natural aspect ratio
2. ✅ AR guidance ring animates smoothly, haptics responsive
3. ✅ 12 frames capture within 60 seconds
4. ✅ Stitching completes within 30 seconds (Pixel 5)
5. ✅ Panorama exports as 4096×2048 JPEG < 50MB
6. ✅ Zero critical crashes on device matrix
7. ✅ All features (calculator, passport, chat, relocation) functional without AI
8. ✅ WCAG 2.1 AA compliance verified
9. ✅ Device compatibility matrix complete
10. ✅ Full test coverage (unit, integration, device tests)

