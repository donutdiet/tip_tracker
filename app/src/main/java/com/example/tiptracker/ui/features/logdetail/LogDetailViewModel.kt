package com.example.tiptracker.ui.features.logdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.LogImage
import com.example.tiptracker.data.entity.totalPerPerson
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.data.repository.LogImageRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlaceDetails
import com.example.tiptracker.data.repository.PlacesRepository
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogDetailUiState(
    val bill: Double = 0.0,
    val tipPercent: Double = 0.0,
    val total: Double = 0.0,
    val partySize: Int = 1,
    val billType: BillType = BillType.SPLIT,
    val restaurantName: String = "",
    val address: String? = null,
    // True when [address] was looked up from Google Places rather than typed by the user;
    // it then needs Google Maps attribution and is never saved
    val addressFromPlaces: Boolean = false,
    val addressAttributions: List<String> = emptyList(),
    val review: String = "",
    val rating: Double = 0.0,
    val date: String = "",
    val tipAmount: Double = 0.0,
    val totalPerPerson: Double = 0.0,
    val images: List<LogImage> = emptyList(),
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false
)

sealed interface LogDetailAction {
    data object Delete : LogDetailAction
}

sealed interface LogDetailEvent {
    data object LogDeleted : LogDetailEvent
    data class ShowError(val message: String) : LogDetailEvent
}

class LogDetailViewModel(
    private val logId: Int,
    private val logsRepository: LogRepository,
    private val imagesRepository: LogImageRepository,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _events = Channel<LogDetailEvent>()
    val events = _events.receiveAsFlow()

    private val isDeleting = MutableStateFlow(false)
    private val logFlow = logsRepository.getLogById(logId)

    // Logs linked to a Google Maps place store only its ID (Google's terms don't allow
    // saving the address), so look the address up each time the screen is shown. Address
    // only, so it bills at Place Details Essentials. Null while loading, offline, or unlinked.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val placeAddress: Flow<PlaceDetails?> = logFlow
        .map { it?.placeId }
        .distinctUntilChanged()
        .mapLatest { placeId ->
            placeId?.let { placesRepository.fetchPlaceDetails(it, includeName = false) }
        }
        .onStart { emit(null) }
        .catch { emit(null) }

    val uiState: StateFlow<LogDetailUiState> = combine(
        isDeleting,
        imagesRepository.getImages(logId),
        logFlow
            .map { log ->
                if (log == null) {
                    LogDetailUiState(
                        isLoading = false,
                        isNotFound = true
                    )
                } else {
                    val tipAmount = (log.total - log.bill).roundToTwoDecimals()
                    LogDetailUiState(
                        isLoading = false,
                        bill = log.bill,
                        tipPercent = log.tipPercent,
                        total = log.total,
                        partySize = log.partySize,
                        billType = log.billType,
                        restaurantName = log.restaurantName,
                        address = log.address,
                        review = log.review,
                        rating = log.rating,
                        date = log.date,
                        tipAmount = tipAmount,
                        totalPerPerson = log.totalPerPerson.roundToTwoDecimals()
                    )
                }
            }
            .catch { e ->
                emit(
                    LogDetailUiState(
                        isLoading = false,
                        errorMessage = "Couldn't load log details. Please try again."
                    )
                )
                android.util.Log.e("LogDetailViewModel", "Error loading log detail", e)
            },
        placeAddress
    ) { deleting, images, state, place ->
        // A typed address always wins; the Places lookup only fills in a missing one
        val fetched = place?.takeIf { state.address == null && it.address != null }
        state.copy(
            isDeleting = deleting,
            images = images,
            address = fetched?.address ?: state.address,
            addressFromPlaces = fetched != null,
            addressAttributions = fetched?.attributions.orEmpty()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LogDetailUiState()
        )

    fun onAction(action: LogDetailAction) {
        when (action) {
            LogDetailAction.Delete -> deleteLog()
        }
    }

    private fun deleteLog() {
        if (isDeleting.value) return
        isDeleting.update { true }

        viewModelScope.launch {
            try {
                val deletedCount = logsRepository.deleteLogById(logId)
                if (deletedCount == 0) {
                    _events.send(LogDetailEvent.ShowError("This log no longer exists."))
                } else {
                    _events.send(LogDetailEvent.LogDeleted)
                }
            } catch (e: Exception) {
                _events.send(LogDetailEvent.ShowError("Couldn't delete your log. Please try again."))
                android.util.Log.e("LogDetailViewModel", "Error deleting log $logId", e)
            } finally {
                isDeleting.update { false }
            }
        }
    }
}