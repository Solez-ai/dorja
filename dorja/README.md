# DORJA — Property Trust Platform

A native Android application for verified real estate in Bangladesh. Features anti-scam verification, 3D room scanning with ARCore, viewing passes, and property handover passports.

## Features

- **3D Room Scanner** — ARCore-powered spatial scanning with guided 360° panorama capture
- **Interactive 3D Tour Viewer** — OpenGL ES 2.0 rendered walkthrough with joystick controls
- **SafeView Viewing Passes** — QR-based property inspection passes with address verification
- **Anti-Scam Verification** — Khatian, Mutation, RAJUK plan verification for listings
- **Handover Passport** — Digital property handover with promise tracking
- **Seller/Buyer Account Switching** — Single device demo mode for both roles

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **AR:** ARCore (plane detection, depth sensing)
- **3D Rendering:** OpenGL ES 2.0
- **Database:** Room (SQLite)
- **Camera:** CameraX
- **QR Codes:** ZXing

## Setup

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
- Android SDK 36
- A physical Android device with ARCore support (for 3D scanning)

### Build & Run

1. Clone the repository
2. Open the `dorja` directory in Android Studio
3. Allow Gradle sync to complete
4. Run on an emulator or physical device

> **Note:** The 3D scanner requires a physical device with ARCore support. Emulators will show a simulated scan.

## Project Structure

```
dorja/
├── app/src/main/java/com/example/
│   ├── data/          # Room database, entities, DAOs, repository
│   ├── ui/
│   │   ├── ar/        # ARCore scan session management
│   │   ├── gl/        # OpenGL 3D room renderer
│   │   ├── scanner/   # Room scanner screens
│   │   ├── tour/      # 3D tour viewer
│   │   ├── listing/   # Create listing form
│   │   ├── explore/   # Buyer property exploration
│   │   ├── detail/    # Property detail view
│   │   ├── chat/      # Messaging
│   │   ├── pass/      # Viewing passes
│   │   ├── handover/  # Handover passport
│   │   ├── visits/    # Scheduled visits
│   │   ├── account/   # Profile & settings
│   │   ├── auth/      # Authentication
│   │   └── navigation/# App navigation
│   └── DorjaApp.kt    # Application class
└── gradle/            # Gradle wrapper
```

## License

Proprietary — DORJA Bangladesh
