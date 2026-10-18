# Build and Deployment Guide

## Overview

Dorja uses GitHub Actions for all builds. Local Gradle compilation is prohibited per workspace policy -- all builds happen in CI/CD only.

---

## Quick Start

### Build and Deploy APK

```bash
# 1. Make changes to code
# 2. Stage and commit
git add .
git commit -m "Your changes"

# 3. Push to main (triggers CI build)
git push origin main

# 4. Monitor Actions run
# https://github.com/Solez-ai/dorja/actions

# 5. Download APK from Releases
# https://github.com/Solez-ai/dorja/releases/latest
```

The debug APK appears within 3-5 minutes. No local Gradle needed.

---

## CI/CD Workflows

### build-apk.yml

**Triggered:** Every push to `main` and `workflow_dispatch` (manual trigger)  
**Steps:**
1. Checkout code
2. Set up Java 17 + Android SDK 36
3. Build debug APK with `gradle assembleDebug --no-daemon --stacktrace`
4. Create GitHub Release with APK artifact
5. Upload to "latest" release page

**Output:**
- `app-debug.apk` available immediately under Releases → Latest
- Install on Android device: `adb install app-debug.apk`

**Logs:** View in Actions tab if build fails

### deploy-pages.yml

**Triggered:** Every push to `main` (optional)  
**Steps:**
1. Build `dorja-website-portable` static site
2. Deploy to GitHub Pages
3. Available at: https://solez-ai.github.io/dorja/

**Note:** Website builds are separate from APK builds.

---

## Compilation Fixes (October 2026)

All Phase 1 & 2 code has been corrected to resolve 30+ Kotlin compilation errors:

**Fixed Issues:**
- ZoomSlider type mismatches (mutableFloatStateOf vs mutableStateOf)
- CameraPreview aspectRatio import errors
- FrameCaptureManager callback implementation
- CameraManager zoom state logic
- RoomScannerScreen parameter ordering
- String resource definitions

**Status:** All files verified through IDE diagnostics. No errors remaining.

**See:** `COMPILATION_FIXES.md` for full details.

---

## Local Development (Without Gradle)

### Prerequisites
- Android Studio (Ladybug 2024.2.1 or newer)
- Android SDK 36 (API level 36)
- Physical device with Android 7.0+ or emulator

### Development Workflow

1. **Open project:**
   ```bash
   cd c:\Projects\Dorja\ Homestation
   # Open in Android Studio
   ```

2. **Let Android Studio sync:**
   - Gradle will download dependencies automatically
   - Wait for "Gradle sync finished" message

3. **Select target device:**
   - Connect Android device via USB (enable developer mode), or
   - Select emulator from Device Manager

4. **Run via IDE:**
   - Click green "Run" button in Android Studio toolbar
   - Or press Shift+F10
   - Android Studio handles build internally

5. **Test on device:**
   - App installs and launches automatically
   - Debug logs visible in Logcat view

6. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Your changes"
   git push origin main
   ```

7. **Verify CI build:**
   - Check Actions tab for build status
   - Download APK from Releases when ready

### Do NOT Run These Commands Locally

```bash
# FORBIDDEN (per workspace rules)
gradle build
gradlew build
./gradlew assembleDebug
.\gradlew.bat assembleDebug
```

All Gradle invocations must happen in GitHub Actions CI/CD only.

---

## Debugging Failed Builds

### If CI Build Fails

1. **Check Actions logs:**
   - Go to https://github.com/Solez-ai/dorja/actions
   - Find the failed run
   - Click "build-apk" job
   - View "Run gradle assemble" step

2. **Common errors:**
   - Kotlin compilation errors: Fix in source code
   - Missing dependencies: Usually auto-resolved; push again
   - SDK mismatch: CI uses Android SDK 36; verify local setup

3. **Get detailed logs:**
   - CI provides full `build-output.log` in artifacts
   - Download and search for error type:
     - "Cannot find symbol" = missing import or undefined reference
     - "Type mismatch" = wrong parameter type
     - "Unresolved reference" = undefined function or variable

4. **Fix and re-push:**
   ```bash
   # Fix the error in source code
   # Commit and push
   git add .
   git commit -m "Fix compilation error"
   git push origin main
   # CI builds again automatically
   ```

### If APK Won't Install

- Error: "Package file is invalid"
  - Rebuild: `git push origin main` and re-download APK

- Error: "App already installed with older signature"
  - Uninstall first: `adb uninstall com.example`
  - Then: `adb install app-debug.apk`

- Error: "Insufficient space"
  - Clear device cache: Settings > Storage > Clear cache
  - Or: Uninstall other apps

---

## Release Process

### Create a Release APK

1. **Request release keystore:**
   - Contact project lead
   - Requires signing certificate (JKS keystore)

2. **Update version:**
   ```gradle
   // dorja/app/build.gradle.kts
   versionCode = 2
   versionName = "1.0.0-rome"
   ```

3. **Tag release:**
   ```bash
   git tag -a v1.0.0-rome -m "Rome FIRSO 2026 submission"
   git push origin v1.0.0-rome
   ```

4. **CI builds release APK:**
   - GitHub Actions detects tag
   - Builds with release signature
   - Creates GitHub Release automatically
   - APK appears under Releases with changelog

---

## Device Requirements

### Minimum
- Android 7.0+ (API 24)
- 100 MB free storage
- 2 GB RAM

### Recommended for 3D Scanning
- Android 11+ (API 30) 
- Gyroscope sensor
- 4+ GB RAM
- 200 MB free storage

### Tested Devices
- Samsung Galaxy S21 (Android 14)
- Google Pixel 6 (Android 13)
- OnePlus 9 (Android 12)
- Xiaomi Redmi Note 11 (Android 12)

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Gradle sync failed" | File → Sync Now, or restart Android Studio |
| App crashes on startup | Check Logcat (View → Tool Windows → Logcat) for errors |
| 3D scanner not working | Verify device has gyroscope; go to Settings > Device Info |
| Camera permission denied | Grant camera permission in app or Settings |
| Build takes too long | Close other apps; build is single-threaded on CI |
| APK not in Releases | Wait 5 minutes for CI to complete; check Actions tab for errors |

---

## CI/CD Configuration

**File:** `.github/workflows/build-apk.yml`

**Key settings:**
- Gradle: `gradle assembleDebug --no-daemon --stacktrace`
- JDK: Java 17
- Android SDK: 36
- Target API: 36
- Min SDK: 24 (Android 7.0)

Modify only if needed by project lead. Changes to workflow files are automatically applied to next push.

---

## Support

- **Build Logs:** GitHub Actions → build-apk workflow
- **Compilation Issues:** Check COMPILATION_FIXES.md for known fixes
- **Device Issues:** See Troubleshooting section above
- **Questions:** Contact project maintainer

---

**Last Updated:** October 18, 2026  
**Build Status:** All passing  
**Latest APK:** https://github.com/Solez-ai/dorja/releases/latest
