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
    private val query = MutableStateFlow("")

    // Text we set ourselves after a selection; it must not trigger another search
    private var suppressedQuery: String? = null

    private var sessionToken = places.newSessionToken()
    private var origin: Coordinates? = null

    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    init {
        combine(
            query.debounce(DEBOUNCE_MS.milliseconds).distinctUntilChanged(),
            connectionStatus.distinctUntilChanged()
        ) { text, status -> text to status }
            .mapLatest { (text, status) -> search(text, status) }
            .launchIn(scope)
    }

    /** Call for every edit the user makes to the field. */
    fun onQueryChange(text: String) {
        suppressedQuery = null
        query.value = text
        _state.update {
            it.copy(
                searchFailed = false,
                predictions = if (text.isSearchable()) it.predictions else emptyList()
            )
        }
    }

    /**
     * Call when the user picks a prediction. Returns the place's coordinates, or null if they
     * couldn't be fetched. Ends the autocomplete session and starts a new one.
     */
    suspend fun select(prediction: PlacePrediction): Coordinates? {
        suppressedQuery = prediction.name
        query.value = prediction.name
        _state.value = PlaceSearchState()

        val finishedSession = sessionToken
        sessionToken = places.newSessionToken()
        return places.fetchCoordinates(prediction.placeId, finishedSession)
    }

    /** Clears everything, e.g. when the form is reset. */
    fun reset() {
        suppressedQuery = null
        query.value = ""
        _state.value = PlaceSearchState()
    }

    private suspend fun search(text: String, status: ConnectionStatus) {
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
        if (query.value != text) return
        _state.value = when (result) {
            is PlaceSearchResult.Success -> PlaceSearchState(predictions = result.predictions)
            PlaceSearchResult.Failure -> PlaceSearchState(searchFailed = true)
        }
    }

    private fun String.isSearchable() = trim().length >= MIN_QUERY_LENGTH

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 3
        const val LOCATION_TIMEOUT_MS = 2_000L
    }
}
