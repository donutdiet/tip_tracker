package com.example.tiptracker.ui.tabs.profile

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiptracker.R
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.data.model.LogStats
import com.example.tiptracker.data.model.RatingCount
import com.example.tiptracker.data.repository.LogRepository
import com.example.tiptracker.utils.formatCurrency
import com.example.tiptracker.utils.formatTipPercent
import com.example.tiptracker.utils.roundToTwoDecimals
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class Award(
    val title: String,
    val log: Log,
    val metric: String,
    @param:DrawableRes val iconRes: Int? = null
)

data class AwardsUiState(
    val highestSpendPerPerson: Log? = null,
    val mostGenerousTip: Log? = null,
    val largestPartySize: Log? = null,
    val topRated: Log? = null,
    val lengthiestReview: Log? = null
) {
    val allAwards: List<Award> = listOfNotNull(
        highestSpendPerPerson?.let {
            Award(
                title = "Highest Spend Per Person",
                log = it,
                metric = "$${formatCurrency((it.total / it.partySize).roundToTwoDecimals())}/ea"
            )
        },
        mostGenerousTip?.let {
            Award(
                title = "Most Generous Tip",
                log = it,
                metric = "${formatTipPercent(it.tipPercent)}%"
            )
        },
        largestPartySize?.let {
            Award(
                title = "Largest Party",
                log = it,
                metric = it.partySize.toString(),
                iconRes = R.drawable.person
            )
        },
        topRated?.let {
            Award(
                title = "Top Rated",
                log = it,
                metric = it.rating.toString(),
                iconRes = R.drawable.star
            )
        },
        lengthiestReview?.let {
            Award(
                title = "Longest Review",
                log = it,
                metric = "${it.review.length} characters"
            )
        }
    )
}

data class ProfileUiState(
    val logStats: LogStats = LogStats(
        totalLogs = 0,
        avgBill = 0.0,
        avgTipPercent = 0.0,
        avgTotal = 0.0,
        avgPartySize = 1.0,
        avgRating = 5.0,
        avgTipAmount = 0.0,
        avgTotalPerPerson = 0.0
    ),
    val ratingDistribution: List<RatingCount> = emptyList(),
    val awards: AwardsUiState = AwardsUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ProfileViewModel(
    logsRepository: LogRepository
) : ViewModel() {
    private val awardsFlow = combine(
        logsRepository.getHighestSpendPerPersonLog(),
        logsRepository.getMostGenerousTipLog(),
        logsRepository.getLargestPartySizeLog(),
        logsRepository.getTopRatedLog(),
        logsRepository.getLongestReviewLog()
    ) { highestSpend, generousTip, largestParty, topRated, longestReview ->
        AwardsUiState(
            highestSpendPerPerson = highestSpend,
            mostGenerousTip = generousTip,
            largestPartySize = largestParty,
            topRated = topRated,
            lengthiestReview = longestReview
        )
    }

    val uiState = combine(
        logsRepository.getLogStats(),
        logsRepository.getRatingDistribution(),
        awardsFlow
    ) { stats, ratingDistribution, awards ->
        ProfileUiState(
            logStats = stats,
            ratingDistribution = ratingDistribution,
            awards = awards,
            isLoading = false
        )
    }
        .catch { e ->
            emit(
                ProfileUiState(
                    isLoading = false,
                    errorMessage = "Couldn't load profile data. Please try again."
                )
            )
            android.util.Log.e("ProfileViewModel", "Error loading profile data", e)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )
}