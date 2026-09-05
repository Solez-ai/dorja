# DORJA Global Update — Implementation Plan

## Goal

Transform DORJA from a Bangladesh-only real estate app into the **country-aware trust layer for property decisions across Europe and Asia**, as defined in the DORJA Eurasia Country Atlas. Bangladesh remains the proving ground; every new country is added only through country adapters, never by pretending one profile fits all.

The core architectural rule from the atlas:

> **One global evidence-and-trust engine, with country-specific property, identity, disclosure, language, privacy, and professional-role adapters.**

This plan specifies exactly **where and how** to implement that in the current codebase.

---

## 1. Current state audit — what is Bangladesh-specific today

Everything below was found by direct inspection of the code. This is the full surface area that the update must touch.

### 1.1 Data model (`app/src/main/java/com/example/data/model/Entities.kt`)

| Location | Problem | Required change |
| --- | --- | --- |
| `User.location` default `"Dhaka, Bangladesh"` (line 15) | Country baked into default | Remove default; add `countryCode` field |
| `Listing.currency` default `"BDT"` (line 34) | Single-currency assumption | Keep field but source value from `CountryProfile`; change default to require explicit currency |
| `LegalDocument.documentType` comment (line 66) lists only `KHATIAN_PORCHA, MUTATION_NAMZARI, TAX_DAKHILA, RAJUK_APPROVAL, NEC_CERTIFICATE, SALE_DEED` | Bangladesh-only document taxonomy | Replace with `countryCode`-scoped document type registry (see §3.3) |
| `Listing` has no energy, liveability, or country fields | Missing atlas §8 "Energy and liveability layer" | Add profile-configurable fields (see §3.4) |
| No provenance/evidence-level fields on `LegalDocument` | Atlas §3 requires evidence levels | Add `evidenceLevel`, `checkedAt`, `expiryState`, `limitationNote` |
| No property/unit stable identity beyond `Listing.id` | Atlas §2 "Property Passport" | Add `PropertyPassport` entity (see §3.2) |

### 1.2 Database seed (`app/src/main/java/com/example/data/db/DorjaDatabase.kt`)

- Lines 86–99: seeded users have `phone = "+880 1712-345678"` and `location = "Dhaka, Bangladesh"`.
- Seed data must become country-agnostic: generic demo phones, `countryCode = "BD"` on the demo profiles, no country string in `location`.

### 1.3 Formatting (`app/src/main/java/com/example/ui/util/Formatters.kt`)

- `bdtFormat = NumberFormat.getNumberInstance(Locale("en", "BD"))` — hardcoded locale and currency.
- `formatPrice` / `formatPriceShort` hardcode the `"BDT "` prefix.
- Change signature to `formatPrice(amount: Long, currency: String, intent: String)` and delegate to a new `CountryRegistry` currency formatter (see §3.1).

### 1.4 UI strings (scan of all screens)

| File | Line | Hardcoded string |
| --- | --- | --- |
| `ui/auth/AuthScreen.kt` | 258 | `"DORJA • Verified Bangladeshi Real Estate"` |
| `ui/explore/ExploreScreen.kt` | 141 | `"Verified Bangladeshi Homes • SafeView Access"` |
| `ui/listing/CreateListingScreen.kt` | 887 | `"Host Suite • Dorja Bangladesh"` |
| `ui/listing/CreateListingScreen.kt` | 966 | `"Monthly Rent (BDT)"` / `"Asking Price (BDT)"` |
| `ui/listing/CreateListingScreen.kt` | 255–259, 737, 767, 1717, 1748 | Khatian/Porcha, Mutation/Namzari, RAJUK/CDA, NEC Certificate, "AC Land Dhanmondi, RAJUK" |
| `ui/detail/PropertyDetailScreen.kt` | 1109 | `"View land records, RAJUK approvals & chain of title"` |

All of these move into string resources + country adapter data. No screen may hardcode a country name, a currency symbol, or a country-specific document name after this update.

### 1.5 What already generalizes well

- `SafeView` QR pass flow (`ui/visits/VisitsScreen.kt`, `ui/pass/ViewingPassScreen.kt`, `Viewing` entity) is country-neutral in logic — it becomes a universal feature, renamed in strings to "SafeView viewing pass" only where the concept exists (atlas gap class: "Informal, scattered discovery and safety").
- `Promise`, `Scan`/3D rooms, `Conversation`/`Message` entities are country-neutral.
- Handover Passport (`ui/handover/HandoverPassportScreen.kt`) is the atlas "Handover Passport" — it generalizes with evidence-level labels added.

---

## 2. New architecture components

### 2.1 `CountryRegistry` — the single source of country configuration

New file: `app/src/main/java/com/example/data/country/CountryRegistry.kt`

```kotlin
data class CountryProfile(
    val iso2: String,                    // "BD", "IN", "NP", "BT", "FR", ...
    val displayName: String,
    val currencyCode: String,            // ISO 4217: "BDT", "INR", "EUR", "JPY"
    val currencySymbol: String,
    val pricePeriodAware: Boolean,       // true where monthly rent quoting is the norm
    val primaryLanguages: List<String>,  // BCP-47 tags, e.g. ["bn-BD", "en"]
    val rtlScripts: Boolean = false,     // true for Arabic/Persian/Hebrew profiles
    val documentTypes: List<DocumentTypeSpec>,   // §3.3
    val liveabilityFields: List<LiveabilityField>, // §3.4
    val discoveryChannels: List<String>, // informational, shown in Disclosure Pack
    val professionalRoles: List<String>, // "Licensed agent (MLIT)", "RERA agent", ...
    val authorityRails: List<AuthorityRail>, // §3.5
    val disclosureChecklist: List<ChecklistItem>,
    val confidence: AtlasConfidence,     // VERIFIED / REGIONAL_EVIDENCE / DISCOVERY_REQUIRED / NO_LAUNCH
    val launchStage: Int                 // matches atlas §10 staged rollout
)
```

Kept as **in-code data first** (no server dependency), moved to on-device JSON assets (`assets/countries/{iso}.json`) when the file count grows past ~15 profiles. Every entry carries its atlas confidence label so marketing and UI never over-claim.

### 2.2 `PropertyPassport` — stable property identity

New Room entity + DAO (atlas §2). Every evidence item, promise, viewing, scan, and message hangs off the passport ID, not just `listingId`. This is what survives a listing being re-posted, and what makes cross-border relocation history portable.

```
PropertyPassport(id, listingId, countryCode, addressFreeform, lat, lng,
                 createdAt, createdByUserId, expiryState)
```

### 2.3 Evidence levels everywhere (atlas §3)

Add to `LegalDocument` (and later to any user-uploaded claim):

```kotlin
val evidenceLevel: String  // SELF_DECLARED, COUNTERPARTY_CONFIRMED, ISSUER_CONFIRMED,
                           // GOVERNMENT_SOURCE_LINKED, INDEPENDENTLY_INSPECTED,
                           // EXPIRED, DISPUTED, NOT_PROVIDED
val checkedAt: Long?
val expiryState: String    // VALID, EXPIRING, EXPIRED, UNKNOWN
val limitationNote: String // mandatory: "Upload only — DORJA has not verified title"
```

UI rule enforced globally: a green badge **never** appears for anything below `ISSUER_CONFIRMED`, and even then with the limitation note visible. One shared composable: `ui/components/EvidenceBadge.kt`.

---

## 3. Step-by-step implementation

### Phase 0 — De-Bangladesh the core (no new features, 1–2 days)

1. **`Entities.kt`**
   - `User`: remove `"Dhaka, Bangladesh"` default; add `val countryCode: String = "BD"`.
   - `Listing`: `currency` loses its default (constructor param required); add `val countryCode: String = "BD"`.
   - Bump Room DB version, add destructive migration (app is pre-release) in `DorjaDatabase.kt`.
2. **`DorjaDatabase.kt` seed** — replace `+880` phones with `+880`/`+91` demo mix or neutral `+000 000-0000`; `location = ""`; set `countryCode` explicitly.
3. **`Formatters.kt`** — new API:
   ```kotlin
   fun formatPrice(amount: Long, currencyCode: String, intent: String): String
   fun formatPriceShort(amount: Long, currencyCode: String): String
   ```
   Implementation: `NumberFormat.getCurrencyInstance()` with the profile's locale from `CountryRegistry`, falling back to `"CODE 1,234"` for locales not present on device. Keep old overloads temporarily marked `@Deprecated` so the build never breaks mid-refactor.
4. **String sweep** — replace every string from §1.4 with resources:
   - `res/values/strings.xml` (English base) gets neutral copy: `"Verified homes. SafeView access."`, `"Host Suite"`, etc.
   - `CreateListingScreen` price label becomes `"Monthly Rent (%1$s)"` / `"Asking Price (%1$s)"` with the currency from the selected country profile.
   - Document-type lists in `CreateListingScreen` (lines 234–259) stop being `remember { fixed list }` and become `CountryRegistry.profile(countryCode).documentTypes`.
5. **Country selector** — `AuthScreen` (and `CreateListingScreen`) get a country picker built on `CountryRegistry`. Only countries with `launchStage <= current` are selectable; others show the atlas label ("coming later", "partner-dependent") exactly as the atlas requires.

### Phase 1 — Bangladesh hardening (atlas Stage 1)

Bangladesh already has the fullest profile: SafeView, promises, handover, 3D scans, document rails. Work to finish it as the reference implementation:

1. Implement `PropertyPassport` entity + migration; backfill for existing listings on first launch.
2. Add evidence levels to `LegalDocument` + `EvidenceBadge` composable; update `PropertyDetailScreen` Handover card (line ~1076) to show levels instead of the current binary "VERIFIED" status.
3. Build the **Disclosure Pack** exporter: `ui/detail/PropertyDetailScreen` → "Export decision pack" button → generates a multi-page PDF/PNG summary (property summary, source labels, open questions, documents, appointment history, promises). Country checklist comes from `CountryProfile.disclosureChecklist`.
4. Enforce the atlas rule in code: if a listing has zero `ISSUER_CONFIRMED`-or-better documents, the "Verified Listing" badge in `CaptureScreen` (line 160) is not shown — status becomes "Evidence pending".

### Phase 2 — India, Nepal, Bhutan adapters (atlas Stage 2)

1. **India** (`countries/in.json` or `IndiaProfile.kt`):
   - Document types: RERA project registration, RERA agent registration, sale deed, encumbrance certificate, property tax receipt, OC/CC.
   - Authority rails: `rera.mohua.gov.in` + state RERA portals as **link-out** (no scraping): store RERA number as `government-source-linked` evidence, deep-link to the state portal for user-side checking.
   - Liveability fields: carpet area (RERA-defined), power backup, water supply, parking, builder reputation.
   - Languages: `hi`, `en` first.
2. **Nepal**:
   - Authority rail: MeroKitta (`merokitta.dos.gov.np`) — link-out to land record search; user uploads the record as `GOVERNMENT_SOURCE_LINKED` only when the URL + screenshot pair is provided.
   - Document types: Lalpurja (land ownership cert), land revenue receipt, road access certificate.
3. **Bhutan**:
   - Authority rail: NLCS/eSakor (`web.nlcs.gov.bt`) transaction status, supplied by user, labelled per atlas.
   - Document types: land transaction certificate, thram number reference.
4. Add `dialectLabel` support: India state-level naming differences land in `documentTypes[].regionalNames: Map<String, String>`.

**Status (shipped):** the adapter machinery is live and country-agnostic.
- `CountryProfile.govtVerifyUrl` / `govtVerifyLabel` carry each country's official portal; `data/country/AuthorityLinks.kt` maps document types to rails (IN → RERA portal, NP → MeroKitta, BT → NLCS/eSakor) with a null rail meaning "no known public portal" (today: BD).
- `ui/components/GovernmentSourceCard.kt` renders the link-out; `GOVERNMENT_SOURCE_LINKED` evidence is rejected at add-document time unless an official source link is attached (stored on the doc notes and surfaced as a tap-to-open chip in the doc list).
- Dialect labels are already supported by `DocumentTypeSpec.regionalNames`; state-level IN entries are a data change, not a code change.
- Next: per-state RERA URL table in `AuthorityLinks` as real state portals are onboarded.

### Phase 3 — One European pilot (atlas Stage 3)

Choose after partner validation; build the machinery country-agnostic so any EU country is a data entry, not a code change:

1. **Energy evidence layer** — `Listing` gains `energyCertificateClass: String?`, `energyCertificateIssuer: String?`, `annualHeatingCost: Long?`, `renovationYear: Int?` — all rendered only when the profile's `liveabilityFields` includes them (France DPE, Germany Energieausweis, Netherlands energielabel, etc.).
2. **GDPR-style data controls** — the atlas requires minimization + deletion:
   - Add "Delete my data" + "Expire evidence" to `AccountScreen` security section.
   - Every evidence item gets `retentionUntil: Long?`; a periodic `WorkManager` job nulls out expired uploads (files + rows).
3. **EUDI-aware identity note** — do **not** integrate wallet yet; add a `identityVerificationNote` field per profile explaining what identity proof the market expects, so the UI can be honest about what DORJA does and does not check.
4. **Moderation & appeals** — `ReportReason` enum + `AppealRecord` entity (atlas §2 "Appeal and dispute record"), plus a neutral conflict view: when seeker and host claims differ (area, date, price), show both, side by side, with source and date. No silent winner, no opaque score.
5. **Cross-border relocation mode** — new screen `ui/relocation/RelocationModeScreen.kt`: pick origin + destination country, get the destination checklist, document requirements, unit conversions (sqft ↔ m²), language notes, and the list of professional roles. Data entirely from the two `CountryProfile`s.

### Phase 4 — Japan + one Gulf market (atlas Stage 4)

1. **Japan**:
   - Professional handoff (atlas §8): generate a "handoff packet" that a licensed agent can fill, timestamp and sign for the section they own. Add `ProfessionalEndorsement` entity: `{section, professionalName, licenceId, endorsedAt, statement}`.
   - Important-matters prep checklist from the MLIT context (atlas ref [10]).
   - Condition/age/disaster-context fields in the liveability layer.
   - Languages: `ja` (+ `en` for foreign residents).
2. **Gulf (UAE recommended first)**:
   - Emirate-level adapter (`subnationalProfile` field in `CountryProfile` — atlas explicitly rejects a single "Gulf profile").
   - Tenancy registration (Ejari-style) as a document type; broker-role labels.
   - RTL support: enable `rtlScripts` rendering path (`android:supportsRtl`, mirrored layouts test).

### Phase 5 — Atlas-as-product (continuous)

- Every additional country = a JSON profile + language review + support process, gated by the atlas confidence label. Nothing marked `DISCOVERY_REQUIRED`/`NO_LAUNCH` is ever selectable in the app.
- Language packs (atlas §7) launch only when translated legal terminology, moderation templates, and source labels have been reviewed by a competent speaker. Structure: `res/values-{locale}/strings.xml` + `assets/countries/` terminology packs; four separated layers per the atlas (interface, legal terminology, user content, machine-assisted explanation).

---

## 4. Files created / modified summary

| Action | Path |
| --- | --- |
| Create | `data/country/CountryRegistry.kt` (profiles, document types, liveability fields, rails) |
| Create | `data/model/PropertyPassport.kt`, `data/model/Evidence.kt` (levels, provenance) |
| Create | `ui/components/EvidenceBadge.kt` |
| Create | `ui/relocation/RelocationModeScreen.kt` |
| Create | `ui/negotiation/ConflictView.kt` (evidence graph conflict display) |
| Create | `assets/countries/{iso}.json` (from Phase 3 onward) |
| Modify | `data/model/Entities.kt` — countryCode, evidence levels, energy fields, currency no default |
| Modify | `data/db/DorjaDatabase.kt` — new entities, version bump, neutral seed |
| Modify | `ui/util/Formatters.kt` — currency-aware formatting |
| Modify | `ui/auth/AuthScreen.kt`, `ui/explore/ExploreScreen.kt`, `ui/listing/CreateListingScreen.kt`, `ui/detail/PropertyDetailScreen.kt` — string sweep, country picker, dynamic document lists, EvidenceBadge |
| Modify | `ui/account/AccountScreen.kt` — data deletion/expiry controls |
| Modify | `AndroidManifest.xml` — `android:supportsRtl="true"` (Phase 4) |

---

## 5. Rollout guards (from the atlas, non-negotiable)

1. No country appears as selectable until its profile has at least `REGIONAL_EVIDENCE` confidence **and** a reviewed language pack.
2. No feature ever claims title verification, criminal clearance, or physical safety. Every badge shows its evidence level and limitation note.
3. Atlas entries marked `NO_LAUNCH` (Vatican, North Korea, Turkmenistan, Syria, Yemen, Afghanistan, Myanmar, Palestine, Belarus/Russia partner-only) may exist as internal data but render as "Not available in your country" if ever surfaced.
4. Marketing copy in `README.md` may only cite `VERIFIED` / `REGIONAL_EVIDENCE` entries with their source links.

---

## 6. Verification checklist per phase

- [ ] `grep -ri "bangladesh\|bdt\|+880\|khatian\|rajuk" app/src` returns only country-profile data files and intentional Bangla language strings.
- [ ] Changing device locale to `bn`, `hi`, `ja`, `de` renders all screens without missing strings (Permissive missing-marker check in debug).
- [ ] Room migration runs clean from v5 → new version on a device with existing data.
- [ ] Currency test: create listings in BDT, INR, EUR, JPY, AED — all render correctly in list, detail, and create screens.
- [ ] Evidence badge test: a `SELF_DECLARED` document never renders green.
- [ ] Exported Disclosure Pack contains the destination-country checklist when opened from relocation mode.

---

## References

The atlas and its evidence links live in the submission README (atlas section + references [1]–[11] list: Eurostat Housing 2025, European Digital Identity, Digital Services Act, EPBD, ADB housing, RERA, MeroKitta, NLCS/eSakor, MLIT important-matters, PIPL translation).
