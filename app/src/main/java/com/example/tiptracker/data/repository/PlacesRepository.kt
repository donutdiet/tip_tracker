package com.example.tiptracker.data.repository

import android.content.Context
import com.example.tiptracker.data.helper.Coordinates
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PlacePrediction(
    val placeId: String,
    val name: String,
    val address: String
)

/**
 * Google content about a place, for display only: Google's terms don't allow storing it
 * (only the place ID may be saved). [attributions] are third-party credits Google requires
 * to be shown alongside it, and may contain HTML links.
 */
data class PlaceDetails(
    val name: String?,
    val address: String?,
    val attributions: List<String> = emptyList()
)

sealed interface PlaceSearchResult {
    data class Success(val predictions: List<PlacePrediction>) : PlaceSearchResult

    /** The request failed (network, quota, bad key, ...). Callers should fall back to free text. */
    data object Failure : PlaceSearchResult
}

class PlacesRepository(private val context: Context) {

    // Null when the SDK wasn't initialized (e.g. no API key in local.properties)
    private val client: PlacesClient? by lazy {
        if (Places.isInitialized()) Places.createClient(context) else null
    }

    val isAvailable: Boolean get() = client != null

    /**
     * One token per autocomplete session: pass it to every [search] while the user is typing
     * and to [fetchCoordinates] for the chosen result, which ends the session.
     */
    fun newSessionToken(): AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    /**
     * Restaurant-like predictions for [query], biased toward [origin] when known.
     * Cancelling the calling coroutine cancels the in-flight request.
     */
    suspend fun search(
        query: String,
        origin: Coordinates?,
        token: AutocompleteSessionToken
    ): PlaceSearchResult {
        val client = client ?: return PlaceSearchResult.Failure
        val cancellation = CancellationTokenSource()
        return try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
                .setTypesFilter(RESTAURANT_TYPES)
                .setCancellationToken(cancellation.token)
                .apply {
                    origin?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        setOrigin(latLng)
                        setLocationBias(CircularBounds.newInstance(latLng, BIAS_RADIUS_METERS))
                    }
                }
                .build()

            val response = client.findAutocompletePredictions(request).await(cancellation)
            PlaceSearchResult.Success(
                response.autocompletePredictions.map {
                    PlacePrediction(
                        placeId = it.placeId,
                        name = it.getPrimaryText(null).toString(),
                        address = it.getSecondaryText(null).toString()
                    )
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PlacesRepository", "Autocomplete failed", e)
            PlaceSearchResult.Failure
        }
    }

    /** Coordinates of [placeId], or null on any failure. Ends the autocomplete session. */
    suspend fun fetchCoordinates(placeId: String, token: AutocompleteSessionToken): Coordinates? {
        val client = client ?: return null
        val cancellation = CancellationTokenSource()
        return try {
            val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LOCATION))
                .setSessionToken(token)
                .setCancellationToken(cancellation.token)
                .build()
            client.fetchPlace(request).await(cancellation).place.location
                ?.let { Coordinates(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PlacesRepository", "Fetching place coordinates failed", e)
            null
        }
    }

    /**
     * Current details of [placeId] for display, or null on any failure (offline, quota, ...).
     * Never persist the result or keep it beyond the screen showing it (Google's terms).
     * The address alone bills at Place Details Essentials; adding the name ([includeName])
     * bills the whole request at Pro, so only ask for it where it's shown.
     * No session token: this is a standalone lookup, not the end of an autocomplete search.
     */
    suspend fun fetchPlaceDetails(placeId: String, includeName: Boolean): PlaceDetails? {
        val client = client ?: return null
        val cancellation = CancellationTokenSource()
        return try {
            val fields = buildList {
                add(Place.Field.FORMATTED_ADDRESS)
                if (includeName) add(Place.Field.DISPLAY_NAME)
            }
            val request = FetchPlaceRequest.builder(placeId, fields)
                .setCancellationToken(cancellation.token)
                .build()
            val place = client.fetchPlace(request).await(cancellation).place
            PlaceDetails(
                name = place.displayName,
                address = place.formattedAddress,
                attributions = place.attributions.orEmpty()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PlacesRepository", "Fetching place details failed", e)
            null
        }
    }

    private suspend fun <T> Task<T>.await(cancellation: CancellationTokenSource): T =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { cancellation.cancel() }
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
            addOnCanceledListener { cont.cancel() }
        }

    private companion object {
        const val BIAS_RADIUS_METERS = 25_000.0

        // The API accepts at most five types
        val RESTAURANT_TYPES = listOf(
            PlaceTypes.RESTAURANT,
            PlaceTypes.CAFE,
            PlaceTypes.BAR,
            PlaceTypes.BAKERY,
            PlaceTypes.MEAL_TAKEAWAY
        )
    }
}
