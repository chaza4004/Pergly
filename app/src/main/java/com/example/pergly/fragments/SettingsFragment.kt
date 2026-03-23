
package com.example.pergly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment() {

    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var connectionStatusText: TextView
    private lateinit var hardwareInfoCard: MaterialCardView
    private lateinit var systemUpdatesCard: MaterialCardView

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var database: DatabaseReference

    // Store listener for proper cleanup
    private var connectionListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        initViews(view)
        loadUserInfo()
        checkConnectionStatus()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        userNameText = view.findViewById(R.id.userNameText)
        userEmailText = view.findViewById(R.id.userEmailText)
        connectionStatusText = view.findViewById(R.id.connectionStatusText)
        hardwareInfoCard = view.findViewById(R.id.hardwareInfoCard)
        systemUpdatesCard = view.findViewById(R.id.systemUpdatesCard)
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        userEmailText.text = user?.email ?: "No email"

        user?.uid?.let { uid ->
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        userNameText.text = "$firstName $lastName"
                    } else {
                        userNameText.text = "User"
                    }
                }
                .addOnFailureListener {
                    userNameText.text = "User"
                }
        }
    }

    private fun checkConnectionStatus() {
        connectionListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val online = snapshot.getValue(Boolean::class.java) ?: false
                connectionStatusText.text = if (online) "Online" else "Offline"
                connectionStatusText.setTextColor(
                    if (online)
                        resources.getColor(android.R.color.holo_green_light, null)
                    else
                        resources.getColor(android.R.color.holo_red_light, null)
                )
            }

            override fun onCancelled(error: DatabaseError) {
                connectionStatusText.text = "Error"
            }
        }

        connectionListener?.let {
            database.child("pergola/power/online").addValueEventListener(it)
        }
    }

    private fun setupClickListeners() {
        hardwareInfoCard.setOnClickListener {
            // TODO: Navigate to hardware info screen
        }

        systemUpdatesCard.setOnClickListener {
            // TODO: Navigate to system updates screen
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectionListener?.let {
            database.child("pergola/power/online").removeEventListener(it)
        }
    }
}