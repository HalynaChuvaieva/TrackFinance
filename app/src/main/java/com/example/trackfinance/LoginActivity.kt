package com.example.trackfinance

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)
        val tvForgotPin = findViewById<TextView>(R.id.tvForgotPin)

        sharedPref = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)

        // Вхід за PIN-кодом
        btnLogin.setOnClickListener {
            val enteredPin = etPin.text.toString()
            if (!FinanceUtils.isValidPin(enteredPin)) {
                Toast.makeText(this, getString(R.string.pin_length_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val savedPin = sharedPref.getString("USER_PIN", null)
            if (savedPin == null) {
                // Перший запуск: просимо задати секретне слово перед збереженням PIN
                showSetSecretWordDialog(enteredPin)
            } else {
                if (enteredPin == savedPin) {
                    FinanceLogger.info("User logged in with PIN")
                    openMainActivity()
                } else {
                    FinanceLogger.warning("Failed PIN login attempt")
                    Toast.makeText(this, getString(R.string.wrong_pin), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Відновлення PIN через секретне слово
        tvForgotPin.setOnClickListener {
            val savedSecret = sharedPref.getString("SECRET_WORD", null)
            if (savedSecret == null) {
                Toast.makeText(this, getString(R.string.no_secret_word), Toast.LENGTH_SHORT).show()
            } else {
                showRecoverPinDialog(savedSecret, etPin)
            }
        }

        // Біометрія
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    FinanceLogger.warning("Biometric auth error: $errString")
                    Toast.makeText(applicationContext, "Помилка: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    FinanceLogger.info("User logged in with biometrics")
                    openMainActivity()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Біометрична авторизація")
            .setSubtitle("Прикладіть палець для входу у Finance Manager")
            .setNegativeButtonText("Використати PIN")
            .build()

        btnBiometric.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun showSetSecretWordDialog(newPin: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recover_pin, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etSecret = dialogView.findViewById<EditText>(R.id.etDialogSecret)
        val etNewPin = dialogView.findViewById<EditText>(R.id.etDialogNewPin)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        tvTitle.text = getString(R.string.set_secret_word_title)
        tvSubtitle.visibility = android.view.View.VISIBLE
        etNewPin.visibility = android.view.View.GONE

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val secretWord = etSecret.text.toString().trim().lowercase()
            if (secretWord.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_secret_word), Toast.LENGTH_SHORT).show()
            } else {
                sharedPref.edit()
                    .putString("USER_PIN", newPin)
                    .putString("SECRET_WORD", secretWord)
                    .apply()
                FinanceLogger.info("Initial PIN and secret word configured")
                Toast.makeText(this, getString(R.string.pin_saved), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                openMainActivity()
            }
        }

        dialog.show()
    }

    private fun showRecoverPinDialog(savedSecret: String, etPin: EditText) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recover_pin, null)
        val etSecret = dialogView.findViewById<EditText>(R.id.etDialogSecret)
        val etNewPin = dialogView.findViewById<EditText>(R.id.etDialogNewPin)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnDialogCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val enteredSecret = etSecret.text.toString()
            val newPin = etNewPin.text.toString()

            when {
                !FinanceUtils.verifySecretWord(enteredSecret, savedSecret) -> {
                    FinanceLogger.warning("Failed PIN recovery: wrong secret word")
                    Toast.makeText(this, getString(R.string.wrong_secret_word), Toast.LENGTH_SHORT).show()
                }
                !FinanceUtils.isValidPin(newPin) -> {
                    Toast.makeText(this, getString(R.string.pin_length_error), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    sharedPref.edit().putString("USER_PIN", newPin).apply()
                    etPin.text.clear()
                    FinanceLogger.warning("User recovered and reset PIN via secret word")
                    Toast.makeText(this, getString(R.string.pin_updated), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}