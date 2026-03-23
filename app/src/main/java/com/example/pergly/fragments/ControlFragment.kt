package com.example.pergly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class ControlFragment : Fragment() {

    private lateinit var currentCommandText: TextView
    private lateinit var upBtn: MaterialButton
    private lateinit var downBtn: MaterialButton
    private lateinit var emergencyStopBtn: MaterialButton
    private lateinit var infoText: TextView

    private lateinit var database: DatabaseReference

    // Store listener for proper cleanup
    private var commandListener: ValueEventListener? = null

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
        setupControls()
        listenToCurrentCommand()
    }

    private fun initViews(view: View) {
        currentCommandText = view.findViewById(R.id.currentCommandText)
        upBtn = view.findViewById(R.id.upBtn)
        downBtn = view.findViewById(R.id.downBtn)
        emergencyStopBtn = view.findViewById(R.id.emergencyStopBtn)
        infoText = view.findViewById(R.id.infoText)
    }

    private fun setupControls() {
        upBtn.setOnClickListener {
            sendCommand("UP")
            Toast.makeText(context, "Moving UP", Toast.LENGTH_SHORT).show()
        }

        downBtn.setOnClickListener {
            sendCommand("DOWN")
            Toast.makeText(context, "Moving DOWN", Toast.LENGTH_SHORT).show()
        }

        emergencyStopBtn.setOnClickListener {
            sendCommand("EMERGENCY_STOP")
            Toast.makeText(context, "⚠️ EMERGENCY STOP ACTIVATED", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendCommand(command: String) {
        val commandData = mapOf(
            "command" to command,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("pergola/control/current_command").setValue(commandData)
            .addOnSuccessListener {
                // Command sent successfully
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to send command", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenToCurrentCommand() {
        commandListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val command = snapshot.child("command").getValue(String::class.java) ?: "NONE"
                currentCommandText.text = command

                when (command) {
                    "UP" -> currentCommandText.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
                    "DOWN" -> currentCommandText.setTextColor(resources.getColor(android.R.color.holo_blue_light, null))
                    "EMERGENCY_STOP" -> currentCommandText.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
                    else -> currentCommandText.setTextColor(resources.getColor(android.R.color.white, null))
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

    override fun onDestroyView() {
        super.onDestroyView()
        commandListener?.let {
            database.child("pergola/control/current_command").removeEventListener(it)
        }
    }
}