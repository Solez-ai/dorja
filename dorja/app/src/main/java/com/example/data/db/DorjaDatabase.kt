package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Listing::class,
        RoomItem::class,
        Scan::class,
        Conversation::class,
        Message::class,
        Viewing::class,
        Promise::class,
        LegalDocument::class
    ],
    version = 4,
    exportSchema = false
)
abstract class DorjaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun listingDao(): ListingDao
    abstract fun roomDao(): RoomDao
    abstract fun scanDao(): ScanDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun viewingDao(): ViewingDao
    abstract fun promiseDao(): PromiseDao
    abstract fun legalDocumentDao(): LegalDocumentDao

    companion object {
        @Volatile
        private var INSTANCE: DorjaDatabase? = null

        fun getDatabase(context: Context): DorjaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DorjaDatabase::class.java,
                    "dorja_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: DorjaDatabase) {
            val userDao = database.userDao()
            val listingDao = database.listingDao()
            val roomDao = database.roomDao()
            val legalDocumentDao = database.legalDocumentDao()

            // Initialize user accounts with clean defaults that the user can freely edit
            val hostUser = User(
                id = "u1",
                username = "host",
                displayName = "Rahim Ahmed (Host)",
                role = "SELLER",
                phone = "+880 1712-345678",
                email = "rahim.ahmed@dorja.bd",
                bio = "Verified property host in Dhaka.",
                location = "Gulshan 2, Dhaka"
            )
            val buyerUser = User(
                id = "u2",
                username = "buyer",
                displayName = "Tanvir Hasan (Buyer)",
                role = "BUYER",
                phone = "+880 1819-876543",
                email = "tanvir.hasan@dorja.bd",
                bio = "Verified property seeker looking for 3-bed apartments.",
                location = "Dhanmondi, Dhaka"
            )
            userDao.insertAll(listOf(hostUser, buyerUser))

            // Seed initial verified listings with rich photos and 3D scans
            val listing1 = Listing(
                id = "l1",
                ownerId = "u1",
                slug = "luxury-3bed-gulshan-avenue",
                title = "Luxury 3-Bed Apartment, Gulshan 2",
                intent = "RENT",
                propertyType = "APARTMENT",
                status = "ACTIVE",
                publicArea = "Road 71, Gulshan 2, Dhaka",
                exactAddress = "House 14, Road 71, Block NW(H), Gulshan 2",
                approximateLat = 23.7925,
                approximateLng = 90.4078,
                priceAmount = 85000,
                bedrooms = 3,
                bathrooms = 3,
                balconies = 2,
                sqft = 2150,
                tags = "Lift,Generator,24/7 Guard,Parking,South Facing",
                virtualTourUrl = "https://dorja.bd/tours/l1",
                coverPhotoUrl = null,
                description = "Architect-designed south-facing luxury flat with ample natural light, imported marble flooring, modular Italian kitchen fittings, and 100% full power backup generator.",
                hasScan = true,
                createdAt = System.currentTimeMillis()
            )

            val listing2 = Listing(
                id = "l2",
                ownerId = "u1",
                slug = "modern-duplex-dhanmondi",
                title = "Modern Duplex Suite, Dhanmondi 8/A",
                intent = "SALE",
                propertyType = "HOUSE",
                status = "ACTIVE",
                publicArea = "Road 8/A, Dhanmondi, Dhaka",
                exactAddress = "House 28, Road 8/A, Dhanmondi R/A",
                approximateLat = 23.7461,
                approximateLng = 90.3742,
                priceAmount = 32000000,
                bedrooms = 4,
                bathrooms = 4,
                balconies = 3,
                sqft = 3400,
                tags = "Corner Plot,Rooftop Garden,CCTV,Intercom,Solar Power",
                virtualTourUrl = "https://dorja.bd/tours/l2",
                coverPhotoUrl = null,
                description = "Prestige corner plot duplex with double-height ceiling living hall, private landscaped rooftop garden, and secure digital access control.",
                hasScan = true,
                createdAt = System.currentTimeMillis() - 86400000
            )

            listingDao.insertListing(listing1)
            listingDao.insertListing(listing2)

            // Seed rooms with 3D scan spatial data for listing 1
            val rooms1 = listOf(
                RoomItem(
                    id = "r_l1_living",
                    listingId = "l1",
                    roomType = "LIVING_ROOM",
                    displayName = "Formal Living Hall",
                    dimensions = "22 x 16 ft",
                    description = "Spacious living room with floor-to-ceiling windows and Italian porcelain tiles.",
                    ordinal = 0,
                    photoPath = null,
                    has3DScan = true,
                    panoramaData = "panorama_mesh_ready"
                ),
                RoomItem(
                    id = "r_l1_master",
                    listingId = "l1",
                    roomType = "BEDROOM",
                    displayName = "Master Suite",
                    dimensions = "18 x 15 ft",
                    description = "King size master bedroom with attached walk-in closet and private south balcony.",
                    ordinal = 1,
                    photoPath = null,
                    has3DScan = true,
                    panoramaData = "panorama_mesh_ready"
                ),
                RoomItem(
                    id = "r_l1_kitchen",
                    listingId = "l1",
                    roomType = "KITCHEN",
                    displayName = "Modular Gourmet Kitchen",
                    dimensions = "14 x 10 ft",
                    description = "Modern island counter with ducted range hood and granite countertops.",
                    ordinal = 2,
                    photoPath = null,
                    has3DScan = true,
                    panoramaData = "panorama_mesh_ready"
                ),
                RoomItem(
                    id = "r_l1_dining",
                    listingId = "l1",
                    roomType = "DINING_ROOM",
                    displayName = "Family Dining Space",
                    dimensions = "15 x 12 ft",
                    description = "Central dining room connecting directly to the main foyer.",
                    ordinal = 3,
                    photoPath = null,
                    has3DScan = false,
                    panoramaData = ""
                )
            )

            // Seed rooms for listing 2
            val rooms2 = listOf(
                RoomItem(
                    id = "r_l2_living",
                    listingId = "l2",
                    roomType = "LIVING_ROOM",
                    displayName = "Grand Double-Height Living Room",
                    dimensions = "26 x 18 ft",
                    description = "Double-height ceiling with bespoke chandelier and wide architectural glass.",
                    ordinal = 0,
                    photoPath = null,
                    has3DScan = true,
                    panoramaData = "panorama_mesh_ready"
                ),
                RoomItem(
                    id = "r_l2_master",
                    listingId = "l2",
                    roomType = "BEDROOM",
                    displayName = "Master Penthouse Bedroom",
                    dimensions = "20 x 16 ft",
                    description = "Ensuite with jacuzzi and direct access to terrace garden.",
                    ordinal = 1,
                    photoPath = null,
                    has3DScan = true,
                    panoramaData = "panorama_mesh_ready"
                )
            )

            roomDao.insertAll(rooms1)
            roomDao.insertAll(rooms2)

            // Legal Documents
            val doc1 = LegalDocument(
                id = "doc_1",
                listingId = "l1",
                documentType = "KHATIAN_PORCHA",
                documentTitle = "CS/SA/RS Khatian Record",
                documentNumber = "RS-9418/2012",
                issuingAuthority = "Gulshan AC Land Office",
                verificationStatus = "VERIFIED",
                notes = "Title chain verified with AC Land record."
            )
            val doc2 = LegalDocument(
                id = "doc_2",
                listingId = "l1",
                documentType = "RAJUK_APPROVAL",
                documentTitle = "RAJUK Plan Approval",
                documentNumber = "RAJUK/EM/2018-091",
                issuingAuthority = "RAJUK Zone 4",
                verificationStatus = "VERIFIED",
                notes = "Building plan approved for G+8 residential."
            )
            legalDocumentDao.insertAll(listOf(doc1, doc2))
        }
    }
}
