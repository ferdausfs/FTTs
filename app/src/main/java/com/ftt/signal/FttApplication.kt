package com.ftt.signal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ftt.signal.worker.SignalSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application entry point.
 *
 * @HiltAndroidApp triggers Hilt's code generation and initialises the
 * component hierarchy.  Implementing [Configuration.Provider] lets us
 * supply a Hilt-aware [HiltWorkerFactory] to WorkManager so injected
 * workers (like [SignalSyncWorker]) receive their dependencies.
 */
@HiltAndroidApp
class FttApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        schedulePeriodicSignalSync()
    }

    /** Provide a custom WorkManager config that uses Hilt's worker factory. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.ERROR)
            .build()

    /**
     * Enqueue a periodic background task that refreshes the current pair's
     * signal every 15 minutes when network is available.
     * Uses KEEP policy — survives process restarts without duplication.
     */
    private fun schedulePeriodicSignalSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SignalSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SignalSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
