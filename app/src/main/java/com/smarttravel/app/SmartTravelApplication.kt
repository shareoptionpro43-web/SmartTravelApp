package com.smarttravel.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SmartTravelApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Init Timber logging (debug only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Location tracking channel
            NotificationChannel(
                CHANNEL_LOCATION,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while location tracking is active"
                manager.createNotificationChannel(this)
            }

            // Smart alerts channel
            NotificationChannel(
                CHANNEL_ALERTS,
                "Smart Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Arrival/departure and stop alerts"
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_LOCATION = "channel_location_tracking"
        const val CHANNEL_ALERTS = "channel_smart_alerts"
    }
}
