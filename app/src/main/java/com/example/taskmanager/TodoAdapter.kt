package com.example.taskmanager

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanager.databinding.TodoItemBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoAdapter(
    private var todos: MutableList<Todo>,
    private val context: Context,
    private val onTodoUpdated: (Todo) -> Unit,
    private val onTodoDeleted: (Int) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    private var expandedPosition = -1
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    inner class TodoViewHolder(val binding: TodoItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentTodo: Todo? = null

        fun bind(todo: Todo) {
            currentTodo = todo
            updateUI(currentTodo!!)
            setupClickListeners()
            setupTextWatchers()



            binding.llTodoDetails.visibility = if (adapterPosition == expandedPosition) View.VISIBLE else View.GONE
            itemView.setOnClickListener {
                expandedPosition = if (adapterPosition == expandedPosition) -1 else adapterPosition
                notifyDataSetChanged()
            }
        }

        private fun updateUI(todo: Todo) {
            with(binding) {
                tvTodoTitle.text = todo.title
                cbDone.isChecked = todo.isChecked
                tvFrom.text = todo.from
                tvTo.text = todo.to

                // Handle deadline date display
                val mainDate = todo.date?.let { dateFormat.format(it) } ?: ""
                tvDeadlineDate.text = when {
                    !todo.deadlineDate.isNullOrEmpty() && todo.deadlineDate != mainDate ->
                        todo.deadlineDate.toEditable()
                    else -> "".toEditable()
                }

                tvDeadlineTime.text = todo.deadlineTime?.toEditable() ?: "".toEditable()
                tvReminderDate.text = todo.reminderTimeDate?.toEditable() ?: "".toEditable()
                tvReminderTime.text = todo.reminderTimeTime?.toEditable() ?: "".toEditable()
                tvDay.isSelected = todo.day
                tvWeek.isSelected = todo.week
                tvMonth.isSelected = todo.month
            }
        }

        // Extension function to convert String to Editable
        private fun String?.toEditable(): Editable =
            Editable.Factory.getInstance().newEditable(this ?: "")
        private fun updateImportanceUI(isImportant: Boolean) {
            binding.ivImportant.apply {
                setImageResource(if (isImportant) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                setColorFilter(ContextCompat.getColor(context,
                    if (isImportant) R.color.important_active else R.color.important_default))
            }
        }

        private fun updateUrgencyUI(isUrgent: Boolean) {
            binding.ivUrgent.apply {
                setImageResource(if (isUrgent) R.drawable.ic_warning_filled else R.drawable.ic_warning_outline)
                setColorFilter(ContextCompat.getColor(context,
                    if (isUrgent) R.color.urgent_active else R.color.urgent_default))
            }
        }

        private fun setupClickListeners() {
            with(binding) {
                ivImportant.setOnClickListener {
                    currentTodo?.let { todo ->
                        todo.isImportant = !todo.isImportant
                        updateImportanceUI(todo.isImportant)
                        onTodoUpdated(todo)
                    }
                }

                ivUrgent.setOnClickListener {
                    currentTodo?.let { todo ->
                        todo.isUrgent = !todo.isUrgent
                        updateUrgencyUI(todo.isUrgent)
                        onTodoUpdated(todo)
                    }
                }

                tvDay.setOnClickListener {
                    currentTodo?.let { todo ->
                        todo.day = !todo.day
                        if (todo.day) {
                            (context as MainActivity).dayRepeater(todo)
                        }
                        onTodoUpdated(todo)
                    }
                }

                tvWeek.setOnClickListener {
                    currentTodo?.let { todo ->
                        (context as MainActivity).weekRepeater(todo)
                    }
                }

                tvMonth.setOnClickListener {
                    currentTodo?.let { todo ->
                        showMonthRepeatDialog(todo)
                    }
                }

                cbDone.setOnCheckedChangeListener { _, isChecked ->
                    currentTodo?.let { todo ->
                        todo.isChecked = isChecked
                        onTodoUpdated(todo)
                    }
                }

                tvFrom.setOnClickListener {
                    currentTodo?.let { todo ->
                        showTimePicker(todo, true) { updatedTodo ->
                            currentTodo?.from = updatedTodo.from
                            updateUI(currentTodo!!)
                            onTodoUpdated(updatedTodo)
                        }
                    }
                }

                tvTo.setOnClickListener {
                    currentTodo?.let { todo ->
                        showTimePicker(todo, false) { updatedTodo ->
                            currentTodo?.to = updatedTodo.to
                            updateUI(currentTodo!!)
                            onTodoUpdated(updatedTodo)
                        }
                    }
                }

                tvDeadlineDate.setOnClickListener {
                    currentTodo?.let { todo ->
                        showDatePicker(todo, true) { updatedTodo ->
                            currentTodo?.deadlineDate = updatedTodo.deadlineDate
                            updateUI(currentTodo!!)
                            onTodoUpdated(updatedTodo)
                        }
                    }
                }

                tvDeadlineTime.setOnClickListener {
                    currentTodo?.let { todo ->
                        if (todo.deadlineDate.isNullOrEmpty()) {
                            todo.deadlineDate = dateFormat.format(todo.date ?: Date())
                        }
                        showTimePicker(todo, null) { updatedTodo ->
                            checkDateTimeValidity(updatedTodo.deadlineDate, updatedTodo.deadlineTime) { isValid ->
                                if (isValid) {
                                    currentTodo?.deadlineTime = updatedTodo.deadlineTime
                                    updateUI(currentTodo!!)
                                    onTodoUpdated(updatedTodo)
                                } else {
                                    Toast.makeText(context, "Cannot set past deadline", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                tvReminderDate.setOnClickListener {
                    currentTodo?.let { todo ->
                        showDatePicker(todo, false) { updatedTodo ->
                            currentTodo?.reminderTimeDate = updatedTodo.reminderTimeDate
                            updateUI(currentTodo!!)
                            onTodoUpdated(updatedTodo)
                        }
                    }
                }

                tvReminderTime.setOnClickListener {
                    currentTodo?.let { todo ->
                        if (todo.reminderTimeDate.isNullOrEmpty()) {
                            todo.reminderTimeDate = dateFormat.format(todo.date ?: Date())
                        }
                        showReminderTimePicker(todo) { updatedTodo ->
                            checkDateTimeValidity(updatedTodo.reminderTimeDate, updatedTodo.reminderTimeTime) { isValid ->
                                if (isValid) {
                                    currentTodo?.reminderTimeTime = updatedTodo.reminderTimeTime
                                    updateUI(currentTodo!!)
                                    onTodoUpdated(updatedTodo)
                                } else {
                                    Toast.makeText(context, "Cannot set past reminder", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun checkDateTimeValidity(dateStr: String?, timeStr: String?, callback: (Boolean) -> Unit) {
            if (dateStr == null || timeStr == null) {
                callback(false)
                return
            }
            try {
                val combinedFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateTime = combinedFormat.parse("$dateStr $timeStr")
                callback(dateTime != null && dateTime.after(Date()))
            } catch (e: Exception) {
                callback(false)
            }
        }

        private fun setupTextWatchers() {
            binding.tvTodoTitle.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    currentTodo?.let { todo ->
                        todo.title = s?.toString() ?: ""
                        onTodoUpdated(todo)
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        private fun showMonthRepeatDialog(todo: Todo) {
            AlertDialog.Builder(context)
                .setTitle("Monthly Repeat")
                .setMessage("Set this task to repeat monthly?")
                .setPositiveButton("Yes") { _, _ ->
                    val updatedTodo = todo.copy(month = true)
                    onTodoUpdated(updatedTodo)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun showTimePicker(
            todo: Todo,
            isFrom: Boolean?,
            callback: (Todo) -> Unit
        ) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                    val updatedTodo = todo.copy().apply {
                        when (isFrom) {
                            true -> from = formattedTime
                            false -> to = formattedTime
                            null -> deadlineTime = formattedTime
                        }
                    }
                    callback(updatedTodo)
                },
                hour, minute, true
            ).show()
        }

        private fun showReminderTimePicker(
            todo: Todo,
            callback: (Todo) -> Unit
        ) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    val updatedTodo = todo.copy().apply {
                        reminderTimeTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                        if (reminderTimeDate == null) {
                            reminderTimeDate = dateFormat.format(date ?: Date())
                        }
                    }
                    callback(updatedTodo)
                },
                hour, minute, true
            ).show()
        }

        private fun showDatePicker(
            todo: Todo,
            isDeadline: Boolean,
            callback: (Todo) -> Unit
        ) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                context,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (selectedDate.before(Calendar.getInstance())) {
                        Toast.makeText(context, "Cannot select a past date", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }
                    val formattedDate = String.format("%02d/%02d/%04d",
                        selectedDay, selectedMonth + 1, selectedYear)
                    val updatedTodo = todo.copy().apply {
                        if (isDeadline) {
                            deadlineDate = formattedDate
                        } else {
                            reminderTimeDate = formattedDate
                        }
                    }
                    callback(updatedTodo)
                },
                year, month, day
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = TodoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(todos[position])
    }

    override fun getItemCount(): Int = todos.size

    fun updateTodos(newTodos: List<Todo>) {
        todos.clear()
        todos.addAll(newTodos)
        notifyDataSetChanged()
    }

    fun removeTodo(position: Int) {
        if (position in 0 until todos.size) {
            todos.removeAt(position)
            notifyItemRemoved(position)
            onTodoDeleted(position)
        }
    }
}