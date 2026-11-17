package com.example.tiptracker.ui.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.helper.LocationProvider
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlacePrediction
import com.example.tiptracker.data.repository.PlacesRepository
import com.example.tiptracker.data.repository.SettingsRepository
import com.example.tiptracker.ui.placesearch.PlaceSearchDelegate
import com.example.tiptracker.ui.placesearch.SelectedPlace
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
    val roundUpTip: Boolean = false,
    val roundUpTotal: Boolean = false,
    // Exactly what the user typed; this is the name that gets saved
    val restaurantName: String = "",
    // The Google Maps place picked from the suggestions, if any. Only its ID is saved.
    val selectedPlace: SelectedPlace? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val predictions: List<PlacePrediction> = emptyList(),
    val searchFailed: Boolean = false,
    val isOffline: Boolean = false,
    val review: String = "",
    val date: String = LocalDate.now().toString(),
    val rating: Double = 5.0,
    val isSaving: Boolean = false,
    val tipPreset1Percent: Int = 10,
    val tipPreset2Percent: Int = 15,
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
    data object onRoundUpTipToggle : HomeAction
    data object onRoundUpTotalToggle : HomeAction
    data class onRestaurantQueryChange(val query: String) : HomeAction
    data class onPlaceSelected(val prediction: PlacePrediction) : HomeAction
    data object onClearPlace : HomeAction
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
    placesRepository: PlacesRepository,
    locationProvider: LocationProvider,
    connectivityRepository: ConnectivityRepository
) : ViewModel() {

    private val placeSearch = PlaceSearchDelegate(
        scope = viewModelScope,
        places = placesRepository,
        location = locationProvider,
        connectionStatus = connectivityRepository.status
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
            is HomeAction.onClearPlace -> clearPlace()
            is HomeAction.onReviewChange -> _uiState.update { it.copy(review = action.review) }
            is HomeAction.onDateChange -> _uiState.update { it.copy(date = action.date) }
            is HomeAction.onRatingChange -> _uiState.update { it.copy(rating = action.rating) }
            is HomeAction.saveLog -> saveLog()
            is HomeAction.clear -> clearState()
        }
    }

    // Typing never unlinks a picked place (that's what Clear is for), and there's nothing to
    // search for while one is linked
    private fun onRestaurantQueryChange(query: String) {
        val placeLinked = _uiState.value.selectedPlace != null
        _uiState.update { it.copy(restaurantName = query) }
        if (!placeLinked) placeSearch.onQueryChange(query)
    }

    // Links the place without touching the typed name: only what the user typed is saved
    private fun onPlaceSelected(prediction: PlacePrediction) {
        coordinatesJob?.cancel()
        _uiState.update {
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

    private fun clearPlace() {
        coordinatesJob?.cancel()
        _uiState.update { it.copy(selectedPlace = null, latitude = null, longitude = null) }
        // Search the current text again so suggestions come back without retyping
        placeSearch.onQueryChange(_uiState.value.restaurantName)
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
                    // Google's terms allow storing only the place ID: the name is the user's own
                    // text, and the place's address is never saved
                    restaurantName = state.restaurantName,
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
