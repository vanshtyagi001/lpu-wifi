package com.example.lpuwifi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY hour ASC, minute ASC")
    fun getAllSchedules(): Flow<List<ConnectionSchedule>>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1")
    suspend fun getEnabledSchedulesSync(): List<ConnectionSchedule>

    // Added explicit return types (Long & Int) to fix the KSP crash
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ConnectionSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: ConnectionSchedule): Int

    @Delete
    suspend fun deleteSchedule(schedule: ConnectionSchedule): Int
}