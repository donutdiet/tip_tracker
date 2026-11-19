package com.example.tiptracker.ui.placesearch

import com.example.tiptracker.data.helper.Coordinates
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlaceSearchResult
import com.example.tiptracker.data.repository.PlacesRepository
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchDelegateTest {

    private val token = mockk<AutocompleteSessionToken>()
    private val places = mockk<PlacesRepository> {
        every { isAvailable } returns true
        every { newSessionToken() } returns token
    }
    private val location = mockk<LocationProvider>()
    private val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Online)

    private val pearl = PlacePrediction("id-pearl", "The Pearl", "1 Main St, Portland, OR")
    private val results = PlaceSearchResult.Success(listOf(pearl))

    init {
        coEvery { places.search(any(), any(), any()) } returns results
        coEvery { location.getCurrentCoordinates() } returns null
    }

    // backgroundScope is cancelled when the test ends, which stops the delegate's collector
    private fun TestScope.delegate() =
        PlaceSearchDelegate(backgroundScope, places, location, status, testTimeSource)

    // advanceUntilIdle() ignores background work, so move virtual time past the debounce
    // (500 ms) and any simulated latency explicitly, then run what is due.
    private fun TestScope.settle() {
        advanceTimeBy(1_500)
        runCurrent()
    }

    @Test
    fun `rapid typing is coalesced into a single search for the final text`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("pizza")
        advanceTimeBy(100)
        delegate.onQueryChange("pizzas")
        advanceTimeBy(100)
        delegate.onQueryChange("pizzash")
        settle()

        coVerify(exactly = 1) { places.search(any(), any(), any()) }
        coVerify { places.search("pizzash", null, token) }
        assertEquals(listOf(pearl), delegate.state.value.predictions)
    }

    @Test
    fun `queries shorter than five characters do not search`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("abcd")
        settle()
        delegate.onQueryChange("  abcd  ")
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `shortening the query clears existing suggestions immediately`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        assertEquals(listOf(pearl), delegate.state.value.predictions)

        delegate.onQueryChange("pe")

        // No time has passed: the stale list must not linger during the debounce window
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `query is trimmed before searching`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("  pearl  ")
        settle()

        coVerify { places.search("pearl", any(), any()) }
    }

    @Test
    fun `no search while offline`() = runTest {
        status.value = ConnectionStatus.Offline
        val delegate = delegate()

        delegate.onQueryChange("pearl")
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `going offline clears suggestions`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        assertEquals(listOf(pearl), delegate.state.value.predictions)

        status.value = ConnectionStatus.Offline
        settle()

        assertTrue(delegate.state.value.predictions.isEmpty())
        assertFalse(delegate.state.value.searchFailed)
    }

    @Test
    fun `coming back online does not search by itself`() = runTest {
        // The delegate outlives the screen, so a reconnect in the background must not bill
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()

        status.value = ConnectionStatus.Offline
        settle()
        status.value = ConnectionStatus.Online
        settle()

        coVerify(exactly = 1) { places.search(any(), any(), any()) }
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `focusing after coming back online searches the current text`() = runTest {
        status.value = ConnectionStatus.Offline
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        status.value = ConnectionStatus.Online
        settle()
        coVerify(exactly = 0) { places.search(any(), any(), any()) }

        delegate.onFocus()
        settle()

        coVerify(exactly = 1) { places.search("pearl", any(), any()) }
        assertEquals(listOf(pearl), delegate.state.value.predictions)
    }

    @Test
    fun `focusing does not search again while its results are shown`() = runTest {
        coEvery { places.search(any(), any(), any()) } returns PlaceSearchResult.Success(emptyList())
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()

        // "No results" is a result too: don't pay to hear it again
        delegate.onFocus()
        settle()

        coVerify(exactly = 1) { places.search(any(), any(), any()) }
    }

    @Test
    fun `focusing with a short query does not search`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pea")
        settle()

        delegate.onFocus()
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
    }

    @Test
    fun `no search when the Places SDK is unavailable`() = runTest {
        every { places.isAvailable } returns false
        val delegate = delegate()

        delegate.onQueryChange("pearl")
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
        assertFalse(delegate.state.value.searchFailed)
    }

    @Test
    fun `results of a superseded query are discarded`() = runTest {
        val stale = PlacePrediction("id-old", "Old Place", "Old St")
        val fresh = PlacePrediction("id-new", "New Place", "New St")
        coEvery { places.search("pizza", any(), any()) } coAnswers {
            delay(1_000)
            PlaceSearchResult.Success(listOf(stale))
        }
        coEvery { places.search("pizzas", any(), any()) } returns
                PlaceSearchResult.Success(listOf(fresh))
        val delegate = delegate()

        delegate.onQueryChange("pizza")
        advanceTimeBy(501) // debounce elapsed, first request now in flight
        delegate.onQueryChange("pizzas")
        settle()

        assertEquals(listOf(fresh), delegate.state.value.predictions)
    }

    @Test
    fun `failed search sets the flag and the next keystroke clears it`() = runTest {
        coEvery { places.search(any(), any(), any()) } returns PlaceSearchResult.Failure
        val delegate = delegate()

        delegate.onQueryChange("pearl")
        settle()
        assertTrue(delegate.state.value.searchFailed)
        assertTrue(delegate.state.value.predictions.isEmpty())

        delegate.onQueryChange("pearls")
        assertFalse(delegate.state.value.searchFailed)
    }

    @Test
    fun `successful search after a failure clears the flag`() = runTest {
        coEvery { places.search("pearl", any(), any()) } returns PlaceSearchResult.Failure
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        assertTrue(delegate.state.value.searchFailed)

        delegate.onQueryChange("pearls")
        settle()

        assertFalse(delegate.state.value.searchFailed)
        assertEquals(listOf(pearl), delegate.state.value.predictions)
    }

    @Test
    fun `selecting a prediction clears suggestions without searching the typed text again`() =
        runTest {
            val delegate = delegate()
            delegate.onQueryChange("pearl")
            settle()
            coVerify(exactly = 1) { places.search(any(), any(), any()) }

            delegate.select(pearl)
            settle()

            assertTrue(delegate.state.value.predictions.isEmpty())
            coVerify(exactly = 1) { places.search(any(), any(), any()) }
            // TODO: remove this assertion when the coordinates lookup is re-enabled
            coVerify(exactly = 0) { places.fetchCoordinates(any(), any()) }
        }

    @Test
    fun `reconnecting after a selection does not search the typed text again`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        delegate.select(pearl)
        settle()

        status.value = ConnectionStatus.Offline
        settle()
        status.value = ConnectionStatus.Online
        settle()

        assertTrue(delegate.state.value.predictions.isEmpty())
        coVerify(exactly = 1) { places.search(any(), any(), any()) }
    }

    @Test
    fun `typing after a selection searches again, even for the same text`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        delegate.select(pearl)
        settle()

        delegate.onQueryChange("The Pearl")
        settle()

        coVerify { places.search("The Pearl", any(), any()) }
    }

    @Test
    fun `clearing a place with unchanged text restores its suggestions without searching`() =
        runTest {
            val delegate = delegate()
            delegate.onQueryChange("pearl")
            settle()
            delegate.select(pearl)
            settle()

            delegate.onPlaceCleared("pearl ")
            settle()

            assertEquals(listOf(pearl), delegate.state.value.predictions)
            coVerify(exactly = 1) { places.search(any(), any(), any()) }

            // And focusing right after doesn't pay for them again
            delegate.onFocus()
            settle()
            coVerify(exactly = 1) { places.search(any(), any(), any()) }
        }

    @Test
    fun `clearing a place after the text changed searches the new text`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()
        delegate.select(pearl)
        settle()

        delegate.onPlaceCleared("pearl bakery")
        settle()

        coVerify { places.search("pearl bakery", any(), any()) }
    }

    @Test
    fun `clearing a place that was linked when the screen opened searches`() = runTest {
        // Nothing was picked in this delegate, so there's nothing to restore
        val delegate = delegate()

        delegate.onPlaceCleared("The Pearl")
        settle()

        coVerify(exactly = 1) { places.search("The Pearl", any(), any()) }
    }

    @Test
    fun `reset starts a new session`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()

        delegate.reset()

        // Once on construction, once on reset
        io.mockk.verify(exactly = 2) { places.newSessionToken() }
    }

    @Test
    fun `a new session token is created after a selection`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()

        delegate.select(pearl)

        // Once on construction, once after the selection ends the session
        io.mockk.verify(exactly = 2) { places.newSessionToken() }
    }

    @Test
    fun `reset clears suggestions and the failure flag`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pearl")
        settle()

        delegate.reset()

        assertTrue(delegate.state.value.predictions.isEmpty())
        assertFalse(delegate.state.value.searchFailed)
    }

    @Test
    fun `device location is used as the search origin and looked up once`() = runTest {
        val here = Coordinates(45.5, -122.6)
        coEvery { location.getCurrentCoordinates() } returns here
        val delegate = delegate()

        delegate.onQueryChange("pizza")
        settle()
        delegate.onQueryChange("pasta")
        settle()

        coVerify(exactly = 1) { location.getCurrentCoordinates() }
        coVerify { places.search("pizza", here, token) }
        coVerify { places.search("pasta", here, token) }
    }

    @Test
    fun `without a location fix, it is not retried within a minute`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("pizza")
        settle()
        delegate.onQueryChange("pasta")
        settle()

        // The second search didn't wait on another location attempt
        coVerify(exactly = 1) { location.getCurrentCoordinates() }
        coVerify { places.search("pasta", null, token) }
    }

    @Test
    fun `without a location fix, it is retried after a minute`() = runTest {
        val here = Coordinates(45.5, -122.6)
        coEvery { location.getCurrentCoordinates() } returnsMany listOf(null, here)
        val delegate = delegate()

        delegate.onQueryChange("pizza")
        settle()
        advanceTimeBy(60_000)
        delegate.onQueryChange("pasta")
        settle()

        coVerify { places.search("pizza", null, token) }
        coVerify { places.search("pasta", here, token) }
    }
}
