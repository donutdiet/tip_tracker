package com.example.tiptracker.ui.placesearch

import com.example.tiptracker.data.helper.Coordinates
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.model.restaurantNameKey
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
    private val history = MutableStateFlow<List<RestaurantHistory>>(emptyList())

    private val pearl = PlacePrediction("id-pearl", "The Pearl", "1 Main St, Portland, OR")
    private val results = PlaceSearchResult.Success(listOf(pearl))

    init {
        coEvery { places.search(any(), any(), any()) } returns results
        coEvery { location.getCurrentCoordinates() } returns null
    }

    // backgroundScope is cancelled when the test ends, which stops the delegate's collector
    private fun TestScope.delegate() =
        PlaceSearchDelegate(backgroundScope, places, location, status, history, testTimeSource)

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
    fun `queries shorter than three characters do not search`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("ab")
        settle()
        delegate.onQueryChange("  ab  ")
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `a three character query searches`() = runTest {
        val delegate = delegate()

        delegate.onQueryChange("KFC")
        settle()

        coVerify(exactly = 1) { places.search("KFC", any(), any()) }
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

        delegate.onFocus("pearl")
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
        delegate.onFocus("pearl")
        settle()

        coVerify(exactly = 1) { places.search(any(), any(), any()) }
    }

    @Test
    fun `focusing with a short query does not search`() = runTest {
        val delegate = delegate()
        delegate.onQueryChange("pe")
        settle()

        delegate.onFocus("pe")
        settle()

        coVerify(exactly = 0) { places.search(any(), any(), any()) }
    }

    @Test
    fun `focusing searches text that was never typed, like a name loaded from a saved log`() =
        runTest {
            val delegate = delegate()

            delegate.onFocus("The Pearl")
            settle()

            coVerify(exactly = 1) { places.search("The Pearl", any(), any()) }
            assertEquals(listOf(pearl), delegate.state.value.predictions)
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
            delegate.onFocus("pearl ")
            settle()
            coVerify(exactly = 1) { places.search(any(), any(), any()) }
        }

    @Test
    fun `clearing a place whose name filled the field restores its suggestions without searching`() =
        runTest {
            val delegate = delegate()
            delegate.onQueryChange("pear")
            settle()
            delegate.select(pearl, fieldText = pearl.name)
            settle()

            delegate.onPlaceCleared(pearl.name)
            settle()

            assertEquals(listOf(pearl), delegate.state.value.predictions)
            coVerify(exactly = 1) { places.search(any(), any(), any()) }

            // Nor does focusing the filled-in name afterwards
            delegate.onFocus(pearl.name)
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

    // ----- History suggestions -----

    private fun past(name: String, visits: Int = 1, last: String = "2026-01-01", placeId: String? = null) =
        RestaurantHistory(name, placeId, visits, last, restaurantNameKey(name))

    @Test
    fun `history matches word starts as typed, without waiting or searching`() = runTest {
        history.value = listOf(past("The Pearl"), past("Pearl Bakery"), past("Spearmint"))
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("pe")

        // Immediate, and from a single word start: "Spearmint" contains "pe" but doesn't start with it
        assertEquals(
            listOf("The Pearl", "Pearl Bakery"),
            delegate.state.value.history.map { it.name }
        )
        settle()
        coVerify(exactly = 0) { places.search(any(), any(), any()) }
    }

    @Test
    fun `history ignores case and punctuation, ranks by visits, and shows at most three`() = runTest {
        history.value = listOf(
            past("Joe's Diner", visits = 1),
            past("JOES PIZZA", visits = 5),
            past("Joe's Cafe", visits = 2, last = "2026-02-01"),
            past("Joes Bar", visits = 2, last = "2026-03-01")
        )
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("joes")

        assertEquals(
            listOf("JOES PIZZA", "Joes Bar", "Joe's Cafe"),
            delegate.state.value.history.map { it.name }
        )
    }

    @Test
    fun `history works offline`() = runTest {
        status.value = ConnectionStatus.Offline
        history.value = listOf(past("The Pearl"))
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("pearl")
        settle()

        assertEquals(listOf("The Pearl"), delegate.state.value.history.map { it.name })
        assertTrue(delegate.state.value.predictions.isEmpty())
    }

    @Test
    fun `history follows logs saved while the field is in use`() = runTest {
        val delegate = delegate()
        runCurrent()
        delegate.onQueryChange("pearl")
        assertTrue(delegate.state.value.history.isEmpty())

        history.value = listOf(past("The Pearl"))
        runCurrent()

        assertEquals(listOf("The Pearl"), delegate.state.value.history.map { it.name })
    }

    @Test
    fun `picking history makes no request, and unlinking brings Google's suggestions back for free`() = runTest {
        history.value = listOf(past("The Pearl", placeId = "id-pearl"))
        val delegate = delegate()
        runCurrent()
        delegate.onQueryChange("pearl")
        settle()

        delegate.onHistoryPicked(fieldText = "The Pearl")
        assertTrue(delegate.state.value.predictions.isEmpty())

        delegate.onPlaceCleared("The Pearl")
        settle()

        assertEquals(listOf(pearl), delegate.state.value.predictions)
        coVerify(exactly = 1) { places.search(any(), any(), any()) }
        coVerify(exactly = 0) { places.fetchPlaceDetails(any()) }
    }

    // ----- Merging history into Google's suggestions -----

    @Test
    fun `a visited place Google suggests is shown once, first, under the user's name`() = runTest {
        val other = PlacePrediction("id-other", "Pearl Diner", "2 Oak Ave")
        coEvery { places.search(any(), any(), any()) } returns
            PlaceSearchResult.Success(listOf(other, pearl))
        history.value = listOf(
            past("Pearl rooftop", visits = 3, placeId = "id-pearl"),
            past("Pearl Bakery")
        )
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("pearl")
        settle()

        val state = delegate.state.value
        // Visited place first, then Google's order
        assertEquals(listOf(pearl, other), state.predictions)
        assertEquals("Pearl rooftop", state.visitedPlaces.getValue("id-pearl").name)
        // Not repeated under the user's logs; the unvisited history row stays
        assertEquals(listOf("Pearl Bakery"), state.history.map { it.name })
        assertEquals("Pearl rooftop", delegate.nameFor(pearl))
        assertEquals("Pearl Diner", delegate.nameFor(other))
    }

    @Test
    fun `a linked past place waits for Google, so it never moves between lists`() = runTest {
        history.value = listOf(past("Pearl rooftop", placeId = "id-pearl"), past("Pearl Bakery"))
        coEvery { places.search(any(), any(), any()) } returns PlaceSearchResult.Success(emptyList())
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("pearl")

        // Unlinked rows never merge, so they show right away; the linked one is held back
        assertEquals(listOf("Pearl Bakery"), delegate.state.value.history.map { it.name })

        settle()

        // Google didn't return it, so it's listed with history
        assertEquals(
            listOf("Pearl rooftop", "Pearl Bakery"),
            delegate.state.value.history.map { it.name }
        )
    }

    @Test
    fun `when Google can't search, linked past places show right away`() = runTest {
        status.value = ConnectionStatus.Offline
        history.value = listOf(past("Pearl rooftop", placeId = "id-pearl"))
        val delegate = delegate()
        runCurrent()

        delegate.onQueryChange("pe")

        assertEquals(listOf("Pearl rooftop"), delegate.state.value.history.map { it.name })
    }
}
