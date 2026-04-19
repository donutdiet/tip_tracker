package com.example.tiptracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.model.RatingCount
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val MAX_Y_AXIS_LABEL_COUNT = 8

@Composable
fun RatingDistributionGraph(distribution: List<RatingCount>) {
    val isPreview = LocalInspectionMode.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val popupContainer = MaterialTheme.colorScheme.surfaceContainerHigh
    val popupTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface
    )
    val axisTextStyle = MaterialTheme.typography.bodySmall.copy(
        color = onSurfaceVariant
    )
    val progressiveLabelShiftStep = 0.6.dp
    val xBins = remember { 0..20 }
    val xAxisLabels = remember {
        (0..10).map { it.toString() }
    }

    val data = remember(distribution, xBins, primaryColor, isPreview) {
        val countByBin = distribution
            .groupBy { it.rating.toHalfBucketIndex() }
            .mapValues { (_, ratings) -> ratings.sumOf { it.count } }

        xBins.map { bin ->
            Bars(
                label = "",
                values = listOf(
                    Bars.Data(
                        value = (countByBin[bin] ?: 0).toDouble(),
                        color = SolidColor(primaryColor),
                        animator = Animatable(if (isPreview) 1f else 0f)
                    )
                )
            )
        }
    }

    val maxCount = remember(data) {
        data.maxOfOrNull { bar -> bar.values.maxOfOrNull { it.value } ?: 0.0 } ?: 0.0
    }
    val yAxisStep = remember(maxCount) {
        calculateYAxisStep(maxCount = maxCount)
    }
    val chartMaxValue = remember(maxCount, yAxisStep) {
        roundUpToStep(value = maxCount.coerceAtLeast(1.0), step = yAxisStep)
    }

    ColumnChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        data = data,
        barProperties = BarProperties(
            spacing = 0.dp,
            thickness = 12.dp,
        ),
        labelProperties = LabelProperties(
            enabled = true,
            labels = xAxisLabels,
            rotation = LabelProperties.Rotation(degree = 0f),
            // Actually don't look 🥸
            builder = { modifier, label, _, index ->
                val fineTuning = when (index) {
                    0 -> 2.2.dp
                    1 -> 1.4.dp
                    2 -> 1.4.dp
                    3 -> 1.4.dp
                    6 -> (-0.6).dp
                    8 -> (-0.8).dp
                    10 -> (-4.2).dp
                    else -> 0.dp
                }
                BasicText(
                    text = label,
                    modifier = modifier.offset(
                        x = (progressiveLabelShiftStep * index) + fineTuning
                    ),
                    style = axisTextStyle
                )
            }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = false
        ),
        gridProperties = GridProperties(
            enabled = false
        ),
        maxValue = chartMaxValue,
        minValue = 0.0,
        indicatorProperties = HorizontalIndicatorProperties(
            count = IndicatorCount.StepBased(stepBy = yAxisStep),
            textStyle = axisTextStyle,
            contentBuilder = { value -> value.roundToInt().toString() }
        ),
        popupProperties = PopupProperties(
            textStyle = popupTextStyle,
            containerColor = popupContainer,
            contentBuilder = { popup ->
                val count = popup.value.roundToInt()
                "${popup.dataIndex.toRatingBucketLabel()}: $count ${count.asLogLabel()}"
            }
        ),
        animationMode = AnimationMode.Together { 0L }
    )
}

private fun Double.toHalfBucketIndex(): Int {
    return floor(this * 2).toInt().coerceIn(0, 20)
}

private fun calculateYAxisStep(
    maxCount: Double,
    maxLabelCount: Int = MAX_Y_AXIS_LABEL_COUNT
): Double {
    val maxWholeCount = maxCount.roundToInt().coerceAtLeast(1)
    val intervalCount = (maxLabelCount - 1).coerceAtLeast(1)
    return ceil(maxWholeCount.toDouble() / intervalCount).coerceAtLeast(1.0)
}

private fun roundUpToStep(value: Double, step: Double): Double {
    return ceil(value / step) * step
}

private fun Int.toRatingBucketLabel(): String {
    val lowerBound = this / 2.0
    val upperBound = (this + 1) / 2.0
    return "%.1f-%.1f".format(lowerBound, upperBound)
}

private fun Int.asLogLabel(): String {
    return if (this == 1) "log" else "logs"
}