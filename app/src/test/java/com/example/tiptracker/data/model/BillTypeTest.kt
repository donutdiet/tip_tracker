package com.example.tiptracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BillTypeTest {

    @Test
    fun split_dividesTotalByParty() {
        assertEquals(25.0, totalPerPerson(100.0, 4, BillType.SPLIT), 0.0)
    }

    @Test
    fun separate_isTheUsersOwnCheck() {
        assertEquals(30.0, totalPerPerson(30.0, 4, BillType.SEPARATE), 0.0)
    }

    @Test
    fun partyOfOne_isTheSameEitherWay() {
        assertEquals(
            totalPerPerson(42.0, 1, BillType.SPLIT),
            totalPerPerson(42.0, 1, BillType.SEPARATE),
            0.0
        )
    }

    @Test
    fun split_withNoParty_isZeroInsteadOfInfinity() {
        assertEquals(0.0, totalPerPerson(100.0, 0, BillType.SPLIT), 0.0)
    }
}
