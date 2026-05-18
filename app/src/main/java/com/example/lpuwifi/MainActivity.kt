package com.example.lpuwifi

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.lpuwifi.ui.MainViewModel
import com.example.lpuwifi.ui.screens.NetworksScreen
import com.example.lpuwifi.ui.screens.SchedulesScreen
import com.example.lpuwifi.ui.screens.SettingsScreen
import com.example.lpuwifi.ui.theme.LPUWiFITheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Request standard permissions (Location, Notifications)
        requestStandardPermissions()

        setContent {
            LPUWiFITheme {
                val navController = rememberNavController()
                val mainViewModel: MainViewModel = viewModel()
                val context = LocalContext.current

                // 2. Check and ask for Advanced Background Permissions
                BackgroundPermissionsDialog(context)

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                            NavigationBarItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Schedules") },
                                label = { Text("Schedules") },
                                selected = currentRoute == "schedules",
                                onClick = { navController.navigate("schedules") { launchSingleTop = true } }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Wifi, contentDescription = "Networks") },
                                label = { Text("Networks") },
                                selected = currentRoute == "networks",
                                onClick = { navController.navigate("networks") { launchSingleTop = true } }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                                selected = currentRoute == "settings",
                                onClick = { navController.navigate("settings") { launchSingleTop = true } }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "schedules",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("schedules") { SchedulesScreen(viewModel = mainViewModel) }
                        composable("networks") { NetworksScreen(viewModel = mainViewModel) }
                        composable("settings") { SettingsScreen(viewModel = mainViewModel) }
                    }
                }
            }
        }
    }

    private fun requestStandardPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun BackgroundPermissionsDialog(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // States to track which permissions are missing
    var needsBatteryBypass by remember { mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName)) }

    var needsExactAlarm by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
    }

    val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) || Build.BRAND.equals("POCO", ignoreCase = true)
    // We can't definitively check if Autostart is on, so we just show it for Xiaomi users once.
    var showXiaomiAutostart by remember { mutableStateOf(isXiaomi) }

    if (needsBatteryBypass || needsExactAlarm || showXiaomiAutostart) {
        AlertDialog(
            onDismissRequest = { /* Force them to interact */ },
            title = { Text("Background Setup Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To automatically connect at 6:00 AM while your phone is locked, please allow the following:")

                    if (needsExactAlarm) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(intent)
                            needsExactAlarm = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("1. Allow Exact Alarms")
                        }
                    }

                    if (needsBatteryBypass) {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            needsBatteryBypass = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("2. Remove Battery Restrictions")
                        }
                    }

                    if (showXiaomiAutostart) {
                        Button(onClick = {
                            try {
                                val intent = Intent()
                                intent.component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to normal settings if activity not found
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                            showXiaomiAutostart = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("3. Xiaomi: Turn ON AutoStart")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    needsBatteryBypass = false
                    needsExactAlarm = false
                    showXiaomiAutostart = false
                }) {
                    Text("I've done this ->")
                }
            }
        )
    }
}