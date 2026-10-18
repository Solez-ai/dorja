package com.example.features.passport

data class HandoverPassport(
    val passportId: String,
    val ownerName: String,
    val promises: List<Promise>,
    val documents: List<Document>,
    val createdAt: Long
)

data class Promise(
    val promiseId: String,
    val description: String,
    val dueDate: Long,
    val status: String,
    val assignedTo: String
)

data class Document(
    val documentId: String,
    val name: String,
    val url: String,
    val uploadedAt: Long
)

class PassportManager {
    fun createPassport(ownerName: String): HandoverPassport {
        return HandoverPassport(
            passportId = System.currentTimeMillis().toString(),
            ownerName = ownerName,
            promises = emptyList(),
            documents = emptyList(),
            createdAt = System.currentTimeMillis()
        )
    }

    fun addPromise(passport: HandoverPassport, description: String, dueDate: Long): HandoverPassport {
        val newPromise = Promise(
            promiseId = System.currentTimeMillis().toString(),
            description = description,
            dueDate = dueDate,
            status = "PENDING",
            assignedTo = ""
        )
        return passport.copy(promises = passport.promises + newPromise)
    }

    fun addDocument(passport: HandoverPassport, name: String, url: String): HandoverPassport {
        val newDoc = Document(
            documentId = System.currentTimeMillis().toString(),
            name = name,
            url = url,
            uploadedAt = System.currentTimeMillis()
        )
        return passport.copy(documents = passport.documents + newDoc)
    }
}
