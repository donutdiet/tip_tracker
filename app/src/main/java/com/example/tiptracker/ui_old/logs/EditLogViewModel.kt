package com.example.tiptracker.ui_old.logs

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.tiptracker.ui_old.DiningLogData
import java.util.UUID

class EditLogViewModel : ViewModel() {
    var tempBillAmount = mutableStateOf("")
        private set
    var tempTipAmount = mutableStateOf("")
        private set
    var tempPersonCount = mutableStateOf("")
        private set
    var tempRestaurantName = mutableStateOf("")
        private set
    var tempRestaurantDescription = mutableStateOf("")
        private set
    var tempDate = mutableStateOf("")
        private set

    lateinit var id: UUID

    fun onBillAmountChange(newAmount: String) {
        tempBillAmount.value = newAmount
    }

    fun onTipAmountChange(newTip: String) {
        tempTipAmount.value = newTip
    }

    fun onPersonCountChange(newCount: String) {
        tempPersonCount.value = newCount
    }

    fun onRestaurantNameChange(newName: String) {
        tempRestaurantName.value = newName
    }

    fun onRestaurantDescriptionChange(newDescription: String) {
        tempRestaurantDescription.value = newDescription
    }

    fun onDateChange(newDate: String) {
        tempDate.value = newDate
    }

    fun getCalculatedTipPercent(): Double {
        val billAmountDouble = tempBillAmount.value.toDoubleOrNull() ?: 0.0
        val tipAmountDouble = tempTipAmount.value.toDoubleOrNull() ?: 0.0
        return if (billAmountDouble > 0) {
            val tipPercent = (tipAmountDouble / billAmountDouble) * 100
            Math.round(tipPercent * 100) / 100.0
        } else {
            0.0
        }
    }

fun getCalculatedTotal(): Double {
    val billAmountDouble = tempBillAmount.value.toDoubleOrNull() ?: 0.0
    val tipAmountDouble = tempTipAmount.value.toDoubleOrNull() ?: 0.0
    return Math.round((billAmountDouble + tipAmountDouble) * 100) / 100.0
}

fun getCalculatedTotalPerPerson(): Double {
    val total = getCalculatedTotal()
    val partySize = tempPersonCount.value.toIntOrNull() ?: 1
    return Math.round((total / partySize) * 100) / 100.0
}

fun loadCurrentLogData(diningLogs: List<DiningLogData>, index: Int) {
    if (index != -1) {
        tempBillAmount.value = diningLogs[index].billAmount.toString()
        tempTipAmount.value = diningLogs[index].tipAmount.toString()
        tempPersonCount.value = diningLogs[index].personCount.toString()
        tempRestaurantName.value = diningLogs[index].restaurantName
        tempRestaurantDescription.value = diningLogs[index].restaurantDescription
        tempDate.value = diningLogs[index].date
        id = diningLogs[index].id
    }
}

fun clearForm() {
    tempBillAmount.value = ""
    tempTipAmount.value = ""
    tempPersonCount.value = ""
    tempRestaurantName.value = ""
    tempRestaurantDescription.value = ""
    tempDate.value = ""
}
}