package com.example.pergly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Animate logo and text
        val logo = findViewById<ImageView>(R.id.splashLogo)
        val title = findViewById<TextView>(R.id.splashTitle)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        logo.startAnimation(fadeIn)
        title.startAnimation(fadeIn)
        subtitle.startAnimation(fadeIn)

        // Delay for splash screen then check auth
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthState()
        }, 2500) // 2.5 seconds
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        val intent = if (currentUser != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()

        // Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}