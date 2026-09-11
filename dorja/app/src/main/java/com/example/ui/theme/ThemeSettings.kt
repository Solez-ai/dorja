package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Device-side appearance. Light mode is the default; Dark Mode is an
 * explicit user choice stored in SharedPreferences.
 */
object ThemeSettings {
    private const val PREFS = "dorja_theme"
    private const val KEY_DARK = "dark_mode"

    private val dark = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = dark.asStateFlow()

    @Volatile private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            dark.value = prefs.getBoolean(KEY_DARK, false)
            initialized = true
        }
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        init(context)
        if (dark.value == enabled) return
        dark.value = enabled
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, enabled)
            .apply()
    }
}
