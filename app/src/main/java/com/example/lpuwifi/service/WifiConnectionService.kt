package com.example.lpuwifi.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lpuwifi.data.AppDatabase
import com.example.lpuwifi.data.ConnectionSchedule
import com.example.lpuwifi.data.CredentialsManager
import com.example.lpuwifi.network.CaptivePortalAuthenticator
import com.example.lpuwifi.network.WifiController
import com.example.lpuwifi.scheduling.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WifiConnectionService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val NOTIFICATION_CHANNEL_ID = "wifi_connection_channel"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification("Initializing connection process...")

        // Reschedule alarm for the next day automatically
        val scheduleId = intent?.getIntExtra("SCHEDULE_ID", -1) ?: -1
        val hour = intent?.getIntExtra("SCHEDULE_HOUR", -1) ?: -1
        val minute = intent?.getIntExtra("SCHEDULE_MINUTE", -1) ?: -1

        if (scheduleId != -1) {
            val scheduler = AlarmScheduler(this)
            scheduler.schedule(ConnectionSchedule(id = scheduleId, hour = hour, minute = minute))
        }

        // Begin the full automation process
        runAutomationProcess()

        return START_NOT_STICKY
    }

    private fun runAutomationProcess() {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val wifiController = WifiController(applicationContext)
                val authenticator = CaptivePortalAuthenticator()
                val credentialsManager = CredentialsManager(applicationContext)
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

                updateNotification("Preparing connection...")

                val savedNetworks = db.wifiDao().getAllWifiNetworks().firstOrNull() ?: emptyList()
                if (savedNetworks.isEmpty()) {
                    updateNotification("No saved Wi-Fi networks in app.")
                    stopSelf(); return@launch
                }

                // In a real scenario, you'd pick the best match. We will use the highest priority one.
                val bestMatch = savedNetworks.first()

                updateNotification("Waiting for Wi-Fi: ${bestMatch.ssid}...")

                // Get the Network object
                val network = wifiController.awaitWifiNetwork(bestMatch.ssid)

                if (network == null) {
                    updateNotification("Timeout: Could not find or connect to ${bestMatch.ssid}.")
                    stopSelf(); return@launch
                }

                // CRUCIAL: Bind the app's traffic to this specific Wi-Fi network to bypass Mobile Data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.bindProcessToNetwork(network)
                }

                updateNotification("Logging into Captive Portal...")

                val regNo = credentialsManager.getRegNo.firstOrNull()
                val password = credentialsManager.getPassword.firstOrNull()

                if (regNo.isNullOrBlank() || password.isNullOrBlank()) {
                    updateNotification("Error: LPU Credentials not set!")
                    stopSelf(); return@launch
                }

                val authSuccess = authenticator.automateLogin(regNo, password)

                if (authSuccess) {
                    updateNotification("Success! Connected & Logged In.")
                } else {
                    updateNotification("Connected, but Login Failed. Check credentials.")
                }

            } catch (e: Exception) {
                Log.e("WifiService", "Error in service: ${e.message}")
                updateNotification("An error occurred during connection.")
            } finally {
                stopSelf()
            }
        }
    }

    private fun startForegroundServiceNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Wi-Fi Automation Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LPU Auto Wi-Fi")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LPU Auto Wi-Fi")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        manager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}