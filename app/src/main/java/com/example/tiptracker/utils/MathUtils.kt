package com.example.tiptracker.utils

import kotlin.math.round

fun Double.roundToTwoDecimals(): Double {
    return round(this * 100) / 100
}
