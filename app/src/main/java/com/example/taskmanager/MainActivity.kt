package com.example.taskmanager

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmanager.databinding.ActivityMainBinding
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var todoAdapter: TodoAdapter
    private lateinit var dayOfWeekAdapter: DayOfWeekAdapter
    private val todoFile = "todos.dat"
    private val todosByDate = mutableMapOf<String, MutableList<Todo>>()
    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var isWeeklyView = true
    private var currentWeekStart: Date = getStartOfWeek(Date())
    private val CHANNEL_ID = "todo_reminder_channel"
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val EXACT_ALARM_PERMISSION_REQUEST_CODE = 1002

    // Filter states
    private var isShowingImportant = false
    private var isShowingUrgent = false
    private val originalTodoList = mutableListOf<Todo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkNotificationPermission()
        checkExactAlarmPermission()
        createNotificationChannel()

        // Initialize days of the week
        val daysOfWeek = getDaysOfWeek()
        selectedDate = getStartOfDay(Date())

        dayOfWeekAdapter = DayOfWeekAdapter(
            daysOfWeek,
            { date -> onDayOfWeekClicked(date) },
            getSelectedDayOfWeek(selectedDate),
            true,
            currentWeekStart
        )

        binding.rvDaysOfWeek.apply {
            adapter = dayOfWeekAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        loadTodos()

        todoAdapter = TodoAdapter(
            getTodosForDate(selectedDate).toMutableList(),
            this,
            { updatedTodo -> handleTodoUpdate(updatedTodo) },
            { position -> handleTodoDelete(position) }
        )

        binding.rvTodoItems.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        updateTodoListForSelectedDate()

        // Set up button click listeners
        binding.btnAddTodo.setOnClickListener {
            addNewTodo()
        }

        binding.btnDeleteDoneTodos.setOnClickListener {
            deleteDoneTodos()
        }

        binding.btnDeleteRepeated.setOnClickListener {
            deleteRepeatedTodos()
        }

        binding.btnCalendar.setOnClickListener {
            showDatePickerDialog()
        }

        // Set up menu button click listener
        binding.btnMenu.setOnClickListener { view ->
            showFilterMenu(view)
        }

        rescheduleAllNotifications()
    }

    private fun addNewTodo() {
        val todoTitle = binding.etTodoTitle.text.toString()
        if (todoTitle.isNotEmpty()) {
            val currentDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1) // Subtract one day from current date
            }.time

            if (selectedDate.before(currentDate)) {
                Toast.makeText(this, "Cannot add a task to a past date", Toast.LENGTH_SHORT).show()
                return
            }

            val todo = Todo(todoTitle, date = selectedDate)
            addTodo(todo)
            binding.etTodoTitle.text.clear()
        } else {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showFilterMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.filter_menu, popup.menu)

        popup.menu.findItem(R.id.menu_important)?.isChecked = isShowingImportant
        popup.menu.findItem(R.id.menu_urgent)?.isChecked = isShowingUrgent

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_important -> {
                    isShowingImportant = !isShowingImportant
                    if (isShowingImportant) isShowingUrgent = false
                    applyFilters()
                    true
                }
                R.id.menu_urgent -> {
                    isShowingUrgent = !isShowingUrgent
                    if (isShowingUrgent) isShowingImportant = false
                    applyFilters()
                    true
                }
                R.id.menu_clear -> {
                    isShowingImportant = false
                    isShowingUrgent = false
                    applyFilters()
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun applyFilters() {
        val filteredList = when {
            isShowingImportant -> originalTodoList.filter { it.isImportant }
            isShowingUrgent -> originalTodoList.filter { it.isUrgent }
            else -> originalTodoList.toList()
        }.sortedBy { it.date }

        todoAdapter.updateTodos(filteredList)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    showPermissionExplanationDialog()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                try {
                    startActivityForResult(intent, EXACT_ALARM_PERMISSION_REQUEST_CODE)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Please enable exact alarms in settings",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to remind you about your tasks.")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Todo Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for todo task reminders"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleTodoNotification(todo: Todo) {
        try {
            if (todo.hasReminder()) {
                val reminderDateTime = "${todo.reminderTimeDate} ${todo.reminderTimeTime}"
                val time = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .parse(reminderDateTime)?.time ?: 0L

                if (time > System.currentTimeMillis()) {
                    scheduleNotification(todo, time, true)
                } else {
                    Log.d("Notification", "Reminder time is in the past, not scheduling")
                }
            } else if (todo.hasDeadline()) {
                val deadlineDateTime = "${todo.deadlineDate} ${todo.deadlineTime}"
                val time = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .parse(deadlineDateTime)?.time ?: 0L

                if (time > System.currentTimeMillis()) {
                    scheduleNotification(todo, time, false)
                } else {
                    Log.d("Notification", "Deadline time is in the past, not scheduling")
                }
            } else if (!todo.from.isNullOrEmpty()) {
                val todoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(todo.date ?: Date())
                val fromDateTime = "$todoDate ${todo.from}"
                val calendar = Calendar.getInstance().apply {
                    time = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .parse(fromDateTime) ?: Date()
                    add(Calendar.MINUTE, -5)
                }
                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    scheduleNotification(todo, calendar.timeInMillis, false)
                } else {
                    Log.d("Notification", "Start time is in the past, not scheduling")
                }
            }
        } catch (e: Exception) {
            Log.e("Notification", "Error scheduling notification for task: ${todo.title}", e)
            Toast.makeText(this, "Error scheduling notification", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleNotification(todo: Todo, triggerTime: Long, isReminder: Boolean) {
        val notificationId = todo.hashCode()
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("notification_id", notificationId)
            putExtra("title", todo.title)
            putExtra("content", buildNotificationContent(todo, isReminder))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    private fun buildNotificationContent(todo: Todo, isReminder: Boolean): String {
        return when {
            isReminder -> "Reminder: ${todo.title}"
            todo.hasDeadline() -> "Deadline: ${todo.deadlineDate} ${todo.deadlineTime}"
            !todo.from.isNullOrEmpty() -> "Starting soon at ${todo.from}"
            else -> "Task reminder"
        }
    }

    private fun rescheduleAllNotifications() {
        todosByDate.values.flatten().forEach { todo ->
            scheduleTodoNotification(todo)
        }
    }

    public fun addTodo(todo: Todo) {
        val dateString = dateFormat.format(todo.date ?: Date())
        val existingTodos = todosByDate.getOrPut(dateString) { mutableListOf() }

        val duplicateTodo = existingTodos.find { it.title == todo.title }
        if (duplicateTodo != null) {
            existingTodos.remove(duplicateTodo)
            originalTodoList.remove(duplicateTodo)
        }

        existingTodos.add(todo)
        todosByDate[dateString] = existingTodos
        originalTodoList.add(todo)

        scheduleTodoNotification(todo)
        updateTodoListForSelectedDate()
        saveTodos()
    }

    private fun deleteDoneTodos() {
        try {
            val currentDateKey = dateFormat.format(selectedDate)
            val currentDateTodos = todosByDate[currentDateKey] ?: mutableListOf()
            val todosToRemove = currentDateTodos.filter { it.isChecked }.toList()

            currentDateTodos.removeAll(todosToRemove)
            originalTodoList.removeAll(todosToRemove)

            if (currentDateTodos.isEmpty()) {
                todosByDate.remove(currentDateKey)
            } else {
                todosByDate[currentDateKey] = currentDateTodos
            }

            updateTodoListForSelectedDate()
            saveTodos()

            Toast.makeText(
                this,
                "Deleted ${todosToRemove.size} completed tasks",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e("DeleteDoneTodos", "Error deleting todos", e)
            Toast.makeText(this, "Error deleting tasks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRepeatedTodos() {
        try {
            val currentDate = selectedDate
            val calendar = Calendar.getInstance().apply { time = currentDate }
            val currentDateKey = dateFormat.format(currentDate)
            val currentTodos = todosByDate[currentDateKey] ?: mutableListOf()
            val checkedTodos = currentTodos.filter { it.isChecked }

            if (checkedTodos.isEmpty()) {
                Toast.makeText(this, "No checked tasks to delete", Toast.LENGTH_SHORT).show()
                return
            }

            var totalDeleted = 0

            for (dayOffset in 0..100) {
                calendar.time = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                val futureDateKey = dateFormat.format(calendar.time)

                todosByDate[futureDateKey]?.let { todos ->
                    val beforeCount = todos.size
                    checkedTodos.forEach { checkedTodo ->
                        todos.removeAll { it.title == checkedTodo.title }
                    }
                    totalDeleted += (beforeCount - todos.size)

                    if (todos.isEmpty()) {
                        todosByDate.remove(futureDateKey)
                    }
                }
            }

            originalTodoList.removeAll(checkedTodos)
            updateTodoListForSelectedDate()
            saveTodos()

            Toast.makeText(
                this,
                "Deleted $totalDeleted tasks across 100 days",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e("DeleteChecked", "Error deleting todos", e)
            Toast.makeText(this, "Failed to delete tasks", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDaysOfWeek(): List<String> {
        val daysOfWeek = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.time = currentWeekStart

        val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        for (i in 0 until 7) {
            daysOfWeek.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return daysOfWeek
    }

    private fun onDayOfWeekClicked(date: Date) {
        selectedDate = getStartOfDay(date)
        dayOfWeekAdapter.setSelectedDay(getSelectedDayOfWeek(selectedDate))
        updateTodoListForSelectedDate()
    }

    private fun updateTodoListForSelectedDate() {
        val todos = getTodosForDate(selectedDate)
        todoAdapter.updateTodos(todos)
    }

    private fun getSelectedDayOfWeek(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.firstDayOfWeek = Calendar.MONDAY
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek
        return if (dayOfWeek < 0) 6 else dayOfWeek
    }

    private fun getStartOfWeek(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getTodosForDate(date: Date): List<Todo> {
        val dateString = dateFormat.format(date)
        return todosByDate[dateString]?.toList() ?: emptyList()
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Calculate two days ago for minDate and comparison
        val twoDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                val selectedDate = getStartOfDay(selectedCalendar.time)

                // Compare with two days ago (allowing two days ago, yesterday and today)
                if (selectedDate.before(getStartOfDay(twoDaysAgo.time))) {
                    Toast.makeText(this, "Cannot select a date before two days ago", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                this.selectedDate = selectedDate
                isWeeklyView = false
                updateTodoListForSelectedDate()
                dayOfWeekAdapter.setWeeklyView(false)
                dayOfWeekAdapter.setSelectedDay(getSelectedDayOfWeek(selectedDate))
            },
            year,
            month,
            day
        )

        // Set minimum date to two days ago
        datePickerDialog.datePicker.minDate = twoDaysAgo.timeInMillis
        datePickerDialog.show()
    }
    public fun saveTodos() {
        try {
            val fileOutputStream: FileOutputStream = openFileOutput(todoFile, Context.MODE_PRIVATE)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(todosByDate)
            objectOutputStream.close()
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadTodos() {
        try {
            val fileInputStream: FileInputStream = openFileInput(todoFile)
            val objectInputStream = ObjectInputStream(fileInputStream)
            val loadedTodos =
                objectInputStream.readObject() as? MutableMap<String, MutableList<Todo>>
            if (loadedTodos != null) {
                todosByDate.clear()
                todosByDate.putAll(loadedTodos)
                originalTodoList.clear()
                originalTodoList.addAll(todosByDate.values.flatten())
            }
            objectInputStream.close()
            fileInputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public fun dayRepeater(originalTodo: Todo) {
        val originalTitle = originalTodo.title
        todosByDate.values.flatten()
            .filter { it.title == originalTitle && it.day }
            .forEach { cancelTodoNotification(it) }

        val calendar = Calendar.getInstance()
        calendar.time = originalTodo.date ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        for (i in 0 until 90) {
            val newTodo = originalTodo.copy(
                date = calendar.time,
                day = true,
                week = false,
                month = false,
                isChecked = false
            )

            val dateKey = dateFormat.format(calendar.time)
            val existingTodos = todosByDate.getOrPut(dateKey) { mutableListOf() }

            if (!existingTodos.any { it.title == newTodo.title }) {
                existingTodos.add(newTodo)
                originalTodoList.add(newTodo)
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        updateTodoListForSelectedDate()
        saveTodos()
    }

    public fun weekRepeater(originalTodo: Todo) {
        val originalTitle = originalTodo.title
        todosByDate.values.flatten()
            .filter { it.title == originalTitle && it.week }
            .forEach { cancelTodoNotification(it) }

        val calendar = Calendar.getInstance()
        calendar.time = originalTodo.date ?: Date()

        val originalDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        calendar.add(Calendar.DAY_OF_YEAR, 7)

        for (i in 0 until 12) {
            val newTodo = originalTodo.copy(
                date = calendar.time,
                day = false,
                week = true,
                month = false,
                isChecked = false
            )

            val dateKey = dateFormat.format(calendar.time)
            val existingTodos = todosByDate.getOrPut(dateKey) { mutableListOf() }

            if (!existingTodos.any { it.title == newTodo.title && it.week }) {
                existingTodos.add(newTodo)
                originalTodoList.add(newTodo)
            }

            calendar.add(Calendar.DAY_OF_YEAR, 7)
        }

        updateTodoListForSelectedDate()
        saveTodos()
    }

    fun cancelTodoNotification(todo: Todo) {
        val notificationId = todo.hashCode()
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun handleTodoUpdate(updatedTodo: Todo) {
        cancelTodoNotification(updatedTodo)
        scheduleTodoNotification(updatedTodo)
        saveTodos()
    }

    private fun handleTodoDelete(position: Int) {
        val dateString = dateFormat.format(selectedDate)
        todosByDate[dateString]?.let { dateTodos ->
            if (position in 0 until dateTodos.size) {
                val deletedTodo = dateTodos[position]
                cancelTodoNotification(deletedTodo)

                dateTodos.removeAt(position)
                originalTodoList.removeAll {
                    it.title == deletedTodo.title && dateFormat.format(it.date) == dateString
                }

                if (dateTodos.isEmpty()) {
                    todosByDate.remove(dateString)
                }

                saveTodos()
                updateTodoListForSelectedDate()
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("notification_id", 0)
        val title = intent.getStringExtra("title") ?: "Task Reminder"
        val content = intent.getStringExtra("content") ?: "You have a task to complete"

        val builder = NotificationCompat.Builder(context, "todo_reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}