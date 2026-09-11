package com.example.ui.i18n

import android.content.Context
import com.example.data.country.CountryRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Country is the settings choice. Language, currency, and identity-document
 * labels all follow the selected country (via [CountryRegistry] primary
 * languages). The UI collects [languageTag] so a country change recomposes
 * every screen that reads [L] / [t].
 */
object LocaleSettings {
    private const val PREFS = "dorja_locale"
    private const val KEY_LANG = "app_language_tag"
    private const val KEY_COUNTRY = "app_country_code"

    private val language = MutableStateFlow("en")
    private val country = MutableStateFlow("BD")

    val languageTag: StateFlow<String> = language.asStateFlow()
    val countryCode: StateFlow<String> = country.asStateFlow()

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val iso = prefs.getString(KEY_COUNTRY, "BD") ?: "BD"
            val storedLang = prefs.getString(KEY_LANG, null)
            country.value = iso
            language.value = storedLang ?: languageTagForCountry(iso)
            initialized = true
        }
    }

    fun load(context: Context): String {
        init(context)
        return language.value
    }

    fun save(context: Context, tag: String) {
        init(context)
        language.value = tag
        persist(context)
    }

    /**
     * Sets the active country and switches the interface language to that
     * country's first supported primary language. No-ops if already applied.
     */
    fun applyCountry(context: Context, iso2: String) {
        init(context)
        val tag = languageTagForCountry(iso2)
        if (country.value.equals(iso2, ignoreCase = true) && language.value == tag) return
        country.value = iso2.uppercase()
        language.value = tag
        persist(context)
    }

    fun languageTagForCountry(iso2: String): String {
        val profile = CountryRegistry.profile(iso2)
        for (raw in profile.primaryLanguages) {
            val base = raw.substringBefore('-').lowercase()
            if (DorjaLocales.byTag(base) != null) return base
        }
        return "en"
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, language.value)
            .putString(KEY_COUNTRY, country.value)
            .apply()
    }
}
