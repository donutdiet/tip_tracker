package com.example.tiptracker.data.model

data class RestaurantVisit(
    val id: Int,
    val name: String,
    val placeId: String?,
    val date: String // yyyy-mm-dd
)

/** A restaurant the user has logged before: one suggestion row, standing for all its visits. */
data class RestaurantHistory(
    // The spelling used on the most recent visit
    val name: String,
    // The Google Maps place the visits are linked to, if any. Picking the row links it.
    val placeId: String?,
    val visitCount: Int,
    val lastVisited: String, // yyyy-mm-dd
    // restaurantNameKey of [name], for matching against what the user types
    val nameKey: String
)

/**
 * Key for treating differently written names as the same restaurant: case, punctuation and
 * spacing are ignored, so "The Pearl", "the pearl" and "The Pearl!" all become "the pearl".
 */
fun restaurantNameKey(name: String): String =
    name.lowercase()
        .replace(NON_NAME_CHARACTERS, "")
        .replace(WHITESPACE, " ")
        .trim()

private val NON_NAME_CHARACTERS = Regex("[^\\p{L}\\p{N}\\s]")
private val WHITESPACE = Regex("\\s+")

/**
 * Groups visits into one row per restaurant:
 * 1. Visits linked to a Google place are grouped by its ID. Exact, so two locations of the
 *    same chain stay apart.
 * 2. Unlinked visits are grouped by [restaurantNameKey].
 * 3. A name group joins a place group when its key matches a name used at exactly one place
 *    (an old free-text "The Pearl" joins the linked Pearl). If it matches several places
 *    (two linked Starbucks), which one is meant is unknown, so it stays on its own.
 *
 * Visits whose name has no letters or digits are skipped.
 */
fun groupRestaurantHistory(visits: List<RestaurantVisit>): List<RestaurantHistory> {
    val named = visits.filter { restaurantNameKey(it.name).isNotEmpty() }

    val placeGroups = named
        .filter { it.placeId != null }
        .groupByTo(LinkedHashMap()) { it.placeId!! }
    val nameGroups = named
        .filter { it.placeId == null }
        .groupBy { restaurantNameKey(it.name) }

    // For each name key, the places that have a visit written that way
    val placesByKey = mutableMapOf<String, MutableSet<String>>()
    placeGroups.forEach { (placeId, placeVisits) ->
        placeVisits.forEach {
            placesByKey.getOrPut(restaurantNameKey(it.name)) { mutableSetOf() } += placeId
        }
    }

    val unfolded = mutableListOf<List<RestaurantVisit>>()
    nameGroups.forEach { (key, nameVisits) ->
        val place = placesByKey[key]?.singleOrNull()
        if (place != null) placeGroups.getValue(place) += nameVisits else unfolded += nameVisits
    }

    return placeGroups.map { (placeId, placeVisits) -> placeVisits.toHistory(placeId) } +
        unfolded.map { it.toHistory(placeId = null) }
}

private fun List<RestaurantVisit>.toHistory(placeId: String?): RestaurantHistory {
    // Dates are yyyy-mm-dd, so they sort as text; the ID breaks ties within a day
    val latest = maxWith(compareBy({ it.date }, { it.id }))
    return RestaurantHistory(
        name = latest.name,
        placeId = placeId,
        visitCount = size,
        lastVisited = latest.date,
        nameKey = restaurantNameKey(latest.name)
    )
}
