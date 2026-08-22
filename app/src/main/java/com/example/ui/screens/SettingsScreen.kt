package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.NotificationHelper
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    isBatteryOptimized: Boolean,
    onEnableBatteryOptimization: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSyncAllNow: () -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "App Settings & Background Sync",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // 1. Notification Permission Card
        SettingsCard(
            title = "Notification Alerts",
            subtitle = "Receive instant alerts when webpage code modifications are detected.",
            icon = Icons.Default.NotificationsActive,
            actionLabel = "Test Notification",
            onAction = {
                NotificationHelper.notifyChangeDetected(
                    context = context,
                    watcherId = 1L,
                    watcherName = "VS Code Releases",
                    url = "https://code.visualstudio.com",
                    additions = 12,
                    deletions = 4
                )
            },
            secondaryActionLabel = "Permission Settings",
            onSecondaryAction = {
                onRequestNotificationPermission()
            }
        )

        // 2. Battery Optimization / Unrestricted Background Card
        SettingsCard(
            title = "Unrestricted Background Usage",
            subtitle = if (isBatteryOptimized) {
                "Active: WebWatch is exempt from battery saver and can run periodic checks reliably."
            } else {
                "Recommended: Allow unrestricted battery usage so Android does not delay background HTML fetching."
            },
            icon = if (isBatteryOptimized) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
            badgeText = if (isBatteryOptimized) "UNRESTRICTED" else "ACTION NEEDED",
            badgeColor = if (isBatteryOptimized) DiffAddedText else BrandError,
            badgeBg = if (isBatteryOptimized) DiffAddedBg else DiffRemovedBg,
            actionLabel = if (isBatteryOptimized) "Manage Battery Settings" else "Allow Unrestricted Usage",
            onAction = onEnableBatteryOptimization
        )

        // 3. Quick Actions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BrandOutline, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sync Controls",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onSyncAllNow,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary
                    )
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check All Watchers Now")
                }

                OutlinedButton(
                    onClick = onClearAllHistory,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = BrandError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Snapshot History", color = BrandError)
                }
            }
        }

        // 4. About Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BrandOutline, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "WebWatch v1.0",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Real-time webpage UI & HTML change tracking engine with GitHub-style code diffs, background sync workers, and instant alert notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandOnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String? = null,
    badgeColor: Color = DiffAddedText,
    badgeBg: Color = DiffAddedBg,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = BrandSurfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = BrandOnPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = BrandOnSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary
                    )
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }

                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(secondaryActionLabel, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
