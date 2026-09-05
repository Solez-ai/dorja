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
    val bedrooms: Int = 3,
    val bathrooms: Int = 2,
    val balconies: Int = 1,
    val sqft: Int = 1250,
    val tags: String = "Lift,Generator,Security Guard", // Comma-separated
    val virtualTourUrl: String? = null, // External 3D Tour / Scan link
    val coverPhotoUrl: String? = null,
    val description: String = "",
    val hasScan: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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
    val verificationStatus: String = "VERIFIED", // VERIFIED, PENDING_REVIEW, UNDER_SCRUTINY
    val notes: String = "",
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
