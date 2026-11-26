package com.example.tiptracker.data.model

import com.example.tiptracker.data.helper.Coordinates

/** Google's terms allow keeping a place's coordinates for at most 30 days. */
const val COORDINATES_MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

/** One log linked to a place, as read for the map. */
data class PlacedVisit(
    val id: Int,
    val placeId: String,
    val name: String,
    val date: String, // yyyy-mm-dd
    val rating: Double,
    val tipPercent: Double,
    val total: Double,
    val partySize: Int,
    val billType: BillType,
    val latitude: Double?,
    val longitude: Double?,
    val coordinatesFetchedAt: Long?
)

/** A place the user has logged, as one marker on the map. */
data class MapPlace(
    val placeId: String,
    // The place's name (one per place), from its most recent visit
    val name: String,
    val visitCount: Int,
    val lastVisited: String, // yyyy-mm-dd
    val averageRating: Double,
    val averageTipPercent: Double,
    // What one person paid, on average (see totalPerPerson)
    val averagePerPerson: Double,
    val averagePartySize: Double,
    // Null until looked up, or once the stored ones expire
    val coordinates: Coordinates?
)

/**
 * One [MapPlace] per place ID. Coordinates come from the most recent lookup among the visits,
 * ignoring any older than [COORDINATES_MAX_AGE_MS] at [now].
 */
fun groupMapPlaces(visits: List<PlacedVisit>, now: Long): List<MapPlace> =
    visits.groupBy { it.placeId }.map { (placeId, placeVisits) ->
        val latest = placeVisits.maxWith(compareBy({ it.date }, { it.id }))
        val located = placeVisits
            .filter {
                it.latitude != null && it.longitude != null && it.coordinatesFetchedAt != null &&
                    now - it.coordinatesFetchedAt <= COORDINATES_MAX_AGE_MS
            }
            .maxByOrNull { it.coordinatesFetchedAt!! }
        MapPlace(
            placeId = placeId,
            name = latest.name,
            visitCount = placeVisits.size,
            lastVisited = latest.date,
            averageRating = placeVisits.map { it.rating }.average(),
            averageTipPercent = placeVisits.map { it.tipPercent }.average(),
            averagePerPerson = placeVisits.map { totalPerPerson(it.total, it.partySize, it.billType) }.average(),
            averagePartySize = placeVisits.map { it.partySize }.average(),
            coordinates = located?.let { Coordinates(it.latitude!!, it.longitude!!) }
        )
    }
