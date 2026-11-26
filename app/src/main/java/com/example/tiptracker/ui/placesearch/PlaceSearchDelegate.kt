package com.example.tiptracker.ui.placesearch

import com.example.tiptracker.data.helper.Coordinates
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.model.restaurantNameKey
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

data class PlaceSearchState(
    /** Google's suggestions, places the user has logged before first. */
    val predictions: List<PlacePrediction> = emptyList(),
    /** The last search request failed. Cleared on the next keystroke or successful search. */
    val searchFailed: Boolean = false,
    /**
     * Restaurants from past logs matching the field's text that aren't among [predictions].
     * Local, so offline and free. Linked places wait for Google's results while Google can
     * search, so they never show here and then move into [predictions].
     */
    val history: List<RestaurantHistory> = emptyList(),
    /**
     * The user's past visits to places in [predictions], by place ID. Those suggestions show the
     * user's own name for the place and their visit count, with Google's address.
     */
    val visitedPlaces: Map<String, RestaurantHistory> = emptyMap()
)

/**
 * Restaurant suggestions shared by the screens with the restaurant name field: the user's past
 * restaurants, and debounced Google autocomplete.
 *
 * Every autocomplete request is billed, so a search only ever starts from something the user
 * does: typing, focusing the field, or clearing a linked place. Coming back online does not
 * search by itself (the pipeline lives as long as the ViewModel, including in the background);
 * suggestions return when the user next focuses the field. Searches need a long enough query
 * and a connection, and a newer query cancels the in-flight one.
 *
 * History suggestions follow the field's text immediately and work offline and without a
 * Places key. A past restaurant that Google also suggests is shown once, as Google's suggestion
 * (which has the address, telling chain locations apart) labeled with the user's visits. So
 * that nothing jumps between the two lists, a past restaurant linked to a place is held back
 * while Google could still return it, and listed with history only if Google doesn't.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PlaceSearchDelegate(
    scope: CoroutineScope,
    private val places: PlacesRepository,
    private val location: LocationProvider,
    private val connectionStatus: StateFlow<ConnectionStatus>,
    history: Flow<List<RestaurantHistory>>,
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

    // All the user's past restaurants, and the field text they're matched against
    private var historyRows: List<RestaurantHistory> = emptyList()
    private var historyText = ""

    // Google's latest results as returned; [state] is built from these and the history
    private var google: List<PlacePrediction> = emptyList()
    private var searchFailed = false

    private val _state = MutableStateFlow(PlaceSearchState())
    val state: StateFlow<PlaceSearchState> = _state.asStateFlow()

    init {
        // Logs saved or edited elsewhere update the suggestions shown
        history
            .onEach {
                historyRows = it
                publish()
            }
            .launchIn(scope)

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
        historyText = text
        setQuery(text)
        searchFailed = false
        if (!text.isSearchable()) google = emptyList()
        publish()
    }

    /**
     * Call when the field gains focus while no place is linked, with the field's current [text].
     * Searches it if its suggestions aren't already shown, e.g. they were cleared while offline,
     * or the text was loaded from a saved log rather than typed.
     */
    fun onFocus(text: String) {
        showHistoryFor(text)
        if (text.isSearchable() && resultsFor != text.trim()) setQuery(text)
    }

    /**
     * Call when the user picks a prediction, with [fieldText] being what the field shows after
     * the pick (if the screen fills in the place's name, that name). Hides suggestions until the
     * next edit and keeps them for [onPlaceCleared] while the field still shows [fieldText].
     * Returns the place's coordinates, or null if they couldn't be fetched. Ends the
     * autocomplete session and starts a new one.
     */
    suspend fun select(
        prediction: PlacePrediction,
        fieldText: String = query.value.text
    ): Coordinates? {
        onPicked(fieldText)

        // Ends the session: its autocomplete requests past the 12th are then free
        val finishedSession = sessionToken
        sessionToken = places.newSessionToken()
        return places.fetchCoordinates(prediction.placeId, finishedSession)
    }

    /**
     * The name the field should show after picking [prediction]: the user's own name for a place
     * they've logged before, otherwise Google's. Call before [select], which hides suggestions.
     */
    fun nameFor(prediction: PlacePrediction): String =
        _state.value.visitedPlaces[prediction.placeId]?.name ?: prediction.name

    /**
     * Call when the user picks a history suggestion, with [fieldText] being what the field shows
     * after the pick. Like [select], but makes no billed request: picking from history links the
     * place ID the past logs already have. Starts a new autocomplete session.
     */
    fun onHistoryPicked(fieldText: String) {
        onPicked(fieldText)
        sessionToken = places.newSessionToken()
    }

    /**
     * Call when the user unlinks the place, with the field's current [text]. If it hasn't
     * changed since the pick, the suggestions picked from come back at no cost; otherwise
     * [text] is searched.
     */
    fun onPlaceCleared(text: String) {
        showHistoryFor(text)
        val picked = pickedFrom
        if (picked != null && picked.first == text.trim() && picked.second.isNotEmpty()) {
            pickedFrom = null
            resultsFor = picked.first
            setGoogleResults(picked.second)
        } else {
            onQueryChange(text)
        }
    }

    /** Clears everything when the form is reset, e.g. after a save; the next search is a new session. */
    fun reset() {
        pickedFrom = null
        resultsFor = null
        setQuery("")
        setGoogleResults(emptyList())
        showHistoryFor("")
        sessionToken = places.newSessionToken()
    }

    // Hides Google's suggestions until the next edit, keeping them for onPlaceCleared
    private fun onPicked(fieldText: String) {
        pickedFrom = fieldText.trim() to google
        setGoogleResults(emptyList())
        showHistoryFor(fieldText)
    }

    private fun setQuery(text: String) {
        query.value = Query(text, ++lastQueryId)
    }

    private fun setGoogleResults(predictions: List<PlacePrediction>, searchFailed: Boolean = false) {
        google = predictions
        this.searchFailed = searchFailed
        publish()
    }

    private fun clearResults() {
        resultsFor = null
        setGoogleResults(emptyList())
    }

    private fun showHistoryFor(text: String) {
        historyText = text
        publish()
    }

    // Merges the history into Google's results: a place in both is shown once, as Google's
    // suggestion with the user's visits, and Google's suggestions for visited places come first
    private fun publish() {
        val historyByPlace = historyRows
            .filter { it.placeId != null }
            .associateBy { it.placeId!! }
        val visited = google
            .mapNotNull { prediction -> historyByPlace[prediction.placeId]?.let { prediction.placeId to it } }
            .toMap()
        _state.value = PlaceSearchState(
            // Stable, so Google's order (restaurants first) holds within each group
            predictions = google.sortedByDescending { it.placeId in visited },
            searchFailed = searchFailed,
            history = matchHistory(historyText, shownByGoogle = visited.keys),
            visitedPlaces = visited
        )
    }

    // Past restaurants with a word starting with what's typed, most visited first
    private fun matchHistory(text: String, shownByGoogle: Set<String>): List<RestaurantHistory> {
        val typed = restaurantNameKey(text)
        if (typed.isEmpty()) return emptyList()
        // Google has answered for this text, or won't be asked
        val googleSettled = !googleCanSearch() || resultsFor == text.trim()
        return historyRows
            .filter { it.placeId == null || (googleSettled && it.placeId !in shownByGoogle) }
            .filter { it.nameKey.startsWith(typed) || it.nameKey.contains(" $typed") }
            .sortedWith(
                compareByDescending<RestaurantHistory> { it.visitCount }
                    .thenByDescending { it.lastVisited }
            )
            .take(MAX_HISTORY_SUGGESTIONS)
    }

    private fun googleCanSearch() =
        connectionStatus.value == ConnectionStatus.Online && places.isAvailable && !searchFailed

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
                setGoogleResults(result.predictions)
            }

            PlaceSearchResult.Failure -> {
                resultsFor = null
                setGoogleResults(emptyList(), searchFailed = true)
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
        const val DEBOUNCE_MS = 450L
        const val MIN_QUERY_LENGTH = 3
        // Leaves room for Google's suggestions below
        const val MAX_HISTORY_SUGGESTIONS = 3
        const val LOCATION_TIMEOUT_MS = 2_000L
        val LOCATION_RETRY_INTERVAL = 1.minutes
    }
}
