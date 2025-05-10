package com.example.taskmanager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.databinding.TodoItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoAdapter(val todos: MutableList<Todo>, private val context: Context) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private var expandedPosition = -1
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class TodoViewHolder(val binding: TodoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(todo: Todo) {
            binding.tvTodoTitle.text = todo.title
            binding.tvFrom.text = todo.from ?: "None"
            binding.tvTo.text = todo.to ?: "None"
            binding.cbDone.isChecked = todo.isChecked
            binding.tvDeadlineDate.text = todo.deadlineDate ?: "None"
            binding.tvDeadlineTime.text = todo.deadlineTime ?: "None"
            binding.tvDay.isSelected = todo.day
            binding.tvWeek.isSelected = todo.week
            binding.tvMonth.isSelected = todo.month
            binding.ivImportant.visibility = if (todo.isImportant) View.VISIBLE else View.GONE
            binding.ivUrgent.visibility = if (todo.isUrgent) View.VISIBLE else View.GONE
            binding.btnAddReminder.setOnClickListener {
                addReminder(todo)
            }
            binding.tvDay.setOnClickListener {
                setRepeat(todo, "Day")
            }
            binding.tvWeek.setOnClickListener {
                setRepeat(todo, "Week")
            }
            binding.tvMonth.setOnClickListener {
                setRepeat(todo, "Month")
            }
            binding.cbDone.setOnCheckedChangeListener { _, isChecked ->
                todo.isChecked = isChecked
                saveTodos()
            }
            binding.tvFrom.setOnClickListener {
                showTimePickerDialog(todo, true)
            }
            binding.tvTo.setOnClickListener {
                showTimePickerDialog(todo, false)
            }
            binding.tvDeadlineDate.setOnClickListener {
                showDatePickerDialog(todo)
            }
            binding.tvDeadlineTime.setOnClickListener {
                showTimePickerDialog(todo, null)
            }
            binding.ivImportant.setOnClickListener {
                todo.isImportant = !todo.isImportant
                binding.ivImportant.visibility = if (todo.isImportant) View.VISIBLE else View.GONE
                saveTodos()
            }
            binding.ivUrgent.setOnClickListener {
                todo.isUrgent = !todo.isUrgent
                binding.ivUrgent.visibility = if (todo.isUrgent) View.VISIBLE else View.GONE
                saveTodos()
            }
            binding.llReminders.removeAllViews()
            for (reminder in todo.reminders) {
                addReminderView(reminder, todo, binding) // Pass the binding here
            }
        }
    }

    private fun addReminder(todo: Todo) {
        val newReminder = "None"
        todo.reminders.add(newReminder)
        //addReminderView(newReminder, todo) // Remove this line
        notifyDataSetChanged()
        saveTodos()
    }

    // Add the binding parameter here
    private fun addReminderView(reminder: String, todo: Todo, binding: TodoItemBinding) {
        val reminderView = LayoutInflater.from(context).inflate(R.layout.reminder_item, null)
        val tvReminder = reminderView.findViewById<TextView>(R.id.tvReminder)
        tvReminder.text = reminder
        tvReminder.setOnClickListener {
            showDateTimePickerDialog(todo, tvReminder)
        }
        binding.llReminders.addView(reminderView) // Now you can use binding here
    }

    private fun setRepeat(todo: Todo, repeatType: String) {
        when (repeatType) {
            "Day" -> {
                todo.day = !todo.day
                todo.week = false
                todo.month = false
            }
            "Week" -> {
                todo.week = !todo.week
                todo.day = false
                todo.month = false
            }
            "Month" -> {
                todo.month = !todo.month
                todo.day = false
                todo.week = false
            }
        }
        notifyDataSetChanged()
        saveTodos()
    }


    private fun showTimePickerDialog(todo: Todo, isFrom: Boolean?) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                if (isFrom == true) {
                    todo.from = formattedTime
                    //binding.tvFrom.text = formattedTime // Remove this line
                    notifyDataSetChanged()
                } else if (isFrom == false) {
                    todo.to = formattedTime
                    //binding.tvTo.text = formattedTime // Remove this line
                    notifyDataSetChanged()
                } else {
                    todo.deadlineTime = formattedTime
                    //binding.tvDeadlineTime.text = formattedTime // Remove this line
                    notifyDataSetChanged()
                }
                saveTodos()
            },
            hour,
            minute,
            true
        )
        timePickerDialog.show()
    }

    private fun showDatePickerDialog(todo: Todo) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = selectedCalendar.time
                todo.deadlineDate = dateFormat.format(selectedDate)
                //binding.tvDeadlineDate.text = todo.deadlineDate // Remove this line
                notifyDataSetChanged()
                saveTodos()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showDateTimePickerDialog(todo: Todo, tvReminder: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val timePickerDialog = TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        val selectedCalendar = Calendar.getInstance()
                        selectedCalendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                        val selectedDateTime = selectedCalendar.time
                        val formattedDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(selectedDateTime)
                        tvReminder.text = formattedDateTime
                        todo.reminders[todo.reminders.indexOf(tvReminder.text)] = formattedDateTime
                        saveTodos()},
                    hour,
                    minute,
                    true
                )
                timePickerDialog.show()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    public fun saveTodos() {
        (context as MainActivity).saveTodos()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = TodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = todos[position]
        holder.bind(todo)
        val isExpanded = position == expandedPosition
        holder.binding.llTodoDetails.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int {
        return todos.size
    }
}