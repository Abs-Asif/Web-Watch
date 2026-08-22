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
import com.example.util.DiffType
import com.example.util.SideBySideRow

@Composable
fun SideBySideDiffViewer(
    rows: List<SideBySideRow>,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
            .background(Color.White)
            .testTag("side_by_side_diff_viewer")
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DiffHeaderBg)
                .border(width = 0.5.dp, color = BrandOutline)
        ) {
            // Left Header (BEFORE)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DiffRemovedBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BEFORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DiffRemovedText
                    )
                }
                Text(
                    text = "Previous Snapshot",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = BrandOnSurfaceVariant
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(BrandOutline)
            )

            // Right Header (AFTER)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DiffAddedBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AFTER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = DiffAddedText
                    )
                }
                Text(
                    text = "Current Snapshot",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = BrandOnSurfaceVariant
                )
            }
        }

        // Main Rows
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No differences to compare side-by-side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandOnSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScroll)
            ) {
                items(rows) { row ->
                    SideBySideRowItem(row = row)
                }
            }
        }
    }
}

@Composable
private fun SideBySideRowItem(row: SideBySideRow) {
    val leftBg = when (row.leftType) {
        DiffType.DELETED -> DiffRemovedBg
        DiffType.UNCHANGED -> Color.Transparent
        else -> Color(0xFFFAFAFA)
    }
    val leftTextCol = when (row.leftType) {
        DiffType.DELETED -> DiffRemovedText
        DiffType.UNCHANGED -> Color(0xFF475569)
        else -> Color.Transparent
    }
    val leftBorder = when (row.leftType) {
        DiffType.DELETED -> DiffRemovedBorder
        else -> Color.Transparent
    }

    val rightBg = when (row.rightType) {
        DiffType.ADDED -> DiffAddedBg
        DiffType.UNCHANGED -> Color.Transparent
        else -> Color(0xFFFAFAFA)
    }
    val rightTextCol = when (row.rightType) {
        DiffType.ADDED -> DiffAddedText
        DiffType.UNCHANGED -> Color(0xFF475569)
        else -> Color.Transparent
    }
    val rightBorder = when (row.rightType) {
        DiffType.ADDED -> DiffAddedBorder
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(width = 0.5.dp, color = Color(0xFFF1F5F9))
    ) {
        // Left Cell (Previous)
        Row(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(leftBg)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(leftBorder)
            )
            Text(
                text = row.leftLineNumber?.toString() ?: "",
                modifier = Modifier
                    .width(32.dp)
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = leftTextCol.copy(alpha = 0.6f)
            )
            Text(
                text = if (row.leftType == DiffType.DELETED) "- " else if (row.leftType == DiffType.UNCHANGED) "  " else "  ",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = leftTextCol
            )
            Text(
                text = row.leftContent ?: "",
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = leftTextCol,
                softWrap = false
            )
        }

        // Center Split Line
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(BrandOutline)
        )

        // Right Cell (Current)
        Row(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(rightBg)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(rightBorder)
            )
            Text(
                text = row.rightLineNumber?.toString() ?: "",
                modifier = Modifier
                    .width(32.dp)
                    .padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = rightTextCol.copy(alpha = 0.6f)
            )
            Text(
                text = if (row.rightType == DiffType.ADDED) "+ " else if (row.rightType == DiffType.UNCHANGED) "  " else "  ",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = rightTextCol
            )
            Text(
                text = row.rightContent ?: "",
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = rightTextCol,
                softWrap = false
            )
        }
    }
}
