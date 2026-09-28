package com.example.ai

import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.model.ProfessionalEndorsement
import com.example.data.model.Promise
import com.example.data.model.PropertyPassport
import com.example.data.model.RoomItem

/**
 * Encapsulates the complete metadata of a property to feed into
 * the Dorja on-device LiteRT AI engine.
 */
data class PropertyAiContext(
    val listingId: String,
    val title: String,
    val propertyType: String,
    val intent: String,
    val priceFormatted: String,
    val location: String,
    val exactAddress: String,
    val bedrooms: Int,
    val bathrooms: Int,
    val balconies: Int,
    val sqft: Int,
    val tags: List<String>,
    val description: String,
    val hasScan: Boolean,
    val energyClass: String?,
    val powerBackup: String?,
    val waterSupply: String?,
    val floodRisk: String?,
    val buildingCondition: String?,
    val buildingAgeYears: Int?,
    val rooms: List<RoomItem>,
    val documents: List<LegalDocument>,
    val promises: List<Promise>,
    val endorsements: List<ProfessionalEndorsement>
) {
    fun hasParking(): Boolean {
        val inTags = tags.any { it.contains("park", ignoreCase = true) || it.contains("garage", ignoreCase = true) }
        val inDesc = description.contains("park", ignoreCase = true) || description.contains("garage", ignoreCase = true)
        val inPromises = promises.any { it.category.equals("PARKING", ignoreCase = true) || it.title.contains("park", ignoreCase = true) }
        return inTags || inDesc || inPromises
    }

    fun parkingDetails(): String {
        val matchedPromises = promises.filter { it.category.equals("PARKING", ignoreCase = true) || it.title.contains("park", ignoreCase = true) }
        val matchedTags = tags.filter { it.contains("park", ignoreCase = true) || it.contains("garage", ignoreCase = true) }

        return when {
            matchedPromises.isNotEmpty() -> {
                val p = matchedPromises.first()
                "Yes, this property includes dedicated parking. Verified commitment: ${p.title} (${p.originalText}). Milestone status: ${p.status}."
            }
            matchedTags.isNotEmpty() -> {
                "Yes, parking is available. Tagged amenities: ${matchedTags.joinToString(", ")}."
            }
            description.contains("park", ignoreCase = true) -> {
                "Yes, parking is explicitly mentioned in the property description."
            }
            else -> {
                "No dedicated parking is explicitly recorded in this listing's verified attributes or seller promises."
            }
        }
    }

    companion object {
        fun from(
            listing: Listing,
            rooms: List<RoomItem> = emptyList(),
            passport: PropertyPassport? = null,
            documents: List<LegalDocument> = emptyList(),
            promises: List<Promise> = emptyList(),
            endorsements: List<ProfessionalEndorsement> = emptyList()
        ): PropertyAiContext {
            val tagList = listing.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return PropertyAiContext(
                listingId = listing.id,
                title = listing.title,
                propertyType = listing.propertyType,
                intent = listing.intent,
                priceFormatted = "${listing.currency} ${listing.priceAmount}",
                location = listing.publicArea,
                exactAddress = listing.exactAddress,
                bedrooms = listing.bedrooms,
                bathrooms = listing.bathrooms,
                balconies = listing.balconies,
                sqft = listing.sqft,
                tags = tagList,
                description = listing.description,
                hasScan = listing.hasScan,
                energyClass = listing.energyCertificateClass,
                powerBackup = listing.powerBackup,
                waterSupply = listing.waterSupply,
                floodRisk = listing.floodRisk,
                buildingCondition = listing.buildingCondition,
                buildingAgeYears = listing.buildingAgeYears,
                rooms = rooms,
                documents = documents,
                promises = promises,
                endorsements = endorsements
            )
        }
    }
}
