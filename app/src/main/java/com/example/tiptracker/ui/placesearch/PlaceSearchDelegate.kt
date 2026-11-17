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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class PlaceSearchState(
    val predictions: List<PlacePrediction> = emptyList(),
    /** The last search request failed. Cleared on the next keystroke or successful search. */
    val searchFailed: Boolean = false
)

/**
 * Debounced restaurant autocomplete shared by the screens that have the combined
 * name/address field. Searches only while online and once the query is long enough;
 * a newer query cancels the in-flight one.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlaceSearchDelegate(
    scope: CoroutineScope,
    private val places: PlacesRepository,
    private val location: LocationProvider,
    connectionStatus: Flow<ConnectionStatus>
) {
    // Each edit gets a fresh id so that retyping identical text (e.g. after a selection)
    // still counts as new input and is not swallowed by distinctUntilChanged
    private data class Query(val text: String, val id: Long)

    private var lastQueryId = 0L
    private val query = MutableStateFlow(Query("", lastQueryId))

    // The text a selection was made from; it must not be searched again (e.g. when
    // connectivity changes) until the user edits it
    private var suppressedQuery: String? = null

    private var sessionToken = places.newSessionToken()
    private var origin: Coordinates? = null

    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    init {
        combine(
            query.debounce(DEBOUNCE_MS.milliseconds).distinctUntilChanged(),
            connectionStatus.distinctUntilChanged()
        ) { input, status -> input to status }
            .mapLatest { (input, status) -> search(input, status) }
            .launchIn(scope)
    }

    /** Call for every edit the user makes to the field. */
    fun onQueryChange(text: String) {
        suppressedQuery = null
        setQuery(text)
        _state.update {
            it.copy(
                searchFailed = false,
                predictions = if (text.isSearchable()) it.predictions else emptyList()
            )
        }
    }

    /**
     * Call when the user picks a prediction. Leaves the typed text alone (only what the user
     * typed is ever saved) and hides suggestions until the next edit. Returns the place's
     * coordinates, or null if they couldn't be fetched. Ends the autocomplete session and
     * starts a new one.
     */
    suspend fun select(prediction: PlacePrediction): Coordinates? {
        suppressedQuery = query.value.text
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

    /** Clears everything, e.g. when the form is reset. */
    fun reset() {
        suppressedQuery = null
        setQuery("")
        _state.value = PlaceSearchState()
    }

    private fun setQuery(text: String) {
        query.value = Query(text, ++lastQueryId)
    }

    private suspend fun search(input: Query, status: ConnectionStatus) {
        val text = input.text
        val trimmed = text.trim()
        if (status != ConnectionStatus.Online ||
            !places.isAvailable ||
            !text.isSearchable() ||
            trimmed == suppressedQuery?.trim()
        ) {
            _state.value = PlaceSearchState()
            return
        }

        if (origin == null) {
            // Bounded so a slow location fix never holds up the first results
            origin = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { location.getCurrentCoordinates() }
        }

        val result = places.search(trimmed, origin, sessionToken)

        // Drop results for a query the user has already edited
        if (query.value != input) return
        _state.value = when (result) {
            is PlaceSearchResult.Success -> PlaceSearchState(predictions = result.predictions)
            PlaceSearchResult.Failure -> PlaceSearchState(searchFailed = true)
        }
    }

    private fun String.isSearchable() = trim().length >= MIN_QUERY_LENGTH

    private companion object {
        // Longer debounce and minimum length keep billed autocomplete requests down
        const val DEBOUNCE_MS = 500L
        const val MIN_QUERY_LENGTH = 5
        const val LOCATION_TIMEOUT_MS = 2_000L
    }
}
