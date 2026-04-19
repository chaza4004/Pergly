package com.example.pergly.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class DashboardFragment : Fragment() {
    private lateinit var statusBadge: View

    private lateinit var powerValue: TextView
    private lateinit var todayTotalText: TextView
    private lateinit var modeBtn: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var statusDot: View

    private lateinit var windSpeedText: TextView
    private lateinit var windStatusText: TextView

    private lateinit var motor1Label: TextView
    private lateinit var motor1Percentage: TextView
    private lateinit var motor1Progress: ProgressBar

    private lateinit var motor2Label: TextView
    private lateinit var motor2Percentage: TextView
    private lateinit var motor2Progress: ProgressBar

    private lateinit var motor3Label: TextView
    private lateinit var motor3Percentage: TextView
    private lateinit var motor3Progress: ProgressBar

    private lateinit var motor4Label: TextView
    private lateinit var motor4Percentage: TextView
    private lateinit var motor4Progress: ProgressBar

    private lateinit var sensorNWDirection: TextView
    private lateinit var sensorNWValue: TextView

    private lateinit var sensorNEDirection: TextView
    private lateinit var sensorNEValue: TextView

    private lateinit var sensorSWDirection: TextView
    private lateinit var sensorSWValue: TextView

    private lateinit var sensorSEDirection: TextView
    private lateinit var sensorSEValue: TextView

    private lateinit var database: DatabaseReference

    private var isAutoMode = false
    private var currentWindSpeed = 0
    private val windThreshold = 30

    private var emergencyActive = false
    private var emergencySource = "none"

    private var powerListener: ValueEventListener? = null
    private var motorListener: ValueEventListener? = null
    private var sensorListener: ValueEventListener? = null
    private var windListener: ValueEventListener? = null
    private var emergencyListener: ValueEventListener? = null
    private var modeListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        initViews(view)
        setupModeButton()
        listenToSensorData()
        listenToMotorPositions()
        listenToLightSensors()
        listenToEmergencyState()
        listenToWindData()
    }

    private fun initViews(view: View) {
        statusBadge = view.findViewById(R.id.statusBadge)

        powerValue = view.findViewById(R.id.powerValue)
        todayTotalText = view.findViewById(R.id.todayTotalText)
        modeBtn = view.findViewById(R.id.modeBtn)
        statusText = view.findViewById(R.id.statusText)
        statusDot = view.findViewById(R.id.statusDot)

        windSpeedText = view.findViewById(R.id.windSpeedText)
        windStatusText = view.findViewById(R.id.windStatusText)

        val motor1Card = view.findViewById<View>(R.id.motor1Card)
        motor1Label = motor1Card.findViewById(R.id.motorLabel)
        motor1Percentage = motor1Card.findViewById(R.id.motorPercentage)
        motor1Progress = motor1Card.findViewById(R.id.motorProgress)
        motor1Label.text = "Motor 1"

        val motor2Card = view.findViewById<View>(R.id.motor2Card)
        motor2Label = motor2Card.findViewById(R.id.motorLabel)
        motor2Percentage = motor2Card.findViewById(R.id.motorPercentage)
        motor2Progress = motor2Card.findViewById(R.id.motorProgress)
        motor2Label.text = "Motor 2"

        val motor3Card = view.findViewById<View>(R.id.motor3Card)
        motor3Label = motor3Card.findViewById(R.id.motorLabel)
        motor3Percentage = motor3Card.findViewById(R.id.motorPercentage)
        motor3Progress = motor3Card.findViewById(R.id.motorProgress)
        motor3Label.text = "Motor 3"

        val motor4Card = view.findViewById<View>(R.id.motor4Card)
        motor4Label = motor4Card.findViewById(R.id.motorLabel)
        motor4Percentage = motor4Card.findViewById(R.id.motorPercentage)
        motor4Progress = motor4Card.findViewById(R.id.motorProgress)
        motor4Label.text = "Motor 4"

        val sensorNW = view.findViewById<View>(R.id.sensorNW)
        sensorNWDirection = sensorNW.findViewById(R.id.sensorDirection)
        sensorNWValue = sensorNW.findViewById(R.id.sensorValue)
        sensorNWDirection.text = "NW"

        val sensorNE = view.findViewById<View>(R.id.sensorNE)
        sensorNEDirection = sensorNE.findViewById(R.id.sensorDirection)
        sensorNEValue = sensorNE.findViewById(R.id.sensorValue)
        sensorNEDirection.text = "NE"

        val sensorSW = view.findViewById<View>(R.id.sensorSW)
        sensorSWDirection = sensorSW.findViewById(R.id.sensorDirection)
        sensorSWValue = sensorSW.findViewById(R.id.sensorValue)
        sensorSWDirection.text = "SW"

        val sensorSE = view.findViewById<View>(R.id.sensorSE)
        sensorSEDirection = sensorSE.findViewById(R.id.sensorDirection)
        sensorSEValue = sensorSE.findViewById(R.id.sensorValue)
        sensorSEDirection.text = "SE"
    }

    private fun setupModeButton() {
        modeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mode = snapshot.getValue(String::class.java)?.lowercase()?.trim() ?: "manual"
                isAutoMode = mode == "auto"
                updateModeButton()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardFragment", "Mode listener cancelled: ${error.message}")
            }
        }

        modeListener?.let {
            database.child("pergola/mode").addValueEventListener(it)
        }

        modeBtn.setOnClickListener {
            if (emergencyActive) {
                Toast.makeText(
                    requireContext(),
                    "Cannot change mode while emergency stop is active",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val newMode = if (isAutoMode) "manual" else "auto"
            database.child("pergola/mode").setValue(newMode)
        }
    }

    private fun updateModeButton() {
        if (!isAdded) return

        if (emergencyActive) {
            modeBtn.text = if (emergencySource == "manual") {
                "Mode: AUTO (MANUAL EMERGENCY LOCK)"
            } else {
                "Mode: AUTO (WIND SAFETY LOCK)"
            }

            modeBtn.isEnabled = false
            modeBtn.alpha = 0.6f
            modeBtn.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_red_dark)
            )
            return
        }

        modeBtn.isEnabled = true
        modeBtn.alpha = 1.0f

        if (isAutoMode) {
            modeBtn.text = "Mode: AUTO TRACKING"
            modeBtn.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_green_dark)
            )
        } else {
            modeBtn.text = "Mode: MANUAL CONTROL"
            modeBtn.setBackgroundColor(
                requireContext().getColor(android.R.color.holo_blue_dark)
            )
        }
    }

    private fun listenToSensorData() {
        statusText.text = "Loading..."
        statusText.setTextColor(requireContext().getColor(R.color.status_online))
        statusDot.setBackgroundResource(R.drawable.circle_green)

        powerValue.text = "--"
        todayTotalText.text = "--"

        powerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentWatts =
                    snapshot.child("current_watts").getValue(Double::class.java)
                        ?: snapshot.child("current_watts").getValue(Long::class.java)?.toDouble()
                        ?: 0.0

                val todayKwh =
                    snapshot.child("today_kwh").getValue(Double::class.java)
                        ?: snapshot.child("today_kwh").getValue(Long::class.java)?.toDouble()
                        ?: 0.0

                powerValue.text = String.format("%.0f", currentWatts)
                todayTotalText.text = String.format("%.2f kWh", todayKwh)

                if (isInternetAvailable()) {
                    statusText.text = "Online"
                    statusText.setTextColor(requireContext().getColor(R.color.status_online))
                    statusDot.setBackgroundResource(R.drawable.circle_green)
                    statusBadge.setBackgroundResource(R.drawable.status_badge)
                } else {
                    statusText.text = "Offline"
                    statusText.setTextColor(requireContext().getColor(R.color.status_offline))
                    statusDot.setBackgroundResource(R.drawable.circle_red)
                    statusBadge.setBackgroundResource(R.drawable.status_badge_red)
                }

                Log.d("API_SUCCESS", "Power loaded: $currentWatts")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("API_ERROR", "Power listener failed: ${error.message}")

                statusText.text = "Offline"
                statusText.setTextColor(requireContext().getColor(R.color.status_offline))
                statusDot.setBackgroundResource(R.drawable.circle_red)

                powerValue.text = "--"
                todayTotalText.text = "--"
            }
        }

        powerListener?.let {
            database.child("pergola/power").addValueEventListener(it)
        }
    }

    private fun listenToMotorPositions() {
        motorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val motor1 = snapshot.child("motor1").getValue(Long::class.java)?.toInt() ?: 0
                val motor2 = snapshot.child("motor2").getValue(Long::class.java)?.toInt() ?: 0
                val motor3 = snapshot.child("motor3").getValue(Long::class.java)?.toInt() ?: 0
                val motor4 = snapshot.child("motor4").getValue(Long::class.java)?.toInt() ?: 0

                updateMotorDisplay(motor1Percentage, motor1Progress, motor1)
                updateMotorDisplay(motor2Percentage, motor2Progress, motor2)
                updateMotorDisplay(motor3Percentage, motor3Progress, motor3)
                updateMotorDisplay(motor4Percentage, motor4Progress, motor4)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("API_ERROR", "Motor listener failed: ${error.message}")
            }
        }

        motorListener?.let {
            database.child("pergola/motors").addValueEventListener(it)
        }
    }

    private fun updateMotorDisplay(percentageView: TextView, progressBar: ProgressBar, value: Int) {
        val safeValue = value.coerceIn(0, 100)
        percentageView.text = "$safeValue%"
        progressBar.progress = safeValue
    }

    private fun listenToLightSensors() {
        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nw = snapshot.child("nw").getValue(Long::class.java)?.toInt() ?: 0
                val ne = snapshot.child("ne").getValue(Long::class.java)?.toInt() ?: 0
                val sw = snapshot.child("sw").getValue(Long::class.java)?.toInt() ?: 0
                val se = snapshot.child("se").getValue(Long::class.java)?.toInt() ?: 0

                sensorNWValue.text = "${nw.coerceIn(0, 100)}%"
                sensorNEValue.text = "${ne.coerceIn(0, 100)}%"
                sensorSWValue.text = "${sw.coerceIn(0, 100)}%"
                sensorSEValue.text = "${se.coerceIn(0, 100)}%"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("API_ERROR", "Sensor listener failed: ${error.message}")
            }
        }

        sensorListener?.let {
            database.child("pergola/light_sensors").addValueEventListener(it)
        }
    }

    private fun listenToWindData() {
        windSpeedText.text = "--"
        windStatusText.text = "Loading..."

        windListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val windSpeed =
                    snapshot.getValue(Long::class.java)?.toInt()
                        ?: snapshot.getValue(Double::class.java)?.toInt()
                        ?: 0

                currentWindSpeed = windSpeed
                windSpeedText.text = "$windSpeed km/h"

                windStatusText.text = if (windSpeed > windThreshold) {
                    "High wind - safety lock active"
                } else {
                    "Safe conditions"
                }

                Log.d("API_SUCCESS", "Wind loaded: $windSpeed")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("API_ERROR", "Wind listener failed: ${error.message}")
                windSpeedText.text = "--"
                windStatusText.text = "Offline"
            }
        }

        windListener?.let {
            database.child("pergola/weather/wind_speed").addValueEventListener(it)
        }
    }

    private fun listenToEmergencyState() {
        emergencyListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                emergencyActive =
                    snapshot.child("emergency_active").getValue(Boolean::class.java) ?: false
                emergencySource =
                    snapshot.child("emergency_source").getValue(String::class.java)
                        ?.lowercase()
                        ?.trim() ?: "none"
                updateModeButton()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardFragment", "Emergency listener cancelled: ${error.message}")
            }
        }

        emergencyListener?.let {
            database.child("pergola/safety").addValueEventListener(it)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        powerListener?.let { database.child("pergola/power").removeEventListener(it) }
        motorListener?.let { database.child("pergola/motors").removeEventListener(it) }
        sensorListener?.let { database.child("pergola/light_sensors").removeEventListener(it) }
        windListener?.let { database.child("pergola/weather/wind_speed").removeEventListener(it) }
        emergencyListener?.let { database.child("pergola/safety").removeEventListener(it) }
        modeListener?.let { database.child("pergola/mode").removeEventListener(it) }
    }
}