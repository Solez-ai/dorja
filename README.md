# DORJA

## Verified Real Estate Platform for Bangladesh

> **A secure property marketplace that eliminates real estate fraud and unsafe house viewings through 3D room scanning, background verification, and digital handover passports. Now expanding into a country-aware trust layer for property decisions across Europe and Asia.**

---

### Team

**Solez-ai**

### Competition

- **FIRSO Bangladesh National Round 2026**
- **FIRO Italy International Round Applicant**

### Category

Software / Application

### Platform

Android (Kotlin + Jetpack Compose)

---

## Install the App

Every push to `main` produces a fresh APK automatically.

- **Website:** https://solez-ai.github.io/dorja/
- **Latest Release:** https://github.com/Solez-ai/dorja/releases/latest

**Requirements:** Android 7.0+ (any modern smartphone with a gyroscope)

---

## What is Dorja?

Dorja is a native Android application built to solve the real estate trust crisis in Bangladesh. Every year, thousands of buyers are scammed through fake property listings, forged documents, and unverified sellers. Women and families face safety risks when visiting unknown properties for viewings.

Dorja replaces this broken process with a digital-first platform where:
- Every listing is verified against government records (Khatian, Mutation, RAJUK plans)
- Every seller undergoes background checks
- Buyers can explore properties through 360-degree 3D room scans
- Properties include a digital **Handover Passport** tracking promises, documents, and legal status

**Dorja means "Door" in Bangla** — because every door should be trustworthy.

---

## Why Dorja Matters

Bangladesh's real estate market is worth over $100 billion yet operates with almost zero digital trust infrastructure.

### Documented Fraud Cases

- **Tk 3,000 crore** in fraud cases filed against rogue realtors
- **330 properties seized** from a former land minister
- **Tk 115 crore** money laundering case against Borak Real Estate
- **Multiple-sale scams** rampant due to weak digitalization

### How Dorja Provides Verifiable Trust

| Problem | Dorja Solution |
|---------|----------------|
| Fake listings | Government document verification (Khatian, Mutation, RAJUK) |
| Unsafe viewings | SafeView QR passes with address verification + emergency contacts |
| Scam sellers | Background-checked profiles with NID verification |
| Paper-based handover | Digital Handover Passport with trackable promises |
| Can't see before visiting | 360-degree 3D room scanning on any smartphone |

---

## What Makes Dorja Unique

### 3D Room Scanning (No Hardware Required)

Dorja's built-in scanner uses the phone's gyroscope and CameraX to capture 12 overlapping frames at 30-degree intervals. These are algorithmically stitched into a 2:1 equirectangular panorama and rendered as an interactive 360-degree view. **No expensive 360-degree camera required** — any modern smartphone with a gyroscope works.

### Built-in Security Features

- QR-based viewing passes with address verification
- Emergency contact integration during property visits
- Encrypted in-app messaging
- No personal phone numbers shared between parties

### Background-Checked Sellers

Every seller undergoes NID verification and background screening before listing properties. Buyers can view verification status and history.

### Digital Handover Passport

A unique feature not found on any other platform:

- Track every promise made by the seller
- Upload and store legal documents
- Digital sign-offs on handover milestones
- Complete audit trail from listing to key handover

### Modern UI/UX

Built with Jetpack Compose and Material 3, Dorja features a modern, intuitive interface designed for the Bangladeshi market with Bangla-friendly formatting and device-optimized layouts.

---

## Comparison: Dorja vs. BProperty

| Feature | BProperty | Dorja |
|---------|-----------|-------|
| 3D Room Scanning | No (limited VR on premium only) | Yes — built-in 360 scanner on every phone |
| Document Verification | No government record integration | Yes — Khatian, Mutation, RAJUK verification |
| Seller Background Checks | Basic listing verification only | Yes — NID + background screening |
| SafeView Passes | No viewing safety system | Yes — QR passes with emergency contacts |
| Handover Passport | Does not exist | Yes — promise tracking + document vault |
| Fraud Prevention | Admittedly "prevalent" | Yes — multi-layer verification system |
| Buyer Safety | No safety features | Yes — QR pass system + emergency integration |
| Document Storage | Not available | Yes — encrypted vault per property |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| 3D Scanning | CameraX + Gyroscope + Panorama Stitching |
| 360 Viewer | Custom equirectangular projection engine |
| Database | Room (SQLite) |
| QR Codes | ZXing |
| Navigation | Compose Navigation |
| Architecture | MVVM + Repository Pattern |
| Build | Gradle (Kotlin DSL) |

---

## Project Structure

```
dorja/
  app/src/main/java/com/example/
    data/              # Room DB, entities, DAOs, repository
    ui/
      scanner/         # 3D Room Scanner
      tour/            # 360 Panorama Viewer
      listing/         # Create/Edit Listing
      explore/         # Property Explorer
      detail/          # Property Detail View
      chat/            # Secure In-App Messaging
      pass/            # SafeView QR Viewing Passes
      handover/        # Digital Handover Passport
      visits/          # Visit Management
      account/         # Profile and Settings
      auth/            # Authentication
    DorjaApp.kt        # Application Class
  gradle/              # Gradle wrapper + version catalog
```

---

## Getting Started

### Prerequisites

- Android Studio (Ladybug or newer)
- Android SDK 36
- Physical Android device with gyroscope (for 3D scanning)

### Build and Run

```bash
git clone https://github.com/Solez-ai/dorja.git
cd dorja
# Open in Android Studio and run on device
```

---

## Global Expansion Roadmap

DORJA is being rebuilt from a Bangladesh-only app into a **country-aware trust layer for property decisions across Europe and Asia**.

| Stage | Markets | What gets built |
|-------|---------|-----------------|
| 1 | Bangladesh | Bangla/English core: Property Passport, SafeView, evidence, promises, handover |
| 2 | India, Nepal, Bhutan | RERA, MeroKitta and eSakor source adapters |
| 3 | One European pilot | Energy certificate evidence, GDPR data minimization |
| 4 | Japan + Gulf market | Professional handoff, important-matters preparation |
| 5 | Further Europe and Asia | Country profiles with local evidence sources |

---

## License

Proprietary — DORJA \| FIRSO 2026 Submission

---

*Dorja — Because every door should be trustworthy.*
