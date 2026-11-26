package com.example.tiptracker.ui.features.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.MapPlace
import com.example.tiptracker.data.repository.ConnectionStatus
import com.example.tiptracker.data.repository.ConnectivityRepository
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.data.repository.PlaceDetails
import com.example.tiptracker.data.repository.PlacesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class MapUiState(
    val places: List<MapPlace> = emptyList(),
    val isLoading: Boolean = true,
    // Places whose coordinates are being looked up right now
    val locatingCount: Int = 0,
    val selectedPlaceId: String? = null,
    // The selected place's visits, newest first
    val selectedVisits: List<Log> = emptyList(),
    // The selected place's address, once the user asks for it: shown with Google's
    // attributions, never saved
    val selectedAddress: PlaceDetails? = null,
    val isLoadingAddress: Boolean = false,
    val addressFailed: Boolean = false
) {
    val markers: List<MapPlace>
        get() = places.filter { it.coordinates != null }

    // Places that can't be shown: not located yet, offline, or the lookup failed
    val unlocatedCount: Int
        get() = places.count { it.coordinates == null }

    val selectedPlace: MapPlace?
        get() = places.find { it.placeId == selectedPlaceId }
}

sealed interface MapAction {
    data class SelectPlace(val placeId: String?) : MapAction
    data object ShowAddress : MapAction
}

/**
 * The user's linked places on a map. Google's terms allow keeping coordinates 30 days, so on
 * opening it clears expired ones and looks up any place without fresh coordinates: one billed
 * Place Details (location only) per place, at most once per place per 30 days.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val logsRepository: LogRepository,
    private val placesRepository: PlacesRepository,
    private val connectivityRepository: ConnectivityRepository
) : ViewModel() {

    /**
     * A picture of the map from when a visit was opened. Leaving the page disposes the map, and
     * rebuilding it on return takes a moment, so this stands in for it until it's drawn.
     */
    var mapSnapshot by mutableStateOf<ImageBitmap?>(null)

    private val selectedPlaceId = MutableStateFlow<String?>(null)
    private val locatingCount = MutableStateFlow(0)
    // Addresses looked up while the map is open, so reselecting a place doesn't look it up again
    private val addresses = MutableStateFlow<Map<String, PlaceDetails>>(emptyMap())
    private val addressLookup = MutableStateFlow<AddressLookup?>(null)

    // Null until the first load
    private val mapPlaces = logsRepository.observeMapPlaces()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<MapUiState> = combine(
        mapPlaces,
        selectedPlaceId,
        // Tagged with their place: right after a new selection, the previous place's visits are
        // still the latest until the new ones load
        selectedPlaceId.flatMapLatest { placeId ->
            placeId?.let { id -> logsRepository.observeLogsAtPlace(id).map { id to it } }
                ?: flowOf(null to emptyList())
        },
        locatingCount,
        combine(addresses, addressLookup, ::Pair)
    ) { places, selected, (visitsPlaceId, visits), locating, (knownAddresses, lookup) ->
        val selectedLookup = lookup?.takeIf { it.placeId == selected }
        MapUiState(
            places = places.orEmpty(),
            isLoading = places == null,
            locatingCount = locating,
            selectedPlaceId = selected,
            selectedVisits = if (visitsPlaceId == selected) visits else emptyList(),
            selectedAddress = selected?.let(knownAddresses::get),
            isLoadingAddress = selectedLookup?.failed == false,
            addressFailed = selectedLookup?.failed == true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MapUiState()
    )

    init {
        viewModelScope.launch {
            logsRepository.clearExpiredCoordinates()
            val unlocated = mapPlaces.filterNotNull().first()
                .filter { it.coordinates == null }
                .map { it.placeId }
            locate(unlocated)
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.SelectPlace -> selectedPlaceId.value = action.placeId
            MapAction.ShowAddress -> showAddress()
        }
    }

    // One billed Place Details call (address only), when the user asks
    private fun showAddress() {
        val placeId = selectedPlaceId.value ?: return
        if (placeId in addresses.value) return
        if (addressLookup.value?.let { it.placeId == placeId && !it.failed } == true) return

        viewModelScope.launch {
            addressLookup.value = AddressLookup(placeId, failed = false)
            val details = placesRepository.fetchPlaceDetails(placeId)
            if (details?.address != null) {
                addresses.update { it + (placeId to details) }
                addressLookup.value = null
            } else {
                addressLookup.value = AddressLookup(placeId, failed = true)
            }
        }
    }

    // Looks the places up a few at a time; each one found joins the map as soon as it's stored.
    // Offline or without a Places key, they're left off until the map is next opened.
    private suspend fun locate(placeIds: List<String>) {
        if (placeIds.isEmpty() || !placesRepository.isAvailable) return
        if (connectivityRepository.status.value != ConnectionStatus.Online) return

        locatingCount.value = placeIds.size
        val permits = Semaphore(MAX_CONCURRENT_LOOKUPS)
        coroutineScope {
            placeIds.forEach { placeId ->
                launch {
                    permits.withPermit {
                        try {
                            placesRepository.fetchCoordinates(placeId)?.let { coordinates ->
                                logsRepository.setPlaceCoordinates(
                                    placeId,
                                    coordinates,
                                    fetchedAt = System.currentTimeMillis()
                                )
                            }
                        } finally {
                            locatingCount.update { it - 1 }
                        }
                    }
                }
            }
        }
    }

    // An address lookup in progress, or the last one that failed
    private data class AddressLookup(val placeId: String, val failed: Boolean)

    private companion object {
        const val MAX_CONCURRENT_LOOKUPS = 4
    }
}
