package com.example.tiptracker.data.repository

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * Overall connectivity of the app. Kept as a sealed type so additional states
 * (e.g. a server being unreachable) can be added later without changing consumers.
 */
sealed interface ConnectionStatus {
    data object Online : ConnectionStatus
    data object Offline : ConnectionStatus
}

class ConnectivityRepository(private val connectivityManager: ConnectivityManager) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Emits [ConnectionStatus.Online] only when the default network can reach the internet
     * (INTERNET + VALIDATED), so captive portals and dead Wi-Fi count as offline.
     */
    val status: StateFlow<ConnectionStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.toStatus())
            }

            override fun onLost(network: Network) {
                trySend(ConnectionStatus.Offline)
            }
        }
        // Registering does not deliver an initial value if there is no default network.
        trySend(connectivityManager.currentStatus())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = connectivityManager.currentStatus()
        )

    private fun ConnectivityManager.currentStatus(): ConnectionStatus =
        getNetworkCapabilities(activeNetwork).toStatus()

    private fun NetworkCapabilities?.toStatus(): ConnectionStatus {
        val online = this != null &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return if (online) ConnectionStatus.Online else ConnectionStatus.Offline
    }
}
