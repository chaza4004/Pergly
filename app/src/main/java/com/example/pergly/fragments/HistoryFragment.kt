package com.example.pergly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var chartPlaceholder: View
    private lateinit var uptimeText: TextView
    private lateinit var dataPointsText: TextView

    private lateinit var database: DatabaseReference

    // Store listener for proper cleanup
    private var systemListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference

        initViews(view)
        loadSystemInfo()
    }

    private fun initViews(view: View) {
        chartPlaceholder = view.findViewById(R.id.chartPlaceholder)
        uptimeText = view.findViewById(R.id.uptimeText)
        dataPointsText = view.findViewById(R.id.dataPointsText)
    }

    private fun loadSystemInfo() {
        systemListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uptimeHours = snapshot.child("uptime_hours").getValue(Double::class.java) ?: 0.0
                val dataPoints = snapshot.child("data_points").getValue(Int::class.java) ?: 0

                uptimeText.text = String.format("%.1f hours", uptimeHours)
                dataPointsText.text = dataPoints.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                uptimeText.text = "-- hours"
                dataPointsText.text = "--"
            }
        }

        systemListener?.let {
            database.child("pergola/system").addValueEventListener(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        systemListener?.let {
            database.child("pergola/system").removeEventListener(it)
        }
    }
}