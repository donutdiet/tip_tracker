package com.example.tiptracker.ui.features.logimagemanager

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.tiptracker.R
import com.example.tiptracker.data.entity.LogImage
import com.example.tiptracker.ui.theme.TipTrackerTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import java.io.File

data class LogImageItemUi(
    val image: LogImage,
    val imageFile: File?,
    val displayName: String
)

@Composable
fun LogImageManagerPage(
    uiState: LogImageManagerUiState,
    imageItems: List<LogImageItemUi>,
    onAddImagesClick: () -> Unit,
    onAction: (LogImageManagerAction) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyGridState = rememberLazyGridState()
    val isBusy = uiState.isAddingImages || uiState.isDeletingImage || uiState.isUpdatingImageOrder
    var localImageItems by remember { mutableStateOf(imageItems) }
    var isDragging by remember { mutableStateOf(false) }
    var hasPendingReorder by remember { mutableStateOf(false) }

    val reorderableLazyGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        val fromIndex = from.index
        val toIndex = to.index.coerceIn(0, localImageItems.lastIndex)
        if (fromIndex in localImageItems.indices && fromIndex != toIndex) {
            localImageItems = localImageItems.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            hasPendingReorder = true
        }
    }

    LaunchedEffect(imageItems) {
        if (!isDragging) {
            localImageItems = imageItems
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Hold and drag to reorder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${uiState.addedCount}/${uiState.maxImages}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = lazyGridState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items = localImageItems, key = { item -> item.image.id }) { item ->
                ReorderableItem(reorderableLazyGridState, key = item.image.id) { isItemDragging ->
                    LogImageGridItem(
                        item = item,
                        isDragged = isItemDragging,
                        isBusy = isBusy,
                        onRemove = { onAction(LogImageManagerAction.RemoveImage(item.image)) },
                        onDragStarted = {
                            isDragging = true
                            hasPendingReorder = false
                        },
                        onDragStopped = {
                            isDragging = false
                            if (hasPendingReorder) {
                                onAction(
                                    LogImageManagerAction.ReorderImages(
                                        localImageItems.map { it.image }
                                    )
                                )
                            }
                            hasPendingReorder = false
                        }
                    )
                }
            }

            if (uiState.canAddMore) {
                item(key = "add_photos_button") {
                    AddPhotosBox(
                        onClick = onAddImagesClick,
                        enabled = !isBusy,
                        isAdding = uiState.isAddingImages
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onDone,
                enabled = !isBusy,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.LogImageGridItem(
    item: LogImageItemUi,
    isDragged: Boolean,
    isBusy: Boolean,
    onRemove: () -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragged) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "drag_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (isBusy) {
                    Modifier
                } else {
                    Modifier.longPressDraggableHandle(
                        onDragStarted = { onDragStarted() },
                        onDragStopped = { onDragStopped() }
                    )
                }
            )
    ) {
        ImageThumbnail(
            imageFile = item.imageFile,
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onRemove,
            enabled = !isBusy,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Remove image",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AddPhotosBox(
    onClick: () -> Unit,
    enabled: Boolean,
    isAdding: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                border = BorderStroke(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        if (isAdding) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add photos",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add photos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    imageFile: File?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        if (imageFile == null) {
            Text(
                text = "No Preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        } else {
            SubcomposeAsyncImage(
                model = imageFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                },
                error = {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogImageManagerPreview() {
    TipTrackerTheme {
        LogImageManagerPage(
            uiState = LogImageManagerUiState(
                images = listOf(
                    LogImage(id = 1, logId = 1, filePath = "log_images/1/img_1.jpg", order = 0),
                    LogImage(id = 2, logId = 1, filePath = "log_images/1/img_2.jpg", order = 1),
                    LogImage(id = 3, logId = 1, filePath = "log_images/1/img_3.jpg", order = 2)
                )
            ),
            imageItems = listOf(
                LogImageItemUi(
                    image = LogImage(
                        id = 1,
                        logId = 1,
                        filePath = "log_images/1/img_1.jpg",
                        order = 0
                    ),
                    imageFile = null,
                    displayName = "img_1.jpg"
                ),
                LogImageItemUi(
                    image = LogImage(
                        id = 2,
                        logId = 1,
                        filePath = "log_images/1/img_2.jpg",
                        order = 1
                    ),
                    imageFile = null,
                    displayName = "img_2.jpg"
                ),
                LogImageItemUi(
                    image = LogImage(
                        id = 3,
                        logId = 1,
                        filePath = "log_images/1/img_3.jpg",
                        order = 2
                    ),
                    imageFile = null,
                    displayName = "img_3.jpg"
                )
            ),
            onAddImagesClick = {},
            onAction = {},
            onDone = {}
        )
    }
}
