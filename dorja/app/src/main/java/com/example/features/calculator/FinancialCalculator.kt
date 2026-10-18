package com.example.features.calculator

import kotlin.math.pow

data class LoanCalculation(
    val principalAmount: Double,
    val interestRate: Double,
    val loanTermMonths: Int,
    val monthlyPayment: Double,
    val totalInterest: Double,
    val amortizationSchedule: List<AmortizationEntry>
)

data class AmortizationEntry(
    val month: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val balance: Double
)

object FinancialCalculator {

    fun calculateLoan(
        principal: Double,
        annualRate: Double,
        years: Int
    ): LoanCalculation {
        val monthlyRate = annualRate / 100 / 12
        val months = years * 12

        val monthlyPayment = if (monthlyRate > 0) {
            principal * (monthlyRate * (1 + monthlyRate).pow(months)) /
                    ((1 + monthlyRate).pow(months) - 1)
        } else {
            principal / months
        }

        val schedule = mutableListOf<AmortizationEntry>()
        var balance = principal

        for (month in 1..months) {
            val interestPayment = balance * monthlyRate
            val principalPayment = monthlyPayment - interestPayment
            balance -= principalPayment

            schedule.add(
                AmortizationEntry(
                    month = month,
                    payment = monthlyPayment,
                    principal = principalPayment,
                    interest = interestPayment,
                    balance = maxOf(0.0, balance)
                )
            )
        }

        return LoanCalculation(
            principalAmount = principal,
            interestRate = annualRate,
            loanTermMonths = months,
            monthlyPayment = monthlyPayment,
            totalInterest = schedule.sumOf { it.interest },
            amortizationSchedule = schedule
        )
    }
}
