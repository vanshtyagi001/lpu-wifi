package com.example.lpuwifi.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WifiController(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun getNearbyWifiNetworks(): List<String> {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return try {
            wifiManager.scanResults.mapNotNull {
                @Suppress("DEPRECATION")
                it.SSID?.removeSurrounding("\"")
            }.filter { it.isNotBlank() }.distinct()
        } catch (e: Exception) { emptyList() }
    }

    /**
     * Finds the Wi-Fi network and returns the Network object so we can route traffic through it.
     */
    suspend fun awaitWifiNetwork(targetSsid: String): Network? = withContext(Dispatchers.IO) {
        // 1. Check if the phone is already connected to the Wi-Fi natively
        for (network in connectivityManager.allNetworks) {
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                @Suppress("DEPRECATION")
                val currentSsid = wifiManager.connectionInfo.ssid?.removeSurrounding("\"")
                if (currentSsid == targetSsid) {
                    Log.d("WifiController", "Already connected to $targetSsid natively.")
                    return@withContext network
                }
            }
        }

        Log.d("WifiController", "Not connected. Suggesting $targetSsid to OS and waiting...")

        // 2. Suggest the network to the OS to encourage auto-connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestion = WifiNetworkSuggestion.Builder().setSsid(targetSsid).build()
            wifiManager.addNetworkSuggestions(listOf(suggestion))
        }

        // 3. Wait up to 60 seconds for the OS to connect to the Wi-Fi
        return@withContext withTimeoutOrNull(60_000) {
            suspendCancellableCoroutine { continuation ->
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        @Suppress("DEPRECATION")
                        val ssid = wifiManager.connectionInfo.ssid?.removeSurrounding("\"")
                        if (ssid == targetSsid) {
                            if (continuation.isActive) {
                                Log.d("WifiController", "OS connected to $targetSsid. Capturing network...")
                                continuation.resume(network)
                                try { connectivityManager.unregisterNetworkCallback(this) } catch (e: Exception) {}
                            }
                        }
                    }
                }

                connectivityManager.requestNetwork(request, callback)

                continuation.invokeOnCancellation {
                    try { connectivityManager.unregisterNetworkCallback(callback) } catch (e: Exception) {}
                }
            }
        }
    }
}