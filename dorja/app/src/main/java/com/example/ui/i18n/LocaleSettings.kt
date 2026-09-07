package com.example.ui.i18n

import android.content.Context
import java.util.concurrent.atomic.AtomicReference

/**
 * Device-side language selection storage (atlas §7: the user's language
 * choice is a device setting, independent of the transaction country).
 *
 * Stored in SharedPreferences under key "app_language_tag" as a BCP-47 tag
 * ("bn", "hi", "ur", "pt-BR", ...). Default "en".
 */
object LocaleSettings {
    private const val PREFS = "dorja_locale"
    private const val KEY = "app_language_tag"
    private val cached = AtomicReference<String?>(null)

    fun load(context: Context): String {
        cached.get()?.let { return it }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tag = prefs.getString(KEY, "en") ?: "en"
        cached.set(tag)
        return tag
    }

    fun save(context: Context, tag: String) {
        cached.set(tag)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, tag).apply()
    }
}
