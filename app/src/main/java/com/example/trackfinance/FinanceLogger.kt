package com.example.trackfinance
import android.util.Log

object FinanceLogger {
    private const val TAG = "FinanceAppLog"

    fun info(message: String) {
        Log.i(TAG, message)
    }

    fun warning(message: String) {
        Log.w(TAG, message)
    }

    fun error(message: String, exception: Throwable? = null) {
        Log.e(TAG, message, exception)
    }
}