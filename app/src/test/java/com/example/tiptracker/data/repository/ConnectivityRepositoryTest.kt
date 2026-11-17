package com.example.tiptracker.data.repository

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityRepositoryTest {

    private val network = mockk<Network>()

    private fun capabilities(internet: Boolean, validated: Boolean) = mockk<NetworkCapabilities> {
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns internet
        every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns validated
    }

    private fun initialStatus(caps: NetworkCapabilities?, hasNetwork: Boolean = true): ConnectionStatus {
        val manager = mockk<ConnectivityManager> {
            every { activeNetwork } returns if (hasNetwork) network else null
            every { getNetworkCapabilities(if (hasNetwork) network else null) } returns caps
        }
        return ConnectivityRepository(manager).status.value
    }

    @Test
    fun `internet and validated is online`() {
        assertEquals(
            ConnectionStatus.Online,
            initialStatus(capabilities(internet = true, validated = true))
        )
    }

    @Test
    fun `connected but not validated is offline`() {
        // e.g. captive portal or Wi-Fi without upstream connectivity
        assertEquals(
            ConnectionStatus.Offline,
            initialStatus(capabilities(internet = true, validated = false))
        )
    }

    @Test
    fun `network without internet capability is offline`() {
        assertEquals(
            ConnectionStatus.Offline,
            initialStatus(capabilities(internet = false, validated = true))
        )
    }

    @Test
    fun `no active network is offline`() {
        assertEquals(
            ConnectionStatus.Offline,
            initialStatus(caps = null, hasNetwork = false)
        )
    }

    // The repository collects on Dispatchers.Default, so these use real (short) waits
    private fun withRegisteredCallback(
        manager: ConnectivityManager,
        block: suspend (ConnectivityRepository, ConnectivityManager.NetworkCallback) -> Unit
    ) = runBlocking {
        val callback = slot<ConnectivityManager.NetworkCallback>()
        every { manager.registerDefaultNetworkCallback(capture(callback)) } just runs
        every { manager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) } just runs
        val repository = ConnectivityRepository(manager)
        val subscriber = launch { repository.status.collect {} }
        withTimeout(2_000) { while (!callback.isCaptured) kotlinx.coroutines.delay(10) }
        block(repository, callback.captured)
        subscriber.cancel()
    }

    @Test
    fun `losing the only network is offline even if it is still reported as active`() {
        val wifi = capabilities(internet = true, validated = true)
        val manager = mockk<ConnectivityManager> {
            // Stale: the lost Wi-Fi is still the active, validated network
            every { activeNetwork } returns network
            every { getNetworkCapabilities(network) } returns wifi
        }
        withRegisteredCallback(manager) { repository, callback ->
            assertEquals(ConnectionStatus.Online, repository.status.value)

            callback.onLost(network)

            withTimeout(2_000) { repository.status.first { it == ConnectionStatus.Offline } }
        }
    }

    @Test
    fun `losing the old network after a handoff stays online`() {
        val cellular = mockk<Network>()
        val validated = capabilities(internet = true, validated = true)
        val manager = mockk<ConnectivityManager> {
            every { activeNetwork } returns cellular
            every { getNetworkCapabilities(cellular) } returns validated
        }
        withRegisteredCallback(manager) { repository, callback ->
            callback.onLost(network)

            kotlinx.coroutines.delay(200)
            assertEquals(ConnectionStatus.Online, repository.status.value)
        }
    }
}
