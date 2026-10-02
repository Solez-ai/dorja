package com.example

import com.example.ui.util.Formatters
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * JVM tests for the availability-window / visit-slot time math (pure
 * java.util, no Android framework). Each test pins the device default
 * timezone, so the UTC-day conventions behind SafeView scheduling hold on
 * any device clock — western DST, eastern fixed-offset, anything else.
 *
 * Conventions under test (see Formatters):
 *  - availability days are stored as UTC-midnight millis of the calendar day;
 *  - visit slots combine a UTC day with a LOCAL hour, preserving the picked
 *    calendar day on the device's clock.
 */
class FormattersTimeTest {

    private val defaultTz = TimeZone.getDefault()
    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        // Deterministic weekday/month formatting regardless of CI locale.
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTz)
        Locale.setDefault(defaultLocale)
    }

    private fun useZone(id: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(id))
    }

    private fun utcCal(year: Int, month0: Int, day: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month0, day, 0, 0, 0)
        }

    @Test
    fun todayUtcDayMillis_isUtcMidnightOfTheLocalDate() {
        useZone("America/New_York")
        val result = Formatters.todayUtcDayMillis()

        // Aligned to a whole UTC day.
        assertEquals(0L, result % 86_400_000L)

        // The UTC date it encodes equals the device's LOCAL calendar date.
        val local = Calendar.getInstance()
        val asUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = result
        }
        assertEquals(local.get(Calendar.YEAR), asUtc.get(Calendar.YEAR))
        assertEquals(local.get(Calendar.MONTH), asUtc.get(Calendar.MONTH))
        assertEquals(local.get(Calendar.DAY_OF_MONTH), asUtc.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun localMillisFor_keepsPickedDayAndAppliesHour() {
        // Same assertions on both sides of UTC: western (DST) and eastern.
        for (zone in listOf("America/New_York", "Asia/Dhaka")) {
            useZone(zone)
            val day = utcCal(2026, Calendar.OCTOBER, 2).timeInMillis

            val visit = Formatters.localMillisFor(day, 15)

            val asLocal = Calendar.getInstance().apply { timeInMillis = visit }
            assertEquals(zone, 2026, asLocal.get(Calendar.YEAR))
            assertEquals(zone, Calendar.OCTOBER, asLocal.get(Calendar.MONTH))
            assertEquals(zone, 2, asLocal.get(Calendar.DAY_OF_MONTH))
            assertEquals(zone, 15, asLocal.get(Calendar.HOUR_OF_DAY))
            assertEquals(zone, 0, asLocal.get(Calendar.MINUTE))
            assertEquals(zone, 0, asLocal.get(Calendar.SECOND))
        }
    }

    @Test
    fun localMillisFor_matchesFixedOffsetMath() {
        useZone("Asia/Dhaka") // UTC+6, no DST
        val day = utcCal(2026, Calendar.OCTOBER, 2).timeInMillis
        val visit = Formatters.localMillisFor(day, 15)
        // 15:00 on the UTC+6 clock is 09:00Z of the same UTC day.
        assertEquals(day + (15 - 6) * 3_600_000L, visit)
    }

    @Test
    fun formatDateUtcDay_formatsTheUtcCalendarDate() {
        useZone("America/New_York")
        val day = utcCal(2026, Calendar.OCTOBER, 2).timeInMillis
        // Oct 2 2026 is a Friday; formatting is anchored to UTC, so a UTC-5
        // device must still print Oct 2, never Oct 1.
        assertEquals("Fri, Oct 2, 2026", Formatters.formatDateUtcDay(day))
    }
}
