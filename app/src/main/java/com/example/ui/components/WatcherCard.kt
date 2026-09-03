package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Watcher
import com.example.ui.theme.*
import com.example.util.DiffUtils
import com.example.util.ScheduleHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WatcherCard(
    watcher: Watcher,
    isChecking: Boolean,
    onCardClicked: () -> Unit,
    onOpenPageClicked: () -> Unit,
    onDismissOrCheckClicked: () -> Unit,
    onCheckNowClicked: () -> Unit,
    onEditClicked: () -> Unit,
    onToggleActiveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSimulateTestChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val diffResult = remember(watcher.lastHtml, watcher.previousHtml) {
        if (watcher.lastStatus == "CHANGED" && watcher.previousHtml != null && watcher.lastHtml != null) {
            val result = DiffUtils.computeDiff(watcher.previousHtml, watcher.lastHtml)
            val filtered = DiffUtils.filterHunksWithContext(result.lines, contextLines = 1)
            result.copy(lines = filtered)
        } else {
            null
        }
    }

    val timeFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val lastCheckedStr = watcher.lastCheckedTime?.let {
        timeFormat.format(Date(it))
    } ?: "Never checked"

    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(watcher.isActive) {
        if (watcher.isActive) {
            while (true) {
                currentTimeMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val scheduleList = remember(watcher.scheduleStartTime, watcher.intervalMinutes) {
        ScheduleHelper.createOneMonthSchedule(watcher.scheduleStartTime, watcher.intervalMinutes)
    }

    val closestNextTime = remember(scheduleList, currentTimeMs) {
        ScheduleHelper.getClosestNextScheduledTime(scheduleList, currentTimeMs)
            ?: ScheduleHelper.getNextScheduledTimeForWatcher(watcher.scheduleStartTime, watcher.intervalMinutes, currentTimeMs)
    }

    val remainingMs = (closestNextTime - currentTimeMs).coerceAtLeast(0L)
    val countdownText = remember(remainingMs) { ScheduleHelper.formatCountdown(remainingMs) }
    val totalScheduleRuns = remember(watcher.intervalMinutes) { ScheduleHelper.getScheduleCount(watcher.intervalMinutes) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(32.dp))
            .clickable { onCardClicked() }
            .testTag("watcher_card_${watcher.id}"),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = watcher.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (!watcher.isActive) {
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = BrandOnSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrandOutline)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = watcher.url.removePrefix("https://").removePrefix("http://"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = BrandOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Badge
                    when (watcher.lastStatus) {
                        "CHANGED" -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(BrandError)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CHANGE DETECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                        "SUCCESS" -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DiffAddedBg)
                                    .border(1.dp, DiffAddedBorder.copy(alpha = 0.4f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "UP TO DATE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DiffAddedText
                                    )
                                )
                            }
                        }
                        "ERROR" -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(DiffRemovedBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CHECK ERROR",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DiffRemovedText
                                    )
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(BrandPrimaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "READY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandOnPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // More Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Watcher Options",
                                tint = BrandOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Check Now") },
                                onClick = {
                                    menuExpanded = false
                                    onCheckNowClicked()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (watcher.isActive) "Pause Watcher" else "Resume Watcher") },
                                onClick = {
                                    menuExpanded = false
                                    onToggleActiveClicked()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (watcher.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Simulate Change (Test Diff)") },
                                onClick = {
                                    menuExpanded = false
                                    onSimulateTestChange()
                                },
                                leadingIcon = { Icon(Icons.Default.Science, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Watcher") },
                                onClick = {
                                    menuExpanded = false
                                    onEditClicked()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Watcher", color = BrandError) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClicked()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = BrandError) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1-Month Schedule Banner with Countdown Timer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BrandPrimaryContainer.copy(alpha = 0.5f))
                    .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "1-Month Schedule ($totalScheduleRuns runs)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandPrimary
                            )
                        }
                        Text(
                            text = if (watcher.isActive) "Next run in: $countdownText" else "Schedule Paused",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${watcher.intervalMinutes}m interval",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body: Diff or Summary Preview
            if (watcher.lastStatus == "CHANGED" && diffResult != null && diffResult.lines.isNotEmpty()) {
                DiffViewer(
                    diffLines = diffResult.lines,
                    headerTitle = "code diff",
                    intervalText = "Interval: ${watcher.intervalMinutes}m",
                    additions = watcher.lastAdditions,
                    deletions = watcher.lastDeletions,
                    maxPreviewLines = 6
                )
            } else if (watcher.lastStatus == "ERROR") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DiffRemovedBg)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = DiffRemovedText,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = watcher.lastErrorMessage ?: "Unable to fetch webpage content",
                            style = MaterialTheme.typography.bodySmall,
                            color = DiffRemovedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Calm Status Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last Check",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandOnSurfaceVariant
                            )
                            Text(
                                text = lastCheckedStr,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Interval: ${watcher.intervalMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BrandPrimaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenPageClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("open_page_button_${watcher.id}"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary
                    )
                ) {
                    Text(
                        text = "Open Page",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }

                Button(
                    onClick = onDismissOrCheckClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("action_button_${watcher.id}"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimaryContainer,
                        contentColor = BrandOnPrimaryContainer
                    )
                ) {
                    Text(
                        text = if (watcher.lastStatus == "CHANGED") "View Diff" else if (isChecking) "Checking..." else "Check Now",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}
