package com.example.pergly

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pergly.fragments.ControlFragment
import com.example.pergly.fragments.DashboardFragment
import com.example.pergly.fragments.HistoryFragment
import com.example.pergly.fragments.SettingsFragment
import com.example.pergly.local.SensorHistoryEntity
import com.example.pergly.viewmodel.SensorHistoryViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var database: DatabaseReference
    private var globalWindListener: ValueEventListener? = null

    private lateinit var historyViewModel: SensorHistoryViewModel
    private var lastSavedTime = 0L

    private val windEmergencyOn = 30
    private val windEmergencyOff = 25

    private val emergencyCloseStep = 5
    private val emergencyCloseDelay = 200L

    private val emergencyHandler = Handler(Looper.getMainLooper())
    private var emergencyCloseRunnable: Runnable? = null

    private var isEmergencyClosing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        historyViewModel = ViewModelProvider(this)[SensorHistoryViewModel::class.java]
        database = FirebaseDatabase.getInstance().reference

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        startGlobalWindSafetyMonitor()

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_control -> {
                    loadFragment(ControlFragment())
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
            bottomNavigation.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun startGlobalWindSafetyMonitor() {
        globalWindListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val windSpeed =
                    snapshot.getValue(Long::class.java)?.toInt()
                        ?: snapshot.getValue(Double::class.java)?.toInt()
                        ?: 0

                Log.d("FIREBASE_DATA", "Received wind from Firebase: $windSpeed")

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSavedTime > 60000) {
                    lastSavedTime = currentTime

                    val historyItem = SensorHistoryEntity(
                        windSpeed = windSpeed.toFloat(),
                        motorStatus = if (isEmergencyClosing) "CLOSING" else "NORMAL"
                    )

                    historyViewModel.insertHistory(historyItem)
                    Log.d("ROOM_SAVE", "Saved to Room: wind=$windSpeed")
                }

                database.child("pergola/safety").get()
                    .addOnSuccessListener { safetySnapshot ->
                        val emergencyActive = safetySnapshot.child("emergency_active")
                            .getValue(Boolean::class.java) ?: false

                        val emergencySource = safetySnapshot.child("emergency_source")
                            .getValue(String::class.java)
                            ?.trim()
                            ?.lowercase() ?: "none"

                        Log.d(
                            "MainActivity",
                            "Wind=$windSpeed, emergencyActive=$emergencyActive, source=$emergencySource"
                        )

                        if (windSpeed >= windEmergencyOn) {
                            if (!emergencyActive || emergencySource != "wind") {
                                startEmergencyClosingFromMain("wind")
                            }
                        } else if (windSpeed <= windEmergencyOff) {
                            if (emergencyActive && emergencySource == "wind") {
                                val commandData = mapOf(
                                    "command" to "NONE",
                                    "timestamp" to System.currentTimeMillis()
                                )

                                val updates = mapOf<String, Any>(
                                    "pergola/safety/emergency_active" to false,
                                    "pergola/safety/emergency_source" to "none",
                                    "pergola/control/current_command" to commandData
                                )

                                database.updateChildren(updates)
                                    .addOnSuccessListener {
                                        Log.d("MainActivity", "Wind emergency cleared")
                                    }
                                    .addOnFailureListener {
                                        Log.e(
                                            "MainActivity",
                                            "Failed to clear wind emergency: ${it.message}"
                                        )
                                    }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e("MainActivity", "Failed to read safety state: ${it.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Global wind listener cancelled: ${error.message}")
            }
        }

        globalWindListener?.let {
            database.child("pergola/weather/wind_speed").addValueEventListener(it)
        }
    }

    private fun startEmergencyClosingFromMain(source: String) {
        if (isEmergencyClosing) return

        isEmergencyClosing = true

        val commandData = mapOf(
            "command" to "EMERGENCY_STOP",
            "timestamp" to System.currentTimeMillis()
        )

        val startUpdates = mapOf<String, Any>(
            "pergola/mode" to "auto",
            "pergola/safety/emergency_active" to true,
            "pergola/safety/emergency_source" to source,
            "pergola/control/current_command" to commandData
        )

        database.updateChildren(startUpdates)

        emergencyCloseRunnable = object : Runnable {
            override fun run() {
                database.child("pergola/motors").get()
                    .addOnSuccessListener { snapshot ->
                        val motor1 = snapshot.child("motor1").getValue(Long::class.java)?.toInt() ?: 0
                        val motor2 = snapshot.child("motor2").getValue(Long::class.java)?.toInt() ?: 0
                        val motor3 = snapshot.child("motor3").getValue(Long::class.java)?.toInt() ?: 0
                        val motor4 = snapshot.child("motor4").getValue(Long::class.java)?.toInt() ?: 0

                        val newMotor1 = (motor1 - emergencyCloseStep).coerceAtLeast(0)
                        val newMotor2 = (motor2 - emergencyCloseStep).coerceAtLeast(0)
                        val newMotor3 = (motor3 - emergencyCloseStep).coerceAtLeast(0)
                        val newMotor4 = (motor4 - emergencyCloseStep).coerceAtLeast(0)

                        val updates = mapOf<String, Any>(
                            "pergola/motors/motor1" to newMotor1,
                            "pergola/motors/motor2" to newMotor2,
                            "pergola/motors/motor3" to newMotor3,
                            "pergola/motors/motor4" to newMotor4
                        )

                        database.updateChildren(updates)
                            .addOnSuccessListener {
                                val allClosed = newMotor1 == 0 &&
                                        newMotor2 == 0 &&
                                        newMotor3 == 0 &&
                                        newMotor4 == 0

                                if (allClosed) {
                                    isEmergencyClosing = false
                                    Log.d("MainActivity", "Pergola returned to safe position")
                                } else {
                                    emergencyHandler.postDelayed(this, emergencyCloseDelay)
                                }
                            }
                            .addOnFailureListener {
                                isEmergencyClosing = false
                                Log.e(
                                    "MainActivity",
                                    "Failed during emergency closing: ${it.message}"
                                )
                            }
                    }
                    .addOnFailureListener {
                        isEmergencyClosing = false
                        Log.e(
                            "MainActivity",
                            "Failed to read motors during emergency closing: ${it.message}"
                        )
                    }
            }
        }

        emergencyHandler.post(emergencyCloseRunnable!!)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()

        globalWindListener?.let {
            database.child("pergola/weather/wind_speed").removeEventListener(it)
        }

        emergencyCloseRunnable?.let {
            emergencyHandler.removeCallbacks(it)
        }

        isEmergencyClosing = false
    }
}