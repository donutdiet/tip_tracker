package com.example.tiptracker.ui.features.logimagemanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import sh.calvin.reorderable.rememberReorderableLazyListState
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
    val lazyListState = rememberLazyListState()
    val isBusy = uiState.isAddingImages || uiState.isDeletingImage || uiState.isUpdatingImageOrder
    var localImageItems by remember { mutableStateOf(imageItems) }
    var isDragging by remember { mutableStateOf(false) }
    var hasPendingReorder by remember { mutableStateOf(false) }
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localImageItems = localImageItems.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        hasPendingReorder = true
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
        if (localImageItems.isEmpty()) {
            EmptyImagesState(
                modifier = Modifier.weight(1f)
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Drag",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    painter = painterResource(R.drawable.reorder),
                    contentDescription = "Reorder icon",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "to reorder pictures",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = localImageItems, key = { item -> item.image.id }) { item ->
                    ReorderableItem(reorderableLazyListState, key = item.image.id) { isItemDragging ->
                        LogImageListItem(
                            item = item,
                            index = localImageItems.indexOfFirst { it.image.id == item.image.id },
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            OutlinedButton(
                onClick = onAddImagesClick,
                enabled = uiState.canAddMore && !isBusy,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isAddingImages) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = "Add pictures (${uiState.addedCount}/${uiState.maxImages})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
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
private fun EmptyImagesState(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(24.dp)
    ) {
        Text(
            text = "No pictures added yet.\nUse the button below to add up to 10 pictures to this log.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReorderableCollectionItemScope.LogImageListItem(
    item: LogImageItemUi,
    index: Int,
    isDragged: Boolean,
    isBusy: Boolean,
    onRemove: () -> Unit,
    onDragStarted: () -> Unit,
    onDragStopped: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDragged) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = {},
                enabled = !isBusy,
                modifier = if (isBusy) {
                    Modifier
                } else {
                    Modifier.draggableHandle(
                        onDragStarted = { onDragStarted() },
                        onDragStopped = { onDragStopped() }
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.reorder),
                    contentDescription = "Drag to reorder image"
                )
            }

            ImageThumbnail(
                imageFile = item.imageFile,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Picture #${index + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onRemove,
                enabled = !isBusy
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = "Remove image",
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (imageFile == null) {
            Text(
                text = "No Preview",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
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
                        text = "No preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
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