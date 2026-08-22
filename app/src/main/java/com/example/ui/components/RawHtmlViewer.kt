package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class RawHtmlVersion {
    CURRENT,
    PREVIOUS
}

@Composable
fun RawHtmlViewer(
    currentHtml: String?,
    previousHtml: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedVersion by remember { mutableStateOf(RawHtmlVersion.CURRENT) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchOpen by remember { mutableStateOf(false) }
    var softWrap by remember { mutableStateOf(false) }

    val activeHtml = when (selectedVersion) {
        RawHtmlVersion.CURRENT -> currentHtml ?: ""
        RawHtmlVersion.PREVIOUS -> previousHtml ?: ""
    }

    val lines = remember(activeHtml) {
        if (activeHtml.isEmpty()) emptyList() else activeHtml.lines()
    }

    val filteredLinesWithIndex = remember(lines, searchQuery) {
        if (searchQuery.isBlank()) {
            lines.mapIndexed { idx, line -> Pair(idx + 1, line) }
        } else {
            lines.mapIndexed { idx, line -> Pair(idx + 1, line) }
                .filter { it.second.contains(searchQuery, ignoreCase = true) }
        }
    }

    val hScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BrandOutline, RoundedCornerShape(16.dp))
            .background(Color.White)
            .testTag("raw_html_viewer")
    ) {
        // Controls Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DiffHeaderBg,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Version Segmented Buttons
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE2E8F0))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selectedVersion == RawHtmlVersion.CURRENT) BrandPrimary else Color.Transparent,
                            onClick = { selectedVersion = RawHtmlVersion.CURRENT }
                        ) {
                            Text(
                                text = "Current (${(currentHtml ?: "").lines().size}L)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (selectedVersion == RawHtmlVersion.CURRENT) BrandOnPrimary else BrandOnSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selectedVersion == RawHtmlVersion.PREVIOUS) BrandPrimary else Color.Transparent,
                            onClick = { selectedVersion = RawHtmlVersion.PREVIOUS }
                        ) {
                            Text(
                                text = "Previous (${(previousHtml ?: "").lines().size}L)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (selectedVersion == RawHtmlVersion.PREVIOUS) BrandOnPrimary else BrandOnSurfaceVariant
                            )
                        }
                    }

                    // Action Icons: Wrap, Search, Copy
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { softWrap = !softWrap },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WrapText,
                                contentDescription = "Toggle wrap",
                                tint = if (softWrap) BrandPrimary else BrandOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isSearchOpen = !isSearchOpen },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search HTML",
                                tint = if (isSearchOpen) BrandPrimary else BrandOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("HTML", activeHtml))
                                Toast.makeText(context, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = BrandOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Expandable Search Field
                if (isSearchOpen) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = { Text("Filter tags or text...", fontSize = 12.sp) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            }
        }

        // Code Lines Body
        if (filteredLinesWithIndex.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (activeHtml.isEmpty()) "No HTML stored for this snapshot." else "No lines match \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandOnSurfaceVariant
                )
            }
        } else {
            val listModifier = if (!softWrap) {
                Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScroll)
            } else {
                Modifier.fillMaxSize()
            }

            LazyColumn(modifier = listModifier) {
                itemsIndexed(filteredLinesWithIndex) { _, (lineNum, content) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (lineNum % 2 == 0) Color(0xFFFAFAFA) else Color.White)
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Line number gutter
                        Text(
                            text = lineNum.toString(),
                            modifier = Modifier
                                .width(42.dp)
                                .padding(horizontal = 6.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )

                        // Code text
                        Text(
                            text = content,
                            modifier = Modifier.padding(end = 16.dp),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFF1E293B),
                            softWrap = softWrap
                        )
                    }
                }
            }
        }
    }
}
