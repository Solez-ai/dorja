# Panorama Sweep Redesign Progress

## Overview
Complete redesign of Dorja scanner from 12-frame circular panorama to Samsung/Apple-style horizontal sweep panorama.

## Completed (Phase 1 - Core Infrastructure)

### ✅ Architecture & Design
- **Requirements Document**: `.kiro/specs/panorama-sweep-redesign/requirements.md`
  - Detailed technical specifications
  - Samsung/Apple panorama analysis
  - Component breakdown and integration plan

### ✅ Core Scanner Components (Committed: 0caea1a)
1. **SweepConfig.kt**
   - Configurable sweep range (90-180°)
   - Direction support (LEFT_TO_RIGHT, RIGHT_TO_LEFT)
   - Strip interval configuration (2-5° per strip)
   - Progress calculation based on heading

2. **MotionAnalyzer.kt**
   - Real-time rotation velocity detection
   - 50-sample circular buffer for heading history
   - Motion state classification (SMOOTH, ERRATIC, STATIONARY, TOO_FAST)
   - Optimal velocity range: 10-25°/second

3. **SweepCaptureState.kt**
   - New state machine replacing 12-frame system
   - States: READY_TO_START → SWEEPING_IN_PROGRESS → PAUSED → SWEEP_COMPLETE → STITCHING → COMPLETED
   - ERROR state with recovery

4. **ContinuousCaptureManager.kt**
   - ImageAnalysis-based frame processing (30fps)
   - YUV_420_888 to RGB conversion
   - Vertical strip extraction (40px width)
   - Automatic strip capture on rotation threshold

### ✅ UI Components (Committed: ef66042)
1. **SweepGuidance.kt**
   - Samsung/Apple-style horizontal guidance
   - Directional arrows (← →)
   - Animated progress bar
   - Velocity feedback ("Too slow", "Keep moving", "Too fast")
   - Preview strip indicator showing accumulated captures

2. **ScannerViewModel.kt**
   - Added sweep state management
   - Tracks: state, progress, direction, velocity, strips captured
   - Legacy state preserved for backward compatibility
   - Error handling with messages

3. **RoomScannerScreen.kt**
   - Replaced ARGuidanceRing with SweepGuidance
   - Integrated MotionAnalyzer for real-time velocity detection
   - Wired ContinuousCaptureManager for strip extraction
   - Automatic state transitions:
     - Detects sweep start (velocity > 5°/s)
     - Pauses on erratic motion
     - Completes at 100% progress
     - Triggers stitching phase

4. **CameraManager.kt**
   - Added `bindCameraWithAnalysis()` method
   - Supports ImageAnalysis use case for continuous frame processing
   - Required for strip extraction during sweep

## In Progress (Phase 2 - Stitching & Polish)

### 🔄 Stitching Engine (Next Task)
- [ ] **IncrementalStitchingEngine.kt**
  - Real-time panorama preview during sweep
  - Blend overlapping strips with feathering
  - Canvas-based composition
  - Seam optimization

- [ ] **StitchingQualityAnalyzer.kt**
  - Detect misalignment between strips
  - Calculate seam quality scores
  - Provide user feedback on problem areas

### 🔄 Integration & Testing
- [ ] Wire stitching engine into ContinuousCaptureManager
- [ ] Add real-time panorama preview to UI
- [ ] Test on device via GitHub Actions CI build
- [ ] Handle edge cases:
  - User reverses direction mid-sweep
  - Phone tilted too much (vertical alignment)
  - Low light conditions
  - Fast panning causing motion blur

### 🔄 Cleanup
- [ ] Remove legacy 12-frame code:
  - `CaptureState.kt` old states
  - `ScanGeometry.kt` 12-target logic
  - `ARGuidanceRing.kt` (replaced by SweepGuidance)
  - `FramePreviewRail.kt` (discrete frames)
  - `CaptureButton.kt` (manual capture)
- [ ] Update database schema for panorama strips
- [ ] Update evidence repository to handle strip-based data

## Technical Highlights

### Motion Detection
```kotlin
// MotionAnalyzer tracks rotation velocity in real-time
val analysis = motionAnalyzer.analyzeMotion(orientation)
when (analysis.motionState) {
    MotionState.SMOOTH -> "Keep moving smoothly" // 10-25°/s
    MotionState.TOO_FAST -> "Slow down" // >30°/s
    MotionState.STATIONARY -> "Start panning" // <5°/s
}
```

### Strip Capture
```kotlin
// Automatic strip extraction every 2-5° of rotation
val strip = extractStripFromYUV(
    yuvImage = image,
    stripX = calculateStripPosition(currentHeading),
    stripWidth = 40 // pixels
)
```

### Progress Tracking
```kotlin
val progress = sweepConfig.calculateProgress(currentHeading)
// 0.0 = start, 1.0 = complete (120° sweep)
```

## CI/CD Status
- ✅ GitHub Actions build triggered
- 🔄 Waiting for APK build on main branch
- 📦 Debug APK will be available at: https://github.com/Solez-ai/dorja/releases/tag/latest

## Next Steps
1. Wait for CI build to complete (check Actions tab)
2. Test current UI on device with new APK
3. Implement IncrementalStitchingEngine for real-time preview
4. Wire stitching into capture pipeline
5. Test complete sweep → stitching → save flow
6. Remove old circular scanning code
7. Polish UI animations and feedback

## Commits
- `0caea1a`: Add core panorama sweep infrastructure
- `ef66042`: Integrate sweep panorama UI and state management

## Files Changed
**Created:**
- `dorja/app/src/main/java/com/example/scanner/SweepConfig.kt`
- `dorja/app/src/main/java/com/example/scanner/MotionAnalyzer.kt`
- `dorja/app/src/main/java/com/example/scanner/SweepCaptureState.kt`
- `dorja/app/src/main/java/com/example/scanner/ContinuousCaptureManager.kt`
- `dorja/app/src/main/java/com/example/ui/scanner/SweepGuidance.kt`
- `.kiro/specs/panorama-sweep-redesign/requirements.md`

**Modified:**
- `dorja/app/src/main/java/com/example/scanner/CameraManager.kt` (added ImageAnalysis support)
- `dorja/app/src/main/java/com/example/ui/scanner/ScannerViewModel.kt` (sweep state management)
- `dorja/app/src/main/java/com/example/ui/scanner/RoomScannerScreen.kt` (new UI integration)

## Testing Notes
- **Do NOT compile locally** (per AGENTS.md rule)
- All builds happen through GitHub Actions
- Download APK from releases after CI completes
- Test on physical device for motion tracking accuracy
