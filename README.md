# WebWatch 🌐🔍

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-005AC1.svg)](https://m3.material.io)
[![Room](https://img.shields.io/badge/Database-Room-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Build & Release](https://github.com/actions/workflows/release.yml/badge.svg)](.github/workflows/release.yml)

**WebWatch** is a modern Android application built with Kotlin and Jetpack Compose that tracks webpage UI and HTML code modifications in real time. Featuring GitHub-style diff rendering, periodic background monitoring via Android WorkManager, instant notifications, and local Room database snapshot logs.

---

## ✨ Key Features

### 🔍 1. Webpage Watcher Management
- **Custom URL Monitoring**: Add any public webpage or API endpoint with custom monitoring intervals (1m test, 5m, 15m, 30m, 1h, 6h, 24h, or custom minutes).
- **Live Connection Tester**: Test URLs instantly before saving, measure network latency, and automatically parse page titles.
- **Smart HTML Normalization**: Option to strip volatile tracking scripts and auto-format minified HTML tags for clean line-by-line diff tracking.

### 🎨 2. GitHub-Style Diff Comparison
- **Unified Diff Mode**: Clean line numbers with green highlighted additions (`+`) and red deletions (`-`), featuring a context filter to collapse unchanged sections.
- **Side-by-Side Mode**: Synchronized dual-pane view showing the previous snapshot on the left and current snapshot on the right.
- **Raw HTML Inspector**: Full source code viewer with quick copy, search query filtering, wrap toggle, and version toggle between Current and Previous snapshots.

### ⏰ 3. Background Sync & Instant Notifications
- **WorkManager Periodic Checks**: Automatically fetches webpage source code in the background even when the app is closed.
- **System Notification Alerts**: Receive instant push notifications on Android 13+ whenever code modifications are detected. Tapping the alert opens directly into the visual diff comparison screen.
- **Battery Optimization Bypass**: Quick guidance and settings toggle to exempt WebWatch from aggressive OS battery throttling.

### 📜 4. Snapshot History & Audit Log
- **Local Persistence**: All watcher configurations and historical change snapshots (`+X additions / -Y deletions`) are saved securely in an on-device Room SQLite database.
- **Change Details Modal**: Click any historical log entry to inspect the exact diff recorded at that point in time.

### 🛡️ 5. Native Navigation & Exit Safeguards
- **Back-Stack Management**: Seamless back navigation returning from diff view, closing dialogs, or switching back to the main watcher list.
- **Exit Confirmation**: Double-checks before closing the app to ensure background monitoring status remains clear to the user.

---

## 🏛 Architecture & Tech Stack

WebWatch follows Android's recommended MVVM (Model-View-ViewModel) architecture:

```
web.watch.abdullah/
├── data/
│   ├── dao/             # Room DAOs (WatcherDao, SnapshotDao)
│   ├── database/        # AppDatabase (Room DB definition & migrations)
│   └── model/           # Data entities (Watcher, SnapshotHistory)
├── notification/        # Android NotificationChannel & alert dispatcher
├── ui/
│   ├── components/      # UI components (DiffViewer, SideBySideDiffViewer, RawHtmlViewer, Cards, Dialogs)
│   ├── screens/         # Main screens (WatchersScreen, DiffDetailScreen, HistoryScreen, SettingsScreen)
│   └── theme/           # Material 3 Color scheme, Typography, Shapes
├── util/                # DiffUtils (LCS Diff algorithm, Normalizer, Side-by-Side splitter), BatteryUtils
└── worker/              # WatcherCheckWorker (WorkManager background sync job)
```

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose & Material 3 |
| **Local Database** | Room Database with SQLite |
| **Networking** | OkHttp 4 / Coroutines |
| **Background Execution** | AndroidX WorkManager |
| **Diff Engine** | Custom LCS-based line diff algorithm with syntax coloring |

---

## 🚀 Getting Started & Building

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17 or higher
- Android SDK 36 (minSdk: 24, targetSdk: 36)

### Clone & Open
```bash
git clone https://github.com/your-username/webwatch.git
cd webwatch
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
WebWatch includes demo release signing configuration. You can build a signed release APK using:
```bash
export STORE_PASSWORD="demopassword123"
export KEY_PASSWORD="demopassword123"
./gradlew assembleRelease
```
The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 🔄 CI/CD & Automated GitHub Releases

This repository includes an automated GitHub Actions workflow located at [`.github/workflows/release.yml`](.github/workflows/release.yml):

- **Trigger**: Runs automatically on every push/merge to `main` or `master` branch, or manually via `workflow_dispatch`.
- **Process**:
  1. Sets up JDK 17 and Gradle environment.
  2. Generates a demo release keystore on the fly.
  3. Builds the release APK (`./gradlew assembleRelease`).
  4. Automatically packages the output as `WebWatch-v1.0.<RUN_NUMBER>.apk`.
  5. Publishes a new GitHub Release under repository Releases (not in a local directory) with changelogs and attaches the signed release APK ready for download.

---

## 📱 Application Details

- **Application ID**: `web.watch.abdullah`
- **App Name**: WebWatch
- **Default Theme**: Sleek Minimalist & Material 3 Dark/Light

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
