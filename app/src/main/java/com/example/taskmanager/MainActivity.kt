package com.example.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmanager.databinding.ActivityMainBinding
import java.io.File
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
    private val todosByDate = mutableMapOf<Int, MutableList<Todo>>()
    private var selectedDayOfWeek: Int = 0
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var isWeeklyView = true
    private var currentWeekStart: Date = getStartOfWeek(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize days of the week
        val daysOfWeek = getDaysOfWeek()
        selectedDayOfWeek = getSelectedDayOfWeek(Date())

        // Initialize the adapter for days of the week
        dayOfWeekAdapter = DayOfWeekAdapter(
            daysOfWeek,
            { date -> onDayOfWeekClicked(date) },
            selectedDayOfWeek,
            true,
            currentWeekStart
        )

        binding.rvDaysOfWeek.apply {
            adapter = dayOfWeekAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        // Load todos and initialize the todo adapter
        loadTodos()
        todoAdapter = TodoAdapter(getTodosForDay(selectedDayOfWeek).toMutableList(), this)

        binding.rvTodoItems.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        // Update the todo list for the selected date
        updateTodoListForSelectedDate()

        // Add todo button click listener
        binding.btnAddTodo.setOnClickListener {
            val todoTitle = binding.etTodoTitle.text.toString()
            if (todoTitle.isNotEmpty()) {
                val todo = Todo(todoTitle, date = getDateForDayOfWeek(selectedDayOfWeek)) // Set date to today
                Log.d("MainActivity", "Adding todo: ${todo.title} for date: ${todo.date}")
                addTodo(todo)
                binding.etTodoTitle.text.clear()
            }
        }

        // Delete done todos button click listener
        binding.btnDeleteDoneTodos.setOnClickListener {
            deleteDoneTodos()
        }

        // Calendar button click listener
        binding.tvCalendar.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun getDaysOfWeek(): List<String> {
        val daysOfWeek = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.time = currentWeekStart

        val dateFormat = SimpleDateFormat("EEE dd/MM", Locale.getDefault()) // Format: "Mon 23/10"

        for (i in 0 until 7) { // Loop through 7 days of the week
            daysOfWeek.add(dateFormat.format(calendar.time)) // Add formatted date to the list
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Move to the next day
        }

        return daysOfWeek
    }

    private fun onDayOfWeekClicked(date: Date) {
        selectedDayOfWeek = getSelectedDayOfWeek(date)
        Log.d("MainActivity", "Selected Day: $selectedDayOfWeek") // Debug log
        dayOfWeekAdapter.setSelectedDay(selectedDayOfWeek)
        updateTodoListForSelectedDate()
    }

    private fun updateTodoListForSelectedDate() {
        Log.d("MainActivity", "Updating todos for day: $selectedDayOfWeek")

        // Create a new list to avoid clearing the original list
        val todos = getTodosForDay(selectedDayOfWeek).toMutableList()
        Log.d("updater1addinsider0", "Todos for day $selectedDayOfWeek: ${todos.size}")

        // Clear the adapter's list
        todoAdapter.todos.clear()
        Log.d("updater1insider1", "Todos for day $selectedDayOfWeek: ${todos.size}")

        // Add the new list to the adapter
        todoAdapter.todos.addAll(todos)
        Log.d("updater1insider2", "Todos for day $selectedDayOfWeek: ${todos.size}")

        // Notify the adapter of data changes
        todoAdapter.notifyDataSetChanged()
    }

    private fun getSelectedDayOfWeek(date: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.firstDayOfWeek = Calendar.MONDAY // Monday is the first day of the week
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek
        return if (dayOfWeek < 0) 6 else dayOfWeek // Handle Sunday (value -1)
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

    private fun getTodosForDay(dayOfWeek: Int): MutableList<Todo> {
        return todosByDate[dayOfWeek]?.toMutableList() ?: mutableListOf()
    }

    private fun getDateForDayOfWeek(dayOfWeek: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = currentWeekStart
        calendar.add(Calendar.DAY_OF_YEAR, dayOfWeek)
        return calendar.time
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                val selectedDate = selectedCalendar.time
                selectedDayOfWeek = getSelectedDayOfWeek(selectedDate)
                isWeeklyView = false
                updateTodoListForSelectedDate()
                dayOfWeekAdapter.setWeeklyView(false)
                dayOfWeekAdapter.setSelectedDay(selectedDayOfWeek)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    fun addTodo(todo: Todo) {
        val dayOfWeek = getSelectedDayOfWeek(todo.date ?: Date())
        Log.d("MainActivity", "Adding todo for day: $dayOfWeek (Date: ${todo.date})")

        // Create a new list to avoid modifying the original list
        val existingTodos = todosByDate.getOrPut(dayOfWeek) { mutableListOf() }.toMutableList()

        // Check for duplicates and remove the older one
        val duplicateTodo = existingTodos.find { it.title == todo.title }
        if (duplicateTodo != null) {
            existingTodos.remove(duplicateTodo)
        }

        existingTodos.add(todo)

        // Update the map with the new list
        todosByDate[dayOfWeek] = existingTodos

        // Handle repeat logic (if needed)
        if (todo.day || todo.week || todo.month) {
            // Add repeat logic here
        }

        updateTodoListForSelectedDate()
        saveTodos()
    }

    public fun saveTodos() {
        try {
            val file = File(filesDir, todoFile)
            val fileOutputStream = FileOutputStream(file)
            val objectOutputStream = ObjectOutputStream(fileOutputStream)
            objectOutputStream.writeObject(todosByDate)
            objectOutputStream.close()
            fileOutputStream.close()
            Log.d("MainActivity", "Todos saved successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving todos", e)
        }
    }

    private fun loadTodos() {
        try {
            val file = File(filesDir, todoFile)
            if (file.exists()) {
                val fileInputStream = FileInputStream(file)
                val objectInputStream = ObjectInputStream(fileInputStream)
                val loadedTodos = objectInputStream.readObject() as? MutableMap<Int, MutableList<Todo>>
                if (loadedTodos != null) {
                    todosByDate.clear()
                    todosByDate.putAll(loadedTodos)
                    Log.d("MainActivity", "Todos loaded successfully: ${todosByDate.size} days")
                }
                objectInputStream.close()
                fileInputStream.close()
            } else {
                Log.d("MainActivity", "No todos file found")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading todos", e)
        }
    }

    private fun deleteDoneTodos() {
        val todosToRemove = mutableListOf<Todo>()
        for ((_, todos) in todosByDate) {
            todosToRemove.addAll(todos.filter { it.isChecked })
        }
        for (todo in todosToRemove) {
            if (todo.day) {
                for (i in 0..6) {
                    todosByDate[i]?.removeAll { it.title == todo.title }
                }
            } else if (todo.week) {
                for (i in 0..6) {
                    if (getSelectedDayOfWeek(todo.date!!) == getSelectedDayOfWeek(getDateForDayOfWeek(i))) {
                        todosByDate[i]?.removeAll { it.title == todo.title }
                    }
                }
            } else {
                for ((_, todos) in todosByDate) {
                    todos.remove(todo)
                }
            }
        }
        updateTodoListForSelectedDate()
        saveTodos()
    }
}