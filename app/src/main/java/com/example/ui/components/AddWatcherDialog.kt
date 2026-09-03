package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Watcher
import com.example.network.FetchResult
import com.example.ui.theme.*
import com.example.util.ScheduleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWatcherDialog(
    initialWatcher: Watcher?,
    isTestingUrl: Boolean,
    testResult: FetchResult?,
    onDismiss: () -> Unit,
    onTestUrl: (String) -> Unit,
    onSave: (id: Long, name: String, url: String, intervalMinutes: Int, stripScripts: Boolean, formatHtml: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialWatcher?.name ?: "") }
    var url by remember { mutableStateOf(initialWatcher?.url ?: "") }
    var intervalMinutes by remember { mutableStateOf(initialWatcher?.intervalMinutes ?: 15) }
    var customIntervalText by remember { mutableStateOf(if (initialWatcher != null && initialWatcher.intervalMinutes !in listOf(1, 5, 15, 30, 60, 360, 720, 1440)) initialWatcher.intervalMinutes.toString() else "") }
    var isCustomInterval by remember { mutableStateOf(initialWatcher != null && initialWatcher.intervalMinutes !in listOf(1, 5, 15, 30, 60, 360, 720, 1440)) }
    var stripScripts by remember { mutableStateOf(initialWatcher?.stripScripts ?: true) }
    var formatHtml by remember { mutableStateOf(initialWatcher?.formatHtml ?: true) }

    var urlError by remember { mutableStateOf<String?>(null) }

    // Auto-fill title if test fetched
    LaunchedEffect(testResult) {
        if (testResult != null && testResult.isSuccess && testResult.title != null && name.isBlank()) {
            name = testResult.title.take(40)
        }
    }

    val intervalPresets = listOf(
        Pair(1, "1m (Test)"),
        Pair(5, "5m"),
        Pair(15, "15m"),
        Pair(30, "30m"),
        Pair(60, "1h"),
        Pair(360, "6h"),
        Pair(1440, "24h")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, BrandOutline, RoundedCornerShape(28.dp)),
            color = BrandSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialWatcher != null) "Edit Watcher" else "Add Web Watcher",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandOnSurfaceVariant)
                    }
                }

                // URL Field
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            urlError = null
                        },
                        label = { Text("Webpage URL") },
                        placeholder = { Text("e.g. github.com/trending or example.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watcher_url_input"),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        isError = urlError != null,
                        supportingText = if (urlError != null) {
                            { Text(urlError!!, color = BrandError) }
                        } else null,
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = BrandPrimary)
                        },
                        trailingIcon = {
                            if (url.isNotBlank()) {
                                IconButton(onClick = { url = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Test Fetch Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (url.isBlank()) {
                                    urlError = "Please enter a valid URL"
                                } else {
                                    onTestUrl(url)
                                }
                            },
                            enabled = !isTestingUrl && url.isNotBlank(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimaryContainer,
                                contentColor = BrandOnPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isTestingUrl) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandOnPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Connection", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Test Result Feedback
                if (testResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (testResult.isSuccess) DiffAddedBg else DiffRemovedBg)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult.isSuccess) DiffAddedText else DiffRemovedText,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (testResult.isSuccess) "Connection verified (${testResult.responseTimeMs}ms)" else "Fetch error",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (testResult.isSuccess) DiffAddedText else DiffRemovedText
                                )
                                if (testResult.title != null) {
                                    Text(
                                        text = "Title: ${testResult.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DiffAddedText,
                                        maxLines = 1
                                    )
                                } else if (testResult.errorMessage != null) {
                                    Text(
                                        text = testResult.errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DiffRemovedText
                                    )
                                }
                            }
                        }
                    }
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Watcher Name") },
                    placeholder = { Text("e.g. VS Code Changelog") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watcher_name_input"),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Label, contentDescription = null, tint = BrandSecondary)
                    }
                )

                // Interval Section
                Column {
                    Text(
                        text = "Check Interval",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalPresets.take(4).forEach { (mins, label) ->
                            val isSelected = !isCustomInterval && intervalMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    intervalMinutes = mins
                                    isCustomInterval = false
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = BrandOnPrimary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalPresets.drop(4).forEach { (mins, label) ->
                            val isSelected = !isCustomInterval && intervalMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    intervalMinutes = mins
                                    isCustomInterval = false
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = BrandOnPrimary
                                )
                            )
                        }

                        FilterChip(
                            selected = isCustomInterval,
                            onClick = { isCustomInterval = true },
                            label = { Text("Custom", fontSize = 11.sp) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimary,
                                selectedLabelColor = BrandOnPrimary
                            )
                        )
                    }

                    if (isCustomInterval) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customIntervalText,
                            onValueChange = {
                                customIntervalText = it.filter { ch -> ch.isDigit() }
                                val parsed = customIntervalText.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    intervalMinutes = parsed
                                }
                            },
                            label = { Text("Custom minutes") },
                            placeholder = { Text("e.g. 10") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val scheduleCount = ScheduleHelper.getScheduleCount(intervalMinutes)
                    Text(
                        text = "Generates a 1-month schedule list ($scheduleCount timestamps) & tracks closest next countdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandOnSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                // Advanced Comparison Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandSurfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ignore dynamic scripts & styles",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Prevents false alerts from tracking tokens",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = stripScripts,
                            onCheckedChange = { stripScripts = it }
                        )
                    }

                    HorizontalDivider(color = BrandOutline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Format HTML for GitHub diffs",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Splits minified tags into clean lines",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = formatHtml,
                            onCheckedChange = { formatHtml = it }
                        )
                    }
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (url.isBlank()) {
                                urlError = "URL is required"
                            } else {
                                onSave(
                                    initialWatcher?.id ?: 0L,
                                    name,
                                    url,
                                    intervalMinutes,
                                    stripScripts,
                                    formatHtml
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_watcher_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        )
                    ) {
                        Text("Save Watcher")
                    }
                }
            }
        }
    }
}
