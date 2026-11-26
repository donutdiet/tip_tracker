package com.example.tiptracker.ui.features.map

import com.example.tiptracker.ui.theme.card
import com.example.tiptracker.ui.theme.floating
import androidx.annotation.DrawableRes
import com.example.tiptracker.utils.formatDateRelative
import com.example.tiptracker.utils.formatTipPercent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import com.google.maps.android.compose.MapEffect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.android.gms.maps.GoogleMap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.example.tiptracker.R
import com.example.tiptracker.data.repository.PlaceDetails
import com.example.tiptracker.ui.components.PlaceAddress
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.MapPlace
import com.example.tiptracker.ui.theme.titleMediumMono
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatDateForDisplay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.sin

private class PlaceMarker(val place: MapPlace, val selected: Boolean) : ClusterItem {
    override val position = LatLng(place.coordinates!!.latitude, place.coordinates.longitude)
    override val title: String = place.name
    override val snippet: String? = null
    // The selected place draws over places it overlaps
    override val zIndex: Float? = if (selected) 1f else null
}

/**
 * Full-screen map of the user's linked places. Tapping a marker centers the map on it and slides
 * up a sheet summarizing the place; dragging the sheet up lists its visits, and tapping a visit
 * opens it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRoot(
    onBack: () -> Unit,
    onLogClick: (Int) -> Unit,
    viewModel: MapViewModel = koinViewModel()
) = BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPlace = uiState.selectedPlace
    // Markers are drawn to images, so a change of selection makes new ones to redraw
    val markers = remember(uiState.markers, uiState.selectedPlaceId) {
        uiState.markers.map { PlaceMarker(it, selected = it.placeId == uiState.selectedPlaceId) }
    }
    // The sheet keeps showing the last place while it slides away
    val sheetPlace = rememberLastNonNull(selectedPlace)
    val sheetVisits = rememberLastNonNull(uiState.selectedVisits.takeIf { selectedPlace != null })
        .orEmpty()

    // Creating the map is heavy, and doing it while the page slid in made the slide stutter, so
    // it waits for the slide to finish, over a plain background in the map's color
    val pageTransition = LocalNavAnimatedContentScope.current.transition
    var slideFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { pageTransition.isRunning }.first { !it }
        slideFinished = true
    }
    val darkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val sheetMaxHeight = maxHeight * SHEET_MAX_HEIGHT_FRACTION
    val scope = rememberCoroutineScope()
    val snapshotter = remember { MapSnapshotter() }
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val density = LocalDensity.current
    var summaryHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val summaryHeight = with(density) { summaryHeightPx.toDp() }
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Shows the drag handle and the place's summary, above the navigation bar
    val peekHeight = DRAG_HANDLE_HEIGHT + summaryHeight + navigationBarHeight

    // The sheet follows the selection, and swiping it away clears the selection
    LaunchedEffect(selectedPlace != null) {
        if (selectedPlace == null) {
            sheetState.hide()
        } else if (sheetState.targetValue == SheetValue.Hidden) {
            // Only when it's hidden or hiding: on returning to the page it's as it was left
            sheetState.partialExpand()
        }
    }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }.collect { value ->
            if (value == SheetValue.Hidden) viewModel.onAction(MapAction.SelectPlace(null))
        }
    }
    BackHandler(enabled = selectedPlace != null) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            scope.launch { sheetState.partialExpand() }
        } else {
            viewModel.onAction(MapAction.SelectPlace(null))
        }
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
        sheetPeekHeight = peekHeight,
        // The Logs tab's page color, so visits show as cards the same way logs do there
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetDragHandle = { DragHandle() },
        sheetContent = {
            sheetPlace?.let { place ->
                PlaceSheet(
                    place = place,
                    visits = sheetVisits,
                    address = uiState.selectedAddress,
                    isLoadingAddress = uiState.isLoadingAddress,
                    addressFailed = uiState.addressFailed,
                    onShowAddress = { viewModel.onAction(MapAction.ShowAddress) },
                    onSummaryClick = {
                        scope.launch {
                            if (sheetState.currentValue == SheetValue.Expanded) {
                                sheetState.partialExpand()
                            } else {
                                sheetState.expand()
                            }
                        }
                    },
                    onSummaryMeasured = { summaryHeightPx = it },
                    // Pictures the map first, to show while it's rebuilt on return
                    onLogClick = { logId ->
                        snapshotter.take { snapshot ->
                            viewModel.mapSnapshot = snapshot
                            onLogClick(logId)
                        }
                    },
                    // Leaves some map showing above it
                    modifier = Modifier.heightIn(max = sheetMaxHeight)
                )
            }
        }
    ) {
        // Full screen: the map runs under the status bar, with floating controls kept clear of it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (darkMap) DARK_MAP_PLACEHOLDER else LIGHT_MAP_PLACEHOLDER)
        ) {
            viewModel.mapSnapshot?.let { snapshot ->
                Image(
                    bitmap = snapshot,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Built once the places have loaded, so it opens already framed on them
            if (!uiState.isLoading && slideFinished) {
                PlacesMap(
                    markers = markers,
                    snapshotter = snapshotter,
                    snapshot = viewModel.mapSnapshot,
                    onShown = { viewModel.mapSnapshot = null },
                    selectedPosition = markers.find { it.selected }?.position,
                    // Google's logo and the centering keep clear of the sheet
                    bottomInset = if (selectedPlace != null) peekHeight else navigationBarHeight,
                    onPlaceClick = { viewModel.onAction(MapAction.SelectPlace(it.placeId)) },
                    onMapClick = { viewModel.onAction(MapAction.SelectPlace(null)) }
                )
            }

            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.floating,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(BACK_BUTTON_SIZE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            MapStatus(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    // Centered, but clear of the back button on either side
                    .padding(top = 16.dp, start = STATUS_SIDE_INSET, end = STATUS_SIDE_INSET)
            )
        }
    }
}

/** Takes pictures of the map, once it's built. */
private class MapSnapshotter {
    var map: GoogleMap? = null

    /** Calls [onDone] with a picture of the map, or null without one. */
    fun take(onDone: (ImageBitmap?) -> Unit) {
        val map = map ?: return onDone(null)
        map.snapshot { onDone(it?.asImageBitmap()) }
    }
}

/** [value], or while it's null, the last non-null value it had. */
@Composable
private fun <T : Any> rememberLastNonNull(value: T?): T? {
    val last = remember { mutableStateOf(value) }
    if (value != null) last.value = value
    return value ?: last.value
}

// Clustering is marked experimental in maps-compose
@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun PlacesMap(
    markers: List<PlaceMarker>,
    snapshotter: MapSnapshotter,
    // A picture of the map to cover it until it's drawn, instead of a plain color
    snapshot: ImageBitmap?,
    // Called once the map shows
    onShown: () -> Unit,
    selectedPosition: LatLng?,
    bottomInset: Dp,
    onPlaceClick: (MapPlace) -> Unit,
    onMapClick: () -> Unit
) = BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    // Opens framed on the places it has, instead of the whole world zooming in
    val initialCount = remember { markers.size }
    val cameraPositionState = rememberCameraPositionState {
        position = framing(markers.map { it.position }, maxWidth.value, maxHeight.value)
            ?: CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 1f)
    }
    val density = LocalDensity.current
    val edgePadding = with(density) { EDGE_PADDING.roundToPx() }
    // Keeps Google's logo and the centering clear of the status bar and whatever covers the
    // bottom (the navigation bar, or the place sheet)
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val padding = with(density) {
        MapPadding(
            left = systemBars.calculateLeftPadding(layoutDirection).roundToPx(),
            top = systemBars.calculateTopPadding().roundToPx(),
            right = systemBars.calculateRightPadding(layoutDirection).roundToPx(),
            bottom = bottomInset.roundToPx()
        )
    }
    var appliedPadding by remember { mutableStateOf<MapPadding?>(null) }
    // Where the camera pointed as the map opened, kept while the padding settles
    var openingTarget by remember { mutableStateOf<LatLng?>(null) }
    // The place the camera last glided to; saved, so returning to the page doesn't glide again
    var centeredOn by rememberSaveable { mutableStateOf<LatLng?>(null) }
    DisposableEffect(snapshotter) { onDispose { snapshotter.map = null } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Follows the app's theme (its own dark mode setting, not only the system's)
    val darkMap = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mapStyle = remember(darkMap) {
        MapStyleOptions.loadRawResourceStyle(
            context,
            if (darkMap) R.raw.map_style_dark else R.raw.map_style
        )
    }
    var mapLoaded by remember { mutableStateOf(false) }
    // Places located after opening join the frame, until the user moves the map themselves
    var userMovedMap by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving &&
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        ) {
            userMovedMap = true
        }
    }
    LaunchedEffect(mapLoaded, markers.size) {
        if (!mapLoaded || userMovedMap || markers.isEmpty() || markers.size == initialCount) {
            return@LaunchedEffect
        }
        val update = if (markers.size == 1) {
            CameraUpdateFactory.newLatLngZoom(markers.first().position, SINGLE_PLACE_ZOOM)
        } else {
            val bounds = LatLngBounds.builder().apply { markers.forEach { include(it.position) } }.build()
            CameraUpdateFactory.newLatLngBounds(bounds, edgePadding)
        }
        cameraPositionState.animate(update)
    }

    val placeholder = if (darkMap) DARK_MAP_PLACEHOLDER else LIGHT_MAP_PLACEHOLDER
    // Until the map has drawn with its style, it shows the SDK's own light background and map,
    // a white flash in dark mode. A cover in the map's color hides that, then fades out. The
    // fallback is for when the map never reports loaded (e.g. offline tiles).
    var mapShown by remember { mutableStateOf(false) }
    LaunchedEffect(mapLoaded) {
        if (!mapLoaded) {
            delay(MAP_COVER_FALLBACK_MS)
        } else if (snapshot != null) {
            // Markers are added just after the map loads; the picture already has them
            delay(MARKERS_SETTLE_MS)
        }
        mapShown = true
    }
    val coverAlpha by animateFloatAsState(
        targetValue = if (mapShown) 0f else 1f,
        animationSpec = tween(MAP_COVER_FADE_MS),
        label = "mapCover"
    )

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        // Drawn before the tiles and style load, so start in the map's own color
        googleMapOptionsFactory = { GoogleMapOptions().backgroundColor(placeholder.toArgb()) },
        // The SDK's base map otherwise follows the system's dark mode, not the app's setting
        mapColorScheme = if (darkMap) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        // The user's places are the point, so the map stays quiet: no other businesses' icons
        // (which also duplicated logged restaurants), and muted colors so the badges stand out
        properties = MapProperties(mapStyleOptions = mapStyle),
        uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false),
        onMapLoaded = { mapLoaded = true },
        onMapClick = { onMapClick() }
    ) {
        MapEffect(Unit) { map -> snapshotter.map = map }

        // Padding is set here rather than through contentPadding to control the camera around
        // it. New padding keeps what's on screen and moves the camera's target to the new
        // center, which is what's wanted once the map shows (the sheet sliding up doesn't move
        // the map). Before then, the map is opening with a camera whose target belongs at the
        // center of the padding (the initial framing, or the camera saved on leaving the page),
        // while the padding settles (the sheet measures itself after the map is built), so the
        // target is put back after each change.
        MapEffect(padding, selectedPosition) { map ->
            if (appliedPadding != padding) {
                val target = if (mapShown) {
                    null
                } else {
                    openingTarget ?: map.cameraPosition.target.also { openingTarget = it }
                }
                map.setPadding(padding.left, padding.top, padding.right, padding.bottom)
                target?.let { map.moveCamera(CameraUpdateFactory.newLatLng(it)) }
                appliedPadding = padding
            }
            // Glides to a newly selected place, keeping the zoom. Marked done only once there:
            // the sheet sliding up changes the padding, which restarts this mid-glide
            if (selectedPosition == null) {
                centeredOn = null
            } else if (selectedPosition != centeredOn) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(selectedPosition))
                centeredOn = selectedPosition
            }
        }

        Clustering(
            items = markers,
            // Zooms in just far enough for the cluster to split apart
            onClusterClick = { cluster ->
                val positions = cluster.items.map { it.position }
                val bounds = LatLngBounds.builder().apply { positions.forEach(::include) }.build()
                val zoom = splitZoom(cluster.position, positions, cameraPositionState.position.zoom)
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(bounds.center, zoom))
                }
                true
            },
            onClusterItemClick = { marker ->
                onPlaceClick(marker.place)
                true // No default info window; the sheet shows the place
            },
            clusterItemContent = { marker ->
                RatingMarker(marker.place.averageRating, marker.place.visitCount, marker.selected)
            },
            clusterContent = { cluster -> ClusterBubble(cluster.size) },
            // Centered on the place, since they're badges rather than pins
            clusterItemContentAnchor = Offset(0.5f, 0.5f),
            clusterContentAnchor = Offset(0.5f, 0.5f),
            onClusterManager = { manager ->
                // Group only places whose badges would collide, even just two of them (the
                // library's defaults suit small pins: 100dp apart, and 4 or more). splitZoom
                // relies on this distance, so it's set rather than left to the default.
                manager.algorithm.maxDistanceBetweenClusteredItems = CLUSTER_DISTANCE_DP
                (manager.renderer as? DefaultClusterRenderer<*>)?.minClusterSize = MIN_CLUSTER_SIZE
            }
        )
    }

    LaunchedEffect(coverAlpha == 0f) { if (coverAlpha == 0f) onShown() }
    // Drawn over the map; it doesn't take touches, so the map is usable as it fades
    if (coverAlpha > 0f) {
        if (snapshot != null) {
            Image(
                bitmap = snapshot,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha }
                    .background(placeholder)
            )
        }
    }
}

/**
 * A place's marker: its average rating in a circle colored by how good it is, with a white ring
 * so it stands out from the map. Selected, a soft halo in its rating's color surrounds it; the
 * circle itself never changes, so its color keeps meaning the rating. With more than one log, a
 * count badge sits on its top-right edge.
 */
@Composable
private fun RatingMarker(rating: Double, logCount: Int, selected: Boolean) {
    val (ratingColor, ratingText) = ratingColors(rating)
    // The marker is drawn into a bitmap the size of this box, so the badge needs room inside it.
    // The room is even on all sides, keeping the circle centered on the place.
    // The room also fits the halo, so selecting a place doesn't change the marker's size
    Box(modifier = Modifier.padding(MARKER_BADGE_ROOM)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(MARKER_SIZE)
                // Drawn past the circle, into the room around it, and before the shadow's clip
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            color = ratingColor.copy(alpha = HALO_ALPHA),
                            radius = size.minDimension / 2 + HALO_WIDTH.toPx()
                        )
                    }
                }
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(2.dp)
                .background(ratingColor, CircleShape)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", rating),
                color = ratingText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        if (logCount > 1) {
            LogCountBadge(
                count = logCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = BADGE_OVERHANG, y = -BADGE_OVERHANG)
            )
        }
    }
}

/** How many logs a place has: dark, so it doesn't read as a rating color. */
@Composable
private fun LogCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(Color.White, CircleShape)
            .padding(1.5.dp)
            .background(MARKER_DARK, CircleShape)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private val MARKER_SIZE = 36.dp
// Space around a marker's circle for its badge to overhang, and its halo when selected
private val MARKER_BADGE_ROOM = 8.dp
// How far the selected halo reaches past the circle (within MARKER_BADGE_ROOM), and how solid
private val HALO_WIDTH = 7.dp
private const val HALO_ALPHA = 0.4f
// How far the badge sticks out past the circle's bounding square
private val BADGE_OVERHANG = 6.dp

private val MARKER_DARK = Color(0xFF202124)

// Red at 5 or below, yellow below 8, green from 8 (ratings are 0-10). Yellow takes dark text,
// which reads better on it than white.
private fun ratingColors(rating: Double): Pair<Color, Color> = when {
    rating <= 5.0 -> Color(0xFFD93025) to Color.White
    rating < 8.0 -> Color(0xFFF9AB00) to MARKER_DARK
    else -> Color(0xFF1E8E3E) to Color.White
}

/** A group of nearby places: white with a dark outline, so it doesn't read as a rating. */
@Composable
private fun ClusterBubble(count: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .shadow(3.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(2.dp, MARKER_DARK, CircleShape)
    ) {
        Text(
            text = count.toString(),
            color = MARKER_DARK,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** What the map is doing, when some places aren't on it. */
@Composable
private fun MapStatus(uiState: MapUiState, modifier: Modifier = Modifier) {
    val text = when {
        uiState.isLoading -> null
        uiState.places.isEmpty() ->
            "No linked places yet. Pick a restaurant from the suggestions when logging to see it here."
        uiState.locatingCount > 0 -> "Finding ${places(uiState.locatingCount)}…"
        uiState.unlocatedCount > 0 -> "${places(uiState.unlocatedCount)} can't be shown right now"
        else -> null
    } ?: return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun places(count: Int) = if (count == 1) "1 place" else "$count places"

@Composable
private fun DragHandle() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(DRAG_HANDLE_HEIGHT)
    ) {
        Box(
            modifier = Modifier
                .size(width = 32.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
        )
    }
}

/**
 * The selected place: a summary (what the collapsed sheet shows; tapping it expands or collapses
 * the sheet), then its visits, newest first. Tapping a visit opens it.
 */
@Composable
private fun PlaceSheet(
    place: MapPlace,
    visits: List<Log>,
    address: PlaceDetails?,
    isLoadingAddress: Boolean,
    addressFailed: Boolean,
    onShowAddress: () -> Unit,
    onSummaryClick: () -> Unit,
    onSummaryMeasured: (heightPx: Int) -> Unit,
    onLogClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        PlaceSummary(
            place = place,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { onSummaryMeasured(it.height) }
                .clickable(onClick = onSummaryClick)
                .padding(start = SHEET_INSET, end = SHEET_INSET, bottom = 16.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = SHEET_INSET)
        )
        // Cards inset their text, so it lines up with the summary's
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = SHEET_INSET - ROW_INSET,
                end = SHEET_INSET - ROW_INSET,
                top = 8.dp,
                bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
        ) {
            item {
                PlaceAddress(
                    details = address,
                    isLoading = isLoadingAddress,
                    failed = addressFailed,
                    onShow = onShowAddress,
                    modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 4.dp)
                )
            }
            items(visits, key = { it.id }) { log ->
                VisitRow(log = log, onClick = { onLogClick(log.id) })
            }
        }
    }
}

/** The place's name, when the user was last there, and their habits there. */
@Composable
private fun PlaceSummary(place: MapPlace, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = lastVisited(place),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Averages across the visits, styled like the stats on each visit below
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PlaceStat(
                        icon = R.drawable.money,
                        text = "$${formatCurrency(place.averagePerPerson)}",
                        description = "Average spent per person"
                    )
                    PlaceStat(
                        icon = R.drawable.hand_meal,
                        text = "${formatTipPercent(place.averageTipPercent)}%",
                        description = "Average tip"
                    )
                    PlaceStat(
                        icon = R.drawable.person,
                        text = formatAverage(place.averagePartySize),
                        description = "Average party size"
                    )
                    PlaceStat(
                        icon = R.drawable.receipt_long,
                        text = "${place.visitCount}",
                        description = "Visits"
                    )
                }
            }
        }
    }
}

/**
 * One visit, a card like a log in the Logs tab: the place is already known, so its total leads, with
 * the rating where the total was.
 */
@Composable
private fun VisitRow(log: Log, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.card,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ROW_INSET, top = 8.dp, bottom = 8.dp, end = 4.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$${formatCurrency(log.total)}",
                        style = MaterialTheme.typography.titleMediumMono,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", log.rating),
                        style = MaterialTheme.typography.titleMediumMono
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        VisitStat(R.drawable.person, "${log.partySize}")
                        VisitStat(R.drawable.hand_meal, "${formatTipPercent(log.tipPercent)}%")
                    }
                    Text(
                        text = formatDateForDisplay(log.date),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
    }
}

/** A stat in the summary: like [VisitStat], a size up, with what it is for screen readers. */
@Composable
private fun PlaceStat(@DrawableRes icon: Int, text: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$description: $text" }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

// 2.5, or 2 rather than 2.0
private fun formatAverage(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)

@Composable
private fun VisitStat(@DrawableRes icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(14.dp)
        )
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

private fun lastVisited(place: MapPlace): String {
    val last = formatDateRelative(place.lastVisited)
    return if (last == "Today" || last == "Yesterday") {
        "Last visited ${last.lowercase()}"
    } else {
        "Last visited $last"
    }
}

/** Map padding in pixels. */
private data class MapPadding(val left: Int, val top: Int, val right: Int, val bottom: Int)

// Map math works in "world" coordinates: Web Mercator scaled to a 0-1 square, which at zoom z
// spans 256 * 2^z dp on screen

private fun worldX(position: LatLng) = position.longitude / 360 + 0.5

private fun worldY(position: LatLng): Double {
    val sinLat = sin(Math.toRadians(position.latitude)).coerceIn(-0.9999, 0.9999)
    return 0.5 - ln((1 + sinLat) / (1 - sinLat)) / (4 * PI)
}

/**
 * A camera showing all [positions] in a [widthDp] by [heightDp] map, inset by EDGE_PADDING.
 * Null without positions.
 */
private fun framing(positions: List<LatLng>, widthDp: Float, heightDp: Float): CameraPosition? {
    if (positions.isEmpty()) return null
    val bounds = LatLngBounds.builder().apply { positions.forEach(::include) }.build()
    if (positions.size == 1) return CameraPosition.fromLatLngZoom(bounds.center, SINGLE_PLACE_ZOOM)
    val spanX = positions.maxOf(::worldX) - positions.minOf(::worldX)
    val spanY = positions.maxOf(::worldY) - positions.minOf(::worldY)
    val usableWidth = widthDp - 2 * EDGE_PADDING.value
    val usableHeight = heightDp - 2 * EDGE_PADDING.value
    val zoom = minOf(
        if (spanX > 0) log2(usableWidth / (256 * spanX)) else Double.MAX_VALUE,
        if (spanY > 0) log2(usableHeight / (256 * spanY)) else Double.MAX_VALUE,
        SINGLE_PLACE_ZOOM.toDouble()
    )
    return CameraPosition.fromLatLngZoom(bounds.center, zoom.toFloat())
}

/**
 * The zoom at which a cluster splits apart. Clustering groups, around one place (where the
 * cluster is drawn, [clusterPosition]), every place within half of CLUSTER_DISTANCE_DP of it in
 * each direction, measured at the whole-number zoom. So the cluster splits at the first whole
 * zoom where its farthest place is outside that box. At least one level in, at most the max.
 */
private fun splitZoom(clusterPosition: LatLng, positions: List<LatLng>, currentZoom: Float): Float {
    val farthest = positions.maxOf {
        maxOf(abs(worldX(it) - worldX(clusterPosition)), abs(worldY(it) - worldY(clusterPosition)))
    }
    val nextLevel = currentZoom.toInt() + 1f
    // Places at the same spot never split
    if (farthest <= 0.0) return MAX_ZOOM
    // Outside the box when farthest > (distance / 256 / 2^z) / 2
    val zoom = floor(log2(CLUSTER_DISTANCE_DP / (512 * farthest))) + 1
    return zoom.toFloat().coerceIn(nextLevel, MAX_ZOOM)
}

private const val SINGLE_PLACE_ZOOM = 15f
private const val MAX_ZOOM = 21f
// How close places must be on screen to group: a bit more than a marker's width, so places
// group only when their markers would overlap
private const val CLUSTER_DISTANCE_DP = 48
private const val MIN_CLUSTER_SIZE = 2
private val EDGE_PADDING = 64.dp
// Roughly the land color of each map style, shown until the map is created
private const val MAP_COVER_FADE_MS = 200
private const val MAP_COVER_FALLBACK_MS = 1_500L
private const val MARKERS_SETTLE_MS = 300L
private val LIGHT_MAP_PLACEHOLDER = Color(0xFFEEF0F1)
private val DARK_MAP_PLACEHOLDER = Color(0xFF212121)
private val BACK_BUTTON_SIZE = 44.dp
private val DRAG_HANDLE_HEIGHT = 24.dp
// Where the sheet's text starts from either edge
private val SHEET_INSET = 24.dp
// How far inside a visit row its text starts
private val ROW_INSET = 8.dp
// How much of the screen the expanded sheet may cover, leaving some map above it
private const val SHEET_MAX_HEIGHT_FRACTION = 0.85f
// The back button plus its margin, so the centered status chip never runs under it
private val STATUS_SIDE_INSET = 68.dp
