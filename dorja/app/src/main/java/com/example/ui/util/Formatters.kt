package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val bdtFormat = NumberFormat.getNumberInstance(Locale("en", "BD"))

    fun formatPrice(amount: Int, intent: String): String {
        val formatted = bdtFormat.format(amount)
        return if (intent.equals("RENT", ignoreCase = true)) {
            "BDT $formatted / month"
        } else {
            "BDT $formatted"
        }
    }

    fun formatPriceShort(amount: Int): String {
        return "BDT " + bdtFormat.format(amount)
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "DJ"
        }
    }
}
