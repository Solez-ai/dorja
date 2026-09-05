package com.example.data.model

/**
 * Evidence levels from the DORJA Eurasia Country Atlas (§3).
 *
 * The same understandable status vocabulary is used in every country.
 * A green visual treatment must NEVER imply legal title, criminal clearance,
 * or physical safety when DORJA has only received an upload.
 */
enum class EvidenceLevel(val code: String, val label: String) {
    SELF_DECLARED("SELF_DECLARED", "Self-declared"),
    COUNTERPARTY_CONFIRMED("COUNTERPARTY_CONFIRMED", "Counterparty confirmed"),
    ISSUER_CONFIRMED("ISSUER_CONFIRMED", "Issuer confirmed"),
    GOVERNMENT_SOURCE_LINKED("GOVERNMENT_SOURCE_LINKED", "Government source linked"),
    INDEPENDENTLY_INSPECTED("INDEPENDENTLY_INSPECTED", "Independently inspected"),
    EXPIRED("EXPIRED", "Expired"),
    DISPUTED("DISPUTED", "Disputed"),
    NOT_PROVIDED("NOT_PROVIDED", "Not provided");

    companion object {
        fun fromCode(code: String?): EvidenceLevel =
            entries.firstOrNull { it.code == code } ?: SELF_DECLARED

        /** Levels that may render a "confirmed"-style visual treatment. */
        fun isConfirmed(level: EvidenceLevel): Boolean = when (level) {
            ISSUER_CONFIRMED, GOVERNMENT_SOURCE_LINKED, INDEPENDENTLY_INSPECTED -> true
            else -> false
        }
    }
}

/** Default honest limitation note shown next to a self-declared upload. */
const val DEFAULT_SELF_DECLARED_NOTE =
    "Upload only — DORJA has not independently verified this document."

/** Expiry/validity states tracked per evidence item. */
enum class EvidenceExpiry(val code: String, val label: String) {
    VALID("VALID", "Valid"),
    EXPIRING("EXPIRING", "Expiring"),
    EXPIRED("EXPIRED", "Expired"),
    UNKNOWN("UNKNOWN", "Unknown")
}

/**
 * How long an evidence check is considered fresh before the atlas honesty
 * rules require it to be surfaced as stale (24 months, mirroring how EPC /
 * energy-certificate validity is commonly bounded in European markets).
 */
const val EVIDENCE_STALENESS_MS: Long = 24L * 30L * 24L * 60L * 60L * 1000L

/** Aggregated evidence-health snapshot for the current user's uploads. */
data class EvidenceSummary(
    val totalDocs: Int = 0,
    val confirmedDocs: Int = 0,
    val selfDeclaredDocs: Int = 0,
    val staleDocs: Int = 0,
    val expiredDocs: Int = 0
)