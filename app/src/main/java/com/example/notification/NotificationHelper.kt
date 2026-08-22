package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_ID = "webwatch_alerts_channel"
    const val CHANNEL_NAME = "WebWatch Alerts"
    const val CHANNEL_DESC = "Notifications for detected webpage changes"

    const val EXTRA_WATCHER_ID = "extra_watcher_id"
    const val EXTRA_OPEN_DIFF = "extra_open_diff"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun notifyChangeDetected(
        context: Context,
        watcherId: Long,
        watcherName: String,
        url: String,
        additions: Int,
        deletions: Int
    ) {
        createNotificationChannel(context)

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WATCHER_ID, watcherId)
            putExtra(EXTRA_OPEN_DIFF, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            watcherId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val diffSummary = buildString {
            if (additions > 0) append("+$additions lines ")
            if (deletions > 0) append("-$deletions lines")
            if (isEmpty()) append("HTML modified")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Change Detected: $watcherName")
            .setContentText("Diff: $diffSummary • $url")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("WebWatch detected $diffSummary on $watcherName.\n$url\nTap to view the GitHub-style diff.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF005AC1.toInt())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(watcherId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission denied
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
