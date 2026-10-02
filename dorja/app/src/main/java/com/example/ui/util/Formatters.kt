package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Formatters {

    /**
     * Visit-availability windows are stored as UTC-day millis (midnight UTC of
     * the chosen calendar day) — the same convention Material3's DatePicker
     * returns in `selectedDateMillis`. Keeping days timezone-free makes host
     * windows and buyer picks directly comparable.
     */
    private val UtcZone: TimeZone = TimeZone.getTimeZone("UTC")

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

    private fun formatAmount(amount: Int, currencyCode: String): String =
        formatAmount(amount.toLong(), currencyCode)

    private fun formatAmount(amount: Long, currencyCode: String): String {
        val locale = localeFor(currencyCode)
        return try {
            val fmt = NumberFormat.getCurrencyInstance(locale)
            val symbolCurrency = Currency.getInstance(locale).currencyCode
            val formatted = fmt.format(amount)
            // If the locale's default currency differs from the requested one,
            // keep the ISO code visible so the amount is never ambiguous.
            if (symbolCurrency == currencyCode) formatted else "$currencyCode $formatted"
        } catch (e: Exception) {
            "$currencyCode ${NumberFormat.getNumberInstance(locale).format(amount)}"
        }
    }

    /** Currency-aware amount formatting for Long values (e.g. annual heating costs). */
    fun formatAmountLong(amount: Long, currencyCode: String): String = formatAmount(amount, currencyCode)

    fun formatPrice(amount: Int, currencyCode: String, intent: String): String {
        val suffix = if (intent.equals("RENT", ignoreCase = true)) " / month" else ""
        return formatAmount(amount, currencyCode) + suffix
    }

    fun formatPriceShort(amount: Int, currencyCode: String): String =
        formatAmount(amount, currencyCode)

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

    /** Format a UTC-day millis (host window / buyer pick) as a readable day. */
    fun formatDateUtcDay(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = UtcZone
        return sdf.format(Date(millis))
    }

    /**
     * Today as a UTC-day millis (the local calendar date stamped at 00:00 UTC),
     * matching how DatePicker marks "today" on this device.
     */
    fun todayUtcDayMillis(): Long {
        val local = Calendar.getInstance()
        val utc = Calendar.getInstance(UtcZone)
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        return utc.timeInMillis
    }

    /**
     * Combine a UTC-day millis with a local wall-clock hour into a concrete
     * local timestamp. The chosen calendar day is preserved; only the hour is
     * applied on the device's timezone (visit slots are local appointments).
     */
    fun localMillisFor(utcDayMillis: Long, hour: Int): Long {
        val day = Calendar.getInstance(UtcZone).apply { timeInMillis = utcDayMillis }
        val local = Calendar.getInstance()
        local.clear()
        local.set(
            day.get(Calendar.YEAR),
            day.get(Calendar.MONTH),
            day.get(Calendar.DAY_OF_MONTH),
            hour,
            0,
            0
        )
        return local.timeInMillis
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