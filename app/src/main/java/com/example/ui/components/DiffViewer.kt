package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.DiffLine
import com.example.util.DiffType

@Composable
fun DiffViewer(
    diffLines: List<DiffLine>,
    modifier: Modifier = Modifier,
    headerTitle: String = "index.html",
    intervalText: String? = null,
    additions: Int? = null,
    deletions: Int? = null,
    maxPreviewLines: Int? = null
) {
    val displayLines = if (maxPreviewLines != null && diffLines.size > maxPreviewLines) {
        diffLines.take(maxPreviewLines)
    } else {
        diffLines
    }

    val horizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
            .background(Color.White)
            .testTag("github_diff_viewer")
    ) {
        // Diff Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiffHeaderBg)
                .border(width = 0.5.dp, color = BrandOutline)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = BrandOnSurfaceVariant
                )

                if (additions != null && additions > 0) {
                    Text(
                        text = "+$additions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DiffAddedText
                    )
                }
                if (deletions != null && deletions > 0) {
                    Text(
                        text = "-$deletions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DiffRemovedText
                    )
                }
            }

            if (intervalText != null) {
                Text(
                    text = intervalText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = BrandPrimary
                )
            }
        }

        // Diff Lines Content
        if (displayLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No code differences detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandOnSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll)
            ) {
                displayLines.forEach { line ->
                    DiffLineRow(line = line)
                }
            }
        }
    }
}

@Composable
fun DiffLineRow(
    line: DiffLine,
    modifier: Modifier = Modifier
) {
    val (rowBg, borderColor, textColor, prefix) = when (line.type) {
        DiffType.ADDED -> Quad(DiffAddedBg, DiffAddedBorder, DiffAddedText, "+")
        DiffType.DELETED -> Quad(DiffRemovedBg, DiffRemovedBorder, DiffRemovedText, "-")
        DiffType.UNCHANGED -> Quad(Color.Transparent, Color.Transparent, Color(0xFF64748B), " ")
    }

    val lineNumStr = (line.newLineNumber ?: line.oldLineNumber)?.toString() ?: ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Colored Border Bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(borderColor)
        )

        // Line Number Gutter
        Text(
            text = lineNumStr,
            modifier = Modifier
                .width(36.dp)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            ),
            color = textColor.copy(alpha = 0.6f)
        )

        // Sign (+ / - / space)
        Text(
            text = prefix,
            modifier = Modifier.padding(end = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )

        // Code Content
        Text(
            text = line.content,
            modifier = Modifier.padding(end = 16.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            ),
            color = textColor,
            softWrap = false
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
