package com.example.tiptracker.ui.features.logsaved

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tiptracker.data.helper.ImageStorageHelper
import com.example.tiptracker.utils.ObserveAsEvents
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LogSavedRoot(
    logId: Int,
    snackbarHostState: SnackbarHostState,
    onDone: () -> Unit,
    viewModel: LogSavedViewModel = koinViewModel(parameters = { parametersOf(logId) })
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageItems = remember(uiState.images, context) {
        uiState.images.map { image ->
            val imageFile = ImageStorageHelper.resolveStoredImageFile(context.filesDir, image.filePath)
            LogImageItemUi(
                image = image,
                imageFile = imageFile,
                displayName = imageFile.name
            )
        }
    }
    val pickMultipleImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = uiState.remainingSlots.coerceAtLeast(2)
        )
    ) { uris ->
        viewModel.onAction(LogSavedAction.AddImages(uris))
    }
    val pickSingleImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onAction(
            LogSavedAction.AddImages(
                uri?.let(::listOf).orEmpty()
            )
        )
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is LogSavedEvent.ShowMessage -> {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold { innerPadding ->
        LogSavedPage(
            uiState = uiState,
            imageItems = imageItems,
            onAddImagesClick = {
                when {
                    uiState.remainingSlots > 1 -> {
                        pickMultipleImagesLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }

                    uiState.remainingSlots == 1 -> {
                        pickSingleImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                }
            },
            onAction = viewModel::onAction,
            onDone = onDone,
            modifier = Modifier.padding(innerPadding)
        )
    }
}