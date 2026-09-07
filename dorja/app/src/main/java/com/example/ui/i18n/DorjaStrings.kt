package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * DORJA universal UI strings (atlas §7 language layer, "interface" tier).
 *
 * Design:
 * - One flat key set used by every screen through [t] / [L].
 * - English (en) is the complete reference dictionary; every other locale
 *   only overrides the keys it translates. A missing key falls back to
 *   English, so a partial translation can never break the UI.
 * - RTL languages (Arabic, Persian, Urdu) are flagged so the app can flip
 *   its layout direction.
 */
data class DorjaStrings(
    val rtl: Boolean = false,
    val values: Map<String, String> = emptyMap()
) {
    operator fun get(key: String): String = values[key] ?: ENGLISH[key] ?: key

    companion object {
        val ENGLISH: Map<String, String> = mapOf(
            // ── App / navigation ──
            "app_name" to "DORJA",
            "app_tagline" to "Property Trust Platform",
            "tab_explore" to "Explore",
            "tab_properties" to "Properties",
            "tab_visits" to "Visits",
            "tab_inbox" to "Inbox",
            "tab_account" to "Account",
            "tab_add" to "Add",
            // ── Auth ──
            "auth_welcome" to "Welcome",
            "auth_sign_in" to "Sign In",
            "auth_create_account" to "Create Account",
            "auth_email" to "Email",
            "auth_password" to "Password",
            "auth_name" to "Full Name",
            "auth_phone" to "Phone",
            "auth_logout" to "Log Out",
            "auth_footer" to "Verified homes. SafeView access.",
            // ── Explore ──
            "explore_title" to "Explore",
            "explore_subtitle" to "Verified homes. SafeView access.",
            "explore_search_hint" to "Search area, city, or property",
            "explore_for_rent" to "FOR RENT",
            "explore_for_sale" to "FOR SALE",
            "explore_no_results" to "No properties match your search yet.",
            "explore_evidence_verified" to "EVIDENCE VERIFIED",
            "explore_evidence_pending" to "EVIDENCE PENDING",
            // ── Property detail ──
            "detail_title" to "Property Details",
            "detail_for_rent" to "For Rent",
            "detail_for_sale" to "For Sale",
            "detail_monthly_rent" to "Monthly Rent",
            "detail_asking_price" to "Asking Price",
            "detail_view_passport" to "View Property Passport",
            "detail_export_pack" to "Export Decision Pack",
            "detail_book_viewing" to "Request SafeView Viewing",
            "detail_message_host" to "Message Host",
            "detail_report_problem" to "Report a Problem",
            "detail_respond_report" to "Respond to a Report",
            "detail_documents" to "Official Documents & Title Records",
            "detail_liveability" to "LIVEABILITY & ENERGY",
            "detail_disputes" to "DISPUTES & CONFLICT VIEW",
            "detail_promises" to "PROMISES",
            "detail_handover" to "HANDOVER PASSPORT",
            "detail_safe_address" to "SafeView Protected Address",
            "detail_open_maps" to "Open in Google Maps",
            "detail_bedrooms" to "Bedrooms",
            "detail_bathrooms" to "Bathrooms",
            "detail_area" to "Area",
            // ── Create listing ──
            "create_title" to "Create Listing",
            "create_edit_title" to "Edit Listing",
            "create_basic_info" to "BASIC INFO",
            "create_location" to "LOCATION",
            "create_details" to "DETAILS",
            "create_documents" to "DOCUMENTS",
            "create_amenities" to "AMENITIES",
            "create_publish" to "Publish Listing",
            "create_save" to "Save Changes",
            "create_country" to "Country of Transaction",
            "create_property_type" to "Property Type",
            "create_intent" to "Listing Intent",
            "create_title_label" to "Listing Title",
            "create_description" to "Description",
            "create_add_document" to "Add Document",
            "create_document_type" to "Document Type",
            "create_evidence_level" to "Evidence Level (honest status)",
            "create_keep_evidence" to "Keep this evidence for",
            "create_keep_until_delete" to "Until I delete it",
            "create_keep_30" to "30 days",
            "create_keep_1y" to "1 year",
            "create_keep_2y" to "2 years",
            "create_retention_hint_keep" to "Kept until you delete it — you can also erase all your data from Account.",
            "create_retention_hint_auto" to "Auto-deleted from DORJA after this window. Download anything you need first.",
            "create_scan_3d" to "Scan 3D",
            // ── Scanner ──
            "scan_title" to "3D Room Scan",
            "scan_instruction" to "Stand in the centre and turn slowly. Keep the phone at chest height.",
            "scan_start" to "Start Scan",
            "scan_pause" to "Pause",
            "scan_resume" to "Resume",
            "scan_finish" to "Finish & Save",
            "scan_cancel" to "Cancel Scan",
            "scan_saving" to "Saving panorama…",
            "scan_shot_progress" to "Shot %1\$d of %2\$d",
            "scan_move_next" to "Move to next position",
            "scan_tilt_up" to "Tilt up",
            "scan_tilt_down" to "Tilt down",
            "scan_tilt_ok" to "Tilt steady",
            // ── Visits ──
            "visits_title" to "Visits",
            "visits_upcoming" to "Upcoming",
            "visits_past" to "Past",
            "visits_request" to "Request Visit",
            "visits_confirm" to "Confirm",
            "visits_cancel" to "Cancel",
            "visits_safe_pass" to "SafeView Pass",
            "visits_no_visits" to "No visits scheduled yet.",
            // ── Inbox ──
            "inbox_title" to "Inbox",
            "inbox_no_messages" to "No conversations yet.",
            "inbox_type_message" to "Type a message…",
            "inbox_send" to "Send",
            // ── Account ──
            "account_title" to "Account",
            "account_settings" to "SETTINGS",
            "account_language" to "Language",
            "account_language_subtitle" to "Use the app in any of 40 languages",
            "account_security" to "SECURITY CREDENTIALS",
            "account_privacy_data" to "PRIVACY & DATA",
            "account_delete_content" to "Delete My Content",
            "account_erase_data" to "Erase My Account Data",
            "account_reports" to "REPORTS & APPEALS",
            "account_evidence_health" to "EVIDENCE HEALTH",
            "account_reconfirm" to "Re-confirm My Evidence",
            "account_relocation" to "Relocation Mode",
            "account_verified_identity" to "Identity Verified",
            "account_edit_profile" to "Edit Profile",
            "account_save_profile" to "Save Profile",
            // ── Language picker ──
            "lang_title" to "App Language",
            "lang_subtitle" to "Choose from 40 languages. Untranslated screens fall back to English.",
            "lang_search" to "Search language…",
            "lang_priority_note" to "Priority languages are listed first.",
            "lang_changed" to "Language updated",
            // ── Common ──
            "common_ok" to "OK",
            "common_cancel" to "Cancel",
            "common_save" to "Save",
            "common_delete" to "Delete",
            "common_confirm" to "Confirm",
            "common_close" to "Close",
            "common_retry" to "Retry",
            "common_loading" to "Loading…",
            "common_error" to "Something went wrong",
            "common_required" to "Required",
            "common_optional" to "Optional",
            "common_yes" to "Yes",
            "common_no" to "No",
            "common_back" to "Back",
            "common_next" to "Next",
            "common_done" to "Done",
            "common_share" to "Share",
            "common_more" to "More",
            "common_search" to "Search"
        )

        /** Priority languages, shown first in the picker (user requirement). */
        val PRIORITY_CODES = listOf("bn", "hi", "ur", "it")

        /**
         * Resolves the translation table for a given BCP-47 language tag across all 40 supported locales.
         * Sets [rtl] flag for RTL languages (Urdu, Arabic, Persian, Hebrew) and falls back to [ENGLISH]
         * for missing keys.
         */
        fun forLanguageTag(tag: String): DorjaStrings {
            val locale = DorjaLocales.byTag(tag) ?: DorjaLocales.byTag("en")!!
            val map = when (tag.lowercase()) {
                // Priority 4
                "bn" -> DorjaTranslationsPriority.BANGLA
                "hi" -> DorjaTranslationsPriority.HINDI
                "ur" -> DorjaTranslationsPriority.URDU
                "it" -> DorjaTranslationsPriority.ITALIAN

                // Asia
                "zh" -> DorjaTranslationsAsia.CHINESE
                "ja" -> DorjaTranslationsAsia.JAPANESE
                "ko" -> DorjaTranslationsAsia.KOREAN
                "vi" -> DorjaTranslationsAsia.VIETNAMESE
                "th" -> DorjaTranslationsAsia.THAI
                "id" -> DorjaTranslationsAsia.INDONESIAN
                "ms" -> DorjaTranslationsAsia.MALAY
                "tl" -> DorjaTranslationsAsia.FILIPINO
                "ta" -> DorjaTranslationsAsia.TAMIL
                "te" -> DorjaTranslationsAsia.TELUGU
                "mr" -> DorjaTranslationsAsia.MARATHI
                "gu" -> DorjaTranslationsAsia.GUJARATI
                "pa" -> DorjaTranslationsAsia.PUNJABI
                "ne" -> DorjaTranslationsAsia.NEPALI
                "si" -> DorjaTranslationsAsia.SINHALA
                "km" -> DorjaTranslationsAsia.KHMER
                "uz" -> DorjaTranslationsAsia.UZBEK
                "kk" -> DorjaTranslationsAsia.KAZAKH
                "ka" -> DorjaTranslationsAsia.GEORGIAN

                // Europe & Middle East
                "en" -> DorjaTranslationsEurope.ENGLISH
                "de" -> DorjaTranslationsEurope.GERMAN
                "fr" -> DorjaTranslationsEurope.FRENCH
                "es" -> DorjaTranslationsEurope.SPANISH
                "pt" -> DorjaTranslationsEurope.PORTUGUESE
                "ru" -> DorjaTranslationsEurope.RUSSIAN
                "tr" -> DorjaTranslationsEurope.TURKISH
                "pl" -> DorjaTranslationsEurope.POLISH
                "nl" -> DorjaTranslationsEurope.DUTCH
                "el" -> DorjaTranslationsEurope.GREEK
                "ro" -> DorjaTranslationsEurope.ROMANIAN
                "hu" -> DorjaTranslationsEurope.HUNGARIAN
                "cs" -> DorjaTranslationsEurope.CZECH
                "uk" -> DorjaTranslationsEurope.UKRAINIAN
                "ar" -> DorjaTranslationsEurope.ARABIC
                "fa" -> DorjaTranslationsEurope.PERSIAN
                "he" -> DorjaTranslationsEurope.HEBREW

                else -> ENGLISH
            }
            return DorjaStrings(rtl = locale.rtl, values = map)
        }
    }
}

/** Composition-local carrying the active translation table. */
val LocalDorjaLocale = staticCompositionLocalOf<DorjaStrings> { DorjaStrings() }
