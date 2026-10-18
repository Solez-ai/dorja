package com.example.features

object AIFeatureFlag {

    var isAIEnabled = false
        private set

    fun initialize() {
        try {
            // Try-catch wrapper around Cactus SDK loading
            // isAIEnabled = CactusSDK.initialize()
            isAIEnabled = false // Graceful fallback
        } catch (e: Exception) {
            isAIEnabled = false
        }
    }

    fun getAIAssistant(): Any? {
        return if (isAIEnabled) {
            // Return Cactus assistant
            null
        } else {
            null
        }
    }
}
