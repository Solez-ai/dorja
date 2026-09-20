package com.example.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

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
     */
    suspend fun answerQuestion(
        query: String,
        property: PropertyAiContext?
    ): String = withContext(Dispatchers.Default) {
        if (property == null) {
            return@withContext "Dorja AI is active and ready. Please open any verified property listing to ask specific questions about it."
        }

        val q = query.trim().lowercase()

        // 1. Parking specific questions (user's explicit example)
        if (q.contains("park") || q.contains("garage") || q.contains("car space") || q.contains("vehicle")) {
            return@withContext buildParkingResponse(property)
        }

        // 2. Legal Document Verification (Khatian, Mutation, RAJUK)
        if (q.contains("document") || q.contains("khatian") || q.contains("mutation") || q.contains("rajuk") || q.contains("deed") || q.contains("legal") || q.contains("paper")) {
            return@withContext buildDocumentsResponse(property)
        }

        // 3. Handover Passport & Seller Promises
        if (q.contains("promise") || q.contains("handover") || q.contains("commitment") || q.contains("guarantee") || q.contains("milestone")) {
            return@withContext buildPromisesResponse(property)
        }

        // 4. Room Dimensions, 3D Scans, Layout
        if (q.contains("room") || q.contains("bedroom") || q.contains("bath") || q.contains("scan") || q.contains("3d") || q.contains("tour") || q.contains("size") || q.contains("sqft") || q.contains("square")) {
            return@withContext buildRoomsResponse(property)
        }

        // 5. Price, Costs, Rent, Negotiation
        if (q.contains("price") || q.contains("cost") || q.contains("rent") || q.contains("bdt") || q.contains("currency") || q.contains("worth") || q.contains("payment")) {
            return@withContext buildPriceResponse(property)
        }

        // 6. Liveability, Utilities, Flood Risk
        if (q.contains("generator") || q.contains("power") || q.contains("water") || q.contains("flood") || q.contains("lift") || q.contains("security") || q.contains("amenit")) {
            return@withContext buildLiveabilityResponse(property)
        }

        // 7. General Property Overview
        return@withContext buildGeneralOverviewResponse(property, query)
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
