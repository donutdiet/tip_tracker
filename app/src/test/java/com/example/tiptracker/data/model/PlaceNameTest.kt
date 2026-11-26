package com.example.tiptracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceNameTest {

    @Test
    fun `nothing to choose for the first log at a place or when the names match`() {
        assertNull(placeNameChoices(emptyList(), typed = "The Pearl"))
        // Spacing alone isn't a different name
        assertNull(placeNameChoices(listOf("The  Pearl ", "The Pearl"), typed = "The Pearl"))
    }

    @Test
    fun `the name being saved comes first, then the others by use`() {
        assertEquals(
            listOf(NameCount("BWW", 2), NameCount("Buffalo Wild Wings", 3), NameCount("Wings", 1)),
            placeNameChoices(
                listOf("Wings", "Buffalo Wild Wings", "BWW", "Buffalo Wild Wings", "Buffalo Wild Wings"),
                typed = "BWW"
            )
        )
    }

    @Test
    fun `case and punctuation make a different name`() {
        assertEquals(
            listOf(NameCount("joes", 1), NameCount("Joe's", 1)),
            placeNameChoices(listOf("Joe's"), typed = "joes")
        )
    }

    @Test
    fun `a blank name never renames the place`() {
        assertNull(placeNameChoices(listOf("The Pearl"), typed = "   "))
    }
}
