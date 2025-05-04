package com.example.tiptracker.ui_old.addentry

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.tiptracker.ui_old.DiningLogData
import com.example.tiptracker.ui_old.UserStats
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.ceil

class LogViewModel : ViewModel() {
    var diningLogs = mutableStateListOf<DiningLogData>()
    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
        loadLogs()
        UserStats.updateUserStats(diningLogs)
        UserStats.updateLogRecords(diningLogs)
    }

    private fun saveLogs() {
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(diningLogs)
        editor.putString("dining_logs", json)
        editor.apply()
    }

    private fun loadLogs() {
        val gson = Gson()
        val json = sharedPreferences.getString("dining_logs", "")
        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<DiningLogData>>() {}.type
            val logs: List<DiningLogData> = gson.fromJson(json, type)
            diningLogs.clear()
            diningLogs.addAll(logs)
        }
    }

    var billAmount = mutableStateOf("")
        private set
    var tipPercent = mutableStateOf("")
        private set
    var personCount = mutableStateOf("")
        private set
    var roundUpTip = mutableStateOf(false)
        private set
    var roundUpTotal = mutableStateOf(false)
        private set

    var restaurantName = mutableStateOf("")
        private set
    var restaurantDescription = mutableStateOf("")
        private set
    var date = mutableStateOf("")
        private set

    fun onBillAmountChange(newAmount: String) {
        billAmount.value = newAmount
    }

    fun onTipPercentChange(newPercent: String) {
        tipPercent.value = newPercent
    }

    fun onPersonCountChange(newCount: String) {
        personCount.value = newCount
    }

    fun onRoundUpTipChange(newOption: Boolean) {
        roundUpTip.value = newOption
        if (newOption) {
            roundUpTotal.value = false
        }
    }

    fun onRoundUpTotalChange(newOption: Boolean) {
        roundUpTotal.value = newOption
        if (newOption) {
            roundUpTip.value = false
        }
    }

    fun onRestaurantNameChange(newName: String) {
        restaurantName.value = newName
    }

    fun onRestaurantDescriptionChange(newDescription: String) {
        restaurantDescription.value = newDescription
    }

    fun onDateChange(newDate: String) {
        date.value = newDate
    }

    fun onFavoriteButtonClicked(index: Int) {
        diningLogs[index].favorite = !diningLogs[index].favorite
        saveLogs()
    }

    fun getCalculatedTip(): Double {
        val billAmount = billAmount.value.toDoubleOrNull() ?: 0.0
        val tipPercent = tipPercent.value.toDoubleOrNull() ?: 0.0
        var tip = Math.round((billAmount * tipPercent / 100) * 100) / 100.0
        if (roundUpTip.value) {
            tip = ceil(tip)
        } else if (roundUpTotal.value) {
            val total = billAmount + tip
            val roundedTotal = ceil(total)
            tip = tip + roundedTotal - total
        }
        return tip
    }

    fun getCalculatedTotal(): Double {
        val billAmount = billAmount.value.toDoubleOrNull() ?: 0.0
        val tip = getCalculatedTip()
        val total = Math.round((billAmount + tip) * 100) / 100.0
        return total
    }

    fun getCalculatedTotalPerPerson(): Double {
        val total = getCalculatedTotal()
        val personCount = personCount.value.toIntOrNull() ?: 1
        val totalPerPerson = Math.round((total / personCount) * 100) / 100.0
        return totalPerPerson
    }

    private fun getActualTipPercent(): Double {
        var tipPercent = tipPercent.value.toDoubleOrNull() ?: 0.0
        val billAmount = billAmount.value.toDoubleOrNull() ?: 0.0
        if ((roundUpTip.value || roundUpTotal.value) && billAmount > 0) {
            tipPercent = getCalculatedTip() / billAmount * 100
            tipPercent = Math.round(tipPercent * 100) / 100.0
        }
        return tipPercent
    }

    fun checkFormValidity(): Boolean {
        val billAmountDouble = billAmount.value.toDoubleOrNull() ?: 0.0
        val tipPercentDouble = tipPercent.value.toDoubleOrNull() ?: 0.0
        // Ensure party size is an integer; default to 1 if left empty
        var partySize = personCount.value.toIntOrNull()
        if (personCount.value.isEmpty()) partySize = 1
        return billAmountDouble >= 0.0 && tipPercentDouble >= 0.0 && partySize != null && partySize >= 1
    }

    fun clearForm() {
        billAmount.value = ""
        tipPercent.value = ""
        personCount.value = ""
        roundUpTip.value = false
        roundUpTotal.value = false
        restaurantName.value = ""
        restaurantDescription.value = ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateCurrentDate() {
        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        date.value = currentDate.format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addLogEntry() {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        val billAmountDouble = billAmount.value.toDoubleOrNull() ?: 0.0
        val tipPercentDouble = getActualTipPercent()
        val tipAmountDouble = getCalculatedTip()
        val totalAmountDouble = getCalculatedTotal()
        val totalPerPersonDouble = getCalculatedTotalPerPerson()
        val diningEntry = DiningLogData(
            billAmount = billAmountDouble,
            tipPercent = tipPercentDouble,
            tipAmount = tipAmountDouble,
            personCount = personCount.value.toIntOrNull() ?: 1,
            totalAmount = totalAmountDouble,
            totalAmountPerPerson = totalPerPersonDouble,
            restaurantName = restaurantName.value,
            restaurantDescription = restaurantDescription.value,
            date = date.value
        )
        diningLogs.add(diningEntry)
        diningLogs.sortByDescending { LocalDate.parse(it.date, formatter) }
        saveLogs()
        UserStats.updateUserStats(diningLogs)
        UserStats.updateLogRecords(diningLogs)
        clearForm()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateEntry(
        id: UUID,
        newBillAmount: String,
        newTipPercent: Double,
        newTipAmount: String,
        newPersonCount: String,
        newTotalAmount: Double,
        newTotalAmountPerPerson: Double,
        newRestaurantName: String,
        newRestaurantDescription: String,
        newDate: String
    ) {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        val index = diningLogs.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldEntry = diningLogs[index]

            val newBillAmountDouble = newBillAmount.toDoubleOrNull() ?: 0.0
            val newTipAmountDouble = newTipAmount.toDoubleOrNull() ?: 0.0
            val newPersonCountInt = newPersonCount.toIntOrNull() ?: 1

            val updatedDiningEntry = oldEntry.copy(
                billAmount = newBillAmountDouble,
                tipPercent = newTipPercent,
                tipAmount = newTipAmountDouble,
                personCount = newPersonCountInt,
                totalAmount = newTotalAmount,
                totalAmountPerPerson = newTotalAmountPerPerson,
                restaurantName = newRestaurantName,
                restaurantDescription = newRestaurantDescription,
                date = newDate
            )

            // Print debug information
            println("Old Entry: $oldEntry")
            println("Updated Entry: $updatedDiningEntry")

            diningLogs[index] = updatedDiningEntry
            diningLogs.sortByDescending { LocalDate.parse(it.date, formatter) }

            saveLogs()
            UserStats.updateUserStats(diningLogs)
            UserStats.updateLogRecords(diningLogs)
        } else {
            println("Entry with ID $id not found.")
        }
    }

    fun deleteEntry(index: Int) {
        diningLogs.removeAt(index)
        saveLogs()
        UserStats.updateUserStats(diningLogs)
        UserStats.updateLogRecords(diningLogs)
    }
}