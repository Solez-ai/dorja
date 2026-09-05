package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

object Formatters {

    /**
     * Map a currency code to a locale that formats it naturally
     * (symbol + digit grouping). Unknown currencies fall back to US format.
     */
    private fun localeFor(currencyCode: String): Locale = when (currencyCode) {
        "BDT" -> Locale.forLanguageTag("bn-BD")
        "INR" -> Locale.forLanguageTag("en-IN")
        "NPR" -> Locale.forLanguageTag("ne-NP")
        "BTN" -> Locale.forLanguageTag("dz-BT")
        "EUR" -> Locale.forLanguageTag("de-DE")
        "JPY" -> Locale.forLanguageTag("ja-JP")
        "AED" -> Locale.forLanguageTag("ar-AE")
        else -> Locale.US
    }

    private fun formatAmount(amount: Int, currencyCode: String): String {
        val locale = localeFor(currencyCode)
        return try {
            val fmt = NumberFormat.getCurrencyInstance(locale)
            val symbolCurrency = Currency.getInstance(locale).currencyCode
            val formatted = fmt.format(amount.toLong())
            // If the locale's default currency differs from the requested one,
            // keep the ISO code visible so the amount is never ambiguous.
            if (symbolCurrency == currencyCode) formatted else "$currencyCode $formatted"
        } catch (e: Exception) {
            "$currencyCode ${NumberFormat.getNumberInstance(locale).format(amount.toLong())}"
        }
    }

    fun formatPrice(amount: Int, currencyCode: String, intent: String): String {
        val suffix = if (intent.equals("RENT", ignoreCase = true)) " / month" else ""
        return formatAmount(amount, currencyCode) + suffix
    }

    fun formatPriceShort(amount: Int, currencyCode: String): String =
        formatAmount(amount, currencyCode)

    // ── Deprecated Bangladesh-only shims (kept so the build never breaks mid-refactor) ──

    @Deprecated("Use the currency-aware overload", ReplaceWith("formatPrice(amount, \"BDT\", intent)"))
    fun formatPrice(amount: Int, intent: String): String = formatPrice(amount, "BDT", intent)

    @Deprecated("Use the currency-aware overload", ReplaceWith("formatPriceShort(amount, \"BDT\")"))
    fun formatPriceShort(amount: Int): String = formatPriceShort(amount, "BDT")

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