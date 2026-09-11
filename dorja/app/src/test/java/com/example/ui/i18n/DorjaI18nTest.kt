package com.example.ui.i18n

import com.example.data.country.CountryRegistry
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

    @Test
    fun testCountryDrivesLanguageTag() {
        assertEquals("bn", LocaleSettings.languageTagForCountry("BD"))
        assertEquals("ja", LocaleSettings.languageTagForCountry("JP"))
        assertEquals("id", LocaleSettings.languageTagForCountry("ID"))
        assertEquals("en", LocaleSettings.languageTagForCountry("US"))
        assertEquals("en", LocaleSettings.languageTagForCountry("GB"))
        assertEquals("hi", LocaleSettings.languageTagForCountry("IN"))
        assertEquals("ar", LocaleSettings.languageTagForCountry("AE"))
    }

    @Test
    fun testIdentityCredentialsAreCountrySpecific() {
        assertEquals("NID", CountryRegistry.identityCredential("BD").shortName)
        assertEquals("My Number", CountryRegistry.identityCredential("JP").shortName)
        assertEquals("KTP", CountryRegistry.identityCredential("ID").shortName)
        assertEquals("Social Security", CountryRegistry.identityCredential("US").shortName)
        assertEquals("Aadhaar", CountryRegistry.identityCredential("IN").shortName)
        assertEquals("Emirates ID", CountryRegistry.identityCredential("AE").shortName)
    }

    @Test
    fun testJapaneseChromeOverridesAccountAndNav() {
        val ja = DorjaStrings.forLanguageTag("ja")
        assertEquals("物件", ja["tab_properties"])
        assertEquals("内見", ja["tab_visits"])
        assertEquals("メッセージ", ja["tab_inbox"])
        assertEquals("アカウント", ja["tab_account"])
        assertEquals("認証済みアカウント", ja["nav_verified_account"])
        assertEquals("まだ物件が登録されていません", ja["host_empty_title"])
        assertEquals("国を選択", ja["settings_select_country"])
        assertEquals("%1\$s認証済", ja["account_identity_verified_fmt"])
        assertEquals("My Number認証済", ja.format("account_identity_verified_fmt", "My Number"))
    }

    @Test
    fun testIndonesianChromeOverridesAccountAndNav() {
        val id = DorjaStrings.forLanguageTag("id")
        assertEquals("Properti", id["tab_properties"])
        assertEquals("Kunjungan", id["tab_visits"])
        assertEquals("Pesan", id["tab_inbox"])
        assertEquals("Akun", id["tab_account"])
        assertEquals("Akun terverifikasi", id["nav_verified_account"])
        assertEquals("Belum Ada Properti Terdaftar", id["host_empty_title"])
        assertEquals("KTP Terverifikasi", id.format("account_identity_verified_fmt", "KTP"))
    }

    @Test
    fun testFormatFallsBackToEnglishForMissingLocale() {
        val en = DorjaStrings.forLanguageTag("en")
        assertEquals("Social Security Verified", en.format("account_identity_verified_fmt", "Social Security"))
        assertEquals("No properties match 'Dhaka'.", en.format("explore_empty_match", "Dhaka"))
    }
}
