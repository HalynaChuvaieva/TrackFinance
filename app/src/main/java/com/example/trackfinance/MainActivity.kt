package com.example.trackfinance

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.RadioButton
import android.widget.RadioGroup

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        barChart = findViewById(R.id.barChart)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddTransaction)
        val btnLanguage = findViewById<Button>(R.id.btnLanguage)
        val btnTheme = findViewById<Button>(R.id.btnTheme)
        btnTheme.text = if (isDarkMode) "☀️" else "🌙"
        btnTheme.setOnClickListener {
            val nextDark = !isDarkMode
            prefs.edit().putBoolean("DARK_MODE", nextDark).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (nextDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
        // 1. Ім'я користувача
        val savedName = prefs.getString("USER_NAME", "User") ?: "User"
        tvGreeting.text = getString(R.string.greeting, savedName)

        tvGreeting.setOnClickListener {
            showChangeNameDialog(prefs)
        }

        // 2. Перемикач мов (EN <-> UA)
        btnLanguage.setOnClickListener {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val isUkrainian = currentLocales.toLanguageTags().startsWith("uk")
            val nextLang = if (isUkrainian) "en" else "uk"
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang))
        }

        // 3. Список транзакцій із кліком для редагування/видалення
        adapter = TransactionAdapter(emptyList()) { selectedTransaction ->
            showTransactionDialog(selectedTransaction)
        }
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        // 4. Кнопка "+" для нової транзакції
        fabAdd.setOnClickListener {
            showTransactionDialog(null)
        }

        updateDashboard()
    }

    private fun updateDashboard() {
        val dao = db.transactionDao()
        val transactions = dao.getAllTransactions()
        val totalIncome = dao.getTotalIncome() ?: 0.0
        val totalExpenses = dao.getTotalExpenses() ?: 0.0
        val balance = totalIncome - totalExpenses

        tvTotalBalance.text = "$ %.2f".format(balance)
        adapter.updateData(transactions)

        if (totalIncome == 0.0 && totalExpenses == 0.0) {
            barChart.clear()
            barChart.invalidate()
        } else {
            val entries = listOf(
                BarEntry(0f, totalIncome.toFloat()),
                BarEntry(1f, totalExpenses.toFloat())
            )
            val dataSet = BarDataSet(entries, getString(R.string.chart_legend))
            dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#EF5350"))
            dataSet.valueTextSize = 12f

            // Ось сюди переносимо ці рядки:
            val chartTextColor = getColor(R.color.text_primary)
            dataSet.valueTextColor = chartTextColor
            barChart.xAxis.textColor = chartTextColor
            barChart.axisLeft.textColor = chartTextColor
            barChart.axisRight.textColor = chartTextColor
            barChart.legend.textColor = chartTextColor

            barChart.data = BarData(dataSet)
            barChart.description.isEnabled = false
            barChart.animateY(500)
            barChart.invalidate()
        }
    }

    private fun showChangeNameDialog(prefs: android.content.SharedPreferences) {
        val input = EditText(this).apply {
            hint = getString(R.string.enter_name_hint)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.change_name_title))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    prefs.edit().putString("USER_NAME", newName).apply()
                    tvGreeting.text = getString(R.string.greeting, newName)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    // Універсальний діалог: якщо transaction == null -> додавання, інакше -> редагування/видалення
    private fun showTransactionDialog(existing: Transaction?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val etCategory = EditText(this).apply {
            hint = getString(R.string.category_hint)
            setText(existing?.category ?: "")
        }
        val etAmount = EditText(this).apply {
            hint = getString(R.string.amount_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (existing != null) setText(existing.amount.toString())
        }

        // Радіогрупа: Витрата / Дохід
        val radioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        val rbExpense = RadioButton(this).apply {
            text = getString(R.string.type_expense)
            setPadding(0, 0, 32, 0)
        }
        val rbIncome = RadioButton(this).apply {
            text = getString(R.string.type_income)
        }

        radioGroup.addView(rbExpense)
        radioGroup.addView(rbIncome)

        // За замовчуванням обираємо «Витрата», або поточний тип при редагуванні
        if (existing?.isIncome == true) {
            rbIncome.isChecked = true
        } else {
            rbExpense.isChecked = true
        }

        layout.addView(etCategory)
        layout.addView(etAmount)
        layout.addView(radioGroup)

        val builder = AlertDialog.Builder(this)
            .setTitle(if (existing == null) getString(R.string.new_transaction) else getString(R.string.edit_transaction))
            .setView(layout)
            .setPositiveButton(if (existing == null) getString(R.string.btn_add) else getString(R.string.btn_save)) { _, _ ->
                val category = etCategory.text.toString().trim()
                val amount = etAmount.text.toString().toDoubleOrNull()
                val isIncome = rbIncome.isChecked

                if (category.isEmpty() || amount == null || amount <= 0) {
                    Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
                } else {
                    if (existing == null) {
                        db.transactionDao().insert(
                            Transaction(amount = amount, category = category, isIncome = isIncome)
                        )
                    } else {
                        db.transactionDao().update(
                            existing.copy(amount = amount, category = category, isIncome = isIncome)
                        )
                    }
                    updateDashboard()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)

        if (existing != null) {
            builder.setNeutralButton(getString(R.string.btn_delete)) { _, _ ->
                db.transactionDao().delete(existing)
                updateDashboard()
            }
        }

        builder.show()
    }
}