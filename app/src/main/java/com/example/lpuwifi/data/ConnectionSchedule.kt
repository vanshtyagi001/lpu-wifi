package com.example.lpuwifi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ConnectionSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,          // 0 to 23 (24-hour format)
    val minute: Int,        // 0 to 59
    val isEnabled: Boolean = true // User can toggle schedule on/off
)