package com.example.ui.i18n

/**
 * The 40 supported interface languages (top-40 spoken languages of Europe
 * and Asia). Bangla, Hindi, Urdu, and Italian are first-priority per
 * product decision and ship with the fullest translation set; every other
 * locale covers the core keys and falls back to English elsewhere, so a
 * partial translation can never break the UI.
 */
data class DorjaLocale(
    val tag: String,       // BCP-47
    val nativeName: String,
    val englishName: String,
    val rtl: Boolean = false,
    val priority: Boolean = false
)

object DorjaLocales {
    val ALL: List<DorjaLocale> = listOf(
        // ── Priority (user-mandated first-priority languages) ──
        DorjaLocale("bn", "বাংলা", "Bangla", priority = true),
        DorjaLocale("hi", "हिन्दी", "Hindi", priority = true),
        DorjaLocale("ur", "اردو", "Urdu", rtl = true, priority = true),
        DorjaLocale("it", "Italiano", "Italian", priority = true),
        // ── Asia ──
        DorjaLocale("ar", "العربية", "Arabic", rtl = true),
        DorjaLocale("fa", "فارسی", "Persian", rtl = true),
        DorjaLocale("he", "עברית", "Hebrew", rtl = true),
        DorjaLocale("zh", "中文", "Chinese"),
        DorjaLocale("ja", "日本語", "Japanese"),
        DorjaLocale("ko", "한국어", "Korean"),
        DorjaLocale("vi", "Tiếng Việt", "Vietnamese"),
        DorjaLocale("th", "ไทย", "Thai"),
        DorjaLocale("id", "Bahasa Indonesia", "Indonesian"),
        DorjaLocale("ms", "Bahasa Melayu", "Malay"),
        DorjaLocale("tl", "Filipino", "Filipino"),
        DorjaLocale("ta", "தமிழ்", "Tamil"),
        DorjaLocale("te", "తెలుగు", "Telugu"),
        DorjaLocale("mr", "मराठी", "Marathi"),
        DorjaLocale("gu", "ગુજરાતી", "Gujarati"),
        DorjaLocale("pa", "ਪੰਜਾਬੀ", "Punjabi"),
        DorjaLocale("ne", "नेपाली", "Nepali"),
        DorjaLocale("si", "සිංහල", "Sinhala"),
        DorjaLocale("km", "ខ្មែរ", "Khmer"),
        DorjaLocale("uz", "Oʻzbek", "Uzbek"),
        DorjaLocale("kk", "Қазақша", "Kazakh"),
        DorjaLocale("ka", "ქართული", "Georgian"),
        // ── Europe ──
        DorjaLocale("en", "English", "English"),
        DorjaLocale("de", "Deutsch", "German"),
        DorjaLocale("fr", "Français", "French"),
        DorjaLocale("es", "Español", "Spanish"),
        DorjaLocale("pt", "Português", "Portuguese"),
        DorjaLocale("ru", "Русский", "Russian"),
        DorjaLocale("tr", "Türkçe", "Turkish"),
        DorjaLocale("pl", "Polski", "Polish"),
        DorjaLocale("nl", "Nederlands", "Dutch"),
        DorjaLocale("el", "Ελληνικά", "Greek"),
        DorjaLocale("ro", "Română", "Romanian"),
        DorjaLocale("hu", "Magyar", "Hungarian"),
        DorjaLocale("cs", "Čeština", "Czech"),
        DorjaLocale("uk", "Українська", "Ukrainian")
    )

    fun byTag(tag: String): DorjaLocale? = ALL.firstOrNull { it.tag == tag }

    /** Sorted for the picker: priority first, then by English name. */
    val SORTED: List<DorjaLocale> =
        ALL.sortedWith(compareByDescending<DorjaLocale> { it.priority }.thenBy { it.englishName })
}
