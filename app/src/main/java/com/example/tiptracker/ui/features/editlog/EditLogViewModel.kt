package com.example.tiptracker.ui.features.editlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlacesRepository
import com.example.tiptracker.ui.placesearch.PlaceSearchDelegate
import com.example.tiptracker.ui.placesearch.SelectedPlace
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class EditLogUiState(
    val bill: String = "",
    val tipAmount: String = "",
    val partySize: String = "",
    // Exactly what the user typed; this is the name that gets saved
    val restaurantName: String = "",
    // Free-text address typed in an older version of the app (never a Google address)
    val address: String = "",
    // The linked Google Maps place, if any. Only its ID is saved; the rest is looked up.
    val selectedPlace: SelectedPlace? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val predictions: List<PlacePrediction> = emptyList(),
    val searchFailed: Boolean = false,
    val isOffline: Boolean = false,
    val review: String = "",
    val date: String = "",
    val rating: Double = 5.0,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
    val isUpdating: Boolean = false,
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
}

sealed interface EditLogAction {
    data class BillChanged(val bill: String) : EditLogAction
    data class TipAmountChanged(val tipAmount: String) : EditLogAction
    data class PartySizeChanged(val partySize: String) : EditLogAction
    data class RestaurantQueryChanged(val query: String) : EditLogAction
    data class PlaceSelected(val prediction: PlacePrediction) : EditLogAction
    data object ClearPlace : EditLogAction
    data class ReviewChanged(val review: String) : EditLogAction
    data class DateChanged(val date: String) : EditLogAction
    data class RatingChanged(val rating: Double) : EditLogAction
    data object Update : EditLogAction
}

sealed interface EditLogEvent {
    data object LogUpdated : EditLogEvent
    data class ShowError(val message: String) : EditLogEvent
}

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
        connectionStatus = connectivityRepository.status
    )

    // In-flight lookup of the selected place's coordinates
    private var coordinatesJob: Job? = null

    private val _uiState = MutableStateFlow(EditLogUiState())
    val uiState: StateFlow<EditLogUiState> = combine(
        _uiState,
        placeSearch.state,
        connectivityRepository.status
    ) { state, search, connection ->
        state.copy(
            predictions = search.predictions,
            searchFailed = search.searchFailed,
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
            is EditLogAction.BillChanged -> edit { it.copy(bill = action.bill) }
            is EditLogAction.TipAmountChanged -> edit { it.copy(tipAmount = action.tipAmount) }
            is EditLogAction.PartySizeChanged -> edit { it.copy(partySize = action.partySize) }
            is EditLogAction.RestaurantQueryChanged -> onRestaurantQueryChange(action.query)
            is EditLogAction.PlaceSelected -> onPlaceSelected(action.prediction)
            EditLogAction.ClearPlace -> clearPlace()
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

    // Typing never unlinks a picked place (that's what Clear is for), and there's nothing to
    // search for while one is linked
    private fun onRestaurantQueryChange(query: String) {
        val placeLinked = _uiState.value.selectedPlace != null
        edit { it.copy(restaurantName = query) }
        if (!placeLinked) placeSearch.onQueryChange(query)
    }

    // Links the place without touching the typed name: only what the user typed is saved
    private fun onPlaceSelected(prediction: PlacePrediction) {
        coordinatesJob?.cancel()
        edit {
            it.copy(
                selectedPlace = SelectedPlace.from(prediction),
                latitude = null,
                longitude = null
            )
        }
        coordinatesJob = viewModelScope.launch {
            val coordinates = placeSearch.select(prediction)
            _uiState.update {
                // Ignore if the selection changed while we were waiting
                if (it.selectedPlace?.placeId == prediction.placeId) {
                    it.copy(latitude = coordinates?.latitude, longitude = coordinates?.longitude)
                } else it
            }
        }
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
                    restaurantName = log.restaurantName,
                    address = log.address ?: "",
                    selectedPlace = log.placeId?.let { SelectedPlace(it) },
                    latitude = log.latitude,
                    longitude = log.longitude,
                    review = log.review,
                    date = log.date,
                    rating = log.rating,
                    errorMessage = null,
                    editsMade = false
                )

                initialUiState = loadedState
                _uiState.value = loadedState
                log.placeId?.let(::loadPlaceDetails)

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

    private fun clearPlace() {
        coordinatesJob?.cancel()
        edit { it.copy(selectedPlace = null, latitude = null, longitude = null) }
        // Search the current text again so suggestions come back without retyping
        placeSearch.onQueryChange(_uiState.value.restaurantName)
    }

    // Only the place ID is stored (Google's terms), so look up the official name and address
    // each time the screen opens. Includes the name, so this bills at Place Details Pro.
    private fun loadPlaceDetails(placeId: String) {
        viewModelScope.launch {
            val details = placesRepository.fetchPlaceDetails(placeId, includeName = true)
            _uiState.update {
                // Skip if the user cleared or changed the place meanwhile
                if (it.selectedPlace?.placeId == placeId) {
                    it.copy(selectedPlace = SelectedPlace.from(placeId, details))
                } else it
            }
        }
    }

    private fun updateLog() {
        if (_uiState.value.isUpdating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }
            try {
                // Give an in-flight coordinates lookup a moment; save without them if it's slow
                withTimeoutOrNull(COORDINATES_WAIT_MS) { coordinatesJob?.join() }
                val state = _uiState.value
                val bill = state.bill.toDoubleOrNull() ?: 0.0
                val tipAmount = state.tipAmount.toDoubleOrNull() ?: 0.0
                val partySize = state.partySize.toIntOrNull() ?: 1
                // Only the user's own typed address (from older versions) is kept; a place's
                // address is Google content and never saved
                val address = state.address.ifEmpty { null }

                if (bill < 0.0 || tipAmount < 0.0 || partySize < 1) {
                    _events.send(EditLogEvent.ShowError("Please enter a valid bill, tip amount, and party size."))
                    return@launch
                }

                logsRepository.updateLog(
                    Log(
                        id = logId,
                        bill = bill,
                        tipPercent = state.tipPercent,
                        total = state.total,
                        partySize = partySize,
                        restaurantName = state.restaurantName,
                        address = address,
                        review = state.review,
                        rating = state.rating,
                        date = state.date,
                        placeId = state.selectedPlace?.placeId,
                        latitude = state.latitude,
                        longitude = state.longitude
                    )
                )
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
                state.restaurantName != initial.restaurantName ||
                state.address != initial.address ||
                state.selectedPlace?.placeId != initial.selectedPlace?.placeId ||
                state.review != initial.review ||
                state.date != initial.date ||
                state.rating != initial.rating
    }

    private companion object {
        const val COORDINATES_WAIT_MS = 3_000L
    }
}