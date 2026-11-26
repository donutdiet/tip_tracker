package com.example.tiptracker.ui.features.editlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.data.model.placeNameChoices
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlacesRepository
import com.example.tiptracker.ui.components.PlaceNamePrompt
import com.example.tiptracker.ui.placesearch.PlaceSearchDelegate
import com.example.tiptracker.ui.placesearch.SelectedPlace
import com.example.tiptracker.utils.collapseWhitespace
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

data class EditLogUiState(
    val bill: String = "",
    val tipAmount: String = "",
    val partySize: String = "",
    val billType: BillType = BillType.SPLIT,
    // Exactly what the user typed; this is the name that gets saved
    val restaurantName: String = "",
    // The linked Google Maps place, if any. Only its ID is saved; the rest is looked up.
    val selectedPlace: SelectedPlace? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coordinatesFetchedAt: Long? = null,
    val predictions: List<PlacePrediction> = emptyList(),
    val history: List<RestaurantHistory> = emptyList(),
    val visitedPlaces: Map<String, RestaurantHistory> = emptyMap(),
    val searchFailed: Boolean = false,
    val isOffline: Boolean = false,
    val review: String = "",
    val date: String = "",
    val rating: Double = 5.0,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
    val isUpdating: Boolean = false,
    // Set while a save waits on the user to pick the place's name
    val placeNamePrompt: PlaceNamePrompt? = null,
    // Names on the user's other logs at the linked place
    val otherNamesAtPlace: List<String> = emptyList(),
    val editsMade: Boolean = false
) {
    val tipPercent: Double
        get() {
            val billAmount = bill.toDoubleOrNull() ?: 0.0
            val tipAmountDouble = tipAmount.toDoubleOrNull() ?: 0.0
            return if (billAmount != 0.0) {
                ((tipAmountDouble / billAmount) * 100.0).roundToTwoDecimals()
            } else {
                0.0
            }
        }

    val total: Double
        get() {
            val billAmount = bill.toDoubleOrNull() ?: 0.0
            val tipAmountDouble = tipAmount.toDoubleOrNull() ?: 0.0
            return (billAmount + tipAmountDouble).roundToTwoDecimals()
        }

    // Split vs separate only matters with more than one person
    val billTypeApplies: Boolean
        get() = (partySize.toIntOrNull() ?: 0) > 1
}

sealed interface EditLogAction {
    data class BillChanged(val bill: String) : EditLogAction
    data class TipAmountChanged(val tipAmount: String) : EditLogAction
    data class PartySizeChanged(val partySize: String) : EditLogAction
    data class BillTypeChanged(val billType: BillType) : EditLogAction
    data class RestaurantQueryChanged(val query: String) : EditLogAction
    data class PlaceSelected(val prediction: PlacePrediction) : EditLogAction
    data class HistorySelected(val history: RestaurantHistory) : EditLogAction
    data object ClearPlace : EditLogAction
    data object RestaurantFieldFocused : EditLogAction
    data object ShowPlaceAddress : EditLogAction
    data class ReviewChanged(val review: String) : EditLogAction
    data class DateChanged(val date: String) : EditLogAction
    data class RatingChanged(val rating: Double) : EditLogAction
    data object Update : EditLogAction
    data class PlaceNameChosen(val name: String) : EditLogAction
    data object PlaceNameCancel : EditLogAction
}

sealed interface EditLogEvent {
    data object LogUpdated : EditLogEvent
    data class ShowError(val message: String) : EditLogEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditLogViewModel(
    private val logId: Int,
    private val logsRepository: LogRepository,
    private val placesRepository: PlacesRepository,
    locationProvider: LocationProvider,
    connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val _events = Channel<EditLogEvent>()
    val events = _events.receiveAsFlow()

    private val placeSearch = PlaceSearchDelegate(
        scope = viewModelScope,
        places = placesRepository,
        location = locationProvider,
        connectionStatus = connectivityRepository.status,
        history = logsRepository.getRestaurantHistory()
    )

    // In-flight lookup of the selected place's coordinates
    private var coordinatesJob: Job? = null

    // Whether the user changed the name since the place was linked (or the log loaded)
    private var nameEditedWhileLinked = false

    private val _uiState = MutableStateFlow(EditLogUiState())
    // Names on the user's other logs at the linked place, which saving renames
    private val otherNamesAtPlace = _uiState
        .map { it.selectedPlace?.placeId }
        .distinctUntilChanged()
        .flatMapLatest { placeId ->
            placeId?.let { logsRepository.observeNamesAtPlace(it, editingLogId = logId) }
                ?: flowOf(emptyList())
        }

    val uiState: StateFlow<EditLogUiState> = combine(
        _uiState,
        placeSearch.state,
        connectivityRepository.status,
        otherNamesAtPlace
    ) { state, search, connection, otherNames ->
        state.copy(
            otherNamesAtPlace = otherNames,
            predictions = search.predictions,
            searchFailed = search.searchFailed,
            history = search.history,
            visitedPlaces = search.visitedPlaces,
            isOffline = connection == ConnectionStatus.Offline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditLogUiState()
    )

    // Keep track of the state as it was when loaded from the database
    private var initialUiState: EditLogUiState? = null

    init {
        loadLog()
    }

    fun onAction(action: EditLogAction) {
        when (action) {
            EditLogAction.Update -> updateLog()
            is EditLogAction.PlaceNameChosen -> {
                _uiState.update { it.copy(restaurantName = action.name, placeNamePrompt = null) }
                updateLog(namesConfirmed = true)
            }
            EditLogAction.PlaceNameCancel -> _uiState.update { it.copy(placeNamePrompt = null) }
            is EditLogAction.BillChanged -> edit { it.copy(bill = action.bill) }
            is EditLogAction.TipAmountChanged -> edit { it.copy(tipAmount = action.tipAmount) }
            is EditLogAction.PartySizeChanged -> edit { it.copy(partySize = action.partySize) }
            is EditLogAction.BillTypeChanged -> edit { it.copy(billType = action.billType) }
            is EditLogAction.RestaurantQueryChanged -> onRestaurantQueryChange(action.query)
            is EditLogAction.PlaceSelected -> onPlaceSelected(action.prediction)
            is EditLogAction.HistorySelected -> onHistorySelected(action.history)
            EditLogAction.ClearPlace -> clearPlace()
            EditLogAction.RestaurantFieldFocused -> {
                // The name loaded from the log was never typed, so pass it in to be searched
                val state = _uiState.value
                if (state.selectedPlace == null) placeSearch.onFocus(state.restaurantName)
            }
            EditLogAction.ShowPlaceAddress -> showPlaceAddress()
            is EditLogAction.ReviewChanged -> edit { it.copy(review = action.review) }
            is EditLogAction.DateChanged -> edit { it.copy(date = action.date) }
            is EditLogAction.RatingChanged -> edit { it.copy(rating = action.rating) }
        }
    }

    // Applies a form change and recalculates whether the form differs from the original data
    private fun edit(transform: (EditLogUiState) -> EditLogUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            updated.copy(editsMade = checkIfEditsMade(updated))
        }
    }

    // Typing never unlinks a picked place (that's what Unlink is for), and there's nothing to
    // search for while one is linked
    private fun onRestaurantQueryChange(query: String) {
        val placeLinked = _uiState.value.selectedPlace != null
        if (placeLinked) nameEditedWhileLinked = true
        edit { it.copy(restaurantName = query) }
        if (!placeLinked) placeSearch.onQueryChange(query)
    }

    // Links the place and fills the name field with the place's name, which the user can still
    // edit before saving
    private fun onPlaceSelected(prediction: PlacePrediction) {
        coordinatesJob?.cancel()
        nameEditedWhileLinked = false
        // The user's own name for a place they've logged before, otherwise Google's
        val name = placeSearch.nameFor(prediction)
        edit {
            it.copy(
                restaurantName = name,
                selectedPlace = SelectedPlace.from(prediction),
                latitude = null,
                longitude = null,
                coordinatesFetchedAt = null
            )
        }
        coordinatesJob = viewModelScope.launch {
            val coordinates = placeSearch.select(prediction, fieldText = name)
            _uiState.update {
                // Ignore if the selection changed while we were waiting
                if (it.selectedPlace?.placeId == prediction.placeId) {
                    it.copy(
                        latitude = coordinates?.latitude,
                        longitude = coordinates?.longitude,
                        coordinatesFetchedAt = coordinates?.let { System.currentTimeMillis() }
                    )
                } else it
            }
        }
    }

    // Fills in the name used on the last visit, and links the place the past visits are linked
    // to, if any. No API call: the place ID is already saved.
    private fun onHistorySelected(history: RestaurantHistory) {
        coordinatesJob?.cancel()
        nameEditedWhileLinked = false
        val name = history.name
        edit {
            it.copy(
                restaurantName = name,
                selectedPlace = history.placeId?.let { placeId -> SelectedPlace(placeId) },
                latitude = null,
                longitude = null,
                coordinatesFetchedAt = null
            )
        }
        placeSearch.onHistoryPicked(fieldText = name)
    }

    private fun loadLog() {
        viewModelScope.launch {
            try {
                val log = logsRepository.getLogById(logId).first()
                if (log == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNotFound = true
                        )
                    }
                    return@launch
                }

                val loadedState = EditLogUiState(
                    isLoading = false,
                    bill = log.bill.toString(),
                    tipAmount = (log.total - log.bill).roundToTwoDecimals().toString(),
                    partySize = log.partySize.toString(),
                    billType = log.billType,
                    restaurantName = log.restaurantName,
                    selectedPlace = log.placeId?.let { SelectedPlace(it) },
                    latitude = log.latitude,
                    longitude = log.longitude,
                    coordinatesFetchedAt = log.coordinatesFetchedAt,
                    review = log.review,
                    date = log.date,
                    rating = log.rating,
                    errorMessage = null,
                    editsMade = false
                )

                nameEditedWhileLinked = false
                initialUiState = loadedState
                _uiState.value = loadedState

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Couldn't load log details. Please try again."
                    )
                }
                android.util.Log.e("EditLogViewModel", "Error loading log detail", e)
            }
        }
    }

    // A linked place has one name: if the place's other logs use a different one, the user picks
    // which name they all take. A name the user changed starts selected; otherwise (e.g. editing
    // an old log's bill) the most used one does, so a quick Save doesn't rename the rest.
    private fun placeNamePrompt(name: String): PlaceNamePrompt? {
        val choices = placeNameChoices(uiState.value.otherNamesAtPlace, name) ?: return null
        val initial = if (nameEditedWhileLinked) choices.first() else choices.maxBy { it.count }
        return PlaceNamePrompt(choices, initial.name)
    }

    private fun clearPlace() {
        coordinatesJob?.cancel()
        edit { it.copy(selectedPlace = null, latitude = null, longitude = null, coordinatesFetchedAt = null) }
        // Brings suggestions back: the ones picked from if the name is unchanged (free),
        // otherwise a new search
        placeSearch.onPlaceCleared(_uiState.value.restaurantName)
    }

    // Only the place ID is stored (Google's terms), so the address of a place linked when the
    // screen opened is looked up only when the user taps "Show address", as each lookup is billed
    // (Place Details Essentials). The name isn't requested: it would bill at Pro.
    private fun showPlaceAddress() {
        val place = _uiState.value.selectedPlace ?: return
        if (place.hasDetails || place.isLoading) return
        val placeId = place.placeId

        _uiState.update { it.copy(selectedPlace = place.copy(isLoading = true, lookupFailed = false)) }
        viewModelScope.launch {
            val details = placesRepository.fetchPlaceDetails(placeId)
            _uiState.update {
                // Skip if the user cleared or changed the place meanwhile
                if (it.selectedPlace?.placeId == placeId) {
                    it.copy(selectedPlace = SelectedPlace.from(placeId, details))
                } else it
            }
        }
    }

    // [namesConfirmed]: the user already picked the place's name in the dialog
    private fun updateLog(namesConfirmed: Boolean = false) {
        if (_uiState.value.isUpdating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                // Give an in-flight coordinates lookup a moment; save without them if it's slow
                withTimeoutOrNull(COORDINATES_WAIT_MS.milliseconds) { coordinatesJob?.join() }
                val state = _uiState.value
                val bill = state.bill.toDoubleOrNull() ?: 0.0
                val tipAmount = state.tipAmount.toDoubleOrNull() ?: 0.0
                val partySize = state.partySize.toIntOrNull() ?: 1

                if (bill < 0.0 || tipAmount < 0.0 || partySize < 1) {
                    _events.send(EditLogEvent.ShowError("Please enter a valid bill, tip amount, and party size."))
                    return@launch
                }
                // A linked place shares one name across its logs, so it needs one
                if (state.selectedPlace != null && state.restaurantName.isBlank()) {
                    _events.send(EditLogEvent.ShowError("Logs linked to a place need a name. Enter one or unlink the place."))
                    return@launch
                }

                val log = Log(
                    id = logId,
                    bill = bill,
                    tipPercent = state.tipPercent,
                    total = state.total,
                    partySize = partySize,
                    // The toggle is hidden for a party of one, so don't keep a stale choice
                    billType = if (partySize > 1) state.billType else BillType.SPLIT,
                    restaurantName = state.restaurantName.collapseWhitespace(),
                    review = state.review,
                    rating = state.rating,
                    date = state.date,
                    placeId = state.selectedPlace?.placeId,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    coordinatesFetchedAt = state.coordinatesFetchedAt
                )
                if (!namesConfirmed) {
                    val prompt = log.placeId?.let { placeNamePrompt(log.restaurantName) }
                    if (prompt != null) {
                        _uiState.update { it.copy(placeNamePrompt = prompt) }
                        return@launch
                    }
                }
                // Linked to a place, this also renames the place's other logs
                logsRepository.updateLog(log)
                _events.send(EditLogEvent.LogUpdated)
            } catch (e: Exception) {
                _events.send(EditLogEvent.ShowError("Couldn't update your log. Please try again."))
                android.util.Log.e("EditLogViewModel", "Error updating log $logId", e)
            } finally {
                _uiState.update { it.copy(isUpdating = false) }
            }
        }
    }


    private fun checkIfEditsMade(state: EditLogUiState): Boolean {
        val initial = initialUiState ?: return false
        return state.bill != initial.bill ||
                state.tipAmount != initial.tipAmount ||
                state.partySize != initial.partySize ||
                state.billType != initial.billType ||
                state.restaurantName != initial.restaurantName ||
                state.selectedPlace?.placeId != initial.selectedPlace?.placeId ||
                state.review != initial.review ||
                state.date != initial.date ||
                state.rating != initial.rating
    }

    private companion object {
        const val COORDINATES_WAIT_MS = 3_000L
    }
}