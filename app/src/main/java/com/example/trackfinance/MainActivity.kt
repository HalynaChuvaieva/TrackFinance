package com.example.trackfinance

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var tvGreeting: TextView
    private lateinit var tvTotalBalance: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var tvBudgetWarning: TextView
    private lateinit var progressBudget: ProgressBar
    private lateinit var barChart: BarChart
    private lateinit var btnPrivacy: Button
    private lateinit var rootView: View
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0L
    private var isPrivacyMode: Boolean = false
    private var monthlyBudgetLimit: Double = 1000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("DARK_MODE", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        rootView = findViewById(R.id.main)
        tvGreeting = findViewById(R.id.tvGreeting)
        tvTotalBalance = findViewById(R.id.tvTotalBalance)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)
        tvBudgetWarning = findViewById(R.id.tvBudgetWarning)
        progressBudget = findViewById(R.id.progressBudget)
        barChart = findViewById(R.id.barChart)
        btnPrivacy = findViewById(R.id.btnPrivacy)

        val cardBudget = findViewById<MaterialCardView>(R.id.cardBudget)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val fabAdd = findViewById<View>(R.id.fabAddTransaction)
        val btnLanguage = findViewById<Button>(R.id.btnLanguage)
        val btnTheme = findViewById<Button>(R.id.btnTheme)
        val btnShare = findViewById<Button>(R.id.btnShare)

        val savedName = prefs.getString("USER_NAME", "User") ?: "User"
        isPrivacyMode = prefs.getBoolean("PRIVACY_MODE", false)
        monthlyBudgetLimit = prefs.getFloat("BUDGET_LIMIT", 1000f).toDouble()

        tvGreeting.text = getString(R.string.greeting, savedName)
        tvGreeting.setOnClickListener { showChangeNameDialog(prefs) }

        // Режим приватності
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        btnPrivacy.setOnClickListener { togglePrivacyMode(prefs) }

        // Налаштування ліміту бюджету по кліку
        cardBudget.setOnClickListener { showSetBudgetDialog(prefs) }

        //  Експорт фінансового звіту
        btnShare.setOnClickListener { shareFinancialReport() }

        // Перемикач теми
        btnTheme.text = if (isDarkMode) "☀️" else "🌙"
        btnTheme.setOnClickListener {
            val nextDark = !isDarkMode
            prefs.edit().putBoolean("DARK_MODE", nextDark).apply()
            FinanceLogger.info("Theme changed: dark=$nextDark")
            AppCompatDelegate.setDefaultNightMode(
                if (nextDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Перемикач мови
        btnLanguage.setOnClickListener {
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            val isUkrainian = currentLocales.toLanguageTags().startsWith("uk")
            val nextLang = if (isUkrainian) "en" else "uk"
            FinanceLogger.info("Language changed to: $nextLang")
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nextLang))
        }

        // Список транзакцій
        adapter = TransactionAdapter(emptyList()) { selectedTransaction ->
            showTransactionDialog(selectedTransaction)
        }
        adapter.setPrivacyMode(isPrivacyMode)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        //  Свайп вліво для видалення
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedItem = adapter.getItemAt(position)

                db.transactionDao().delete(deletedItem)
                FinanceLogger.warning("Swiped & deleted transaction: ${deletedItem.category} (${deletedItem.amount})")
                updateDashboard()

                Snackbar.make(rootView, getString(R.string.transaction_deleted), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.btn_undo)) {
                        db.transactionDao().insert(deletedItem)
                        FinanceLogger.info("Undo delete transaction: ${deletedItem.category}")
                        updateDashboard()
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvTransactions)

        fabAdd.setOnClickListener { showTransactionDialog(null) }

        updateDashboard()

        val cardBalance = findViewById<View>(R.id.cardBalance)
        cardBalance.alpha = 0f
        cardBalance.scaleX = 0.94f
        cardBalance.scaleY = 0.94f
        cardBalance.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > 2.3) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 1200) {
                    lastShakeTime = now
                    val prefs = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
                    FinanceLogger.info("Device shaken -> toggling privacy mode")
                    togglePrivacyMode(prefs)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun togglePrivacyMode(prefs: android.content.SharedPreferences) {
        isPrivacyMode = !isPrivacyMode
        prefs.edit().putBoolean("PRIVACY_MODE", isPrivacyMode).apply()
        adapter.setPrivacyMode(isPrivacyMode)
        updateDashboard()

        val msg = if (isPrivacyMode) R.string.privacy_enabled else R.string.privacy_disabled
        Toast.makeText(this, getString(msg), Toast.LENGTH_SHORT).show()
        FinanceLogger.info("Privacy mode: $isPrivacyMode")
    }

    private fun updateDashboard() {
        val transactions = db.transactionDao().getAllTransactions()
        val totalIncome = FinanceUtils.calculateTotalIncome(transactions)
        val totalExpenses = FinanceUtils.calculateTotalExpenses(transactions)
        val balance = FinanceUtils.calculateBalance(transactions)

        btnPrivacy.text = if (isPrivacyMode) "🙈" else "👁️"
        tvTotalBalance.text = if (isPrivacyMode) "$ ****" else "$ %.2f".format(balance)
        adapter.updateData(transactions)

        val percent = FinanceUtils.calculateBudgetPercent(totalExpenses, monthlyBudgetLimit)
        val isExceeded = FinanceUtils.isBudgetExceeded(totalExpenses, monthlyBudgetLimit)

        progressBudget.progress = percent
        tvBudgetStatus.text = if (isPrivacyMode) {
            "**** / ****"
        } else {
            getString(R.string.budget_spent_format, totalExpenses, monthlyBudgetLimit)
        }

        val barColor = when {
            percent >= 90 -> Color.parseColor("#EF5350")
            percent >= 70 -> Color.parseColor("#FFA726")
            else -> Color.parseColor("#4CAF50")
        }
        progressBudget.progressTintList = ColorStateList.valueOf(barColor)
        tvBudgetWarning.visibility = if (isExceeded && !isPrivacyMode) View.VISIBLE else View.GONE

        if (isExceeded) {
            FinanceLogger.warning("Budget limit exceeded: $totalExpenses / $monthlyBudgetLimit")
        }

        if (totalIncome == 0.0 && totalExpenses == 0.0) {
            barChart.clear()
            barChart.invalidate()
        } else {
            val entries = listOf(
                BarEntry(0f, totalIncome.toFloat()),
                BarEntry(1f, totalExpenses.toFloat())
            )
            val dataSet = BarDataSet(entries, getString(R.string.chart_legend))
            dataSet.colors = listOf(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#EF5350")
            )
            dataSet.valueTextSize = 12f

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

    private fun shareFinancialReport() {
        val transactions = db.transactionDao().getAllTransactions()
        val totalIncome = FinanceUtils.calculateTotalIncome(transactions)
        val totalExpenses = FinanceUtils.calculateTotalExpenses(transactions)

        val report = FinanceUtils.formatReport(transactions, totalIncome, totalExpenses, monthlyBudgetLimit)

        FinanceLogger.info("Exporting financial report (${transactions.size} items)")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_report_title))
            putExtra(Intent.EXTRA_TEXT, report)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_report_title)))
        } catch (e: Exception) {
            FinanceLogger.error("Failed to share financial report", e)
        }
    }

    private fun showSetBudgetDialog(prefs: android.content.SharedPreferences) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_styled_form, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvModalTitle)
        val etLimit = dialogView.findViewById<EditText>(R.id.etModalFirst)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnModalCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnModalSave)

        tvTitle.text = getString(R.string.set_budget_title)
        etLimit.hint = getString(R.string.set_budget_hint)
        etLimit.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etLimit.setText(monthlyBudgetLimit.toString())

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newLimit = etLimit.text.toString().toDoubleOrNull()
            if (newLimit != null && newLimit > 0) {
                monthlyBudgetLimit = newLimit
                prefs.edit().putFloat("BUDGET_LIMIT", newLimit.toFloat()).apply()
                FinanceLogger.info("Budget limit updated to: $newLimit")
                updateDashboard()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showChangeNameDialog(prefs: android.content.SharedPreferences) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_styled_form, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvModalTitle)
        val etName = dialogView.findViewById<EditText>(R.id.etModalFirst)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnModalCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnModalSave)

        tvTitle.text = getString(R.string.change_name_title)
        etName.hint = getString(R.string.enter_name_hint)

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newName = etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                prefs.edit().putString("USER_NAME", newName).apply()
                tvGreeting.text = getString(R.string.greeting, newName)
                FinanceLogger.info("Username updated to: $newName")
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showTransactionDialog(existing: Transaction?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_styled_form, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvModalTitle)
        val etCategory = dialogView.findViewById<EditText>(R.id.etModalFirst)
        val etAmount = dialogView.findViewById<EditText>(R.id.etModalSecond)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgModalType)
        val rbExpense = dialogView.findViewById<RadioButton>(R.id.rbModalExpense)
        val rbIncome = dialogView.findViewById<RadioButton>(R.id.rbModalIncome)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnModalDelete)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnModalCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnModalSave)

        tvTitle.text = if (existing == null) getString(R.string.new_transaction) else getString(R.string.edit_transaction)
        etCategory.hint = getString(R.string.category_hint)
        etCategory.setText(existing?.category ?: "")

        etAmount.visibility = View.VISIBLE
        etAmount.hint = getString(R.string.amount_hint)
        if (existing != null) etAmount.setText(existing.amount.toString())

        rgType.visibility = View.VISIBLE
        if (existing?.isIncome == true) rbIncome.isChecked = true else rbExpense.isChecked = true

        btnSave.text = if (existing == null) getString(R.string.btn_add) else getString(R.string.btn_save)
        if (existing != null) btnDelete.visibility = View.VISIBLE

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            if (existing != null) {
                db.transactionDao().delete(existing)
                FinanceLogger.warning("Deleted transaction via dialog: ${existing.category}")
                updateDashboard()
                dialog.dismiss()
            }
        }

        btnSave.setOnClickListener {
            val category = etCategory.text.toString().trim()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val isIncome = rbIncome.isChecked

            if (!FinanceUtils.isValidTransaction(category, amount)) {
                FinanceLogger.warning("Invalid transaction input: category='$category', amount='$amount'")
                Toast.makeText(this, getString(R.string.invalid_input), Toast.LENGTH_SHORT).show()
            } else {
                val validAmount = amount!!
                if (existing == null) {
                    db.transactionDao().insert(
                        Transaction(amount = validAmount, category = category, isIncome = isIncome)
                    )
                    FinanceLogger.info("Added transaction: $category ($validAmount, income=$isIncome)")
                } else {
                    db.transactionDao().update(
                        existing.copy(amount = validAmount, category = category, isIncome = isIncome)
                    )
                    FinanceLogger.info("Updated transaction: $category ($validAmount, income=$isIncome)")
                }
                updateDashboard()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}