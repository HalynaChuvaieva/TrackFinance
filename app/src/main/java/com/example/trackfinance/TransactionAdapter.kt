package com.example.trackfinance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class TransactionAdapter(
    private var items: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var isPrivacyMode: Boolean = false

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

    private fun translateCategory(raw: String): String {
        val lang = Locale.getDefault().language
        val key = raw.trim().lowercase()
        return if (lang == "uk") enToUa[key] ?: raw else uaToEn[key] ?: raw
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvCategory.text = translateCategory(item.category)

        if (isPrivacyMode) {
            holder.tvAmount.text = "$ ****"
            holder.tvAmount.setTextColor(Color.GRAY)
        } else if (item.isIncome) {
            holder.tvAmount.text = "+$ %.2f".format(item.amount)
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            holder.tvAmount.text = "-$ %.2f".format(item.amount)
            holder.tvAmount.setTextColor(Color.parseColor("#EF5350"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun getItemAt(position: Int): Transaction = items[position]

    fun updateData(newItems: List<Transaction>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setPrivacyMode(enabled: Boolean) {
        isPrivacyMode = enabled
        notifyDataSetChanged()
    }
}