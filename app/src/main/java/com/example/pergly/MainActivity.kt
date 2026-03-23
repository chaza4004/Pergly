package com.example.pergly

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.pergly.fragments.DashboardFragment
import com.example.pergly.fragments.ControlFragment
import com.example.pergly.fragments.HistoryFragment
import com.example.pergly.fragments.SettingsFragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupToolbar()
        setupNavigationDrawer()
        loadUserHeader()

        // Load default fragment - Dashboard
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            navigationView.setCheckedItem(R.id.nav_dashboard)
            supportActionBar?.title = "Dashboard"
        }
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title for custom header
    }

    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            findViewById(R.id.toolbar),
            R.string.app_name,
            R.string.app_name
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun loadUserHeader() {
        val user = auth.currentUser ?: return
        val headerView = navigationView.getHeaderView(0)
        val nameText = headerView.findViewById<TextView>(R.id.navHeaderName)
        val emailText = headerView.findViewById<TextView>(R.id.navHeaderEmail)

        emailText.text = user.email

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    nameText.text = "$firstName $lastName"
                } else {
                    nameText.text = "User"
                }
            }
            .addOnFailureListener {
                nameText.text = "User"
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                loadFragment(DashboardFragment())
                supportActionBar?.title = "Dashboard"
            }
            R.id.nav_control -> {
                loadFragment(ControlFragment())
                supportActionBar?.title = "Control"
            }
            R.id.nav_history -> {
                loadFragment(HistoryFragment())
                supportActionBar?.title = "History"
            }
            R.id.nav_settings -> {
                loadFragment(SettingsFragment())
                supportActionBar?.title = "Settings"
            }
            R.id.nav_logout -> {
                logout()
                return true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}