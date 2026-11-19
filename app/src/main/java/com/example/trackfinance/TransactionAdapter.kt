package com.example.trackfinance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private var items: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

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
        holder.tvCategory.text = item.category
        if (item.isIncome) {
            holder.tvAmount.text = "+$ %.2f".format(item.amount)
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            holder.tvAmount.text = "-$ %.2f".format(item.amount)
            holder.tvAmount.setTextColor(Color.parseColor("#C62828"))
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Transaction>) {
        items = newItems
        notifyDataSetChanged()
    }
}