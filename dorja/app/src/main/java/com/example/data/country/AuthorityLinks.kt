package com.example.data.country

/**
 * Official public portals where a user can independently verify a
 * government-issued document — atlas §4 "Document and authority rails".
 *
 * Link-out only: DORJA never scrapes, proxies, or claims to have queried
 * these registries. Opening the portal is a user action; attaching the
 * resulting record is a user-supplied upload. The [GovernmentSourceCard]
 * exists so a green "Government source linked" badge is always backed by
 * an explicit source URL the viewer can check themselves.
 *
 * URLs are public landing pages only (state RERA portals, land-records
 * services). They may change; each entry is a hint, not a guarantee.
 */
object AuthorityLinks {

    /** A document type's verification rail for a given country. */
    data class Rail(
        val url: String,
        val label: String
    )

    // Official portals, by country (atlas references).
    private const val INDIA_RERA = "https://rera.mohua.gov.in"
    private const val INDIA_RERA_LABEL = "RERA portal (rera.mohua.gov.in)"
    private const val NEPAL_MEROKITTA = "https://www.merokitta.dos.gov.np/"
    private const val NEPAL_MEROKITTA_LABEL = "MeroKitta land records (merokitta.dos.gov.np)"
    private const val BHUTAN_NLCS = "https://www.nlcs.gov.bt/"
    private const val BHUTAN_NLCS_LABEL = "NLCS / eSakor (web.nlcs.gov.bt)"

    /** Per-country, per-document-type rails. Falls back to the country rail. */
    private val rails: Map<String, Map<String, Rail>> = mapOf(
        "IN" to mapOf(
            "RERA_PROJECT_REGISTRATION" to Rail(INDIA_RERA, INDIA_RERA_LABEL),
            "RERA_AGENT_REGISTRATION" to Rail(INDIA_RERA, INDIA_RERA_LABEL)
        ),
        "NP" to mapOf(
            "LALPURJA" to Rail(NEPAL_MEROKITTA, NEPAL_MEROKITTA_LABEL),
            "REVENUE_RECEIPT" to Rail(NEPAL_MEROKITTA, NEPAL_MEROKITTA_LABEL),
            "SALE_DEED" to Rail(NEPAL_MEROKITTA, NEPAL_MEROKITTA_LABEL)
        ),
        "BT" to mapOf(
            "LAND_TRANSACTION_CERTIFICATE" to Rail(BHUTAN_NLCS, BHUTAN_NLCS_LABEL),
            "THRAM_NUMBER" to Rail(BHUTAN_NLCS, BHUTAN_NLCS_LABEL),
            "SALE_DEED" to Rail(BHUTAN_NLCS, BHUTAN_NLCS_LABEL)
        )
    )

    /**
     * The verification rail for a document type, or `null` when DORJA knows
     * of no official public portal for it (e.g. Bangladesh's fragmented
     * sub-registry system — the atlas records no single public lookup).
     */
    fun railFor(countryCode: String, documentType: String): Rail? =
        rails[countryCode.uppercase()]?.get(documentType)

    /** Which document types in a country have an official public portal. */
    fun hasRail(countryCode: String, documentType: String): Boolean =
        railFor(countryCode, documentType) != null
}
