# DORJA -- Verified Real Estate Platform for Bangladesh

> **A secure, AI-powered property marketplace that eliminates real estate fraud and unsafe house viewings in Bangladesh through 3D room scanning, background verification, and digital handover passports.**

**Team:** Solez-ai  
**Competition:** FIRSO Bangladesh National Round 2026  
**Category:** Software / Application  
**Platform:** Android (Kotlin + Jetpack Compose)

---

## 1. What is Dorja?

Dorja is a native Android application built to solve the real estate trust crisis in Bangladesh. Every year, thousands of buyers are scammed through fake property listings, forged documents, and unverified sellers. Women and families face safety risks when visiting unknown properties for viewings.

Dorja replaces this broken process with a digital-first platform where every listing is verified against government records (Khatian, Mutation, RAJUK plans), every seller undergoes background checks, and buyers can explore properties through **360-degree 3D room scans** -- all from their phone. Properties include a digital **Handover Passport** that tracks promises, documents, and legal status from listing to key handover.

**Dorja means "Door" in Bangla** -- because every door should be trustworthy.

---

## 2. Why is Dorja Useful?

Bangladesh's real estate market is worth over **$100 billion** yet operates with almost zero digital trust infrastructure. According to news reports:

- **Tk 3,000 crore** in fraud cases have been filed against rogue realtors who deceived buyers of land and money. ([TBS News, 2024](https://www.tbsnews.net/dropped/real-estate/rogue-realtors-deceive-buyers-land-and-money-335575))
- A Dhaka court ordered the seizure of **330 houses, flats and apartments** owned by a former land minister in eight districts. ([Instagram/TBS News, 2025](https://www.instagram.com/p/DTe4PcxDY1G/))
- **Multiple-sale scams** where the same property is sold to multiple buyers are rampant due to weak digitalization and corrupt connections between scammers. ([Advocate SM Mishuk, June 2026](https://advmsmishuk.com/multiple-sale-scam-in-bangladesh-land-and-apartment/))
- **Tk 115 crore money laundering** case filed against Nur Ali and Borak Real Estate over fraud allegations. ([Dhaka Tribune, May 2026](https://www.dhakatribune.com/bangladesh/crime/409664/money-laundering-case-filed-against-nur-ali-borak))
- From August 2024 to February 2025, the country witnessed a "shocking surge" in rape, sexual assault, and armed robbery. ([Ain o Salish Kendra / Facebook, 2026](https://www.facebook.com/groups/awamileague.1949group/posts/1890722221495394/))

Dorja is useful because it provides **verifiable trust** at every step:

| Problem | Dorja's Solution |
|---------|-----------------|
| Fake listings | Government document verification (Khatian, Mutation, RAJUK) |
| Unsafe viewings | SafeView QR passes with address verification + emergency contact |
| Scam sellers | Background-checked seller profiles with NID verification |
| Paper-based handover | Digital Handover Passport with trackable promises and uploaded documents |
| Can't see before visiting | 360-degree 3D room scanning -- explore every room from your phone |

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
+-----------------------------------------------+
|              DORJA PLATFORM                    |
|                                                |
|  Search verified listings                      |
|  Explore via 3D room scans                     |
|  Verify documents (Khatian/Mutation)           |
|  Book SafeView passes with QR codes            |
|  Digital Handover Passport                     |
|  Secure in-app messaging                       |
|                                                |
|  From search -> to keys. All verified.         |
+-----------------------------------------------+
```

The purpose is to make **every property transaction in Bangladesh transparent, safe, and verifiable** -- without relying on intermediaries, phone calls, or paper documents.

---

## 4. What Makes Dorja Unique?

### 3D Room Scanning (No Hardware Needed)
Dorja's built-in scanner uses the phone's **gyroscope + CameraX** to capture 12 overlapping frames at 30-degree intervals. These are stitched into a **2:1 equirectangular panorama** and rendered as an interactive 360-degree view. **No expensive 360-degree camera required** -- any modern smartphone with a gyroscope works.

### Built-in Security and Encryption
- QR-based viewing passes with address verification
- Emergency contact integration during property visits
- Encrypted in-app messaging
- No personal phone numbers shared between buyers and sellers

### Background-Checked Individuals
Every seller on Dorja undergoes identity verification (NID check) and background screening before listing properties. Buyers can view a seller's verification badge and history.

### Digital Handover Passport
A unique feature not found on any other platform in Bangladesh (or globally):
- Track every promise made by the seller (renovation, legal clearance, etc.)
- Upload and store legal documents (deed, mutation papers, clearance certificates)
- Digital sign-off on handover milestones
- Complete audit trail from listing to key handover

### Modern and Advanced UI
Built with **Jetpack Compose + Material 3**, Dorja features a modern, intuitive interface designed for the Bangladeshi market -- with Bangla-friendly formatting, location-aware search, and device-optimized layouts.

---

## 5. How is Dorja Different from BProperty.com?

[BProperty.com](https://www.bproperty.com) is the largest existing real estate platform in Bangladesh. However, it has significant limitations that Dorja addresses:

| Feature | BProperty | Dorja |
|---------|-----------|-------|
| 3D Room Scanning | No (limited VR tours on select premium listings only) | Yes -- built-in 360-degree scanner on every phone |
| Document Verification | No -- no government record integration | Yes -- Khatian, Mutation, RAJUK plan verification |
| Seller Background Checks | Basic listing verification only | Yes -- NID-based identity + background screening |
| SafeView Passes | No viewing safety system | Yes -- QR passes with address verification + emergency contacts |
| Handover Passport | Does not exist | Yes -- digital promise tracking + document vault |
| Fraud Prevention | Platform itself admits fraud is "prevalent" | Yes -- multi-layer verification system |
| Buyer Safety | No safety features for viewings | Yes -- QR-based pass system + emergency integration |
| In-App Document Storage | Not available | Yes -- encrypted document vault per property |
| Pricing Model | Commission-based (agent fees) | Transparent listing fees |

**Key insight:** BProperty itself acknowledges that "unregulated property listings, lack of transparency, and the prevalence of fraudulent activities have historically plagued the industry." ([BProperty News, Jan 2026](https://www.bproperty.com/news/bpropertys-dominance-in-bangladeshs-real-estate-market)). Dorja was built specifically to solve these exact problems.

**Additionally:** BProperty was reportedly approaching shutdown in late 2025, highlighting the fragility of existing platforms. ([LinkedIn, 2025](https://www.linkedin.com/posts/shifat_bproperty-realestatebangladesh-activity-7401472693482668032-5xWZ)). Dorja represents the next generation -- a trust-first platform rather than a simple listing board.

---

## 6. Why are Security Features Needed?

Bangladesh's property market is plagued by **safety and fraud crises** that demand built-in security features. The following is evidence from news sources:

### Real Estate Fraud

- **Tk 3,000 crore fraud cases** filed against rogue realtors who deceived buyers of land and money. Buyers filed around 40 separate fraud cases with allegations of embezzlement.  
  Source: [TBS News -- "Rogue realtors deceive buyers of land and money"](https://www.tbsnews.net/dropped/real-estate/rogue-realtors-deceive-buyers-land-and-money-335575)

- **Multiple-sale scams** where the same property is sold to multiple buyers due to gaps in the legal framework, weak digitalization, and corrupt connections between scammers.  
  Source: [Advocate SM Mishuk -- "Multiple Sale Scam in Bangladesh Land & Apartment"](https://advmsmishuk.com/multiple-sale-scam-in-bangladesh-land-and-apartment/) (June 2026)

- **Tk 115 crore money laundering** case filed against Nur Ali and Borak Real Estate over fraud allegations. CID investigation found the building was built illegally.  
  Source: [Dhaka Tribune -- "Money laundering case filed against Nur Ali, Borak Real Estate"](https://www.dhakatribune.com/bangladesh/crime/409664/money-laundering-case-filed-against-nur-ali-borak) (May 2026)

- **330 properties seized** from a former land minister by a Dhaka court, showing systemic corruption at the highest levels of real estate.  
  Source: [Instagram/TBS News -- Dhaka court seizure order](https://www.instagram.com/p/DTe4PcxDY1G/) (2025)

- A man was **arrested for assaulting the managing director of a real estate company** in Barishal and forcing him to sign documents.  
  Source: [TBS News / Facebook -- Police arrest](https://www.facebook.com/tbsnews.net/posts/police-today-5-july-arrested-a-man-accused-of-assaulting-the-managing-director-o/1464655025709227/) (July 2026)

- The **Ministry of Commerce formed a two-member committee** to investigate allegations against bproperty, the biggest online real estate broker in Bangladesh, and proposed a Housing Fraud Reporting Platform.  
  Source: [Financial Express Bangladesh / Facebook](https://www.facebook.com/febdonline/posts/the-ministry-of-commerce-has-formed-a-two-member-committee-to-investigate-alga/1472239608265658/) (2026)

### Physical Safety During Viewings

- **Robbery incidents** rose to 424 cases in Dhaka in 2024 (up from 375 in 2023). March and April 2026 alone recorded 605 murders and 294 snatching incidents nationwide.  
  Source: [Dhaka Tribune / Facebook -- Crime statistics](https://www.facebook.com/DhakaTribune/posts/march-and-april-alone-the-country-recorded-605-murders-294-snatching-incidents-s/1461457139356338/) (June 2026)

- From August 2024 to February 2025, Bangladesh witnessed a "shocking surge" in rape, sexual assault, and armed robbery according to Ain o Salish Kendra data.  
  Source: [Ain o Salish Kendra / Facebook](https://www.facebook.com/groups/awamileague.1949group/posts/1890722221495394/)

- **186 out of 680 reported rape cases** occurred inside victims' homes, followed by 179 cases in other locations -- highlighting the danger of visiting unknown properties alone.  
  Source: [The Daily Star / Instagram](https://www.instagram.com/p/DZUHJRjnz4Y/) (June 2026)

- Violence against women and children rose even as theft and robbery fell, with police recording 328 robbery cases but increased murders, rioting, and attacks on police.  
  Source: [TBS News -- "Theft, robbery fall, but killings, violence against women, children rise"](https://www.tbsnews.net/bangladesh/crime/theft-robbery-fall-killings-violence-against-women-children-rise-1522211)

- Deep-rooted criminal cooperation between dishonest real estate traders and corrupt officials has been documented.  
  Source: [TBS News / Facebook -- Flat buyers' decade-long battles](https://www.facebook.com/tbsnews.net/posts/%F0%9D%90%85%F0%9D%90%AB%F0%9D%90%A8%F0%9D%90%A6-%F0%9D%90%9D%F0%9D%90%AB%F0%9D%90%9E%F0%9D%90%9A%F0%9D%90%A6-%F0%9D%90%A1%F0%9D%90%A8%F0%9D%90%A6%F0%9D%90%9E%F0%9D%90%AC-%F0%9D%90%AD%F0%9D%90%A8-%F0%9D%90%A5%F0%9D%90%9E%F0%9D%90%A0%F0%9D%90%9A%F0%9D%90%A5-%F0%9D%90%A6%F0%9D%90%9A%F0%9D%90%B3%F0%9D%90%9E-%F0%9D%90%85%F0%9D%90%A5%F0%9D%90%9A%F0%9D%90%AD-%F0%9D%90%9B%F0%9D%90%AE%F0%9D%90%B2%F0%9D%90%9E%F0%9D%90%AB%F0%9D%90%AC-%F0%9D%90%9D%F0%9D%90%9E%F0%9D%90%9C%F0%9D%90%9A%F0%9D%90%9D%F0%9D%90%9E-%F0%9D%90%A5%F0%9D%90%A8%F0%9D%90%A7%F0%9D%90%A0-%F0%9D%90%9B%F0%9D%90%9A%F0%9D%90%AD%F0%9D%90%AD%F0%9D%90%A5%F0%9D%90%9E%F0%9D%90%AC-%F0%9D%90%9F%F0%9D%90%A8%F0%9D%90%AB-%F0%9D%90%A8%F0%9D%90%B0%F0%9D%90%A7%F0%9D%90%9E%F0%9D%90%AB%F0%9D%90%AC%F0%9D%90%A1%F0%9D%90%A2%F0%9D%90%A9for-/1292339296274135/) (2025)

- Real estate investment in Dhaka remains the most targeted asset class, with 75% of land records digitized by 2026 to reduce fraud.  
  Source: [Concord Real Estate -- "Why Real Estate Investment in Dhaka is the Safest Asset"](https://concordrealestatebd.com/why-real-estate-investment-in-dhaka-remains-the-safest-asset/)

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

**Q: What is Dorja?**  
A: Dorja is a verified real estate marketplace Android app for Bangladesh. It provides 3D room scanning, document verification, safe viewing passes, and digital handover passports.

**Q: Does Dorja require a 360-degree camera?**  
A: No. Dorja's built-in 3D scanner uses your phone's regular camera and gyroscope to capture 12 overlapping frames, which are algorithmically stitched into a 360-degree panoramic view. Any modern Android phone with a gyroscope works.

**Q: How does document verification work?**  
A: Dorja verifies property listings against official Bangladeshi records including Khatian (land records), Mutation documents, and RAJUK development plans. Sellers must upload original documents which are checked before listing approval.

**Q: What is a SafeView Pass?**  
A: A SafeView Pass is a QR-code-based property viewing appointment. It includes verified address details, the seller's background-check status, and an optional emergency contact notification system. When you scan the QR at the property, your emergency contact is alerted.

**Q: What is a Handover Passport?**  
A: A Handover Passport is a digital record that tracks every milestone from property listing to key handover. It includes promises made by the seller, uploaded legal documents (deed, clearance, mutation), digital sign-offs, and a complete audit trail. It replaces paper-based agreements.

**Q: Is Dorja free to use?**  
A: Dorja offers free property searching and 3D viewing. Listing creation and advanced verification features are available through affordable subscription plans.

**Q: Who can list properties on Dorja?**  
A: Verified sellers only. Every seller must complete NID (National ID) verification and a background check before their first listing goes live.

**Q: How is Dorja different from BProperty or other apps?**  
A: BProperty is a listing board -- it shows properties but does not verify them deeply, does not provide 3D scanning, does not have viewing safety features, and does not offer a handover tracking system. Dorja is a trust platform that verifies, protects, and tracks the entire property journey.

**Q: Can I use Dorja for both buying and renting?**  
A: Yes. Dorja supports property sales, rentals, and investments. Sellers can list any property type -- apartments, houses, commercial spaces, and land.

**Q: What happens if a listing turns out to be fraudulent?**  
A: Dorja's multi-layer verification (government records + seller background check + document vault) makes fraud extremely difficult. In the rare event of a fraudulent listing, the complete audit trail in the Handover Passport provides evidence for legal proceedings.

---

## 8. How is Dorja Better Than the Competition?

### vs. LIFULL HOME (Japan)
LIFULL HOME is one of Japan's largest property databases with train-line and commute-time filtering. However:
- **Japan-only** -- no coverage for Bangladesh or South Asia
- **No 3D scanning** -- relies on agent-uploaded photos
- **No document verification** -- does not integrate with government land records
- **No viewing safety** -- no SafeView pass system
- **No handover tracking** -- no digital passport for transactions
- Dorja provides all of these features specifically for the Bangladeshi market

### vs. SUUMO (Japan)
SUUMO is considered Japan's best all-around property platform. However:
- **Japan-only** -- irrelevant for Bangladeshi users
- **No verification layer** -- listings are agent-managed without government record checks
- **No 3D scanning** -- standard photo listings only
- Dorja solves the same problems SUUMO solves but for a market with far higher fraud risk and less existing infrastructure

### vs. Best-Estate.jp (Japan)
Designed for foreigners in Japan with English/Chinese support. However:
- **Japan-focused** -- no relevance to Bangladesh
- **No security features** -- no background checks or viewing passes
- **No document verification** -- no integration with any land records
- Dorja addresses the same "trust gap" for newcomers in Bangladesh's property market

### vs. Properstar (Global)
A global property search tool for cross-border rentals. However:
- **Search aggregator** -- does not verify listings or provide security
- **No 3D scanning** -- relies on agent photos
- **No local verification** -- no integration with any country's land records
- **No safety features** -- no viewing passes or emergency systems
- Dorja goes far beyond search -- it provides end-to-end verified transactions

### vs. Zillow 3D Home (USA/Canada)
Zillow's 3D Home feature integrates interactive floor plans and 360-degree virtual tours using a companion app on Android and iOS. Landlords and agents capture panoramas using a phone or a 360-degree camera. However:
- **US and Canada only** -- does not work in Bangladesh, Japan, or anywhere else
- **No rental listings in Japan** -- explicitly limited to North America
- **No document verification** -- Zillow does not verify government records
- **No safety features** -- no viewing passes or background checks
- **No handover tracking** -- no digital passport system
- **Agent/landlord controlled** -- 3D tours are optional, not mandatory on every listing
- Dorja adds the entire trust ecosystem around the 3D scan, not just the scan itself

### vs. BProperty (Bangladesh)
The most direct competitor in Bangladesh. Key differences:

| Capability | BProperty | Dorja |
|-----------|-----------|-------|
| Basic listings | Yes | Yes |
| Advanced search/filter | Yes | Yes |
| 3D room scanning | No (limited VR) | Yes (every phone) |
| Government doc verification | No | Yes (Khatian/Mutation/RAJUK) |
| Seller background checks | No | Yes (NID + screening) |
| SafeView passes | No | Yes (QR + emergency) |
| Handover Passport | No | Yes (unique feature) |
| In-app document vault | No | Yes |
| Anti-fraud system | No | Yes (multi-layer) |
| Buyer safety during visits | No | Yes |

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
      scanner/         # 3D Room Scanner (CameraX + Gyro + Stitching)
      tour/            # 360 Panorama Viewer
      listing/         # Create/Edit Listing (camera crop, gallery)
      explore/         # Buyer Property Explorer
      detail/          # Property Detail View
      chat/            # Secure In-App Messaging
      pass/            # SafeView QR Viewing Passes
      handover/        # Digital Handover Passport
      visits/          # Scheduled Visit Management
      account/         # Profile and Settings
      auth/            # Authentication and NID Verification
      gl/              # 3D Renderer
      util/            # QR Generator, Formatters
      theme/           # Material 3 Theming
      navigation/      # App Navigation Graph
    DorjaApp.kt        # Application Class
  gradle/              # Gradle wrapper + version catalog
  metadata.json        # Project metadata
```

---

## Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
- Android SDK 36
- Physical Android device with gyroscope (for 3D scanning)

### Build and Run
```bash
git clone https://github.com/Solez-ai/dorja.git
cd dorja
# Open in Android Studio and run on device
```

---

## License

Proprietary -- DORJA Bangladesh | FIRSO 2026 Submission

---

*Dorja -- Because every door should be trustworthy.*
