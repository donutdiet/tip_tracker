package com.example.tiptracker.ui.features.logsaved

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.LogImage
import com.example.tiptracker.data.repository.LogImageRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LogSavedUiState(
    val images: List<LogImage> = emptyList(),
    val isAddingImages: Boolean = false,
    val isUpdatingImageOrder: Boolean = false,
    val isDeletingImage: Boolean = false,
    val maxImages: Int = LogImageRepository.MAX_IMAGES_PER_LOG
) {
    val addedCount: Int
        get() = images.size

    val remainingSlots: Int
        get() = (maxImages - addedCount).coerceAtLeast(0)

    val canAddMore: Boolean
        get() = remainingSlots > 0
}

sealed interface LogSavedAction {
    data class AddImages(val uris: List<Uri>) : LogSavedAction
    data class ReorderImages(val images: List<LogImage>) : LogSavedAction
    data class RemoveImage(val image: LogImage) : LogSavedAction
}

sealed interface LogSavedEvent {
    data class ShowMessage(val message: String) : LogSavedEvent
}

class LogSavedViewModel(
    private val logId: Int,
    private val repository: LogImageRepository
) : ViewModel() {
    private val isAddingImages = MutableStateFlow(false)
    private val isUpdatingImageOrder = MutableStateFlow(false)
    private val isDeletingImage = MutableStateFlow(false)

    private val _events = Channel<LogSavedEvent>()
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<LogSavedUiState> = combine(
        repository.getImages(logId),
        isAddingImages,
        isUpdatingImageOrder,
        isDeletingImage
    ) { images, addingImages, updatingImageOrder, deletingImage ->
        LogSavedUiState(
            images = images,
            isAddingImages = addingImages,
            isUpdatingImageOrder = updatingImageOrder,
            isDeletingImage = deletingImage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogSavedUiState()
    )

    fun onAction(action: LogSavedAction) {
        when (action) {
            is LogSavedAction.AddImages -> addImages(action.uris)
            is LogSavedAction.ReorderImages -> reorderImages(action.images)
            is LogSavedAction.RemoveImage -> removeImage(action.image)
        }
    }

    private fun addImages(uris: List<Uri>) {
        if (uris.isEmpty() || isAddingImages.value) return

        viewModelScope.launch {
            if (!uiState.value.canAddMore) {
                _events.trySend(
                    LogSavedEvent.ShowMessage(
                        "You can only attach up to ${uiState.value.maxImages} images."
                    )
                )
                return@launch
            }

            isAddingImages.value = true
            try {
                repository.addImages(logId, uris).getOrThrow()
            } catch (e: Exception) {
                _events.trySend(LogSavedEvent.ShowMessage("Couldn't add images. Please try again."))
                android.util.Log.e("LogSavedViewModel", "Error adding images for log $logId", e)
            } finally {
                isAddingImages.value = false
            }
        }
    }

    private fun reorderImages(images: List<LogImage>) {
        val currentImages = uiState.value.images
        if (isUpdatingImageOrder.value || images.isEmpty()) return
        if (images.map { it.id } == currentImages.map { it.id }) return

        viewModelScope.launch {
            isUpdatingImageOrder.value = true
            try {
                repository.reorderImages(images)
            } catch (e: Exception) {
                _events.trySend(LogSavedEvent.ShowMessage("Couldn't reorder images. Please try again."))
                android.util.Log.e("LogSavedViewModel", "Error reordering images for log $logId", e)
            } finally {
                isUpdatingImageOrder.value = false
            }
        }
    }

    private fun removeImage(image: LogImage) {
        if (isDeletingImage.value) return

        viewModelScope.launch {
            isDeletingImage.value = true
            try {
                repository.deleteImage(logId, image.id, image.filePath)
                _events.trySend(LogSavedEvent.ShowMessage("Image removed."))
            } catch (e: Exception) {
                _events.trySend(LogSavedEvent.ShowMessage("Couldn't remove image. Please try again."))
                android.util.Log.e("LogSavedViewModel", "Error deleting image ${image.id}", e)
            } finally {
                isDeletingImage.value = false
            }
        }
    }
}