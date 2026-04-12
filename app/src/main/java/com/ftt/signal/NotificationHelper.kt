package com.ftt.signal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_SIGNALS = "ftt_signals"
        private val counter = AtomicInteger(1000)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_SIGNALS, "FTT Signal Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Trading signal alerts"; enableVibration(true) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    fun notifySignal(pair: String, direction: String, grade: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val emoji = if (direction == "BUY") "🟢" else "🔴"
        val n = NotificationCompat.Builder(context, CHANNEL_SIGNALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$emoji $pair — $direction")
            .setContentText(if (grade.isNotEmpty()) "Grade $grade • New signal" else "New signal")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(counter.incrementAndGet(), n) }
        catch (e: SecurityException) {}
    }

    fun vibrate(ms: Int) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun hasPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    else true
}
