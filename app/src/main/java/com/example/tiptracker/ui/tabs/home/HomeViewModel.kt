package com.example.tiptracker.ui.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.data.model.RestaurantHistory
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlacesRepository
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.placesearch.PlaceSearchDelegate
import com.example.tiptracker.ui.placesearch.SelectedPlace
import com.example.tiptracker.utils.collapseWhitespace
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatTipPercent
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.ceil
import kotlin.text.toDoubleOrNull

data class HomeUiState(
    val billAmount: String = "",
    val tipPreset1: Boolean = false,
    val tipPreset2: Boolean = false,
    val tipPercent: String = "",
    val partySize: String = "",
    val billType: BillType = BillType.SPLIT,
    val roundUpTip: Boolean = false,
    val roundUpTotal: Boolean = false,
    val restaurantName: String = "",
    val review: String = "",
    val date: String = LocalDate.now().toString(),
    val rating: Double = 5.0,
    val predictions: List<PlacePrediction> = emptyList(),
    val history: List<RestaurantHistory> = emptyList(),
    val visitedPlaces: Map<String, RestaurantHistory> = emptyMap(),
    val selectedPlace: SelectedPlace? = null,
    val searchFailed: Boolean = false,
    val isOffline: Boolean = false,
    val isSaving: Boolean = false,
    val tipPreset1Percent: Int = 10,
    val tipPreset2Percent: Int = 15,

    // Not used rn
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    // Text displayed in the custom tip text field
    val displayedTipPercent: String
        get() {
            return if (tipPreset1) {
                ""
            } else if (tipPreset2) {
                ""
            } else {
                tipPercent
            }
        }

    val tipAmount: Double
        get() {
            val bill = billAmount.toDoubleOrNull() ?: 0.0
            val tipPercent = tipPercent.toDoubleOrNull() ?: 0.0
            val rawTip = bill * (tipPercent / 100.0)

            return when {
                roundUpTip -> ceil(rawTip)
                roundUpTotal -> {
                    val rawTotal = bill + rawTip
                    val roundedTotal = ceil(rawTotal)
                    roundedTotal - bill
                }

                else -> rawTip
            }.roundToTwoDecimals()
        }

    val total: Double
        get() {
            val bill = billAmount.toDoubleOrNull() ?: 0.0
            return (bill + tipAmount).roundToTwoDecimals()
        }

    val trueTipPercent: Double
        get() {
            if (tipAmount == 0.0) return 0.0

            val percent = tipPercent.toDoubleOrNull() ?: 0.0
            if (!roundUpTip && !roundUpTotal) return percent.roundToTwoDecimals()

            return (100 * tipAmount / (total - tipAmount)).roundToTwoDecimals()
        }

    // Split vs separate only matters with more than one person
    val billTypeApplies: Boolean
        get() = (partySize.toIntOrNull() ?: 0) > 1

    val formattedTipAmount: String
        get() = formatCurrency(tipAmount)

    val formattedTotal: String
        get() = formatCurrency(total)

    val formattedTotalPerPerson: String
        get() {
            val party = partySize.toDoubleOrNull() ?: 0.0
            val perPerson = (total / party).roundToTwoDecimals()
            return formatCurrency(perPerson)
        }

    val formattedTrueTipPercent: String
        get() = formatTipPercent(trueTipPercent)
}

sealed interface HomeAction {
    data class onBillAmountChange(val billAmount: String) : HomeAction
    data object onTipPreset1Toggle : HomeAction
    data object onTipPreset2Toggle : HomeAction
    data class onTipPercentChange(val tipPercent: String) : HomeAction
    data class onPartySizeChange(val partySize: String) : HomeAction
    data class onBillTypeChange(val billType: BillType) : HomeAction
    data object onRoundUpTipToggle : HomeAction
    data object onRoundUpTotalToggle : HomeAction
    data class onRestaurantQueryChange(val query: String) : HomeAction
    data class onPlaceSelected(val prediction: PlacePrediction) : HomeAction
    data class onHistorySelected(val history: RestaurantHistory) : HomeAction
    data object onClearPlace : HomeAction
    data object onRestaurantFieldFocus : HomeAction
    data object onShowPlaceAddress : HomeAction
    data class onReviewChange(val review: String) : HomeAction
    data class onDateChange(val date: String) : HomeAction
    data class onRatingChange(val rating: Double) : HomeAction
    data object saveLog : HomeAction
    data object clear : HomeAction
}

sealed interface HomeEvent {
    data class LogSaved(val logId: Int) : HomeEvent
    data class ShowError(val message: String) : HomeEvent
}

class HomeViewModel(
    private val logsRepository: LogRepository,
    settingsRepository: SettingsRepository,
    private val placesRepository: PlacesRepository,
    locationProvider: LocationProvider,
    connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val placeSearch = PlaceSearchDelegate(
        scope = viewModelScope,
        places = placesRepository,
        location = locationProvider,
        connectionStatus = connectivityRepository.status,
        history = logsRepository.getRestaurantHistory()
    )

    // In-flight lookup of the selected place's coordinates
    private var coordinatesJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = combine(
        _uiState,
        settingsRepository.tipPreset1Percent,
        settingsRepository.tipPreset2Percent,
        placeSearch.state,
        connectivityRepository.status
    ) { state, p1, p2, search, connection ->
        state.copy(
            tipPreset1Percent = p1,
            tipPreset2Percent = p2,
            predictions = search.predictions,
            searchFailed = search.searchFailed,
            history = search.history,
            visitedPlaces = search.visitedPlaces,
            isOffline = connection == ConnectionStatus.Offline
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private val _events = Channel<HomeEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.onBillAmountChange -> _uiState.update { it.copy(billAmount = action.billAmount) }
            is HomeAction.onTipPreset1Toggle -> {
                val preset1Percent = uiState.value.tipPreset1Percent
                _uiState.update {
                    val newSwitchState = !it.tipPreset1
                    it.copy(
                        tipPreset1 = newSwitchState,
                        tipPreset2 = false,
                        tipPercent = if (newSwitchState) preset1Percent.toString() else ""
                    )
                }
            }

            is HomeAction.onTipPreset2Toggle -> {
                val preset2Percent = uiState.value.tipPreset2Percent
                _uiState.update {
                    val newSwitchState = !it.tipPreset2
                    it.copy(
                        tipPreset2 = newSwitchState,
                        tipPreset1 = false,
                        tipPercent = if (newSwitchState) preset2Percent.toString() else ""
                    )
                }
            }

            is HomeAction.onTipPercentChange -> {
                _uiState.update {
                    it.copy(
                        tipPercent = action.tipPercent,
                        tipPreset1 = false,
                        tipPreset2 = false
                    )
                }
            }

            is HomeAction.onPartySizeChange -> _uiState.update { it.copy(partySize = action.partySize) }
            is HomeAction.onBillTypeChange -> _uiState.update { it.copy(billType = action.billType) }
            is HomeAction.onRoundUpTipToggle -> {
                _uiState.update {
                    it.copy(roundUpTip = !it.roundUpTip, roundUpTotal = false)
                }
            }

            is HomeAction.onRoundUpTotalToggle -> {
                _uiState.update {
                    it.copy(roundUpTotal = !it.roundUpTotal, roundUpTip = false)
                }
            }

            is HomeAction.onRestaurantQueryChange -> onRestaurantQueryChange(action.query)
            is HomeAction.onPlaceSelected -> onPlaceSelected(action.prediction)
            is HomeAction.onHistorySelected -> onHistorySelected(action.history)
            is HomeAction.onShowPlaceAddress -> showPlaceAddress()
            is HomeAction.onClearPlace -> clearPlace()
            is HomeAction.onRestaurantFieldFocus -> {
                val state = _uiState.value
                if (state.selectedPlace == null) placeSearch.onFocus(state.restaurantName)
            }
            is HomeAction.onReviewChange -> _uiState.update { it.copy(review = action.review) }
            is HomeAction.onDateChange -> _uiState.update { it.copy(date = action.date) }
            is HomeAction.onRatingChange -> _uiState.update { it.copy(rating = action.rating) }
            is HomeAction.saveLog -> saveLog()
            is HomeAction.clear -> clearState()
        }
    }

    // Typing never unlinks a picked place (that's what Unlink is for), and there's nothing to
    // search for while one is linked
    private fun onRestaurantQueryChange(query: String) {
        val placeLinked = _uiState.value.selectedPlace != null
        _uiState.update { it.copy(restaurantName = query) }
        if (!placeLinked) placeSearch.onQueryChange(query)
    }

    // Links the place and fills the name field with the place's name, which the user can still
    // edit before saving
    private fun onPlaceSelected(prediction: PlacePrediction) {
        coordinatesJob?.cancel()
        // The user's own name for a place they've logged before, otherwise Google's
        val name = placeSearch.nameFor(prediction)
        _uiState.update {
            it.copy(
                restaurantName = name,
                selectedPlace = SelectedPlace.from(prediction),
                latitude = null,
                longitude = null
            )
        }
        coordinatesJob = viewModelScope.launch {
            val coordinates = placeSearch.select(prediction, fieldText = name)
            _uiState.update {
                // Ignore if the selection changed while we were waiting
                if (it.selectedPlace?.placeId == prediction.placeId) {
                    it.copy(latitude = coordinates?.latitude, longitude = coordinates?.longitude)
                } else it
            }
        }
    }

    // Fills in the name used on the last visit, and links the place the past visits are linked
    // to, if any. No API call: the place ID is already saved.
    private fun onHistorySelected(history: RestaurantHistory) {
        coordinatesJob?.cancel()
        val name = history.name
        _uiState.update {
            it.copy(
                restaurantName = name,
                selectedPlace = history.placeId?.let { placeId -> SelectedPlace(placeId) },
                latitude = null,
                longitude = null
            )
        }
        placeSearch.onHistoryPicked(fieldText = name)
    }

    // A place linked from history has only its ID, so its address is looked up when the user
    // taps "Show address", as each lookup is billed (Place Details Essentials)
    private fun showPlaceAddress() {
        val place = _uiState.value.selectedPlace ?: return
        if (place.hasDetails || place.isLoading) return
        val placeId = place.placeId

        _uiState.update { it.copy(selectedPlace = place.copy(isLoading = true, lookupFailed = false)) }
        viewModelScope.launch {
            val details = placesRepository.fetchPlaceDetails(placeId)
            _uiState.update {
                // Skip if the user unlinked or changed the place meanwhile
                if (it.selectedPlace?.placeId == placeId) {
                    it.copy(selectedPlace = SelectedPlace.from(placeId, details))
                } else it
            }
        }
    }

    private fun clearPlace() {
        coordinatesJob?.cancel()
        _uiState.update { it.copy(selectedPlace = null, latitude = null, longitude = null) }
        // Brings suggestions back: the ones picked from if the name is unchanged (free),
        // otherwise a new search
        placeSearch.onPlaceCleared(_uiState.value.restaurantName)
    }

    private fun saveLog() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Give an in-flight coordinates lookup a moment; save without them if it's slow
                withTimeoutOrNull(COORDINATES_WAIT_MS) { coordinatesJob?.join() }
                val state = _uiState.value
                val bill = state.billAmount.toDoubleOrNull() ?: 0.0
                val tipPercent = state.tipPercent.toDoubleOrNull() ?: 0.0
                val partySize = state.partySize.toIntOrNull() ?: 1

                if (bill < 0.0 || tipPercent < 0.0 || partySize < 1) {
                    _events.send(HomeEvent.ShowError("Please enter a valid bill, tip percent, and party size."))
                    return@launch
                }

                val log = Log(
                    bill = bill,
                    tipPercent = state.trueTipPercent,
                    total = state.total,
                    partySize = partySize,
                    // The toggle is hidden for a party of one, so don't keep a stale choice
                    billType = if (partySize > 1) state.billType else BillType.SPLIT,
                    // Whatever is in the name field (filled from a picked place, possibly edited);
                    // the place's address is never saved
                    restaurantName = state.restaurantName.collapseWhitespace(),
                    address = null,
                    review = state.review,
                    rating = state.rating,
                    date = state.date,
                    placeId = state.selectedPlace?.placeId,
                    latitude = state.latitude,
                    longitude = state.longitude
                )
                val id = logsRepository.insertLog(log)
                clearState()
                _events.send(HomeEvent.LogSaved(id))
            } catch (e: Exception) {
                val userMessage = when (e) {
                    is android.database.sqlite.SQLiteFullException -> "Storage is full. Please free some space."
                    else -> "Couldn't save your log. Please try again."
                }
                _events.send(HomeEvent.ShowError(userMessage))
                android.util.Log.e("HomeViewModel", "Error saving log", e)
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun clearState() {
        coordinatesJob?.cancel()
        placeSearch.reset()
        _uiState.value = HomeUiState()
    }

    private companion object {
        const val COORDINATES_WAIT_MS = 3_000L
    }
}
