package com.example.taskmanager

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Todo(
    var title: String,
    var isChecked: Boolean = false,
    var from: String? = null,
    var to: String? = null,
    var isImportant: Boolean = false,
    var isUrgent: Boolean = false,
    var deadlineDate: String? = null,
    val id: String = UUID.randomUUID().toString(),
    var deadlineTime: String? = null,
    var reminders: MutableList<String> = mutableListOf(),
    var day: Boolean = false,
    var week: Boolean = false,
    var month: Boolean = false,
    var date: Date? = null,
    var reminderTimeDate: String? = null,
    var reminderTimeTime: String? = null
) : Serializable {

    fun hasReminder(): Boolean {
        return !reminderTimeDate.isNullOrEmpty() && !reminderTimeTime.isNullOrEmpty()
    }

    fun hasDeadline(): Boolean {
        return !deadlineDate.isNullOrEmpty() && !deadlineTime.isNullOrEmpty()
    }

    fun hasTimeRange(): Boolean {
        return !from.isNullOrEmpty() && !to.isNullOrEmpty()
    }

    fun getReminderTimeString(): String {
        return if (hasReminder()) {
            "$reminderTimeDate $reminderTimeTime"
        } else if (hasDeadline()) {
            "$deadlineDate $deadlineTime"
        } else if (!from.isNullOrEmpty()) {
            "${dateFormat.format(date ?: Date())} $from"
        } else {
            "No reminder set"
        }
    }

    // Deep copy method
    fun deepCopy(): Todo {
        return Todo(
            title = this.title,
            isChecked = this.isChecked,
            from = this.from,
            to = this.to,
            isImportant = this.isImportant,
            isUrgent = this.isUrgent,
            deadlineDate = this.deadlineDate,
            deadlineTime = this.deadlineTime,
            reminders = this.reminders.toMutableList(),
            day = this.day,
            week = this.week,
            month = this.month,
            date = this.date?.let { Date(it.time) },
            reminderTimeDate = this.reminderTimeDate,
            reminderTimeTime = this.reminderTimeTime
        )
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    }
}