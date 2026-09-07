package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.i18n.DorjaStrings
import com.example.ui.i18n.LocalDorjaLocale
import com.example.ui.i18n.LocaleSettings
import com.example.ui.navigation.DorjaNavHost
import com.example.ui.theme.DorjaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val tag = LocaleSettings.load(applicationContext)
            var strings by remember { mutableStateOf(DorjaStrings.forLanguageTag(tag)) }
            DorjaTheme {
                CompositionLocalProvider(LocalDorjaLocale provides strings) {
                    DorjaNavHost()
                }
            }
        }
    }
}
