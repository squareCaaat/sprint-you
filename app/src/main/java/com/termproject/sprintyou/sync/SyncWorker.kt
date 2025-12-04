package com.termproject.sprintyou.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            FirebaseSyncManager.pushLocalData(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}