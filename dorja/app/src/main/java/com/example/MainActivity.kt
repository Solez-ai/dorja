package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.i18n.DorjaStrings
import com.example.ui.i18n.LocalDorjaLocale
import com.example.ui.i18n.LocaleSettings
import com.example.ui.navigation.DorjaNavHost
import com.example.ui.theme.DorjaTheme
import com.example.ui.theme.ThemeSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleSettings.init(applicationContext)
        ThemeSettings.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val tag by LocaleSettings.languageTag.collectAsState()
            val strings = remember(tag) { DorjaStrings.forLanguageTag(tag) }
            val darkTheme by ThemeSettings.darkMode.collectAsState()
            val currentUser by DorjaApp.instance.repository.currentUser.collectAsState()
            val context = LocalContext.current
            // Keep the app language/country pinned to the user's explicit Settings
            // choice. Switching accounts must NOT re-seed the locale from the
            // freshly-loaded profile row.
            LaunchedEffect(Unit) {
                LocaleSettings.setPinnedLanguageTag(LocaleSettings.languageTag.value)
            }
            LaunchedEffect(currentUser?.countryCode) {
                if (!LocaleSettings.isLanguagePinned()) {
                    val code = currentUser?.countryCode
                    if (!code.isNullOrBlank()) {
                        LocaleSettings.applyCountry(context, code)
                    }
                }
            }
            DorjaTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalDorjaLocale provides strings,
                    LocalLayoutDirection provides if (strings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    DorjaNavHost()
                }
            }
        }
    }
}
