package com.example.trackfinance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceUtilsTest {

    @Test
    fun calculateBalance_withMixedTransactions_returnsCorrectBalance() {
        val list = listOf(
            Transaction(id = 1, amount = 3000.0, category = "Salary", isIncome = true),
            Transaction(id = 2, amount = 500.0, category = "Groceries", isIncome = false),
            Transaction(id = 3, amount = 250.5, category = "Cafe", isIncome = false)
        )
        assertEquals(3000.0, FinanceUtils.calculateTotalIncome(list), 0.001)
        assertEquals(750.5, FinanceUtils.calculateTotalExpenses(list), 0.001)
        assertEquals(2249.5, FinanceUtils.calculateBalance(list), 0.001)
    }

    @Test
    fun calculateBalance_emptyList_returnsZero() {
        assertEquals(0.0, FinanceUtils.calculateBalance(emptyList()), 0.001)
    }

    @Test
    fun calculateBudgetPercent_normalAndOverflow_clampedTo100() {
        assertEquals(50, FinanceUtils.calculateBudgetPercent(500.0, 1000.0))
        assertEquals(100, FinanceUtils.calculateBudgetPercent(1500.0, 1000.0))
        assertEquals(0, FinanceUtils.calculateBudgetPercent(500.0, 0.0))
    }

    @Test
    fun isBudgetExceeded_checksThresholdCorrectly() {
        assertFalse(FinanceUtils.isBudgetExceeded(900.0, 1000.0))
        assertFalse(FinanceUtils.isBudgetExceeded(1000.0, 1000.0))
        assertTrue(FinanceUtils.isBudgetExceeded(1000.01, 1000.0))
    }

    @Test
    fun isValidTransaction_validAndInvalidCases() {
        assertTrue(FinanceUtils.isValidTransaction("Food", 150.0))
        assertFalse(FinanceUtils.isValidTransaction("", 150.0))
        assertFalse(FinanceUtils.isValidTransaction("   ", 150.0))
        assertFalse(FinanceUtils.isValidTransaction("Food", 0.0))
        assertFalse(FinanceUtils.isValidTransaction("Food", -50.0))
        assertFalse(FinanceUtils.isValidTransaction("Food", null))
    }

    @Test
    fun isValidPin_requiresExactly4Digits() {
        assertTrue(FinanceUtils.isValidPin("1234"))
        assertFalse(FinanceUtils.isValidPin("123"))
        assertFalse(FinanceUtils.isValidPin("12345"))
        assertFalse(FinanceUtils.isValidPin("12ab"))
    }

    @Test
    fun verifySecretWord_isCaseAndWhitespaceInsensitive() {
        assertTrue(FinanceUtils.verifySecretWord("  Barsik ", "barsik"))
        assertFalse(FinanceUtils.verifySecretWord("Rex", "barsik"))
    }


    @Test
    fun translateCategory_translatesKnownAndKeepsUnknown() {
        assertEquals("Salary", FinanceUtils.translateCategory("Зарплата", "en"))
        assertEquals("Продукти", FinanceUtils.translateCategory("groceries", "uk"))
        assertEquals("Netflix", FinanceUtils.translateCategory("Netflix", "uk"))
    }

    @Test
    fun formatReport_containsSummaryAndTransactions() {
        val list = listOf(
            Transaction(id = 1, amount = 1000.0, category = "Salary", isIncome = true),
            Transaction(id = 2, amount = 200.0, category = "Food", isIncome = false)
        )
        val report = FinanceUtils.formatReport(list, 1000.0, 200.0, 500.0)

        assertTrue(report.contains("Finance Manager Report"))
        assertTrue(report.contains("Salary"))
        assertTrue(report.contains("Food"))
    }
}