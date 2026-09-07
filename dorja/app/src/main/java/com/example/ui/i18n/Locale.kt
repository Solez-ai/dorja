package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The active translation table for the current composition.
 * Falls back to English for any missing key, so partial locales are safe.
 */
@Composable
@ReadOnlyComposable
fun t(): DorjaStrings = LocalDorjaLocale.current

/** Convenience one-liner: t()["key"] inside composables. */
@Composable
@ReadOnlyComposable
fun L(key: String): String = LocalDorjaLocale.current[key]
