package com.example.tiptracker.ui.features.logdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.titleMediumMono
import com.example.tiptracker.ui.theme.titleSmallMono
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.example.tiptracker.utils.formatTipPercent
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun LogDetailPage(
    uiState: LogDetailUiState,
    imageFiles: List<File?>,
    modifier: Modifier = Modifier
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
        Spacer(modifier = Modifier.height(20.dp))

        LogStatsCard(
            billAmount = uiState.bill,
            tipAmount = uiState.tipAmount,
            total = uiState.total,
            date = uiState.date,
            partySize = uiState.partySize,
            totalPerPerson = uiState.totalPerPerson,
            tipPercent = uiState.tipPercent,
            address = uiState.address
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.star),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = uiState.rating.toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Text(text = uiState.review, style = MaterialTheme.typography.bodyMedium)
//        Spacer(modifier = Modifier.height(20.dp))


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
    totalPerPerson: Double,
    tipPercent: Double,
    address: String? = null
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
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
                        text = "$partySize",
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

            address?.let {
                Spacer(modifier = Modifier.height(12.dp))
                val inlineContent = mapOf(
                    "locationIcon" to InlineTextContent(
                        Placeholder(
                            width = 18.sp,
                            height = 18.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.location),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
                Text(
                    text = buildAnnotatedString {
                        appendInlineContent("locationIcon", "[location]")
                        append(" ")
                        append(it)
                    },
                    inlineContent = inlineContent,
                    style = MaterialTheme.typography.titleSmallMono,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
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
        delay(3500)
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

            // HACK: figure out why I can't just import normally
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 72.dp)
            ) { page ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val imageFile = imageFiles[page]
                    if (imageFile == null) {
                        Text(
                            text = "Preview image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = imageFile,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp,
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

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter)
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
                address = "test address",
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