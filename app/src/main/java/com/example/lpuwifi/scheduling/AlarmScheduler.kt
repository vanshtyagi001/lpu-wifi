package com.example.lpuwifi.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.lpuwifi.data.ConnectionSchedule
import com.example.lpuwifi.receiver.WifiAlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(schedule: ConnectionSchedule) {
        val intent = Intent(context, WifiAlarmReceiver::class.java).apply {
            putExtra("SCHEDULE_ID", schedule.id)
            putExtra("SCHEDULE_HOUR", schedule.hour)
            putExtra("SCHEDULE_MINUTE", schedule.minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, schedule.hour)
            set(Calendar.MINUTE, schedule.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // UPGRADE: setAlarmClock forces the OS to wake up, bypassing Xiaomi/Doze restrictions.
        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d("AlarmScheduler", "Invincible Alarm set for ${calendar.time}")
    }

    fun cancel(schedule: ConnectionSchedule) {
        val intent = Intent(context, WifiAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm ID: ${schedule.id}")
    }
}