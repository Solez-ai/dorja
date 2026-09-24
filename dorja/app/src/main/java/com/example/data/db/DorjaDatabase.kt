package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppealRecord
import com.example.data.model.Conversation
import com.example.data.model.IdentityVerification
import com.example.data.model.LegalDocument
import com.example.data.model.Listing
import com.example.data.model.Message
import com.example.data.model.ProfessionalEndorsement
import com.example.data.model.Promise
import com.example.data.model.PropertyPassport
import com.example.data.model.Report
import com.example.data.model.ReportResponse
import com.example.data.model.RoomItem
import com.example.data.model.Scan
import com.example.data.model.ThirdPartyCheck
import com.example.data.model.User
import com.example.data.model.UserCredential
import com.example.data.model.Viewing
import androidx.room.TypeConverters

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
        LegalDocument::class,
        PropertyPassport::class,
        ProfessionalEndorsement::class,
        Report::class,
        ReportResponse::class,
        AppealRecord::class,
        IdentityVerification::class,
        UserCredential::class,
        ThirdPartyCheck::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
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
    abstract fun propertyPassportDao(): PropertyPassportDao
    abstract fun professionalEndorsementDao(): ProfessionalEndorsementDao
    abstract fun reportDao(): ReportDao
    abstract fun reportResponseDao(): ReportResponseDao
    abstract fun appealDao(): AppealDao
    abstract fun identityVerificationDao(): IdentityVerificationDao
    abstract fun userCredentialDao(): UserCredentialDao
    abstract fun thirdPartyCheckDao(): ThirdPartyCheckDao

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
                    // The app ships with zero seed data: a fresh install starts
                    // empty and every account is created by a real user through
                    // the signup flow.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
