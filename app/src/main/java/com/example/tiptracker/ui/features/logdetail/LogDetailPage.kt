package com.example.tiptracker.ui.features.logdetail

import com.example.tiptracker.ui.theme.card
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.example.tiptracker.R
import com.example.tiptracker.ui.components.PlaceAddress
import com.example.tiptracker.data.model.BillType
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.titleMediumMono
import com.example.tiptracker.ui.theme.titleSmallMono
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.formatTipPercent
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun LogDetailPage(
    uiState: LogDetailUiState,
    imageFiles: List<File?>,
    modifier: Modifier = Modifier,
    onShowPlaceAddress: () -> Unit = {}
) {
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = uiState.restaurantName,
            style = MaterialTheme.typography.headlineMedium
        )
        if (uiState.placeId != null) {
            Spacer(modifier = Modifier.height(4.dp))
            PlaceAddress(
                details = uiState.placeAddress,
                isLoading = uiState.isLoadingAddress,
                onShow = onShowPlaceAddress
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        LogStatsCard(
            billAmount = uiState.bill,
            tipAmount = uiState.tipAmount,
            total = uiState.total,
            date = uiState.date,
            partySize = uiState.partySize,
            billType = uiState.billType,
            totalPerPerson = uiState.totalPerPerson,
            tipPercent = uiState.tipPercent
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (imageFiles.isNotEmpty()) {
            LogDetailImageGallery(
                imageFiles = imageFiles,
                onImageClick = { index -> selectedImageIndex = index }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Review", style = MaterialTheme.typography.bodyLarge)

            Text(
                text = buildAnnotatedString {
                    append("%.1f".format(uiState.rating))
                    withStyle(
                        MaterialTheme.typography.bodySmall.toSpanStyle()
                            .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        append(" / 10")
                    }
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(text = uiState.review, style = MaterialTheme.typography.bodyMedium)
    }

    selectedImageIndex?.let { initialPage ->
        FullScreenImageDialog(
            imageFiles = imageFiles,
            initialPage = initialPage,
            onDismiss = { selectedImageIndex = null }
        )
    }
}

@Composable
private fun LogStatsCard(
    billAmount: Double,
    tipAmount: Double,
    total: Double,
    date: String,
    partySize: Int,
    billType: BillType,
    totalPerPerson: Double,
    tipPercent: Double
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.card)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatDateForDisplay(date),
                    style = MaterialTheme.typography.titleSmallMono
                )

                Row {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        // Separate checks: the amounts below are only the user's own check
                        text = if (billType == BillType.SEPARATE) "$partySize · Separate" else "$partySize",
                        style = MaterialTheme.typography.titleSmallMono,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Bill", style = MaterialTheme.typography.titleMediumMono)
                Text(
                    text = "$${formatCurrency(billAmount)}",
                    style = MaterialTheme.typography.titleMediumMono
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Tip", style = MaterialTheme.typography.titleMediumMono)
                Text(
                    text = "$${formatCurrency(tipAmount)}",
                    style = MaterialTheme.typography.titleMediumMono
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 2.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outline
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Total", style = MaterialTheme.typography.titleMediumMono)
                Text(
                    text = "$${formatCurrency(total)}",
                    style = MaterialTheme.typography.titleMediumMono,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Total per person", style = MaterialTheme.typography.titleSmallMono)
                Text(
                    text = "$${formatCurrency(totalPerPerson)}",
                    style = MaterialTheme.typography.titleSmallMono
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Tip percent", style = MaterialTheme.typography.titleSmallMono)
                Text(
                    text = "${formatTipPercent(tipPercent)}%",
                    style = MaterialTheme.typography.titleSmallMono
                )
            }
        }
    }
}

@Composable
private fun LogDetailImageGallery(
    imageFiles: List<File?>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { imageFiles.size })
    val showImageCount = imageFiles.size > 1
    var isImageCountVisible by remember(showImageCount) { mutableStateOf(showImageCount) }

    LaunchedEffect(pagerState.currentPage, showImageCount) {
        if (!showImageCount) {
            isImageCountVisible = false
            return@LaunchedEffect
        }

        isImageCountVisible = true
        delay(3500.milliseconds)
        isImageCountVisible = false
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            ) { page ->
                LogDetailImageCard(
                    imageFile = imageFiles[page],
                    onClick = { onImageClick(page) }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isImageCountVisible,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imageFiles.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LogDetailImageCard(
    imageFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceDim)
    ) {
        if (imageFile == null) {
            Text(
                text = "No image",
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                },
                error = {
                    Text(
                        text = "Image unavailable",
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

@Composable
private fun FullScreenImageDialog(
    imageFiles: List<File?>,
    initialPage: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageFiles.size }
    )
    var currentScale by remember { mutableFloatStateOf(1f) }
    // How far the current photo has been dragged toward closing, 0 to 1. Read only while
    // drawing, so dragging doesn't recompose the dialog.
    var dismissProgress by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Fades out as the photo is dragged away, revealing the screen behind
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.95f * (1f - dismissProgress)))
                }
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentScale <= 1.05f,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageFile = imageFiles[page]
                if (imageFile == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                    ) {
                        Text(
                            text = "Preview image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    ZoomableImage(
                        imageFile = imageFile,
                        pageKey = page,
                        onDismiss = onDismiss,
                        onScaleChanged = { scale ->
                            if (pagerState.currentPage == page) {
                                currentScale = scale
                            }
                        },
                        onDismissProgress = { progress ->
                            if (pagerState.currentPage == page) {
                                dismissProgress = progress
                            }
                        }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // The dialog draws edge to edge, so keep the bar clear of the status bar
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f - dismissProgress }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${imageFiles.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Close",
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * A photo that can be zoomed with a pinch (toward the fingers) or a double-tap (toward the
 * tap), panned when zoomed, and, at normal size, dragged up or down to close. Tapping outside
 * the photo closes it too.
 */
@Composable
private fun ZoomableImage(
    imageFile: File,
    pageKey: Any,
    onDismiss: () -> Unit,
    onScaleChanged: (Float) -> Unit,
    onDismissProgress: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(pageKey) { mutableFloatStateOf(1f) }
    var offset by remember(pageKey) { mutableStateOf(Offset.Zero) }
    // Vertical distance the photo has been dragged toward closing, in pixels
    var dismissDrag by remember(pageKey) { mutableFloatStateOf(0f) }
    // The full-screen area that takes the gestures, and the photo's laid-out size within it,
    // which is its on-screen size before zooming (the box matches the photo's aspect ratio once
    // it loads)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var imageAspectRatio by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val dismissVelocity = with(LocalDensity.current) { DISMISS_VELOCITY.toPx() }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            val height = containerSize.height
            if (height == 0) 0f else (abs(dismissDrag) / (height * DISMISS_FADE_DISTANCE)).coerceIn(0f, 1f)
        }.collect(onDismissProgress)
    }

    // Keeps the zoomed photo covering its box: it may move at most the amount it overhangs
    fun clampOffset(value: Offset, atScale: Float): Offset {
        val maxX = imageSize.width / 2f * (atScale - 1f)
        val maxY = imageSize.height / 2f * (atScale - 1f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // The offset that zooms to [newScale] while keeping the photo point at [fromCenter] (a
    // position relative to the container's center, which the zoom scales around) in place
    fun zoomedOffset(fromCenter: Offset, newScale: Float, pan: Offset = Offset.Zero): Offset =
        clampOffset(fromCenter + pan - (fromCenter - offset) * (newScale / scale), newScale)

    // Zooms toward the fingers: the point of the photo under them stays under them. Gestures
    // are read on the full-screen container, outside the zoom, so they're in screen pixels,
    // panning follows the fingers one to one, and they work anywhere the zoomed photo shows.
    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        if (newScale <= 1f || imageSize == IntSize.Zero) {
            scale = 1f
            offset = Offset.Zero
            return@rememberTransformableState
        }
        val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
        offset = zoomedOffset(centroid - center, newScale, panChange)
        scale = newScale
    }

    // Double-tap: zoom in toward the tap, or back out if already zoomed. [tap] is in the photo
    // box's own (unzoomed) coordinates.
    fun toggleZoom(tap: Offset) {
        if (imageSize == IntSize.Zero) return
        val startScale = scale
        val startOffset = offset
        val endScale = if (scale > 1f) 1f else DOUBLE_TAP_ZOOM
        val endOffset = if (endScale == 1f) {
            Offset.Zero
        } else {
            val half = Offset(imageSize.width / 2f, imageSize.height / 2f)
            zoomedOffset((tap - half) * scale + offset, endScale)
        }
        scope.launch {
            animate(0f, 1f, animationSpec = tween(ZOOM_ANIMATION_MS)) { t, _ ->
                scale = lerp(startScale, endScale, t)
                offset = lerp(startOffset, endOffset, t)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            // Outside transformable, so a two-finger pinch is claimed by the zoom first. Claims
            // only clearly vertical drags; sideways ones go to the pager for the next photo.
            .draggable(
                state = rememberDraggableState { delta -> dismissDrag += delta },
                orientation = Orientation.Vertical,
                enabled = scale == 1f,
                onDragStopped = { velocity ->
                    val height = containerSize.height.toFloat()
                    val closing = abs(dismissDrag) > height * DISMISS_DISTANCE ||
                        abs(velocity) > dismissVelocity
                    if (closing) {
                        // Off the screen in the direction it was going, then close
                        val direction = if (dismissDrag != 0f) sign(dismissDrag) else sign(velocity)
                        animate(
                            dismissDrag,
                            direction * height,
                            animationSpec = tween(DISMISS_ANIMATION_MS)
                        ) { value, _ -> dismissDrag = value }
                        onDismiss()
                    } else {
                        animate(dismissDrag, 0f, animationSpec = spring()) { value, _ ->
                            dismissDrag = value
                        }
                    }
                }
            )
            .transformable(
                state = transformState,
                canPan = { scale > 1f }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .then(
                    if (imageAspectRatio > 0f) {
                        Modifier.aspectRatio(imageAspectRatio)
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .onSizeChanged { imageSize = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y + dismissDrag
                }
                // Inside the zoom, so the tap area grows with the photo. Also keeps taps on the
                // photo from reaching the container, where a tap closes the viewer.
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { toggleZoom(it) }, onTap = {})
                }
        ) {
            SubcomposeAsyncImage(
                model = imageFile,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    val size = state.painter.intrinsicSize
                    if (size.width > 0 && size.height > 0) {
                        imageAspectRatio = size.width / size.height
                    }
                },
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                error = {
                    Text(
                        text = "Image unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LogDetailPagePreview() {
    TipTrackerTheme {
        LogDetailPage(
            uiState = LogDetailUiState(
                bill = 128.12,
                tipPercent = 20.0,
                total = 153.74,
                partySize = 2,
                restaurantName = "The Pearl",
                review = "goysters were so geurch",
                rating = 8.6,
                date = "2026-03-21",
                tipAmount = 25.62,
                totalPerPerson = 76.87,
                images = listOf(),
                isLoading = false,
                isNotFound = false,
                errorMessage = null,
                isDeleting = false
            ),
            imageFiles = listOf(null, null, null)
        )
    }
}

private const val MAX_ZOOM = 4.5f
private const val DOUBLE_TAP_ZOOM = 2.5f
private const val ZOOM_ANIMATION_MS = 250
// Dragging a photo this fraction of the screen height (or flicking it) closes the viewer;
// the background is fully faded at DISMISS_FADE_DISTANCE
private const val DISMISS_DISTANCE = 0.2f
private const val DISMISS_FADE_DISTANCE = 0.5f
private val DISMISS_VELOCITY = 1000.dp // per second
private const val DISMISS_ANIMATION_MS = 150
