# Panorama Sweep Redesign - Requirements

## Overview

Redesign the Dorja room scanner from a 12-frame circular capture system to a Samsung/Apple-style horizontal sweep panorama.

## Current System (To Replace)

**Architecture:**
- 12 discrete frames captured at 30° intervals in a complete circle
- Manual capture at each target position after 2-second stabilization
- Circular AR guidance ring (red/yellow/green)
- Batch stitching after all 12 frames captured
- CameraX ImageCapture API (discrete photos)

**Problems:**
1. Slow and tedious (user must stop 12 times)
2. Requires careful positioning at each target
3. No real-time feedback during capture
4. Fixed 360° circular pattern (overkill for most rooms)
5. Poor user experience compared to modern phone cameras

## Target System (Samsung/Apple Style)

**Architecture:**
- Continuous capture during smooth horizontal pan
- Horizontal sweep (90-180° typical range, not full 360°)
- Directional arrow guide with real-time progress preview
- Automatic strip capture as user pans smoothly
- CameraX ImageAnalysis API (continuous frame processing)
- Real-time incremental stitching with live preview

**Benefits:**
1. Fast and intuitive (single smooth motion)
2. Natural camera movement like taking a photo
3. Real-time visual feedback (see panorama building)
4. Flexible sweep range (capture only what's needed)
5. Professional quality matching Samsung/Apple UX

---

## Functional Requirements

### FR-1: Horizontal Sweep Capture

**FR-1.1** The system SHALL support horizontal left-to-right sweep panoramas
**FR-1.2** The system SHALL support horizontal right-to-left sweep panoramas  
**FR-1.3** The system SHALL automatically detect sweep direction from first movement
**FR-1.4** The sweep range SHALL be configurable from 90° to 180° (default: 120°)
**FR-1.5** The system SHALL NOT require full 360° rotation

### FR-2: Continuous Capture Mechanism

**FR-2.1** The system SHALL capture vertical image strips continuously during pan
**FR-2.2** Strips SHALL be captured every 2-5° of horizontal rotation
**FR-2.3** Capture SHALL occur automatically during smooth motion (no manual trigger)
**FR-2.4** The system SHALL NOT require stabilization delays between captures
**FR-2.5** CameraX ImageAnalysis SHALL be used for frame-by-frame processing

### FR-3: Motion Detection and Guidance

**FR-3.1** The system SHALL analyze rotation velocity in real-time
**FR-3.2** The system SHALL detect if user is panning too fast (>30°/sec)
**FR-3.3** The system SHALL detect if user is panning too slow (<5°/sec)
**FR-3.4** The system SHALL detect jerky/unstable motion
**FR-3.5** The system SHALL prompt user to adjust movement speed if needed
**FR-3.6** The system SHALL only capture strips during smooth, properly-paced motion

### FR-4: Visual Guidance UI

**FR-4.1** The system SHALL display a directional arrow indicating sweep direction
**FR-4.2** The system SHALL display a horizontal progress bar showing sweep completion
**FR-4.3** The system SHALL display a real-time preview strip at bottom showing captured panorama
**FR-4.4** The system SHALL display speed feedback ("Too fast" / "Good" / "Too slow")
**FR-4.5** The system SHALL indicate when user approaches sweep end boundary
**FR-4.6** The circular AR guidance ring SHALL be removed entirely

### FR-5: Real-Time Stitching

**FR-5.1** The system SHALL stitch strips incrementally as they are captured
**FR-5.2** The stitched preview SHALL update in real-time during capture
**FR-5.3** The preview SHALL show the panorama building from left to right (or reverse)
**FR-5.4** Stitching latency SHALL be <100ms per strip to maintain smooth preview
**FR-5.5** The system SHALL use image blending to smooth strip boundaries

### FR-6: Capture State Management

**FR-6.1** States SHALL include: READY_TO_START, SWEEPING_IN_PROGRESS, PAUSED, SWEEP_COMPLETE, STITCHING, COMPLETED, ERROR
**FR-6.2** STABILIZING state SHALL be removed (no stabilization required)
**FR-6.3** MOVING_TO_TARGET state SHALL be removed (no discrete targets)
**FR-6.4** Transition from SWEEPING_IN_PROGRESS to PAUSED if motion stops for >1 second
**FR-6.5** Automatic transition to SWEEP_COMPLETE when sweep range is covered

### FR-7: Sensor Requirements

**FR-7.1** Gyroscope SHALL track horizontal rotation velocity
**FR-7.2** Accelerometer SHALL detect vertical tilt to ensure level panning
**FR-7.3** The system SHALL warn if device tilt exceeds ±10° from horizontal
**FR-7.4** Magnetometer SHALL provide compass heading for absolute positioning (optional)
**FR-7.5** Sensor fusion SHALL maintain <50ms latency for motion feedback

### FR-8: Output Specifications

**FR-8.1** Panorama width SHALL be proportional to sweep angle (not fixed 2:1 aspect)
**FR-8.2** For 120° sweep: width ≈ 1365px (120/360 * 4096)
**FR-8.3** For 180° sweep: width ≈ 2048px (180/360 * 4096)
**FR-8.4** Vertical resolution SHALL be 2048px
**FR-8.5** Output format SHALL be JPEG with 85% quality
**FR-8.6** File size SHALL be <20MB for typical 120° panorama

---

## Non-Functional Requirements

### NFR-1: Performance

**NFR-1.1** Strip extraction SHALL complete in <10ms per frame  
**NFR-1.2** Incremental stitching SHALL complete in <100ms per strip  
**NFR-1.3** UI feedback SHALL update at minimum 30fps during sweep  
**NFR-1.4** Memory usage SHALL not exceed 200MB during capture  
**NFR-1.5** Battery drain SHALL not exceed 5% per panorama capture

### NFR-2: Usability

**NFR-2.1** Capture SHALL complete in <10 seconds for typical 120° sweep  
**NFR-2.2** UI guidance SHALL be immediately understandable without instructions  
**NFR-2.3** Motion feedback SHALL be instant (<50ms response)  
**NFR-2.4** Preview updates SHALL appear smooth (no visible lag)  
**NFR-2.5** Error recovery SHALL be graceful (allow restart without app crash)

### NFR-3: Compatibility

**NFR-3.1** SHALL work on devices with gyroscope + accelerometer (magnetometer optional)  
**NFR-3.2** SHALL work on Android 11+ (API 30+)  
**NFR-3.3** SHALL support 5+ device manufacturers tested (Pixel, Samsung, OnePlus, Xiaomi, Motorola)  
**NFR-3.4** SHALL handle varying camera FOVs (45-80° horizontal FOV)  
**NFR-3.5** SHALL gracefully degrade on low-end devices (reduce resolution if needed)

### NFR-4: Reliability

**NFR-4.1** Capture success rate SHALL be >95% for smooth user motion  
**NFR-4.2** Stitching SHALL handle minor alignment errors (<3° per strip)  
**NFR-4.3** The system SHALL detect and recover from dropped frames  
**NFR-4.4** The system SHALL handle sensor data gaps gracefully  
**NFR-4.5** The system SHALL provide clear error messages on failure

---

## Out of Scope

- ❌ Vertical panoramas (up/down sweep)
- ❌ Multi-row panoramas (stacked horizontal sweeps)
- ❌ 360° spherical panoramas
- ❌ HDR stitching
- ❌ AI-based scene understanding
- ❌ Cloud-based stitching
- ❌ Video mode (sweep panoramas are still images)

---

## Success Criteria

1. ✅ User can capture a panorama with a single smooth horizontal pan (no stops required)
2. ✅ Panorama quality matches Samsung/Apple phone camera panorama mode
3. ✅ Capture time is <10 seconds for typical room scan
4. ✅ Real-time preview shows panorama building during capture
5. ✅ UI guidance is intuitive (arrow + progress bar)
6. ✅ No circular AR ring (replaced with sweep guidance)
7. ✅ Works reliably on 5+ device manufacturers

---

## User Stories

**US-1: As a property lister, I want to capture a room panorama quickly so that I don't waste time positioning my camera 12 times.**

**US-2: As a property lister, I want to see the panorama building in real-time so that I know the capture is working correctly.**

**US-3: As a property lister, I want the app to tell me if I'm moving too fast or too slow so that my panorama quality is good.**

**US-4: As a property lister, I want to capture only the important part of a room (not full 360°) so that the panorama focuses on what matters.**

**US-5: As a property lister, I want the scanning experience to feel like using Samsung/Apple camera so that I don't have to learn a new interaction pattern.**

---

## Technical Constraints

- Must use CameraX library (already integrated)
- Must maintain sensor fusion architecture (already robust)
- Must work offline (no cloud dependencies)
- Must integrate with existing listing/property workflow
- Must not break existing database schema (Room DB)

---

## Acceptance Criteria

### AC-1: Continuous Capture
- [ ] ImageAnalysis use case processes frames at 30fps
- [ ] Vertical strips are extracted from each frame
- [ ] Strips are captured every 2-5° of rotation
- [ ] No manual capture button needed during sweep

### AC-2: Motion Guidance
- [ ] Arrow points left or right based on sweep direction
- [ ] Speed indicator updates in real-time
- [ ] "Too fast" warning appears if velocity >30°/sec
- [ ] "Too slow" warning appears if velocity <5°/sec
- [ ] Level indicator warns if tilt >10° from horizontal

### AC-3: Real-Time Preview
- [ ] Bottom strip shows panorama building during capture
- [ ] Preview updates within 100ms of each strip capture
- [ ] Preview is visually smooth (no flicker/jump)
- [ ] Progress bar accurately reflects sweep completion %

### AC-4: Capture Flow
- [ ] Tap "Start" begins capture
- [ ] Single smooth pan completes panorama
- [ ] Automatic stop when sweep range reached
- [ ] Final panorama displayed for review
- [ ] Option to retake or save

### AC-5: Output Quality
- [ ] Panorama width matches sweep angle
- [ ] No visible seams between strips
- [ ] Straight lines appear straight (no warping)
- [ ] Exposure is consistent across panorama
- [ ] File size <20MB

---

## Priority

**P0 (Must Have):**
- FR-2 (Continuous capture)
- FR-3 (Motion detection)
- FR-4 (Visual guidance)
- FR-6 (State management)

**P1 (Should Have):**
- FR-5 (Real-time stitching)
- FR-1 (Sweep configuration)

**P2 (Nice to Have):**
- Advanced motion feedback
- Panorama editing/cropping
- Multiple sweep modes

---

**Document Version:** 1.0  
**Date:** January 18, 2026  
**Status:** Draft for Review
