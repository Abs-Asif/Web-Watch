package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Watcher
import com.example.ui.components.BackgroundSyncBanner
import com.example.ui.components.WatcherCard
import com.example.ui.theme.*

@Composable
fun WatchersScreen(
    watchers: List<Watcher>,
    isBatteryOptimized: Boolean,
    isChecking: Boolean,
    checkingWatcherId: Long?,
    onEnableBatteryOptimization: () -> Unit,
    onWatcherSelected: (Long) -> Unit,
    onCheckWatcherNow: (Long) -> Unit,
    onEditWatcher: (Watcher) -> Unit,
    onToggleWatcherActive: (Watcher) -> Unit,
    onDeleteWatcher: (Watcher) -> Unit,
    onSimulateTestChange: (Long) -> Unit,
    onMarkReviewed: (Long) -> Unit,
    onAddNewClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Background sync & battery optimization banner
            item {
                BackgroundSyncBanner(
                    isBatteryOptimized = isBatteryOptimized,
                    onEnableClicked = onEnableBatteryOptimization
                )
            }

            // Summary Stats Row
            item {
                val changesCount = watchers.count { it.lastStatus == "CHANGED" }
                val activeCount = watchers.count { it.isActive }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$activeCount active watchers",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandOnSurfaceVariant
                        )
                    )

                    if (changesCount > 0) {
                        Text(
                            text = "$changesCount change${if (changesCount > 1) "s" else ""} detected",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandError
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DiffRemovedBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Watcher Cards
            if (watchers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = BrandPrimaryContainer,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No Web Watchers Added",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add any webpage URL to track HTML code changes and get notified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onAddNewClicked,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandPrimary,
                                    contentColor = BrandOnPrimary
                                )
                            ) {
                                Text("Add Your First Watcher")
                            }
                        }
                    }
                }
            } else {
                items(watchers, key = { it.id }) { watcher ->
                    WatcherCard(
                        watcher = watcher,
                        isChecking = isChecking && checkingWatcherId == watcher.id,
                        onCardClicked = { onWatcherSelected(watcher.id) },
                        onOpenPageClicked = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watcher.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismissOrCheckClicked = {
                            if (watcher.lastStatus == "CHANGED") {
                                onWatcherSelected(watcher.id)
                            } else {
                                onCheckWatcherNow(watcher.id)
                            }
                        },
                        onCheckNowClicked = { onCheckWatcherNow(watcher.id) },
                        onEditClicked = { onEditWatcher(watcher) },
                        onToggleActiveClicked = { onToggleWatcherActive(watcher) },
                        onDeleteClicked = { onDeleteWatcher(watcher) },
                        onSimulateTestChange = { onSimulateTestChange(watcher.id) }
                    )
                }
            }
        }

        // Floating Action Button (rounded-2xl matching design HTML)
        FloatingActionButton(
            onClick = onAddNewClicked,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 96.dp, end = 24.dp)
                .size(56.dp)
                .testTag("fab_add_watcher"),
            shape = RoundedCornerShape(16.dp),
            containerColor = BrandPrimaryContainer,
            contentColor = BrandOnPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Watcher",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
