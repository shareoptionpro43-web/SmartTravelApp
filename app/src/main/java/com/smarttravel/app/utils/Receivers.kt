package com.smarttravel.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.smarttravel.app.R
import com.smarttravel.app.SmartTravelApplication
import com.smarttravel.app.ui.MainActivity
import timber.log.Timber

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val placeName = intent.getStringExtra("place_name") ?: "Saved location"
        val transitionType = intent.getStringExtra("transition_type") ?: "arrival"
        val message = if (transitionType == "enter") "Arrived at $placeName" else "Departed from $placeName"

        Timber.d("Geofence: $message")
        showNotification(context, message)
    }

    private fun showNotification(context: Context, message: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, SmartTravelApplication.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_location_pin)
            .setContentTitle("Smart Travel Alert")
            .setContentText(message)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.d("Boot completed - SmartTravel ready")
            // Could restart tracking service if it was active before shutdown
        }
    }
}
