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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    // Set when the log is linked to a Google Maps place. Only the ID is stored (Google's terms),
    // so the address is looked up when the user asks for it, since each lookup is billed.
    val placeId: String? = null,
    // The looked-up address: shown with Google's attributions, never saved
    val placeAddress: PlaceDetails? = null,
    val isLoadingAddress: Boolean = false,
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
    data object ShowPlaceAddress : LogDetailAction
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

    // Kept for the life of this screen once looked up, keyed by place so that linking a
    // different place (via Edit) hides it again
    private val placeAddress = MutableStateFlow<Pair<String, PlaceDetails>?>(null)
    private val isLoadingAddress = MutableStateFlow(false)
    private val logFlow = logsRepository.getLogById(logId)

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
                        placeId = log.placeId,
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
        placeAddress,
        isLoadingAddress
    ) { deleting, images, state, address, loadingAddress ->
        state.copy(
            isDeleting = deleting,
            images = images,
            placeAddress = address?.takeIf { it.first == state.placeId }?.second,
            isLoadingAddress = loadingAddress
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
            LogDetailAction.ShowPlaceAddress -> showPlaceAddress()
        }
    }

    private fun showPlaceAddress() {
        val state = uiState.value
        val placeId = state.placeId ?: return
        if (state.placeAddress != null || isLoadingAddress.value) return

        viewModelScope.launch {
            isLoadingAddress.value = true
            val details = placesRepository.fetchPlaceDetails(placeId)
            isLoadingAddress.value = false
            if (details?.address != null) {
                placeAddress.value = placeId to details
            } else {
                _events.send(LogDetailEvent.ShowError("Couldn't load the address. Please try again."))
            }
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