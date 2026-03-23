package com.example.pergly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class DashboardFragment : Fragment() {

    // Main displays
    private lateinit var powerValue: TextView
    private lateinit var todayTotalText: TextView
    private lateinit var modeBtn: MaterialButton
    private lateinit var statusText: TextView

    // Motor displays
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

    // Sensor displays
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

    // Store listeners for proper cleanup
    private var powerListener: ValueEventListener? = null
    private var motorListener: ValueEventListener? = null
    private var sensorListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
    }

    private fun initViews(view: View) {
        powerValue = view.findViewById(R.id.powerValue)
        todayTotalText = view.findViewById(R.id.todayTotalText)
        modeBtn = view.findViewById(R.id.modeBtn)
        statusText = view.findViewById(R.id.statusText)

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
        database.child("pergola/mode").get().addOnSuccessListener { snapshot ->
            val mode = snapshot.getValue(String::class.java) ?: "manual"
            isAutoMode = mode == "auto"
            updateModeButton()
        }

        modeBtn.setOnClickListener {
            isAutoMode = !isAutoMode
            val mode = if (isAutoMode) "auto" else "manual"

            database.child("pergola/mode").setValue(mode)
                .addOnSuccessListener {
                    updateModeButton()
                }
        }
    }

    private fun updateModeButton() {
        if (isAutoMode) {
            modeBtn.text = "Mode: AUTO TRACKING"
            modeBtn.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            modeBtn.text = "Mode: MANUAL CONTROL"
            modeBtn.setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, null))
        }
    }

    private fun listenToSensorData() {
        powerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentWatts = snapshot.child("current_watts").getValue(Double::class.java) ?: 0.0
                val todayKwh = snapshot.child("today_kwh").getValue(Double::class.java) ?: 0.0
                val online = snapshot.child("online").getValue(Boolean::class.java) ?: false

                powerValue.text = String.format("%.0f", currentWatts)
                todayTotalText.text = String.format("%.2f kWh", todayKwh)
                statusText.text = if (online) "Online" else "Offline"
            }

            override fun onCancelled(error: DatabaseError) {
                powerValue.text = "---"
                statusText.text = "Offline"
            }
        }

        powerListener?.let {
            database.child("pergola/power").addValueEventListener(it)
        }
    }

    private fun listenToMotorPositions() {
        motorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val motor1 = snapshot.child("motor1").getValue(Int::class.java) ?: 0
                val motor2 = snapshot.child("motor2").getValue(Int::class.java) ?: 0
                val motor3 = snapshot.child("motor3").getValue(Int::class.java) ?: 0
                val motor4 = snapshot.child("motor4").getValue(Int::class.java) ?: 0

                updateMotorDisplay(motor1Percentage, motor1Progress, motor1)
                updateMotorDisplay(motor2Percentage, motor2Progress, motor2)
                updateMotorDisplay(motor3Percentage, motor3Progress, motor3)
                updateMotorDisplay(motor4Percentage, motor4Progress, motor4)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        motorListener?.let {
            database.child("pergola/motors").addValueEventListener(it)
        }
    }

    private fun updateMotorDisplay(percentageView: TextView, progressBar: ProgressBar, value: Int) {
        percentageView.text = "$value%"
        progressBar.progress = value
    }

    private fun listenToLightSensors() {
        sensorListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nw = snapshot.child("nw").getValue(Int::class.java) ?: 0
                val ne = snapshot.child("ne").getValue(Int::class.java) ?: 0
                val sw = snapshot.child("sw").getValue(Int::class.java) ?: 0
                val se = snapshot.child("se").getValue(Int::class.java) ?: 0

                sensorNWValue.text = "$nw%"
                sensorNEValue.text = "$ne%"
                sensorSWValue.text = "$sw%"
                sensorSEValue.text = "$se%"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        sensorListener?.let {
            database.child("pergola/light_sensors").addValueEventListener(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove listeners properly
        powerListener?.let { database.child("pergola/power").removeEventListener(it) }
        motorListener?.let { database.child("pergola/motors").removeEventListener(it) }
        sensorListener?.let { database.child("pergola/light_sensors").removeEventListener(it) }
    }
}