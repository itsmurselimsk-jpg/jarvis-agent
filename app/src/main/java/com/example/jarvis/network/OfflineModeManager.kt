package com.example.jarvis.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Offline Mode Manager & Local Tool Router.
 * Detects online/offline network transitions and directs local commands
 * (Volume, Brightness, Flashlight, Apps, Battery, Device Info, Timer, Calculator)
 * to run on-device with zero latency and zero internet dependency.
 */
class OfflineModeManager(private val context: Context) {

    companion object {
        val OFFLINE_CAPABLE_TOOLS = setOf(
            "volume",
            "brightness",
            "flashlight",
            "battery",
            "deviceinfo",
            "applauncher",
            "timer",
            "datetime",
            "calculator",
            "clipboard",
            "tasks",
            "memory",
            "wifi",
            "bluetooth",
            "phonecall"
        )

        fun isToolOfflineCapable(toolName: String): Boolean {
            return OFFLINE_CAPABLE_TOOLS.contains(toolName.lowercase().trim())
        }

        fun getOfflineGracefulMessage(isHindi: Boolean = false): String {
            return if (isHindi) {
                "⚡ [OFFLINE MODE ACTIVE] Internet disconnected hai. Aapke local tools (Flashlight, Volume, Brightness, Battery, Apps, Timer, Calculator) on-device active hain."
            } else {
                "⚡ [OFFLINE MODE ACTIVE] No internet connection detected. Local device tools (Volume, Brightness, Flashlight, Battery, Apps, Timer, Calculator) are fully functional on-device."
            }
        }
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow("CHECKING")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            updateNetworkType(network)
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
            _networkType.value = "OFFLINE"
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            if (hasInternet) {
                updateNetworkType(network)
            } else {
                _networkType.value = "LIMITED"
            }
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}
    }

    private fun checkInitialConnectivity(): Boolean {
        val active = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateNetworkType(network: Network) {
        val caps = connectivityManager?.getNetworkCapabilities(network) ?: return
        _networkType.value = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "CONNECTED"
        }
    }

    fun unregister() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
    }
}
