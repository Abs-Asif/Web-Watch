package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SnapshotHistory
import com.example.ui.theme.*
import com.example.util.DiffUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SnapshotDetailDialog(
    snapshot: SnapshotHistory,
    onDismiss: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault()) }

    val diffResult = remember(snapshot.oldHtml, snapshot.newHtml) {
        if (!snapshot.oldHtml.isNullOrEmpty() || !snapshot.newHtml.isNullOrEmpty()) {
            DiffUtils.computeDiff(snapshot.oldHtml, snapshot.newHtml)
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, BrandOutline, RoundedCornerShape(28.dp)),
            color = BrandSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = snapshot.watcherName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = timeFormat.format(Date(snapshot.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandOnSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Diff Viewer
                if (diffResult != null && diffResult.lines.isNotEmpty()) {
                    DiffViewer(
                        diffLines = diffResult.lines,
                        headerTitle = "${snapshot.watcherName} diff",
                        additions = snapshot.additions,
                        deletions = snapshot.deletions,
                        modifier = Modifier.weight(1f)
                    )
                } else if (snapshot.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DiffRemovedBg)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${snapshot.errorMessage}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DiffRemovedText
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandSurfaceVariant)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = snapshot.diffSummary ?: "No diff data available for this check.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandOnSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}
