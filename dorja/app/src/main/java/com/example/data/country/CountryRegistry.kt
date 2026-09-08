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
    val selectable: Boolean get() = launchStage <= 6
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
        ),

        // ── Stage 5: Europe — Strong Regional Lead ────────────────────────

        CountryProfile(
            iso2 = "GB", displayName = "United Kingdom",
            currencyCode = "GBP", currencySymbol = "£", formatLocaleTag = "en-GB",
            primaryLanguages = listOf("en-GB"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("EPC_CERTIFICATE", "EPC (Energy Performance Certificate)"),
                DocumentTypeSpec("GAS_SAFETY_CERT", "Gas Safety Certificate (CP12)"),
                DocumentTypeSpec("ELECTRICAL_CERT", "Electrical Installation Condition Report (EICR)"),
                DocumentTypeSpec("INVENTORY_CHECKLIST", "Inventory / Condition Report"),
                DocumentTypeSpec("TENANCY_AGREEMENT", "Tenancy Agreement (AST)"),
                DocumentTypeSpec("DEPOSIT_PROTECTION_CERT", "Deposit Protection Certificate"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("RICS Surveyor", "Letting Agent (ARLA/Propertymark)", "Deposit Protection Scheme"),
            authorityRails = listOf("EPC Register (epcregister.com)", "TDS / DPS / mydeposits"),
            govtVerifyUrl = "https://www.epcregister.com",
            govtVerifyLabel = "EPC Register (epcregister.com)",
            disclosureChecklist = listOf(
                "EPC certificate (valid, not expired)",
                "Gas Safety Certificate (annual, CP12)",
                "Electrical Installation Condition Report (EICR) — 5-year cycle",
                "How to Rent guide (mandatory for ASTs in England)",
                "Deposit protection scheme certificate and prescribed information",
                "Inventory / condition report at check-in and check-out",
                "Landlord licence (where local authority scheme applies)"
            ),
            identityVerificationNote = "UK landlords must conduct Right to Rent checks (England); DORJA stores your identity evidence but does not run Right to Rent or credit checks.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "IE", displayName = "Ireland",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "en-IE",
            primaryLanguages = listOf("en-IE", "ga-IE"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("BER_CERTIFICATE", "BER Certificate (Building Energy Rating)"),
                DocumentTypeSpec("RTB_TENANCY_REGISTRATION", "RTB Tenancy Registration"),
                DocumentTypeSpec("TENANCY_AGREEMENT", "Tenancy Agreement"),
                DocumentTypeSpec("DEPOSIT_RECEIPT", "Deposit Receipt"),
                DocumentTypeSpec("INVENTORY_CHECKLIST", "Inventory / Condition Report"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("PSRA-licensed Agent", "RTB-registered Landlord", "Solicitor"),
            authorityRails = listOf("RTB (rtb.ie)", "BER Register (SEAI)"),
            govtVerifyUrl = "https://www.rtb.ie",
            govtVerifyLabel = "Residential Tenancies Board (rtb.ie)",
            disclosureChecklist = listOf(
                "BER certificate (valid, SEAI registered)",
                "RTB tenancy registration confirmation",
                "Tenancy agreement with mandatory terms",
                "Deposit receipt",
                "Inventory / condition report at move-in and move-out",
                "Landlord PSRA registration number"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "BE", displayName = "Belgium",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "fr-BE",
            primaryLanguages = listOf("fr-BE", "nl-BE", "de-BE", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.HEATING_COST, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("EPC_CERTIFICATE", "EPC / PEB / EPB Certificate"),
                DocumentTypeSpec("LEASE_AGREEMENT", "Bail / Huurovereenkomst"),
                DocumentTypeSpec("INSPECTION_REPORT", "Plaatsbeschrijving / Etat des Lieux"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("IPI-licensed Agent (Wallonia/Brussels)", "BIV-licensed Agent (Flanders)", "Notaire / Notaris"),
            disclosureChecklist = listOf(
                "EPC / PEB / EPB certificate (valid for 10 years)",
                "Signed lease with inspection report at move-in",
                "Building permit / planning compliance",
                "Urban planning certificate",
                "Soil certificate where required"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "NL", displayName = "Netherlands",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "nl-NL",
            primaryLanguages = listOf("nl-NL", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("ENERGY_LABEL", "Energielabel"),
                DocumentTypeSpec("TENANCY_AGREEMENT", "Huurovereenkomst"),
                DocumentTypeSpec("SERVICE_COST_SPEC", "Servicekosten Specificatie"),
                DocumentTypeSpec("WOZ_REFERENCE", "WOZ Waarde Reference"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("NVM Makelaar", "VBO Makelaar", "VastgoedPRO Agent"),
            disclosureChecklist = listOf(
                "Energielabel (registered, not expired)",
                "Tenancy agreement with huurbescherming section",
                "WOZ value reference",
                "Servicekosten specification",
                "Deposit terms and conditions"
            ),
            identityVerificationNote = "Dutch AVG/GDPR restricts landlord requests; DORJA issues purpose-limited, expiring application packs with audit trail.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "PT", displayName = "Portugal",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "pt-PT",
            primaryLanguages = listOf("pt-PT", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("CADERNETA_PREDIAL", "Caderneta Predial (Property Register)"),
                DocumentTypeSpec("EPC_CERTIFICATE", "Certificado de Desempenho Energetico (CDE)"),
                DocumentTypeSpec("LICENCA_HABITABILIDADE", "Licenca de Utilizacao / Habitabilidade"),
                DocumentTypeSpec("CPCV", "CPCV (Contrato-Promessa de Compra e Venda)"),
                DocumentTypeSpec("ESCRITURA", "Escritura Publica (Notarial Deed)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Mediador Imobiliario (AMI licensed)", "Notario", "Solicitador"),
            govtVerifyUrl = "https://www.dgterritorio.gov.pt",
            govtVerifyLabel = "DGT Cartografia e Cadastro",
            disclosureChecklist = listOf(
                "Caderneta predial from Financas",
                "CDE energy certificate — class and expiry",
                "Licenca de utilizacao / habitabilidade",
                "CPCV (promissory contract) before final deed",
                "Escritura publica at notary",
                "IMI (property tax) clearance"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "ES", displayName = "Spain",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "es-ES",
            primaryLanguages = listOf("es-ES", "ca", "eu", "gl", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("NOTA_SIMPLE", "Nota Simple (Property Registry Extract)"),
                DocumentTypeSpec("CEE_CERTIFICATE", "Certificado de Eficiencia Energetica (CEE)"),
                DocumentTypeSpec("NIE_DOCUMENT", "NIE (Numero de Identidad de Extranjero)"),
                DocumentTypeSpec("CONTRATO_ARRAS", "Contrato de Arras (Reservation Contract)"),
                DocumentTypeSpec("ESCRITURA", "Escritura de Compraventa"),
                DocumentTypeSpec("LICENCIA_TURISTICA", "Licencia de Alquiler Turistico"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("API (Agente de la Propiedad Inmobiliaria)", "Notario", "Gestor"),
            disclosureChecklist = listOf(
                "Nota simple from Registro de la Propiedad",
                "CEE energy certificate — class and expiry",
                "NIE for foreign buyers",
                "Tourist licence check (restricted zones)",
                "Contrato de arras / escritura de compraventa",
                "IBI (property tax) receipt current year"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "IT", displayName = "Italy",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "it-IT",
            primaryLanguages = listOf("it-IT", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("APE_CERTIFICATE", "APE (Attestato di Prestazione Energetica)"),
                DocumentTypeSpec("VISURA_CATASTALE", "Visura Catastale"),
                DocumentTypeSpec("PLANIMETRIA_CATASTALE", "Planimetria Catastale"),
                DocumentTypeSpec("ATTO_ROGITO", "Atto di Rogito (Notarial Deed)"),
                DocumentTypeSpec("CERTIFICATO_AGIBILITA", "Certificato di Agibilita"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Agente Immobiliare (FIAIP / FIMAA)", "Notaio", "Geometra / Ingegnere"),
            disclosureChecklist = listOf(
                "APE — class, issuer, and expiry",
                "Visura catastale and planimetria catastale",
                "Atto rogito (deed) from notaio",
                "Certificato di agibilita",
                "Condominium rules and annual charges where applicable"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "PL", displayName = "Poland",
            currencyCode = "PLN", currencySymbol = "zł", formatLocaleTag = "pl-PL",
            primaryLanguages = listOf("pl-PL", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("KW_EXTRACT", "Odpis z Ksiegi Wieczystej (KW Extract)"),
                DocumentTypeSpec("EPC_CERTIFICATE", "Swiadectwo Charakterystyki Energetycznej"),
                DocumentTypeSpec("MPZP_PLAN", "MPZP Zoning Plan Reference"),
                DocumentTypeSpec("SALE_DEED", "Akt Notarialny (Notarial Deed)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Posrednik Nieruchomosci (Licensed Agent)", "Notariusz", "Rzeczoznawca Majatkowy"),
            authorityRails = listOf("Electronic Land & Mortgage Register (ekw.ms.gov.pl)"),
            govtVerifyUrl = "https://ekw.ms.gov.pl",
            govtVerifyLabel = "Elektroniczne Ksiegi Wieczyste (ekw.ms.gov.pl)",
            disclosureChecklist = listOf(
                "KW (Ksiega Wieczysta) current extract — encumbrances and ownership",
                "MPZP zoning plan reference",
                "Swiadectwo charakterystyki energetycznej",
                "Akt notarialny at purchase",
                "Building year and last major renovation"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "RO", displayName = "Romania",
            currencyCode = "RON", currencySymbol = "lei", formatLocaleTag = "ro-RO",
            primaryLanguages = listOf("ro-RO", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("CARTE_FUNCIARA", "Extras de Carte Funciara (Land Register Extract)"),
                DocumentTypeSpec("EPC_CERTIFICATE", "Certificat de Performanta Energetica"),
                DocumentTypeSpec("BUILDING_PERMIT", "Autorizatie de Construire / Regularizare"),
                DocumentTypeSpec("SALE_DEED", "Contract de Vanzare-Cumparare (Notarized)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Agent Imobiliar (ANI Licensed)", "Notar Public", "Evaluator"),
            disclosureChecklist = listOf(
                "Extras de carte funciara (current encumbrances and ownership)",
                "Certificat de performanta energetica — class and expiry",
                "Autorizatie de construire / regularizare",
                "Utility connection status",
                "Condominium rules and monthly charges where applicable"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "BG", displayName = "Bulgaria",
            currencyCode = "BGN", currencySymbol = "лв", formatLocaleTag = "bg-BG",
            primaryLanguages = listOf("bg-BG", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("PROPERTY_DEED", "Notarialen Akt (Notarial Deed)"),
                DocumentTypeSpec("SKETCH_CERT", "Skitsa i Udostoverenie (Cadastral Sketch & Certificate)"),
                DocumentTypeSpec("EPC_CERTIFICATE", "Sertifikat za Energiyni Harakteristiki (EPC)"),
                DocumentTypeSpec("TAX_ASSESSMENT", "Danachna Otsenka (Tax Assessment)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Notarius", "Licensed Valuer"),
            disclosureChecklist = listOf(
                "Notarialen akt (notarially certified deed)",
                "Cadastral sketch and area certificate",
                "EPC (sertifikat za energiyni harakteristiki)",
                "Tax assessment (danachna otsenka)",
                "Condominium management rules where applicable",
                "Utility running-cost evidence (heating, electricity)"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "GR", displayName = "Greece",
            currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "el-GR",
            primaryLanguages = listOf("el-GR", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("EPC_CERTIFICATE", "Pistopoiitiko Energeiakis Apodosis (PEA)"),
                DocumentTypeSpec("E9_TAP_TAX", "E9 / TAP Property Tax"),
                DocumentTypeSpec("TOPOGRAFIKO", "Topografiko Diagramma (Survey Diagram)"),
                DocumentTypeSpec("SALE_DEED", "Symvolaio Polisis (Deed of Sale)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Real Estate Broker (Mesitis)", "Civil Engineer (Michanikos)", "Notary (Symvolaio)"),
            disclosureChecklist = listOf(
                "PEA — class and expiry",
                "E9 / TAP property tax clearance",
                "Topografiko (topographic survey by licensed engineer)",
                "Structural engineer condition note where building age > 30 years",
                "Urban planning clearance"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "HU", displayName = "Hungary",
            currencyCode = "HUF", currencySymbol = "Ft", formatLocaleTag = "hu-HU",
            primaryLanguages = listOf("hu-HU", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("INGATLAN_NYILVANTARTO", "Ingatlan-nyilvantartas Kivonat (Property Register Extract)"),
                DocumentTypeSpec("EPC_CERTIFICATE", "Energetikai Tanusitvany (EPC)"),
                DocumentTypeSpec("SALE_CONTRACT", "Adasveteli Szerzodes (Sale Contract)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Ingatlankozveto (Licensed Agent)", "Kozjegyzo (Notary)", "Ugyved (Lawyer)"),
            disclosureChecklist = listOf(
                "Property register extract with encumbrances",
                "EPC (energetikai tanusitvany)",
                "Sale contract notarised",
                "Building permit compliance"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        CountryProfile(
            iso2 = "TR", displayName = "Turkiye",
            currencyCode = "TRY", currencySymbol = "₺", formatLocaleTag = "tr-TR",
            primaryLanguages = listOf("tr-TR", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE, LiveabilityField.DISASTER_CONTEXT, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("TAPU_DEED", "Tapu Senedi (Title Deed)"),
                DocumentTypeSpec("ISKAN_CERTIFICATE", "Iskan / Yapi Ruhsati (Building Permit / Occupancy)"),
                DocumentTypeSpec("DEPREM_RISK_REPORT", "Deprem Risk Degerlendirme Raporu"),
                DocumentTypeSpec("DASK_INSURANCE", "DASK Zorunlu Deprem Sigortasi"),
                DocumentTypeSpec("FOREIGN_BUYER_PERMIT", "Askeri Izin (Military Clearance) for foreign buyers"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent (Tasinmaz Ticareti Yetki Belgesi)", "Notary (Noter)", "Licensed Structural Engineer"),
            authorityRails = listOf("Land Registry and Cadastre (tkgm.gov.tr)"),
            govtVerifyUrl = "https://www.tkgm.gov.tr",
            govtVerifyLabel = "Land Registry and Cadastre (tkgm.gov.tr)",
            disclosureChecklist = listOf(
                "Tapu senedi (title deed) from Tapu Sicil Mudurlugu",
                "Iskan / yapi ruhsati (occupancy certificate)",
                "DASK earthquake insurance (mandatory)",
                "Deprem risk assessment report",
                "Askeri izin (military clearance) for foreign buyers",
                "Building age and structural reinforcement history"
            ),
            identityVerificationNote = "Foreign buyers must obtain military clearance (askeri izin) before purchase; DORJA explains required steps but does not process permits.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 5
        ),

        // ── Stage 6: Asia — Strong Regional Lead ──────────────────────────

        CountryProfile(
            iso2 = "LK", displayName = "Sri Lanka",
            currencyCode = "LKR", currencySymbol = "Rs", formatLocaleTag = "si-LK",
            primaryLanguages = listOf("si-LK", "ta-LK", "en"),
            liveabilityFields = listOf(LiveabilityField.WATER_SUPPLY, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("DEED_OF_TRANSFER", "Deed of Transfer (Registered)"),
                DocumentTypeSpec("SURVEY_PLAN", "Survey Plan (Licensed Surveyor)"),
                DocumentTypeSpec("LAND_REGISTRY_EXTRACT", "Land Registry Extract"),
                DocumentTypeSpec("LST_TITLE", "Land Settlement Title (LST) where applicable"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Notary", "Licensed Land Surveyor", "Attorney-at-Law"),
            govtVerifyUrl = "https://www.landregistry.gov.lk",
            govtVerifyLabel = "Land Registry of Sri Lanka",
            disclosureChecklist = listOf(
                "Deed of transfer registered at the Land Registry",
                "Survey plan by licensed surveyor",
                "Land registry extract (title search)",
                "LST where applicable",
                "Lot number and assessment number reference"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "PK", displayName = "Pakistan",
            currencyCode = "PKR", currencySymbol = "₨", formatLocaleTag = "ur-PK",
            primaryLanguages = listOf("ur-PK", "en"), rtlScripts = true,
            liveabilityFields = listOf(LiveabilityField.WATER_SUPPLY, LiveabilityField.POWER_BACKUP),
            documentTypes = listOf(
                DocumentTypeSpec("FARD_REGISTRY", "Fard (Land Record Extract from Patwari / PLRA)"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("NOC_BUILDING", "NOC (Town Committee / LDA / PDA)"),
                DocumentTypeSpec("TAX_RECEIPT", "Property Tax Receipt"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Sub-Registrar", "Property Dealer (PBTE Registered)", "Advocate"),
            govtVerifyUrl = "https://www.plra.punjab.gov.pk",
            govtVerifyLabel = "Punjab Land Records Authority (plra.punjab.gov.pk)",
            disclosureChecklist = listOf(
                "Fard from Patwari or online PLRA",
                "Registered sale deed at Sub-Registrar",
                "NOC from Town Committee / LDA / PDA",
                "Property tax clearance",
                "Approved map from relevant authority"
            ),
            identityVerificationNote = "Land records in Punjab via PLRA; Sindh, KPK, Balochistan have separate systems; DORJA links out but does not certify records.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "VN", displayName = "Vietnam",
            currencyCode = "VND", currencySymbol = "₫", formatLocaleTag = "vi-VN",
            primaryLanguages = listOf("vi-VN", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("LUCC_PINK_BOOK", "So Do / So Hong (Land Use Rights Certificate)"),
                DocumentTypeSpec("PROJECT_APPROVAL", "Quyet Dinh Phe Duyet Du An (Project Approval)"),
                DocumentTypeSpec("CONSTRUCTION_PERMIT", "Giay Phep Xay Dung (Construction Permit)"),
                DocumentTypeSpec("HANDOVER_CERTIFICATE", "Bien Ban Ban Giao (Handover Acceptance Certificate)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Real Estate Business", "Notary (Cong Chung)"),
            disclosureChecklist = listOf(
                "So do / so hong (land use rights certificate)",
                "Project investment approval document",
                "Giay phep xay dung (construction permit)",
                "Bien ban ban giao (handover/acceptance certificate)",
                "Apartment management rules and fees"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "TH", displayName = "Thailand",
            currencyCode = "THB", currencySymbol = "฿", formatLocaleTag = "th-TH",
            primaryLanguages = listOf("th-TH", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE),
            documentTypes = listOf(
                DocumentTypeSpec("CHANOTE_TITLE", "Chanote (Nor Sor 4 Jor) Title"),
                DocumentTypeSpec("CONDO_JURISTIC_CERT", "Condo Juristic Person Certificate"),
                DocumentTypeSpec("FOREIGN_QUOTA_CERT", "Foreign Quota Confirmation"),
                DocumentTypeSpec("LEASE_30YR", "30-Year Lease Agreement (Registered)"),
                DocumentTypeSpec("LAND_OFFICE_RECEIPT", "Land Office Transaction Receipt"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Broker (AREA)", "Lawyer", "Land Office Official"),
            govtVerifyUrl = "https://www.dol.go.th",
            govtVerifyLabel = "Department of Lands (dol.go.th)",
            disclosureChecklist = listOf(
                "Chanote (Nor Sor 4 Jor) — land department registered",
                "Condo juristic person certificate (common fee, rules, sinking fund)",
                "Foreign quota confirmation (49% freehold condo limit)",
                "30-year lease registration at land office if applicable",
                "Transfer fee and withholding tax breakdown"
            ),
            identityVerificationNote = "Foreign buyers cannot own land freehold; condo or long-lease structures require legal advice; DORJA explains structures but does not give legal advice.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "PH", displayName = "Philippines",
            currencyCode = "PHP", currencySymbol = "₱", formatLocaleTag = "tl-PH",
            primaryLanguages = listOf("tl-PH", "en-PH"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE),
            documentTypes = listOf(
                DocumentTypeSpec("TCT_TITLE", "Transfer Certificate of Title (TCT)"),
                DocumentTypeSpec("TAX_DECLARATION", "Tax Declaration (Assessor)"),
                DocumentTypeSpec("BIR_CAR", "BIR CAR (Certificate Authorizing Registration)"),
                DocumentTypeSpec("DEED_OF_SALE", "Deed of Absolute Sale (Notarized)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Real Estate Broker (PRC)", "Notary Public", "BIR Liaison"),
            govtVerifyUrl = "https://www.lra.gov.ph",
            govtVerifyLabel = "Land Registration Authority (lra.gov.ph)",
            disclosureChecklist = listOf(
                "TCT at Register of Deeds",
                "Tax declaration from local assessor",
                "BIR CAR at sale",
                "Annotated TCT — check for encumbrances",
                "HLURB / DHSUD clearance for condo projects"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "ID", displayName = "Indonesia",
            currencyCode = "IDR", currencySymbol = "Rp", formatLocaleTag = "id-ID",
            primaryLanguages = listOf("id-ID", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.FLOOD_RISK),
            documentTypes = listOf(
                DocumentTypeSpec("SHM_TITLE", "Sertifikat Hak Milik (SHM) or HGB"),
                DocumentTypeSpec("IMB_PERMIT", "IMB / PBG (Building Permit)"),
                DocumentTypeSpec("PBB_TAX", "PBB (Pajak Bumi dan Bangunan) Clearance"),
                DocumentTypeSpec("AJB_DEED", "AJB (Akta Jual Beli) before PPAT"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("PPAT (Notary / Land Deed Official)", "Licensed Agent (SIM Agen)", "BPN Land Agency"),
            govtVerifyUrl = "https://www.atrbpn.go.id",
            govtVerifyLabel = "BPN Land Agency (atrbpn.go.id)",
            disclosureChecklist = listOf(
                "SHM or HGB — registered at BPN",
                "IMB / PBG building permit",
                "PBB property tax clearance",
                "AJB (deed of sale) executed before PPAT",
                "BPN certificate verification"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "KH", displayName = "Cambodia",
            currencyCode = "KHR", currencySymbol = "៛", formatLocaleTag = "km-KH",
            primaryLanguages = listOf("km-KH", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE),
            documentTypes = listOf(
                DocumentTypeSpec("HARD_TITLE_CERT", "Hard Title Certificate (Sertifika / LMAP)"),
                DocumentTypeSpec("STRATA_TITLE", "Strata Title (Co-Ownership Certificate)"),
                DocumentTypeSpec("DEVELOPER_AGREEMENT", "Developer Sales Agreement"),
                DocumentTypeSpec("HANDOVER_CERT", "Handover / Completion Certificate"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Cadastral Officer (Ministry of Land)", "Licensed Agent", "Lawyer"),
            disclosureChecklist = listOf(
                "Hard title certificate (not soft title)",
                "Strata title (co-ownership) for condos",
                "Developer sales agreement",
                "Handover / completion certificate",
                "Land use restriction and zoning check"
            ),
            identityVerificationNote = "Foreigners may hold strata title (floors 2+) but not land freehold; DORJA explains structures but does not give legal advice.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "KZ", displayName = "Kazakhstan",
            currencyCode = "KZT", currencySymbol = "₸", formatLocaleTag = "kk-KZ",
            primaryLanguages = listOf("kk-KZ", "ru-RU"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("CADASTRAL_PASSPORT", "Cadastral Passport"),
                DocumentTypeSpec("PROPERTY_REGISTER_CERT", "Property Register Certificate"),
                DocumentTypeSpec("SALE_CONTRACT", "Notarised Sale Contract"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Notary (Notarius)"),
            govtVerifyUrl = "https://egov.kz",
            govtVerifyLabel = "e-Government Portal (egov.kz)",
            disclosureChecklist = listOf(
                "Cadastral passport from state cadastre",
                "Property register certificate from Justice Department",
                "Notarially certified sale contract",
                "Encumbrance and arrest check"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "UZ", displayName = "Uzbekistan",
            currencyCode = "UZS", currencySymbol = "soʻm", formatLocaleTag = "uz-UZ",
            primaryLanguages = listOf("uz-UZ", "ru-RU"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.POWER_BACKUP),
            documentTypes = listOf(
                DocumentTypeSpec("MAKKON_CERT", "Guvohnoma / Makkon (Ownership Certificate)"),
                DocumentTypeSpec("BUILDING_PERMIT", "Qurilish ruxsatnomasi (Building Permit)"),
                DocumentTypeSpec("COMMISSION_ACT", "Davlat qabul akti (Commission Act)"),
                DocumentTypeSpec("SALE_AGREEMENT", "Sotish shartnomasi (Sale Agreement)"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Notary (Notarius)", "Cadastral Engineer"),
            disclosureChecklist = listOf(
                "Guvohnoma / Makkon reference",
                "Registered sale agreement",
                "Building permit and commission act",
                "Utility connection act"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "GE", displayName = "Georgia",
            currencyCode = "GEL", currencySymbol = "₾", formatLocaleTag = "ka-GE",
            primaryLanguages = listOf("ka-GE", "en", "ru-RU"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("PROPERTY_EXTRACT", "Property Extract (Public Registry — napr.gov.ge)"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("CONSTRUCTION_PERMIT", "Construction Permit / Commissioning Act"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Notary (Notariusi)", "Licensed Agent", "Licensed Engineer"),
            govtVerifyUrl = "https://www.napr.gov.ge",
            govtVerifyLabel = "National Agency of Public Registry (napr.gov.ge)",
            disclosureChecklist = listOf(
                "Property extract from Public Registry (napr.gov.ge)",
                "Notarially certified sale deed",
                "Construction permit and commissioning act",
                "Apartment building management rules where applicable"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "UA", displayName = "Ukraine",
            currencyCode = "UAH", currencySymbol = "₴", formatLocaleTag = "uk-UA",
            primaryLanguages = listOf("uk-UA", "en"),
            liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE),
            documentTypes = listOf(
                DocumentTypeSpec("STATE_PROPERTY_EXTRACT", "State Register Property Extract (Derzhreyestr)"),
                DocumentTypeSpec("SALE_DEED", "Notarially Certified Sale Deed"),
                DocumentTypeSpec("DAMAGE_ASSESSMENT", "War Damage Assessment Record"),
                DocumentTypeSpec("OCCUPANCY_STATUS", "Occupancy / Safety Status"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Notary (Notarius)", "State Emergency Service Contact"),
            govtVerifyUrl = "https://rp.ibt.com.ua",
            govtVerifyLabel = "State Property Register (ibt.com.ua)",
            disclosureChecklist = listOf(
                "State property register extract (online check)",
                "Occupancy and safety status (where obtainable)",
                "War damage assessment record (where applicable)",
                "Notarially certified deed"
            ),
            identityVerificationNote = "Address privacy mode is mandatory — DORJA never displays precise conflict-zone addresses without explicit user consent and security review.",
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "JO", displayName = "Jordan",
            currencyCode = "JOD", currencySymbol = "د.أ", formatLocaleTag = "ar-JO",
            primaryLanguages = listOf("ar-JO", "en"), rtlScripts = true,
            liveabilityFields = listOf(LiveabilityField.WATER_SUPPLY, LiveabilityField.BUILDING_CONDITION),
            documentTypes = listOf(
                DocumentTypeSpec("TABO_CERT", "Tabo Land Registry Certificate"),
                DocumentTypeSpec("TITLE_DEED", "Title Deed"),
                DocumentTypeSpec("TENANCY_CONTRACT", "Registered Tenancy Contract"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Real Estate Broker", "Notary", "Advocate"),
            disclosureChecklist = listOf(
                "Land registry (tabo) extract",
                "Registered tenancy contract",
                "Building permit compliance",
                "Utility deposit receipts"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        CountryProfile(
            iso2 = "MN", displayName = "Mongolia",
            currencyCode = "MNT", currencySymbol = "₮", formatLocaleTag = "mn-MN",
            primaryLanguages = listOf("mn-MN", "en"),
            liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION, LiveabilityField.RENOVATION_YEAR),
            documentTypes = listOf(
                DocumentTypeSpec("PROPERTY_CERT", "State Property Certificate"),
                DocumentTypeSpec("LAND_CERT", "Land Ownership Certificate"),
                DocumentTypeSpec("SALE_DEED", "Registered Sale Deed"),
                DocumentTypeSpec("OTHER", "Other Document")
            ),
            professionalRoles = listOf("Licensed Agent", "Notary (Notariat)"),
            disclosureChecklist = listOf(
                "State property certificate",
                "Land ownership certificate (where freehold)",
                "Registered sale deed",
                "Heating system inspection record (critical for Ulaanbaatar winters)"
            ),
            confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 6
        ),

        // ── Stage 99: Discovery required / Partner dependent / No launch ──

        CountryProfile(iso2 = "CH", displayName = "Switzerland", currencyCode = "CHF", currencySymbol = "Fr.", formatLocaleTag = "de-CH", primaryLanguages = listOf("de-CH", "fr-CH", "it-CH", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "NO", displayName = "Norway", currencyCode = "NOK", currencySymbol = "kr", formatLocaleTag = "nb-NO", primaryLanguages = listOf("nb-NO", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.BUILDING_CONDITION), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "LV", displayName = "Latvia", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "lv-LV", primaryLanguages = listOf("lv-LV", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.HEATING_COST, LiveabilityField.BUILDING_CONDITION), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "LT", displayName = "Lithuania", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "lt-LT", primaryLanguages = listOf("lt-LT", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.HEATING_COST, LiveabilityField.RENOVATION_YEAR), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "RS", displayName = "Serbia", currencyCode = "RSD", currencySymbol = "din", formatLocaleTag = "sr-RS", primaryLanguages = listOf("sr-RS", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "HR", displayName = "Croatia", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "hr-HR", primaryLanguages = listOf("hr-HR", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.BUILDING_CONDITION), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "AL", displayName = "Albania", currencyCode = "ALL", currencySymbol = "L", formatLocaleTag = "sq-AL", primaryLanguages = listOf("sq-AL", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "MD", displayName = "Moldova", currencyCode = "MDL", currencySymbol = "L", formatLocaleTag = "ro-MD", primaryLanguages = listOf("ro-MD", "ru-RU", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "AM", displayName = "Armenia", currencyCode = "AMD", currencySymbol = "֏", formatLocaleTag = "hy-AM", primaryLanguages = listOf("hy-AM", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "KR", displayName = "South Korea", currencyCode = "KRW", currencySymbol = "₩", formatLocaleTag = "ko-KR", primaryLanguages = listOf("ko-KR", "en"), liveabilityFields = listOf(LiveabilityField.BUILDING_CONDITION, LiveabilityField.BUILDING_AGE), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "MY", displayName = "Malaysia", currencyCode = "MYR", currencySymbol = "RM", formatLocaleTag = "ms-MY", primaryLanguages = listOf("ms-MY", "en", "zh"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "LB", displayName = "Lebanon", currencyCode = "LBP", currencySymbol = "L£", formatLocaleTag = "ar-LB", primaryLanguages = listOf("ar-LB", "fr-LB", "en"), rtlScripts = true, confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "SA", displayName = "Saudi Arabia", currencyCode = "SAR", currencySymbol = "ر.س", formatLocaleTag = "ar-SA", primaryLanguages = listOf("ar-SA", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "QA", displayName = "Qatar", currencyCode = "QAR", currencySymbol = "ر.ق", formatLocaleTag = "ar-QA", primaryLanguages = listOf("ar-QA", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "KW", displayName = "Kuwait", currencyCode = "KWD", currencySymbol = "د.ك", formatLocaleTag = "ar-KW", primaryLanguages = listOf("ar-KW", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "BH", displayName = "Bahrain", currencyCode = "BHD", currencySymbol = "د.ب", formatLocaleTag = "ar-BH", primaryLanguages = listOf("ar-BH", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "OM", displayName = "Oman", currencyCode = "OMR", currencySymbol = "ر.ع", formatLocaleTag = "ar-OM", primaryLanguages = listOf("ar-OM", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "BA", displayName = "Bosnia & Herzegovina", currencyCode = "BAM", currencySymbol = "KM", formatLocaleTag = "bs-BA", primaryLanguages = listOf("bs-BA", "hr-BA", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "XK", displayName = "Kosovo", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "sq-XK", primaryLanguages = listOf("sq-XK", "sr-XK", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "CY", displayName = "Cyprus", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "el-CY", primaryLanguages = listOf("el-CY", "tr-CY", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "ME", displayName = "Montenegro", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "sr-ME", primaryLanguages = listOf("sr-ME", "en"), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "LU", displayName = "Luxembourg", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "lb-LU", primaryLanguages = listOf("lb-LU", "fr-LU", "de-LU", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.ENERGY_ISSUER), confidence = AtlasConfidence.REGIONAL_EVIDENCE, launchStage = 99),
        CountryProfile(iso2 = "AT", displayName = "Austria", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "de-AT", primaryLanguages = listOf("de-AT", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.HEATING_COST), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "CZ", displayName = "Czechia", currencyCode = "CZK", currencySymbol = "Kč", formatLocaleTag = "cs-CZ", primaryLanguages = listOf("cs-CZ", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS, LiveabilityField.RENOVATION_YEAR), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "SK", displayName = "Slovakia", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "sk-SK", primaryLanguages = listOf("sk-SK", "en"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "SI", displayName = "Slovenia", currencyCode = "EUR", currencySymbol = "€", formatLocaleTag = "sl-SI", primaryLanguages = listOf("sl-SI", "en"), liveabilityFields = listOf(LiveabilityField.ENERGY_CLASS), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "MK", displayName = "North Macedonia", currencyCode = "MKD", currencySymbol = "den", formatLocaleTag = "mk-MK", primaryLanguages = listOf("mk-MK", "sq-MK", "en"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "AZ", displayName = "Azerbaijan", currencyCode = "AZN", currencySymbol = "₼", formatLocaleTag = "az-AZ", primaryLanguages = listOf("az-AZ", "en"), confidence = AtlasConfidence.PARTNER_DEPENDENT, launchStage = 99),
        CountryProfile(iso2 = "LA", displayName = "Laos", currencyCode = "LAK", currencySymbol = "₭", formatLocaleTag = "lo-LA", primaryLanguages = listOf("lo-LA", "en"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "KG", displayName = "Kyrgyzstan", currencyCode = "KGS", currencySymbol = "лв", formatLocaleTag = "ky-KG", primaryLanguages = listOf("ky-KG", "ru-RU"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "TJ", displayName = "Tajikistan", currencyCode = "TJS", currencySymbol = "SM", formatLocaleTag = "tg-TJ", primaryLanguages = listOf("tg-TJ", "ru-RU"), confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "CN", displayName = "China", currencyCode = "CNY", currencySymbol = "¥", formatLocaleTag = "zh-CN", primaryLanguages = listOf("zh-CN"), confidence = AtlasConfidence.PARTNER_DEPENDENT, launchStage = 99),
        CountryProfile(iso2 = "IL", displayName = "Israel", currencyCode = "ILS", currencySymbol = "₪", formatLocaleTag = "he-IL", primaryLanguages = listOf("he-IL", "ar-IL", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99),
        CountryProfile(iso2 = "MV", displayName = "Maldives", currencyCode = "MVR", currencySymbol = "Rf", formatLocaleTag = "dv-MV", primaryLanguages = listOf("dv-MV", "en"), rtlScripts = true, confidence = AtlasConfidence.DISCOVERY_REQUIRED, launchStage = 99)
    )

    /** Look up a profile; falls back to Bangladesh (the launch market). */
    fun profile(iso2: String): CountryProfile =
        profiles.firstOrNull { it.iso2.equals(iso2, ignoreCase = true) } ?: profiles.first()

    /** Countries the user may actually transact in today. */
    fun launchableProfiles(): List<CountryProfile> = profiles.filter { it.selectable }

    /** Countries available for selection in UI pickers. */
    fun selectableProfiles(): List<CountryProfile> = profiles.filter { it.launchStage <= 6 }
}