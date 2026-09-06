package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val role: String, // SELLER or BUYER
    val phone: String,
    val email: String = "",
    val bio: String = "",
    val location: String = "",
    val countryCode: String = "BD",
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "listings")
data class Listing(
    @PrimaryKey val id: String,
    val ownerId: String,
    val slug: String,
    val title: String,
    val intent: String, // RENT or SALE
    val propertyType: String, // APARTMENT, HOUSE, ROOM, SUBLET, OFFICE, SHOP, LAND
    val status: String = "ACTIVE", // ACTIVE, RENTED, SOLD
    val publicArea: String,
    val exactAddress: String = "",
    val approximateLat: Double? = null,
    val approximateLng: Double? = null,
    val priceAmount: Int,
    val currency: String,
    val countryCode: String = "BD",
    /** Sub-national subdivision code (e.g. "AE-DXB") — rules differ within countries. */
    val subnationalCode: String? = null,
    val bedrooms: Int = 3,
    val bathrooms: Int = 2,
    val balconies: Int = 1,
    val sqft: Int = 1250,
    val tags: String = "Lift,Generator,Security Guard", // Comma-separated
    val virtualTourUrl: String? = null, // External 3D Tour / Scan link
    val coverPhotoUrl: String? = null,
    val description: String = "",
    val hasScan: Boolean = false,
    // Phase 3 liveability/energy evidence — rendered only when the listing's
    // CountryProfile.liveabilityFields includes the matching field.
    val energyCertificateClass: String? = null,
    val energyCertificateIssuer: String? = null,
    val annualHeatingCost: Long? = null,
    val renovationYear: Int? = null,
    val powerBackup: String? = null,
    val waterSupply: String? = null,
    val floodRisk: String? = null,
    // Phase 4: Japan condition/age/disaster fields (profile-gated)
    val buildingCondition: String? = null,
    val buildingAgeYears: Int? = null,
    val disasterContext: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Professional handoff (atlas §8, PLAN.md Phase 4): a licensed professional
 * (takken agent, advocate, land investigator…) takes responsibility for one
 * section of a listing's evidence by signing/timestamping it. DORJA records
 * the endorsement; it never verifies the licence itself.
 */
@Entity(tableName = "professional_endorsements")
data class ProfessionalEndorsement(
    @PrimaryKey val id: String,
    val listingId: String,
    val section: String,             // e.g. OWNERSHIP, CONDITION, MEASUREMENTS, DISCLOSURE
    val professionalName: String,
    val licenceId: String,
    val roleLabel: String = "",      // e.g. "Licensed Takken Agent (宅建士)"
    val statement: String = "",
    val endorsedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "rooms")
data class RoomItem(
    @PrimaryKey val id: String,
    val listingId: String,
    val roomType: String, // LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, BALCONY, DINING_ROOM, OFFICE, OTHER
    val displayName: String,
    val dimensions: String = "",
    val description: String = "",
    val ordinal: Int = 0,
    val photoPath: String? = null,
    val scanId: String? = null,
    val has3DScan: Boolean = false,
    val panoramaData: String = ""
)

@Entity(tableName = "legal_documents")
data class LegalDocument(
    @PrimaryKey val id: String,
    val listingId: String,
    val documentType: String, // Codes come from CountryRegistry profiles (per country)
    val documentTitle: String,
    val documentNumber: String,
    val issuingAuthority: String,
    val issueDate: String = "2024",
    val verificationStatus: String = "VERIFIED", // Legacy binary field (kept for compat)
    val notes: String = "",
    // Atlas §3 evidence vocabulary — the honest status for every upload
    val evidenceLevel: String = EvidenceLevel.SELF_DECLARED.code,
    val checkedAt: Long? = null,
    val expiryState: String = EvidenceExpiry.UNKNOWN.code,
    val limitationNote: String = DEFAULT_SELF_DECLARED_NOTE,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "property_passports")
data class PropertyPassport(
    @PrimaryKey val id: String,
    val listingId: String,
    val countryCode: String = "BD",
    val addressFreeform: String = "",
    val approximateLat: Double? = null,
    val approximateLng: Double? = null,
    val createdByUserId: String = "",
    val expiryState: String = EvidenceExpiry.UNKNOWN.code,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scans")
data class Scan(
    @PrimaryKey val id: String,
    val listingId: String,
    val roomType: String,
    val roomName: String,
    val frameCount: Int = 0,
    val coveragePercent: Float = 100f,
    val planesDetected: Int = 0,
    val pointCloudSize: Int = 0,
    val durationMs: Long = 0,
    val scanDataJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderUserId: String,
    val body: String,
    val kind: String = "TEXT", // TEXT, SYSTEM
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val listingId: String,
    val seekerUserId: String,
    val hostUserId: String,
    val lastMessageAt: Long? = null,
    val lastMessageText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "viewings")
data class Viewing(
    @PrimaryKey val id: String,
    val listingId: String,
    val seekerId: String,
    val hostId: String,
    val status: String = "CONFIRMED", // CONFIRMED, CHECKED_IN, COMPLETED, CANCELLED
    val startsAt: Long,
    val endsAt: Long,
    val passToken: String,
    val isBuyerVerified: Boolean = true,
    val isHostVerified: Boolean = true,
    val isLocationVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "promises")
data class Promise(
    @PrimaryKey val id: String,
    val listingId: String,
    val category: String, // HANDOVER_DATE, UNIT_SIZE_OR_LAYOUT, PARKING, FIXTURES, AMENITIES
    val title: String,
    val originalText: String,
    val status: String = "PENDING", // PENDING, ACKNOWLEDGED, RESOLVED
    val evidenceNote: String = "Contract clause verified by developer registry.",
    val createdAt: Long = System.currentTimeMillis()
)

// ═════════════════════════════════════════════════════════════
//  Moderation & appeals (atlas §2 "Appeal and dispute record") —
//  Phase 5: neutral records, no silent winner, no opaque score.
// ═════════════════════════════════════════════════════════════

/** Why a listing or claim was reported (atlas §2 moderation rails). */
enum class ReportReason(val code: String, val label: String) {
    INACCURATE_CLAIM("INACCURATE_CLAIM", "Inaccurate claim or measurement"),
    SUSPECTED_FRAUD("SUSPECTED_FRAUD", "Suspected fraud or fake listing"),
    DOCUMENT_CONCERN("DOCUMENT_CONCERN", "Document authenticity concern"),
    SAFETY_CONCERN("SAFETY_CONCERN", "Viewing or property safety concern"),
    CONDUCT("CONDUCT", "Counterparty conduct"),
    OTHER("OTHER", "Other");

    companion object {
        fun fromCode(code: String?): ReportReason =
            entries.firstOrNull { it.code == code } ?: OTHER
    }
}

/**
 * A neutral report record. DORJA records the report, notifies the
 * counterparty, and tracks resolution state — it does not adjudicate
 * truth or punish anyone. Resolution options are recorded, not judged.
 */
@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val id: String,
    val listingId: String,
    val reportedByUserId: String,
    val reason: String,                 // ReportReason.code
    val details: String = "",
    val subjectClaim: String = "",      // what the reporting party says is true
    val state: String = "OPEN",         // OPEN, COUNTERPARTY_RESPONDED, RESOLVED, WITHDRAWN
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolutionNote: String = ""     // how it ended, recorded neutrally
)

/**
 * The counterparty's response to a report. Both claims are stored side by
 * side and displayed side by side — the evidence graph conflict view shows
 * both, with source and date, and never picks a silent winner.
 */
@Entity(tableName = "report_responses")
data class ReportResponse(
    @PrimaryKey val id: String,
    val reportId: String,
    val respondedByUserId: String,
    val counterClaim: String,           // what the responding party says is true
    val evidenceReference: String = "", // doc ID, promise ID, or free-text source
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Appeal of a moderation or resolution outcome (atlas §2). The appeal is
 * a record of disagreement with a resolution — it re-opens review, nothing more.
 */
@Entity(tableName = "appeals")
data class AppealRecord(
    @PrimaryKey val id: String,
    val reportId: String,
    val appealedByUserId: String,
    val grounds: String,
    val state: String = "SUBMITTED",    // SUBMITTED, UNDER_REVIEW, UPHELD, OVERTURNED
    val createdAt: Long = System.currentTimeMillis(),
    val decidedAt: Long? = null,
    val decisionNote: String = ""
)
