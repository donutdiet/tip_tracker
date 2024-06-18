package com.example.tiptracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat

class TipTrackerTests {
    @Test
    fun calculateTip_20PercentNoRoundup() {
        val amount = 10.00
        val tipPercent = 20.00
        val expectedTip = NumberFormat.getCurrencyInstance().format(2)
        val actualTip = calculateTip(amount, tipPercent, false, false)
        val actualTipString = NumberFormat.getCurrencyInstance().format(actualTip)
        assertEquals(expectedTip, actualTipString)
    }
}