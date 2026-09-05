package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Conversation
import com.example.data.model.Listing
import com.example.data.model.Message
import com.example.data.model.ProfessionalEndorsement
import com.example.data.model.Promise
import com.example.data.model.PropertyPassport
import com.example.data.model.RoomItem
import com.example.data.model.Scan
import com.example.data.model.User
import com.example.data.model.Viewing
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings ORDER BY createdAt DESC")
    fun getAllListings(): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getListingsByOwner(ownerId: String): Flow<List<Listing>>

    @Query("SELECT * FROM listings WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getListingsByOwnerSync(ownerId: String): List<Listing>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: String): Listing?

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    fun observeListingById(id: String): Flow<Listing?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: Listing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listings: List<Listing>)

    @Update
    suspend fun updateListing(listing: Listing)

    @Delete
    suspend fun deleteListing(listing: Listing)

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListingById(id: String)
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE listingId = :listingId ORDER BY ordinal ASC")
    fun getRoomsByListing(listingId: String): Flow<List<RoomItem>>

    @Query("SELECT * FROM rooms WHERE listingId = :listingId ORDER BY ordinal ASC")
    suspend fun getRoomsByListingSync(listingId: String): List<RoomItem>

    @Query("SELECT * FROM rooms WHERE id = :id LIMIT 1")
    suspend fun getRoomById(id: String): RoomItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<RoomItem>)

    @Update
    suspend fun updateRoom(room: RoomItem)

    @Delete
    suspend fun deleteRoom(room: RoomItem)

    @Query("DELETE FROM rooms WHERE listingId = :listingId")
    suspend fun deleteRoomsByListing(listingId: String)
}

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans WHERE listingId = :listingId ORDER BY createdAt DESC")
    fun getScansByListing(listingId: String): Flow<List<Scan>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: String): Scan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: Scan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scans: List<Scan>)

    @Query("DELETE FROM scans WHERE listingId = :listingId")
    suspend fun deleteScansByListing(listingId: String)

    @Query("DELETE FROM scans")
    suspend fun deleteAllScans()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE seekerUserId = :userId OR hostUserId = :userId ORDER BY lastMessageAt DESC")
    fun getConversationsForUser(userId: String): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): Conversation?

    @Query("SELECT * FROM conversations WHERE listingId = :listingId AND seekerUserId = :seekerId LIMIT 1")
    suspend fun getConversationByListingAndSeeker(listingId: String, seekerId: String): Conversation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conversations: List<Conversation>)

    @Update
    suspend fun updateConversation(conversation: Conversation)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<Message>)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

@Dao
interface PropertyPassportDao {
    @Query("SELECT * FROM property_passports WHERE listingId = :listingId LIMIT 1")
    fun observeByListing(listingId: String): Flow<PropertyPassport?>

    @Query("SELECT * FROM property_passports WHERE listingId = :listingId LIMIT 1")
    suspend fun getByListing(listingId: String): PropertyPassport?

    @Query("SELECT * FROM property_passports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PropertyPassport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(passport: PropertyPassport)

    @Query("DELETE FROM property_passports WHERE listingId = :listingId")
    suspend fun deleteByListing(listingId: String)
}

@Dao
interface ViewingDao {
    @Query("SELECT * FROM viewings ORDER BY startsAt ASC")
    fun getAllViewings(): Flow<List<Viewing>>

    @Query("SELECT * FROM viewings WHERE seekerId = :userId OR hostId = :userId ORDER BY startsAt ASC")
    fun getViewingsForUser(userId: String): Flow<List<Viewing>>

    @Query("SELECT * FROM viewings WHERE hostId = :hostId ORDER BY startsAt ASC")
    fun getViewingsForHost(hostId: String): Flow<List<Viewing>>

    @Query("SELECT * FROM viewings WHERE listingId = :listingId ORDER BY startsAt ASC")
    fun getViewingsByListing(listingId: String): Flow<List<Viewing>>

    @Query("SELECT * FROM viewings WHERE seekerId = :seekerId ORDER BY startsAt ASC")
    fun getViewingsForSeeker(seekerId: String): Flow<List<Viewing>>

    @Query("SELECT * FROM viewings WHERE id = :id LIMIT 1")
    suspend fun getViewingById(id: String): Viewing?

    @Query("SELECT * FROM viewings WHERE passToken = :token LIMIT 1")
    suspend fun getViewingByPassToken(token: String): Viewing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertViewing(viewing: Viewing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(viewings: List<Viewing>)

    @Update
    suspend fun updateViewing(viewing: Viewing)

    @Query("DELETE FROM viewings WHERE id = :id")
    suspend fun deleteViewingById(id: String)

    @Query("DELETE FROM viewings WHERE seekerId = :userId OR hostId = :userId")
    suspend fun deleteViewingsForUser(userId: String)

    @Query("DELETE FROM viewings")
    suspend fun deleteAllViewings()
}

@Dao
interface PromiseDao {
    @Query("SELECT * FROM promises WHERE listingId = :listingId ORDER BY createdAt ASC")
    fun getPromisesByListing(listingId: String): Flow<List<Promise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromise(promise: Promise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(promises: List<Promise>)

    @Update
    suspend fun updatePromise(promise: Promise)

    @Query("DELETE FROM promises WHERE listingId = :listingId")
    suspend fun deletePromisesByListing(listingId: String)

    @Query("DELETE FROM promises")
    suspend fun deleteAllPromises()
}

@Dao
interface ProfessionalEndorsementDao {
    @Query("SELECT * FROM professional_endorsements WHERE listingId = :listingId ORDER BY endorsedAt ASC")
    fun observeByListing(listingId: String): Flow<List<ProfessionalEndorsement>>

    @Query("SELECT * FROM professional_endorsements WHERE listingId = :listingId ORDER BY endorsedAt ASC")
    suspend fun getByListingSync(listingId: String): List<ProfessionalEndorsement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(endorsement: ProfessionalEndorsement)

    @Query("DELETE FROM professional_endorsements WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM professional_endorsements WHERE listingId = :listingId")
    suspend fun deleteByListing(listingId: String)

    @Query("DELETE FROM professional_endorsements")
    suspend fun deleteAll()
}

@Dao
interface LegalDocumentDao {
    @Query("SELECT * FROM legal_documents WHERE listingId = :listingId ORDER BY createdAt ASC")
    fun getLegalDocumentsByListing(listingId: String): Flow<List<com.example.data.model.LegalDocument>>

    @Query("SELECT * FROM legal_documents WHERE listingId = :listingId ORDER BY createdAt ASC")
    suspend fun getLegalDocumentsByListingSync(listingId: String): List<com.example.data.model.LegalDocument>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegalDocument(doc: com.example.data.model.LegalDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<com.example.data.model.LegalDocument>)

    @Delete
    suspend fun deleteLegalDocument(doc: com.example.data.model.LegalDocument)

    @Query("DELETE FROM legal_documents WHERE id = :id")
    suspend fun deleteLegalDocumentById(id: String)

    @Query("DELETE FROM legal_documents WHERE listingId = :listingId")
    suspend fun deleteLegalDocumentsByListing(listingId: String)

    @Query("SELECT * FROM legal_documents")
    suspend fun getAllLegalDocuments(): List<com.example.data.model.LegalDocument>

    @Query("DELETE FROM legal_documents")
    suspend fun deleteAllLegalDocuments()
}
