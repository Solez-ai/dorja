package com.example.ui.i18n

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

    fun format(key: String, vararg args: Any): String {
        var result = this[key]
        args.forEachIndexed { index, arg ->
            val n = index + 1
            result = result
                .replace("%${n}\$s", arg.toString())
                .replace("%${n}\$d", arg.toString())
        }
        return result
    }

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
            "tab_settings" to "Settings",
            "tab_add" to "Add",
            "tab_admin" to "Admin",
            "admin_title" to "Admin Console",
            "admin_subtitle" to "Identity reviews · third-party checks · agent register",
            "admin_awaiting" to "Awaiting review",
            "admin_approved_count" to "Approved identities",
            "admin_queue" to "VERIFICATION QUEUE",
            "admin_history" to "DECISION HISTORY",
            "admin_check_log" to "THIRD-PARTY CHECK LOG",
            "admin_agent_register" to "AGENT REGISTER",
            "admin_queue_empty_title" to "Queue is clear",
            "admin_queue_empty_body" to "New identity submissions from buyers and sellers appear here for review.",
            "admin_approve" to "Approve",
            "admin_reject" to "Reject",
            "admin_record_check" to "Record third-party check",
            "admin_review_note" to "Review note (recorded on the audit trail)",
            "admin_decide_title" to "Decide verification",
            "admin_check_title" to "Record a third-party check",
            "admin_check_intro" to "Log what was verified and how. Checks stay attached to the submission forever.",
            "admin_check_ref" to "Reference / note",
            "admin_no_checks" to "No third-party checks recorded yet. Run and log checks while reviewing a submission.",
            "admin_no_agents" to "Accounts appear here once buyers or sellers sign up and submit documents.",
            "admin_check_type" to "Check type",
            "admin_check_result" to "Result",
            "admin_check_issuer_db" to "Issuer DB",
            "admin_check_doc_auth" to "Doc authenticity",
            "admin_check_sanctions" to "Sanctions",
            "admin_check_pass" to "Pass",
            "admin_check_flagged" to "Flagged",
            "admin_check_inconclusive" to "Inconclusive",
            "admin_check_ref_hint" to "e.g. Issuer portal case #48211 — name and number match",
            "admin_check_record_cta" to "Record check",
            "admin_review_note_hint" to "e.g. Matched against issuer portal record",
            "admin_no_note" to "No note",
            "admin_unknown_account" to "Unknown account",
            "admin_submitted_at_fmt" to "submitted %1\$s",
            "admin_review_note_fmt" to "Review note: %1\$s",
            "admin_doc_photo_cd" to "Attached document photo",
            "admin_agent_verified" to "VERIFIED",
            "admin_agent_unverified" to "UNVERIFIED",
            "admin_role_admin" to "ADMIN",
            // ── Auth ──
            "auth_welcome" to "Welcome",
            "auth_sign_in" to "Sign In",
            "auth_create_account" to "Create Account",
            "auth_email" to "Email",
            "auth_password" to "Password",
            "auth_name" to "Full Name",
            "auth_phone" to "Phone",
            "auth_logout" to "Log Out",
            "auth_joining_as" to "I am joining as",
            "auth_register_cta" to "Register Account",
            "auth_have_account" to "Already have an account? Sign In",
            "auth_new_here" to "New to DORJA? Create Account",
            "auth_signup_verify_note_fmt" to "After signup you can verify your %1\$s. Admins check every submission — listings stay hidden from buyers until your documents are confirmed.",
            "auth_create_admin_cta" to "Create the platform Admin account",
            "auth_admin_note" to "One admin per device. Fill the form above with name, phone and password.",
            "common_loading" to "Please wait…",
            // ── Identity verification (real, admin-reviewed) ──
            "account_verify_card_title_fmt" to "%1\$s verification",
            "account_verify_title_fmt" to "Verify your %1\$s",
            "account_verify_intro" to "Your submission goes to the platform admin. Listings from unverified accounts stay hidden from buyers.",
            "account_verify_name" to "Name exactly as on the document",
            "account_verify_number_fmt" to "%1\$s number",
            "account_verify_photo_add" to "Attach a photo of the document (optional but speeds up review)",
            "account_verify_photo_set" to "Document photo attached — tap to change",
            "account_verify_privacy" to "DORJA stores only the last 4 digits and a one-way hash. The full number never leaves this form.",
            "account_verify_submit" to "Submit for review",
            "account_verify_failed" to "Could not submit. Check the details and try again.",
            "account_verified" to "VERIFIED",
            "account_under_review" to "UNDER REVIEW",
            "account_rejected" to "REJECTED",
            "account_unverified" to "UNVERIFIED",
            "account_verified_body" to "Your identity is confirmed. Your listings and passes are fully trusted.",
            "account_rejected_body_fmt" to "Submission rejected: %1\$s You can correct the details and submit again.",
            "account_pending_body_fmt" to "%1\$s ending %2\$s is with the admin for review. You will be notified of the decision.",
            "account_unverified_body_fmt" to "Verify your %1\$s to unlock full trust. Submissions are reviewed by the platform admin.",
            "account_verify_cta_fmt" to "Verify with %1\$s",
            "account_status_pending" to "PENDING",
            // ── Real account management ──
            "account_devices_accounts" to "ACCOUNTS ON THIS DEVICE",
            "account_you" to "you",
            "account_switch" to "Switch",
            "account_delete_short" to "Delete",
            "account_delete_title_fmt" to "Delete %1\$s's account?",
            "account_delete_body" to "Their listings, visits, reports and identity submissions are permanently removed. This cannot be undone.",
            "account_delete_confirm" to "Delete account",
            "account_admin_singleton_note" to "The platform has a single admin account. Admins review identity submissions and cannot be deleted.",
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
            "scan_localize" to "Look at a wall to localise",
            "scan_floor_found" to "FLOOR DETECTED",
            "scan_ceiling_found" to "CEILING DETECTED",
            "scan_turn_left" to "Turn left",
            "scan_turn_right" to "Turn right",
            "scan_hold" to "Hold steady — capturing…",
            "scan_ar_fail" to "AR unavailable — using motion guidance",
            "scan_low_light" to "Low light — stitches may look flat",
            "scan_slow_down" to "Too much motion — slow down",
            "scan_ar_on" to "AR-TRACKED",
            "scan_ar_install_title" to "DORJA AR needs one small add-on",
            "scan_ar_install_hint" to "Google Play Services for AR gives the scanner real room awareness: it knows floor, ceiling and walls, and guides you precisely.",
            "scan_ar_install_cta" to "Install AR",
            "scan_ar_use_motion" to "Continue without AR (motion guidance)",
            // ── Country picker ──
            "country_sheet_title" to "Change Country & Language",
            "country_sheet_subtitle" to "Choose your country to load market-specific evidence, law and language across DORJA.",
            "country_sheet_search" to "Search country…",
            "country_sheet_popular" to "POPULAR",
            "country_sheet_language" to "LANGUAGE",
            "country_sheet_device_lang" to "Use device language",
            "country_sheet_stage" to "Stage %1\$d",
            "country_signature_badge" to "SIGNATURE",
            "country_locked_badge" to "REGION LOCKED",
            "country_sig_exclusive" to "Exclusive to this market on DORJA",
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
            "account_title_full" to "My Dorja Account",
            "account_subtitle" to "Identity & Real Estate Credentials",
            "account_country_settings" to "Country & Settings",
            "account_role_host" to "HOST",
            "account_role_buyer" to "BUYER",
            "account_switch_to_buyer" to "Switch to Buyer Account",
            "account_switch_to_host" to "Switch to Host Account",
            "account_switch_subtitle" to "Switch to %1\$s to chat & explore as the other party. Each demo account keeps its own country & currency.",
            "account_identity_verified_fmt" to "%1\$s Verified",
            "account_identity_verification_fmt" to "%1\$s Verification",
            "account_safeview_gps" to "SafeView GPS Token Registry",
            "account_local_persistence" to "Local Room Persistence",
            "account_status_passed" to "PASSED",
            "account_status_active" to "ACTIVE",
            "account_status_on_device" to "ON-DEVICE",
            "account_db_management" to "DATABASE MANAGEMENT",
            "account_clear_data" to "Clear Local Data & Re-initialize",
            "account_loading" to "Loading account...",
            "account_edit_profile_role" to "Edit Profile & Role",
            "account_select_mode" to "Select Active Account Mode",
            "account_host_seller" to "Host / Seller",
            "account_seeker_buyer" to "Seeker / Buyer",
            "account_display_name" to "Display Name",
            "account_phone_number" to "Phone Number",
            "account_city_area" to "City / Area",
            "account_bio" to "Bio / Tagline",
            "account_save_changes" to "Save Changes",
            "account_reset_title" to "Reset Local Storage",
            "account_reset_body" to "This will clear all local listings, chats, and viewing passes.",
            "account_reset_all" to "Reset All",
            "account_delete_content_title" to "Delete My Content",
            "account_delete_content_body" to "This permanently deletes your listings, rooms, 3D scans, legal documents, property passports, chats, messages, and viewing passes from this device. Your profile rows are kept. This cannot be undone.",
            "account_delete_everything" to "Delete Everything",
            "account_erase_title" to "Erase My Account Data",
            "account_erase_body" to "This erases EVERYTHING stored about you on this device: all content plus your profile rows. The app then re-initializes with clean demo accounts. This cannot be undone.",
            "account_erase_everything" to "Erase Everything",
            "account_reconfirm_done_title" to "Evidence Re-confirmed",
            "account_reconfirm_done_body" to "%1\$s document(s) re-confirmed. This is your own re-attestation — the evidence level of each document is unchanged and DORJA has not independently verified anything new.",
            "account_content_deleted_title" to "Content Deleted",
            "account_content_deleted_body" to "All of your listings, evidence, chats, and viewing passes have been removed from this device.",
            "account_erased_title" to "Account Data Erased",
            "account_erased_body" to "All personal data stored on this device has been erased and the app has been reset to clean demo accounts.",
            "account_checking_evidence" to "Checking your document evidence…",
            "account_no_docs" to "No legal documents attached to any listing yet. Evidence levels appear here once you add documents in Host Suite.",
            "account_docs_on_record" to "Documents on record",
            "account_issuer_confirmed" to "Issuer / government confirmed",
            "account_self_declared" to "Self-declared (not verified)",
            "account_stale_checks" to "Checks older than 24 months",
            "account_marked_expired" to "Marked expired",
            "account_reconfirming" to "Re-confirming…",
            "account_reconfirm_hint" to "Re-confirming refreshes your own attestation and evidence-check dates. It never raises an evidence level and is never shown as independent verification.",
            "account_relocation_subtitle" to "Moving abroad? Get the destination checklist, official records, language notes and the professionals you may need.",
            "account_reports_intro" to "Reports you filed and their resolution state. Counterparty responses stay visible in the listing's conflict view — nothing is hidden and nobody is silently judged.",
            "account_no_reports" to "No reports filed.",
            "account_withdraw" to "Withdraw",
            "account_and_more" to "…and %1\$s more",
            "account_privacy_intro" to "Your evidence, chats, and viewing history live on this device. You control deletion — no support ticket required.",
            "account_delete_content_sub" to "Removes your listings, evidence, scans, chats, and viewing passes. Keeps your profile.",
            "account_erase_data_sub" to "Erases everything above plus your profile rows, then resets the app to clean demo accounts.",
            "account_consent_note" to "Consent records and access logs stay with the data they describe — when the data goes, they go too.",
            // ── Country settings (replaces language picker) ──
            "settings_select_country" to "Select Country",
            "settings_country_subtitle" to "Language, currency, and legal terms follow the country you choose.",
            "settings_title" to "Settings",
            "settings_subtitle" to "Appearance, country, and data",
            "settings_appearance" to "APPEARANCE",
            "settings_dark_mode" to "Dark Mode",
            "settings_dark_mode_sub" to "Light mode is the default. Turn this on for a dark canvas.",
            "settings_country_section" to "COUNTRY & LANGUAGE",
            "settings_country_currency" to "Currency",
            "settings_country_identity" to "Identity",
            "settings_country_language" to "Language",
            "lang_title" to "App Language",
            "lang_subtitle" to "Choose from 40 languages. Untranslated screens fall back to English.",
            "lang_search" to "Search language…",
            "lang_priority_note" to "Priority languages are listed first.",
            "lang_changed" to "Language updated",
            // ── Navigation chrome ──
            "settings_accounts_section" to "ACCOUNT SESSION",
            "settings_sign_out" to "Sign out",
            "settings_sign_out_sub" to "End this session. Your accounts stay on this device.",
            "settings_new_account" to "Create a new account",
            "settings_new_account_sub" to "Sign up as a buyer or seller — or verify with your national ID.",
            "nav_verified_account" to "Verified account",
            "nav_tagline" to "Because every door should be trustworthy.",
            "nav_open_menu" to "Open menu",
            "nav_close_menu" to "Close menu",
            // ── Host listings ──
            "host_title" to "My Properties",
            "host_subtitle" to "Host Management Suite",
            "host_new" to "+ New",
            "host_my_listings" to "MY LISTINGS",
            "host_empty_title" to "No Properties Listed Yet",
            "host_empty_body" to "You are currently in Host mode. Tap the button below to add your first verified property listing with custom rooms and amenities.",
            "host_create" to "Create Property Listing",
            "host_delete_title" to "Delete Property Listing",
            "host_delete_body" to "Are you sure you want to remove '%1\$s'? All associated room data will also be removed.",
            "host_view_listing" to "View Listing",
            "host_options" to "Options",
            "host_3d_tour" to "3D TOUR",
            // ── Explore extras ──
            "explore_header" to "Dorja Properties",
            "explore_anti_scam" to "ANTI-SCAM",
            "explore_search_city" to "Search city or area",
            "explore_all_listings" to "All Listings",
            "explore_apartments" to "Apartments",
            "explore_houses" to "Houses",
            "explore_properties_available" to "PROPERTIES AVAILABLE",
            "explore_safeview_gated" to "SAFEVIEW GATED",
            "explore_empty_title" to "No Properties Found",
            "explore_empty_none" to "No active listings published yet. Properties listed by hosts will appear here.",
            "explore_empty_match" to "No properties match '%1\$s'.",
            "explore_clear_search" to "Clear search",
            // ── Inbox extras ──
            "inbox_encrypted_title" to "Encrypted Inbox",
            "inbox_encrypted_subtitle" to "Zero-leak real estate messaging channel",
            "inbox_secured" to "SECURED",
            "inbox_no_inquiries" to "No inquiries yet",
            "inbox_start_chat" to "Start a chat directly from any property listing.",
            // ── Visits extras ──
            "visits_host_title" to "Visitor Passes",
            "visits_seeker_title" to "My Viewing Passes",
            "visits_host_subtitle" to "Scheduled appointments for your listed properties",
            "visits_seeker_subtitle" to "SafeView geofenced access tokens",
            "visits_gps_gated" to "GPS GATED",
            "visits_cancel_title" to "Cancel this visit?",
            "visits_cancel_body" to "The SafeView pass will be voided and the time slot released. This cannot be undone.",
            "visits_cancel_cta" to "Cancel Visit",
            "visits_cancel_keep" to "Keep it",
            "visits_cancel_link" to "Cancel this visit",
            "visits_empty_host_title" to "No Visitor Requests Yet",
            "visits_empty_seeker_title" to "No Active Viewing Passes",
            "visits_empty_host_body" to "When seekers book a visit for your properties, their encrypted pass requests will appear here for verification.",
            "visits_empty_seeker_body" to "When you book a physical inspection on any property, your SafeView QR access pass will be generated here.",
            "visits_pass_status" to "PASS STATUS",
            "visits_security_token" to "SECURITY TOKEN",
            "visits_active" to "Active",
            "visits_past_passes" to "COMPLETED & PAST PASSES",
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
            val extras = DorjaTranslationsChrome.forTag(tag)
            return DorjaStrings(rtl = locale.rtl, values = map + extras)
        }
    }
}

/** Composition-local carrying the active translation table. */
val LocalDorjaLocale = staticCompositionLocalOf<DorjaStrings> { DorjaStrings() }
