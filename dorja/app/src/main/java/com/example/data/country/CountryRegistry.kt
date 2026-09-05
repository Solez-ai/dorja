package com.example.data.country

/**
 * Country registry for DORJA's global (Eurasia) expansion.
 *
 * One global evidence-and-trust engine, with country-specific property,
 * identity, disclosure, language, privacy, and professional-role adapters.
 * From PLAN.md Phase 0 — this is the single source of truth for every
 * country-specific value in the app (currency, documents, launch state).
 *
 * Confidence labels follow the DORJA Eurasia Country Atlas: a country may
 * only be selected/launched when its profile is well-evidenced. Everything
 * marked DISCOVERY_REQUIRED or beyond is listed for research only and is
 * never selectable in the app.
 */

enum class AtlasConfidence(val label: String) {
    VERIFIED("Verified"),
    REGIONAL_EVIDENCE("Regional evidence"),
    DISCOVERY_REQUIRED("Discovery required"),
    PARTNER_DEPENDENT("Partner dependent"),
    NO_LAUNCH("No launch")
}

data class DocumentTypeSpec(
    val code: String,
    val label: String,
    val regionalNames: Map<String, String> = emptyMap()
)

data class CountryProfile(
    val iso2: String,
    val displayName: String,
    val currencyCode: String,            // ISO 4217
    val currencySymbol: String,
    val formatLocaleTag: String,         // BCP-47 for number/date formatting
    val pricePeriodAware: Boolean = true, // true where monthly rent quoting is the norm
    val primaryLanguages: List<String> = emptyList(),
    val rtlScripts: Boolean = false,
    val documentTypes: List<DocumentTypeSpec> = emptyList(),
    val discoveryChannels: List<String> = emptyList(),
    val professionalRoles: List<String> = emptyList(),
    val authorityRails: List<String> = emptyList(),
    val disclosureChecklist: List<String> = emptyList(),
    val confidence: AtlasConfidence = AtlasConfidence.DISCOVERY_REQUIRED,
    val launchStage: Int = 99            // Atlas staged rollout: 1=launched, 2..5=planned, 99=not planned
) {
    val selectable: Boolean get() = launchStage <= 1
    val confidenceLabel: String get() = confidence.label
}

object CountryRegistry {

    val profiles: List<CountryProfile> = listOf(
        // ── Stage 1: Bangladesh (launch market — the proving ground) ──────
        CountryProfile(
            iso2 = "BD",
            displayName = "Bangladesh",
            currencyCode = "BDT",
            currencySymbol = "\u09F3",
            formatLocaleTag = "bn-BD",
            primaryLanguages = listOf("bn-BD", "en"),
            documentTypes = listOf(
                DocumentTypeSpec("KHATIAN_PORCHA", "Khatian / Porcha"),
                DocumentTypeSpec("MUTATION_NAMZARI", "Mutation / Namzari"),
                DocumentTypeSpec("RAJUK_APPROVAL", "RAJUK / CDA Plan"),
                DocumentTypeSpec("TAX_DAKHILA", "Municipal Tax Dakhila"),
                DocumentTypeSpec("NEC_CERTIFICATE", "NEC Certificate"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Land Broker", "Advocate (Title)", "Sub-Registry Office"),
            authorityRails = listOf("RAJUK / CDA approval lookup", "Sub-registry office records"),
            confidence = AtlasConfidence.VERIFIED,
            launchStage = 1
        ),

        // ── Stage 2: India, Nepal, Bhutan ──────────────────────────────────
        CountryProfile(
            iso2 = "IN",
            displayName = "India",
            currencyCode = "INR",
            currencySymbol = "\u20B9",
            formatLocaleTag = "en-IN",
            primaryLanguages = listOf("hi-IN", "en"),
            documentTypes = listOf(
                DocumentTypeSpec("RERA_PROJECT_REGISTRATION", "RERA Project Registration"),
                DocumentTypeSpec("RERA_AGENT_REGISTRATION", "RERA Agent Registration"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("ENCUMBRANCE_CERTIFICATE", "Encumbrance Certificate"),
                DocumentTypeSpec("PROPERTY_TAX_RECEIPT", "Property Tax Receipt"),
                DocumentTypeSpec("OCCUPANCY_CERTIFICATE", "Occupancy / Completion Certificate"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("RERA Registered Agent", "Advocate (Title)", "Sub-Registrar"),
            authorityRails = listOf("RERA portal link-out (rera.mohua.gov.in)"),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE,
            launchStage = 2
        ),
        CountryProfile(
            iso2 = "NP",
            displayName = "Nepal",
            currencyCode = "NPR",
            currencySymbol = "रू",
            formatLocaleTag = "ne-NP",
            primaryLanguages = listOf("ne-NP", "en"),
            documentTypes = listOf(
                DocumentTypeSpec("LALPURJA", "Lalpurja (Land Ownership Certificate)"),
                DocumentTypeSpec("REVENUE_RECEIPT", "Land Revenue Receipt"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Land Broker", "Advocate"),
            authorityRails = listOf("MeroKitta link-out (merokitta.dos.gov.np)"),
            confidence = AtlasConfidence.VERIFIED,
            launchStage = 2
        ),
        CountryProfile(
            iso2 = "BT",
            displayName = "Bhutan",
            currencyCode = "BTN",
            currencySymbol = "Nu.",
            formatLocaleTag = "dz-BT",
            primaryLanguages = listOf("dz-BT", "en"),
            documentTypes = listOf(
                DocumentTypeSpec("LAND_TRANSACTION_CERTIFICATE", "Land Transaction Certificate"),
                DocumentTypeSpec("THRAM_NUMBER", "Thram Number Reference"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Advocate"),
            authorityRails = listOf("NLCS / eSakor link-out (web.nlcs.gov.bt)"),
            confidence = AtlasConfidence.VERIFIED,
            launchStage = 2
        ),

        // ── Coming later (atlas stages 3–4) — research only, NOT selectable ──
        CountryProfile(
            iso2 = "FR", displayName = "France",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "fr-FR",
            primaryLanguages = listOf("fr-FR", "en"),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 3
        ),
        CountryProfile(
            iso2 = "DE", displayName = "Germany",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "de-DE",
            primaryLanguages = listOf("de-DE", "en"),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 3
        ),
        CountryProfile(
            iso2 = "JP", displayName = "Japan",
            currencyCode = "JPY", currencySymbol = "¥", formatLocaleTag = "ja-JP",
            primaryLanguages = listOf("ja-JP", "en"),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 4
        ),
        CountryProfile(
            iso2 = "AE", displayName = "United Arab Emirates",
            currencyCode = "AED", currencySymbol = "د.إ", formatLocaleTag = "ar-AE",
            primaryLanguages = listOf("ar-AE", "en"), rtlScripts = true,
            confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 4
        )
    )

    /** Look up a profile; falls back to Bangladesh (the launch market). */
    fun profile(iso2: String): CountryProfile =
        profiles.firstOrNull { it.iso2.equals(iso2, ignoreCase = true) } ?: profiles.first()

    /** Countries the user may actually transact in today. */
    fun launchableProfiles(): List<CountryProfile> = profiles.filter { it.selectable }
}