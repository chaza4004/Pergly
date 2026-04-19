package com.example.pergly.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pergly.R
import com.google.firebase.database.*

class HistoryFragment : Fragment() {

    private lateinit var chartPlaceholder: LinearLayout
    private lateinit var peakPowerText: TextView
    private lateinit var totalPowerText: TextView
    private lateinit var uptimeText: TextView
    private lateinit var dataPointsText: TextView

    private lateinit var database: DatabaseReference

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
        loadDemoChart()
        loadSystemInfo()
    }

    private fun initViews(view: View) {
        chartPlaceholder = view.findViewById(R.id.chartPlaceholder)
        uptimeText = view.findViewById(R.id.uptimeText)
        dataPointsText = view.findViewById(R.id.dataPointsText)
        peakPowerText = view.findViewById(R.id.peakPowerText)
        totalPowerText = view.findViewById(R.id.totalPowerText)
    }

    private fun loadDemoChart() {
        val baseValues = listOf(
            0, 0, 0, 0, 1, 3, 6, 10, 15, 20, 24, 28,
            30, 27, 23, 18, 12, 7, 3, 1, 0, 0, 0, 0
        )

        val demoValues = baseValues.map { base ->
            if (base == 0) 0 else (base + (0..3).random()).coerceAtMost(32)
        }
        val peak = demoValues.maxOrNull() ?: 0
        val total = demoValues.sum()

        peakPowerText.text = "Peak: ${peak} W"
        totalPowerText.text = "Total: ${total} Wh"

        chartPlaceholder.removeAllViews()

        val maxValue = (demoValues.maxOrNull() ?: 1).toFloat()
        val chartHeightDp = 176f
        val density = resources.displayMetrics.density

        for (value in demoValues) {
            val bar = View(requireContext())

            val barHeightDp = if (value == 0) 6f else (value / maxValue) * chartHeightDp
            val barHeightPx = (barHeightDp * density).toInt()
            val barWidthPx = (8 * density).toInt()
            val marginPx = (2 * density).toInt()

            val params = LinearLayout.LayoutParams(barWidthPx, barHeightPx)
            params.marginEnd = marginPx
            bar.layoutParams = params

            bar.setBackgroundResource(R.drawable.chart_bar_bg)

            chartPlaceholder.addView(bar)
        }
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