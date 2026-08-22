package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Watcher
import com.example.ui.DiffDisplayMode
import com.example.ui.components.DiffLineRow
import com.example.ui.components.RawHtmlViewer
import com.example.ui.components.SideBySideDiffViewer
import com.example.ui.theme.*
import com.example.util.DiffUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffDetailScreen(
    watcher: Watcher,
    diffMode: DiffDisplayMode,
    onlyChangedLines: Boolean,
    onBackClicked: () -> Unit,
    onModeChanged: (DiffDisplayMode) -> Unit,
    onToggleOnlyChanged: () -> Unit,
    onMarkReviewed: () -> Unit,
    onCheckNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept back key to return to watcher list
    BackHandler(onBack = onBackClicked)

    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm:ss", Locale.getDefault()) }

    val fullDiffResult = remember(watcher.previousHtml, watcher.lastHtml, watcher.stripScripts, watcher.formatHtml) {
        val old = watcher.previousHtml ?: ""
        val new = watcher.lastHtml ?: ""
        DiffUtils.computeDiff(old, new)
    }

    val displayLines = remember(fullDiffResult, onlyChangedLines) {
        if (onlyChangedLines) {
            DiffUtils.filterHunksWithContext(fullDiffResult.lines, contextLines = 3)
        } else {
            fullDiffResult.lines
        }
    }

    val sideBySideRows = remember(displayLines) {
        DiffUtils.computeSideBySideRows(displayLines)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = watcher.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = watcher.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandOnSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked, modifier = Modifier.testTag("diff_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCheckNow) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check now", tint = BrandPrimary)
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val diffText = fullDiffResult.lines.joinToString("\n") {
                                val prefix = when (it.type) {
                                    com.example.util.DiffType.ADDED -> "+"
                                    com.example.util.DiffType.DELETED -> "-"
                                    com.example.util.DiffType.UNCHANGED -> " "
                                }
                                "$prefix ${it.content}"
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Diff", diffText))
                            Toast.makeText(context, "Diff copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy diff")
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watcher.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandSurface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BrandSurfaceVariant,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(watcher.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open URL", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Page")
                    }

                    Button(
                        onClick = onMarkReviewed,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimaryContainer,
                            contentColor = BrandOnPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dismiss")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BrandBackground)
        ) {
            // Stats and Switcher Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Change Stats Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Additions Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DiffAddedBorder)
                            )
                            Text(
                                text = "+${fullDiffResult.additions}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DiffAddedText
                                )
                            )
                        }

                        // Deletions Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(DiffRemovedBorder)
                            )
                            Text(
                                text = "-${fullDiffResult.deletions}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = DiffRemovedText
                                )
                            )
                        }

                        // Total lines
                        Text(
                            text = "${fullDiffResult.lines.size} total lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandOnSurfaceVariant
                        )
                    }

                    val lastCheckTimeStr = watcher.lastCheckedTime?.let { timeFormat.format(Date(it)) } ?: "Recent"
                    Text(
                        text = lastCheckTimeStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = BrandOnSurfaceVariant
                    )
                }

                // Mode Tabs Segmented Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Unified Diff Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(50),
                        color = if (diffMode == DiffDisplayMode.UNIFIED) BrandPrimary else Color.Transparent,
                        onClick = { onModeChanged(DiffDisplayMode.UNIFIED) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Difference,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (diffMode == DiffDisplayMode.UNIFIED) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                                Text(
                                    text = "Unified",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (diffMode == DiffDisplayMode.UNIFIED) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Side-by-Side Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(50),
                        color = if (diffMode == DiffDisplayMode.SIDE_BY_SIDE) BrandPrimary else Color.Transparent,
                        onClick = { onModeChanged(DiffDisplayMode.SIDE_BY_SIDE) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (diffMode == DiffDisplayMode.SIDE_BY_SIDE) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                                Text(
                                    text = "Side-by-Side",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (diffMode == DiffDisplayMode.SIDE_BY_SIDE) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Raw HTML Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(50),
                        color = if (diffMode == DiffDisplayMode.RAW_HTML) BrandPrimary else Color.Transparent,
                        onClick = { onModeChanged(DiffDisplayMode.RAW_HTML) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (diffMode == DiffDisplayMode.RAW_HTML) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                                Text(
                                    text = "Raw HTML",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (diffMode == DiffDisplayMode.RAW_HTML) BrandOnPrimary else BrandOnSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Filter option for Unified and Side-by-Side
                if (diffMode == DiffDisplayMode.UNIFIED || diffMode == DiffDisplayMode.SIDE_BY_SIDE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (onlyChangedLines) "Showing changed sections & context (3 lines)" else "Showing all lines in document",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = BrandOnSurfaceVariant
                        )

                        TextButton(
                            onClick = onToggleOnlyChanged,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (onlyChangedLines) Icons.Default.FormatAlignLeft else Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BrandPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (onlyChangedLines) "Show Full Diff" else "Only Changed",
                                fontSize = 11.sp,
                                color = BrandPrimary
                            )
                        }
                    }
                }
            }

            // Main Diff Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                when (diffMode) {
                    DiffDisplayMode.UNIFIED -> {
                        val horizontalScroll = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
                                .background(Color.White)
                        ) {
                            // Header bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DiffHeaderBg)
                                    .border(width = 0.5.dp, color = BrandOutline)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HTML Unified Diff",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrandOnSurfaceVariant
                                )
                                Text(
                                    text = "${displayLines.size} lines displayed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandOnSurfaceVariant
                                )
                            }

                            if (displayLines.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No content differences detected in this snapshot.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandOnSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .horizontalScroll(horizontalScroll)
                                ) {
                                    items(displayLines) { line ->
                                        DiffLineRow(line = line)
                                    }
                                }
                            }
                        }
                    }

                    DiffDisplayMode.SIDE_BY_SIDE -> {
                        SideBySideDiffViewer(rows = sideBySideRows)
                    }

                    DiffDisplayMode.RAW_HTML -> {
                        RawHtmlViewer(
                            currentHtml = watcher.lastHtml,
                            previousHtml = watcher.previousHtml
                        )
                    }
                }
            }
        }
    }
}
