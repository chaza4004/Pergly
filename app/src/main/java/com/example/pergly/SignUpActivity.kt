package com.example.pergly

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class SignUpActivity : AppCompatActivity() {
    private lateinit var backBtn: MaterialButton


    private val auth = FirebaseAuth.getInstance()
    private lateinit var backBtn: MaterialButton
    private lateinit var database: DatabaseReference

    private lateinit var firstNameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText

    private lateinit var signUpBtn: MaterialButton
    private lateinit var loginText: MaterialTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        database = FirebaseDatabase.getInstance().reference

        initViews()
        setupListeners()
    }

    private fun initViews() {
        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        emailInput = findViewById(R.id.emailInput)
        locationInput = findViewById(R.id.locationInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)

        signUpBtn = findViewById(R.id.signUpBtn)
        loginText = findViewById(R.id.loginText)
        backBtn = findViewById(R.id.backBtn)
    }

    private fun setupListeners() {
        signUpBtn.setOnClickListener {
            signUpUser()
        }
        backBtn.setOnClickListener {
            finish()
        }

        loginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }


    private fun signUpUser() {
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val city = locationInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        if (firstName.isEmpty() ||
            lastName.isEmpty() ||
            email.isEmpty() ||
            city.isEmpty() ||
            password.isEmpty() ||
            confirmPassword.isEmpty()
        ) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        signUpBtn.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    if (userId == null) {
                        signUpBtn.isEnabled = true
                        Toast.makeText(this, "Signup failed. Please try again.", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val userMap = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "city" to city
                    )

                    database.child("users").child(userId).setValue(userMap)
                        .addOnSuccessListener {
                            user.sendEmailVerification()
                                .addOnSuccessListener {
                                    signUpBtn.isEnabled = true
                                    Toast.makeText(
                                        this,
                                        "Account created. Please verify your email before logging in.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    auth.signOut()
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    signUpBtn.isEnabled = true
                                    Toast.makeText(
                                        this,
                                        "Could not send verification email: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            signUpBtn.isEnabled = true
                            Toast.makeText(
                                this,
                                "Failed to save user data: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    signUpBtn.isEnabled = true

                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(
                                this,
                                "This email is already registered. Please log in instead.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Signup failed: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
    }
}
