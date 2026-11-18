package com.example.trackfinance

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TransactionDao {
    // Додавання транзакції
    @Insert
    fun insert(transaction: Transaction)

    // Видалення
    @Delete
    fun delete(transaction: Transaction)

    // Отримання всіх транзакцій від найновіших до найстаріших
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): List<Transaction>

    // Підрахунок загального доходу
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Double?

    // Підрахунок загальних витрат
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpenses(): Double?
}