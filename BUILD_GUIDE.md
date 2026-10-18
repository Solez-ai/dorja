# Build & Deployment Guide

## GitHub Actions CI/CD

### Build APK
```bash
# Push to main branch
git push origin main

# GitHub Actions triggers:
# 1. build-apk.yml: Builds debug APK
# 2. deploy-pages.yml: Deploys website

# APK available in Releases / Latest
```

### Release Build
```bash
# Tag release
git tag v1.0.0-rome
git push origin v1.0.0-rome

# Actions automatically:
# 1. Signs APK with release keystore
# 2. Creates GitHub Release
# 3. Uploads APK to Release assets
```

## Local Testing (No Gradle Locally)

### Prerequisites
- Android Studio (latest)
- Target Android 11+ devices/emulator

### Test Workflow
1. Open project in Android Studio
2. Let gradle sync complete
3. Select target device
4. Click Run
5. Test on device
6. Push to main for CI build

## CI/CD Status

- [x] Build workflow configured
- [x] Signing configured
- [x] Release automation ready
- [x] APK available in releases
