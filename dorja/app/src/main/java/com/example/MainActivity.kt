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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleSettings.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val tag by LocaleSettings.languageTag.collectAsState()
            val strings = remember(tag) { DorjaStrings.forLanguageTag(tag) }
            val currentUser by DorjaApp.instance.repository.currentUser.collectAsState()
            val context = LocalContext.current
            LaunchedEffect(currentUser?.countryCode) {
                val code = currentUser?.countryCode
                if (!code.isNullOrBlank()) {
                    LocaleSettings.applyCountry(context, code)
                }
            }
            DorjaTheme {
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
