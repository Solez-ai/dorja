package com.example.features.chat

data class ChatMessage(
    val messageId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: String
)

class ChatService {
    private val messages = mutableListOf<ChatMessage>()

    fun sendMessage(senderId: String, content: String): ChatMessage {
        val message = ChatMessage(
            messageId = System.currentTimeMillis().toString(),
            senderId = senderId,
            content = content,
            timestamp = System.currentTimeMillis(),
            deliveryStatus = "SENT"
        )
        messages.add(message)
        return message
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun markAsRead(messageId: String) {
        val index = messages.indexOfFirst { it.messageId == messageId }
        if (index >= 0) {
            messages[index] = messages[index].copy(deliveryStatus = "READ")
        }
    }

    fun clearMessages() {
        messages.clear()
    }
}

class ChatViewModel {
    private val chatService = ChatService()
    val messages = mutableListOf<ChatMessage>()

    fun sendMessage(senderId: String, content: String) {
        val message = chatService.sendMessage(senderId, content)
        messages.add(message)
    }

    fun getUnreadCount(): Int {
        return messages.count { it.deliveryStatus == "SENT" }
    }
}
