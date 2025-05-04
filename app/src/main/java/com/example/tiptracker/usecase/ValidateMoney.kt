package com.example.tiptracker.usecase

object ValidateMoney {
    private val MONEY_PATTERN = Regex("^\\d*(.\\d{2})?$")
    fun execute(bill: String): Boolean = MONEY_PATTERN.matches(bill)
}