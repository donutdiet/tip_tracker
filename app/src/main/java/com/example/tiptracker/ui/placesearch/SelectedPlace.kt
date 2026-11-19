package com.example.tiptracker.ui.placesearch

import com.example.tiptracker.data.repository.PlaceDetails
import com.example.tiptracker.data.repository.PlacePrediction

/**
 * The Google Maps place a log is linked to. Only [placeId] is ever saved; everything else is
 * Google content shown for reference while the screen is open (Google's terms don't allow
 * storing it). The restaurant name that gets saved is always the one the user typed.
 */
data class SelectedPlace(
    val placeId: String,
    // From the picked suggestion. Null for a place looked up when a saved log is opened:
    // that lookup asks for the address only, since the name would bill at a higher rate.
    val name: String? = null,
    // Null while being looked up, or if the lookup failed (see [lookupFailed])
    val address: String? = null,
    // Third-party credits Google requires next to looked-up details; may contain HTML links
    val attributions: List<String> = emptyList(),
    val lookupFailed: Boolean = false
) {
    companion object {
        fun from(prediction: PlacePrediction) =
            SelectedPlace(prediction.placeId, prediction.name, prediction.address)

        fun from(placeId: String, details: PlaceDetails?) =
            if (details == null) {
                SelectedPlace(placeId, lookupFailed = true)
            } else {
                SelectedPlace(placeId, address = details.address, attributions = details.attributions)
            }
    }
}
