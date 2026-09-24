package com.example.data.repository

import com.example.data.country.CountryRegistry
import com.example.data.db.DorjaDatabase
import com.example.data.model.AppealRecord
import com.example.data.model.Conversation
import com.example.data.model.EVIDENCE_STALENESS_MS
import com.example.data.model.EvidenceExpiry
import com.example.data.model.EvidenceLevel
import com.example.data.model.EvidenceSummary
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DorjaRepository(private val database: DorjaDatabase, private val appContext: Context? = null) {
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
    private val reportDao = database.reportDao()
    private val reportResponseDao = database.reportResponseDao()
    private val appealDao = database.appealDao()
    private val identityVerificationDao = database.identityVerificationDao()
    private val userCredentialDao = database.userCredentialDao()
    private val thirdPartyCheckDao = database.thirdPartyCheckDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // No seed accounts: a fresh install starts signed-out and the
            // first real account is created through the auth screen.
            _currentUser.value = null
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

    /** Clears the active session. All accounts stay on the device. */
    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            _currentUser.value = null
        }
    }

    // ── Real auth: accounts are created with phone + password and verified ──

    fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Creates a real account (BUYER, SELLER, or the single allowed ADMIN).
     * Returns null on success or a user-facing error message.
     */
    suspend fun signUp(
        displayName: String,
        phone: String,
        password: String,
        role: String,
        countryCode: String
    ): String? {
        val normalizedPhone = phone.replace(" ", "")
        if (displayName.isBlank()) return "Please enter your full name."
        if (normalizedPhone.length < 6) return "Please enter a valid phone number."
        if (password.length < 6) return "Password must be at least 6 characters."
        if (userDao.getAllUsersSync().any { it.phone.replace(" ", "") == normalizedPhone }) {
            return "An account with this phone number already exists. Sign in instead."
        }
        if (role == "ADMIN" && userDao.countAdmins() > 0) {
            return "An admin account already exists. Only one admin is allowed."
        }
        val id = "u_" + UUID.randomUUID().toString().take(10)
        val user = User(
            id = id,
            username = displayName.lowercase().replace(Regex("[^a-z0-9]"), "").ifBlank { "user" } + id.takeLast(4),
            displayName = displayName.trim(),
            role = role,
            phone = phone.trim(),
            countryCode = countryCode
        )
        userDao.insertUser(user)
        val salt = UUID.randomUUID().toString()
        userCredentialDao.insert(
            UserCredential(userId = id, salt = salt, passwordHash = sha256(salt + password))
        )
        _currentUser.value = user
        return null
    }

    /** True when the platform's single admin account exists. */
    suspend fun hasAdmin(): Boolean = userDao.countAdmins() > 0

    /**
     * One-time bootstrap for the device's admin account. Requires a name,
     * phone and password; refuses if any admin already exists.
     */
    suspend fun createAdminAccount(
        phone: String,
        password: String,
        displayName: String,
        countryCode: String
    ): String? {
        if (userDao.countAdmins() > 0) {
            return "An admin account already exists on this device."
        }
        return signUp(
            displayName = displayName,
            phone = phone,
            password = password,
            role = "ADMIN",
            countryCode = countryCode
        )
    }

    /** Signs in with phone + password. Returns null on success or an error message. */
    suspend fun signIn(phone: String, password: String): String? {
        val normalizedPhone = phone.replace(" ", "")
        val user = userDao.getAllUsersSync().firstOrNull { it.phone.replace(" ", "") == normalizedPhone }
            ?: return "No account exists for this phone number. Create one below."
        val cred = userCredentialDao.getByUser(user.id)
            ?: return "This account has no password set. Delete it in Settings → Accounts and create it again."
        if (sha256(cred.salt + password) != cred.passwordHash) return "Incorrect password."
        _currentUser.value = user
        return null
    }

    /**
     * Deletes an account and everything it owns (listings, viewings, reports,
     * identity submissions). The single admin account cannot be deleted.
     */
    suspend fun deleteAccount(userId: String) {
        val target = userDao.getUserById(userId) ?: return
        if (target.role == "ADMIN") return
        listingDao.getListingsByOwnerSync(userId).forEach { deleteListing(it.id) }
        viewingDao.deleteViewingsForUser(userId)
        reportDao.deleteByReporter(userId)
        val ctx = appContext
        identityVerificationDao.getByUserSync(userId).forEach { v ->
            if (ctx != null) {
                v.documentImageFileName?.let { name ->
                    try { File(documentsDir(ctx), name).delete() } catch (_: Exception) {}
                }
            }
        }
        identityVerificationDao.deleteByUser(userId)
        userCredentialDao.deleteByUser(userId)
        userDao.deleteById(userId)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = userDao.getAdminUser() ?: userDao.getAllUsersSync().firstOrNull()
        }
    }

    // ── Identity verification (real submissions, admin-reviewed) ──

    /** The country's identity document label, e.g. NID, Aadhaar, Social Security. */
    fun identityCredentialName(countryCode: String): String =
        CountryRegistry.identityCredential(countryCode).shortName

    /**
     * Submits the active user's identity document for admin review. Only a
     * masked number and an integrity hash are stored — never the raw number.
     */
    suspend fun submitIdentityVerification(
        countryCode: String,
        documentNumber: String,
        holderName: String,
        documentImageUri: Uri? = null
    ): Result<Unit> {
        val user = _currentUser.value
            ?: return Result.failure(IllegalStateException("No active account"))
        val ctx = appContext
            ?: return Result.failure(IllegalStateException("Storage unavailable"))
        val cleanNumber = documentNumber.replace(" ", "")
        if (cleanNumber.length < 4) {
            return Result.failure(IllegalArgumentException("The document number looks too short."))
        }
        if (holderName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter the name exactly as on the document."))
        }
        val hash = sha256(countryCode.uppercase() + cleanNumber)
        val existing = identityVerificationDao.getByHash(hash)
        if (existing != null && existing.userId != user.id) {
            return Result.failure(IllegalStateException("This document is already registered to another account."))
        }
        if (existing != null && existing.userId == user.id && existing.status != "REJECTED") {
            return Result.failure(IllegalStateException("This document is already submitted and is under review or approved."))
        }
        var imageFileName: String? = null
        if (documentImageUri != null) {
            try {
                val dir = documentsDir(ctx)
                val name = "idv_" + UUID.randomUUID().toString().take(8) + ".img"
                val outFile = File(dir, name)
                ctx.contentResolver.openInputStream(documentImageUri)?.use { input ->
                    FileOutputStream(outFile).use { output -> input.copyTo(output) }
                }
                imageFileName = name
            } catch (_: Exception) {
                imageFileName = null
            }
        }
        identityVerificationDao.insert(
            IdentityVerification(
                id = "idv_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                countryCode = countryCode.uppercase(),
                documentKind = CountryRegistry.identityCredential(countryCode).shortName,
                holderName = holderName.trim(),
                documentNumberMasked = "•••• " + cleanNumber.takeLast(4),
                documentNumberHash = hash,
                documentImageFileName = imageFileName
            )
        )
        return Result.success(Unit)
    }

    fun observeVerificationsForUser(userId: String): Flow<List<IdentityVerification>> =
        identityVerificationDao.observeByUser(userId)

    fun observeAllVerifications(): Flow<List<IdentityVerification>> =
        identityVerificationDao.observeAll()

    fun getVerificationImageFile(v: IdentityVerification): File? {
        val ctx = appContext ?: return null
        val name = v.documentImageFileName ?: return null
        val file = File(documentsDir(ctx), name)
        return if (file.exists()) file else null
    }

    /** Admin decision on a submission. Only the admin account can review. */
    suspend fun reviewVerification(
        verificationId: String,
        approve: Boolean,
        note: String,
        reviewerId: String
    ): Result<Unit> {
        val reviewer = userDao.getUserById(reviewerId)
        if (reviewer?.role != "ADMIN") {
            return Result.failure(IllegalStateException("Only the admin account can review verifications."))
        }
        val v = identityVerificationDao.getById(verificationId)
            ?: return Result.failure(IllegalStateException("Submission not found."))
        identityVerificationDao.updateDecision(
            id = verificationId,
            status = if (approve) "APPROVED" else "REJECTED",
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = reviewerId,
            note = note.trim()
        )
        userDao.getUserById(v.userId)?.let { subject ->
            val updated = subject.copy(
                isIdentityVerified = approve,
                identityVerifiedAt = if (approve) System.currentTimeMillis() else null
            )
            userDao.updateUser(updated)
            if (_currentUser.value?.id == subject.id) _currentUser.value = updated
        }
        return Result.success(Unit)
    }

    // ── Third-party checks recorded during admin review (atlas §8) ──

    suspend fun addThirdPartyCheck(
        verificationId: String,
        subjectUserId: String,
        checkType: String,
        result: String,
        note: String,
        checkedByUserId: String
    ) {
        thirdPartyCheckDao.insert(
            ThirdPartyCheck(
                id = "tpc_" + UUID.randomUUID().toString().take(8),
                verificationId = verificationId,
                subjectUserId = subjectUserId,
                checkType = checkType,
                result = result,
                note = note,
                checkedByUserId = checkedByUserId
            )
        )
    }

    fun observeAllThirdPartyChecks(): Flow<List<ThirdPartyCheck>> =
        thirdPartyCheckDao.observeAll()

    suspend fun getThirdPartyChecksForVerificationSync(verificationId: String): List<ThirdPartyCheck> =
        thirdPartyCheckDao.getByVerificationSync(verificationId)

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

    /** Every account registered on this device (for the account switcher). */
    fun observeAllUsers(): Flow<List<User>> = userDao.getAllUsers()
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
        val ownerId = _currentUser.value?.id
            ?: throw IllegalStateException("No signed-in account")
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
        reportResponseDao.deleteByListing(listingId)
        appealDao.deleteByListing(listingId)
        reportDao.deleteByListing(listingId)
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
     * GDPR-style retention enforcement (Phase 3 minimisation rule): removes
     * every evidence row whose `retentionUntil` timestamp has passed. Evidence
     * is stored as metadata rows only (no binary files on disk), so removal is
     * a row delete. Called by [com.example.data.work.EvidenceRetentionWorker]
     * on a schedule and once at app start.
     *
     * @return the number of evidence rows removed.
     */
    suspend fun applyRetentionCutoff(now: Long = System.currentTimeMillis()): Int {
        val due = legalDocumentDao.getExpiredByRetention(now)
        for (doc in due) {
            legalDocumentDao.deleteLegalDocumentById(doc.id)
        }
        return due.size
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
        appealDao.deleteAll()
        reportResponseDao.deleteAll()
        reportDao.deleteAll()
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
        userCredentialDao.deleteAll()
        database.clearAllTables()
        _currentUser.value = null
    }

    // Rooms
    fun getRoomsByListing(listingId: String): Flow<List<RoomItem>> = roomDao.getRoomsByListing(listingId)
    suspend fun getRoomsByListingSync(listingId: String): List<RoomItem> = roomDao.getRoomsByListingSync(listingId)
    suspend fun getRoomById(roomId: String): RoomItem? = roomDao.getRoomById(roomId)

    suspend fun updateRoom3DScan(roomId: String, panoramaData: String = "") {
        val room = roomDao.getRoomById(roomId) ?: return
        if (room.panoramaData.isNotBlank() && room.panoramaData != panoramaData) {
            try {
                val oldJson = org.json.JSONObject(room.panoramaData)
                val oldPath = oldJson.optString("stitchedPanorama", "")
                if (oldPath.isNotBlank()) {
                    val oldFile = java.io.File(oldPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
            } catch (_: Exception) {}
        }
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

    /**
     * Evidence-gated listing status (atlas §3 + PLAN Phase 1 rule 4): a
     * listing shows "EVIDENCE VERIFIED" only when at least one attached
     * document is ISSUER_CONFIRMED, GOVERNMENT_SOURCE_LINKED, or
     * INDEPENDENTLY_INSPECTED and not expired/stale. Everything else is
     * "EVIDENCE PENDING" — an upload alone never earns the verified claim.
     */
    fun hasVerifiedEvidence(docs: List<LegalDocument>): Boolean {
        val now = System.currentTimeMillis()
        return docs.any { doc ->
            val level = EvidenceLevel.fromCode(doc.evidenceLevel)
            EvidenceLevel.isConfirmed(level) &&
                level != EvidenceLevel.EXPIRED &&
                doc.expiryState != EvidenceExpiry.EXPIRED.code &&
                !(doc.checkedAt != null && now - doc.checkedAt > EVIDENCE_STALENESS_MS)
        }
    }

    /** One-shot synchronous doc fetch for composable-level status derivation. */
    suspend fun getDocsForListings(listingIds: List<String>): Map<String, List<LegalDocument>> {
        val map = mutableMapOf<String, List<LegalDocument>>()
        for (id in listingIds) {
            map[id] = legalDocumentDao.getLegalDocumentsByListingSync(id)
        }
        return map
    }

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
// Removed unused sync method – keep async Flow version only
    // Removed unused sync method – keep async Flow version only
    suspend fun addLegalDocument(doc: LegalDocument) {
        legalDocumentDao.insertLegalDocument(doc)
    }

    /**
     * Appends a captured verification photo to the listing's gallery.
     * Photos are stored as newline-separated content Uris on the listing row.
     */
    suspend fun addCapturedPhoto(listingId: String, uri: String): Result<Unit> {
        return try {
            val listing = listingDao.getListingById(listingId) ?: return Result.failure(
                IllegalStateException("Listing not found: $listingId")
            )
            val updated = if (listing.galleryUris.isBlank()) uri
            else listing.galleryUris + "\n" + uri
            listingDao.updateListing(listing.copy(galleryUris = updated))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attaches a real document file. The picked Uri is copied into the app's
     * private documents directory so the record keeps working after restarts
     * (content-provider grant URIs expire when the process dies).
     */
    suspend fun addLegalDocument(
        listingId: String,
        uri: Uri,
        documentType: String = "UNKNOWN",
        documentNumber: String = "",
        issuingAuthority: String = "",
        documentTitle: String? = null
    ): Result<Unit> {
        return try {
            val ctx = appContext ?: return Result.failure(
                IllegalStateException("Repository has no application context")
            )
            val dir = documentsDir(ctx)
            val displayName = queryDisplayName(ctx, uri) ?: (uri.lastPathSegment ?: "document")
            val safeBase = displayName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "document" }
            val fileName = "ld_" + UUID.randomUUID().toString().take(8) + "_" + safeBase
            val outFile = File(dir, fileName)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            } ?: return Result.failure(IllegalStateException("Could not read the picked file"))
            val doc = LegalDocument(
                id = "ld_" + UUID.randomUUID().toString().take(8),
                listingId = listingId,
                documentType = documentType,
                documentTitle = documentTitle ?: displayName,
                documentNumber = documentNumber,
                issuingAuthority = issuingAuthority,
                localFilePath = outFile.absolutePath
            )
            legalDocumentDao.insertLegalDocument(doc)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Resolves the stored file for a document record, if one was attached. */
    fun getLegalDocumentFile(doc: LegalDocument): File? {
        val path = doc.localFilePath ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }


    suspend fun deleteLegalDocument(docId: String) {
        legalDocumentDao.getById(docId)?.let { doc ->
            doc.localFilePath?.let { path ->
                try { File(path).delete() } catch (_: Exception) {}
            }
            legalDocumentDao.deleteLegalDocumentById(docId)
        }
    }

    private fun documentsDir(ctx: Context): File {
        val dir = File(ctx.filesDir, "legal_documents")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun queryDisplayName(ctx: Context, uri: Uri): String? {
        return try {
            ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
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

    // ── Reports / responses / appeals (Phase 5, atlas §2 & §8) ────────────
    // Neutral records: DORJA records, notifies and displays. It does not
    // adjudicate truth and never picks a silent winner.

    fun observeReportsForListing(listingId: String): Flow<List<Report>> =
        reportDao.observeByListing(listingId)

    fun observeReportsByUser(userId: String): Flow<List<Report>> =
        reportDao.observeByReporter(userId)

    fun observeResponsesForReport(reportId: String): Flow<List<ReportResponse>> =
        reportResponseDao.observeByReport(reportId)

    fun observeAppealsForReport(reportId: String): Flow<List<AppealRecord>> =
        appealDao.observeByReport(reportId)

    suspend fun getReportsForListingSync(listingId: String): List<Report> =
        reportDao.getByListingSync(listingId)

    suspend fun getResponsesForReportSync(reportId: String): List<ReportResponse> =
        reportResponseDao.getByReportSync(reportId)

    suspend fun addReport(
        listingId: String,
        reportedByUserId: String,
        reason: String,
        details: String,
        subjectClaim: String
    ): Report {
        val report = Report(
            id = "rep_" + UUID.randomUUID().toString().take(8),
            listingId = listingId,
            reportedByUserId = reportedByUserId,
            reason = reason,
            details = details,
            subjectClaim = subjectClaim
        )
        reportDao.insert(report)
        return report
    }

    suspend fun addReportResponse(
        reportId: String,
        respondedByUserId: String,
        counterClaim: String,
        evidenceReference: String
    ) {
        reportResponseDao.insert(
            ReportResponse(
                id = "rr_" + UUID.randomUUID().toString().take(8),
                reportId = reportId,
                respondedByUserId = respondedByUserId,
                counterClaim = counterClaim,
                evidenceReference = evidenceReference
            )
        )
        // First response moves the report into the responded state.
        reportDao.getById(reportId)?.let { report ->
            if (report.state == "OPEN") {
                reportDao.update(report.copy(state = "COUNTERPARTY_RESPONDED"))
            }
        }
    }

    suspend fun resolveReport(reportId: String, resolutionNote: String) {
        reportDao.getById(reportId)?.let { report ->
            reportDao.update(
                report.copy(
                    state = "RESOLVED",
                    resolvedAt = System.currentTimeMillis(),
                    resolutionNote = resolutionNote
                )
            )
        }
    }

    suspend fun withdrawReport(reportId: String) {
        reportDao.getById(reportId)?.let { report ->
            reportDao.update(report.copy(state = "WITHDRAWN", resolvedAt = System.currentTimeMillis()))
        }
    }

    /** Reopens a resolved/withdrawn dispute after an appeal is overturned. */
    suspend fun reopenReport(reportId: String) {
        reportDao.getById(reportId)?.let { report ->
            reportDao.update(report.copy(state = "OPEN", resolvedAt = null, resolutionNote = ""))
        }
    }

    suspend fun addAppeal(
        reportId: String,
        appealedByUserId: String,
        grounds: String
    ): AppealRecord {
        val appeal = AppealRecord(
            id = "apl_" + UUID.randomUUID().toString().take(8),
            reportId = reportId,
            appealedByUserId = appealedByUserId,
            grounds = grounds
        )
        appealDao.insert(appeal)
        return appeal
    }

    suspend fun decideAppeal(reportId: String, appealId: String, upheld: Boolean, decisionNote: String) {
        appealDao.getByReportSync(reportId).firstOrNull { it.id == appealId }?.let { appeal ->
            appealDao.update(
                appeal.copy(
                    state = if (upheld) "UPHELD" else "OVERTURNED",
                    decidedAt = System.currentTimeMillis(),
                    decisionNote = decisionNote
                )
            )
        }
    }

    suspend fun resetAllData() {
        database.clearAllTables()
        _currentUser.value = null
    }
}
