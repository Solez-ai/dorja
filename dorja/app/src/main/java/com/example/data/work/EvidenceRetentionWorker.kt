package com.example.data.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DorjaApp
import java.util.concurrent.TimeUnit

/**
 * GDPR-style retention enforcement (atlas §3 + Phase 3 minimisation rule).
 *
 * Removes evidence rows whose `retentionUntil` timestamp has passed. Evidence
 * is stored as metadata rows only, so removal is a row delete — the user's
 * "keep until" choice is honoured even when the app is not open, which is the
 * whole point of doing this in WorkManager rather than only at launch.
 *
 * Scheduling:
 * - [schedule] runs the sweep roughly every 24 hours (unique periodic work).
 * - [runNow] runs a one-off sweep at app start so expiry applies immediately
 *   on the first launch after an update.
 */
class EvidenceRetentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val removed = DorjaApp.instance.repository.applyRetentionCutoff()
            if (removed > 0) {
                Log.i("EvidenceRetention", "Removed $removed evidence row(s) past retention cutoff")
            }
            Result.success()
        } catch (e: Exception) {
            Log.w("EvidenceRetention", "Retention sweep failed, will retry: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "evidence_retention_periodic"
        private const val ONESHOT_WORK_NAME = "evidence_retention_oneshot"

        /** Schedule the daily retention sweep. Safe to call repeatedly. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<EvidenceRetentionWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Run one retention sweep as soon as the app starts. */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<EvidenceRetentionWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
