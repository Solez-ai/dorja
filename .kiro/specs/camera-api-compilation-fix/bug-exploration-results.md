# Bug Condition Exploration Test Results

**Test Execution Date:** CI Run #39112912011  
**Commit:** 275fea160d1bdef69062970058e249f9610ba4e2  
**CI Workflow:** Build APK (`:app:compileDebugKotlin` step)  
**Test Outcome:** ✅ **PASSED** - Bug condition confirmed (compilation errors detected as expected)

## Purpose

This test validates **Property 1: Bug Condition - Correct API References Compile Successfully** by running the Kotlin compiler on the UNFIXED code to confirm that incorrect CameraX API references cause compilation failures. These counterexamples demonstrate that the bug exists and must be fixed.

**CRITICAL NOTE:** For bug condition exploration tests, FAILURE of the compilation is the SUCCESS case - it proves the bug exists. Once the fix is implemented, these same compilation units should compile successfully.

## Test Execution Method

Following the workspace constraint that Gradle must NOT be run locally, this test was executed via GitHub Actions CI:

1. Current code (unfixed) is on `main` branch
2. GitHub Actions `build-apk.yml` workflow automatically runs on push to main
3. Workflow executes `gradle assembleDebug` which includes `:app:compileDebugKotlin` task
4. Compilation errors are captured and published to `ci-logs` branch in `ci-errors.txt`

## Counterexamples Found

The Kotlin compiler produced the following unresolved reference errors, confirming the hypothesized root causes:

### Counterexample 1: ImageCaptureException Type Nesting Error

**File:** `FrameCaptureManager.kt`  
**Line:** 32  
**Code:** `override fun onError(exception: ImageCapture.ImageCaptureException)`

**Compiler Error:**
```
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/FrameCaptureManager.kt:32:66 Unresolved reference 'ImageCaptureException'.
```

**Analysis:** The code references `ImageCapture.ImageCaptureException` as a nested class, but `ImageCaptureException` is actually a top-level class in `androidx.camera.core` package, not nested under `ImageCapture`. This confirms the hypothesized root cause in the design document.

**Validates:** Requirement 2.1

---

### Counterexample 2: OnError Method Signature Mismatch

**File:** `FrameCaptureManager.kt`  
**Line:** 26, 32  

**Compiler Errors:**
```
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/FrameCaptureManager.kt:26:17 Class '<anonymous>' is not abstract and does not implement abstract member
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/FrameCaptureManager.kt:32:21 'onError' overrides nothing. Potential signatures for overriding:
```

**Analysis:** Because the `onError` method uses the incorrect exception type path (`ImageCapture.ImageCaptureException`), it doesn't properly override the abstract method from `OnImageSavedCallback` interface. The interface expects `ImageCaptureException` (top-level class), not the nested path. This is a cascading effect of Counterexample 1.

**Validates:** Requirement 2.3

---

### Counterexample 3: Exception Message Property Access Error

**File:** `FrameCaptureManager.kt`  
**Line:** 34  
**Code:** `android.util.Log.e("FrameCaptureManager", "Capture error: ${exception.message}")`

**Compiler Error:**
```
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/FrameCaptureManager.kt:34:95 Unresolved reference 'message'.
```

**Analysis:** This error is also a cascading effect of the incorrect exception type. Because the compiler cannot resolve `ImageCapture.ImageCaptureException` (Counterexample 1), it cannot determine the type of the `exception` parameter, and therefore cannot resolve the `message` property. Once the type is corrected to `ImageCaptureException`, the `message` property should resolve correctly as it's inherited from `Throwable`.

**Validates:** Requirement 2.2

---

### Counterexample 4: MinZoom Property Access Error

**File:** `CameraManager.kt`  
**Line:** 250  
**Code:** `val minZoom = (zoomState?.minZoom?.toFloat() ?: 1.0f)`

**Compiler Error:**
```
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/CameraManager.kt:250:39 Unresolved reference 'minZoom'.
```

**Analysis:** The code attempts to access a `minZoom` property on `ZoomState`, but this property does not exist. According to CameraX documentation, `ZoomState` provides zoom bounds via the `getMinZoomRatio()` method. In Kotlin, this should be accessed as the `minZoomRatio` property (without the `get` prefix and with proper capitalization).

**Validates:** Requirement 2.4

---

### Counterexample 5: MaxZoom Property Access Error

**File:** `CameraManager.kt`  
**Line:** 251  
**Code:** `val maxZoom = (zoomState?.maxZoom?.toFloat() ?: zoomRatio)`

**Compiler Error:**
```
e: file:///home/runner/work/dorja/dorja/dorja/app/src/main/java/com/example/scanner/CameraManager.kt:251:39 Unresolved reference 'maxZoom'.
```

**Analysis:** Similar to Counterexample 4, the code attempts to access a `maxZoom` property that doesn't exist. The correct property name is `maxZoomRatio`, which corresponds to the `getMaxZoomRatio()` getter method in the `ZoomState` interface.

**Validates:** Requirement 2.5

---

## Compilation Failure Details

**Task:** `:app:compileDebugKotlin`  
**Result:** FAILED  
**Build Time:** 37s  
**Tasks:** 32 actionable (12 executed, 20 from cache)

**Error Summary:**
- 6 compilation errors total (5 distinct counterexamples, 1 cascading effect)
- 2 files affected: `FrameCaptureManager.kt` and `CameraManager.kt`
- All errors are unresolved reference errors related to CameraX API usage

## Root Cause Validation

These counterexamples validate the hypothesized root causes from the design document:

1. ✅ **Confirmed:** `ImageCaptureException` is incorrectly referenced as a nested class
2. ✅ **Confirmed:** `ZoomState` properties use wrong names (`minZoom`/`maxZoom` instead of `minZoomRatio`/`maxZoomRatio`)
3. ✅ **Confirmed:** Exception message access fails due to incorrect exception type (cascading effect)
4. ✅ **Confirmed:** `onError` method signature mismatch due to incorrect exception type path (cascading effect)

## Next Steps

With the bug condition confirmed through these counterexamples, the next task is to implement the fix by:

1. Changing `ImageCapture.ImageCaptureException` to `ImageCaptureException` in FrameCaptureManager.kt line 32
2. Adding import for `androidx.camera.core.ImageCaptureException` at top of FrameCaptureManager.kt
3. Changing `zoomState?.minZoom` to `zoomState?.minZoomRatio` in CameraManager.kt line 250
4. Changing `zoomState?.maxZoom` to `zoomState?.maxZoomRatio` in CameraManager.kt line 251

After implementing these fixes, a subsequent CI build should compile successfully, validating that the property "Correct API References Compile Successfully" holds.

## Test Status

**Property-Based Test:** Bug Condition Exploration  
**Status:** ✅ **PASSED** (compilation failed as expected, confirming bug exists)  
**Counterexamples:** 5 distinct unresolved references documented above  
**Requirements Validated:** 2.1, 2.2, 2.3, 2.4, 2.5

---

**CI Log Reference:** `origin/ci-logs` branch, file `ci-errors.txt`  
**Full CI Run:** https://github.com/Solez-ai/dorja/actions/runs/39112912011
