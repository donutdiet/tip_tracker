package com.example.tiptracker.ui.tabs.home.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.components.LabeledSwitch
import com.example.tiptracker.ui.tabs.home.HomeAction
import com.example.tiptracker.ui.tabs.home.HomeUiState
import com.example.tiptracker.ui.theme.TipTrackerTheme
import com.example.tiptracker.ui.theme.numberFontFamily
import com.example.tiptracker.ui.theme.titleLargeMono

@Composable
fun TipCalculatorPage(
    uiState: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onContinue: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 4.dp)
    ) {
        OutlinedTextField(
            value = uiState.billAmount,
            onValueChange = { onAction(HomeAction.onBillAmountChange(it)) },
            label = {
                Text("Bill Amount", style = MaterialTheme.typography.titleSmall)
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.receipt_long),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tip Percent", style = MaterialTheme.typography.labelLarge)

            if (uiState.roundUpTip || uiState.roundUpTotal) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = MaterialTheme.typography.titleSmall.toSpanStyle().copy(
                                fontFamily = numberFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append("${uiState.formattedTrueTipPercent}%")
                        }
                        append("  after rounding up")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            PresetTipButton(
                percent = uiState.tipPreset1Percent,
                isSelected = uiState.tipPreset1,
                onClick = { onAction(HomeAction.onTipPreset1Toggle) },
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            )
            PresetTipButton(
                percent = uiState.tipPreset2Percent,
                isSelected = uiState.tipPreset2,
                onClick = { onAction(HomeAction.onTipPreset2Toggle) },
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            )
            OutlinedTextField(
                value = uiState.displayedTipPercent,
                onValueChange = { onAction(HomeAction.onTipPercentChange(it)) },
                placeholder = {
                    Text(
                        "Custom",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.partySize,
            onValueChange = { onAction(HomeAction.onPartySizeChange(it)) },
            label = {
                Text("Party Size", style = MaterialTheme.typography.titleSmall)
            },
            supportingText = {
                if ((uiState.partySize.toIntOrNull() ?: 0) > 1) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = MaterialTheme.typography.titleMedium.toSpanStyle().copy(
                                    fontFamily = numberFontFamily
                                )
                            ) {
                                append("$${uiState.formattedTotalPerPerson}")
                            }
                            append(" per person")
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Tip",
                fontFamily = numberFontFamily,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$${uiState.formattedTipAmount}",
                    style = MaterialTheme.typography.titleLargeMono
                )
                IconButton(onClick = { onAction(HomeAction.onRoundUpTipToggle) }) {
                    Icon(
                        painter = painterResource(R.drawable.decimal_increase),
                        contentDescription = "Round up tip amount",
                        tint = if (uiState.roundUpTip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceDim
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Total",
                fontFamily = numberFontFamily,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$${uiState.formattedTotal}",
                    style = MaterialTheme.typography.titleLargeMono
                )
                IconButton(onClick = { onAction(HomeAction.onRoundUpTotalToggle) }) {
                    Icon(
                        painter = painterResource(R.drawable.decimal_increase),
                        contentDescription = "Round up total amount",
                        tint = if (uiState.roundUpTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceDim
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { onAction(HomeAction.clear) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(2f)
            ) {
                Text("Clear", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onContinue() },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(3f)
            ) {
                Text(
                    "Write a review",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun PresetTipButton(
    percent: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            modifier = modifier
        ) {
            Text("$percent%", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = modifier
        ) {
            Text("$percent%", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TipCalculatorPagePreview() {
    TipTrackerTheme {
        TipCalculatorPage(
            uiState = HomeUiState(
                billAmount = "154.28",
                tipPreset1 = true,
                tipPreset2 = false,
                tipPercent = "15",
                partySize = "2",
                roundUpTip = true,
                roundUpTotal = false,
                restaurantName = "The Pearl",
                review = "Oysters",
                date = "2026-03-21",
                rating = 8.7,
                isSaving = false,
                tipPreset1Percent = 15,
                tipPreset2Percent = 20
            ),
            onAction = {},
            onContinue = {}
        )
    }
}
