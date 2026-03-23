package com.example.pergly

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var emailInput: TextInputEditText
    private lateinit var resetBtn: MaterialButton
    private lateinit var loginText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        emailInput = findViewById(R.id.emailInput)
        resetBtn = findViewById(R.id.resetBtn)
        loginText = findViewById(R.id.loginText)
    }

    private fun setupListeners() {
        resetBtn.setOnClickListener {
            resetPassword()
        }

        loginText.setOnClickListener {
            finish()
        }

        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun resetPassword() {
        val email = emailInput.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter valid email", Toast.LENGTH_SHORT).show()
            return
        }

        resetBtn.isEnabled = false
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                resetBtn.isEnabled = true
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent! Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}