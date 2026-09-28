@file:OptIn(
    ExperimentalCupertinoApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DorjaApp
import com.example.data.country.CountryRegistry
import com.example.ui.components.DorjaChip
import com.example.ui.i18n.L
import com.example.ui.theme.DorjaColors
import com.example.ui.theme.DorjaFontFamily
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoTextField
import com.slapps.cupertino.ExperimentalCupertinoApi
import java.text.DecimalFormat
import kotlin.math.pow

/**
 * Per-country mortgage reality. Rates/fees are representative launch-market
 * defaults for estimation only — banks quote the binding numbers.
 */
private data class LoanProfile(
    val rateOptions: List<Double>,
    val maxTenureYears: Int,
    val minDownPct: Double,
    val stampDutyPct: Double,
    val regPct: Double
)

private object CountryLoanProfiles {
    private val generic = LoanProfile(
        rateOptions = listOf(7.5, 8.5, 9.5),
        maxTenureYears = 25,
        minDownPct = 15.0,
        stampDutyPct = 0.0,
        regPct = 0.0
    )

    private val map = mapOf(
        // Bangladesh: commercial ~11-13%, 10y typical cap, 15% min down,
        // stamp 1.5% + registration 1% + 2% local govt tax on deeds.
        "BD" to LoanProfile(
            rateOptions = listOf(10.5, 11.5, 12.5),
            maxTenureYears = 10,
            minDownPct = 15.0,
            stampDutyPct = 1.5,
            regPct = 3.0
        ),
        // India: competitive home-loan market, up to 30y tenure.
        "IN" to LoanProfile(
            rateOptions = listOf(8.0, 8.75, 9.5),
            maxTenureYears = 30,
            minDownPct = 20.0,
            stampDutyPct = 5.0,
            regPct = 1.0
        ),
        // Nepal.
        "NP" to LoanProfile(
            rateOptions = listOf(9.0, 10.0, 11.0),
            maxTenureYears = 20,
            minDownPct = 25.0,
            stampDutyPct = 5.0,
            regPct = 2.0
        ),
        // Bhutan: NPPF/BOB style subsidized housing loans.
        "BT" to LoanProfile(
            rateOptions = listOf(8.0, 9.0, 10.0),
            maxTenureYears = 20,
            minDownPct = 20.0,
            stampDutyPct = 2.0,
            regPct = 1.0
        ),
        // US: 30-year fixed is the benchmark product.
        "US" to LoanProfile(
            rateOptions = listOf(6.0, 6.75, 7.5),
            maxTenureYears = 30,
            minDownPct = 10.0,
            stampDutyPct = 1.0,
            regPct = 0.5
        ),
        // UK.
        "GB" to LoanProfile(
            rateOptions = listOf(4.5, 5.25, 6.0),
            maxTenureYears = 30,
            minDownPct = 10.0,
            stampDutyPct = 3.0,
            regPct = 0.0
        ),
        // UAE.
        "AE" to LoanProfile(
            rateOptions = listOf(4.0, 4.75, 5.5),
            maxTenureYears = 25,
            minDownPct = 20.0,
            stampDutyPct = 4.0,
            regPct = 0.25
        ),
        // Singapore: bank floating vs HDB concessionary.
        "SG" to LoanProfile(
            rateOptions = listOf(3.5, 4.25, 5.0),
            maxTenureYears = 30,
            minDownPct = 25.0,
            stampDutyPct = 3.0,
            regPct = 0.0
        )
    )

    fun get(iso2: String): LoanProfile =
        map[iso2.uppercase()] ?: generic
}

/** Annual rate → equivalent monthly compounding rate. */
private fun amr(annualPct: Double): Double = annualPct / 100.0 / 12.0

/** Monthly rate → effective annual rate (what banks actually charge yearly). */
private fun emr(monthlyRate: Double): Double =
    ((1.0 + monthlyRate).pow(12.0) - 1.0) * 100.0

private val amountFormat = DecimalFormat("#,##0")

private fun parseAmount(raw: String): Double =
    raw.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0

@Composable
fun FinancialCalculatorScreen(onBack: () -> Unit) {
    val user by DorjaApp.instance.repository.currentUser.collectAsState()
    var countryCode by remember(user?.countryCode) {
        mutableStateOf(user?.countryCode ?: "BD")
    }
    var showCountryPicker by remember { mutableStateOf(false) }

    val country = CountryRegistry.profile(countryCode)
    val loan = CountryLoanProfiles.get(countryCode)

    var priceText by remember { mutableStateOf("") }
    var downPct by remember(loan.minDownPct) { mutableStateOf(loan.minDownPct) }
    var tenureYears by remember(loan.maxTenureYears) {
        mutableStateOf(minOf(20, loan.maxTenureYears))
    }
    var rate by remember(loan.rateOptions) { mutableStateOf(loan.rateOptions.first()) }
    val clipboard = LocalClipboardManager.current

    val price = parseAmount(priceText)
    val loanAmount = price * (1.0 - downPct / 100.0)
    val stampDuty = price * loan.stampDutyPct / 100.0
    val regFee = price * loan.regPct / 100.0
    val monthlyRate = amr(rate)
    val nMonths = tenureYears * 12
    val emi = if (loanAmount > 0.0 && monthlyRate > 0.0 && nMonths > 0) {
        loanAmount * monthlyRate * (1.0 + monthlyRate).pow(nMonths) /
                ((1.0 + monthlyRate).pow(nMonths) - 1.0)
    } else 0.0
    val totalInterest = (emi * nMonths - loanAmount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DorjaColors.CanvasBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar — iOS plain style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "< " + L("common_back"),
                style = MaterialTheme.typography.bodyLarge,
                color = DorjaColors.Jol600,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onBack() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = L("calc_title"),
                style = MaterialTheme.typography.titleLarge,
                color = DorjaColors.Ink950,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Country ──────────────────────────────────────────────
            Surface12 {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCountryPicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = L("calc_country"),
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Gray600
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = country.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = DorjaColors.Ink950,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = country.currencyCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Jol600,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DorjaFontFamily
                    )
                }
            }

            // ── Price ────────────────────────────────────────────────
            Surface12 {
                Column(modifier = Modifier.padding(14.dp)) {
                    CalcField(
                        label = L("calc_price"),
                        value = priceText,
                        onValueChange = { priceText = it },
                        hint = country.currencySymbol + " 5,000,000"
                    )
                }
            }

            // ── Down payment ─────────────────────────────────────────
            Surface12 {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = L("calc_down_pct"),
                            style = MaterialTheme.typography.labelMedium,
                            color = DorjaColors.Gray600,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${downPct.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = DorjaColors.Jol600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CupertinoSlider(
                        value = (downPct / 100.0).toFloat(),
                        onValueChange = { downPct = (it * 100.0).toInt().toDouble() },
                        valueRange = 0f..0.9f
                    )
                    if (downPct < loan.minDownPct) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = L("calc_min_down") + " " + loan.minDownPct.toInt() + "%",
                            style = MaterialTheme.typography.labelSmall,
                            color = DorjaColors.Warning
                        )
                    }
                }
            }

            // ── Tenure ───────────────────────────────────────────────
            Surface12 {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = L("calc_tenure") + " (" + loan.maxTenureYears + " " + L("calc_years") + " " + L("calc_max") + ")",
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray600
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (5..loan.maxTenureYears step 5).forEach { years ->
                            DorjaChip(
                                selected = tenureYears == years,
                                label = years.toString(),
                                onClick = { tenureYears = years }
                            )
                        }
                    }
                }
            }

            // ── Rate ─────────────────────────────────────────────────
            Surface12 {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = L("calc_rate"),
                        style = MaterialTheme.typography.labelMedium,
                        color = DorjaColors.Gray600
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        loan.rateOptions.forEach { r ->
                            DorjaChip(
                                selected = rate == r,
                                label = "$r%",
                                onClick = { rate = r }
                            )
                        }
                    }
                }
            }

            // ── Breakdown ────────────────────────────────────────────
            Surface12(backgroundColor = DorjaColors.Jol600) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = L("calc_monthly"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = country.currencySymbol + " " +
                                amountFormat.format(emi.toLong()) + " / " + L("calc_month_short"),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (monthlyRate > 0.0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = L("calc_apr") + " " +
                                    String.format(java.util.Locale.US, "%.2f", emr(monthlyRate)) + "%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Surface12 {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    ResultRow(L("calc_price_label"), price, country.currencySymbol)
                    ResultRow(L("calc_stamp_duty"), stampDuty, country.currencySymbol,
                        note = loan.stampDutyPct.toString() + "%")
                    ResultRow(L("calc_reg_fee"), regFee, country.currencySymbol,
                        note = loan.regPct.toString() + "%")
                    ResultRow(L("calc_down_amount"), price * downPct / 100.0, country.currencySymbol,
                        note = downPct.toInt().toString() + "%")
                    ResultRow(L("calc_loan_amount"), loanAmount, country.currencySymbol)
                    ResultRow(L("calc_total_interest"), totalInterest, country.currencySymbol)
                    ResultRow(L("calc_total_payable"), loanAmount + totalInterest, country.currencySymbol)
                }
            }

            Text(
                text = L("calc_estimates_note"),
                style = MaterialTheme.typography.labelSmall,
                color = DorjaColors.Gray500,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Strings resolved in composition — L() is @Composable and must not
            // be called inside the onClick lambda.
            val sCountry = L("calc_country")
            val sPrice = L("calc_price_label")
            val sDown = L("calc_down_amount")
            val sLoan = L("calc_loan_amount")
            val sRate = L("calc_rate")
            val sTenure = L("calc_tenure")
            val sYears = L("calc_years")
            val sMonthly = L("calc_monthly")
            val sInterest = L("calc_total_interest")
            val sTitle = L("calc_title")

            CupertinoButton(
                onClick = {
                    // Copy a plain-text snapshot — shareable with bank/agent.
                    clipboard.setText(
                        AnnotatedString(
                            buildString {
                                appendLine(sCountry + ": " + country.displayName + " (" + country.currencyCode + ")")
                                appendLine(sPrice + ": " + country.currencySymbol + " " + amountFormat.format(price.toLong()))
                                appendLine(sDown + ": " + country.currencySymbol + " " + amountFormat.format((price * downPct / 100.0).toLong()) + " (" + downPct.toInt() + "%)")
                                appendLine(sLoan + ": " + country.currencySymbol + " " + amountFormat.format(loanAmount.toLong()))
                                appendLine(sRate + ": " + rate + "%  " + sTenure + ": " + tenureYears + " " + sYears)
                                appendLine(sMonthly + ": " + country.currencySymbol + " " + amountFormat.format(emi.toLong()))
                                appendLine(sInterest + ": " + country.currencySymbol + " " + amountFormat.format(totalInterest.toLong()))
                                append("DORJA " + sTitle)
                            }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(bottom = 16.dp)
                    .navigationBarsPadding(),
                colors = CupertinoButtonDefaults.plainButtonColors(contentColor = DorjaColors.Jol600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = L("calc_copy_summary"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showCountryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCountryPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = L("calc_country"),
                    style = MaterialTheme.typography.titleMedium,
                    color = DorjaColors.Ink950,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CountryRegistry.profiles.forEach { p ->
                        DorjaChip(
                            selected = p.iso2 == countryCode,
                            label = p.displayName,
                            onClick = {
                                countryCode = p.iso2
                                showCountryPicker = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Surface12(
    backgroundColor: Color = DorjaColors.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
    ) {
        content()
    }
}

@Composable
private fun CalcField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = DorjaColors.Gray600
    )
    Spacer(modifier = Modifier.height(6.dp))
    CupertinoTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = DorjaColors.Ink950,
            fontFamily = DorjaFontFamily,
            fontWeight = FontWeight.SemiBold
        ),
        placeholder = {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = DorjaColors.Gray500
            )
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        singleLine = true
    )
}

@Composable
private fun ResultRow(
    label: String,
    amount: Double,
    symbol: String,
    note: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (note != null) "$label · $note" else label,
            style = MaterialTheme.typography.bodyMedium,
            color = DorjaColors.Gray700,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = symbol + " " + amountFormat.format(amount.toLong()),
            style = MaterialTheme.typography.titleSmall,
            color = DorjaColors.Ink950,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DorjaFontFamily
        )
    }
}
