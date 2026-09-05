package com.example.data.repository

import com.example.data.country.CountryRegistry
import com.example.data.db.DorjaDatabase
import com.example.data.model.Conversation
import com.example.data.model.EVIDENCE_STALENESS_MS
import com.example.data.model.EvidenceExpiry
import com.example.data.model.EvidenceSummary
import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.model.Message
import com.example.data.model.ProfessionalEndorsement
import com.example.data.model.Promise
import com.example.data.model.PropertyPassport
import com.example.data.model.RoomItem
import com.example.data.model.Scan
import com.example.data.model.User
import com.example.data.model.Viewing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class DorjaRepository(private val database: DorjaDatabase) {
    private val userDao = database.userDao()
    private val listingDao = database.listingDao()
    private val roomDao = database.roomDao()
    private val scanDao = database.scanDao()
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val viewingDao = database.viewingDao()
    private val promiseDao = database.promiseDao()
    private val legalDocumentDao = database.legalDocumentDao()
    private val propertyPassportDao = database.propertyPassportDao()
    private val endorsementDao = database.professionalEndorsementDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val defaultUser = userDao.getUserById("u1")
            _currentUser.value = defaultUser
        }
    }

    // User lookups
    suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)

    // Active User Management
    fun switchUser(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = userDao.getUserById(userId)
            if (user != null) {
                _currentUser.value = user
            }
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        phone: String,
        email: String,
        location: String,
        bio: String,
        role: String,
        countryCode: String = "BD"
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName,
            phone = phone,
            email = email,
            location = location,
            bio = bio,
            role = role,
            countryCode = countryCode
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
    }

    /** Set the active user's transaction country without touching other profile fields. */
    fun setUserCountryCode(countryCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val current = _currentUser.value ?: return@launch
            val updated = current.copy(countryCode = countryCode)
            userDao.updateUser(updated)
            _currentUser.value = updated
        }
    }

    // Listings
    fun getAllListings(): Flow<List<Listing>> = listingDao.getAllListings()
    fun getListingsByOwner(ownerId: String): Flow<List<Listing>> = listingDao.getListingsByOwner(ownerId)
    suspend fun getListingsByOwnerSync(ownerId: String): List<Listing> = listingDao.getListingsByOwnerSync(ownerId)
    suspend fun getListingById(id: String): Listing? = listingDao.getListingById(id)
    fun observeListingById(id: String): Flow<Listing?> = listingDao.observeListingById(id)

    suspend fun createListingWithRooms(
        title: String,
        intent: String,
        propertyType: String,
        publicArea: String,
        exactAddress: String,
        priceAmount: Int,
        bedrooms: Int,
        bathrooms: Int,
        balconies: Int,
        sqft: Int,
        tags: String,
        virtualTourUrl: String?,
        coverPhotoUrl: String? = null,
        description: String,
        customRooms: List<RoomItem>,
        legalDocs: List<LegalDocument> = emptyList(),
        countryCode: String = "BD",
        subnationalCode: String? = null,
        energyCertificateClass: String? = null,
        energyCertificateIssuer: String? = null,
        annualHeatingCost: Long? = null,
        renovationYear: Int? = null,
        powerBackup: String? = null,
        waterSupply: String? = null,
        floodRisk: String? = null,
        buildingCondition: String? = null,
        buildingAgeYears: Int? = null,
        disasterContext: String? = null
    ): String {
        val ownerId = _currentUser.value?.id ?: "u1"
        val id = "l_" + UUID.randomUUID().toString().take(8)
        val slug = title.lowercase().replace(" ", "-").replace(",", "")
        val has3D = customRooms.any { it.has3DScan } || !virtualTourUrl.isNullOrBlank()

        val listing = Listing(
            id = id,
            ownerId = ownerId,
            slug = slug,
            title = title,
            intent = intent,
            propertyType = propertyType,
            status = "ACTIVE",
            publicArea = publicArea,
            exactAddress = exactAddress,
            approximateLat = 23.8041,
            approximateLng = 90.3468,
            priceAmount = priceAmount,
            currency = CountryRegistry.profile(countryCode).currencyCode,
            countryCode = countryCode,
            subnationalCode = subnationalCode,
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            balconies = balconies,
            sqft = sqft,
            tags = tags,
            virtualTourUrl = virtualTourUrl,
            coverPhotoUrl = coverPhotoUrl,
            description = description,
            hasScan = has3D,
            energyCertificateClass = energyCertificateClass,
            energyCertificateIssuer = energyCertificateIssuer,
            annualHeatingCost = annualHeatingCost,
            renovationYear = renovationYear,
            powerBackup = powerBackup,
            waterSupply = waterSupply,
            floodRisk = floodRisk,
            buildingCondition = buildingCondition,
            buildingAgeYears = buildingAgeYears,
            disasterContext = disasterContext,
            createdAt = System.currentTimeMillis()
        )
        listingDao.insertListing(listing)

        // Every listing gets a stable Property Passport (atlas §2): the identity
        // that survives re-posting and carries evidence across borders.
        ensurePropertyPassport(listing.id, listing)

        if (customRooms.isNotEmpty()) {
            val roomsToInsert = customRooms.mapIndexed { index, room ->
                room.copy(
                    id = if (room.id.isBlank()) "r_" + UUID.randomUUID().toString().take(8) else room.id,
                    listingId = id,
                    ordinal = index
                )
            }
            roomDao.insertAll(roomsToInsert)
        }

        if (legalDocs.isNotEmpty()) {
            val docsToInsert = legalDocs.map { doc ->
                doc.copy(
                    id = if (doc.id.isBlank()) "doc_" + UUID.randomUUID().toString().take(8) else doc.id,
                    listingId = id
                )
            }
            legalDocumentDao.insertAll(docsToInsert)
        }

        return id
    }

    suspend fun deleteListing(listingId: String) {
        listingDao.deleteListingById(listingId)
        propertyPassportDao.deleteByListing(listingId)
        roomDao.deleteRoomsByListing(listingId)
        scanDao.deleteScansByListing(listingId)
        promiseDao.deletePromisesByListing(listingId)
        legalDocumentDao.deleteLegalDocumentsByListing(listingId)
        endorsementDao.deleteByListing(listingId)
    }

    // Professional handoff endorsements (Phase 4)
    fun observeEndorsementsForListing(listingId: String): Flow<List<ProfessionalEndorsement>> =
        endorsementDao.observeByListing(listingId)

    suspend fun addEndorsement(
        listingId: String,
        section: String,
        professionalName: String,
        licenceId: String,
        roleLabel: String,
        statement: String
    ): ProfessionalEndorsement {
        val endorsement = ProfessionalEndorsement(
            id = "pe_" + UUID.randomUUID().toString().take(8),
            listingId = listingId,
            section = section,
            professionalName = professionalName,
            licenceId = licenceId,
            roleLabel = roleLabel,
            statement = statement
        )
        endorsementDao.insert(endorsement)
        return endorsement
    }

    suspend fun deleteEndorsement(id: String) {
        endorsementDao.deleteById(id)
    }

    suspend fun getEndorsementsForListingSync(listingId: String): List<ProfessionalEndorsement> =
        endorsementDao.getByListingSync(listingId)

    /**
     * Aggregate evidence-health snapshot across every legal document the user
     * can see (all local listings). Counts confirmed uploads, self-declared
     * uploads, stale checks (older than [EVIDENCE_STALENESS_MS]) and docs
     * explicitly marked EXPIRED (atlas §3 vocabulary).
     */
    suspend fun getEvidenceSummary(): EvidenceSummary {
        val docs = legalDocumentDao.getAllLegalDocuments()
        val now = System.currentTimeMillis()
        var confirmed = 0
        var selfDeclared = 0
        var stale = 0
        var expired = 0
        for (doc in docs) {
            val level = com.example.data.model.EvidenceLevel.fromCode(doc.evidenceLevel)
            if (com.example.data.model.EvidenceLevel.isConfirmed(level)) confirmed++
            if (level == com.example.data.model.EvidenceLevel.SELF_DECLARED) selfDeclared++
            if (doc.expiryState == EvidenceExpiry.EXPIRED.code || level == com.example.data.model.EvidenceLevel.EXPIRED) expired++
            val checked = doc.checkedAt
            if (checked != null && now - checked > EVIDENCE_STALENESS_MS) stale++
        }
        return EvidenceSummary(
            totalDocs = docs.size,
            confirmedDocs = confirmed,
            selfDeclaredDocs = selfDeclared,
            staleDocs = stale,
            expiredDocs = expired
        )
    }

    /**
     * Re-confirm every self-declared or stale document across the user's
     * listings: refreshes `checkedAt` to now and downgrades EXPIRED state back
     * to VALID. This is a *user re-attestation*, not an independent
     * verification — the evidence level itself is never raised here.
     */
    suspend fun reconfirmEvidence(): Int {
        val docs = legalDocumentDao.getAllLegalDocuments()
        val now = System.currentTimeMillis()
        var updated = 0
        for (doc in docs) {
            val level = com.example.data.model.EvidenceLevel.fromCode(doc.evidenceLevel)
            val isStale = doc.checkedAt != null && now - doc.checkedAt > EVIDENCE_STALENESS_MS
            val needsTouch = level == com.example.data.model.EvidenceLevel.SELF_DECLARED || isStale ||
                doc.expiryState == EvidenceExpiry.EXPIRED.code || doc.expiryState == EvidenceExpiry.UNKNOWN.code
            if (!needsTouch) continue
            legalDocumentDao.insertLegalDocument(
                doc.copy(
                    checkedAt = now,
                    expiryState = EvidenceExpiry.VALID.code
                )
            )
            updated++
        }
        return updated
    }

    /**
     * GDPR Art. 17 "right to erasure" — content scope: removes every listing
     * the user owns plus all conversations, messages, viewings, scans,
     * promises and legal documents. User profile rows are kept.
     */
    suspend fun deleteAllMyContent() {
        val userId = _currentUser.value?.id ?: return
        val myListings = listingDao.getListingsByOwnerSync(userId)
        for (listing in myListings) {
            listingDao.deleteListingById(listing.id)
            propertyPassportDao.deleteByListing(listing.id)
            roomDao.deleteRoomsByListing(listing.id)
            scanDao.deleteScansByListing(listing.id)
            promiseDao.deletePromisesByListing(listing.id)
        }
        legalDocumentDao.deleteAllLegalDocuments()
        messageDao.deleteAllMessages()
        conversationDao.deleteAllConversations()
        viewingDao.deleteAllViewings()
        endorsementDao.deleteAll()
    }

    /**
     * GDPR Art. 17 "right to erasure" — full scope: everything in
     * [deleteAllMyContent] plus the user profile rows themselves. The local
     * database is re-initialized with clean seed accounts afterwards so the
     * app remains usable.
     */
    suspend fun eraseAllMyData() {
        deleteAllMyContent()
        userDao.deleteAllUsers()
        database.clearAllTables()
        DorjaDatabase.populateInitialData(database)
        _currentUser.value = userDao.getUserById("u1")
    }

    // Rooms
    fun getRoomsByListing(listingId: String): Flow<List<RoomItem>> = roomDao.getRoomsByListing(listingId)
    suspend fun getRoomsByListingSync(listingId: String): List<RoomItem> = roomDao.getRoomsByListingSync(listingId)
    suspend fun getRoomById(roomId: String): RoomItem? = roomDao.getRoomById(roomId)

    suspend fun updateRoom3DScan(roomId: String, panoramaData: String = "") {
        val room = roomDao.getRoomById(roomId) ?: return
        val updated = room.copy(has3DScan = true, panoramaData = panoramaData)
        roomDao.updateRoom(updated)

        // Also ensure listing hasScan is true
        val listing = listingDao.getListingById(room.listingId)
        if (listing != null && !listing.hasScan) {
            listingDao.updateListing(listing.copy(hasScan = true))
        }
    }

    suspend fun addRoom(room: RoomItem) {
        roomDao.insertAll(listOf(room))
    }

    suspend fun addRoom(listingId: String, roomType: String, displayName: String, dimensions: String, description: String) {
        val room = RoomItem(
            id = "r_" + UUID.randomUUID().toString().take(8),
            listingId = listingId,
            roomType = roomType,
            displayName = displayName,
            dimensions = dimensions,
            description = description,
            ordinal = 0
        )
        roomDao.insertAll(listOf(room))
    }

    // Legal Documents
    fun getLegalDocumentsByListing(listingId: String): Flow<List<LegalDocument>> = legalDocumentDao.getLegalDocumentsByListing(listingId)

    // Property Passport
    fun observePassportForListing(listingId: String): Flow<PropertyPassport?> =
        propertyPassportDao.observeByListing(listingId)

    suspend fun getPassportForListing(listingId: String): PropertyPassport? =
        propertyPassportDao.getByListing(listingId)

    /** Create a passport for a listing if none exists yet (backfill-safe). */
    private suspend fun ensurePropertyPassport(listingId: String, listing: Listing) {
        if (propertyPassportDao.getByListing(listingId) == null) {
            propertyPassportDao.insert(
                PropertyPassport(
                    id = "pp_" + UUID.randomUUID().toString().take(8),
                    listingId = listingId,
                    countryCode = listing.countryCode,
                    addressFreeform = listing.exactAddress.ifBlank { listing.publicArea },
                    approximateLat = listing.approximateLat,
                    approximateLng = listing.approximateLng,
                    createdByUserId = listing.ownerId
                )
            )
        }
    }
    suspend fun getLegalDocumentsByListingSync(listingId: String): List<LegalDocument> = legalDocumentDao.getLegalDocumentsByListingSync(listingId)
    suspend fun addLegalDocument(doc: LegalDocument) {
        legalDocumentDao.insertLegalDocument(doc)
    }
    suspend fun deleteLegalDocument(docId: String) {
        legalDocumentDao.deleteLegalDocumentById(docId)
    }

    // Chat & Conversations
    fun getConversationsForUser(userId: String): Flow<List<Conversation>> = conversationDao.getConversationsForUser(userId)
    suspend fun getConversationById(id: String): Conversation? = conversationDao.getConversationById(id)
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>> = messageDao.getMessagesByConversation(conversationId)

    suspend fun getOrCreateConversation(listingId: String, seekerId: String, hostId: String): Conversation {
        val existing = conversationDao.getConversationByListingAndSeeker(listingId, seekerId)
        if (existing != null) return existing

        val newId = "c_" + UUID.randomUUID().toString().take(8)
        val newConv = Conversation(
            id = newId,
            listingId = listingId,
            seekerUserId = seekerId,
            hostUserId = hostId,
            lastMessageAt = System.currentTimeMillis(),
            lastMessageText = "Started inquiry for property."
        )
        conversationDao.insertConversation(newConv)
        return newConv
    }

    suspend fun sendMessage(conversationId: String, senderId: String, text: String, kind: String = "TEXT") {
        val msgId = "m_" + UUID.randomUUID().toString().take(8)
        val message = Message(
            id = msgId,
            conversationId = conversationId,
            senderUserId = senderId,
            body = text,
            kind = kind,
            createdAt = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)

        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            conversationDao.updateConversation(
                conv.copy(
                    lastMessageAt = System.currentTimeMillis(),
                    lastMessageText = if (kind == "SYSTEM") "Notice: $text" else text
                )
            )
        }
    }

    // Viewings & SafePass
    fun getViewingsForUser(userId: String): Flow<List<Viewing>> = viewingDao.getViewingsForUser(userId)
    fun getViewingsForHost(hostId: String): Flow<List<Viewing>> = viewingDao.getViewingsForHost(hostId)
    fun getViewingsForSeeker(seekerId: String): Flow<List<Viewing>> = viewingDao.getViewingsForSeeker(seekerId)
    suspend fun getViewingById(id: String): Viewing? = viewingDao.getViewingById(id)

    suspend fun requestViewing(listingId: String, seekerId: String, hostId: String, startsAt: Long, endsAt: Long): Viewing {
        val passToken = "PASS-SAFE-" + (1000..9999).random()
        val viewingId = "v_" + UUID.randomUUID().toString().take(8)
        val viewing = Viewing(
            id = viewingId,
            listingId = listingId,
            seekerId = seekerId,
            hostId = hostId,
            status = "CONFIRMED",
            startsAt = startsAt,
            endsAt = endsAt,
            passToken = passToken,
            isBuyerVerified = true,
            isHostVerified = true,
            isLocationVerified = true
        )
        viewingDao.insertViewing(viewing)

        val conv = getOrCreateConversation(listingId, seekerId, hostId)
        sendMessage(
            conv.id,
            "SYSTEM",
            "SafeView Viewing Pass Issued ($passToken). Scheduled inspection appointment registered.",
            "SYSTEM"
        )
        return viewing
    }

    suspend fun checkInViewing(viewingId: String) {
        val viewing = viewingDao.getViewingById(viewingId)
        if (viewing != null) {
            viewingDao.updateViewing(viewing.copy(status = "CHECKED_IN"))
        }
    }

    suspend fun cancelViewing(viewingId: String) {
        val viewing = viewingDao.getViewingById(viewingId)
        if (viewing != null) {
            viewingDao.updateViewing(viewing.copy(status = "CANCELLED"))
        }
    }

    // Promises
    fun getPromisesByListing(listingId: String): Flow<List<Promise>> = promiseDao.getPromisesByListing(listingId)
    fun getViewingsByListing(listingId: String): Flow<List<Viewing>> = viewingDao.getViewingsByListing(listingId)
    suspend fun addPromise(listingId: String, category: String, title: String, originalText: String, evidenceNote: String) {
        val promise = Promise(
            id = "p_" + UUID.randomUUID().toString().take(8),
            listingId = listingId,
            category = category,
            title = title,
            originalText = originalText,
            status = "PENDING",
            evidenceNote = evidenceNote
        )
        promiseDao.insertPromise(promise)
    }

    suspend fun resetAllData() {
        database.clearAllTables()
        DorjaDatabase.populateInitialData(database)
        _currentUser.value = userDao.getUserById("u1")
    }
}
