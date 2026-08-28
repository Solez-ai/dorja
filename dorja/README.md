# 🏠 DORJA — Verified Real Estate Platform for Bangladesh

> **A secure, AI-powered property marketplace that eliminates real estate fraud and unsafe house viewings in Bangladesh through 3D room scanning, background verification, and digital handover passports.**

**Team:** Solez-ai  
**Competition:** FIRSO Bangladesh National Round 2026  
**Category:** Software / Application  
**Platform:** Android (Kotlin + Jetpack Compose)

---

## 1. What is Dorja?

Dorja is a native Android application built to solve the real estate trust crisis in Bangladesh. Every year, thousands of buyers are scammed through fake property listings, forged documents, and unverified sellers. Women and families face safety risks when visiting unknown properties for viewings.

Dorja replaces this broken process with a digital-first platform where every listing is verified against government records (Khatian, Mutation, RAJUK plans), every seller undergoes background checks, and buyers can explore properties through **360° 3D room scans** — all from their phone. Properties include a digital **Handover Passport** that tracks promises, documents, and legal status from listing to key handover.

**Dorja means "Door" in Bangla** — because every door should be trustworthy.

---

## 2. Why is Dorja Useful?

Bangladesh's real estate market is worth over **$100 billion** yet operates with almost zero digital trust infrastructure. According to news reports:

- **Tk 3,000 crore** in fraud cases have been filed against rogue realtors (TBS News, 2024)
- A Dhaka court seized **330 properties** from a single corrupt land minister (2025)
- **Multiple-sale scams** are rampant due to weak digitalization and corrupt connections (2026)
- Women face **safety risks** during property viewings — crimes against women rose significantly in 2024–2025

Dorja is useful because it provides **verifiable trust** at every step:

| Problem | Dorja's Solution |
|---------|-----------------|
| Fake listings | Government document verification (Khatian, Mutation, RAJUK) |
| Unsafe viewings | SafeView QR passes with address verification + emergency contact |
| Scam sellers | Background-checked seller profiles with NID verification |
| Paper-based handover | Digital Handover Passport with trackable promises and uploaded documents |
| Can't see before visiting | 360° 3D room scanning — explore every room from your phone |

---

## 3. What is the Purpose?

Dorja exists to **replace phone calls, in-person visits, and paper documents with a single verified digital platform**.

Currently in Bangladesh, buying or renting property requires:
1. Calling agents repeatedly
2. Physically visiting dozens of properties (often unsafe)
3. Manually verifying documents at government offices
4. Keeping paper records of promises and agreements

**Dorja consolidates all of this into one app:**

```
┌─────────────────────────────────────────────┐
│              DORJA PLATFORM                  │
│                                              │
│  🔍 Search verified listings                 │
│  📱 Explore via 3D room scans                │
│  🔐 Verify documents (Khatian/Mutation)      │
│  🛡️ Book SafeView passes with QR codes       │
│  📄 Digital Handover Passport                │
│  💬 Secure in-app messaging                  │
│                                              │
│  From search → to keys. All verified.        │
└─────────────────────────────────────────────┘
```

The purpose is to make **every property transaction in Bangladesh transparent, safe, and verifiable** — without relying on intermediaries, phone calls, or paper documents.

---

## 4. What Makes Dorja Unique?

### 🔬 3D Room Scanning (No Hardware Needed)
Dorja's built-in scanner uses the phone's **gyroscope + CameraX** to capture 12 overlapping frames at 30° intervals. These are stitched into a **2:1 equirectangular panorama** and rendered as an interactive 360° view. **No expensive 360° camera required** — any modern smartphone works.

### 🔐 Built-in Security & Encryption
- QR-based viewing passes with address verification
- Emergency contact integration during property visits
- Encrypted in-app messaging
- No personal phone numbers shared between buyers and sellers

### ✅ Background-Checked Individuals
Every seller on Dorja undergo identity verification (NID check) and background screening before listing properties. Buyers can view a seller's verification badge and history.

### 📄 Digital Handover Passport
A unique feature not found on any other platform in Bangladesh (or globally):
- Track every promise made by the seller (renovation, legal clearance, etc.)
- Upload and store legal documents (deed, mutation papers, clearance certificates)
- Digital sign-off on handover milestones
- Complete audit trail from listing to key handover

### 🎨 Modern & Advanced UI
Built with **Jetpack Compose + Material 3**, Dorja features a modern, intuitive interface designed for the Bangladeshi market — with Bangla-friendly formatting, location-aware search, and device-optimized layouts.

---

## 5. How is Dorja Different from BProperty.com?

[BProperty.com](https://www.bproperty.com) is the largest existing real estate platform in Bangladesh. However, it has significant limitations that Dorja addresses:

| Feature | BProperty | Dorja |
|---------|-----------|-------|
| **3D Room Scanning** | ❌ Limited VR tours (only for select premium listings) | ✅ Built-in 360° scanner on every phone |
| **Document Verification** | ❌ No government record integration | ✅ Khatian, Mutation, RAJUK plan verification |
| **Seller Background Checks** | ⚠️ Basic listing verification | ✅ NID-based identity + background screening |
| **SafeView Passes** | ❌ No viewing safety system | ✅ QR passes with address verification + emergency contacts |
| **Handover Passport** | ❌ Does not exist | ✅ Digital promise tracking + document vault |
| **Fraud Prevention** | ❌ Platform admits fraud is "prevalent" | ✅ Multi-layer verification system |
| **Buyer Safety** | ❌ No safety features for viewings | ✅ QR-based pass system + emergency integration |
| **In-App Document Storage** | ❌ Not available | ✅ Encrypted document vault per property |
| **Pricing Model** | Commission-based (agent fees) | Transparent listing fees |

**Key insight:** BProperty itself acknowledges that "unregulated property listings, lack of transparency, and the prevalence of fraudulent activities have historically plagued the industry" (BProperty News, Jan 2026). Dorja was built specifically to solve these exact problems.

**Additionally:** BProperty was reportedly approaching shutdown in late 2025 (LinkedIn reports), highlighting the fragility of existing platforms. Dorja represents the next generation — a trust-first platform rather than a simple listing board.

---

## 6. Why are Security Features Needed?

Bangladesh's property market is plagued by **safety and fraud crises** that demand built-in security features. Evidence from news sources:

### Real Estate Fraud
- **Tk 3,000 crore fraud cases** filed against rogue realtors who deceived buyers of land and money (TBS News, 2024)
- **Multiple-sale scams** where the same property is sold to multiple buyers due to weak digitalization (Lawyer Bangladesh, June 2026)
- **Tk 115 crore money laundering** case filed against a real estate company (Dhaka Tribune, May 2026)
- **330 properties seized** from a former land minister — showing systemic corruption (2025)

### Physical Safety During Viewings
- **Robbery incidents** rose to 424 cases in Dhaka in 2024 (Dhaka Tribune, June 2026)
- Crimes against women, including assault during property visits, have been reported regularly
- From August 2024 to February 2025, Bangladesh witnessed a "shocking surge" in rape, sexual assault, and armed robbery (Ain o Salish Kendra data)
- **186 out of 680 reported rape cases** occurred inside victims' homes (Daily Star, June 2026) — highlighting the danger of visiting unknown properties alone

### Why This Matters for Dorja
Every safety feature in Dorja directly addresses a documented real-world threat:

| Threat | Dorja Feature |
|--------|--------------|
| Fake listings selling non-existent properties | Government document verification |
| Same property sold to multiple buyers | Blockchain-style audit trail in Handover Passport |
| Buyers robbed during viewings | SafeView passes with GPS tracking + emergency contacts |
| Sellers hiding property defects | 3D room scans show the actual property |
| Paper documents forged | Digital document vault with verification |
| Agents acting as intermediaries for fraud | Direct verified seller-to-buyer connection |

---

## 7. Frequently Asked Questions

### Q: What is Dorja?
**A:** Dorja is a verified real estate marketplace Android app for Bangladesh. It provides 3D room scanning, document verification, safe viewing passes, and digital handover passports.

### Q: Does Dorja require a 360° camera?
**A:** No. Dorja's built-in 3D scanner uses your phone's regular camera and gyroscope to capture 12 overlapping frames, which are algorithmically stitched into a 360° panoramic view. Any modern Android phone with a gyroscope works.

### Q: How does document verification work?
**A:** Dorja verifies property listings against official Bangladeshi records including Khatian (land records), Mutation documents, and RAJUK development plans. Sellers must upload original documents which are checked before listing approval.

### Q: What is a SafeView Pass?
**A:** A SafeView Pass is a QR-code-based property viewing appointment. It includes verified address details, the seller's background-check status, and an optional emergency contact notification system. When you scan the QR at the property, your emergency contact is alerted.

### Q: What is a Handover Passport?
**A:** A Handover Passport is a digital record that tracks every milestone from property listing to key handover. It includes: promises made by the seller, uploaded legal documents (deed, clearance, mutation), digital sign-offs, and a complete audit trail. It replaces paper-based agreements.

### Q: Is Dorja free to use?
**A:** Dorja offers free property searching and 3D viewing. Listing creation and advanced verification features are available through affordable subscription plans.

### Q: Who can list properties on Dorja?
**A:** Verified sellers only. Every seller must complete NID (National ID) verification and a background check before their first listing goes live.

### Q: How is Dorja different from BProperty or other apps?
**A:** BProperty is a listing board — it shows properties but doesn't verify them deeply, doesn't provide 3D scanning, doesn't have viewing safety features, and doesn't offer a handover tracking system. Dorja is a **trust platform** that verifies, protects, and tracks the entire property journey.

### Q: Can I use Dorja for both buying and renting?
**A:** Yes. Dorja supports property sales, rentals, and investments. Sellers can list any property type — apartments, houses, commercial spaces, and land.

### Q: What happens if a listing turns out to be fraudulent?
**A:** Dorja's multi-layer verification (government records + seller background check + document vault) makes fraud extremely difficult. In the rare event of a fraudulent listing, the complete audit trail in the Handover Passport provides evidence for legal proceedings.

---

## 8. How is Dorja Better Than the Competition?

### vs. LIFULL HOME (Japan)
LIFULL HOME is one of Japan's largest property databases with train-line and commute-time filtering. However:
- **Japan-only** — no coverage for Bangladesh or South Asia
- **No 3D scanning** — relies on agent-uploaded photos
- **No document verification** — does not integrate with government land records
- **No viewing safety** — no SafeView pass system
- **No handover tracking** — no digital passport for transactions
- Dorja provides all of these features **specifically for the Bangladeshi market**

### vs. SUUMO (Japan)
SUUMO is considered Japan's best all-around property platform. However:
- **Japan-only** — irrelevant for Bangladeshi users
- **No verification layer** — listings are agent-managed without government record checks
- **No 3D scanning** — standard photo listings only
- Dorja solves the same problems SUUMO solves but for a market with **far higher fraud risk** and **less existing infrastructure**

### vs. Best-Estate.jp (Japan)
Designed for foreigners in Japan with English/Chinese support. However:
- **Japan-focused** — no relevance to Bangladesh
- **No security features** — no background checks or viewing passes
- **No document verification** — no integration with Japanese (or any) land records
- Dorja addresses the same "trust gap" for foreigners/newcomers in Bangladesh's property market

### vs. Properstar (Global)
A global property search tool for cross-border rentals. However:
- **Search aggregator** — does not verify listings or provide security
- **No 3D scanning** — relies on agent photos
- **No local verification** — no integration with any country's land records
- **No safety features** — no viewing passes or emergency systems
- Dorja goes far beyond search — it provides **end-to-end verified transactions**

### vs. Zillow 3D Home (USA/Canada)
Zillow's 3D Home feature integrates interactive floor plans and 360° virtual tours using a companion app. However:
- **US and Canada only** — does not work in Bangladesh or Japan
- **No rental listings in Japan** — explicitly limited to North America
- **No document verification** — Zillow does not verify government records
- **No safety features** — no viewing passes or background checks
- **No handover tracking** — no digital passport system
- **Requires 360° camera or phone panorama** — similar capture method to Dorja, but Dorja adds the entire trust ecosystem around it
- **Agent/landlord controlled** — 3D tours are optional, not mandatory on every listing

### vs. BProperty (Bangladesh)
The most direct competitor in Bangladesh. Key differences:

| Capability | BProperty | Dorja |
|-----------|-----------|-------|
| Basic listings | ✅ | ✅ |
| Advanced search/filter | ✅ | ✅ |
| 3D room scanning | ❌ (limited VR) | ✅ (every phone) |
| Government doc verification | ❌ | ✅ (Khatian/Mutation/RAJUK) |
| Seller background checks | ❌ | ✅ (NID + screening) |
| SafeView passes | ❌ | ✅ (QR + emergency) |
| Handover Passport | ❌ | ✅ (unique feature) |
| In-app document vault | ❌ | ✅ |
| Anti-fraud system | ❌ | ✅ (multi-layer) |
| Buyer safety during visits | ❌ | ✅ |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| 3D Scanning | CameraX + Gyroscope + Panorama Stitching |
| 360° Viewer | Custom equirectangular projection engine |
| Database | Room (SQLite) |
| QR Codes | ZXing |
| Navigation | Compose Navigation |
| Architecture | MVVM + Repository Pattern |
| Build | Gradle (Kotlin DSL) |

---

## 📱 Project Structure

```
dorja/
├── app/src/main/java/com/example/
│   ├── data/              # Room DB, entities, DAOs, repository
│   ├── ui/
│   │   ├── scanner/       # 3D Room Scanner (CameraX + Gyro + Stitching)
│   │   ├── tour/          # 360° Panorama Viewer
│   │   ├── listing/       # Create/Edit Listing (camera crop, gallery)
│   │   ├── explore/       # Buyer Property Explorer
│   │   ├── detail/        # Property Detail View
│   │   ├── chat/          # Secure In-App Messaging
│   │   ├── pass/          # SafeView QR Viewing Passes
│   │   ├── handover/      # Digital Handover Passport
│   │   ├── visits/        # Scheduled Visit Management
│   │   ├── account/       # Profile & Settings
│   │   ├── auth/          # Authentication & NID Verification
│   │   ├── gl/            # 3D Renderer
│   │   ├── util/          # QR Generator, Formatters
│   │   ├── theme/         # Material 3 Theming
│   │   └── navigation/    # App Navigation Graph
│   └── DorjaApp.kt        # Application Class
├── gradle/                # Gradle wrapper + version catalog
└── metadata.json          # Project metadata
```

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
- Android SDK 36
- Physical Android device with gyroscope (for 3D scanning)

### Build & Run
```bash
git clone https://github.com/Solez-ai/dorja.git
cd dorja
# Open in Android Studio and run on device
```

---

## 📄 License

Proprietary — DORJA Bangladesh | FIRSO 2026 Submission

---

*Dorja — Because every door should be trustworthy.* 🚪
