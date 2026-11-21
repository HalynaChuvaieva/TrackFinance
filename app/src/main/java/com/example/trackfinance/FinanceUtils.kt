package com.example.trackfinance

object FinanceUtils {

    private val uaToEn = mapOf(
        "зарплата" to "Salary",
        "продукти" to "Groceries",
        "їжа" to "Food",
        "транспорт" to "Transport",
        "кафе" to "Cafe",
        "розваги" to "Entertainment",
        "комунальні" to "Utilities",
        "одяг" to "Clothes",
        "здоров'я" to "Health",
        "подарунки" to "Gifts",
        "стипендія" to "Scholarship"
    )

    private val enToUa = mapOf(
        "salary" to "Зарплата",
        "groceries" to "Продукти",
        "food" to "Їжа",
        "transport" to "Транспорт",
        "cafe" to "Кафе",
        "entertainment" to "Розваги",
        "utilities" to "Комунальні",
        "clothes" to "Одяг",
        "health" to "Здоров'я",
        "gifts" to "Подарунки",
        "scholarship" to "Стипендія"
    )

    fun calculateTotalIncome(transactions: List<Transaction>): Double =
        transactions.filter { it.isIncome }.sumOf { it.amount }

    fun calculateTotalExpenses(transactions: List<Transaction>): Double =
        transactions.filter { !it.isIncome }.sumOf { it.amount }

    fun calculateBalance(transactions: List<Transaction>): Double =
        calculateTotalIncome(transactions) - calculateTotalExpenses(transactions)

    fun calculateBudgetPercent(expenses: Double, limit: Double): Int {
        if (limit <= 0.0) return 0
        return ((expenses / limit) * 100).toInt().coerceIn(0, 100)
    }

    fun isBudgetExceeded(expenses: Double, limit: Double): Boolean =
        limit > 0.0 && expenses > limit

    fun isValidTransaction(category: String, amount: Double?): Boolean =
        category.trim().isNotEmpty() && amount != null && amount > 0.0

    fun isValidPin(pin: String): Boolean =
        pin.length == 4 && pin.all { it.isDigit() }

    fun verifySecretWord(entered: String, saved: String): Boolean =
        entered.trim().lowercase() == saved.trim().lowercase()

    fun translateCategory(raw: String, lang: String): String {
        val key = raw.trim().lowercase()
        return if (lang == "uk") enToUa[key] ?: raw else uaToEn[key] ?: raw
    }

    fun formatReport(
        transactions: List<Transaction>,
        totalIncome: Double,
        totalExpenses: Double,
        monthlyLimit: Double
    ): String {
        val balance = totalIncome - totalExpenses
        return buildString {
            appendLine("=== Finance Manager Report ===")
            appendLine("Total Balance: $ %.2f".format(balance))
            appendLine("Total Income: +$ %.2f".format(totalIncome))
            appendLine("Total Expenses: -$ %.2f".format(totalExpenses))
            appendLine("Monthly Limit: $ %.2f".format(monthlyLimit))
            appendLine()
            appendLine("--- Transactions ---")
            transactions.forEach { t ->
                val sign = if (t.isIncome) "+" else "-"
                appendLine("${t.category}: $sign$ %.2f".format(t.amount))
            }
        }
    }
}