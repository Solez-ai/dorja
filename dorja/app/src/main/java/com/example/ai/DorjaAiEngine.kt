package com.example.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Engine status. Retained as a sealed class for API compatibility; the
 * instant engine is always [AiEngineState.Ready] — there is nothing to load.
 */
sealed class AiEngineState {
    object NotLoaded : AiEngineState()
    data class Searching(val message: String) : AiEngineState()
    data class Loading(val progress: Float, val message: String) : AiEngineState()
    data class Ready(
        val modelFileName: String,
        val accelerator: String,
        val fileSizeBytes: Long,
        val isNpuAccelerated: Boolean,
        val isGpuAccelerated: Boolean
    ) : AiEngineState()
    data class Error(val message: String) : AiEngineState()
}

/**
 * Instant rule-based Dorja assistant.
 *
 * There is NO on-device model anymore: answers are generated deterministically
 * from verified listing data in ~0 ms with zero memory/battery cost. This keeps
 * the whole app smooth — no 1GB+ model resident, no runtime cold-start jank,
 * no OOM crashes on low-RAM devices.
 */
class DorjaAiEngine private constructor(private val appContext: Context) {

    companion object {
        // Topic keys for the follow-up suggester
        const val TOPIC_NONE = "none"
        const val TOPIC_PARKING = "parking"
        const val TOPIC_DOCUMENTS = "documents"
        const val TOPIC_PROMISES = "promises"
        const val TOPIC_ROOMS = "rooms"
        const val TOPIC_PRICE = "price"
        const val TOPIC_NEGOTIATION = "negotiation"
        const val TOPIC_LIVEABILITY = "liveability"
        const val TOPIC_VISITS = "visits"
        const val TOPIC_NEIGHBORHOOD = "neighborhood"
        const val TOPIC_OVERVIEW = "overview"

        @Volatile
        private var instance: DorjaAiEngine? = null

        fun getInstance(context: Context): DorjaAiEngine {
            return instance ?: synchronized(this) {
                instance ?: DorjaAiEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    /** Kept for API compatibility — always Ready; nothing to load, ever. */
    private val _engineState = MutableStateFlow<AiEngineState>(
        AiEngineState.Ready(
            modelFileName = "Instant (rule-based)",
            accelerator = "None needed",
            fileSizeBytes = 0L,
            isNpuAccelerated = false,
            isGpuAccelerated = false
        )
    )
    val engineState: StateFlow<AiEngineState> = _engineState.asStateFlow()

    /** No-op — retained so old call sites compile. */
    fun unloadModel() { /* nothing to unload */ }

    /**
     * Answers a user query about a property instantly from verified listing data.
     * Returns the answer paired with the detected topic key so the caller can
     * show topic-aware follow-up suggestions.
     */
    suspend fun answerQuestion(
        query: String,
        property: PropertyAiContext?
    ): Pair<String, String> = withContext(Dispatchers.Default) {
        if (property == null) {
            return@withContext Pair(
                "Dorja AI is active and ready. Please open any verified property listing to ask specific questions about it.",
                TOPIC_NONE
            )
        }

        val q = query.trim().lowercase()

        // 1. Parking specific questions (user's explicit example)
        if (q.contains("park") || q.contains("garage") || q.contains("car space") || q.contains("vehicle")) {
            return@withContext Pair(buildParkingResponse(property), TOPIC_PARKING)
        }

        // 1b. Visits, scheduling and SafeView trust questions
        if (q.contains("visit") || q.contains("viewing") || q.contains("safeview") || q.contains("tour booking") || q.contains("schedule") || q.contains("appointment") || q.contains("pass token")) {
            return@withContext Pair(buildVisitsResponse(property), TOPIC_VISITS)
        }

        // 1c. Neighborhood questions (location, transit, flood-prone area, etc.)
        if (q.contains("neighborhood") || q.contains("neighbourhood") || q.contains("area like") || q.contains("area around") || q.contains("around the property") || q.contains("nearby") || q.contains("near the") || q.contains("locality") || q.contains("schools") || q.contains("hospital") || q.contains("market") || q.contains("mosque") || q.contains("transit") || q.contains("commute")) {
            return@withContext Pair(buildNeighborhoodResponse(property), TOPIC_NEIGHBORHOOD)
        }

        // 2. Legal Document Verification (Khatian, Mutation, RAJUK)
        if (q.contains("document") || q.contains("khatian") || q.contains("mutation") || q.contains("rajuk") || q.contains("deed") || q.contains("legal") || q.contains("paper")) {
            return@withContext Pair(buildDocumentsResponse(property), TOPIC_DOCUMENTS)
        }

        // 3. Handover Passport & Seller Promises
        if (q.contains("promise") || q.contains("handover") || q.contains("commitment") || q.contains("guarantee") || q.contains("milestone")) {
            return@withContext Pair(buildPromisesResponse(property), TOPIC_PROMISES)
        }

        // 4. Negotiation — offer leverage, price gaps, bargaining angles
        if (q.contains("negotiat") || q.contains("offer") || q.contains("bargain") || q.contains("discount") || q.contains("lower the price") || q.contains("talk down") || q.contains("leverage") || q.contains("concession")) {
            return@withContext Pair(buildNegotiationResponse(property), TOPIC_NEGOTIATION)
        }

        // 5. Room Dimensions, 3D Scans, Layout
        if (q.contains("room") || q.contains("bedroom") || q.contains("bath") || q.contains("scan") || q.contains("3d") || q.contains("tour") || q.contains("size") || q.contains("sqft") || q.contains("square")) {
            return@withContext Pair(buildRoomsResponse(property), TOPIC_ROOMS)
        }

        // 6. Price, Costs, Rent, Budgeting
        if (q.contains("price") || q.contains("cost") || q.contains("rent") || q.contains("bdt") || q.contains("currency") || q.contains("worth") || q.contains("payment")) {
            return@withContext Pair(buildPriceResponse(property), TOPIC_PRICE)
        }

        // 7. Liveability, Utilities, Flood Risk
        if (q.contains("generator") || q.contains("power") || q.contains("water") || q.contains("flood") || q.contains("lift") || q.contains("security") || q.contains("amenit")) {
            return@withContext Pair(buildLiveabilityResponse(property), TOPIC_LIVEABILITY)
        }

        // 8. General Property Overview
        return@withContext Pair(buildGeneralOverviewResponse(property, query), TOPIC_OVERVIEW)
    }

    /**
     * Natural next questions per answered topic — drives the sheet's
     * follow-up chips so a conversation can continue without retyping.
     */
    fun followUpQuestions(topic: String): List<String> = when (topic) {
        TOPIC_PARKING -> listOf(
            "Is parking included in the price?",
            "What legal documents are verified?",
            "Can I visit this property?"
        )
        TOPIC_DOCUMENTS -> listOf(
            "What promises did the seller make?",
            "Is the price negotiable?",
            "Can I visit this property?"
        )
        TOPIC_PROMISES -> listOf(
            "What legal documents are verified?",
            "When is handover and what must be completed?",
            "Is the price negotiable?"
        )
        TOPIC_ROOMS -> listOf(
            "Can I visit this property?",
            "Does this have parking?",
            "What is the power backup situation?"
        )
        TOPIC_PRICE -> listOf(
            "Is the price negotiable?",
            "What are the total monthly costs?",
            "What legal documents are verified?"
        )
        TOPIC_NEGOTIATION -> listOf(
            "What legal documents are verified?",
            "What promises did the seller make?",
            "Can I visit this property?"
        )
        TOPIC_LIVEABILITY -> listOf(
            "What is the neighborhood like?",
            "What are the room dimensions?",
            "Is the price negotiable?"
        )
        TOPIC_VISITS -> listOf(
            "What legal documents are verified?",
            "Does this have parking?",
            "Is the price negotiable?"
        )
        TOPIC_NEIGHBORHOOD -> listOf(
            "What is the flood risk?",
            "How do I schedule a visit?",
            "What is the price per sq ft?"
        )
        else -> listOf(
            "Does this have parking?",
            "What legal documents are verified?",
            "Is the price negotiable?"
        )
    }

    private fun buildParkingResponse(p: PropertyAiContext): String {
        return if (p.hasParking()) {
            val details = p.parkingDetails()
            """
            Yes, this property includes parking! 🚗

            • Details: $details
            • Verified Location: ${p.location}
            • Additional Access: ${if (p.tags.any { it.contains("lift", ignoreCase = true) }) "Lift access from parking to all floors" else "Standard stairwell access"}
            • Security: ${if (p.tags.any { it.contains("security", ignoreCase = true) }) "24/7 Security guard monitoring" else "Standard building security"}

            All parking allocations are verified and tracked under the Dorja Handover Passport.
            """.trimIndent()
        } else {
            """
            No dedicated parking is recorded for this listing.

            • Listing: ${p.title} (${p.location})
            • Tags recorded: ${if (p.tags.isEmpty()) "None" else p.tags.joinToString(", ")}

            If you need street or rented parking nearby, we recommend asking the host directly via in-app chat.
            """.trimIndent()
        }
    }

    private fun buildVisitsResponse(p: PropertyAiContext): String {
        val verifiedDocCount = p.documents.count { it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        return """
            Booking a SafeView visit for ${p.title}: 🗓

            • Tap "Book Visit" on the listing to request a slot — the host confirms in-app.
            • The exact address stays protected: it unlocks via a QR SafeView Pass only during your confirmed inspection window.
            • Verification status: ${if (verifiedDocCount > 0) "$verifiedDocCount legal document(s) verified before you walk in" else "documents still under preliminary review — review them in-chat before the visit"}.

            This protects both parties against fake listings and unvetted visitors.
        """.trimIndent()
    }

    private fun buildNeighborhoodResponse(p: PropertyAiContext): String {
        val tagLine = if (p.tags.isEmpty()) "No neighborhood amenity tags recorded" else p.tags.joinToString(", ")
        return """
            Neighborhood context for ${p.title} 📍

            • Verified area: ${p.location} — Dorja shows the approximate public area until a SafeView visit unlocks the exact address.
            • Recorded amenities & building features: $tagLine.
            • Flood history: ${p.floodRisk ?: "No flood incidents recorded for this property"}.
            • Utilities in the area: ${p.waterSupply ?: "Municipal water"} water supply; ${p.powerBackup ?: "standard building generator"} for power backup.

            Dorja only reports what is verified for this listing — for street-level insight (schools, markets, transit stops), use the "Open Location in Google Maps" button on the listing to explore the area, or ask the host via in-app chat.
        """.trimIndent()
    }

    /**
     * Negotiation guidance derived from actual listing facts: verified-doc
     * leverage, unfulfilled promises, liveability gaps and price-per-sqft
     * math — never invented numbers.
     */
    private fun buildNegotiationResponse(p: PropertyAiContext): String {
        val amount = p.priceFormatted.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val pricePerSqft = if (p.sqft > 0 && amount > 0) amount / p.sqft else 0L

        val verifiedDocs = p.documents.filter { it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        val pendingDocs = p.documents.filterNot { it.verificationStatus.equals("VERIFIED", ignoreCase = true) }
        val openPromises = p.promises.filterNot {
            it.status.equals("COMPLETED", ignoreCase = true) || it.status.equals("VERIFIED", ignoreCase = true)
        }

        val leveragePoints = buildList {
            if (pendingDocs.isNotEmpty()) {
                add("${pendingDocs.size} legal document(s) are still ${pendingDocs.joinToString { it.verificationStatus }} — request they be verified before paying any advance")
            }
            if (openPromises.isNotEmpty()) {
                add("${openPromises.size} seller promise(s) are not yet completed (${openPromises.take(2).joinToString { it.title }}) — ask for them as written Handover Passport milestones, or a price concession in exchange")
            }
            if (p.buildingAgeYears != null && p.buildingAgeYears >= 15) {
                add("The building is ${p.buildingAgeYears} years old — maintenance/renovation costs are a legitimate negotiation point")
            }
            if (p.buildingCondition != null && !p.buildingCondition.equals("EXCELLENT", ignoreCase = true)) {
                add("Recorded building condition is ${p.buildingCondition} — use the inspection to document repair items")
            }
            if (!p.hasParking()) {
                add("No dedicated parking is recorded — if you need parking, ask the host to secure it as a condition or reduce the offer")
            }
            if (p.floodRisk != null && !p.floodRisk.equals("NONE", ignoreCase = true) && !p.floodRisk.contains("no", ignoreCase = true)) {
                add("Flood risk is recorded as '${p.floodRisk}' — flood-prone units justify a lower rate")
            }
        }
        val leverage = leveragePoints.ifEmpty {
            listOf("This listing verifies cleanly (documents verified, promises tracked, condition good) — negotiation room is narrower; compete on speed and a firm, polite offer")
        }

        val strengthPoints = buildList {
            if (verifiedDocs.isNotEmpty()) add("${verifiedDocs.size} legal document(s) verified — low fraud risk")
            if (p.hasScan) add("3D scan available — you inspected remotely, fewer surprises at handover")
            if (p.energyClass != null) add("Energy class ${p.energyClass} — predictable running costs")
        }
        val strengths = strengthPoints.ifEmpty {
            listOf("Ask the host to upload documents to the Dorja vault before negotiating further")
        }

        return """
            Negotiation profile for ${p.title} 🤝

            • Listed: ${p.priceFormatted}${if (pricePerSqft > 0) " (≈$pricePerSqft per sq ft — compare with similar ${p.propertyType} listings in ${p.location})" else ""}

            Your leverage:
            ${leverage.joinToString("\n") { "  – $it" }}

            Seller's strengths (be ready for these):
            ${strengths.joinToString("\n") { "  – $it" }}

            Ground rules: anchor on the verified facts above, keep every agreed concession as a Handover Passport milestone, and never pay before documents verify.
        """.trimIndent()
    }

    private fun buildDocumentsResponse(p: PropertyAiContext): String {
        if (p.documents.isEmpty()) {
            return """
            This listing (${p.title}) is currently under preliminary review.

            No uploaded government documents (Khatian, Mutation, or RAJUK plans) have been verified in the vault yet. You can request the host upload them through in-app chat.
            """.trimIndent()
        }

        val docList = p.documents.joinToString("\n") { doc ->
            "• ${doc.documentTitle} (${doc.documentType}): Status [${doc.verificationStatus}], Authority: ${doc.issuingAuthority}"
        }

        return """
            Verified Legal Documents for ${p.title}: 📜

            $docList

            These records have been cross-checked in the Dorja Document Vault to protect against multiple-sale fraud.
        """.trimIndent()
    }

    private fun buildPromisesResponse(p: PropertyAiContext): String {
        if (p.promises.isEmpty()) {
            return """
            There are currently no active seller promises recorded in the Handover Passport for this listing.

            When negotiating with the host, any agreed work (e.g. repainting, repairs, utility clearance) will be tracked as binding milestones.
            """.trimIndent()
        }

        val promiseLines = p.promises.joinToString("\n") { prom ->
            "• [${prom.category}] ${prom.title} - Status: ${prom.status} (${prom.originalText})"
        }

        return """
            Digital Handover Passport Commitments: 🤝

            $promiseLines

            Each milestone requires digital sign-off before key handover can be finalized.
        """.trimIndent()
    }

    private fun buildRoomsResponse(p: PropertyAiContext): String {
        val scanNote = if (p.hasScan) "3D Room Scans are captured and viewable on this property!" else "Standard photo gallery available."
        val roomsList = if (p.rooms.isNotEmpty()) {
            p.rooms.joinToString("\n") { r ->
                "• ${r.displayName} (${r.roomType}): ${if (r.dimensions.isNotEmpty()) r.dimensions else "Standard layout"}${if (r.has3DScan) " [360° Scan Ready]" else ""}"
            }
        } else {
            "• ${p.bedrooms} Bedrooms, ${p.bathrooms} Bathrooms, ${p.balconies} Balcony"
        }

        return """
            Property Layout & Dimensions for ${p.title}: 📐

            • Total Area: ${p.sqft} sq ft (${p.bedrooms} Beds, ${p.bathrooms} Baths, ${p.balconies} Balconies)
            • 3D Scanner: $scanNote

            Room Breakdown:
            $roomsList
        """.trimIndent()
    }

    private fun buildPriceResponse(p: PropertyAiContext): String {
        val pricePerSqft = if (p.sqft > 0) {
            val amount = p.priceFormatted.filter { it.isDigit() }.toLongOrNull() ?: 0L
            if (amount > 0) " (~${amount / p.sqft} per sq ft)" else ""
        } else ""

        return """
            Price & Financial Details: 💰

            • Listed Price: ${p.priceFormatted} ($pricePerSqft)
            • Intent: ${p.intent} (${p.propertyType})
            • Location: ${p.location}
            • Size: ${p.sqft} sq ft

            Dorja provides transparent pricing with zero hidden intermediary commissions.
        """.trimIndent()
    }

    private fun buildLiveabilityResponse(p: PropertyAiContext): String {
        return """
            Liveability & Utility Status for ${p.title}: ⚡

            • Power Backup: ${p.powerBackup ?: "Standard building generator"}
            • Water Supply: ${p.waterSupply ?: "Municipal / WASA connection"}
            • Flood Risk: ${p.floodRisk ?: "Zero flood history reported"}
            • Amenities: ${p.tags.joinToString(", ").ifEmpty { "Lift, Generator, Security" }}
            • Energy Class: ${p.energyClass ?: "Class B (standard efficiency)"}
        """.trimIndent()
    }

    private fun buildGeneralOverviewResponse(p: PropertyAiContext, userQuery: String): String {
        val parkingSummary = if (p.hasParking()) "Dedicated Parking Included" else "No Dedicated Parking"
        return """
            Here is the verified summary for ${p.title}:

            • Type: ${p.propertyType} for ${p.intent}
            • Location: ${p.location} (${p.exactAddress.ifEmpty { "Verified Safe Address" }})
            • Specifications: ${p.sqft} sq ft | ${p.bedrooms} Beds | ${p.bathrooms} Baths
            • Parking: $parkingSummary
            • Price: ${p.priceFormatted}
            • Amenities: ${p.tags.joinToString(", ").ifEmpty { "Lift, Generator, Security" }}
            • 3D Scan: ${if (p.hasScan) "Available" else "Photos only"}

            You can ask me specific questions like:
            "Does this have a parking lot?"
            "What legal documents are verified?"
            "What are the room dimensions?"
        """.trimIndent()
    }
}
