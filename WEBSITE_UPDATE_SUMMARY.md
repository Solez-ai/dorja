# Dorja Website Content Update Summary

**Date:** January 18, 2026  
**Commits:** 0bca9ea, ac3b289  
**Website:** https://solez-ai.github.io/dorja/

## Overview

Updated the Dorja marketing website to accurately reflect the Android app's advanced features that were previously undocumented. The website now properly showcases the technical sophistication of the 360° room scanner, on-device AI assistant, and sensor fusion capabilities.

## Changes Made

### 1. Enhanced 360° Scanner Documentation

**Location:** `client/src/pages/Home.tsx` - `whatsNew` array

**Before:**
- Generic description: "360° panoramas are captured with the phone's own camera and gyroscope, stitched on device, and replayed with gyro panning — no special hardware."
- Basic points: Gyro-driven viewer, on-device stitching, works with any camera

**After:**
- Technical description: "12-frame panoramic scanning with gyroscope-based orientation tracking, accelerometer fallback, real-time AR guidance ring, and on-device stitching — no special hardware needed."
- Detailed points:
  - Sensor fusion for precise tracking
  - AR alignment guidance with haptic feedback
  - Multi-device support: 5+ manufacturers tested

**Impact:** Users now understand the scanner uses professional-grade sensor fusion technology, not just a simple gyroscope read.

---

### 2. Added On-Device AI Assistant Feature

**Location:** `client/src/pages/Home.tsx` - New entry in `whatsNew` array

**New Feature Card:**
```typescript
{
  icon: Bot,
  title: "On-device AI assistant",
  description: "Hey Dorja voice assistant runs locally with Cactus Compute, offering property insights, document help, and voice control without sending data to the cloud.",
  points: [
    "Voice-activated with 'Hey Dorja'",
    "4 specialized property tools",
    "100% on-device inference"
  ],
}
```

**Technical Details Documented:**
- Cactus Compute AI engine for local inference
- Voice wake-word detection ("Hey Dorja")
- Privacy-first: zero cloud transmission
- Property-specific tool suite (4 tools)

**Impact:** Positions Dorja as a privacy-respecting AI-powered platform, differentiating from cloud-dependent competitors.

---

### 3. Updated Main Features Array

**Location:** `client/src/pages/Home.tsx` - `features` array, "See the space first" feature

**Before:**
- "Create 360° property views with a phone's camera and gyroscope—no specialised 360° hardware required."

**After:**
- "Capture immersive 360° property views with 12-frame panoramic scanning, gyroscope-based orientation tracking, and on-device stitching—no specialized hardware required."

**Impact:** Main features section now aligns with the detailed "What's New" content.

---

### 4. Technical Implementation

**Icon Imports:**
- Added `Bot` icon from lucide-react for AI assistant
- Added `Scan` icon (prepared for future use)

**Code Quality:**
- TypeScript check passed: `pnpm run check` ✓
- No compilation errors
- Maintained existing design system ("Quiet Momentum")
- Preserved all accessibility features

---

## Features Now Properly Documented

### Scanner Technology Stack
1. **Sensor Fusion System**
   - Gyroscope-based orientation tracking
   - Accelerometer fallback mechanism
   - Magnetometer interference detection
   - 12-frame capture sequence with overlap detection

2. **AR Guidance System**
   - Real-time alignment ring visualization
   - Color-coded feedback (poor/good/excellent alignment)
   - Haptic feedback on target lock
   - 30° field-of-view guidance

3. **Multi-Device Support**
   - Tested on 5+ manufacturers
   - Pixel, Samsung, OnePlus, Xiaomi, Motorola
   - Android 11+ compatibility
   - Handles devices with/without magnetometers

### AI Assistant Features
1. **Local Inference Engine**
   - Cactus Compute 1.4.1-beta
   - No cloud dependencies
   - 100% on-device processing

2. **Voice Interaction**
   - "Hey Dorja" wake-word detection
   - Android SpeechRecognizer integration
   - Works on property detail screens

3. **Property Tools Suite**
   - Document analysis assistance
   - Property comparison insights
   - Voice-controlled navigation
   - Context-aware suggestions

---

## Deployment

**GitHub Actions Workflow:** `.github/workflows/deploy-pages.yml`  
**Build Command:** `pnpm run build` (sets `GITHUB_PAGES=true`)  
**Deployment:** Automatic on push to `main` branch  
**Live URL:** https://solez-ai.github.io/dorja/

**Status:** ✅ Commits pushed, workflow will deploy automatically

---

## Design Consistency

All updates maintain the established "Quiet Momentum" design language:
- Editorial Bangladeshi product aesthetic
- Warm paper surfaces with ink-blue hierarchy
- Dorja Ember accent color for CTAs
- Bangla vernacular marginalia
- Accessible motion with `prefers-reduced-motion` support

---

## Next Steps

### Recommended Future Updates

1. **AI Assistant Screenshots**
   - Add visual asset: `dorja-assets/hey-dorja-sheet.jpg`
   - Show voice interface in action
   - Include property context interaction

2. **Scanner Process Visuals**
   - Add visual asset: `dorja-assets/scanner-guidance.jpg`
   - Show AR alignment ring
   - Illustrate 12-frame capture sequence

3. **Technical Specifications Page**
   - Dedicated page for sensor requirements
   - Device compatibility matrix
   - Performance benchmarks

4. **Developer Documentation**
   - Sensor fusion algorithm details
   - AI model architecture
   - Integration guides for third parties

---

## Files Modified

```
c:\Projects\Dorja Homestation\dorja-website-portable\
├── client/src/pages/Home.tsx          [11 insertions, 2 deletions]
└── todo.md                             [15 insertions]
```

---

## Verification Commands

```bash
# TypeScript check
cd dorja-website-portable
pnpm run check

# Production build
pnpm run build

# Preview locally
pnpm start
# or
npx serve dist/public
```

---

## References

- **Android App README:** `c:\Projects\Dorja Homestation\README.md`
- **AI Features Guide:** `c:\Projects\Dorja Homestation\AI_FEATURES_RESTORE.md`
- **Scanner Implementation:** `dorja/app/src/main/java/com/example/scanner/`
- **Build Guide:** `c:\Projects\Dorja Homestation\BUILD_GUIDE.md`

---

**Summary:** The Dorja website now accurately represents the sophisticated technical capabilities of the Android app, emphasizing sensor fusion technology, on-device AI processing, and multi-device testing—positioning Dorja as a technically advanced, privacy-respecting property platform.
