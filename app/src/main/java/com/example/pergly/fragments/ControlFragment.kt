package com.example.pergly.fragments


import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class ControlFragment : Fragment() {

    private lateinit var currentCommandText: TextView
    private lateinit var selectedMotorText: TextView

    private lateinit var motor1PercentText: TextView
    private lateinit var motor2PercentText: TextView
    private lateinit var motor3PercentText: TextView
    private lateinit var motor4PercentText: TextView

    private lateinit var motor1SelectCard: MaterialCardView
    private lateinit var motor2SelectCard: MaterialCardView
    private lateinit var motor3SelectCard: MaterialCardView
    private lateinit var motor4SelectCard: MaterialCardView

    private lateinit var upBtn: MaterialButton
    private lateinit var downBtn: MaterialButton
    private lateinit var emergencyStopBtn: MaterialButton
    private lateinit var clearEmergencyBtn: MaterialButton
    private lateinit var infoText: TextView

    private lateinit var database: DatabaseReference

    private val emergencyCloseStep = 5
    private val emergencyCloseDelay = 200L

    private val emergencyHandler = Handler(Looper.getMainLooper())
    private var emergencyCloseRunnable: Runnable? = null
    private var isEmergencyClosing = false
    private var emergencyActive = false
    private var emergencySource = "none"
    private var emergencyStateListener: ValueEventListener? = null
    private var actionToast: Toast? = null
    private var currentMode = "manual"
    private var currentWindSpeed = 0
    private val windEmergencyOn = 30
    private val windEmergencyOff = 25
//    private var emergencyTriggered = false
    private var selectedMotor = "motor1"

    private var commandListener: ValueEventListener? = null
    private var modeListener: ValueEventListener? = null
    private var windListener: ValueEventListener? = null
    private var motorsListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_control, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        initViews(view)
        setupMotorSelection()
        setupControls()
        listenToCurrentCommand()
        listenToMode()
        listenToWind()
        listenToMotorPositions()
        listenToEmergencyState()
        updateSelectedMotorUI()
    }

    private fun initViews(view: View) {
        currentCommandText = view.findViewById(R.id.currentCommandText)
        selectedMotorText = view.findViewById(R.id.selectedMotorText)

        motor1PercentText = view.findViewById(R.id.motor1PercentText)
        motor2PercentText = view.findViewById(R.id.motor2PercentText)
        motor3PercentText = view.findViewById(R.id.motor3PercentText)
        motor4PercentText = view.findViewById(R.id.motor4PercentText)

        motor1SelectCard = view.findViewById(R.id.motor1SelectCard)
        motor2SelectCard = view.findViewById(R.id.motor2SelectCard)
        motor3SelectCard = view.findViewById(R.id.motor3SelectCard)
        motor4SelectCard = view.findViewById(R.id.motor4SelectCard)

        upBtn = view.findViewById(R.id.upBtn)
        downBtn = view.findViewById(R.id.downBtn)
        emergencyStopBtn = view.findViewById(R.id.emergencyStopBtn)
        clearEmergencyBtn = view.findViewById(R.id.clearEmergencyBtn)
        infoText = view.findViewById(R.id.infoText)
    }

    private fun setupMotorSelection() {
        motor1SelectCard.setOnClickListener { selectMotor("motor1") }
        motor2SelectCard.setOnClickListener { selectMotor("motor2") }
        motor3SelectCard.setOnClickListener { selectMotor("motor3") }
        motor4SelectCard.setOnClickListener { selectMotor("motor4") }
    }

    private fun selectMotor(motor: String) {
        val manualAllowed = currentMode == "manual" && currentWindSpeed <= windEmergencyOff && !emergencyActive

        if (!manualAllowed) {
            Toast.makeText(
                requireContext(),
                "Motor selection is disabled right now",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        selectedMotor = motor
        updateSelectedMotorUI()
        updateControlState()
    }

    private fun updateSelectedMotorUI() {
        if (!isAdded) return

        selectedMotorText.text = "Selected: ${formatMotorName(selectedMotor)}"

        styleMotorCard(motor1SelectCard, selectedMotor == "motor1")
        styleMotorCard(motor2SelectCard, selectedMotor == "motor2")
        styleMotorCard(motor3SelectCard, selectedMotor == "motor3")
        styleMotorCard(motor4SelectCard, selectedMotor == "motor4")
    }

    private fun styleMotorCard(card: MaterialCardView, selected: Boolean) {
        if (selected) {
            card.setCardBackgroundColor(resources.getColor(R.color.teal_700, null))
            card.strokeColor = resources.getColor(R.color.teal_200, null)
            card.strokeWidth = 3
        } else {
            card.setCardBackgroundColor(resources.getColor(R.color.control_motor_card_bg, null))
            card.strokeColor = resources.getColor(R.color.control_motor_card_stroke, null)
            card.strokeWidth = 1
        }
    }

    private fun setupControls() {
        upBtn.setOnClickListener {
            if (currentMode != "manual" || currentWindSpeed > windEmergencyOff || emergencyActive) {
                Toast.makeText(
                    requireContext(),
                    "Manual controls are disabled",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            sendCommand("UP", selectedMotor)
            updateSelectedMotorPercentage(+5)

            showFastToast("${formatMotorName(selectedMotor)} moving UP")
        }

        downBtn.setOnClickListener {
            if (currentMode != "manual" || currentWindSpeed > windEmergencyOff || emergencyActive) {
                Toast.makeText(
                    requireContext(),
                    "Manual controls are disabled",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            sendCommand("DOWN", selectedMotor)
            updateSelectedMotorPercentage(-5)

            showFastToast("${formatMotorName(selectedMotor)} moving DOWN")
        }

        emergencyStopBtn.setOnClickListener {
            sendManualEmergencyStop()
            showFastToast("⚠ Emergency stop activated")
        }
        clearEmergencyBtn.setOnClickListener {
            clearEmergencyStop()
        }
    }

    private fun sendCommand(command: String, motor: String) {
        val commandData = mapOf(
            "command" to command,
            "motor" to motor,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("pergola/control/current_command").setValue(commandData)
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to send command",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    private fun startEmergencyClosing(source: String) {
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
                                    showFastToast("Pergola returned to safe position")
                                } else {
                                    emergencyHandler.postDelayed(this, emergencyCloseDelay)
                                }
                            }
                            .addOnFailureListener {
                                isEmergencyClosing = false
                                Log.e("ControlFragment", "Failed during emergency closing: ${it.message}")
                            }
                    }
                    .addOnFailureListener {
                        isEmergencyClosing = false
                        Log.e("ControlFragment", "Failed to read motors during emergency closing: ${it.message}")
                    }
            }
        }

        emergencyHandler.post(emergencyCloseRunnable!!)
    }

    private fun updateSelectedMotorPercentage(delta: Int) {
        val motorRef = database.child("pergola/motors").child(selectedMotor)

        motorRef.get()
            .addOnSuccessListener { snapshot ->
                val currentValue = snapshot.getValue(Long::class.java)?.toInt()
                    ?: snapshot.getValue(Double::class.java)?.toInt()
                    ?: 0

                val newValue = (currentValue + delta).coerceIn(0, 100)

                motorRef.setValue(newValue)
                    .addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Failed to update motor position",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to read motor position",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendManualEmergencyStop() {
        startEmergencyClosing("manual")
    }

//    private fun sendWindEmergencyStop() {
//        val commandData = mapOf(
//            "command" to "EMERGENCY_STOP",
//            "timestamp" to System.currentTimeMillis()
//        )
//
//        database.child("pergola/mode").setValue("auto")
//        database.child("pergola/safety/emergency_active").setValue(true)
//        database.child("pergola/safety/emergency_source").setValue("wind")
//        database.child("pergola/control/current_command").setValue(commandData)
//    }

    private fun clearEmergencyStop() {
        if (currentWindSpeed > windEmergencyOff) {
            showFastToast("Cannot clear emergency while wind is high")
            return
        }

        if (isEmergencyClosing) {
            showFastToast("Wait until pergola reaches safe position")
            return
        }

        val commandData = mapOf(
            "command" to "NONE",
            "timestamp" to System.currentTimeMillis()
        )

        val updates = mapOf<String, Any>(
            "pergola/control/current_command" to commandData,
            "pergola/safety/emergency_active" to false,
            "pergola/safety/emergency_source" to "none"
        )

        database.updateChildren(updates)
            .addOnSuccessListener {
                showFastToast("Emergency cleared")
            }
            .addOnFailureListener {
                Log.e("ControlFragment", "Failed to clear emergency: ${it.message}")
            }
    }

    private fun listenToCurrentCommand() {
        commandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val command = snapshot.child("command").getValue(String::class.java) ?: "NONE"
                val motor = snapshot.child("motor").getValue(String::class.java)

                currentCommandText.text = when {
                    command == "EMERGENCY_STOP" -> "EMERGENCY STOP"
                    motor.isNullOrBlank() -> command
                    else -> "${formatMotorName(motor)} $command"
                }

                when (command) {
                    "UP" -> currentCommandText.setTextColor(
                        resources.getColor(android.R.color.holo_green_light, null)
                    )
                    "DOWN" -> currentCommandText.setTextColor(
                        resources.getColor(android.R.color.holo_blue_light, null)
                    )
                    "EMERGENCY_STOP" -> currentCommandText.setTextColor(
                        resources.getColor(android.R.color.holo_red_light, null)
                    )
                    else -> currentCommandText.setTextColor(
                        resources.getColor(android.R.color.white, null)
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                currentCommandText.text = "ERROR"
            }
        }

        commandListener?.let {
            database.child("pergola/control/current_command").addValueEventListener(it)
        }
    }

    private fun listenToMode() {
        modeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentMode = snapshot.getValue(String::class.java)?.lowercase()?.trim() ?: "manual"

                if (currentMode != "manual" && currentMode != "auto") {
                    currentMode = "manual"
                }

                updateControlState()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ControlFragment", "Mode listener cancelled: ${error.message}")
            }
        }

        modeListener?.let {
            database.child("pergola/mode").addValueEventListener(it)
        }
    }

    private fun listenToWind() {
        windListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentWindSpeed =
                    snapshot.getValue(Long::class.java)?.toInt()
                        ?: snapshot.getValue(Double::class.java)?.toInt()
                                ?: 0

                Log.d("ControlFragment", "Wind speed updated: $currentWindSpeed")
                updateControlState()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ControlFragment", "Wind listener cancelled: ${error.message}")
            }
        }

        windListener?.let {
            database.child("pergola/weather/wind_speed").addValueEventListener(it)
        }
    }

    private fun listenToMotorPositions() {
        motorsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val motor1 = snapshot.child("motor1").getValue(Long::class.java)?.toInt() ?: 0
                val motor2 = snapshot.child("motor2").getValue(Long::class.java)?.toInt() ?: 0
                val motor3 = snapshot.child("motor3").getValue(Long::class.java)?.toInt() ?: 0
                val motor4 = snapshot.child("motor4").getValue(Long::class.java)?.toInt() ?: 0

                motor1PercentText.text = "${motor1.coerceIn(0, 100)}%"
                motor2PercentText.text = "${motor2.coerceIn(0, 100)}%"
                motor3PercentText.text = "${motor3.coerceIn(0, 100)}%"
                motor4PercentText.text = "${motor4.coerceIn(0, 100)}%"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ControlFragment", "Motors listener cancelled: ${error.message}")
            }
        }

        motorsListener?.let {
            database.child("pergola/motors").addValueEventListener(it)
        }
    }

    private fun updateControlState() {
        if (!isAdded) return

        val manualAllowed = currentMode == "manual" && currentWindSpeed <= windEmergencyOff && !emergencyActive
        val canClearEmergency = currentWindSpeed <= windEmergencyOff && emergencyActive && emergencySource == "manual"

        upBtn.isEnabled = manualAllowed
        downBtn.isEnabled = manualAllowed

        upBtn.alpha = if (manualAllowed) 1.0f else 0.45f
        downBtn.alpha = if (manualAllowed) 1.0f else 0.45f

        clearEmergencyBtn.isEnabled = canClearEmergency
        clearEmergencyBtn.alpha = if (canClearEmergency) 1.0f else 0.45f

        motor1SelectCard.isClickable = manualAllowed
        motor2SelectCard.isClickable = manualAllowed
        motor3SelectCard.isClickable = manualAllowed
        motor4SelectCard.isClickable = manualAllowed

        motor1SelectCard.alpha = if (manualAllowed || selectedMotor == "motor1") 1.0f else 0.65f
        motor2SelectCard.alpha = if (manualAllowed || selectedMotor == "motor2") 1.0f else 0.65f
        motor3SelectCard.alpha = if (manualAllowed || selectedMotor == "motor3") 1.0f else 0.65f
        motor4SelectCard.alpha = if (manualAllowed || selectedMotor == "motor4") 1.0f else 0.65f

        infoText.text = when {
            emergencyActive && emergencySource == "wind" ->
                "⚠ Wind emergency is active. System is locked in AUTO mode and will clear automatically when wind becomes safe."

            emergencyActive && emergencySource == "manual" ->
                "⚠ Manual emergency stop is active. Manual controls are locked until you press CLEAR EMERGENCY."

            currentMode == "auto" ->
                "AUTO mode is active. The pergola follows the sun automatically using light sensors. Motor selection and manual controls are disabled."

            else ->
                "MANUAL mode is active. Selected motor: ${formatMotorName(selectedMotor)}. Use UP or DOWN to move only this motor."
        }
    }

    private fun formatMotorName(motor: String): String {
        return when (motor.lowercase()) {
            "motor1" -> "Motor 1"
            "motor2" -> "Motor 2"
            "motor3" -> "Motor 3"
            "motor4" -> "Motor 4"
            else -> motor
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        emergencyStateListener?.let {
            database.child("pergola/safety").removeEventListener(it)
        }

        commandListener?.let {
            database.child("pergola/control/current_command").removeEventListener(it)
        }

        modeListener?.let {
            database.child("pergola/mode").removeEventListener(it)
        }

        windListener?.let {
            database.child("pergola/weather/wind_speed").removeEventListener(it)
        }

        motorsListener?.let {
            database.child("pergola/motors").removeEventListener(it)
        }
        emergencyCloseRunnable?.let {
            emergencyHandler.removeCallbacks(it)
        }
        isEmergencyClosing = false
    }
    private fun showFastToast(message: String) {
        actionToast?.cancel()
        actionToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        actionToast?.show()
    }
    private fun listenToEmergencyState() {
        emergencyStateListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                emergencyActive = snapshot.child("emergency_active").getValue(Boolean::class.java) ?: false
                emergencySource = snapshot.child("emergency_source").getValue(String::class.java)?.lowercase()?.trim() ?: "none"
                updateControlState()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ControlFragment", "Emergency state listener cancelled: ${error.message}")
            }
        }

        emergencyStateListener?.let {
            database.child("pergola/safety").addValueEventListener(it)
        }
    }

}
