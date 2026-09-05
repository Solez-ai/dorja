package com.example.ui.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.DorjaApp
import com.example.data.country.CountryRegistry
import com.example.data.model.EvidenceLevel
import com.example.data.model.Viewing
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports a country-specific, evidence-labelled decision pack for a property
 * (atlas §8 "Property decision pack").
 *
 * The pack distinguishes original documents from user annotations, shows the
 * evidence level of every upload, lists the country's disclosure checklist,
 * and includes promises + appointment history. It never claims legal advice.
 */
object DisclosurePackExporter {

    private const val PAGE_W = 595   // A4 portrait (pt)
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val BOTTOM = 70f
    private const val LINE_H = 14f
    private const val BRAND = Color.rgb(0x00, 0x61, 0xA4)
    private const val MUTED = Color.rgb(0x74, 0x77, 0x7F)
    private const val AMBER = Color.rgb(0x82, 0x55, 0x00)

    suspend fun generate(context: Context, listingId: String): File? {
        val repo = DorjaApp.instance.repository
        val listing = repo.getListingById(listingId) ?: return null
        val docs = repo.getLegalDocumentsByListing(listingId).first()
        val promises = repo.getPromisesByListing(listingId).first()
        val viewings = repo.getViewingsByListing(listingId).first()
        val passport = repo.getPassportForListing(listingId)
        val owner = repo.getUserById(listing.ownerId)
        val profile = CountryRegistry.profile(listing.countryCode)

        val document = PdfDocument()
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var canvas = page.canvas
        var y = MARGIN
        var pageNo = 1

        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 19f; typeface = Typeface.DEFAULT_BOLD }
        val sectionPaint = Paint().apply { color = BRAND; textSize = 13f; typeface = Typeface.DEFAULT_BOLD }
        val bodyPaint = Paint().apply { color = Color.BLACK; textSize = 9.5f; typeface = Typeface.DEFAULT }
        val bodyBold = Paint().apply { color = Color.BLACK; textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD }
        val smallPaint = Paint().apply { color = MUTED; textSize = 8f; typeface = Typeface.ITALIC }
        val amberPaint = Paint().apply { color = AMBER; textSize = 8.5f; typeface = Typeface.ITALIC }

        fun newPageIfNeeded(needed: Float) {
            if (y + needed > PAGE_H - BOTTOM) {
                document.finishPage(page)
                pageNo++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawWrapped(text: String, paint: Paint = bodyPaint, indent: Float = 0f) {
            val words = text.split(" ")
            var cur = ""
            val maxW = PAGE_W - MARGIN * 2 - indent
            for (w in words) {
                val test = if (cur.isEmpty()) w else "$cur $w"
                if (paint.measureText(test) > maxW && cur.isNotEmpty()) {
                    newPageIfNeeded(LINE_H)
                    canvas.drawText(cur, MARGIN + indent, y, paint)
                    y += LINE_H
                    cur = w
                } else {
                    cur = test
                }
            }
            if (cur.isNotEmpty()) {
                newPageIfNeeded(LINE_H)
                canvas.drawText(cur, MARGIN + indent, y, paint)
                y += LINE_H
            }
        }

        fun section(title: String) {
            newPageIfNeeded(LINE_H * 2.4f)
            y += LINE_H * 0.6f
            canvas.drawText(title, MARGIN, y, sectionPaint)
            y += LINE_H * 1.2f
            val rule = Paint().apply { color = Color.rgb(0xE1, 0xE2, 0xEC); strokeWidth = 1.5f }
            canvas.drawLine(MARGIN, y - 6f, PAGE_W - MARGIN, y - 6f, rule)
        }

        fun humanize(code: String): String =
            code.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

        // ── Header ─────────────────────────────────────────────────────────
        canvas.drawText("D O R J A", MARGIN, y, titlePaint)
        y += LINE_H
        canvas.drawText("Property Decision Pack", MARGIN, y, sectionPaint)
        y += LINE_H * 1.3f
        canvas.drawText(
            "Generated ${SimpleDateFormat("EEEE, MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())}",
            MARGIN, y, smallPaint
        )
        y += LINE_H * 1.6f

        // ── 1. Property summary ────────────────────────────────────────────
        section("1. PROPERTY SUMMARY")
        drawWrapped(listing.title.ifBlank { "Untitled listing" }, bodyBold)
        drawWrapped(
            "Intent: ${if (listing.intent == "RENT") "For Rent" else "For Sale"}  •  " +
                Formatters.formatPrice(listing.priceAmount, listing.currency, listing.intent),
            bodyPaint
        )
        drawWrapped(
            "Type: ${humanize(listing.propertyType)}  •  ${listing.bedrooms} bed / " +
                "${listing.bathrooms} bath / ${listing.balconies} balcony  •  ${listing.sqft} sqft",
            bodyPaint
        )
        drawWrapped("Area / Neighborhood: ${listing.publicArea}", bodyPaint)
        if (listing.exactAddress.isNotBlank()) {
            drawWrapped("Exact address is withheld on DORJA until a confirmed SafeView pass.", smallPaint)
        }
        drawWrapped("Country of transaction: ${profile.displayName} (${profile.currencyCode})", bodyPaint)
        if (passport != null) {
            drawWrapped("Property Passport: ${passport.id.uppercase()}", bodyBold)
        }
        if (owner != null) {
            drawWrapped("Host: ${owner.displayName}", bodyPaint)
        }

        // ── 2. Source-labelled documents ───────────────────────────────────
        section("2. DOCUMENTS & EVIDENCE LEVELS")
        if (docs.isEmpty()) {
            drawWrapped("No documents attached to this listing yet.", bodyPaint)
        } else {
            docs.forEach { doc ->
                newPageIfNeeded(LINE_H * 5.2f)
                drawWrapped(doc.documentTitle.ifBlank { doc.documentType }, bodyBold)
                drawWrapped(
                    "Type: ${doc.documentType}  •  No: ${doc.documentNumber}  •  " +
                        "Issuer: ${doc.issuingAuthority}  •  Date: ${doc.issueDate}",
                    bodyPaint
                )
                val level = EvidenceLevel.fromCode(doc.evidenceLevel)
                drawWrapped("Evidence: ${level.label.uppercase(Locale.getDefault())}", bodyBold)
                val note = when {
                    doc.limitationNote.isNotBlank() -> doc.limitationNote
                    !EvidenceLevel.isConfirmed(level) -> "Not independently verified by DORJA."
                    else -> ""
                }
                if (note.isNotBlank()) drawWrapped("Note: $note", amberPaint)
                y += LINE_H * 0.6f
            }
        }

        // ── 3. Country disclosure checklist ────────────────────────────────
        section("3. DISCLOSURE CHECKLIST — ${profile.displayName}")
        if (profile.disclosureChecklist.isEmpty()) {
            drawWrapped("No country checklist configured for this market yet.", smallPaint)
        } else {
            profile.disclosureChecklist.forEach { item ->
                newPageIfNeeded(LINE_H)
                val box = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1f }
                canvas.drawRect(RectF(MARGIN, y - 8f, MARGIN + 9f, y + 1f), box)
                drawWrapped(" $item", bodyPaint, indent = 16f)
            }
        }
        drawWrapped(
            "Original documents and translations must be kept in their source language; " +
                "translations are labelled as translations.",
            smallPaint
        )

        // ── 4. Open questions ──────────────────────────────────────────────
        section("4. OPEN QUESTIONS")
        listOf(
            "Has ownership been verified against an official registry?",
            "Are all promised inclusions written into the agreement?",
            "Are there any outstanding disputes, liens, or encumbrances?",
            "What is the condition and energy / running-cost status?",
            "Which licensed professional will take responsibility at handover?"
        ).forEach { q ->
            newPageIfNeeded(LINE_H * 1.1f)
            canvas.drawText("\u2022  $q", MARGIN, y, bodyPaint)
            y += LINE_H
        }

        // ── 5. Promise lines ───────────────────────────────────────────────
        section("5. PROMISES ON RECORD")
        if (promises.isEmpty()) {
            drawWrapped("No structured promises recorded for this listing.", bodyPaint)
        } else {
            promises.forEach { p ->
                newPageIfNeeded(LINE_H * 3.2f)
                drawWrapped("[${p.status}] ${humanize(p.category)} — ${p.title}", bodyBold)
                drawWrapped(p.originalText.ifBlank { "—" }, bodyPaint, indent = 12f)
                if (p.evidenceNote.isNotBlank()) drawWrapped(p.evidenceNote, smallPaint, indent = 12f)
                y += LINE_H * 0.5f
            }
        }

        // ── 6. Viewing / appointment history ───────────────────────────────
        section("6. VIEWING & INSPECTION HISTORY")
        if (viewings.isEmpty()) {
            drawWrapped("No scheduled viewings recorded in DORJA.", bodyPaint)
        } else {
            viewings.forEach { v: Viewing ->
                newPageIfNeeded(LINE_H * 2.2f)
                drawWrapped(
                    "${Formatters.formatDateOnly(v.startsAt)} — ${v.status} (pass ${v.passToken})",
                    bodyBold
                )
                drawWrapped("Consent-registered appointment between parties.", smallPaint, indent = 12f)
                y += LINE_H * 0.4f
            }
        }

        // ── Disclaimer ─────────────────────────────────────────────────────
        section("DISCLAIMER")
        drawWrapped(
            "This pack records claims, uploads and events as supplied through DORJA. " +
                "It is not legal advice, not a title certificate, and not a crime or safety clearance. " +
                "Evidence levels describe what DORJA has received, not what has been legally proved.",
            smallPaint
        )

        document.finishPage(page)

        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "DORJA_Decision_Pack_${listingId}.pdf")
        return try {
            FileOutputStream(file).use { document.writeTo(it) }
            file
        } catch (e: Exception) {
            null
        } finally {
            document.close()
        }
    }
}