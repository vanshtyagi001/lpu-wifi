package com.example.lpuwifi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lpuwifi.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworksScreen(viewModel: MainViewModel) {
    val wifiList by viewModel.wifiList.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Saved Wi-Fi Networks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Wi-Fi")
            }
        }
    ) { padding ->
        if (wifiList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No networks saved yet. Tap + to add.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(wifiList) { wifi ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(wifi.ssid, style = MaterialTheme.typography.titleMedium)
                                Text("Priority: ${wifi.priority}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteWifi(wifi) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Choosing Add Method
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Wi-Fi Network") },
            text = { Text("Do you want to type the name manually or scan nearby networks?") },
            confirmButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    showScanDialog = true
                }) { Text("Scan Nearby") }
            },
            dismissButton = {
                // For simplicity in this demo, if they dismiss, we just add default LPU
                // In a real app, you'd open a text field dialog here.
                TextButton(onClick = {
                    viewModel.addWifi("LPU Hostels-5G", 10)
                    viewModel.addWifi("LPU Hostels", 5)
                    showAddDialog = false
                }) { Text("Add Default LPU") }
            }
        )
    }

    // Dialog for Scanning Nearby Wi-Fi
    if (showScanDialog) {
        ScanWifiDialog(viewModel, onDismiss = { showScanDialog = false })
    }
}

@Composable
fun ScanWifiDialog(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var scannedNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scannedNetworks = viewModel.scanNearbyWifi()
        isScanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nearby Networks") },
        text = {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else if (scannedNetworks.isEmpty()) {
                Text("No networks found. Ensure Location and Wi-Fi are turned on.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(scannedNetworks) { ssid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addWifi(ssid = ssid, priority = 5) // Default priority
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text(ssid)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}