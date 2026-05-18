package com.example.lpuwifi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_networks")
data class WifiNetwork(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String,       // The Wi-Fi name (e.g., "LPU Hostels-5G")
    val priority: Int = 0   // Higher number means higher priority
)