package com.example.ai

import com.cactus.CactusLM
import com.cactus.CactusCompletionParams
import com.cactus.CactusInitParams
import com.cactus.ChatMessage
import com.cactus.models.createTool
import com.cactus.models.ToolParameter
import com.example.data.model.Listing
import com.example.data.repository.DorjaRepository
import com.example.ui.util.Formatters
import kotlinx.coroutines.flow.first

/**
 * On-device assistant for DORJA, powered by the Cactus SDK (tool calling over
 * a small local model — Needle-class, no network needed at inference time).
 *
 * The model never answers property questions itself: it only picks a tool and
 * fills its arguments. Everything it returns is executed against the local
 * repository, so the answers are always the app's real data.
 */
class DorjaAssistant(private val repository: DorjaRepository) {

    private val lm = CactusLM(enableToolFiltering = true)
    private var ready = false

    /** Status of one assistant turn, surfaced to the UI. */
    sealed class Turn {
        /** The model picked a tool and it executed. */
        data class Result(val message: String) : Turn()

        /** The request matched no tool — the model returned no calls. */
        object Unmatched : Turn()

        /** Model or engine failure. */
        data class Error(val message: String) : Turn()
    }

    private val tools = listOf(
        createTool(
            name = "search_listings",
            description = "Search property listings. Filter by intent (RENT or SALE), " +
                "city or area text, max price and minimum bedrooms.",
            parameters = mapOf(
                "intent" to ToolParameter(
                    type = "string",
                    description = "RENT or SALE, or ALL for everything",
                    required = false,
                ),
                "area" to ToolParameter(
                    type = "string",
                    description = "City, area or neighborhood text to match",
                    required = false,
                ),
                "maxPrice" to ToolParameter(
                    type = "integer",
                    description = "Maximum monthly rent or sale price",
                    required = false,
                ),
                "minBedrooms" to ToolParameter(
                    type = "integer",
                    description = "Minimum number of bedrooms",
                    required = false,
                ),
            ),
        ),
        createTool(
            name = "get_listing_details",
            description = "Get full details of one property by its exact id: price, " +
                "rooms, area, description and status.",
            parameters = mapOf(
                "listingId" to ToolParameter(
                    type = "string",
                    description = "The listing id",
                    required = true,
                ),
            ),
        ),
        createTool(
            name = "count_listings",
            description = "Count how many active listings match a filter. " +
                "Use for questions like 'how many apartments for rent'.",
            parameters = mapOf(
                "intent" to ToolParameter(
                    type = "string",
                    description = "RENT or SALE, or ALL",
                    required = false,
                ),
                "area" to ToolParameter(
                    type = "string",
                    description = "City, area or neighborhood text to match",
                    required = false,
                ),
            ),
        ),
        createTool(
            name = "app_support",
            description = "Explain how the DORJA app works: identity verification, " +
                "listings, visits, 3D scans, comparing properties.",
            parameters = mapOf(
                "topic" to ToolParameter(
                    type = "string",
                    description = "Short topic of the question, e.g. verification, visits, scans",
                    required = true,
                ),
            ),
        ),
    )

    /**
     * Downloads the model on first use (a few hundred MB for qwen3-0.6, one
     * time) and initializes it. Safe to call repeatedly; work is skipped when
     * already ready.
     */
    suspend fun ensureReady(): Boolean {
        if (ready && lm.isLoaded()) return true
        return try {
            if (!com.cactus.CactusModelManager.isModelDownloaded(MODEL)) {
                lm.downloadModel(MODEL)
            }
            lm.initializeModel(CactusInitParams(model = MODEL, contextSize = 2048))
            ready = true
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isReady(): Boolean = ready && lm.isLoaded()

    /** Runs one user turn through the tool-calling loop. */
    suspend fun ask(userText: String): Turn {
        if (!ensureReady()) {
            return Turn.Error("Assistant model is not available yet.")
        }
        return try {
            val result = lm.generateCompletion(
                messages = listOf(
                    ChatMessage(
                        content = SYSTEM_PROMPT,
                        role = "system",
                    ),
                    ChatMessage(
                        content = userText,
                        role = "user",
                    ),
                ),
                params = CactusCompletionParams(
                    tools = tools,
                    maxTokens = 200,
                ),
            )
            val call = result?.toolCalls?.firstOrNull()
            if (call == null) {
                Turn.Unmatched
            } else {
                Turn.Result(execute(call.name, call.arguments))
            }
        } catch (e: Exception) {
            Turn.Error(e.message ?: "Assistant failed.")
        }
    }

    /** Executes a model-chosen tool against the local repository. */
    private suspend fun execute(name: String, args: Map<String, String>): String = when (name) {
        "search_listings" -> {
            val listings = filterListings(
                intent = args["intent"],
                area = args["area"],
                maxPrice = args["maxPrice"]?.toIntOrNull(),
                minBedrooms = args["minBedrooms"]?.toIntOrNull(),
            )
            if (listings.isEmpty()) {
                "No listings match that search."
            } else {
                listings.take(5).joinToString("\n") { describeBrief(it) } +
                    if (listings.size > 5) "\n…and ${listings.size - 5} more." else ""
            }
        }
        "get_listing_details" -> {
            val listing = repository.getListingById(args["listingId"].orEmpty())
            if (listing == null) "Listing not found." else describeFull(listing)
        }
        "count_listings" -> {
            val listings = filterListings(intent = args["intent"], area = args["area"])
            "${listings.size} active listing(s) match."
        }
        "app_support" -> supportAnswer(args["topic"].orEmpty())
        else -> "That request is not supported yet."
    }

    private suspend fun filterListings(
        intent: String?,
        area: String?,
        maxPrice: Int? = null,
        minBedrooms: Int? = null,
    ): List<Listing> {
        val all = repository.getAllListings().first()
            .filter { it.status.equals("ACTIVE", ignoreCase = true) }
        return all.filter { listing ->
            val intentOk = intent.isNullOrBlank() || intent.equals("ALL", true) ||
                listing.intent.equals(intent, ignoreCase = true)
            val areaOk = area.isNullOrBlank() ||
                listing.publicArea.contains(area, ignoreCase = true) ||
                listing.title.contains(area, ignoreCase = true) ||
                listing.countryCode.contains(area, ignoreCase = true)
            val priceOk = maxPrice == null || listing.priceAmount <= maxPrice
            val bedOk = minBedrooms == null || listing.bedrooms >= minBedrooms
            intentOk && areaOk && priceOk && bedOk
        }
    }

    private fun describeBrief(l: Listing): String =
        "${l.title} — ${l.publicArea} · ${l.intent} · ${Formatters.formatPrice(l.priceAmount, l.currency, l.intent)} · ${l.bedrooms} bed / ${l.bathrooms} bath · id=${l.id}"

    private fun describeFull(l: Listing): String = buildString {
        appendLine(l.title)
        appendLine("${l.publicArea}, ${l.countryCode} · ${l.propertyType} · ${l.intent}")
        appendLine(Formatters.formatPrice(l.priceAmount, l.currency, l.intent))
        appendLine("${l.bedrooms} bedrooms · ${l.bathrooms} bathrooms · ${l.sqft} sqft")
        if (l.hasScan) appendLine("3D scan available.")
        if (l.description.isNotBlank()) appendLine(l.description)
        append("id=${l.id}")
    }

    private fun supportAnswer(topic: String): String {
        val t = topic.lowercase()
        return when {
            "verif" in t -> "Identity verification: submit your documents from the Account " +
                "screen. An admin reviews them; once approved, a verified badge marks your " +
                "account and listings across the app."
            "visit" in t || "book" in t -> "Visits: open a property, pick a slot and request a " +
                "visit. Hosts approve from their Visits tab, and both sides get a QR viewing " +
                "pass for the appointment."
            "scan" in t || "3d" in t -> "3D scans: hosts capture rooms with the Room Scanner; " +
                "buyers can walk the scan from the property page or the tour viewer."
            "compar" in t -> "Compare: buyers open two verified properties side by side with " +
                "the compare arrows button on a property page."
            else -> "Ask me about identity verification, listings, visits, 3D scans or " +
                "comparing properties."
        }
    }

    fun unload() {
        try {
            lm.unload()
        } catch (_: Exception) {
        }
        ready = false
    }

    companion object {
        private const val MODEL = "qwen3-0.6"

        private val SYSTEM_PROMPT = """
            You are DORJA's property assistant. Pick exactly one tool for the
            user's request and fill its arguments. If no tool matches the
            request, return no tool call. Never invent listing ids; only use
            ids from earlier results.
        """.trimIndent()
    }
}
