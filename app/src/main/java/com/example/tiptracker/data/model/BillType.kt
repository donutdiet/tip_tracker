package com.example.tiptracker.data.model

/**
 * How the party paid. Stored by name as TEXT, so new values can be added without a migration,
 * but existing constants must never be renamed (raw SQL in LogDao also compares against them).
 */
enum class BillType {
    // One bill shared evenly: bill/total cover the whole party
    SPLIT,

    // Everyone paid their own check: bill/total cover only the user's own check
    SEPARATE
}

/** The per-person cost: a split bill divided by the party, or the user's own separate check. */
fun totalPerPerson(total: Double, partySize: Int, billType: BillType): Double = when {
    billType == BillType.SEPARATE -> total
    partySize > 0 -> total / partySize
    else -> 0.0
}
