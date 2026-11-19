package com.example.tiptracker.ui.placesearch

import com.example.tiptracker.data.helper.Coordinates
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlaceSearchResult
import com.example.tiptracker.data.repository.PlacesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

data class PlaceSearchState(
    val predictions: List<PlacePrediction> = emptyList(),
    /** The last search request failed. Cleared on the next keystroke or successful search. */
    val searchFailed: Boolean = false
)

/**
 * Debounced restaurant autocomplete shared by the screens with the restaurant name field.
 *
 * Every autocomplete request is billed, so a search only ever starts from something the user
 * does: typing, focusing the field, or clearing a linked place. Coming back online does not
 * search by itself (the pipeline lives as long as the ViewModel, including in the background);
 * suggestions return when the user next focuses the field. Searches need a long enough query
 * and a connection, and a newer query cancels the in-flight one.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlaceSearchDelegate(
    scope: CoroutineScope,
    private val places: PlacesRepository,
    private val location: LocationProvider,
    private val connectionStatus: StateFlow<ConnectionStatus>,
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic
) {
    // Each request gets a fresh id so that asking again for identical text (e.g. on focus)
    // still counts as new input and is not swallowed by distinctUntilChanged
    private data class Query(val text: String, val id: Long)

    private var lastQueryId = 0L
    private val query = MutableStateFlow(Query("", lastQueryId))

    // Trimmed text whose results are currently shown (including "no results"), so focusing
    // the field doesn't pay for the same search again
    private var resultsFor: String? = null

    // Suggestions the user picked from, kept until the next edit so that clearing the link
    // can show them again for free
    private var pickedFrom: Pair<String, List<PlacePrediction>>? = null

    private var sessionToken = places.newSessionToken()
    private var origin: Coordinates? = null
    private var lastLocationAttempt: ComparableTimeMark? = null

    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    init {
        query.debounce(DEBOUNCE_MS.milliseconds)
            .distinctUntilChanged()
            .mapLatest { search(it) }
            .launchIn(scope)

        // Clearing is free; searching again is left to the user's next interaction
        connectionStatus
            .filter { it != ConnectionStatus.Online }
            .onEach { clearResults() }
            .launchIn(scope)
    }

    /** Call for every edit the user makes to the field. */
    fun onQueryChange(text: String) {
        pickedFrom = null
        setQuery(text)
        _state.update {
            it.copy(
                searchFailed = false,
                predictions = if (text.isSearchable()) it.predictions else emptyList()
            )
        }
    }

    /**
     * Call when the field gains focus while no place is linked. Searches the current text if
     * its suggestions aren't already shown, e.g. they were cleared while offline.
     */
    fun onFocus() {
        val text = query.value.text
        if (text.isSearchable() && resultsFor != text.trim()) setQuery(text)
    }

    /**
     * Call when the user picks a prediction. Hides suggestions until the next edit and keeps
     * them for [onPlaceCleared]. Returns the place's coordinates, or null if they couldn't be
     * fetched. Ends the autocomplete session and starts a new one.
     */
    suspend fun select(prediction: PlacePrediction): Coordinates? {
        pickedFrom = query.value.text.trim() to _state.value.predictions
        _state.value = PlaceSearchState()

        // TODO: re-enable when the map ships. Disabled so no Place Details request is made;
        //  logs are saved with place_id only and lat/lng stay null until then.
        //  Note this also means the autocomplete session is never ended by a details call.
        // val finishedSession = sessionToken
        // sessionToken = places.newSessionToken()
        // return places.fetchCoordinates(prediction.placeId, finishedSession)

        sessionToken = places.newSessionToken()
        return null
    }

    /**
     * Call when the user unlinks the place, with the field's current [text]. If it hasn't
     * changed since the pick, the suggestions picked from come back at no cost; otherwise
     * [text] is searched.
     */
    fun onPlaceCleared(text: String) {
        val picked = pickedFrom
        if (picked != null && picked.first == text.trim() && picked.second.isNotEmpty()) {
            pickedFrom = null
            resultsFor = picked.first
            _state.value = PlaceSearchState(predictions = picked.second)
        } else {
            onQueryChange(text)
        }
    }

    /** Clears everything when the form is reset, e.g. after a save; the next search is a new session. */
    fun reset() {
        pickedFrom = null
        resultsFor = null
        setQuery("")
        _state.value = PlaceSearchState()
        sessionToken = places.newSessionToken()
    }

    private fun setQuery(text: String) {
        query.value = Query(text, ++lastQueryId)
    }

    private fun clearResults() {
        resultsFor = null
        _state.value = PlaceSearchState()
    }

    private suspend fun search(input: Query) {
        val trimmed = input.text.trim()
        if (connectionStatus.value != ConnectionStatus.Online ||
            !places.isAvailable ||
            !input.text.isSearchable()
        ) {
            clearResults()
            return
        }

        val result = places.search(trimmed, currentOrigin(), sessionToken)

        // Drop results for a query the user has already edited, or if we went offline
        if (query.value != input) return
        if (connectionStatus.value != ConnectionStatus.Online) {
            clearResults()
            return
        }
        when (result) {
            is PlaceSearchResult.Success -> {
                resultsFor = trimmed
                _state.value = PlaceSearchState(predictions = result.predictions)
            }

            PlaceSearchResult.Failure -> {
                resultsFor = null
                _state.value = PlaceSearchState(searchFailed = true)
            }
        }
    }

    // Device location to rank nearby places first. Free, but waiting for it slows results,
    // so while no fix is available it's retried at most once a minute rather than every search.
    private suspend fun currentOrigin(): Coordinates? {
        origin?.let { return it }
        val last = lastLocationAttempt
        if (last != null && last.elapsedNow() < LOCATION_RETRY_INTERVAL) return null
        lastLocationAttempt = timeSource.markNow()
        // Bounded so a slow location fix never holds up results for long
        origin = withTimeoutOrNull(LOCATION_TIMEOUT_MS.milliseconds) {
            location.getCurrentCoordinates()
        }
        return origin
    }

    private fun String.isSearchable() = trim().length >= MIN_QUERY_LENGTH

    private companion object {
        // Longer debounce and minimum length keep billed autocomplete requests down
        const val DEBOUNCE_MS = 500L
        const val MIN_QUERY_LENGTH = 5
        const val LOCATION_TIMEOUT_MS = 2_000L
        val LOCATION_RETRY_INTERVAL = 1.minutes
    }
}
