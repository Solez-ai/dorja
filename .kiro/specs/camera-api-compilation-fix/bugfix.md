# Bugfix Requirements Document

## Introduction

The Dorja Android APK build fails during Kotlin compilation on GitHub Actions CI with unresolved reference errors in two scanner-related files: `FrameCaptureManager.kt` and `CameraManager.kt`. These files use CameraX APIs but reference incorrect type names and property names that don't exist in the imported CameraX library dependencies. This prevents the CI build from succeeding at the `:app:compileDebugKotlin` step.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN compiling `FrameCaptureManager.kt` line 32 THEN the system fails with "Unresolved reference 'ImageCaptureException'"

1.2 WHEN compiling `FrameCaptureManager.kt` line 34 THEN the system fails with "Unresolved reference 'message'"

1.3 WHEN compiling `FrameCaptureManager.kt` line 26-36 THEN the system reports that the anonymous class implementing `OnImageSavedCallback` doesn't properly implement the abstract `onError` method

1.4 WHEN compiling `CameraManager.kt` line 250 THEN the system fails with "Unresolved reference 'minZoom'"

1.5 WHEN compiling `CameraManager.kt` line 251 THEN the system fails with "Unresolved reference 'maxZoom'"

### Expected Behavior (Correct)

2.1 WHEN compiling `FrameCaptureManager.kt` line 32 THEN the system SHALL successfully reference the correct CameraX exception type for image capture errors

2.2 WHEN compiling `FrameCaptureManager.kt` line 34 THEN the system SHALL successfully access the exception message using the correct CameraX exception API

2.3 WHEN compiling `FrameCaptureManager.kt` line 26-36 THEN the system SHALL properly implement the `OnImageSavedCallback` interface with the correct method signature for `onError`

2.4 WHEN compiling `CameraManager.kt` line 250 THEN the system SHALL successfully reference the correct property name for minimum zoom ratio from CameraX ZoomState

2.5 WHEN compiling `CameraManager.kt` line 251 THEN the system SHALL successfully reference the correct property name for maximum zoom ratio from CameraX ZoomState

### Unchanged Behavior (Regression Prevention)

3.1 WHEN compiling any other scanner-related Kotlin files THEN the system SHALL CONTINUE TO compile successfully without errors

3.2 WHEN compiling files that correctly use CameraX APIs THEN the system SHALL CONTINUE TO resolve all references properly

3.3 WHEN running GitHub Actions CI build steps before `:app:compileDebugKotlin` THEN the system SHALL CONTINUE TO complete successfully

3.4 WHEN the camera manager sets zoom ratios using the bound camera's cameraInfo THEN the system SHALL CONTINUE TO access zoom state information correctly

3.5 WHEN the frame capture manager handles successful image captures THEN the system SHALL CONTINUE TO process `OutputFileResults` correctly
