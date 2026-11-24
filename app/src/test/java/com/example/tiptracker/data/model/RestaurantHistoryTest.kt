package com.example.tiptracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RestaurantHistoryTest {

    private var nextId = 1
    private fun visit(name: String, placeId: String? = null, date: String = "2026-01-01") =
        RestaurantVisit(nextId++, name, placeId, date)

    private fun List<RestaurantHistory>.summary() =
        map { "${it.name}|${it.placeId}|${it.visitCount}|${it.lastVisited}" }.sorted()

    @Test
    fun `name key ignores case, punctuation and spacing`() {
        assertEquals("the pearl", restaurantNameKey("  The   Pearl! "))
        assertEquals("joes diner", restaurantNameKey("Joe's Diner"))
        assertEquals("café 21", restaurantNameKey("Café #21"))
    }

    @Test
    fun `unlinked visits group by name, showing the latest spelling`() {
        val rows = groupRestaurantHistory(
            listOf(
                visit("The Pearl", date = "2026-01-01"),
                visit("the pearl!", date = "2026-03-01"),
                visit("Pearl Bakery", date = "2026-02-01")
            )
        )

        assertEquals(
            listOf("Pearl Bakery|null|1|2026-02-01", "the pearl!|null|2|2026-03-01"),
            rows.summary()
        )
    }

    @Test
    fun `linked visits group by place, so chain locations stay apart`() {
        val rows = groupRestaurantHistory(
            listOf(
                visit("Starbucks", placeId = "downtown"),
                visit("Starbucks", placeId = "airport"),
                visit("Starbucks Downtown", placeId = "downtown", date = "2026-02-01")
            )
        )

        assertEquals(
            listOf("Starbucks Downtown|downtown|2|2026-02-01", "Starbucks|airport|1|2026-01-01"),
            rows.summary()
        )
    }

    @Test
    fun `an unlinked name joins the one place that uses it`() {
        val rows = groupRestaurantHistory(
            listOf(
                visit("The Pearl", placeId = "pearl", date = "2026-01-01"),
                visit("the pearl", date = "2026-02-01")
            )
        )

        // The newest visit's spelling wins, even though that visit was the unlinked one
        assertEquals(listOf("the pearl|pearl|2|2026-02-01"), rows.summary())
    }

    @Test
    fun `an unlinked name used at several places stays on its own`() {
        val rows = groupRestaurantHistory(
            listOf(
                visit("Starbucks", placeId = "downtown"),
                visit("Starbucks", placeId = "airport"),
                visit("starbucks")
            )
        )

        assertEquals(
            listOf(
                "Starbucks|airport|1|2026-01-01",
                "Starbucks|downtown|1|2026-01-01",
                "starbucks|null|1|2026-01-01"
            ),
            rows.summary()
        )
    }

    @Test
    fun `names without letters or digits are skipped`() {
        val rows = groupRestaurantHistory(listOf(visit(""), visit("  "), visit("!!")))

        assertEquals(emptyList<String>(), rows.summary())
    }
}
