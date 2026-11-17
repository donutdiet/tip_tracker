package com.example.tiptracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.R
import com.example.tiptracker.ui.theme.TipTrackerTheme

/**
 * The app's confirmation dialog: an icon badge, a centered title and message, and two
 * side-by-side buttons styled like the rest of the app's buttons.
 *
 * @param destructive colors the confirm button with the error color, for actions that throw
 *   something away.
 * @param destructiveIcon colors the icon badge with the error color too; otherwise it is
 *   neutral. Defaults to [destructive]; turn it off when the icon itself isn't a warning
 *   (e.g. a pencil).
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @DrawableRes icon: Int,
    destructive: Boolean = false,
    destructiveIcon: Boolean = destructive,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val confirmColor = if (destructive) colors.error else colors.primary
    // Neutral (near-black in light mode, near-white in dark) unless the icon is a warning
    val iconColor = if (destructiveIcon) colors.error else colors.onSurface
    val iconContainer = if (destructiveIcon) colors.errorContainer else colors.surfaceContainerHighest
    val buttonShape = RoundedCornerShape(4.dp)
    // Less side padding than the default 24dp so two labels like "Keep editing" fit on one line
    val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconContainer)
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        // Both buttons live in one row so they share the width evenly
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = enabled,
                    shape = buttonShape,
                    contentPadding = buttonPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    ButtonLabel(dismissText)
                }
                Button(
                    onClick = onConfirm,
                    enabled = enabled,
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmColor,
                        contentColor = if (destructive) colors.onError else colors.onPrimary
                    ),
                    contentPadding = buttonPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    ButtonLabel(confirmText)
                }
            }
        }
    )
}

/** One line, centered; shrinks to an ellipsis rather than wrapping and unevening the buttons. */
@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
private fun ConfirmationDialogPreview() {
    TipTrackerTheme {
        ConfirmationDialog(
            title = "Delete log?",
            message = "This log and its photos will be permanently deleted.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {},
            onDismiss = {},
            icon = R.drawable.delete,
            destructive = true
        )
    }
}
