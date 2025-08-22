import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tiptracker.data.entity.Log
import com.example.tiptracker.ui.theme.TipTrackerTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LogListItem(
    log: Log,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListItem(
            headlineContent = {
                Text(
                    text = log.restaurantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            supportingContent = {
                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.date.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Party size",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = log.partySize.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "10",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            trailingContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Space between total and icon
                ) {
                    Text(
                        text = "$%.2f".format(log.total),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Go to details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(log.id) }
        )
        // Optional: Divider after each list item
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewLogListItemMaterial3() {
    TipTrackerTheme {
        Column {
            LogListItem(
                log = Log(
                    id = 1,
                    bill = 75.50,
                    tipPercent = 0.18,
                    partySize = 3,
                    tip = 13.59,
                    total = 89.09,
                    perPerson = 29.70,
                    restaurantName = "Gourmet Garden Bistro",
                    restaurantDescription = "A delightful culinary experience.",
                    date = LocalDate.of(2025, 7, 27)
                ),
                onItemClick = { logId -> println("Clicked log ID: $logId") }
            )
            LogListItem(
                log = Log(
                    id = 2,
                    bill = 25.00,
                    tipPercent = 0.10,
                    partySize = 1,
                    tip = 2.50,
                    total = 27.50,
                    perPerson = 27.50,
                    restaurantName = "Quick Bites Cafe",
                    restaurantDescription = "",
                    date = LocalDate.of(2025, 7, 26)
                ),
                onItemClick = { logId -> println("Clicked log ID: $logId") }
            )
        }
    }
}