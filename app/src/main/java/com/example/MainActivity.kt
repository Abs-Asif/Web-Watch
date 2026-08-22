package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notification.NotificationHelper
import com.example.ui.MainTab
import com.example.ui.WebWatchViewModel
import com.example.ui.components.AddWatcherDialog
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.components.HeaderBar
import com.example.ui.components.SnapshotDetailDialog
import com.example.ui.screens.DiffDetailScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WatchersScreen
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.util.BatteryUtils

class MainActivity : ComponentActivity() {

    private val viewModel: WebWatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleNotificationIntent(intent)

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val activity = context as? Activity
                val snackbarHostState = remember { SnackbarHostState() }
                var showExitConfirmDialog by remember { mutableStateOf(false) }

                // Notification Permission Request (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.refreshBatteryStatus()
                    }
                }

                // Launch notification permission prompt automatically on startup if needed
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Handle Snackbar User Messages
                LaunchedEffect(uiState.userMessage) {
                    uiState.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                // Action to request battery optimization exemption
                val requestBatteryOptimization = {
                    val intent = BatteryUtils.createRequestIgnoreBatteryOptimizationsIntent(context)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                }

                val selectedWatcher = uiState.watchers.find { it.id == uiState.selectedWatcherId }

                // Navigation BackHandler System
                BackHandler(enabled = true) {
                    when {
                        showExitConfirmDialog -> {
                            showExitConfirmDialog = false
                        }
                        selectedWatcher != null -> {
                            viewModel.selectWatcher(null)
                        }
                        uiState.isAddDialogOpen -> {
                            viewModel.closeAddDialog()
                        }
                        uiState.selectedSnapshot != null -> {
                            viewModel.selectSnapshot(null)
                        }
                        uiState.currentTab != MainTab.WATCHERS -> {
                            viewModel.setTab(MainTab.WATCHERS)
                        }
                        else -> {
                            // On root screen, prompt user before exiting
                            showExitConfirmDialog = true
                        }
                    }
                }

                if (selectedWatcher != null) {
                    // Fullscreen Diff Detail View with its own TopAppBar & BottomBar
                    DiffDetailScreen(
                        watcher = selectedWatcher,
                        diffMode = uiState.diffDisplayMode,
                        onlyChangedLines = uiState.onlyChangedLines,
                        onBackClicked = { viewModel.selectWatcher(null) },
                        onModeChanged = { viewModel.setDiffMode(it) },
                        onToggleOnlyChanged = { viewModel.toggleOnlyChangedLines() },
                        onMarkReviewed = {
                            viewModel.markReviewed(selectedWatcher.id)
                            viewModel.selectWatcher(null)
                        },
                        onCheckNow = { viewModel.checkWatcherNow(selectedWatcher.id) }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrandBackground),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            HeaderBar(
                                isChecking = uiState.isCheckingNow,
                                onSyncAllClicked = { viewModel.checkAllNow() }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(
                                currentTab = uiState.currentTab,
                                onTabSelected = { viewModel.setTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (uiState.currentTab) {
                                MainTab.WATCHERS -> {
                                    WatchersScreen(
                                        watchers = uiState.watchers,
                                        isBatteryOptimized = uiState.isBatteryOptimized,
                                        isChecking = uiState.isCheckingNow,
                                        checkingWatcherId = uiState.checkingWatcherId,
                                        onEnableBatteryOptimization = requestBatteryOptimization,
                                        onWatcherSelected = { viewModel.selectWatcher(it) },
                                        onCheckWatcherNow = { viewModel.checkWatcherNow(it) },
                                        onEditWatcher = { viewModel.openAddDialog(it) },
                                        onToggleWatcherActive = { viewModel.toggleWatcherActive(it) },
                                        onDeleteWatcher = { viewModel.deleteWatcher(it) },
                                        onSimulateTestChange = { viewModel.simulateChangeForTesting(it) },
                                        onMarkReviewed = { viewModel.markReviewed(it) },
                                        onAddNewClicked = { viewModel.openAddDialog() }
                                    )
                                }

                                MainTab.HISTORY -> {
                                    HistoryScreen(
                                        history = uiState.history,
                                        onSnapshotClicked = { viewModel.selectSnapshot(it) },
                                        onClearHistory = { viewModel.clearAllHistory() }
                                    )
                                }

                                MainTab.SETTINGS -> {
                                    SettingsScreen(
                                        isBatteryOptimized = uiState.isBatteryOptimized,
                                        onEnableBatteryOptimization = requestBatteryOptimization,
                                        onRequestNotificationPermission = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:$packageName")
                                                }
                                                context.startActivity(appIntent)
                                            }
                                        },
                                        onSyncAllNow = { viewModel.checkAllNow() },
                                        onClearAllHistory = { viewModel.clearAllHistory() }
                                    )
                                }
                            }

                            // Add / Edit Watcher Dialog
                            if (uiState.isAddDialogOpen) {
                                AddWatcherDialog(
                                    initialWatcher = uiState.editingWatcher,
                                    isTestingUrl = uiState.isTestingUrl,
                                    testResult = uiState.testFetchResult,
                                    onDismiss = { viewModel.closeAddDialog() },
                                    onTestUrl = { viewModel.testFetch(it) },
                                    onSave = { id, name, url, interval, stripScripts, formatHtml ->
                                        viewModel.saveWatcher(
                                            id = id,
                                            name = name,
                                            url = url,
                                            intervalMinutes = interval,
                                            stripScripts = stripScripts,
                                            formatHtml = formatHtml
                                        )
                                    }
                                )
                            }

                            // History Snapshot Detail Dialog
                            if (uiState.selectedSnapshot != null) {
                                SnapshotDetailDialog(
                                    snapshot = uiState.selectedSnapshot!!,
                                    onDismiss = { viewModel.selectSnapshot(null) }
                                )
                            }
                        }
                    }
                }

                // App Close Confirmation Dialog
                if (showExitConfirmDialog) {
                    ExitConfirmationDialog(
                        onConfirmExit = {
                            showExitConfirmDialog = false
                            activity?.finish()
                        },
                        onDismiss = {
                            showExitConfirmDialog = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val watcherId = intent?.getLongExtra(NotificationHelper.EXTRA_WATCHER_ID, -1L) ?: -1L
        if (watcherId > 0) {
            viewModel.selectWatcher(watcherId)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryStatus()
    }
}
