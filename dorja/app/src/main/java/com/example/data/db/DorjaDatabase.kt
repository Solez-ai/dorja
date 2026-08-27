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
    version = 5,
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

            // Only create default user accounts — no fake listings, rooms, scans, or documents
            val hostUser = User(
                id = "u1",
                username = "shovro",
                displayName = "Shovro",
                role = "SELLER",
                phone = "+880 1712-345678",
                email = "",
                bio = "Verified Host on Dorja",
                location = "Dhaka, Bangladesh"
            )
            val buyerUser = User(
                id = "u2",
                username = "samin",
                displayName = "Samin Yeasar",
                role = "BUYER",
                phone = "+880 1812-345678",
                email = "",
                bio = "Property Seeker",
                location = "Dhaka, Bangladesh"
            )
            userDao.insertAll(listOf(hostUser, buyerUser))
        }
    }
}
