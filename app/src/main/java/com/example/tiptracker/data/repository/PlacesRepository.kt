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
    val address: String,
    // A restaurant or somewhere similar (cafe, bar, bakery, ...); these are listed first
    val isFoodPlace: Boolean = false
)

/**
 * Whether Google's place [types] describe somewhere people eat or drink. Places like grocery
 * stores aren't, but are still suggested (e.g. for their food courts), just after these.
 * Types: https://developers.google.com/maps/documentation/places/android-sdk/place-types
 */
private fun isFoodPlace(types: List<String>): Boolean = types.any { type ->
    type in FOOD_PLACE_TYPES || FOOD_PLACE_TYPE_SUFFIXES.any { type.endsWith(it) }
}

/** Food places first; otherwise keeps Google's relevance order. */
private fun List<PlacePrediction>.foodPlacesFirst(): List<PlacePrediction> =
    sortedByDescending { it.isFoodPlace } // Stable, so order within each group is kept

// Covers e.g. "pizza_restaurant", "cat_cafe", "wine_bar"
private val FOOD_PLACE_TYPE_SUFFIXES = listOf("_restaurant", "_cafe", "_bar")

private val FOOD_PLACE_TYPES = setOf(
    "restaurant", "cafe", "bar", "pub", "bakery", "bistro", "diner", "deli", "cafeteria",
    "food_court", "meal_takeaway", "meal_delivery", "steak_house", "gastropub", "tea_house",
    "coffee_shop", "sandwich_shop", "ice_cream_shop", "dessert_shop", "donut_shop",
    "bagel_shop", "juice_shop", "acai_shop", "noodle_shop"
)

/**
 * Google content about a place, for display only: Google's terms don't allow storing it
 * (only the place ID may be saved). [attributions] are third-party credits Google requires
 * to be shown alongside it, and may contain HTML links.
 */
data class PlaceDetails(
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
     * Place predictions of any type for [query], biased toward [origin] when known, with
     * restaurant-like places first. Not filtered by type, so places like grocery stores can
     * still be picked.
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
                        address = it.getSecondaryText(null).toString(),
                        isFoodPlace = isFoodPlace(it.types)
                    )
                }.foodPlacesFirst()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PlacesRepository", "Autocomplete failed", e)
            PlaceSearchResult.Failure
        }
    }

    /**
     * Coordinates of [placeId], or null on any failure. With a [token], ends that autocomplete
     * session; without one it's a standalone lookup, as when the map locates a saved place.
     * Location only, so it bills at Place Details Essentials. A session ended by an Essentials
     * lookup still bills its first 12 autocomplete requests (only later ones are free), but an
     * unended session bills all of them.
     */
    suspend fun fetchCoordinates(placeId: String, token: AutocompleteSessionToken? = null): Coordinates? {
        val client = client ?: return null
        val cancellation = CancellationTokenSource()
        return try {
            val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.LOCATION))
                .apply { token?.let(::setSessionToken) }
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
     * Current address of [placeId] for display, or null on any failure (offline, quota, ...).
     * Never persist the result or keep it beyond the screen showing it (Google's terms).
     * Address only, so it bills at Place Details Essentials; adding DISPLAY_NAME would bill
     * the whole request at Pro.
     * No session token: this is a standalone lookup, not the end of an autocomplete search.
     */
    suspend fun fetchPlaceDetails(placeId: String): PlaceDetails? {
        val client = client ?: return null
        val cancellation = CancellationTokenSource()
        return try {
            val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.FORMATTED_ADDRESS))
                .setCancellationToken(cancellation.token)
                .build()
            val place = client.fetchPlace(request).await(cancellation).place
            PlaceDetails(
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
    }
}
