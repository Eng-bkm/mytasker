package com.example.taskmanager

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DayOfWeekAdapter(
    private val daysOfWeek: List<String>,
    private val onDayOfWeekClicked: (Date) -> Unit,
    private var selectedDay: Int,
    private var isWeeklyView: Boolean,
    private var currentWeekStart: Date
) : RecyclerView.Adapter<DayOfWeekAdapter.DayOfWeekViewHolder>() {

    inner class DayOfWeekViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayOfWeek: TextView = itemView.findViewById(R.id.tvDayOfWeek)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayOfWeekViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_day_of_week, parent, false)
        return DayOfWeekViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DayOfWeekViewHolder, position: Int) {
        val dayOfWeek = daysOfWeek[position]
        holder.tvDayOfWeek.text = dayOfWeek

        // Highlight the selected day
        if (position == selectedDay) {
            holder.tvDayOfWeek.setTextColor(Color.WHITE)
            holder.itemView.setBackgroundColor(Color.BLUE) // Highlight color
        } else {
            holder.tvDayOfWeek.setTextColor(Color.BLACK)
            holder.itemView.setBackgroundColor(Color.WHITE) // Default color
        }

        // Handle day click
        holder.itemView.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = currentWeekStart
            calendar.add(Calendar.DAY_OF_YEAR, position)
            val date = calendar.time
            onDayOfWeekClicked(date)
            selectedDay = position
            notifyDataSetChanged() // Refresh the adapter to update highlights
        }
    }

    override fun getItemCount(): Int {
        return daysOfWeek.size
    }

    fun setSelectedDay(day: Int) {
        selectedDay = day
        notifyDataSetChanged()
    }

    fun setWeeklyView(weeklyView: Boolean) {
        isWeeklyView = weeklyView
        notifyDataSetChanged()
    }
}