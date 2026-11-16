package com.example.tiptracker.data.repository

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
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
}
