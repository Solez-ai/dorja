package com.example.features.calculator

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinancialCalculatorTest {

    @Test
    fun testLoanCalculation_Standard() {
        val result = FinancialCalculator.calculateLoan(
            principal = 200000.0,
            annualRate = 5.0,
            years = 30
        )
        
        assertEquals(200000.0, result.principalAmount, 0.01)
        assertEquals(5.0, result.interestRate, 0.01)
        assertEquals(360, result.loanTermMonths)
        assertTrue(result.monthlyPayment > 0)
        assertEquals(360, result.amortizationSchedule.size)
    }

    @Test
    fun testAmortizationSchedule() {
        val result = FinancialCalculator.calculateLoan(
            principal = 100000.0,
            annualRate = 3.0,
            years = 10
        )
        
        val firstEntry = result.amortizationSchedule[0]
        assertTrue(firstEntry.interest > firstEntry.principal)
        
        val lastEntry = result.amortizationSchedule.last()
        assertTrue(lastEntry.principal > lastEntry.interest)
        
        assertEquals(0.0, lastEntry.balance, 1.0)
    }

    @Test
    fun testZeroInterestRate() {
        val result = FinancialCalculator.calculateLoan(
            principal = 100000.0,
            annualRate = 0.0,
            years = 10
        )
        
        val expectedMonthly = 100000.0 / 120
        assertEquals(expectedMonthly, result.monthlyPayment, 0.01)
        assertEquals(0.0, result.totalInterest, 0.01)
    }
}
