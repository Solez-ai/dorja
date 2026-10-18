# CameraX API Compilation Errors Bugfix Design

## Overview

The Dorja Android build fails during Kotlin compilation due to incorrect CameraX API references in scanner files. Two files contain API usage errors: `FrameCaptureManager.kt` uses a non-existent `ImageCaptureException` type nested under `ImageCapture` and accesses properties incorrectly, while `CameraManager.kt` references non-existent `minZoom`/`maxZoom` properties on `ZoomState`. The fix involves correcting these references to use the actual CameraX API types and method names documented in the official Android CameraX library.

## Glossary

- **Bug_Condition (C)**: The condition that triggers compilation errors - when Kotlin compiler encounters incorrect CameraX API references
- **Property (P)**: The desired behavior - code compiles successfully with correct CameraX types and method calls
- **Preservation**: Existing camera functionality (zoom control, frame capture, error handling) must remain unchanged
- **ImageCaptureException**: Top-level CameraX exception class (not nested under `ImageCapture`) for image capture errors
- **ZoomState**: CameraX interface providing zoom information via getter methods (`getMinZoomRatio()`, `getMaxZoomRatio()`)
- **OnImageSavedCallback**: CameraX callback interface with `onError(ImageCaptureException)` signature

## Bug Details

### Bug Condition

The bug manifests when the Kotlin compiler processes `FrameCaptureManager.kt` and `CameraManager.kt` and encounters unresolved references to CameraX API types and properties. The compiler cannot resolve these references because they use incorrect nested type paths or non-existent property names.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type KotlinCompilationUnit
  OUTPUT: boolean
  
  RETURN (input.file == "FrameCaptureManager.kt" 
          AND input.containsReference("ImageCapture.ImageCaptureException"))
         OR (input.file == "FrameCaptureManager.kt" 
          AND input.containsPropertyAccess("exception.message"))
         OR (input.file == "CameraManager.kt" 
          AND input.containsPropertyAccess("zoomState.minZoom"))
         OR (input.file == "CameraManager.kt" 
          AND input.containsPropertyAccess("zoomState.maxZoom"))
END FUNCTION
```

### Examples

- **FrameCaptureManager.kt line 32**: `exception: ImageCapture.ImageCaptureException` → compiler error "Unresolved reference 'ImageCaptureException'" because `ImageCaptureException` is a top-level class, not nested under `ImageCapture`
- **FrameCaptureManager.kt line 34**: `exception.message` → compiler error "Unresolved reference 'message'" because the correct API for accessing the message is not a direct property
- **CameraManager.kt line 250**: `zoomState?.minZoom` → compiler error "Unresolved reference 'minZoom'" because ZoomState uses getter method `getMinZoomRatio()`, not a property `minZoom`
- **CameraManager.kt line 251**: `zoomState?.maxZoom` → compiler error "Unresolved reference 'maxZoom'" because ZoomState uses getter method `getMaxZoomRatio()`, not a property `maxZoom`

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Frame capture functionality must continue to save images to cache directory
- Error logging in frame capture must continue to log errors to Android Log
- Zoom ratio clamping logic must continue to constrain values to camera's supported range
- Camera binding and unbinding operations must continue to work on main thread
- Zoom state null-safety checks must continue to provide safe fallback values

**Scope:**
All inputs that do NOT involve the four specific incorrect API references should be completely unaffected by this fix. This includes:
- Successful image capture callback handling (`onImageSaved`)
- Zoom ratio setting via `cameraControl.setZoomRatio()`
- Camera lifecycle management (bind/unbind)
- Other CameraX API calls that are already correct

## Hypothesized Root Cause

Based on the bug description and CameraX API documentation research, the root causes are:

1. **Incorrect Type Nesting**: `ImageCaptureException` is referenced as `ImageCapture.ImageCaptureException` but it's actually a top-level class `androidx.camera.core.ImageCaptureException`, not a nested class within `ImageCapture`

2. **Property vs Method Access**: `ZoomState` provides zoom bounds via getter methods (`getMinZoomRatio()`, `getMaxZoomRatio()`) but the code attempts to access them as Kotlin properties (`minZoom`, `maxZoom`) which don't exist

3. **Exception Message Access**: The code attempts to access `exception.message` directly, but CameraX `ImageCaptureException` may require using the inherited `Throwable.message` property or a different API pattern

4. **Incorrect Method Signature**: The `onError` method signature uses the wrong exception type path, causing the abstract method to not be properly overridden

## Correctness Properties

Property 1: Bug Condition - Correct API References Compile Successfully

_For any_ Kotlin compilation unit that references CameraX exception types or ZoomState properties, the fixed code SHALL use the correct top-level `ImageCaptureException` class and SHALL call `getMinZoomRatio()`/`getMaxZoomRatio()` methods instead of accessing non-existent properties, causing compilation to succeed without unresolved reference errors.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Existing Camera Functionality Unchanged

_For any_ camera operation that does NOT involve the four specific incorrect API references (frame capture success handling, zoom setting, camera lifecycle, other CameraX calls), the fixed code SHALL produce exactly the same runtime behavior as the original code, preserving all existing camera functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

Based on the root cause analysis and CameraX API documentation ([OnImageSavedCallback](https://developer.android.com/reference/androidx/camera/core/ImageCapture.OnImageSavedCallback), [ZoomState](https://developer.android.com/reference/androidx/camera/core/ZoomState)):

**File**: `dorja/app/src/main/java/com/example/scanner/FrameCaptureManager.kt`

**Function**: `captureFrame` (anonymous `OnImageSavedCallback` implementation)

**Specific Changes**:
1. **Line 32 - Exception Type**: Change `exception: ImageCapture.ImageCaptureException` to `exception: ImageCaptureException`
   - Import `androidx.camera.core.ImageCaptureException` at top of file
   - Remove incorrect nested class path `ImageCapture.ImageCaptureException`

2. **Line 34 - Exception Message Access**: Verify `exception.message` works with correct type, or adjust if needed
   - With correct `ImageCaptureException` type, `message` property should resolve correctly (inherited from `Throwable`)
   - If still unresolved, may need null-safe access: `exception.message ?: "Unknown error"`

**File**: `dorja/app/src/main/java/com/example/scanner/CameraManager.kt`

**Function**: `setZoomRatio`

**Specific Changes**:
3. **Line 250 - Min Zoom**: Change `zoomState?.minZoom` to `zoomState?.minZoomRatio`
   - ZoomState interface provides `getMinZoomRatio()` method
   - Kotlin property access syntax works with getter methods, so `minZoomRatio` property access calls `getMinZoomRatio()`

4. **Line 251 - Max Zoom**: Change `zoomState?.maxZoom` to `zoomState?.maxZoomRatio`
   - ZoomState interface provides `getMaxZoomRatio()` method
   - Kotlin property access syntax works with getter methods, so `maxZoomRatio` property access calls `getMaxZoomRatio()`

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, confirm the compilation errors exist on unfixed code by running CI build, then verify the fixes resolve all compilation errors and preserve existing runtime behavior.

### Exploratory Bug Condition Checking

**Goal**: Confirm the compilation errors exist BEFORE implementing the fix. Verify the exact error messages match the hypothesized root causes.

**Test Plan**: Run the GitHub Actions CI build on the UNFIXED code and capture the exact Kotlin compilation error output. Parse the error log to confirm the four specific unresolved references.

**Test Cases**:
1. **ImageCaptureException Nesting Error**: Verify CI log shows "Unresolved reference 'ImageCaptureException'" at FrameCaptureManager.kt line 32 (will fail on unfixed code)
2. **Exception Message Access Error**: Verify CI log shows "Unresolved reference 'message'" at FrameCaptureManager.kt line 34 (will fail on unfixed code)
3. **MinZoom Property Error**: Verify CI log shows "Unresolved reference 'minZoom'" at CameraManager.kt line 250 (will fail on unfixed code)
4. **MaxZoom Property Error**: Verify CI log shows "Unresolved reference 'maxZoom'" at CameraManager.kt line 251 (will fail on unfixed code)

**Expected Counterexamples**:
- Kotlin compiler cannot resolve `ImageCapture.ImageCaptureException` because it's a top-level class
- Kotlin compiler cannot resolve `minZoom`/`maxZoom` properties because ZoomState uses `getMinZoomRatio()`/`getMaxZoomRatio()` methods
- Possible additional cause: `onError` method signature mismatch preventing abstract method implementation

### Fix Checking

**Goal**: Verify that for all compilation units where the bug condition holds (the four incorrect API references), the fixed code compiles successfully.

**Pseudocode:**
```
FOR ALL file IN {"FrameCaptureManager.kt", "CameraManager.kt"} DO
  result := kotlinCompile(file_fixed)
  ASSERT result.success == true
  ASSERT result.unresolvedReferences.isEmpty()
END FOR
```

**Test Plan**: Push the fixed code to GitHub and run the CI build. Monitor the `:app:compileDebugKotlin` step to confirm it completes successfully without unresolved reference errors.

**Test Cases**:
1. **Compilation Success**: CI build completes `:app:compileDebugKotlin` step successfully
2. **No Unresolved References**: CI log shows zero "Unresolved reference" errors for FrameCaptureManager.kt and CameraManager.kt
3. **APK Generation**: CI build proceeds to APK assembly and release steps

### Preservation Checking

**Goal**: Verify that for all code paths that do NOT involve the four incorrect API references, the fixed code produces the same runtime behavior as the original code.

**Pseudocode:**
```
FOR ALL operation IN {frameCaptureSuccess, zoomSetting, cameraBinding, otherCameraXCalls} DO
  ASSERT behavior_fixed(operation) == behavior_original(operation)
END FOR
```

**Testing Approach**: Since these are compilation fixes with no logic changes, preservation checking focuses on ensuring the API corrections maintain semantic equivalence. Property-based testing is less applicable here; instead, manual inspection and existing integration tests provide coverage.

**Test Plan**: Review the fixed code to confirm only the type/method names changed, not the logic. Run existing camera integration tests to verify frame capture and zoom functionality still work correctly.

**Test Cases**:
1. **Frame Capture Success Preservation**: Verify `onImageSaved` callback still adds `FrameData` to frames list with correct path and heading
2. **Error Logging Preservation**: Verify fixed `onError` still logs to Android Log with tag "FrameCaptureManager"
3. **Zoom Clamping Preservation**: Verify `setZoomRatio` still clamps values using min/max from ZoomState (now correctly accessed)
4. **Null Safety Preservation**: Verify zoom code still handles null `zoomState` with fallback values
5. **Thread Safety Preservation**: Verify camera operations still execute on correct thread (Main for binding/unbinding)

### Unit Tests

- Test that `ImageCaptureException` can be caught and logged without compilation errors
- Test that `ZoomState.minZoomRatio` and `maxZoomRatio` return expected float values
- Test that zoom clamping logic works correctly with the correct property access

### Property-Based Tests

Property-based testing is not applicable for compilation fixes. The fixes are pure API corrections with no algorithmic changes to test.

### Integration Tests

- Run existing `CameraManagerIntegrationTest.kt` to verify camera binding and zoom functionality
- Run existing `ScannerIntegrationTest.kt` to verify frame capture with error handling
- Run GitHub Actions CI build end-to-end to verify APK generation succeeds
