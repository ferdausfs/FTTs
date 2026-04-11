package com.ftt.signal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID   = "ftt_signal_channel"
        const val CHANNEL_NAME = "FTT Signal Alerts"
        private const val NOTIFICATION_ID = 1001
    }

    init { createChannel() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "New trading signal alerts"
                enableVibration(true)
                enableLights(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun sendSignalNotification(pair: String, direction: String, grade: String) {
        val emoji = when (direction.uppercase()) {
            "BUY"  -> "🟢"
            "SELL" -> "🔴"
            else   -> "📊"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji $pair — $direction")
            .setContentText("Grade $grade  •  New signal available")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission not granted — silent fail
        }
    }
}
