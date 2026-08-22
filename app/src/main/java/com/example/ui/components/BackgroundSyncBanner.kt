package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandOnPrimary
import com.example.ui.theme.BrandOnPrimaryContainer
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryContainer

@Composable
fun BackgroundSyncBanner(
    isBatteryOptimized: Boolean,
    onEnableClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isBatteryOptimized) BrandPrimaryContainer.copy(alpha = 0.65f) else BrandPrimaryContainer
    val title = if (isBatteryOptimized) "Background sync active" else "Unrestricted background usage needed"
    val subtitle = if (isBatteryOptimized) {
        "Unrestricted battery usage enabled"
    } else {
        "Tap to allow background checks without OS sleep limits"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(bgColor)
            .clickable { onEnableClicked() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("battery_sync_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBatteryOptimized) Icons.Default.Info else Icons.Default.BatteryChargingFull,
                    contentDescription = "Background sync status",
                    tint = BrandOnPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = BrandOnPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = BrandOnPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
