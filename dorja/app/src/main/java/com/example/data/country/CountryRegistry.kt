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

/**
 * Liveability/energy evidence fields (PLAN.md Phase 3). Which fields exist is
 * a per-country data decision: Europe renders energy classes and running
 * costs; Bangladesh/South Asia renders power backup, water, flood risk.
 */
enum class LiveabilityField(val label: String) {
    ENERGY_CLASS("Energy performance class"),
    ENERGY_ISSUER("Certificate issuer"),
    HEATING_COST("Annual heating cost"),
    RENOVATION_YEAR("Last major renovation"),
    POWER_BACKUP("Power backup"),
    WATER_SUPPLY("Water supply"),
    FLOOD_RISK("Flood / waterlogging risk"),
    BUILDING_CONDITION("Building condition"),
    BUILDING_AGE("Building age"),
    DISASTER_CONTEXT("Disaster context")
}

data class DocumentTypeSpec(
    val code: String,
    val label: String,
    val regionalNames: Map<String, String> = emptyMap()
)

/**
 * Sub-national profile (atlas: "Emirate-level adapter — a single Gulf profile
 * is explicitly rejected"). Countries where transaction rules differ by state
 * / emirate / canton declare their subdivisions here; UI and documents key
 * off the subdivision code, not the country alone.
 */
data class SubnationalProfile(
    val code: String,               // e.g. "AE-DXB"
    val displayName: String,
    val extraDocumentTypes: List<DocumentTypeSpec> = emptyList(),
    val extraAuthorityRails: List<String> = emptyList(),
    val govtVerifyUrl: String? = null,
    val govtVerifyLabel: String? = null,
    val notes: String = ""
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
    /** Official public portal where a user can independently check a government-issued document (link-out only). */
    val govtVerifyUrl: String? = null,
    /** Human label for that portal, e.g. "RERA portal (rera.mohua.gov.in)". */
    val govtVerifyLabel: String? = null,
    /**
     * Liveability/energy fields this market expects (Phase 3 machinery).
     * Code renders only what the profile lists — adding a market is data.
     */
    val liveabilityFields: List<LiveabilityField> = emptyList(),
    /** What identity proof this market expects, so UI can be honest about what DORJA does/does not check. */
    val identityVerificationNote: String = "",
    /** Sub-national subdivisions (emirates, states, cantons) where rules differ. */
    val subnationalProfiles: List<SubnationalProfile> = emptyList(),
    val confidence: AtlasConfidence = AtlasConfidence.DISCOVERY_REQUIRED,
    val launchStage: Int = 99            // Atlas staged rollout: 1=launched, 2..5=planned, 99=not planned
) {
    val selectable: Boolean get() = launchStage <= 1
    val confidenceLabel: String get() = confidence.label

    /** Document types including sub-national additions, for a given subdivision code. */
    fun documentTypesFor(subnationalCode: String? = null): List<DocumentTypeSpec> {
        val sub = subnationalProfiles.firstOrNull { it.code.equals(subnationalCode ?: "", ignoreCase = true) }
        return documentTypes + (sub?.extraDocumentTypes ?: emptyList())
    }

    fun subnational(code: String?): SubnationalProfile? =
        subnationalProfiles.firstOrNull { it.code.equals(code ?: "", ignoreCase = true) }
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
            liveabilityFields = listOf(
                LiveabilityField.POWER_BACKUP,
                LiveabilityField.WATER_SUPPLY,
                LiveabilityField.FLOOD_RISK
            ),
            identityVerificationNote = "Bangladesh transactions commonly rely on NID verification at the sub-registry office; DORJA records the upload but does not certify identity.",
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
            disclosureChecklist = listOf(
                "Registered sale deed (kabala) with the sub-registry office",
                "Mutation (namzari) updated in AC land / municipal records",
                "RAJUK / CDA approval or building completion certificate",
                "Municipal tax dakhila (current year)",
                "Electricity, gas and water connection papers",
                "NEC clearance where mortgage is involved",
                "Approved floor map / layout plan",
                "Owner NID copy and holding number verification"
            ),
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
            govtVerifyUrl = "https://rera.mohua.gov.in",
            govtVerifyLabel = "RERA portal (rera.mohua.gov.in)",
            disclosureChecklist = listOf(
                "RERA project registration number (state portal)",
                "Encumbrance certificate from the sub-registrar",
                "Occupancy / completion certificate",
                "Property tax receipt (current year)",
                "Approved building plan sanction",
                "Registered sale deed / agreement for sale"
            ),
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
            govtVerifyUrl = "https://www.merokitta.dos.gov.np/",
            govtVerifyLabel = "MeroKitta land records (merokitta.dos.gov.np)",
            disclosureChecklist = listOf(
                "Lalpurja (land ownership certificate)",
                "Land revenue receipt (current year)",
                "Registered sale deed at the district registrar",
                "MeroKitta land-record reference",
                "Apartment / maison evidence where applicable"
            ),
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
            govtVerifyUrl = "https://www.nlcs.gov.bt/",
            govtVerifyLabel = "NLCS / eSakor (web.nlcs.gov.bt)",
            disclosureChecklist = listOf(
                "Land transaction certificate (thram-based)",
                "NLCS / eSakor transaction status",
                "Registered deed with the relevant authority",
                "Plot approval / national land commission clearance"
            ),
            confidence = AtlasConfidence.VERIFIED,
            launchStage = 2
        ),

        // ── Coming later (atlas stages 3–4) — research only, NOT selectable ──
        CountryProfile(
            iso2 = "FR", displayName = "France",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "fr-FR",
            primaryLanguages = listOf("fr-FR", "en"),
            liveabilityFields = listOf(
                LiveabilityField.ENERGY_CLASS,
                LiveabilityField.ENERGY_ISSUER,
                LiveabilityField.HEATING_COST,
                LiveabilityField.RENOVATION_YEAR
            ),
            identityVerificationNote = "French rentals commonly expect a dossier with payslips and a guarantor; DORJA stores your evidence but does not run credit checks.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 3
        ),
        CountryProfile(
            iso2 = "DE", displayName = "Germany",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "de-DE",
            primaryLanguages = listOf("de-DE", "en"),
            liveabilityFields = listOf(
                LiveabilityField.ENERGY_CLASS,
                LiveabilityField.ENERGY_ISSUER,
                LiveabilityField.HEATING_COST,
                LiveabilityField.RENOVATION_YEAR
            ),
            identityVerificationNote = "German landlords commonly request SCHUFA and income proof; DORJA stores your evidence but does not run credit checks.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 3
        ),
        CountryProfile(
            iso2 = "JP", displayName = "Japan",
            currencyCode = "JPY", currencySymbol = "¥", formatLocaleTag = "ja-JP",
            primaryLanguages = listOf("ja-JP", "en"),
            documentTypes = listOf(
                DocumentTypeSpec("TAKKEN_LICENSE", "Real Estate Brokerage License (Takken)") ,
                DocumentTypeSpec("IMPORTANT_MATTERS_DOC", "Important Matters Explanation (Jūyō Jikō Setsumeisho)") ,
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("BUILDING_CONFIRMATION", "Building Confirmation (Kenchiku Kakunin)") ,
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Takken Agent (宅建士)", "Land & House Investigator (土地家屋調査士)", "Administrative Scrivener (行政書士)"),
            authorityRails = listOf("MLIT existing-home transaction guidance (link-out)"),
            disclosureChecklist = listOf(
                "Important Matters explanation (jūyō jikō setsumeisho) received and explained",
                "Building confirmation / inspection certificate",
                "Existing-home condition survey (where available)",
                "Boundary and registration survey by a land & house investigator",
                "Disaster risk context for the plot (earthquake, flood maps)"
            ),
            liveabilityFields = listOf(
                LiveabilityField.BUILDING_CONDITION,
                LiveabilityField.BUILDING_AGE,
                LiveabilityField.DISASTER_CONTEXT,
                LiveabilityField.RENOVATION_YEAR
            ),
            identityVerificationNote = "Japanese transactions run through licensed takken professionals; DORJA prepares evidence and handoff, it does not replace the licensed explanation of important matters.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 4
        ),
        CountryProfile(
            iso2 = "AE", displayName = "United Arab Emirates",
            currencyCode = "AED", currencySymbol = "د.إ", formatLocaleTag = "ar-AE",
            primaryLanguages = listOf("ar-AE", "en"), rtlScripts = true,
            documentTypes = listOf(
                DocumentTypeSpec("TITLE_DEED", "Title Deed"),
                DocumentTypeSpec("EJARI_REGISTRATION", "Tenancy Registration (Ejari-style)"),
                DocumentTypeSpec("AGENT_PERMIT", "Broker RERA Permit"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("RERA-licensed Broker", "Advocate (Tenancy)", "Property Registrar"),
            authorityRails = listOf("Emirate land department / tenancy registry (link-out)"),
            disclosureChecklist = listOf(
                "Title deed from the emirate land department",
                "Tenancy registration (Ejari in Dubai) current",
                "Broker's RERA permit number",
                "Service charge / maintenance history",
                "Developer completion and handover status (off-plan)"
            ),
            subnationalProfiles = listOf(
                SubnationalProfile(
                    code = "AE-DXB",
                    displayName = "Dubai",
                    extraDocumentTypes = listOf(
                        DocumentTypeSpec("EJARI_REGISTRATION", "Ejari Tenancy Registration"),
                        DocumentTypeSpec("DLD_TITLE_DEED", "DLD Title Deed"),
                        DocumentTypeSpec("OQOOD_OFFPLAN", "Oqood Off-Plan Registration")
                    ),
                    extraAuthorityRails = listOf("Dubai Land Department (dubailand.gov.ae)"),
                    govtVerifyUrl = "https://dubailand.gov.ae",
                    govtVerifyLabel = "Dubai Land Department (dubailand.gov.ae)",
                    notes = "Ejari tenancy registration is mandatory for rentals; Oqood registers off-plan sales before title issue."
                ),
                SubnationalProfile(
                    code = "AE-AUH",
                    displayName = "Abu Dhabi",
                    extraDocumentTypes = listOf(
                        DocumentTypeSpec("TAWTHEQ_REGISTRATION", "Tawtheq Tenancy Contract Registration")
                    ),
                    extraAuthorityRails = listOf("ADREC / Tawtheq (adrec.gov.ae)"),
                    govtVerifyUrl = "https://www.adrec.gov.ae",
                    govtVerifyLabel = "ADREC / Tawtheq (adrec.gov.ae)",
                    notes = "Tenancy contracts are registered via Tawtheq under ADREC."
                ),
                SubnationalProfile(
                    code = "AE-SHJ",
                    displayName = "Sharjah",
                    extraDocumentTypes = listOf(
                        DocumentTypeSpec("SQ_TOTALITIES", "Sharjah Municipality Tenancy Attestation")
                    ),
                    extraAuthorityRails = listOf("Sharjah Municipality (shjmun.gov.ae)"),
                    notes = "Rental contracts require municipality attestation; freehold ownership is restricted to designated zones."
                )
            ),
            identityVerificationNote = "UAE transactions run through RERA-licensed brokers and emirate land departments; DORJA stores your evidence and handoff trail but does not replace the registries.",
            confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 4
        )
    )

    /** Look up a profile; falls back to Bangladesh (the launch market). */
    fun profile(iso2: String): CountryProfile =
        profiles.firstOrNull { it.iso2.equals(iso2, ignoreCase = true) } ?: profiles.first()

    /** Countries the user may actually transact in today. */
    fun launchableProfiles(): List<CountryProfile> = profiles.filter { it.selectable }
}