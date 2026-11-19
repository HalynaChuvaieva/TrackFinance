package com.example.trackfinance

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        tvGreeting = findViewById(R.id.tvGreeting)
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        barChart = findViewById(R.id.barChart)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddTransaction)

        // 1. Завантаження та зміна імені користувача
        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("USER_NAME", "User")
        tvGreeting.text = "Hi, $savedName ✎"

        tvGreeting.setOnClickListener {
            showChangeNameDialog(prefs)
        }

        // 2. Налаштування списку транзакцій
        adapter = TransactionAdapter(emptyList())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        // 3. Кнопка "+" для додавання нової транзакції
        fabAdd.setOnClickListener {
            showAddTransactionDialog()
        }

        // 4. Завантаження реальних даних з БД
        updateDashboard()
    }

    private fun updateDashboard() {
        val dao = db.transactionDao()
        val transactions = dao.getAllTransactions()
        val totalIncome = dao.getTotalIncome() ?: 0.0
        val totalExpenses = dao.getTotalExpenses() ?: 0.0
        val balance = totalIncome - totalExpenses

        // Оновлюємо баланс та список
        tvTotalBalance.text = "$ %.2f".format(balance)
        adapter.updateData(transactions)

        // Оновлюємо графік (Доходи vs Витрати)
        if (totalIncome == 0.0 && totalExpenses == 0.0) {
            barChart.clear()
            barChart.invalidate()
        } else {
            val entries = listOf(
                BarEntry(0f, totalIncome.toFloat()),
                BarEntry(1f, totalExpenses.toFloat())
            )
            val dataSet = BarDataSet(entries, "Доходи (зелений) / Витрати (синій)")
            dataSet.colors = listOf(Color.parseColor("#4CAF50"), Color.parseColor("#29B6F6"))
            dataSet.valueTextSize = 12f

            barChart.data = BarData(dataSet)
            barChart.description.isEnabled = false
            barChart.animateY(500)
            barChart.invalidate()
        }
    }

    private fun showChangeNameDialog(prefs: android.content.SharedPreferences) {
        val input = EditText(this)
        input.hint = "Введіть ваше ім'я"

        AlertDialog.Builder(this)
            .setTitle("Змінити ім'я")
            .setView(input)
            .setPositiveButton("Зберегти") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    prefs.edit().putString("USER_NAME", newName).apply()
                    tvGreeting.text = "Hi, $newName ✎"
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showAddTransactionDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val etCategory = EditText(this).apply { hint = "Категорія (напр. Продукти, Зарплата)" }
        val etAmount = EditText(this).apply {
            hint = "Сума"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val cbIsIncome = CheckBox(this).apply { text = "Це дохід (якщо ні — витрата)" }

        layout.addView(etCategory)
        layout.addView(etAmount)
        layout.addView(cbIsIncome)

        AlertDialog.Builder(this)
            .setTitle("Нова транзакція")
            .setView(layout)
            .setPositiveButton("Додати") { _, _ ->
                val category = etCategory.text.toString().trim()
                val amount = etAmount.text.toString().toDoubleOrNull()

                if (category.isEmpty() || amount == null || amount <= 0) {
                    Toast.makeText(this, "Введіть коректну категорію та суму", Toast.LENGTH_SHORT).show()
                } else {
                    db.transactionDao().insert(
                        Transaction(amount = amount, category = category, isIncome = cbIsIncome.isChecked)
                    )
                    updateDashboard()
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
}