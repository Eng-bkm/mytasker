package com.example.taskmanager

import java.io.Serializable
import java.util.Date

data class Todo(
    val title: String,
    var isChecked: Boolean = false,
    var from: String? = null,
    var to: String? = null,
    var deadlineDate: String? = null,
    var deadlineTime: String? = null,
    var day: Boolean = false,
    var week: Boolean = false,
    var month: Boolean = false,
    var isImportant: Boolean = false,
    var isUrgent: Boolean = false,
    val reminders: MutableList<String> = mutableListOf(),
    var date: Date? = null
) : Serializable