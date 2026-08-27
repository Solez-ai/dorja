package com.example.data.repository

import com.example.data.db.DorjaDatabase
import com.example.data.model.Conversation
import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.model.Message
import com.example.data.model.Promise
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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val defaultUser = userDao.getUserById("u1")
            _currentUser.value = defaultUser
        }
    }

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
        role: String
    ) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            displayName = displayName,
            phone = phone,
            email = email,
            location = location,
            bio = bio,
            role = role
        )
        userDao.updateUser(updated)
        _currentUser.value = updated
    }

    // Listings
    fun getAllListings(): Flow<List<Listing>> = listingDao.getAllListings()
    fun getListingsByOwner(ownerId: String): Flow<List<Listing>> = listingDao.getListingsByOwner(ownerId)
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
        legalDocs: List<LegalDocument> = emptyList()
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
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            balconies = balconies,
            sqft = sqft,
            tags = tags,
            virtualTourUrl = virtualTourUrl,
            coverPhotoUrl = coverPhotoUrl,
            description = description,
            hasScan = has3D,
            createdAt = System.currentTimeMillis()
        )
        listingDao.insertListing(listing)

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
        roomDao.deleteRoomsByListing(listingId)
        legalDocumentDao.deleteLegalDocumentsByListing(listingId)
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
    suspend fun getLegalDocumentsByListingSync(listingId: String): List<LegalDocument> = legalDocumentDao.getLegalDocumentsByListingSync(listingId)
    suspend fun addLegalDocument(doc: LegalDocument) {
        legalDocumentDao.insertLegalDocument(doc)
    }
    suspend fun deleteLegalDocument(docId: String) {
        legalDocumentDao.deleteLegalDocumentById(docId)
    }

    // Chat & Conversations
    fun getConversationsForUser(userId: String): Flow<List<Conversation>> = conversationDao.getConversationsForUser(userId)
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
