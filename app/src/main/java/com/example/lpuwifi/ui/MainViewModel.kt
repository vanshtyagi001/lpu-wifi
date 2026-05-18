package com.example.lpuwifi.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lpuwifi.data.AppDatabase
import com.example.lpuwifi.data.ConnectionSchedule
import com.example.lpuwifi.data.CredentialsManager
import com.example.lpuwifi.data.WifiNetwork
import com.example.lpuwifi.network.WifiController
import com.example.lpuwifi.scheduling.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val credentialsManager = CredentialsManager(application)
    private val wifiController = WifiController(application)
    private val alarmScheduler = AlarmScheduler(application)

    // Expose Data to UI
    val wifiList: StateFlow<List<WifiNetwork>> = db.wifiDao().getAllWifiNetworks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val scheduleList: StateFlow<List<ConnectionSchedule>> = db.scheduleDao().getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val regNo = credentialsManager.getRegNo.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val password = credentialsManager.getPassword.stateIn(viewModelScope, SharingStarted.Lazily, "")

    // Credentials Logic
    fun saveCredentials(reg: String, pass: String) = viewModelScope.launch {
        credentialsManager.saveCredentials(reg, pass)
    }

    // Wi-Fi Logic
    fun addWifi(ssid: String, priority: Int = 0) = viewModelScope.launch {
        db.wifiDao().insertWifi(WifiNetwork(ssid = ssid, priority = priority))
    }

    fun deleteWifi(wifi: WifiNetwork) = viewModelScope.launch {
        db.wifiDao().deleteWifi(wifi)
    }

    suspend fun scanNearbyWifi(): List<String> = withContext(Dispatchers.IO) {
        wifiController.getNearbyWifiNetworks()
    }

    // Scheduling Logic
    fun addSchedule(hour: Int, minute: Int) = viewModelScope.launch {
        val schedule = ConnectionSchedule(hour = hour, minute = minute)
        // Insert into DB and get the generated ID
        val id = db.scheduleDao().insertSchedule(schedule).toInt()
        val newSchedule = schedule.copy(id = id)

        // Schedule the alarm in Android System
        alarmScheduler.schedule(newSchedule)
    }

    fun toggleSchedule(schedule: ConnectionSchedule, isEnabled: Boolean) = viewModelScope.launch {
        val updated = schedule.copy(isEnabled = isEnabled)
        db.scheduleDao().updateSchedule(updated)

        if (isEnabled) {
            alarmScheduler.schedule(updated)
        } else {
            alarmScheduler.cancel(updated)
        }
    }

    fun deleteSchedule(schedule: ConnectionSchedule) = viewModelScope.launch {
        alarmScheduler.cancel(schedule)
        db.scheduleDao().deleteSchedule(schedule)
    }
}