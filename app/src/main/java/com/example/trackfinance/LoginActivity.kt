package com.example.trackfinance

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etPin = findViewById<EditText>(R.id.etPin)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnBiometric = findViewById<Button>(R.id.btnBiometric)

        val sharedPref = getSharedPreferences("FinancePrefs", Context.MODE_PRIVATE)
        val savedPin = sharedPref.getString("USER_PIN", null)

        // Налаштування входу за PIN-кодом
        btnLogin.setOnClickListener {
            val enteredPin = etPin.text.toString()
            if (enteredPin.length != 4) {
                Toast.makeText(this, "PIN має містити 4 цифри", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (savedPin == null) {
                // Перший запуск: зберігаємо новий PIN
                sharedPref.edit().putString("USER_PIN", enteredPin).apply()
                Toast.makeText(this, "PIN успішно збережено", Toast.LENGTH_SHORT).show()
                openMainActivity()
            } else {
                // Перевірка існуючого PIN
                if (enteredPin == savedPin) {
                    openMainActivity()
                } else {
                    Toast.makeText(this, "Невірний PIN-код", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Налаштування біометрії
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Помилка: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
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

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Закриваємо екран логіна, щоб користувач не міг повернутися назад кнопкою "Back"
    }
}