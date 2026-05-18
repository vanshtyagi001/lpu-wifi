package com.example.lpuwifi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {
    @Query("SELECT * FROM wifi_networks ORDER BY priority DESC")
    fun getAllWifiNetworks(): Flow<List<WifiNetwork>>

    // Added explicit return types (Long & Int) to fix the KSP crash
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifi(wifiNetwork: WifiNetwork): Long

    @Delete
    suspend fun deleteWifi(wifiNetwork: WifiNetwork): Int

    @Update
    suspend fun updateWifi(wifiNetwork: WifiNetwork): Int
}