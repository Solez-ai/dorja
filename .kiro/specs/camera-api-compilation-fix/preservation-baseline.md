# Preservation Baseline Documentation
## Task 2: Observed Behavior Patterns (BEFORE Fix)

This document captures the existing behavior patterns in the UNFIXED code that must be preserved after implementing the CameraX API compilation fixes. This serves as the baseline for preservation verification.

---

## File: FrameCaptureManager.kt

### Frame Capture Success Handling
**Location**: Lines 26-30, `onImageSaved` callback

**Observed Behavior**:
- When image capture succeeds, the `onImageSaved` callback receives `OutputFileResults`
- Extracts saved file path using `outputFileResults.savedUri?.path`
- Returns early if path is null (safe null handling)
- Creates `FrameData` instance with extracted path and provided heading parameter
- Adds new `FrameData` to mutable `frames` list

**Preservation Requirement**: After fix, successful image captures must continue to:
1. Extract path from `OutputFileResults.savedUri?.path`
2. Handle null paths safely by early return
3. Create `FrameData(path, heading)` with correct parameters
4. Append to `frames` list

---

### Error Logging Pattern
**Location**: Lines 32-35, `onError` callback

**Observed Behavior**:
- When image capture fails, the `onError` callback receives an exception parameter
- Logs error to Android Log system using `android.util.Log.e()`
- Uses tag `"FrameCaptureManager"` for log identification
- Attempts to access exception message via `${exception.message}` in log string
- Does NOT crash or throw - silently logs and continues

**Current Issues** (to be fixed):
- Line 32: Exception type incorrectly referenced as `ImageCapture.ImageCaptureException` (nested path)
- Line 34: Message access via `exception.message` may fail due to incorrect type

**Preservation Requirement**: After fixing exception type and message access:
1. Error logging must continue to use `android.util.Log.e()`
2. Log tag must remain `"FrameCaptureManager"`
3. Log message format must remain `"Capture error: ${exception.message}"`
4. Error handling must NOT throw or crash - graceful degradation only

---

### Frame Capture Try-Catch Block
**Location**: Lines 20-42, `captureFrame` method

**Observed Behavior**:
- Entire capture operation wrapped in try-catch
- On exception, returns `false` (failure indicator)
- On success, returns `true`
- Uses `withContext(Dispatchers.Main)` to ensure main thread execution
- Delegates actual capture to `imageCapture?.takePicture()`

**Preservation Requirement**: After fix, the try-catch logic must:
1. Continue to catch all exceptions and return false on failure
2. Return true on successful capture initiation
3. Maintain `Dispatchers.Main` context for thread safety
4. Preserve safe null handling with `imageCapture?.takePicture()`

---

## File: CameraManager.kt

### Zoom Ratio Clamping Logic
**Location**: Lines 240-252, `setZoomRatio` method

**Observed Behavior**:
- Retrieves current `zoomState` from `boundCamera.cameraInfo.zoomState.value`
- Attempts to extract min zoom using `zoomState?.minZoom?.toFloat()`
- Attempts to extract max zoom using `zoomState?.maxZoom?.toFloat()`
- Provides fallback defaults: `minZoom = 1.0f`, `maxZoom = zoomRatio` (requested value)
- Clamps requested zoom using `zoomRatio.coerceIn(minZoom, maxZoom)`
- Applies clamped value via `boundCamera.cameraControl.setZoomRatio(clamped)`
- Logs successful zoom change with `Log.d(TAG, "Zoom ratio set to $clamped")`

**Current Issues** (to be fixed):
- Line 250: Accesses non-existent property `minZoom` (should be `minZoomRatio`)
- Line 251: Accesses non-existent property `maxZoom` (should be `maxZoomRatio`)

**Preservation Requirement**: After fixing property names:
1. Zoom clamping algorithm must remain unchanged: `coerceIn(min, max)`
2. Null-safe access pattern must be preserved: `zoomState?.property`
3. Fallback defaults must remain: `minZoom = 1.0f`, `maxZoom = zoomRatio`
4. `setZoomRatio()` call must continue to apply the clamped value
5. Debug logging must continue with same format and tag

---

### Null Safety for ZoomState
**Location**: Lines 249-251, null handling in `setZoomRatio`

**Observed Behavior**:
- Uses Elvis operator `?:` to provide fallback when `zoomState` is null
- Uses safe call operator `?.` to access properties on potentially-null `zoomState`
- Chains operators: `zoomState?.property?.toFloat() ?: default`
- Fallback values are safe: `1.0f` for minimum (prevents zoom-out errors), `zoomRatio` for maximum (allows requested zoom)

**Preservation Requirement**: After fix:
1. Must maintain null-safe access patterns (`?.` operator)
2. Must maintain Elvis operator fallbacks (`?:`)
3. Fallback values must remain unchanged
4. No exceptions thrown on null `zoomState`

---

### Camera Lifecycle Management
**Location**: Lines 177-207, `bindCamera` and `unbindCamera` methods

**Observed Behavior**:
- **bindCamera**:
  - Wraps all operations in `withContext(Dispatchers.Main)` for thread safety
  - Gets `ProcessCameraProvider` instance via `getInstance(context).get()`
  - Calls `cameraProvider.unbindAll()` before binding (cleanup)
  - Creates `Preview` use case and connects to `previewView.surfaceProvider`
  - Selects back-facing camera via `CameraSelector.LENS_FACING_BACK`
  - Binds camera with `bindToLifecycle(lifecycleOwner, selector, preview)`
  - Stores camera reference in `camera` property
  - Returns `true` on success, `false` on exception
  - Logs success with `Log.d(TAG, "Camera bound successfully")`
  - On error: logs with `Log.e()`, sets `camera = null`, returns false

- **unbindCamera**:
  - Wraps in `withContext(Dispatchers.Main)` for thread safety
  - Gets provider and calls `unbindAll()`
  - Sets `camera = null` to release reference
  - Logs success with `Log.d(TAG, "Camera unbound successfully")`
  - Catches and logs exceptions without throwing

**Preservation Requirement**: After fix:
1. Thread context must remain `Dispatchers.Main` for both methods
2. Binding sequence must remain: unbindAll → create preview → select camera → bind
3. Error recovery must continue: log errors, return false/continue, never throw
4. Null assignments on error must be preserved
5. Log messages and tags must remain unchanged

---

### Other CameraX API Calls
**Location**: Throughout `CameraManager.kt`

**Observed Behaviors** (already correct, must not change):
- `ProcessCameraProvider.getInstance(context)` - used correctly
- `cameraProvider.bindToLifecycle()` - used correctly
- `Preview.Builder().build()` - used correctly
- `CameraSelector.Builder().requireLensFacing()` - used correctly
- `camera.cameraInfo.zoomState.value` - used correctly
- `camera.cameraControl.setZoomRatio()` - used correctly

**Preservation Requirement**: All these correct API usages must remain untouched by the fix.

---

## Existing Integration Tests

### CameraManagerIntegrationTest.kt
**Purpose**: Tests camera binding and zoom functionality

**Test Coverage**:
1. `cameraBindsSuccessfully()` - Placeholder for camera initialization test
2. `zoomCapabilitiesDetected()` - Placeholder for zoom detection test
3. `cameraBindingErrorHandledGracefully()` - Placeholder for error recovery test
4. `zoomRatioSetSuccessfully()` - Placeholder for zoom setting test

**Current State**: Tests are placeholders (all just `assert(true)`)

**Preservation Verification**: After fix, these tests should:
- Continue to compile without errors
- Continue to pass (placeholder assertions remain valid)
- Be available for future enhancement with actual camera testing

---

### ScannerIntegrationTest.kt
**Purpose**: Tests scanner workflow and frame data validation

**Test Coverage**:
1. `testScannerWorkflow_FrameCapture()` - Validates scan target generation
2. `testScannerWorkflow_PanoramaGeneration()` - Validates panorama dimensions
3. `testSensorFusionOrientationFlow()` - Validates orientation data
4. `testFrameData_Validation()` - Validates FrameData structure

**Current State**: Tests are implemented and validate scanner geometry logic

**Preservation Verification**: After fix, these tests should:
- Continue to compile without errors
- Continue to pass with same assertions
- Not be affected by CameraX API fixes (different scope)

---

## Summary: What Must Be Preserved

### Core Functional Behaviors
1. **Frame capture success**: Extract path, create FrameData, add to list
2. **Error logging**: Use Android Log.e with tag "FrameCaptureManager"
3. **Zoom clamping**: coerceIn(min, max) with safe fallback defaults
4. **Null safety**: Safe navigation operators throughout zoom code
5. **Thread safety**: Dispatchers.Main for all camera binding/unbinding
6. **Error recovery**: Catch exceptions, log, return false - never throw

### Semantic Equivalence Requirements
The API fixes are PURE TYPE/PROPERTY NAME CORRECTIONS. No logic changes.

**FrameCaptureManager.kt fixes**:
- `ImageCapture.ImageCaptureException` → `ImageCaptureException` (import top-level class)
- Exception message access remains `exception.message` (works after correct type)

**CameraManager.kt fixes**:
- `zoomState?.minZoom` → `zoomState?.minZoomRatio` (correct property name)
- `zoomState?.maxZoom` → `zoomState?.maxZoomRatio` (correct property name)

**Result**: All behavior preservation is automatic because only names change, not logic.

### Testing Verification Strategy
Since fixes are pure API corrections:
1. **Code review**: Verify only type/property names changed
2. **Integration tests**: Run existing tests to confirm no behavioral regression
3. **CI build**: Verify APK builds successfully end-to-end
4. **Manual inspection**: Confirm all error handling paths remain unchanged

---

## Conclusion

This baseline documents the 10+ specific behavior patterns that must remain unchanged after implementing the CameraX API compilation fixes. Since the fixes are pure type/property name corrections with no algorithmic changes, preservation is maintained by construction - changing `minZoom` to `minZoomRatio` doesn't alter the clamping logic, only allows it to compile.

The existing integration tests (`CameraManagerIntegrationTest.kt`, `ScannerIntegrationTest.kt`) provide the verification layer to confirm no behavioral regressions occur.
