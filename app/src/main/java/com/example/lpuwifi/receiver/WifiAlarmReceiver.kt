package com.example.lpuwifi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.lpuwifi.service.WifiConnectionService

class WifiAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("WifiAlarmReceiver", "Alarm triggered! Grabbing WakeLock...")

        // Force the CPU to stay awake for up to 3 minutes to guarantee the login finishes
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LPUWifi::AlarmWakeLock")
        wakeLock.acquire(3 * 60 * 1000L)

        val scheduleId = intent.getIntExtra("SCHEDULE_ID", -1)
        val hour = intent.getIntExtra("SCHEDULE_HOUR", -1)
        val minute = intent.getIntExtra("SCHEDULE_MINUTE", -1)

        val serviceIntent = Intent(context, WifiConnectionService::class.java).apply {
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("SCHEDULE_HOUR", hour)
            putExtra("SCHEDULE_MINUTE", minute)
        }

        ContextCompat.startForegroundService(context, serviceIntent)

        // We release the WakeLock immediately because starting a Foreground Service
        // automatically hands the CPU priority over to the Service!
        wakeLock.release()
    }
}