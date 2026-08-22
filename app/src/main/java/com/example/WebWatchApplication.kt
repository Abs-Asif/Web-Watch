package com.example

import android.app.Application
import com.example.notification.NotificationHelper
import com.example.worker.WorkScheduler

class WebWatchApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)
        // Ensure background periodic monitoring is scheduled
        WorkScheduler.schedulePeriodicMonitoring(this, intervalMinutes = 15)
    }
}
