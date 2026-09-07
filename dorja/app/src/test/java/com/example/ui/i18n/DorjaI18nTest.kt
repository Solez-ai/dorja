package com.example.ui.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying the DORJA 40-locale i18n translation layer.
 */
class DorjaI18nTest {

    @Test
    fun testAll40LocalesSupported() {
        assertEquals(40, DorjaLocales.ALL.size)
    }

    @Test
    fun testPriorityLocalesFlagged() {
        val priorityTags = DorjaLocales.ALL.filter { it.priority }.map { it.tag }
        assertEquals(listOf("bn", "hi", "ur", "it"), priorityTags)
    }

    @Test
    fun testRtlFlaggedLocales() {
        val expectedRtl = setOf("ur", "ar", "fa", "he")
        for (locale in DorjaLocales.ALL) {
            val strings = DorjaStrings.forLanguageTag(locale.tag)
            if (locale.tag in expectedRtl) {
                assertTrue("Expected ${locale.tag} to be RTL", strings.rtl)
                assertTrue("Expected ${locale.tag} locale object to be RTL", locale.rtl)
            } else {
                assertFalse("Expected ${locale.tag} to be LTR", strings.rtl)
                assertFalse("Expected ${locale.tag} locale object to be LTR", locale.rtl)
            }
        }
    }

    @Test
    fun testAll40LocalesReturnValidTranslations() {
        for (locale in DorjaLocales.ALL) {
            val strings = DorjaStrings.forLanguageTag(locale.tag)
            assertNotNull("DorjaStrings should not be null for ${locale.tag}", strings)

            // Test key lookup with fallback
            assertEquals("DORJA", strings["app_name"])
            assertTrue("tab_explore should not be blank for ${locale.tag}", strings["tab_explore"].isNotBlank())
            assertTrue("auth_welcome should not be blank for ${locale.tag}", strings["auth_welcome"].isNotBlank())
            assertTrue("common_ok should not be blank for ${locale.tag}", strings["common_ok"].isNotBlank())
        }
    }

    @Test
    fun testUnknownTagFallback() {
        val strings = DorjaStrings.forLanguageTag("xx-YY")
        assertFalse(strings.rtl)
        assertEquals("DORJA", strings["app_name"])
        assertEquals("Explore", strings["tab_explore"])
    }

    @Test
    fun testSortedLocalesPutsPriorityFirst() {
        val sorted = DorjaLocales.SORTED
        val firstFour = sorted.take(4).map { it.tag }
        assertTrue(firstFour.containsAll(listOf("bn", "hi", "ur", "it")))
    }
}
